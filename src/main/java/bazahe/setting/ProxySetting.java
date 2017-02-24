package bazahe.setting;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.val;

import java.io.Serializable;

/**
 * @author Liu Dong
 */
@Getter
@Setter
@ToString
public class ProxySetting implements Serializable {
    private static final long serialVersionUID = 7257755061846443485L;
    private String type;
    private String host;
    private int port;
    private String user;
    private String password;
    private boolean use;

    public static ProxySetting getDefault() {
        val setting = new ProxySetting();
        setting.setType("socks5");
        setting.setHost("");
        setting.setPort(0);
        setting.setUser("");
        setting.setPassword("");
        setting.setUse(false);
        return setting;
    }

}
