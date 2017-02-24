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
import java.util.Objects;

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
    private volatile KeyStoreSetting keyStoreSetting;
    @Getter
    private volatile SecondaryProxySetting secondaryProxySetting;
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
    public void setKeyStoreSetting(KeyStoreSetting setting) {
        Objects.requireNonNull(setting);
        Path path = Paths.get(setting.usedKeyStore());
        if (sslContextManager == null || !Files.isSameFile(path, Paths.get(this.sslContextManager.getKeyStorePath()))) {
//            updateMessage("Load new key store file");
            this.sslContextManager = new SSLContextManager(setting.usedKeyStore(),
                    setting.usedPassword().toCharArray());
        }
        this.keyStoreSetting = setting;
    }

    @SneakyThrows
    public void setMainSetting(MainSetting mainSetting) {
        this.mainSetting = Objects.requireNonNull(mainSetting);
    }

    public void setSecondaryProxySetting(SecondaryProxySetting secondaryProxySetting) {
        Objects.requireNonNull(secondaryProxySetting);
        if (secondaryProxySetting.isUse()) {
            dialer = (host, port) -> {
                if (secondaryProxySetting.getType().equals("socks5")) {
                    proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(secondaryProxySetting.getHost(),
                            secondaryProxySetting.getPort()));
                } else if (secondaryProxySetting.getType().equals("http")) {
                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(secondaryProxySetting.getHost(),
                            secondaryProxySetting.getPort()));
                } else {
                    throw new RuntimeException("unsupport proxy type: " + secondaryProxySetting.getType());
                }
                Socket socket = new Socket(proxy);
                socket.connect(InetSocketAddress.createUnresolved(host, port));
                return socket;
            };
        } else {
            dialer = Socket::new;
            proxy = Proxy.NO_PROXY;
        }


        if (secondaryProxySetting.isUse() && (this.secondaryProxySetting == null ||
                !secondaryProxySetting.getUser().equals(this.secondaryProxySetting.getUser()) ||
                !secondaryProxySetting.getPassword().equals(this.secondaryProxySetting.getPassword()))) {
            if (secondaryProxySetting.getUser().isEmpty()) {
                Authenticator.setDefault(null);
            } else {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(secondaryProxySetting.getUser(),
                                secondaryProxySetting.getPassword().toCharArray());
                    }
                });
            }
        }
        this.secondaryProxySetting = secondaryProxySetting;
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
        return factory.createSocket(createSocket(host, port), secondaryProxySetting.getHost(),
                secondaryProxySetting.getPort(), true);
    }
}
