package bazahe.httpparse;

import bazahe.exception.HttpParserException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * The http request line
 *
 * @author Liu Dong
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class RequestLine implements Serializable {
    private static final long serialVersionUID = 1L;
    private String method;
    private String path;
    private String version;

    @Override
    public String toString() {
        return String.format("RequestLine(%s %s %s)", method, path, version);
    }

    public static RequestLine parse(String str) {
        String[] items = str.split(" ");
        if (items.length != 3) {
            throw new HttpParserException("Invalid http request line:" + str);
        }
        return new RequestLine(items[0], items[1], items[2]);
    }

    public boolean isHttp10() {
        return "HTTP/1.0".equalsIgnoreCase(version);
    }

    public boolean isHttp11() {
        return "HTTP/1.1".equalsIgnoreCase(version);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(method);
        out.writeUTF(path);
        out.writeUTF(version);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        method = in.readUTF();
        path = in.readUTF();
        version = in.readUTF();
    }
}
