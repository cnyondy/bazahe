package bazahe.store;

import bazahe.httpparse.ContentType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.BinarySize;
import net.dongliu.commons.RefValues;
import net.dongliu.commons.io.ByteArrayOutputStreamEx;
import net.dongliu.commons.io.Closeables;
import net.dongliu.commons.io.InputOutputs;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

    private static final int MAX_BUFFER_SIZE = (int) BinarySize.megabyte(1);

    @Getter
    @Setter
    private volatile BodyStoreType type;
    @Getter
    @Setter
    private volatile Charset charset;
    @Nullable
    @Getter
    private final String contentEncoding;

    public BodyStore(@Nullable BodyStoreType type, @Nullable Charset charset,
                     @Nullable String contentEncoding) {
        this.type = RefValues.ifNullThen(type, BodyStoreType.unknown);
        this.charset = RefValues.ifNullThen(charset, StandardCharsets.UTF_8);
        this.contentEncoding = contentEncoding;
        this.bos = new ByteArrayOutputStreamEx();
    }


    public static BodyStore of(@Nullable ContentType contentType, @Nullable String contentEncoding) {
        if (contentType == null) {
            return new BodyStore(null, null, contentEncoding);
        } else {
            BodyStoreType bodyStoreType;
            String type = contentType.getMimeType().getType();
            String subType = contentType.getMimeType().getSubType();
            if (contentType.isImage()) {
                if ("png".equals(subType)) {
                    bodyStoreType = BodyStoreType.png;
                } else if ("jpeg".equals(subType)) {
                    bodyStoreType = BodyStoreType.jpeg;
                } else if ("gif".equals(subType)) {
                    bodyStoreType = BodyStoreType.gif;
                } else if ("bmp".equals(subType)) {
                    bodyStoreType = BodyStoreType.bmp;
                } else if ("x-icon".equals(subType)) {
                    bodyStoreType = BodyStoreType.icon;
                } else {
                    bodyStoreType = BodyStoreType.otherImage;
                }
            } else if (contentType.isText()) {
                if ("json".equals(subType)) {
                    bodyStoreType = BodyStoreType.json;
                } else if ("html".equals(subType)) {
                    bodyStoreType = BodyStoreType.html;
                } else if ("xml".equals(subType)) {
                    bodyStoreType = BodyStoreType.xml;
                } else if ("www-form-encoded".equals(subType)) {
                    bodyStoreType = BodyStoreType.formEncoded;
                } else if ("css".equals(subType)) {
                    bodyStoreType = BodyStoreType.css;
                } else if ("javascript".equals(subType)) {
                    bodyStoreType = BodyStoreType.javascript;
                } else {
                    bodyStoreType = BodyStoreType.plainText;
                }
            } else {
                bodyStoreType = BodyStoreType.binary;
            }
            return new BodyStore(bodyStoreType, contentType.getCharset(), contentEncoding);
        }
    }

    @Override
    public synchronized void write(int b) throws IOException {
        getOutput().write(b);
    }

    @Override
    public synchronized void write(byte[] b) throws IOException {
        getOutput().write(b);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        getOutput().write(b, off, len);
    }

    @Override
    public synchronized void flush() throws IOException {
        getOutput().flush();
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

    private OutputStream getOutput() {
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
