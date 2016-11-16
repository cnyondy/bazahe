package bazahe.httpparse;

import bazahe.exception.HttpParserException;
import net.dongliu.commons.collection.Lists;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Input stream for http parser
 *
 * @author Liu Dong
 */
public class HttpInputStream extends InputStream {
    private volatile boolean closed;
    private final InputStream inputStream;

    public HttpInputStream(InputStream input) {
        this.inputStream = input;
    }

    private final byte[] lineBuffer = new byte[65536];

    /**
     * Read ascii line, separated by '\r\n'
     *
     * @return the line. return null if reach end of input stream
     */
    @Nullable
    public String readLine() throws IOException {
        if (this.line != null) {
            String line = this.line;
            this.line = null;
            return line;
        }
        int count = 0;
        boolean flag = false;
        while (true) {
            int c = read();
            if (c == -1) {
                break;
            }
            if (c == '\r') {
                flag = true;
            } else if (flag) {
                if (c == '\n') {
                    break;
                } else {
                    flag = false;
                }
            }
            lineBuffer[count] = (byte) c;
            count++;
        }

        if (count == 0) {
            return null;
        }
        return new String(lineBuffer, 0, count - 1, StandardCharsets.US_ASCII);
    }

    @Nullable
    private volatile String line;

    /**
     * Hacker for putback request line... do not relay on this method
     */
    public void putBackLine(String line) {
        if (this.line != null) {
            throw new IllegalStateException();
        }
        this.line = line;
    }

    /**
     * Read http request header.
     *
     * @return null if reach end of input stream
     */
    @Nullable
    public RequestHeaders readRequestHeaders() throws IOException {
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
    public ResponseHeaders readResponseHeaders() throws IOException {
        String line = readLine();
        if (line == null) {
            return null;
        }
        List<String> rawHeaders = readHeaders();
        return new ResponseHeaders(line, rawHeaders);
    }

    public List<String> readHeaders() throws IOException {
        String line;
        List<String> rawHeaders = Lists.create();
        while (true) {
            line = readLine();
            if (line == null) {
                // non-completed header
                throw new HttpParserException("Http header read not finished when reach the end of stream");
            }
            if (line.isEmpty()) {
                break;
            }
            rawHeaders.add(line);
        }
        return rawHeaders;
    }


    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }

    @Override
    public void close() throws IOException {
        super.close();
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    /**
     * Get http request body as input stream.
     *
     * @param len the content-len. if len == -1 means chunked
     */
    public InputStream getBody(long len) {
        if (len >= 0) {
            return new FixLenInputStream(this, len);
        } else {
            return new ChunkedInputStream(this);
        }
    }
}
