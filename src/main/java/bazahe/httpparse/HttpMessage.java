package bazahe.httpparse;

import bazahe.store.BodyStore;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @author Liu Dong
 */
@Getter
public class HttpMessage extends Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private RequestHeaders requestHeaders;
    private BodyStore requestBody;
    @Nullable
    @Setter
    private volatile ResponseHeaders responseHeaders;
    @Setter
    private volatile BodyStore responseBody;

    public HttpMessage(String id, String host, String url, RequestHeaders requestHeaders, BodyStore requestBody) {
        super(id, host, url);
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
    }

    @Override
    public String getDisplay() {
        return getUrl();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(requestHeaders);
        out.writeObject(requestBody);
        out.writeObject(responseHeaders);
        out.writeObject(responseBody);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        requestHeaders = (RequestHeaders) in.readObject();
        requestBody = (BodyStore) in.readObject();
        responseHeaders = (ResponseHeaders) in.readObject();
        responseBody = (BodyStore) in.readObject();
    }
}
