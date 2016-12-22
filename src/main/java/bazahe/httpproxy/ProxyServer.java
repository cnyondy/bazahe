package bazahe.httpproxy;

import bazahe.def.ProxyConfig;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.io.Closeables;

import javax.annotation.concurrent.ThreadSafe;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Proxy server
 *
 * @author Liu Dong
 */
@Log4j2
@ThreadSafe
public class ProxyServer {

    private final ProxyConfig proxyConfig;
    private final SSLContextManager sslContextManager;
    private volatile ServerSocket serverSocket;
    @Setter
    private volatile MessageListener messageListener;

    private volatile ExecutorService executor;
    private volatile Thread masterThread;
    private final AtomicInteger threadCounter = new AtomicInteger();

    public ProxyServer(ProxyConfig config, SSLContextManager sslContextManager) {
        this.proxyConfig = Objects.requireNonNull(config);
        this.sslContextManager = sslContextManager;
    }

    /**
     * Start proxy server
     */
    public void start() {
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("proxy-server-worker-" + threadCounter.getAndIncrement());
            t.setDaemon(true);
            return t;
        });

        masterThread = new Thread(this::run);
        masterThread.setName("proxy-server-master");
        masterThread.setDaemon(true);
        masterThread.start();
    }

    /**
     * Wait proxy server to stop
     */
    @SneakyThrows
    public void join() {
        masterThread.join();
    }

    /**
     * Start proxy
     */
    @SneakyThrows
    private void run() {
        if (proxyConfig.getHost().isEmpty()) {
            serverSocket = new ServerSocket(proxyConfig.getPort(), 128);
        } else {
            serverSocket = new ServerSocket(proxyConfig.getPort(), 128, InetAddress.getByName(proxyConfig.getHost()));
        }
        logger.info("proxy server run at {}:{}", proxyConfig.getHost(), proxyConfig.getPort());
        while (true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (SocketException e) {
                if (Thread.currentThread().isInterrupted()) {
                    // server be stopped
                    break;
                } else {
                    logger.error("", e);
                }
                continue;
            }
            ProxyWorker worker;
            try {
                socket.setSoTimeout(proxyConfig.getTimeout());
                worker = new ProxyWorker(socket, sslContextManager, messageListener);
            } catch (Exception e) {
                Closeables.closeQuietly(socket);
                logger.error("Create new proxy worker failed.", e);
                continue;
            }
            logger.debug("Accept new connection, from: {}", socket.getInetAddress());
            executor.submit(worker);
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
        }
    }

    /**
     * Stop proxy
     */
    @SneakyThrows
    public void stop() {
        if (!masterThread.isInterrupted()) {
            logger.info("Stopping proxy server...");
            masterThread.interrupt();
            Closeables.closeQuietly(serverSocket);
            executor.shutdownNow();
        }
    }
}
