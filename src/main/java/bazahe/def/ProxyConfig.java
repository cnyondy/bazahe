package bazahe.def;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The proxy config infos
 *
 * @author Liu Dong
 */
@Getter
@Setter
@ToString
public class ProxyConfig {
    private String host;
    private int port;
    // timeout in milliseconds
    private int timeout;
    // path for keyStore file
    private String keyStore;

    public static ProxyConfig getDefault() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setHost("");
        proxyConfig.setPort(1024);
        proxyConfig.setKeyStore("certificates/bazahe.jks");
        proxyConfig.setTimeout(60000);
        return proxyConfig;
    }
}
