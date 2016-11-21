package bazahe.httpproxy;

import bazahe.httpparse.HttpInputStream;
import bazahe.httpparse.HttpOutputStream;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Non-connect http handler
 *
 * @author Liu Dong
 */
@Log4j2
public class CommonProxyHandler extends Http1xHandler {

    @Override
    public void handle(Socket socket, String rawRequestLine, @Nullable HttpMessageListener httpMessageListener) throws IOException {
        HttpInputStream inputStream = new HttpInputStream(new BufferedInputStream(socket.getInputStream()));
        inputStream.putBackLine(rawRequestLine);
        HttpOutputStream outputStream = new HttpOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        super.handle(inputStream, outputStream, httpMessageListener);
    }

    @Override
    protected String getUrl(String path) {
        return path;
    }
}
