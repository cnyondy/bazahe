package bazahe;

import bazahe.httpproxy.SSLContextManager;
import bazahe.httpproxy.SSLUtils;
import bazahe.setting.KeyStoreSetting;
import bazahe.setting.MainSetting;
import bazahe.setting.ProxySetting;
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
    private volatile ProxySetting proxySetting;
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

    public void setProxySetting(ProxySetting proxySetting) {
        Objects.requireNonNull(proxySetting);
        if (proxySetting.isUse()) {
            dialer = (host, port) -> {
                InetSocketAddress proxyAddress = new InetSocketAddress(proxySetting.getHost(), proxySetting.getPort());
                if (proxySetting.getType().equals("socks5")) {
                    proxy = new Proxy(Proxy.Type.SOCKS, proxyAddress);
                } else if (proxySetting.getType().equals("http")) {
                    proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
                } else {
                    throw new RuntimeException("unsupported proxy type: " + proxySetting.getType());
                }
                Socket socket = new Socket(proxy);
                socket.connect(InetSocketAddress.createUnresolved(host, port));
                return socket;
            };
        } else {
            dialer = Socket::new;
            proxy = Proxy.NO_PROXY;
        }


        if (proxySetting.isUse() && (this.proxySetting == null ||
                !proxySetting.getUser().equals(this.proxySetting.getUser()) ||
                !proxySetting.getPassword().equals(this.proxySetting.getPassword()))) {
            if (proxySetting.getUser().isEmpty()) {
                Authenticator.setDefault(null);
            } else {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxySetting.getUser(),
                                proxySetting.getPassword().toCharArray());
                    }
                });
            }
        }
        this.proxySetting = proxySetting;
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
        return factory.createSocket(createSocket(host, port), proxySetting.getHost(), proxySetting.getPort(), true);
    }
}
