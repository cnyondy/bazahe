package bazahe.httpparse;

import lombok.Cleanup;
import lombok.SneakyThrows;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Liu Dong
 */
public class HttpInputStreamTest {
    @Test
    public void readLine() throws Exception {
        @Cleanup HttpInputStream inputStream = new HttpInputStream(
                new ByteArrayInputStream("test\r123\n\r\n\r\n".getBytes()));
        String line = inputStream.readLine();
        assertEquals("test\r123\n", line);

        line = inputStream.readLine();
        assertEquals("", line);

        line = inputStream.readLine();
        assertNull(line);
    }



    private void mock() {
        RequestHeaders requestHeaders = mockRequest();
        ResponseHeaders responseHeaders = mockResponse();
    }

    @SneakyThrows
    private RequestHeaders mockRequest() {
        @Cleanup InputStream input = getClass().getResourceAsStream("/req.txt");
        @Cleanup HttpInputStream httpInputStream = new HttpInputStream(input);
        return httpInputStream.readRequestHeaders();
    }

    @SneakyThrows
    private ResponseHeaders mockResponse() {
        @Cleanup InputStream input = getClass().getResourceAsStream("/resp.txt");
        @Cleanup HttpInputStream httpInputStream = new HttpInputStream(input);
        return httpInputStream.readResponseHeaders();
    }
}