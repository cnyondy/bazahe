package bazahe;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author Liu Dong
 */
@Getter
@Setter
@ToString
public class SecondaryProxy implements Serializable {
    private static final long serialVersionUID = 7257755061846443485L;
    private String type = "socks";
    private String host = "";
    private int port;
    private String user = "";
    private String passsword = "";
    private boolean use;

}
