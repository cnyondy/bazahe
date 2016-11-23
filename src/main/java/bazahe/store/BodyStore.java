package bazahe.store;

import bazahe.httpparse.ContentType;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.BinarySize;
import net.dongliu.commons.io.ByteArrayOutputStreamEx;
import net.dongliu.commons.io.Closeables;
import net.dongliu.commons.io.InputOutputs;

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
public class BodyStore extends OutputStream {
    private ByteArrayOutputStreamEx bos;
    private OutputStream fos;
    private File file;
    private boolean closed;

    private static final int MAX_BUFFER_SIZE = (int) BinarySize.kilobyte(512);

    @Nullable
    @Getter
    private final ContentType contentType;
    @Nullable
    @Getter
    private final String contentEncoding;

    public BodyStore(@Nullable ContentType contentType, @Nullable String contentEncoding) {
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
        this.bos = new ByteArrayOutputStreamEx();
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
            Closeables.closeQuietly(fos, bos);
        } finally {
            closed = true;
        }
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    private OutputStream delegate() {
        if (fos != null) {
            return fos;
        }
        if (bos.size() > MAX_BUFFER_SIZE) {
            try {
                file = File.createTempFile("bazahe_tmp", ".tmp");
                file.deleteOnExit();
                fos = new BufferedOutputStream(new FileOutputStream(file));
                bos.close();
                InputOutputs.copy(bos.asInputStream(), fos);
            } catch (IOException e) {
                log.error("Create tmp file for http body failed", e);
                //TODO: deal with this...
            }
            bos = null;
            return fos;
        }
        return bos;
    }

    /**
     * The len of data
     */
    public synchronized long getSize() {
        if (!closed) {
            throw new IllegalStateException("Still writing");
        }
        if (bos != null) {
            return bos.toByteArray().length;
        } else if (file != null) {
            return file.length();
        } else {
            throw new RuntimeException();
        }

    }

    private InputStream inputStream() throws FileNotFoundException {
        if (!closed) {
            throw new IllegalStateException("Still writing");
        }
        if (bos != null) {
            return bos.asInputStream();
        } else if (file != null) {
            return new BufferedInputStream(new FileInputStream(file));
        } else {
            // should not happen
            throw new RuntimeException();
        }
    }

    public synchronized InputStream getInputStream() throws IOException {
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
