package bazahe.def;

import bazahe.httpparse.RequestHeaders;
import bazahe.httpparse.ResponseHeaders;
import bazahe.store.HttpBodyStore;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

/**
 * @author Liu Dong
 */
@Getter
@Setter
public class HttpMessage {
    private final String id;
    private final String url;
    private final RequestHeaders requestHeaders;
    private final HttpBodyStore requestBody;
    @Nullable
    private volatile ResponseHeaders responseHeaders;
    private volatile HttpBodyStore responseBody;

    public HttpMessage(String id, String url, RequestHeaders requestHeaders, HttpBodyStore requestBody) {
        this.id = id;
        this.url = url;
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
    }
}
