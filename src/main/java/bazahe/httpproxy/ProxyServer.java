package bazahe.httpproxy;

import bazahe.MainSetting;
import com.google.common.io.Closeables;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

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

    private final MainSetting mainSetting;
    private final SSLContextManager sslContextManager;
    private volatile ServerSocket serverSocket;
    @Setter
    private volatile MessageListener messageListener;

    private volatile ExecutorService executor;
    private volatile Thread masterThread;
    private final AtomicInteger threadCounter = new AtomicInteger();

    public ProxyServer(MainSetting config, SSLContextManager sslContextManager) {
        this.mainSetting = Objects.requireNonNull(config);
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
        if (mainSetting.getHost().isEmpty()) {
            serverSocket = new ServerSocket(mainSetting.getPort(), 128);
        } else {
            serverSocket = new ServerSocket(mainSetting.getPort(), 128, InetAddress.getByName(mainSetting.getHost()));
        }
        logger.info("proxy server run at {}:{}", mainSetting.getHost(), mainSetting.getPort());
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
                socket.setSoTimeout(mainSetting.getTimeout() * 1000);
                worker = new ProxyWorker(socket, sslContextManager, messageListener);
            } catch (Exception e) {
                Closeables.close(socket, true);
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
            Closeables.close(serverSocket, true);
            executor.shutdownNow();
        }
    }
}
