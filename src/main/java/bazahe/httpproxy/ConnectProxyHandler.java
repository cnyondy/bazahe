package bazahe.httpproxy;

import bazahe.httpparse.HttpInputStream;
import bazahe.httpparse.HttpOutputStream;
import bazahe.httpparse.RequestLine;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.Strings;
import net.dongliu.commons.io.InputOutputs;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

/**
 * @author Liu Dong
 */
@Log4j2
public class ConnectProxyHandler implements ProxyHandler {
    @Override
    public void handle(String rawRequestLine, HttpInputStream input, HttpOutputStream output,
                       HttpMessageListener httpMessageListener) throws IOException {
        RequestLine requestLine = RequestLine.parse(rawRequestLine);
        log.debug("Receive connect request to {}", requestLine.getUrl());
        List<String> rawHeaders = input.readHeaders();

        String host = Strings.before(requestLine.getUrl(), ":");
        int port = Integer.parseInt(Strings.after(requestLine.getUrl(), ":"));
        Socket socket = new Socket(InetAddress.getByName(host), port);
        output.writeLine("HTTP/1.1 200 OK\r\n");
        output.flush();
        new Thread(() -> {
            try {
                InputOutputs.copy(input, socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        InputOutputs.copy(socket.getInputStream(), output);
        log.debug("Connect proxy request to {} finished", requestLine.getUrl());
    }
}
