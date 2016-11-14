package bazahe.httpproxy;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.io.Closeables;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Proxy server
 *
 * @author Liu Dong
 */
@Log4j2
public class ProxyServer {

    private final String host;
    private final int port;
    private volatile ExecutorService executor;
    private volatile boolean stop;
    private volatile ServerSocket serverSocket;
    @Setter
    private volatile HttpMessageListener httpMessageListener;

    public ProxyServer(int port) {
        this("", port);
    }

    public ProxyServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Start proxy
     */
    @SneakyThrows
    public void run() {
        stop = false;
        executor = Executors.newCachedThreadPool();
        if (host.isEmpty()) {
            serverSocket = new ServerSocket(port, 128);
        } else {
            serverSocket = new ServerSocket(port, 128, InetAddress.getByName(host));
        }
        log.info("proxy server run at {}:{}", host, port);
        while (true) {
            Socket socket = serverSocket.accept();
            ProxyWorker proxyWorker;
            try {
                proxyWorker = new ProxyWorker(socket, httpMessageListener);
            } catch (Exception e) {
                Closeables.closeQuietly(socket);
                log.error("create new proxy worker failed.", e);
                continue;
            }
            log.debug(() -> "accept new connection, from: " + socket.getInetAddress());
            executor.submit(proxyWorker);
            if (Thread.interrupted()) {
                break;
            }
            if (stop) {
                break;
            }
        }
    }

    /**
     * Stop proxy
     */
    @SneakyThrows
    public void stop() {
        stop = true;
        serverSocket.close();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        new ProxyServer(1024).run();
    }
}
