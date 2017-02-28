package bazahe.httpproxy;

import bazahe.httpparse.WebSocketInputStream;
import com.google.common.io.Closeables;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Handle web socket traffic
 *
 * @author Liu Dong
 */
@Log4j2
public class WebSocketHandler {
    @SneakyThrows
    public void handle(InputStream srcInput, InputStream dstInput, String host, String url,
                       @Nullable MessageListener messageListener) {
        Thread thread = new Thread(() -> {
            try {
                readWebSocket(dstInput, host, url, messageListener, false);
            } catch (Throwable t) {
                logger.warn("WebSocket down stream read webSocket error", t);
            }
        });
        thread.start();

        readWebSocket(srcInput, host, url, messageListener, true);
        thread.join();
    }

    private void readWebSocket(InputStream input, String host, String url, @Nullable MessageListener messageListener,
                               boolean isRequest) throws IOException {
        WebSocketInputStream srcWSInput = new WebSocketInputStream(input);

        while (true) {
            WebSocketInputStream.Frame frame = srcWSInput.readMessage();
            if (frame == null) {
                break;
            }
            String messageId = MessageIdGenerator.getInstance().nextId();
            @Nullable OutputStream outputStream;
            if (messageListener != null) {
                outputStream = messageListener.onWebSocket(messageId, host, url, frame.getOpcode(), isRequest);
            } else {
                outputStream = null;
            }
            try {
                srcWSInput.readMessageBody(outputStream);
            } finally {
                Closeables.close(outputStream, true);
            }
        }
    }
}
