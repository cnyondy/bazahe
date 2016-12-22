package bazahe.httpparse;

import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

/**
 * InputStream for websocket
 * //TODO extensions support
 *
 * @author Liu Dong
 */
@Log4j2
public class WebSocketInputStream extends DataInputStream {

    public WebSocketInputStream(InputStream inputStream) {
        super(inputStream);
    }

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
        int first;
        try {
            first = in.read();
        } catch (SocketException e) {
            // special hack for socket close. because we did not use control frames
            if (e.getMessage() != null && e.getMessage().contains("closed")) {
                return null;
            }
            throw e;
        }
        if (first == -1) {
            return null;
        }
        int second = in.read();
        boolean fin = BitUtils.getBit(first, 7) != 0;
        boolean rsv1 = BitUtils.getBit(first, 6) != 0;
        boolean rsv2 = BitUtils.getBit(first, 5) != 0;
        boolean rsv3 = BitUtils.getBit(first, 4) != 0;
        int opcode = first & 0xf;
        boolean mask = BitUtils.getBit(first, 7) != 0;
        long payloadLen = second & 0x7f;
        if (payloadLen <= 125) {

        } else if (payloadLen == 126) {
            payloadLen = readUnsigned2();
        } else if (payloadLen == 127) {
            payloadLen = readUnsigned8();
        }
        byte[] maskData = null;
        if (mask) {
            maskData = readExact(4);
        }
        logger.debug("fin: {}, opcode: {}, mask: {}, payloadLen: {}", fin, opcode, mask, payloadLen);
        return new Frame(fin, opcode, payloadLen, mask, maskData);
    }

    private class Frame {
        private static final int FRAME_CONTINUATION = 0;
        private static final int FRAME_TEXT = 1;
        private static final int FRAME_BINARY = 2;
        private static final int FRAME_CONNECTION_CLOSE = 8;
        private static final int FRAME_PING = 9;
        private static final int FRAME_PONG = 10;

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
                int read = in.read(buffer, 0, toRead);
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
                int read = in.read(buffer, 0, toRead);
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
