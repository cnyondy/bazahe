package bazahe.httpparse;

import jdk.nashorn.internal.ir.annotations.Immutable;
import lombok.Getter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Liu Dong
 */
@Getter
@Immutable
public class ResponseHeaders extends Headers implements Serializable {
    private static final long serialVersionUID = 1L;
    private String rawStatusLine;
    private StatusLine statusLine;

    public ResponseHeaders(String rawStatusLine, List<String> rawHeaders) {
        super(rawHeaders);
        this.rawStatusLine = rawStatusLine;
        this.statusLine = StatusLine.parse(rawStatusLine);
    }

    @Override
    public String toString() {
        return "ResponseHeaders(statusLine=" + rawStatusLine + ", headers=" + super.toString() + ")";
    }

    @Override
    public List<String> toRawLines() {
        List<String> rawLines = new ArrayList<>(getRawHeaders().size() + 1);
        rawLines.add(rawStatusLine);
        rawLines.addAll(getRawHeaders());
        return rawLines;
    }

    /**
     * If this request/response has no body.
     */
    public boolean hasBody() {
        /*
         * For response, a message-body is explicitly forbidden in responses to HEAD requests
         * a message-body is explicitly forbidden in 1xx (informational), 204 (no content), and 304 (not modified)
         * responses
         */
        int code = statusLine.getCode();
        return !(code >= 100 && code < 200 || code == 204 || code == 304);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(rawStatusLine);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        rawStatusLine = in.readUTF();
        statusLine = StatusLine.parse(rawStatusLine);
    }
}
