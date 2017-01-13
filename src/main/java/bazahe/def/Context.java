package bazahe.def;

import bazahe.httpproxy.SSLContextManager;
import lombok.Getter;
import lombok.Setter;

/**
 * Context and settings
 *
 * @author Liu Dong
 */
public class Context {
    @Getter
    @Setter
    private volatile ProxyConfig config;
    @Getter
    @Setter
    private volatile SSLContextManager sslContextManager;

    private static Context instance = new Context();

    private Context() {
    }

    public static Context getInstance() {
        return instance;
    }

}
