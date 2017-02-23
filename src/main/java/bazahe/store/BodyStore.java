package bazahe.store;

import bazahe.httpparse.ContentType;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.brotli.dec.BrotliInputStream;

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
public class BodyStore extends OutputStream implements Serializable {
    private static final long serialVersionUID = 1L;

    private ByteArrayOutputStreamEx bos;
    private OutputStream fos;
    private File file;
    private boolean closed;

    private static final int MAX_BUFFER_SIZE = 1024 * 1024;

    @Getter
    @Setter
    private volatile BodyStoreType type;
    @Getter
    @Setter
    private volatile Charset charset;
    @Getter
    private String contentEncoding;

    @Getter
    @Setter
    private transient boolean beautify;

    public BodyStore(@Nullable BodyStoreType type, @Nullable Charset charset,
                     @Nullable String contentEncoding) {
        this.type = type == null ? BodyStoreType.unknown : type;
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
        this.contentEncoding = Strings.nullToEmpty(contentEncoding);
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
                } else if ("x-www-form-urlencoded".equals(subType)) {
                    bodyStoreType = BodyStoreType.www_form;
                } else if ("css".equals(subType)) {
                    bodyStoreType = BodyStoreType.css;
                } else if ("javascript".equals(subType) || "x-javascript".equals(subType)) {
                    bodyStoreType = BodyStoreType.javascript;
                } else {
                    bodyStoreType = BodyStoreType.text;
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
            if (fos != null) {
                fos.close();
            }
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } finally {
                closed = true;
            }
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
                newTempFile();
                try (InputStream in = bos.asInputStream()) {
                    ByteStreams.copy(in, fos);
                }
            } catch (IOException e) {
                logger.error("Create tmp file for http body failed", e);
                //TODO: deal with this...
            }
            bos = null;
            return fos;
        }
        return bos;
    }

    private void newTempFile() throws IOException {
        file = File.createTempFile("bazahe_tmp", ".tmp");
        file.deleteOnExit();
        fos = new BufferedOutputStream(new FileOutputStream(file));
        bos.close();
    }

    /**
     * The len of data
     */
    public synchronized long getSize() {
        if (!closed) {
            throw new IllegalStateException("Still writing");
        }
        if (bos != null) {
            return bos.size();
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

    /**
     * Get content as input stream
     *
     * @return
     * @throws IOException
     */
    public synchronized InputStream getInputStream() throws IOException {
        InputStream input = inputStream();
        if (getSize() == 0) {
            return input;
        }

        try {
            if (Strings.isNullOrEmpty(contentEncoding) || contentEncoding.equalsIgnoreCase("identity")) {
                // do nothing
            } else if ("gzip".equalsIgnoreCase(contentEncoding)) {
                input = new GZIPInputStream(input);
            } else if ("deflate".equalsIgnoreCase(contentEncoding)) {
                input = new DeflaterInputStream(input);
            } else if (contentEncoding.equalsIgnoreCase("compress")) {
                input = new ZCompressorInputStream(input);
            } else if ("br".equalsIgnoreCase(contentEncoding)) {
                input = new BrotliInputStream(input);
            } else if ("lzma".equalsIgnoreCase(contentEncoding)) {
                input = new LZMACompressorInputStream(input);
            } else {
                logger.warn("unsupported content-encoding: {}", contentEncoding);
            }
        } catch (Throwable t) {
            logger.error("Decode stream failed, encoding: {}", contentEncoding, t);
            return input;
        }
        return input;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeBoolean(closed);
        out.writeObject(type);
        out.writeUTF(charset.name());
        out.writeUTF(contentEncoding);

        if (closed) {
            synchronized (this) {
                if (bos != null) {
                    out.writeInt(1);
                    out.writeLong(getSize());
                    try (InputStream in = bos.asInputStream()) {
                        ByteStreams.copy(in, out);
                    }
                } else if (file != null) {
                    out.writeInt(2);
                    out.writeLong(getSize());
                    try (InputStream in = new FileInputStream(file);
                         InputStream bin = new BufferedInputStream(in)) {
                        ByteStreams.copy(bin, out);
                    }
                } else {
                    throw new IllegalStateException();
                }
            }
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        closed = in.readBoolean();
        type = (BodyStoreType) in.readObject();
        charset = Charset.forName(in.readUTF());
        contentEncoding = in.readUTF();

        if (closed) {
            int store = in.readInt();
            long size = in.readLong();
            OutputStream out;
            if (store == 1) {
                bos = new ByteArrayOutputStreamEx();
                out = bos;
            } else if (store == 2) {
                newTempFile();
                out = fos;
            } else {
                throw new IllegalStateException();
            }
            copyWithSize(in, out, size);
        }
    }


    /**
     * Copy input stream to output stream, and close input
     */
    private static void copyWithSize(InputStream input, OutputStream output, long size) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        long remain = size;
        int toCopy = (int) Math.min(buffer.length, remain);
        int read;
        while ((read = input.read(buffer, 0, toCopy)) != -1) {
            output.write(buffer, 0, read);
            remain -= read;
            if (remain <= 0) {
                break;
            }
            toCopy = (int) Math.min(buffer.length, remain);
        }
    }
}
