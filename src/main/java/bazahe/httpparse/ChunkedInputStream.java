package bazahe.httpparse;

import bazahe.exception.HttpParserException;
import net.dongliu.commons.Strings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Liu Dong
 */
@ThreadSafe
class ChunkedInputStream extends FilterInputStream {
    private boolean closed = false;

    private boolean end = false;
    private long remainLen;
    private boolean firstChunk = true;

    protected ChunkedInputStream(InputStream in) {
        super(in);
    }

    private void nextChunkIfNecessary() throws IOException {
        if (!end && remainLen == 0) {
            nextChunk();
        }
    }

    private void nextChunk() throws IOException {
        if (!firstChunk) {
            String line = readLine();
            if (line == null) {
                throw new HttpParserException("chunked stream unexpected end");
            }
            if (!line.isEmpty()) {
                // chunk should end with empty line
                throw new HttpParserException("chunked not end with empty line");
            }
        }
        String line = readLine();
        if (line == null) {
            throw new HttpParserException("chunked stream unexpected end");
        }
        line = Strings.before(line, ";"); //ignore extensions
        long chunkLen = Long.parseLong(line, 16);
        if (chunkLen == 0) {
            end = true;
            // read trailers
            String tline;
            while ((tline = readLine()) != null) {
                // add one line
                if (tline.isEmpty()) {
                    break;
                }
            }
        }
        firstChunk = false;
        remainLen = chunkLen;
    }

    @Override
    public synchronized int read() throws IOException {
        checkClosed();
        nextChunkIfNecessary();
        if (end) {
            return -1;
        }
        int read = super.read();
        if (read == -1) {
            throw new HttpParserException("Unexpected end of chunked stream");
        }
        remainLen -= 1;
        return read;
    }

    @Override
    public synchronized int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        checkClosed();
        nextChunkIfNecessary();
        if (end) {
            return -1;
        }
        int toRead = (int) Math.min(this.remainLen, len);
        int read = super.read(b, off, toRead);
        if (read == -1) {
            throw new HttpParserException("Unexpected end of chunked stream");
        }
        this.remainLen -= read;
        return read;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        checkClosed();
        if (end) {
            return 0;
        }
        long toSkip = Math.min(this.remainLen, n);
        return super.skip(toSkip);
    }

    @Override
    public synchronized int available() throws IOException {
        checkClosed();
        return (int) Math.min(super.available(), this.remainLen);
    }

    @Override
    public synchronized void close() throws IOException {
        closed = true;
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Stream already closed");
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized boolean markSupported() {
        return false;
    }


    private final byte[] lineBuffer = new byte[65536];

    /**
     * Read ascii line, separated by '\r\n'
     *
     * @return the line. return null if reach end of input stream
     */
    @Nullable
    private String readLine() throws IOException {
        int count = 0;
        boolean flag = false;
        while (true) {
            int c = super.read();
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
}
