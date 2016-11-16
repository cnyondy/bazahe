package bazahe.httpproxy;

import lombok.extern.log4j.Log4j2;

/**
 * Non-connect http handler
 *
 * @author Liu Dong
 */
@Log4j2
public class CommonProxyHandler extends Http1xHandler {
    @Override
    protected String getUrl(String path) {
        return path;
    }
}
