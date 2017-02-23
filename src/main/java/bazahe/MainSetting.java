package bazahe;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
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

    /**
     * Get mainSetting file path
     */
    public static Path configPath() {
        return Constants.getParentPath().resolve(Paths.get("config"));
    }


    public static MainSetting getDefault() {
        MainSetting mainSetting = new MainSetting();
        mainSetting.setHost("");
        mainSetting.setPort(1024);
        mainSetting.setTimeout(120);
        return mainSetting;
    }
}
