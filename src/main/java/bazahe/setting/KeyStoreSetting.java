package bazahe.setting;

import lombok.Value;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Key store setting
 *
 * @author Liu Dong
 */
@Value
public class KeyStoreSetting implements Serializable {
    private static final long serialVersionUID = -8001899659204205513L;
    // path for keyStore file
    private String keyStore;
    private String keyStorePassword;
    private boolean useCustom;

    /**
     * The default key store file path
     */
    private static Path defaultKeyStorePath() {
        return Settings.getParentPath().resolve(Paths.get("bazahe.p12"));
    }

    /**
     * The default key store password
     */
    private static String defaultKeyStorePassword() {
        return "123456";
    }

    public String usedKeyStore() {
        if (useCustom) {
            return keyStore;
        }
        return defaultKeyStorePath().toString();
    }

    public String usedPassword() {
        if (useCustom) {
            return keyStorePassword;
        }
        return defaultKeyStorePassword();
    }

    public static KeyStoreSetting getDefault() {
        return new KeyStoreSetting("", "", false);
    }
}
