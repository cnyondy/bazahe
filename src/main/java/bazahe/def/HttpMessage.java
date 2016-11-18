package bazahe.def;

import bazahe.httpparse.RequestHeaders;
import bazahe.httpparse.ResponseHeaders;
import bazahe.store.HttpBodyStore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nullable;

/**
 * @author Liu Dong
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HttpMessage {
    private volatile String id;
    private volatile RequestHeaders requestHeaders;
    private volatile HttpBodyStore requestBody;
    @Nullable
    private volatile ResponseHeaders responseHeaders;
    private volatile HttpBodyStore responseBody;
}
