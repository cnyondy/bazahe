package bazahe.def;

import bazahe.httpparse.RequestHeaders;
import bazahe.httpparse.ResponseHeaders;
import bazahe.store.BodyStore;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

/**
 * @author Liu Dong
 */
@Getter
@Setter
public class HttpMessage extends Message {
    private final RequestHeaders requestHeaders;
    private final BodyStore requestBody;
    @Nullable
    private volatile ResponseHeaders responseHeaders;
    private volatile BodyStore responseBody;

    public HttpMessage(String id, String url, RequestHeaders requestHeaders, BodyStore requestBody) {
        super(id, url);
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
    }

    @Override
    public String getDisplay() {
        return requestHeaders.getRequestLine().getMethod() + " " + getUrl();
    }
}
