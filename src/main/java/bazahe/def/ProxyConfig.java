package bazahe.def;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * The proxy config infos
 *
 * @author Liu Dong
 */
@Getter
@Setter
@ToString
public class ProxyConfig implements Serializable {
    private String host;
    private int port;
    // timeout in milliseconds
    private int timeout;
    // path for keyStore file
    private String keyStore;
    private char[] keyStorePassword;

    public static ProxyConfig getDefault() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setHost("");
        proxyConfig.setPort(1024);
        proxyConfig.setKeyStore("");
        proxyConfig.setKeyStorePassword(new char[0]);
        proxyConfig.setTimeout(120000);
        return proxyConfig;
    }
}
