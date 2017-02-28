package bazahe.httpproxy;

import com.google.common.io.Closeables;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * @author Liu Dong
 */
class TeeInputStream extends FilterInputStream {
    private final OutputStream output;

    protected TeeInputStream(InputStream in, OutputStream output) {
        super(Objects.requireNonNull(in));
        this.output = Objects.requireNonNull(output);
    }

    @Override
    public int read() throws IOException {
        int v = in.read();
        if (v != -1) {
            output.write(v);
        }
        return v;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int n = in.read(b);
        if (n > 0) {
            output.write(b, 0, n);
        }
        return n;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = in.read(b, off, len);
        if (n > 0) {
            output.write(b, off, n);
        }
        return n;
    }

    @Override
    public long skip(long n) throws IOException {
        byte[] buffer = new byte[(int) Math.min(1024, n)];
        long count = 0;
        while (count < n) {
            int read = read(buffer);
            if (read == -1) {
                break;
            }
            count += read;
        }
        return count;
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

    @Override
    public void close() throws IOException {
        Closeables.close(output, true);
        in.close();
    }
}
