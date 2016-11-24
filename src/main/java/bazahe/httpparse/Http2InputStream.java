package bazahe.httpparse;

import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream for websocket
 * //TODO extensions support
 *
 * @author Liu Dong
 */
@Log4j2
public class Http2InputStream extends InputStream {

    private CompressionContext compressionContext;

    private final InputStream inputStream;

    public Http2InputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Read an webSocket frame
     */
    @Nullable
    private Frame readFrameHeader() throws IOException {
        int payloadLen = (inputStream.read() << 16)
                + (inputStream.read() << 8)
                + (inputStream.read());
        int type = inputStream.read();
        int fllag = inputStream.read();
        int streamIdentifier = ((inputStream.read() & 0x7fffffff) << 24)
                + (inputStream.read() << 16)
                + (inputStream.read() << 8)
                + (inputStream.read());
        return null;
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
    public void close() throws IOException {
        inputStream.close();
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

    private class Frame {

    }

    private static class CompressionContext {

    }
}
