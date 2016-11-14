package bazahe.httpproxy;

import bazahe.httpparse.RequestHeaders;
import bazahe.httpparse.ResponseHeaders;

import java.io.OutputStream;

/**
 * Listener to receive request data
 *
 * @author Liu Dong
 */
public interface HttpMessageListener {
    /**
     * Http request received
     *
     * @return a output stream for body to write
     */
    OutputStream onRequest(String id, RequestHeaders requestHeaders);

    /**
     * On response received
     *
     * @return a output stram for body to write
     */
    OutputStream onResponse(String id, ResponseHeaders responseHeaders);
}
