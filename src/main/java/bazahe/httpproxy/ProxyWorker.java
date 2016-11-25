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
    public void run() {
        try {
            HttpInputStream input = new HttpInputStream(serverSocket.getInputStream());
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
            String method = requestLine.getMethod();
            String path = requestLine.getPath();
            if (method.equalsIgnoreCase("CONNECT")) {
                handler = new ConnectProxyHandler(sslContextManager);
            } else if (path.startsWith("/")) {
                handler = new HttpHandler(sslContextManager);
            } else {
                handler = new CommonProxyHandler();
            }
            handler.handle(serverSocket, rawRequestLine, messageListener);
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
            Closeables.closeQuietly(serverSocket);
        }
    }
}
