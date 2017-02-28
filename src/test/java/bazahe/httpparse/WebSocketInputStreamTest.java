package bazahe.httpparse;

import lombok.Cleanup;
import lombok.SneakyThrows;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.*;

/**
 * @author Liu Dong
 */
public class WebSocketInputStreamTest {
    @Test
    public void readMessage() throws Exception {
        @Cleanup WebSocketInputStream input = new WebSocketInputStream(getClass().getResourceAsStream("/websocket.1"));
        WebSocketInputStream.Frame frame = input.readMessage();
        assertNotNull(frame);
        assertEquals(1, frame.getOpcode());
        assertTrue(frame.isFin());
        assertTrue(frame.isMask());
        String body = bodyToString(frame);
        assertTrue(body.contains("channel"));
    }


    @Test
    public void readMessage2() throws Exception {
        @Cleanup WebSocketInputStream input = new WebSocketInputStream(getClass().getResourceAsStream("/websocket.2"));
        WebSocketInputStream.Frame frame = input.readMessage();
        assertNotNull(frame);
        assertEquals(1, frame.getOpcode());
        assertTrue(frame.isFin());
        assertFalse(frame.isMask());
        String body = bodyToString(frame);
        assertTrue(body.startsWith("[{\"ext\":{\"timesync\":"));
    }


    @SneakyThrows
    private String bodyToString(WebSocketInputStream.Frame frame) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        frame.copyTo(bos);
        return bos.toString("UTF-8");
    }

}