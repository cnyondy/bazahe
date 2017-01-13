package bazahe.httpparse;

import com.google.common.io.ByteStreams;
import lombok.val;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Output stream for http
 *
 * @author Liu Dong
 */
@ThreadSafe
public class HttpOutputStream extends OutputStream {
    private final OutputStream output;
    private volatile boolean closed;

    public HttpOutputStream(OutputStream output) {
        this.output = output;
    }

    /**
     * Output http response headers
     */
    public synchronized void writeResponseHeaders(ResponseHeaders headers) throws IOException {
        writeLine(headers.getRawStatusLine());
        writeRawHeaders(headers);
    }

    /**
     * Output http request headers
     */
    public synchronized void writeRequestHeaders(RequestHeaders headers) throws IOException {
        writeLine(headers.getRawRequestLine());
        writeRawHeaders(headers);
    }

    public synchronized void writeHeaders(List<Header> respHeaders) throws IOException {
        for (val header : respHeaders) {
            output.write(header.getKey().getBytes(StandardCharsets.US_ASCII));
            output.write(':');
            output.write(' ');
            output.write(header.getValue().getBytes(StandardCharsets.US_ASCII));
            output.write('\r');
            output.write('\n');
        }
        output.write('\r');
        output.write('\n');
    }

    /**
     * Write one http header
     */
    public synchronized void writeRawHeaders(Headers headers) throws IOException {
        for (String header : headers.getRawHeaders()) {
            writeLine(header);
        }
        output.write('\r');
        output.write('\n');
    }

    public synchronized void writeRawHeaders(List<String> lines) throws IOException {
        for (String header : lines) {
            writeLine(header);
        }
        output.write('\r');
        output.write('\n');
    }

    public synchronized void writeLine(String line) throws IOException {
        output.write(line.getBytes(StandardCharsets.US_ASCII));
        output.write('\r');
        output.write('\n');
    }

    @Override
    public synchronized void write(int b) throws IOException {
        output.write(b);
    }

    @Override
    public synchronized void write(byte[] b) throws IOException {
        output.write(b);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        output.write(b, off, len);
    }

    @Override
    public synchronized void flush() throws IOException {
        output.flush();
    }

    @Override
    public synchronized void close() throws IOException {
        output.close();
        closed = true;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Write http body to output stream
     */
    public synchronized void writeBody(long len, InputStream input) throws IOException {
        if (len > 0) {
            ByteStreams.copy(input, output);
            return;
        }
        int chunkSize = 1024 * 8;
        byte[] buffer = new byte[chunkSize];
        while (true) {
            int read = ByteStreams.read(input, buffer, 0, buffer.length);
            if (read > 0) {
                String s = Integer.toHexString(read);
                output.write(s.getBytes(StandardCharsets.US_ASCII));
                output.write('\r');
                output.write('\n');
                output.write(buffer, 0, read);
                output.write('\r');
                output.write('\n');
                output.flush();
            }
            if (read < chunkSize) {
                // last chunk
                output.write('0');
                output.write('\r');
                output.write('\n');
                // todo: trailers
                output.write('\r');
                output.write('\n');
                break;
            }
        }
    }
}
