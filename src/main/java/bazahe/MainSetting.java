package bazahe;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The proxy mainSetting infos
 *
 * @author Liu Dong
 */
@Getter
@Setter
@ToString
public class MainSetting implements Serializable {
    private static final long serialVersionUID = -1828819182428842928L;
    private String host;
    private int port;
    // timeout in seconds
    private int timeout;
    // path for keyStore file
    private boolean useCustomKeyStore;
    private String keyStore;
    private char[] keyStorePassword;

    private static final Path parentPath = Paths.get(System.getProperty("user.home"), ".bazahe");

    @SneakyThrows
    public static Path getParentPath() {
        if (!Files.exists(parentPath)) {
            Files.createDirectory(parentPath);
        }
        return parentPath;
    }

    /**
     * Get mainSetting file path
     */
    public static Path configPath() {
        return getParentPath().resolve(Paths.get("config"));
    }

    /**
     * The default key store file path
     */
    private static Path defaultKeyStorePath() {
        return getParentPath().resolve(Paths.get("bazahe.p12"));
    }

    /**
     * The default key store password
     */
    private static char[] defaultKeyStorePassword() {
        return new char[]{'1', '2', '3', '4', '5', '6'};
    }

    public String usedKeyStore() {
        if (useCustomKeyStore) {
            return keyStore;
        }
        return defaultKeyStorePath().toString();
    }

    public char[] usedPassword() {
        if (useCustomKeyStore) {
            return keyStorePassword;
        }
        return defaultKeyStorePassword();
    }

    public static MainSetting getDefault() {
        MainSetting mainSetting = new MainSetting();
        mainSetting.setHost("");
        mainSetting.setPort(1024);
        mainSetting.setKeyStore("");
        mainSetting.setKeyStorePassword(new char[0]);
        mainSetting.setTimeout(120);
        return mainSetting;
    }
}
