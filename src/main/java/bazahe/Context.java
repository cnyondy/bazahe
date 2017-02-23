package bazahe;

import bazahe.httpproxy.SSLContextManager;
import bazahe.httpproxy.SSLUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Context and settings
 *
 * @author Liu Dong
 */
@Log4j2
public class Context {
    @Getter
    private volatile MainSetting mainSetting;
    @Getter
    private volatile SecondaryProxy secondaryProxy;
    @Getter
    private volatile SSLContextManager sslContextManager;
    @Getter
    private volatile Dialer dialer;
    @Getter
    private volatile Proxy proxy = Proxy.NO_PROXY;


    private static Context instance = new Context();

    private Context() {
    }

    @SneakyThrows
    public void setMainSettingAndRefreshSSLContextManager(MainSetting mainSetting) {
        Path path = Paths.get(mainSetting.usedKeyStore());
        if (sslContextManager == null || !Files.isSameFile(path, Paths.get(this.sslContextManager.getKeyStorePath()))) {
//            updateMessage("Load new key store file");
            this.sslContextManager = new SSLContextManager(mainSetting.usedKeyStore(), mainSetting.usedPassword());
        }
        this.mainSetting = mainSetting;
    }

    public void setSecondaryProxyAndRefreshSocketFactory(SecondaryProxy secondaryProxy) {
        if (secondaryProxy.isUse()) {
            dialer = (host, port) -> {
                if (secondaryProxy.getType().equals("socks5")) {
                    proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(secondaryProxy.getHost(),
                            secondaryProxy.getPort()));
                } else if (secondaryProxy.getType().equals("http")) {
                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(secondaryProxy.getHost(),
                            secondaryProxy.getPort()));
                } else {
                    throw new RuntimeException("unsupport proxy type: " + secondaryProxy.getType());
                }
                Socket socket = new Socket(proxy);
                socket.connect(InetSocketAddress.createUnresolved(host, port));
                return socket;
            };
        } else {
            dialer = Socket::new;
            proxy = Proxy.NO_PROXY;
        }


        if (secondaryProxy.isUse() && (this.secondaryProxy == null ||
                !secondaryProxy.getUser().equals(this.secondaryProxy.getUser()) ||
                !secondaryProxy.getPasssword().equals(this.secondaryProxy.getPasssword()))) {
            if (secondaryProxy.getUser().isEmpty()) {
                Authenticator.setDefault(null);
            } else {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(secondaryProxy.getUser(),
                                secondaryProxy.getPasssword().toCharArray());
                    }
                });
            }
        }
        this.secondaryProxy = secondaryProxy;
    }

    public static Context getInstance() {
        return instance;
    }

    /**
     * create plain socket
     */
    @SneakyThrows
    public Socket createSocket(String host, int port) {
        return dialer.dial(host, port);
    }

    /**
     * create trust-all ssl socket
     */
    @SneakyThrows
    public Socket createSSLSocket(String host, int port) {
        SSLContext clientSSlContext = SSLUtils.createClientSSlContext();
        SSLSocketFactory factory = clientSSlContext.getSocketFactory();
        return factory.createSocket(createSocket(host, port), secondaryProxy.getHost(), secondaryProxy.getPort(), true);
    }
}
