package bazahe.httpparse;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Liu Dong
 */
@Getter
public class ResponseHeaders extends Headers {
    private final String rawStatusLine;
    private final StatusLine statusLine;

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
}
