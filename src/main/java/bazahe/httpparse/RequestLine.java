package bazahe.httpparse;

import bazahe.exception.HttpParserException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * The http request line
 *
 * @author Liu Dong
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class RequestLine {
    private final String method;
    private final String url;
    private final String version;

    @Override
    public String toString() {
        return String.format("RequestLine(%s %s %s)", method, url, version);
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
}
