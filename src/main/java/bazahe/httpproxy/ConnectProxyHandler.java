package bazahe.httpproxy;

import bazahe.exception.HttpParserException;
import bazahe.httpparse.*;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import net.dongliu.commons.Strings;
import net.dongliu.commons.codec.Digests;
import net.dongliu.commons.io.Closeables;
import net.dongliu.commons.io.InputOutputs;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;

/**
 * Handle connect method.
 * Connect may proxy maybe: https(1.x, 2.0), webSocket, or even plain http1.x traffics
 *
 * @author Liu Dong
 */
@Log4j2
public class ConnectProxyHandler implements ProxyHandler {

    private final SSLContextManager sslContextManager;

    public ConnectProxyHandler(SSLContextManager sslContextManager) {
        this.sslContextManager = sslContextManager;
    }

    @Override
    public void handle(Socket serverSocket, String rawRequestLine, @Nullable MessageListener messageListener)
            throws IOException {
        HttpInputStream input = new HttpInputStream(serverSocket.getInputStream());
        input.putBackLine(rawRequestLine);
        RequestHeaders headers = input.readRequestHeaders();
        if (headers == null) {
            //should not happen because we already check it
            return;
        }
        RequestLine requestLine = headers.getRequestLine();
        String target = requestLine.getPath();
        log.debug("Receive connect request to {}", target);
        String host = AddressUtils.getHostFromTarget(target);
        int port = AddressUtils.getPortFromTarget(target);
        // just tell client ok..
        HttpOutputStream output = new HttpOutputStream(serverSocket.getOutputStream());
        output.writeLine("HTTP/1.1 200 OK\r\n");
        output.flush();

        // read first two byte to see if is ssl/tls connection
        val bos = new ByteArrayOutputStream();
        TLSInputStream tlsIn = new TLSInputStream(new ObservableInputStream(serverSocket.getInputStream(), bos));
        val tlsPlaintextHeader = tlsIn.readPlaintextHeader();

        boolean ssl;
        Socket wrappedServerSocket;
        Socket clientSocket;
        if (tlsPlaintextHeader.isValidHandShake()) {
            //TODO: Java8 not support alpn now, it is hard to know if target server support http2 by tls. Wait java9
            val handShakeMessage = tlsIn.readHandShakeMessage();
            val clientHello = (TLSInputStream.ClientHello) handShakeMessage.getMessage();
            if (clientHello.alpnHas("h2")) {
                // connect to sever, and check sever protocol
            }

            Socket wrappedSocket = new WrappedSocket(serverSocket, bos.toByteArray());
            SSLContext serverSslContext = sslContextManager.createSSlContext(host);
            SSLSocketFactory sslSocketFactory = serverSslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(wrappedSocket, null, serverSocket.getPort(),
                    false);
            sslSocket.setUseClientMode(false);
            wrappedServerSocket = sslSocket;
            ssl = true;

            SSLContext clientSSlContext = SSLUtils.createClientSSlContext();
            SSLSocketFactory factory = clientSSlContext.getSocketFactory();
            clientSocket = factory.createSocket(host, port);
        } else {
            wrappedServerSocket = new WrappedSocket(serverSocket, bos.toByteArray());
            ssl = false;
            clientSocket = new Socket(host, port);
        }

        try {
            handle(wrappedServerSocket, clientSocket, ssl, target, messageListener);
        } catch (SSLHandshakeException e) {
            // something wrong with ssl
            log.error("SSL connection error for {}.", target, e);
        } finally {
            Closeables.closeQuietly(clientSocket);
        }

    }

    private void handle(Socket serverSocket, Socket clientSocket, Boolean ssl, String target,
                        @Nullable MessageListener messageListener) throws IOException {
        OutputStream srcOut = clientSocket.getOutputStream();
        HttpInputStream srcIn = new HttpInputStream(new BufferedInputStream(
                new ObservableInputStream(serverSocket.getInputStream(), srcOut)));
        HttpInputStream dstIn = new HttpInputStream(new BufferedInputStream(
                new ObservableInputStream(clientSocket.getInputStream(), serverSocket.getOutputStream())));


        while (true) {
            boolean finish = handleOneRequest(srcIn, srcOut, dstIn, ssl, target, messageListener);
            if (finish) {
                break;
            }
        }

    }

    @SneakyThrows
    private boolean handleOneRequest(HttpInputStream srcIn, OutputStream srcOut, HttpInputStream dstIn,
                                     boolean ssl, String target,
                                     @Nullable MessageListener messageListener) {
        @Nullable RequestHeaders requestHeaders = srcIn.readRequestHeaders();
        // client close connection
        if (requestHeaders == null) {
            log.debug("Client close connection");
            return true;
        }
        String rawRequestLine = requestHeaders.getRawRequestLine();
        log.debug("Accept new request: {}", rawRequestLine);

        // expect-100
        boolean expect100 = false;
        if ("100-continue".equalsIgnoreCase(requestHeaders.getFirst("Expect"))) {
            HttpOutputStream srcHttpOut = new HttpOutputStream(srcOut);
            srcHttpOut.writeLine("HTTP/1.1 100 Continue\r\n");
            expect100 = true;
        }

        String id = Digests.md5().update(rawRequestLine).toHexLower() + System.nanoTime();
        RequestLine requestLine = requestHeaders.getRequestLine();
        String upgrade = requestHeaders.getFirst("Upgrade");
        String host = requestHeaders.getFirst("Host");
        if (host == null) {
            host = AddressUtils.getHostFromTarget(target);
        }
        int port = AddressUtils.getPortFromTarget(target);

        String url = AddressUtils.getUrl(ssl, upgrade, host, port, requestLine.getPath());


        @Nullable OutputStream requestStore = null;
        if (messageListener != null) {
            requestStore = messageListener.onHttpRequest(id, host, url, requestHeaders);
        }

        boolean shouldClose = requestHeaders.shouldClose();
        @Cleanup @Nullable InputStream requestBody = getRequestBodyInputStream(srcIn, requestHeaders);
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

        ResponseHeaders responseHeaders = dstIn.readResponseHeaders();
        if (responseHeaders == null) {
            log.debug("Target server  close connection");
            return true;
        }
        int code = responseHeaders.getStatusLine().getCode();

        if (expect100) {
            // server respond expect-100-continue request
            if (code == 100) {
                // ignore this header, read next
                responseHeaders = dstIn.readResponseHeaders();
                if (responseHeaders == null) {
                    log.debug("Target server  close connection");
                    return true;
                }
            } else if (code == 417) {
                // hope this not happen
                log.debug("Server return 417 for expect 100");
                return true;
            }
        }

        @Nullable OutputStream responseStore = null;
        if (messageListener != null) {
            responseStore = messageListener.onHttpResponse(id, responseHeaders);
        }
        @Cleanup InputStream responseBody = getResponseBodyInput(dstIn, responseHeaders, requestLine.getMethod());
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


        if ("websocket".equals(upgrade) && code == 101) {
            // upgrade to websocket
            log.info("{} upgrade to websocket", url);
            int version = Strings.toInt(Strings.nullToEmpty(requestHeaders.getFirst("Sec-WebSocket-Version")), -1);
            //TODO: server may not support the version. in this case server will send supported versions, client should
            WebSocketHandler webSocketHandler = new WebSocketHandler();
            webSocketHandler.handle(srcIn, dstIn, host, url, messageListener);
            shouldClose = true;
        } else if ("h2c".equals(upgrade) && code == 101) {
            // http2 from http1 upgrade
            log.info("{} upgrade to http2", url);
            String http2Settings = requestHeaders.getFirst("HTTP2-Settings");
            Http2Handler handler = new Http2Handler();
            handler.handle(srcIn, dstIn, ssl, target, messageListener);
            shouldClose = true;
        }

        return shouldClose;
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
