package bazahe.httpparse;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream for websocket
 *
 * @author Liu Dong
 */
@Log4j2
public class Http2InputStream extends DataInputStream {

    private CompressionContext compressionContext;

    public Http2InputStream(InputStream inputStream) {
        super(inputStream);
    }

    /**
     * Read an webSocket frame
     */
    @Nullable
    private Frame readFrameHeader() throws IOException {
        int payloadLen = readUnsigned3();
        int type = read();
        int flag = read();
        int streamIdentifier = ((read() & 0x7fffffff) << 24) + readUnsigned3();
        return new Frame(payloadLen, type, flag, streamIdentifier);
    }

    @Getter
    private class Frame {
        private final int payloadLen;
        private final int type;
        private final int flag;
        private final int streamIdentifier;

        public Frame(int payloadLen, int type, int flag, int streamIdentifier) {
            this.payloadLen = payloadLen;
            this.type = type;
            this.flag = flag;
            this.streamIdentifier = streamIdentifier;
        }
    }

    private static class CompressionContext {

    }
}
