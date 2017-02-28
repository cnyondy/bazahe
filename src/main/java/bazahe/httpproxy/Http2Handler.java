package bazahe.httpproxy;

import bazahe.httpparse.HttpInputStream;
import bazahe.utils.ByteStreamUtils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;

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
        ByteStreamUtils.tunnel(srcInput, destInput);
    }


}
