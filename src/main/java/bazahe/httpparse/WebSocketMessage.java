package bazahe.httpparse;

import bazahe.httpparse.Message;
import bazahe.store.BodyStore;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * WebSocket message
 *
 * @author Liu Dong
 */
public class WebSocketMessage extends Message implements Serializable {
    private static final long serialVersionUID = 1L;
    // type: 1 txt
    // type: 2 binary
    @Getter
    private int type;
    @Getter
    private boolean request;
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

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(type);
        out.writeBoolean(request);
        out.writeObject(bodyStore);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        type = in.readInt();
        request = in.readBoolean();
        bodyStore = (BodyStore) in.readObject();
    }
}
