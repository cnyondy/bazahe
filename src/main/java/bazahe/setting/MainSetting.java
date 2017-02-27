package bazahe.setting;

import lombok.Value;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The proxy mainSetting infos
 *
 * @author Liu Dong
 */
@Value
public class MainSetting implements Serializable {
    private static final long serialVersionUID = -1828819182428842928L;
    private final String host;
    private final int port;
    // timeout in seconds
    private final int timeout;

    /**
     * Get mainSetting file path
     */
    public static Path configPath() {
        return Settings.getParentPath().resolve(Paths.get("config"));
    }


    public static MainSetting getDefault() {
        return new MainSetting("", 6080, 120);
    }
}
