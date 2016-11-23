package bazahe.httpproxy;

import bazahe.exception.HttpParserException;
import bazahe.httpparse.HttpInputStream;
import bazahe.httpparse.RequestLine;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.io.Closeables;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Proxy workers
 *
 * @author Liu Dong
 */
@Log4j2
public class ProxyWorker implements Runnable {
    private final Socket socket;
    @Nullable
    private final HttpMessageListener httpMessageListener;

    public ProxyWorker(Socket socket, @Nullable HttpMessageListener httpMessageListener)
            throws IOException {
        this.socket = socket;
        this.httpMessageListener = httpMessageListener;
    }

    @Override
    public void run() {
        try {
            HttpInputStream input = new HttpInputStream(socket.getInputStream());
            String rawRequestLine = input.readLine();
            if (rawRequestLine == null) {
                //error
                log.error("empty client request");
                return;
            }
            RequestLine requestLine = RequestLine.parse(rawRequestLine);
            if (requestLine.isHttp10()) {
                //TODO: now just forbidden http 1.0
                log.error("Http 1.0 not supported");
                return;
            }
            ProxyHandler handler;
            if (requestLine.getMethod().equalsIgnoreCase("CONNECT")) {
                handler = new ConnectProxyHandler();
            } else {
                handler = new CommonProxyHandler();
            }
            handler.handle(socket, rawRequestLine, httpMessageListener);
        } catch (HttpParserException e) {
            log.error("Illegal http data", e);
        } catch (SocketTimeoutException e) {
            log.debug("Socket Timeout", e);
        } catch (SocketException e) {
            log.debug("Socket reset or closed?", e);
        } catch (IOException | UncheckedIOException e) {
            log.error("IO error", e);
        } catch (Exception e) {
            log.error("Error while handle http traffic", e);
        } catch (Throwable e) {
            log.error("", e);
        } finally {
            Closeables.closeQuietly(socket);
        }
    }
}
