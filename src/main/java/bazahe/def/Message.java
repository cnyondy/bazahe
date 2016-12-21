package bazahe.def;

import lombok.Getter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Message parent class
 *
 * @author Liu Dong
 */
public abstract class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    @Getter
    private String id;
    @Getter
    private String host;
    @Getter
    private String url;

    protected Message(String id, String host, String url) {
        this.id = id;
        this.host = host;
        this.url = url;
    }

    /**
     * For show in abstract
     */
    public abstract String getDisplay();

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(id);
        out.writeUTF(host);
        out.writeUTF(url);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        id = in.readUTF();
        host = in.readUTF();
        url = in.readUTF();
    }
}
