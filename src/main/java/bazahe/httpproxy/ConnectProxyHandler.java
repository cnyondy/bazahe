package bazahe.httpproxy;

import bazahe.httpparse.HttpInputStream;
import bazahe.httpparse.HttpOutputStream;
import bazahe.httpparse.RequestHeaders;
import bazahe.httpparse.RequestLine;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.Strings;
import net.dongliu.commons.concurrent.Lazy;

import javax.annotation.Nullable;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * Connect may proxy maybe: https(1.x, 2.0), webSocket, or even plain http1.x traffics
 * TODO: now just deal with https1.x
 *
 * @author Liu Dong
 */
@Log4j2
public class ConnectProxyHandler extends Http1xHandler {
    private final Lazy<SSLContext> sslContextLazy = Lazy.create(this::createSSlContext);

    private String target;
    private final String keyStorePath;

    public ConnectProxyHandler(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    @Override
    public void handle(Socket socket, HttpInputStream input, HttpOutputStream output,
                       @Nullable HttpMessageListener httpMessageListener) throws IOException {
        RequestHeaders headers = input.readRequestHeaders();
        if (headers == null) {
            //should not happen because we already check it
            return;
        }
        RequestLine requestLine = headers.getRequestLine();
        String target = requestLine.getUrl();
        this.target = target;
        log.debug("Receive connect request to {}", target);
        String host = Strings.before(target, ":");
        int port = Integer.parseInt(Strings.after(target, ":"));
        // just tell client ok..
        output.writeLine("HTTP/1.1 200 OK\r\n");
        output.flush();


        // upgrade socket to ssl socket
        SSLSocketFactory sslSocketFactory = sslContextLazy.get().getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(socket, null, socket.getPort(), false);
        sslSocket.setUseClientMode(false);

        super.handle(sslSocket, new HttpInputStream(sslSocket.getInputStream()),
                new HttpOutputStream(sslSocket.getOutputStream()), httpMessageListener);
    }

    @Override
    protected String getUrl(String path) {
        return "https://" + target + path;
    }


    @SneakyThrows
    private SSLContext createSSlContext() {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream inputStream = new FileInputStream(keyStorePath)) {
            keyStore.load(inputStream, "123456".toCharArray());
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "123456".toCharArray());
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

}
