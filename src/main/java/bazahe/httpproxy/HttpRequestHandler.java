package bazahe.httpproxy;

import bazahe.httpparse.*;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Handler to server http request
 *
 * @author Liu Dong
 */
public class HttpRequestHandler implements Handler {
    private final SSLContextManager sslContextManager;

    public HttpRequestHandler(SSLContextManager sslContextManager) {
        this.sslContextManager = sslContextManager;
    }

    @Override
    public void handle(Socket serverSocket, String rawRequestLine,
                       @Nullable MessageListener messageListener) throws IOException {
        HttpInputStream input = new HttpInputStream(serverSocket.getInputStream());
        HttpOutputStream out = new HttpOutputStream(serverSocket.getOutputStream());
        input.putBackLine(rawRequestLine);
        RequestHeaders headers = input.readRequestHeaders();
        if (headers == null) {
            return;
        }
        RequestLine requestLine = headers.getRequestLine();
        String method = requestLine.getMethod();
        String path = requestLine.getPath();
        if (!method.equals("GET")) {
            return;
        }
        switch (path) {
            case "/":
                sendIndexHtml(out);
                break;
            case "/bazahe.pem":
                sendPem(out);
                break;
            case "/bazahe.crt":
                sendCrt(out);
                break;
            case "/bazahe.cer":
                sendCer(out);
                break;
            default:
        }
    }

    private void sendCer(HttpOutputStream out) throws IOException {
        AppKeyStoreGenerator appKeyStoreGenerator = sslContextManager.getAppKeyStoreGenerator();
        byte[] data = appKeyStoreGenerator.exportCACertificate(false);
        sendResponse(out, "application/x-x509-ca-cert", data);
    }

    private void sendCrt(HttpOutputStream out) throws IOException {
        AppKeyStoreGenerator appKeyStoreGenerator = sslContextManager.getAppKeyStoreGenerator();
        byte[] data = appKeyStoreGenerator.exportCACertificate(false);
        sendResponse(out, "application/x-x509-ca-cert", data);
    }

    private void sendPem(HttpOutputStream out) throws IOException {
        AppKeyStoreGenerator appKeyStoreGenerator = sslContextManager.getAppKeyStoreGenerator();
        byte[] data = appKeyStoreGenerator.exportCACertificate(true);
        sendResponse(out, "application/x-pem-file", data);
    }

    private void sendIndexHtml(HttpOutputStream out) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/www/html/index.html")) {
            byte[] data = ByteStreams.toByteArray(in);
            sendResponse(out, "text/html; charset=utf-8", data);
        }
    }

    private void sendResponse(HttpOutputStream out, String contentType, byte[] body) throws IOException {
        out.writeLine("HTTP/1.1 200 OK");
        out.writeHeaders(ImmutableList.of(
                new Header("Content-Type", contentType),
                new Header("Content-Length", String.valueOf(body.length)),
                new Header("Connection", "close")
        ));
        out.write(body);
        out.flush();
    }
}
