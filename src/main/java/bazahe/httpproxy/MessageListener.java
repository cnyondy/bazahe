package bazahe.httpproxy;

import bazahe.httpparse.RequestHeaders;
import bazahe.httpparse.ResponseHeaders;

import java.io.OutputStream;

/**
 * Listener to receive request data
 *
 * @author Liu Dong
 */
public interface MessageListener {
    /**
     * Http request received
     *
     * @return a output stream for body to write
     */
    OutputStream onHttpRequest(String id, String url, RequestHeaders requestHeaders);

    /**
     * On response received
     *
     * @return a output stram for body to write
     */
    OutputStream onHttpResponse(String id, ResponseHeaders responseHeaders);

    /**
     * One receive a websocket message
     */
    OutputStream onWebSocket(String id, String url, int type, boolean request);
}
