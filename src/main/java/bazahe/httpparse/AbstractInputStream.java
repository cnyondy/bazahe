package bazahe.httpparse;

import com.google.common.io.ByteStreams;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Input streams, that delegate to another input stream, and have helper methods, no pre-read-buffer, yet support
 * mark and reset operation.
 *
 * @author Liu Dong
 */
public class AbstractInputStream extends InputStream {

    private InputStream input;

    // only for mark & reset
    private byte[] buffer;
    private int pos = 0;
    private int count = 0;
    private boolean mark;

    protected AbstractInputStream(InputStream input) {
        this.input = input;
    }

    /**
     * Read exactly size bytes
     */
    public byte[] readExact(int size) throws IOException {
        byte[] buffer = new byte[size];
        int read = ByteStreams.read(this, buffer, 0, size);
        if (read != size) {
            throw new EOFException("Unexpected end of stream");
        }
        return buffer;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (mark) {
            int read = input.read(b, off, len);
            if (read <= 0) {
                return read;
            }
            System.arraycopy(b, off, buffer, count, read);
            count += read;
            return read;
        }
        if (pos < count) {
            int toRead = Math.min(len, count - pos);
            System.arraycopy(buffer, pos, b, off, toRead);
            pos += toRead;
            return toRead;
        }
        return input.read(b, off, len);
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        if (mark) {
            int toSkip = (int) Math.min(buffer.length - count, n);
            int read = input.read(buffer, count, toSkip);
            if (read == -1) {
                return 0;
            }
            count += read;
            return read;
        }
        if (pos < count) {
            int toSkip = (int) Math.min(n, count - pos);
            pos += toSkip;
            return toSkip;
        }
        return input.skip(n);
    }

    @Override
    public synchronized int available() throws IOException {
        if (!mark && pos < count) {
            return count - pos;
        }
        return input.available();
    }

    @Override
    public synchronized void close() throws IOException {
        buffer = null;
        pos = count = 0;
        mark = false;
        input.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        mark = true;
        pos = count = 0;
        if (buffer == null) {
            buffer = new byte[readlimit];
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        mark = false;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public synchronized int read() throws IOException {
        return _read();
    }


    public synchronized int _read() throws IOException {
        if (mark) {
            int b = input.read();
            if (b == -1) {
                return b;
            }
            buffer[count++] = (byte) b;
            return b;
        }

        if (pos < count) {
            return Byte.toUnsignedInt(buffer[pos++]);
        }
        return input.read();
    }

    public void enableBuffered() {
        if (!(input instanceof BufferedInputStream)) {
            input = new BufferedInputStream(input);
        }
    }

    @Nullable
    private byte[] lineBuffer;

    /**
     * Read ascii line, separated by '\r\n'
     */
    @Nullable
    public synchronized String readLine() throws IOException {
        if (lineBuffer == null) {
            lineBuffer = new byte[256];
        }
        int count = 0;
        boolean flag = false;
        while (true) {
            int c = _read();
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
            if (count >= lineBuffer.length) {
                byte[] newBuffer = new byte[lineBuffer.length * 2];
                System.arraycopy(lineBuffer, 0, newBuffer, 0, lineBuffer.length);
                lineBuffer = newBuffer;
            }
            lineBuffer[count] = (byte) c;
            count++;
        }

        if (count == 0) {
            return null;
        }
        String line = new String(lineBuffer, 0, count - 1, StandardCharsets.US_ASCII);
        if (lineBuffer.length > 8192) {
            lineBuffer = null;
        }
        return line;
    }

}
