package bazahe.store;

import javax.annotation.concurrent.ThreadSafe;
import java.io.*;

/**
 * OutputStream impl for storing http body
 *
 * @author Liu Dong
 */
@ThreadSafe
public class HttpBodyStore extends OutputStream {
    private ByteArrayOutputStream bos;
    private boolean closed;

    public HttpBodyStore() {
        this.bos = new ByteArrayOutputStream();
    }

    @Override
    public synchronized void write(int b) throws IOException {
        delegate().write(b);
    }

    @Override
    public synchronized void write(byte[] b) throws IOException {
        delegate().write(b);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        delegate().write(b, off, len);
    }

    @Override
    public synchronized void flush() throws IOException {
        delegate().flush();
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
        try {
            this.bos.close();
        } finally {
            closed = true;
        }
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    private OutputStream delegate() {
        return bos;
    }

    public InputStream getInputStream() {
        if (!closed) {
            throw new IllegalStateException("Still writing");
        }
        return new ByteArrayInputStream(bos.toByteArray());
    }
}
