package bazahe.def;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    // timeout in seconds
    private int timeout;
    // path for keyStore file
//    private String keyStore;
//    private char[] keyStorePassword;

    private static final Path parentPath = Paths.get(System.getProperty("user.home"), ".bazahe");

    @SneakyThrows
    public static Path getParentPath() {
        if (!Files.exists(parentPath)) {
            Files.createDirectory(parentPath);
        }
        return parentPath;
    }

    public static Path getConfigPath() {
        return getParentPath().resolve(Paths.get("config"));
    }

    public static Path getDefaultKeyStorePath() {
        return getParentPath().resolve(Paths.get("bazahe.p12"));
    }

    public static ProxyConfig getDefault() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setHost("");
        proxyConfig.setPort(1024);
//        proxyConfig.setKeyStore("");
//        proxyConfig.setKeyStorePassword(new char[0]);
        proxyConfig.setTimeout(120);
        return proxyConfig;
    }
}
