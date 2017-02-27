package bazahe.setting;

import lombok.Value;

import java.io.Serializable;

/**
 * @author Liu Dong
 */
@Value
public class ProxySetting implements Serializable {
    private static final long serialVersionUID = 7257755061846443485L;
    private String type;
    private String host;
    private int port;
    private String user;
    private String password;
    private boolean use;

    public static ProxySetting getDefault() {
        return new ProxySetting("socks5", "", 0, "", "", false);
    }

}
