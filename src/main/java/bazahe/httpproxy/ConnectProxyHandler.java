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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connect may proxy maybe: https(1.x, 2.0), webSocket, or even plain http1.x traffics
 * TODO: now just deal with https1.x
 *
 * @author Liu Dong
 */
@Log4j2
public class ConnectProxyHandler extends Http1xHandler {
    private final static ConcurrentHashMap<String, SSLContext> sslContextCache = new ConcurrentHashMap<>();

    private String target;
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
        this.target = target;
        log.debug("Receive connect request to {}", target);
        String host = Strings.before(target, ":");
        int port = Integer.parseInt(Strings.after(target, ":"));
        // just tell client ok..
        HttpOutputStream output = new HttpOutputStream(socket.getOutputStream());
        output.writeLine("HTTP/1.1 200 OK\r\n");
        output.flush();


        // upgrade socket to ssl socket
        SSLContext sslContext = sslContextCache.computeIfAbsent(host, this::createSSlContext);
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(socket, null, socket.getPort(), false);
        sslSocket.setUseClientMode(false);

        super.handle(new HttpInputStream(new BufferedInputStream(sslSocket.getInputStream())),
                new HttpOutputStream(sslSocket.getOutputStream()),
                httpMessageListener);
    }

    @Override
    protected String getUrl(String path) {
        return "https://" + target + path;
    }


    private KeyStore generateKeyStoreForSite(String domain) {
        return appKeyStoreGeneratorLazy.get().generateKeyStore(domain, 365, appKeyStorePassword);
    }

    @SneakyThrows
    private SSLContext createSSlContext(String domain) {
        KeyStore keyStore = generateKeyStoreForSite(domain);
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

}
