package bazahe.httpparse;

import lombok.Getter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Http request headers
 *
 * @author Liu Dong
 */
@Getter
public class RequestHeaders extends Headers implements Serializable {
    private static final long serialVersionUID = 1L;
    private String rawRequestLine;
    private RequestLine requestLine;

    public RequestHeaders(String rawRequestLine, List<String> rawHeaders) {
        super(rawHeaders);
        this.rawRequestLine = rawRequestLine;
        this.requestLine = RequestLine.parse(rawRequestLine);
    }

    @Override
    public String toString() {
        return "RequestHeaders(requestLine=" + rawRequestLine + ", headers=" + super.toString() + ")";
    }

    @Override
    public List<String> toRawLines() {
        List<String> rawLines = new ArrayList<>(getRawHeaders().size() + 1);
        rawLines.add(rawRequestLine);
        rawLines.addAll(getRawHeaders());
        return rawLines;
    }

    /**
     * If this request/response has body.
     */
    public boolean hasBody() {
        return !"TRACE".equalsIgnoreCase(requestLine.getMethod())
                && !"GET".equalsIgnoreCase(requestLine.getMethod())
                && !"OPTIONS".equalsIgnoreCase(requestLine.getMethod());
    }


    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(rawRequestLine);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        rawRequestLine = in.readUTF();
        this.requestLine = RequestLine.parse(rawRequestLine);
    }
}
