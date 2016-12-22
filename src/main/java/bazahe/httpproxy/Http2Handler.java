package bazahe.httpproxy;

import bazahe.httpparse.HttpInputStream;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import java.io.InputStream;

/**
 * Handle http2 traffic
 *
 * @author Liu Dong
 */
@Log4j2
public class Http2Handler {

    @SneakyThrows
    public void handle(HttpInputStream srcInput, HttpInputStream destInput, boolean ssl, String target,
                       @Nullable MessageListener messageListener) {

        // header compression http://httpwg.org/specs/rfc7541.html
        // http2 http://httpwg.org/specs/rfc7540.html

        // read client connection preface
        // read server connection preface
        tunnel(srcInput, destInput);
    }


    private void tunnel(InputStream input1, InputStream input2) throws InterruptedException {
        Thread thread = new Thread(() -> {
            try {
                IOUtils.consumeAll(input1);
            } catch (Throwable t) {
                logger.warn("tunnel traffic failed", t);
            }
        });
        thread.start();
        IOUtils.consumeAll(input2);
        thread.join();
    }
}
