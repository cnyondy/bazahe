package bazahe.httpparse;

import lombok.Getter;
import net.dongliu.commons.Joiner;
import net.dongliu.commons.collection.Lists;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Commons for request headers and response headers
 *
 * @author Liu Dong
 */
@Getter
public abstract class Headers {
    private final List<Header> headers;
    private final List<String> rawHeaders;

    public Headers(List<String> rawHeaders) {
        this(rawHeaders, Lists.map(rawHeaders, Header::parse));
    }

    public Headers(List<String> rawHeaders, List<Header> headers) {
        this.rawHeaders = rawHeaders;
        this.headers = headers;
    }

    @Override
    public String toString() {
        return Joiner.of("\n", "Headers(", ")").join(rawHeaders);
    }

    /**
     * Get first header value by name
     *
     * @return null if not found
     */
    @Nullable
    public String getFirst(String name) {
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                return header.getValue();
            }
        }
        return null;
    }

    /**
     * If is chunked http body
     */
    public boolean chunked() {
        return "chunked".equalsIgnoreCase(getFirst("Content-Encoding"));
    }

    /**
     * The content-len set in header.
     *
     * @return -1 if not found
     */
    public long contentLen() {
        String value = getFirst("Content-Length");
        if (value == null) {
            return -1;
        }
        return Long.parseLong(value);
    }

    /**
     * If should close connection after this msg.
     * Note: Only for http 1.1, 1.0 not supported
     */
    public boolean shouldClose() {
        String connection = getFirst("Connection");
        if ("close".equalsIgnoreCase(connection)) {
            return true;
        }
        return false;
    }

    /**
     * Get content type from http header
     */
    @Nullable
    public ContentType contentType() {
        String contentType = getFirst("Content-Type");
        if (contentType == null) {
            return null;
        }
        return ContentType.parse(contentType);
    }

    @Nullable
    public String contentEncoding() {
        return getFirst("Content-Encoding");
    }

    /**
     * Convert to raw lines string
     */
    public abstract List<String> toRawLines();

}
