package bazahe.httpproxy;

import bazahe.exception.HttpParserException;
import bazahe.httpparse.*;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.Strings;
import net.dongliu.commons.codec.Digests;
import net.dongliu.commons.io.Closeables;
import net.dongliu.commons.io.InputOutputs;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Handle connect method.
 * Connect may proxy maybe: https(1.x, 2.0), webSocket, or even plain http1.x traffics
 *
 * @author Liu Dong
 */
@Log4j2
public class ConnectProxyHandler implements ProxyHandler {

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
            SSLContext sslContext = SSLContextManager.getInstance().createSSlContext(host);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(wrappedSocket, null, socket.getPort(),
                    false);
            sslSocket.setUseClientMode(false);
            serverSocket = sslSocket;
            protocol = "https";
            SSLContext clientSSlContext = SSLUtils.createClientSSlContext();
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
        HttpInputStream srcInput = new HttpInputStream(new BufferedInputStream(
                new ObservableInputStream(serverSocket.getInputStream(), clientSocket.getOutputStream())));
        HttpInputStream destInput = new HttpInputStream(new BufferedInputStream(
                new ObservableInputStream(clientSocket.getInputStream(), serverSocket.getOutputStream())));


        while (true) {
            boolean finish = handleOneRequest(srcInput, destInput, protocol, target, httpMessageListener);
            if (finish) {
                break;
            }
        }

    }

    @SneakyThrows
    private boolean handleOneRequest(HttpInputStream srcInput, HttpInputStream destInput,
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
        if ("100-continue".equalsIgnoreCase(requestHeaders.getFirst("Expect"))) {
            // TODO: show read external server header, if have one
        }

        String id = Digests.md5().update(rawRequestLine).toHexLower() + System.nanoTime();
        RequestLine requestLine = requestHeaders.getRequestLine();
        String url = protocol + "://" + target + requestLine.getUrl();

        @Nullable OutputStream requestStore = null;
        if (httpMessageListener != null) {
            requestStore = httpMessageListener.onRequest(id, url, requestHeaders);
        }

        boolean shouldClose = requestHeaders.shouldClose();
        @Cleanup @Nullable InputStream requestBody = getRequestBodyInputStream(srcInput, requestHeaders);
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

        @Nullable OutputStream responseStore = null;
        if (httpMessageListener != null) {
            responseStore = httpMessageListener.onResponse(id, responseHeaders);
        }
        @Cleanup InputStream responseBody = getResponseBodyInput(destInput, responseHeaders, requestLine.getMethod());
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
            tunnel(srcInput, destInput);
            shouldClose = true;
        } else if ("h2c".equals(upgrade) && responseHeaders.getStatusLine().getCode() == 101) {
            // upgrade to http2
            tunnel(srcInput, destInput);
            log.info("{} upgrade to http2", url);
            shouldClose = true;
        }

        return shouldClose;
    }

    private void tunnel(InputStream input1, InputStream input2) throws InterruptedException {
        Thread thread = new Thread(() -> {
            try {
                IOUtils.consumeAll(input1);
            } catch (Throwable t) {
                log.warn("tunnel traffic failed", t);
            }
        });
        thread.start();
        IOUtils.consumeAll(input2);
        thread.join();
    }

    private InputStream getResponseBodyInput(HttpInputStream destInput, ResponseHeaders responseHeaders,
                                             String method) {
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
    private InputStream getRequestBodyInputStream(HttpInputStream input, RequestHeaders requestHeaders) {
        InputStream requestBody;
        if (requestHeaders.chunked()) {
            requestBody = input.getBody(-1);
        } else if (requestHeaders.contentLen() >= 0) {
            requestBody = input.getBody(requestHeaders.contentLen());
        } else if (!requestHeaders.hasBody()) {
            requestBody = null;
        } else {
            log.error("Cannot find body info, headers:", requestHeaders);
            throw new HttpParserException("Cannot find body info");
        }
        return requestBody;
    }


}
