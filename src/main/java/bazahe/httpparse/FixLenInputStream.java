package bazahe.httpparse;

import bazahe.exception.HttpParserException;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Liu Dong
 */
class FixLenInputStream extends FilterInputStream {
    private final long capacity;
    private long count;

    private boolean closed = false;

    protected FixLenInputStream(InputStream in, long capacity) {
        super(in);
        this.capacity = capacity;
    }

    @Override
    public int read() throws IOException {
        checkClosed();
        if (count >= capacity) {
            return -1;
        }
        int read = super.read();
        if (read == -1) {
            throw new HttpParserException("Unexpected end of fix len stream");
        }
        count += 1;
        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkClosed();
        if (count >= this.capacity) {
            return -1;
        }
        int toRead = (int) Math.min(this.capacity - count, len);
        int read = super.read(b, off, toRead);
        if (read == -1) {
            throw new HttpParserException("Unexpected end of fix len stream");
        }
        this.count += read;
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        checkClosed();
        if (count >= this.capacity) {
            return 0;
        }
        long toSkip = Math.min(this.capacity - count, n);
        return super.skip(toSkip);
    }

    @Override
    public int available() throws IOException {
        checkClosed();
        return (int) Math.min(super.available(), capacity - count);
    }

    @Override
    public void close() throws IOException {
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
    public boolean markSupported() {
        return false;
    }
}
