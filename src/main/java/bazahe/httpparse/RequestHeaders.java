package bazahe.httpparse;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Http request headers
 *
 * @author Liu Dong
 */
@Getter
public class RequestHeaders extends Headers {
    private final String rawRequestLine;
    private final RequestLine requestLine;

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
        /*
         * For request, an entity-body is explicitly forbidden in TRACE requests only, all other request types are
         * unrestricted
         */
        return !"TRACE".equalsIgnoreCase(requestLine.getMethod())
                && !"GET".equalsIgnoreCase(requestLine.getMethod());
    }

}
