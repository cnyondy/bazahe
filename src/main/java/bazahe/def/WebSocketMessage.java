package bazahe.def;

import bazahe.store.BodyStore;
import lombok.Getter;
import lombok.Setter;

/**
 * WebSocket message
 *
 * @author Liu Dong
 */
public class WebSocketMessage extends Message {
    // type: 1 txt
    // type: 2 binary
    @Getter
    private final int type;
    @Getter
    private final boolean request;
    @Getter
    @Setter
    private volatile BodyStore bodyStore;

    public WebSocketMessage(String id, String host, String url, int type, boolean request) {
        super(id, host, url);
        this.type = type;
        this.request = request;
    }

    @Override
    public String getDisplay() {
        return getUrl();
    }
}
