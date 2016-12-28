package bazahe.httpproxy;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Socket;

/**
 * Proxy handler
 *
 * @author Liu Dong
 */
public interface ProxyHandler {

    void handle(Socket serverSocket, String rawRequestLine, @Nullable MessageListener messageListener) throws IOException;
}
