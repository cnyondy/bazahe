package bazahe.httpproxy;

import bazahe.exception.HttpParserException;
import bazahe.httpparse.HttpInputStream;
import bazahe.httpparse.RequestLine;
import com.google.common.io.Closeables;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

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
    private final Socket serverSocket;
    private final SSLContextManager sslContextManager;
    @Nullable
    private final MessageListener messageListener;

    public ProxyWorker(Socket serverSocket, SSLContextManager sslContextManager,
                       @Nullable MessageListener messageListener)
            throws IOException {
        this.serverSocket = serverSocket;
        this.sslContextManager = sslContextManager;
        this.messageListener = messageListener;
    }

    @Override
    @SneakyThrows
    public void run() {
        try {
            HttpInputStream input = new HttpInputStream(serverSocket.getInputStream());
            input.mark(4096);
            String rawRequestLine = input.readLine();
            input.reset();
            if (rawRequestLine == null) {
                //error
                logger.error("empty client request");
                return;
            }
            RequestLine requestLine = RequestLine.parse(rawRequestLine);
            if (requestLine.isHttp10()) {
                //now just forbidden http 1.0
                logger.error("Http 1.0 not supported");
                return;
            }
            Handler handler;
            String method = requestLine.getMethod();
            String path = requestLine.getPath();
            if (method.equalsIgnoreCase("CONNECT")) {
                handler = new ConnectProxyHandler(sslContextManager);
            } else if (path.startsWith("/")) {
                handler = new HttpRequestHandler(sslContextManager);
            } else {
                handler = new CommonProxyHandler();
            }
            handler.handle(serverSocket, input, messageListener);
        } catch (HttpParserException e) {
            logger.error("Illegal http data", e);
        } catch (SocketTimeoutException e) {
            logger.debug("Socket Timeout", e);
        } catch (SocketException e) {
            logger.debug("Socket reset or closed?", e);
        } catch (IOException | UncheckedIOException e) {
            logger.error("IO error", e);
        } catch (Exception e) {
            logger.error("Error while handle http traffic", e);
        } catch (Throwable e) {
            logger.error("", e);
        } finally {
            Closeables.close(serverSocket, true);
        }
    }
}
