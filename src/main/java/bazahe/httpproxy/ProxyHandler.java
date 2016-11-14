package bazahe.httpproxy;

import bazahe.httpparse.HttpInputStream;
import bazahe.httpparse.HttpOutputStream;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Proxy handler
 *
 * @author Liu Dong
 */
public interface ProxyHandler {

    void handle(String rawRequestLine, HttpInputStream input, HttpOutputStream output,
                @Nullable HttpMessageListener httpMessageListener) throws IOException;
}
