package bazahe.httpparse;

import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.io.InputOutputs;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * InputStream for websocket
 * //TODO extensions support
 *
 * @author Liu Dong
 */
@Log4j2
public class WebSocketInputStream extends InputStream {

    private final InputStream inputStream;

    public WebSocketInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    private static final int FRAME_CONTINUATION = 0;
    private static final int FRAME_TEXT = 1;
    private static final int FRAME_BINARY = 2;
    private static final int FRAME_CONNECTION_CLOSE = 8;
    private static final int FRAME_PING = 9;
    private static final int FRAME_PONG = 10;

    private Frame lastFrame;

    /**
     * Read an entire webSocket data message
     *
     * @return the type(opcode) of this message. return -1 if reach the end of stream
     */
    public int readMessage() throws IOException {
        Frame frame = readDataFrame();
        lastFrame = frame;
        if (frame == null) {
            return -1;
        }

        return frame.opcode;
    }

    /**
     * Called after readMessage
     */
    public void readMessageBody(@Nullable OutputStream outputStream) throws IOException {
        if (outputStream != null) {
            lastFrame.copyTo(outputStream);
        } else {
            lastFrame.consumeAll();
        }
        if (lastFrame.fin) {
            lastFrame = null;
            return;
        }

        while (true) {
            Frame f = readDataFrame();
            if (f == null) {
                throw new IOException("WebSocket Message not terminated");
            }
            if (outputStream != null) {
                f.copyTo(outputStream);
            } else {
                f.consumeAll();
            }
            if (f.fin) {
                break;
            }
        }
    }

    /**
     * Read next data frame
     */
    private Frame readDataFrame() throws IOException {
        while (true) {
            Frame frame = readFrameHeader();
            if (frame == null) {
                return null;
            }
            if (!frame.isControlFrame()) {
                return frame;
            }
            frame.consumeAll();
        }
    }

    /**
     * Read an webSocket frame
     */
    @Nullable
    private Frame readFrameHeader() throws IOException {
        int first = inputStream.read();
        if (first == -1) {
            return null;
        }
        int second = inputStream.read();
        boolean fin = ((first >> 7) & 1) != 0;
        boolean rsv1 = ((first >> 6) & 1) != 0;
        boolean rsv2 = ((first >> 5) & 1) != 0;
        boolean rsv3 = ((first >> 4) & 1) != 0;
        int opcode = first & 0xf;
        boolean mask = ((second >> 7) & 1) != 0;
        long payloadLen = second & 0x7f;
        if (payloadLen <= 125) {

        } else if (payloadLen == 126) {
            payloadLen = (inputStream.read() << 8) + inputStream.read();
        } else if (payloadLen == 127) {
            payloadLen = ((long) inputStream.read() << 56)
                    + ((long) inputStream.read() << 48)
                    + ((long) inputStream.read() << 40)
                    + ((long) inputStream.read() << 32)
                    + ((long) inputStream.read() << 24)
                    + ((long) inputStream.read() << 16)
                    + ((long) inputStream.read() << 8)
                    + ((long) inputStream.read());
        }
        byte[] maskData = null;
        if (mask) {
            maskData = new byte[4];
            InputOutputs.readExact(inputStream, maskData);
        }
        log.debug("fin: {}, opcode: {}, mask: {}, payloadLen: {}", fin, opcode, mask, payloadLen);
        return new Frame(fin, opcode, payloadLen, mask, maskData);
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }

    private class Frame {
        private final boolean fin;
        private final int opcode;
        private final long payloadLen;
        private final boolean mask;
        @Nullable
        private final byte[] maskData;

        private Frame(boolean fin, int opcode, long payloadLen, boolean mask, @Nullable byte[] maskData) {
            this.fin = fin;
            this.opcode = opcode;
            this.payloadLen = payloadLen;
            this.mask = mask;
            this.maskData = maskData;
        }

        boolean isControlFrame() {
            return ((opcode >> 3) & 1) == 1;
        }

        void consumeAll() throws IOException {
            int bufferSize = (int) Math.min(payloadLen, 1024 * 8);
            byte[] buffer = new byte[bufferSize];
            long total = 0;
            while (true) {
                int toRead = (int) Math.min(payloadLen - total, bufferSize);
                int read = inputStream.read(buffer, 0, toRead);
                if (read == -1) {
                    break;
                }
                total += read;
                if (total >= payloadLen) {
                    break;
                }
            }
        }

        void copyTo(OutputStream os) throws IOException {
            int bufferSize = (int) Math.min(payloadLen, 1024 * 8);
            byte[] buffer = new byte[bufferSize];
            long total = 0;
            while (true) {
                int toRead = (int) Math.min(payloadLen - total, bufferSize);
                int read = inputStream.read(buffer, 0, toRead);
                if (read == -1) {
                    break;
                }
                if (maskData != null) {
                    unmask(buffer, read, maskData, total);
                }
                total += read;
                os.write(buffer, 0, read);
                if (total >= payloadLen) {
                    break;
                }
            }
        }

        private void unmask(byte[] buffer, int read, byte[] mask, long total) {
            for (int i = 0; i < read; i++) {
                buffer[i] = (byte) (buffer[i] ^ mask[(int) ((i + total) % mask.length)]);
            }
        }
    }
}
