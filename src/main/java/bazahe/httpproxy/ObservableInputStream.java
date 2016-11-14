package bazahe.httpproxy;

import net.dongliu.commons.io.Closeables;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * @author Liu Dong
 */
class ObservableInputStream extends FilterInputStream {
    private final OutputStream output;

    protected ObservableInputStream(InputStream in, OutputStream output) {
        super(Objects.requireNonNull(in));
        this.output = Objects.requireNonNull(output);
    }

    @Override
    public int read() throws IOException {
        int v = super.read();
        if (v != -1) {
            output.write(v);
        }
        return v;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int n = super.read(b);
        if (n > 0) {
            output.write(b, 0, n);
        }
        return n;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n > 0) {
            output.write(b, off, n);
        }
        return n;
    }

    @Override
    public void close() throws IOException {
        Closeables.closeQuietly(output);
        super.close();
    }
}
