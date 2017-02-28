package bazahe.httpproxy;

import bazahe.httpparse.HttpInputStream;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Socket;

/**
 * Proxy handler
 *
 * @author Liu Dong
 */
public interface Handler {

    void handle(Socket serverSocket, HttpInputStream input, @Nullable MessageListener messageListener)
            throws IOException;
}
