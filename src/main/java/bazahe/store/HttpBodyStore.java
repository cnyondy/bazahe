package bazahe.store;

import bazahe.httpparse.ContentType;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

/**
 * OutputStream impl for storing http body
 *
 * @author Liu Dong
 */
@ThreadSafe
@Log4j2
public class HttpBodyStore extends OutputStream {
    private ByteArrayOutputStream bos;
    private boolean closed;

    @Nullable
    @Getter
    private final ContentType contentType;
    @Nullable
    @Getter
    private final String contentEncoding;

    public HttpBodyStore(@Nullable ContentType contentType, @Nullable String contentEncoding) {
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
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

    /**
     * The len of data
     */
    public long getSize() {
        if (!closed) {
            throw new IllegalStateException("Still writing");
        }
        return bos.toByteArray().length;
    }

    private InputStream inputStream() {
        if (!closed) {
            throw new IllegalStateException("Still writing");
        }
        return new ByteArrayInputStream(bos.toByteArray());
    }

    public InputStream getInputStream() throws IOException {
        InputStream input = inputStream();
        if (getSize() == 0) {
            return input;
        }

        try {
            if ("gzip".equalsIgnoreCase(contentEncoding)) {
                input = new GZIPInputStream(input);
            } else if ("deflate".equalsIgnoreCase(contentEncoding)) {
                input = new DeflaterInputStream(input);
            }
        } catch (Throwable t) {
            log.error("Decode stream failed, encoding: {}", contentEncoding, t);
            return input;
        }
        return input;
    }
}
