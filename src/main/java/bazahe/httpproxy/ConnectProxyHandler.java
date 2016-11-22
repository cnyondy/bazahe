package bazahe.httpproxy;

import bazahe.exception.HttpParserException;
import bazahe.httpparse.*;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.Strings;
import net.dongliu.commons.codec.Digests;
import net.dongliu.commons.concurrent.Lazy;
import net.dongliu.commons.io.Closeables;
import net.dongliu.commons.io.InputOutputs;

import javax.annotation.Nullable;
import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handle connect method.
 * Connect may proxy maybe: https(1.x, 2.0), webSocket, or even plain http1.x traffics
 *
 * @author Liu Dong
 */
@Log4j2
public class ConnectProxyHandler implements ProxyHandler {
    private final static ConcurrentHashMap<String, SSLContext> sslContextCache = new ConcurrentHashMap<>();

    private final Lazy<AppKeyStoreGenerator> appKeyStoreGeneratorLazy;
    private final static char[] appKeyStorePassword = "123456".toCharArray();

    public ConnectProxyHandler(Lazy<AppKeyStoreGenerator> appKeyStoreGeneratorLazy) {
        this.appKeyStoreGeneratorLazy = appKeyStoreGeneratorLazy;
    }

    @Override
    public void handle(Socket socket, String rawRequestLine, @Nullable HttpMessageListener httpMessageListener)
            throws IOException {
        HttpInputStream input = new HttpInputStream(socket.getInputStream());
        input.putBackLine(rawRequestLine);
        RequestHeaders headers = input.readRequestHeaders();
        if (headers == null) {
            //should not happen because we already check it
            return;
        }
        RequestLine requestLine = headers.getRequestLine();
        String target = requestLine.getUrl();
        log.debug("Receive connect request to {}", target);
        String host = Strings.before(target, ":");
        int port = Integer.parseInt(Strings.after(target, ":"));
        // just tell client ok..
        HttpOutputStream output = new HttpOutputStream(socket.getOutputStream());
        output.writeLine("HTTP/1.1 200 OK\r\n");
        output.flush();

        // read first two byte to see if is ssl/tls connection
        int first = input.read();
        int second = input.read();
        int third = input.read();

        byte[] heading = {(byte) first, (byte) second, (byte) third};
        Socket wrappedSocket = new WrappedSocket(socket, heading);

        String protocol;
        Socket serverSocket;
        Socket clientSocket;
        if (first == 22 && second <= 3 && third <= 3) {
            // ssl client hello message
            SSLContext sslContext = sslContextCache.computeIfAbsent(host, this::createSSlContext);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(wrappedSocket, null, socket.getPort(),
                    false);
            sslSocket.setUseClientMode(false);
            serverSocket = sslSocket;
            protocol = "https";
            SSLContext clientSSlContext = createClientSSlContext();
            SSLSocketFactory factory = clientSSlContext.getSocketFactory();
            clientSocket = factory.createSocket(host, port);
        } else {
            serverSocket = wrappedSocket;
            protocol = "http";
            clientSocket = new Socket(host, port);
        }

        try {
            handle(serverSocket, clientSocket, protocol, target, httpMessageListener);
        } finally {
            Closeables.closeQuietly(clientSocket);
        }

    }

    private void handle(Socket serverSocket, Socket clientSocket, String protocol, String target,
                        @Nullable HttpMessageListener httpMessageListener) throws IOException {
        HttpInputStream srcInput = new HttpInputStream(new BufferedInputStream(serverSocket.getInputStream()));
        HttpOutputStream srcOutput = new HttpOutputStream(serverSocket.getOutputStream());
        HttpInputStream destInput = new HttpInputStream(new BufferedInputStream(clientSocket.getInputStream()));
        HttpOutputStream destOutput = new HttpOutputStream(clientSocket.getOutputStream());

        while (true) {
            boolean finish = handleOneRequest(srcInput, srcOutput, destInput, destOutput, protocol, target,
                    httpMessageListener);
            if (finish) {
                break;
            }
        }

    }

    @SneakyThrows
    private boolean handleOneRequest(HttpInputStream srcInput, HttpOutputStream srcOutput,
                                     HttpInputStream destInput, HttpOutputStream destOutput,
                                     String protocol, String target,
                                     @Nullable HttpMessageListener httpMessageListener) {
        @Nullable RequestHeaders requestHeaders = srcInput.readRequestHeaders();
        // client close connection
        if (requestHeaders == null) {
            log.debug("Client close connection");
            return true;
        }
        String rawRequestLine = requestHeaders.getRawRequestLine();
        log.debug("Accept new request: {}", rawRequestLine);

        // expect-100
        // TODO: should forward "100-continue" if target support http 1.1
        if ("100-continue".equalsIgnoreCase(requestHeaders.getFirst("Expect"))) {
            srcOutput.writeLine("HTTP/1.1 100 Continue\r\n");
        }

        String id = Digests.md5().update(rawRequestLine).toHexLower() + System.nanoTime();
        RequestLine requestLine = requestHeaders.getRequestLine();
        String url = protocol + "://" + target + requestLine.getUrl();

        @Nullable OutputStream requestStore = null;
        if (httpMessageListener != null) {
            requestStore = httpMessageListener.onRequest(id, url, requestHeaders);
        }

        destOutput.writeRequestHeaders(requestHeaders);


        boolean shouldClose = requestHeaders.shouldClose();
        @Cleanup @Nullable InputStream requestBody = getRequestBodyInputStream(srcInput, requestHeaders, destOutput);
        try {
            if (requestBody != null) {
                if (requestStore != null) {
                    InputOutputs.copy(requestBody, requestStore);
                } else {
                    InputOutputs.skipAll(requestBody);
                }
            }
        } finally {
            Closeables.closeQuietly(requestStore);
        }

        ResponseHeaders responseHeaders = destInput.readResponseHeaders();
        if (responseHeaders == null) {
            log.debug("Target server  close connection");
            return true;
        }

        srcOutput.writeResponseHeaders(responseHeaders);
        @Nullable OutputStream responseStore = null;
        if (httpMessageListener != null) {
            responseStore = httpMessageListener.onResponse(id, responseHeaders);
        }
        @Cleanup InputStream responseBody = getResponseBodyInput(destInput, responseHeaders, requestLine.getMethod(),
                srcOutput);
        try {
            if (responseBody != null) {
                if (responseStore != null) {
                    InputOutputs.copy(responseBody, responseStore);
                } else {
                    InputOutputs.skipAll(responseBody);
                }
            }
        } finally {
            Closeables.closeQuietly(responseStore);
        }

        String upgrade = requestHeaders.getFirst("Upgrade");

        if ("websocket".equals(upgrade) && responseHeaders.getStatusLine().getCode() == 101) {
            // upgrade to websocket
            log.info("{} upgrade to websocket", url);
            SocketsUtils.tunnel(srcInput, srcOutput, destInput, destOutput);
            shouldClose = true;
        } else if ("h2c".equals(upgrade) && responseHeaders.getStatusLine().getCode() == 101) {
            // upgrade to http2
            SocketsUtils.tunnel(srcInput, srcOutput, destInput, destOutput);
            log.info("{} upgrade to http2", url);
            shouldClose = true;
        }

        return shouldClose;
    }

    private InputStream getResponseBodyInput(HttpInputStream destInput, ResponseHeaders responseHeaders, String method,
                                             HttpOutputStream srcOutput) {
        destInput = new HttpInputStream(new ObservableInputStream(destInput, srcOutput));
        InputStream responseBody;
        if (responseHeaders.chunked()) {
            responseBody = destInput.getBody(-1);
        } else if (responseHeaders.contentLen() >= 0) {
            responseBody = destInput.getBody(responseHeaders.contentLen());
        } else if (!responseHeaders.hasBody() || method.equals("HEAD")) {
            responseBody = null;
        } else {
            throw new HttpParserException("Where is body");
        }

        return responseBody;
    }

    @SneakyThrows
    @Nullable
    private InputStream getRequestBodyInputStream(HttpInputStream input, RequestHeaders requestHeaders,
                                                  OutputStream destOutput) {
        input = new HttpInputStream(new ObservableInputStream(input, destOutput));
        InputStream requestBody;
        if (requestHeaders.chunked()) {
            requestBody = input.getBody(-1);
        } else if (requestHeaders.contentLen() >= 0) {
            requestBody = input.getBody(requestHeaders.contentLen());
        } else if (!requestHeaders.hasBody()) {
            requestBody = null;
        } else {
            throw new HttpParserException("Where is body");
        }
        return requestBody;
    }


    private KeyStore generateKeyStoreForSite(String host) {
        return appKeyStoreGeneratorLazy.get().generateKeyStore(host, 10, appKeyStorePassword);
    }

    @SneakyThrows
    private SSLContext createSSlContext(String host) {
        KeyStore keyStore = generateKeyStoreForSite(host);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, appKeyStorePassword);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagers, trustAllCerts, new SecureRandom());
        return sslContext;
    }


    @SneakyThrows
    private SSLContext createClientSSlContext() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        return sslContext;
    }
}
