package bazahe.httpparse;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Input stream for http parser.
 *
 * @author Liu Dong
 */
@ThreadSafe
public class HttpInputStream extends AbstractInputStream {
    private volatile boolean closed;

    public HttpInputStream(InputStream input) {
        super(input);
    }

    /**
     * Read http request header.
     *
     * @return null if reach end of input stream
     */
    @Nullable
    public synchronized RequestHeaders readRequestHeaders() throws IOException {
        String line = readLine();
        if (line == null) {
            return null;
        }
        List<String> rawHeaders = readHeaders();
        return new RequestHeaders(line, rawHeaders);
    }

    /**
     * Read http response header.
     *
     * @return null if reach end of input stream
     */
    @Nullable
    public synchronized ResponseHeaders readResponseHeaders() throws IOException {
        String line = readLine();
        if (line == null) {
            return null;
        }
        List<String> rawHeaders = readHeaders();
        return new ResponseHeaders(line, rawHeaders);
    }

    public synchronized List<String> readHeaders() throws IOException {
        String line;
        List<String> rawHeaders = new ArrayList<>();
        while (true) {
            line = readLine();
            if (line == null) {
                // non-completed header
                throw new EOFException("Http header read not finished when reach the end of stream");
            }
            if (line.isEmpty()) {
                break;
            }
            rawHeaders.add(line);
        }
        return rawHeaders;
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
        closed = true;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Get http request body as input stream.
     *
     * @param len the content-len. if len == -1 means chunked
     */
    public synchronized InputStream getBody(long len) {
        if (len >= 0) {
            return new FixLenInputStream(this, len);
        } else {
            return new ChunkedInputStream(this);
        }
    }
}
