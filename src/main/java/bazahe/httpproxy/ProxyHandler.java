package bazahe.httpproxy;

import bazahe.httpparse.HttpInputStream;
import bazahe.httpparse.HttpOutputStream;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Socket;

/**
 * Proxy handler
 *
 * @author Liu Dong
 */
public interface ProxyHandler {

    void handle(Socket socket, HttpInputStream input, HttpOutputStream output,
                @Nullable HttpMessageListener httpMessageListener) throws IOException;
}
