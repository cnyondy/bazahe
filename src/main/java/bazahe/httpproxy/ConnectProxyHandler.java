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

    @Override
    public void handle(Socket socket, String rawRequestLine, @Nullable MessageListener messageListener)
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
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TLSInputStream tlsIn = new TLSInputStream(new ObservableInputStream(socket.getInputStream(), bos));
        TLSInputStream.TLSPlaintextHeader tlsPlaintextHeader = tlsIn.readPlaintextHeader();

        boolean ssl;
        Socket serverSocket;
        Socket clientSocket;
        if (tlsPlaintextHeader.isValidHandShake()) {
            // read entire ssl client hello message, to serach ALPN extension. Java8 not support ALPN now
            // see https://tools.ietf.org/html/rfc5246
//            int length = tlsPlaintextHeader.getLength();
//            byte[] data = new byte[length];
//            int read2 = InputOutputs.readExact(input, data);
//            TLSInputStream.HandShakeMessage<?> message = TLSInputStream.readHandShakeMessage(new
// ByteArrayInputStream(data));


            Socket wrappedSocket = new WrappedSocket(socket, bos.toByteArray());
            SSLContext sslContext = SSLContextManager.getInstance().createSSlContext(host);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(wrappedSocket, null, socket.getPort(),
                    false);
            sslSocket.setUseClientMode(false);
            serverSocket = sslSocket;
            ssl = true;
            SSLContext clientSSlContext = SSLUtils.createClientSSlContext();
            SSLSocketFactory factory = clientSSlContext.getSocketFactory();
            clientSocket = factory.createSocket(host, port);
            //TODO: http2 established by  ALPN https://tools.ietf.org/html/rfc7301, by send protocol "h2"
//            handleHttp2();
        } else {
            serverSocket = new WrappedSocket(socket, bos.toByteArray());
            ssl = false;
            clientSocket = new Socket(host, port);
        }

        try {
            handle(serverSocket, clientSocket, ssl, target, messageListener);
        } finally {
            Closeables.closeQuietly(clientSocket);
        }

    }

    private void handle(Socket serverSocket, Socket clientSocket, Boolean ssl, String target,
                        @Nullable MessageListener messageListener) throws IOException {
        HttpInputStream srcInput = new HttpInputStream(new BufferedInputStream(
                new ObservableInputStream(serverSocket.getInputStream(), clientSocket.getOutputStream())));
        HttpInputStream destInput = new HttpInputStream(new BufferedInputStream(
                new ObservableInputStream(clientSocket.getInputStream(), serverSocket.getOutputStream())));


        while (true) {
            boolean finish = handleOneRequest(srcInput, destInput, ssl, target, messageListener);
            if (finish) {
                break;
            }
        }

    }

    @SneakyThrows
    private boolean handleOneRequest(HttpInputStream srcInput, HttpInputStream destInput,
                                     boolean ssl, String target,
                                     @Nullable MessageListener messageListener) {
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
        String upgrade = requestHeaders.getFirst("Upgrade");
        String protocol;
        if ("websocket".equals(upgrade)) {
            protocol = ssl ? "wss" : "ws";
        } else {
            protocol = ssl ? "https" : "http";
        }
        String url = protocol + "://" + target + requestLine.getUrl();
        String host = AddressUtils.getHostFromTarget(target);

        @Nullable OutputStream requestStore = null;
        if (messageListener != null) {
            requestStore = messageListener.onHttpRequest(id, host, url, requestHeaders);
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
        if (messageListener != null) {
            responseStore = messageListener.onHttpResponse(id, responseHeaders);
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

        int code = responseHeaders.getStatusLine().getCode();

        if ("websocket".equals(upgrade) && code == 101) {
            // upgrade to websocket
            log.info("{} upgrade to websocket", url);
            int version = Strings.toInt(Strings.nullToEmpty(requestHeaders.getFirst("Sec-WebSocket-Version")), -1);
            //TODO: server may not support the version. in this case server will send supported versions, client should
            WebSocketHandler webSocketHandler = new WebSocketHandler();
            webSocketHandler.handle(srcInput, destInput, host, url, messageListener);
            shouldClose = true;
        } else if ("h2c".equals(upgrade) && code == 101) {
            // http2 from http1 upgrade
            log.info("{} upgrade to http2", url);
            String http2Settings = requestHeaders.getFirst("HTTP2-Settings");
            Http2Handler handler = new Http2Handler();
            handler.handle(srcInput, destInput, protocol, target, messageListener);
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
