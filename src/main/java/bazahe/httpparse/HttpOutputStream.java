package bazahe.httpparse;

import lombok.val;
import net.dongliu.commons.io.InputOutputs;

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
public class HttpOutputStream extends OutputStream {
    private final OutputStream output;

    public HttpOutputStream(OutputStream output) {
        this.output = output;
    }

    /**
     * Output http response headers
     */
    public void writeResponseHeaders(ResponseHeaders headers) throws IOException {
        writeLine(headers.getRawStatusLine());
        writeRawHeaders(headers);
    }

    /**
     * Output http request headers
     */
    public void writeRequestHeaders(RequestHeaders headers) throws IOException {
        writeLine(headers.getRawRequestLine());
        writeRawHeaders(headers);
    }

    public void writeHeaders(List<Header> respHeaders) throws IOException {
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
    public void writeRawHeaders(Headers headers) throws IOException {
        for (String header : headers.getRawHeaders()) {
            writeLine(header);
        }
        output.write('\r');
        output.write('\n');
    }

    public void writeRawHeaders(List<String> lines) throws IOException {
        for (String header : lines) {
            writeLine(header);
        }
        output.write('\r');
        output.write('\n');
    }

    public void writeLine(String line) throws IOException {
        output.write(line.getBytes(StandardCharsets.US_ASCII));
        output.write('\r');
        output.write('\n');
    }

    @Override
    public void write(int b) throws IOException {
        output.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        output.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        output.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        output.flush();
    }

    @Override
    public void close() throws IOException {
        output.close();
    }

    /**
     * Write http body to output stream
     */
    public void writeBody(long len, InputStream input) throws IOException {
        if (len > 0) {
            InputOutputs.copy(input, output);
            return;
        }
        int chunkSize = 1024 * 8;
        byte[] buffer = new byte[chunkSize];
        while (true) {
            int read = InputOutputs.readExact(input, buffer);
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
