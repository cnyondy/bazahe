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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Proxy server
 *
 * @author Liu Dong
 */
@Log4j2
@ThreadSafe
public class ProxyServer {

    private final String host;
    private final int port;
    private final String keyStorePath;

    private volatile ServerSocket serverSocket;
    @Setter
    private volatile HttpMessageListener httpMessageListener;

    private volatile ExecutorService executor;
    private volatile Thread masterThread;
    private final AtomicInteger threadCounter = new AtomicInteger();

    public ProxyServer(ProxyConfig config) {
        this.host = config.getHost();
        this.port = config.getPort();
        this.keyStorePath = config.getKeyStore();
    }

    /**
     * Start proxy server
     */
    public void start() {
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("proxy-server-worker-" + threadCounter.getAndIncrement());
            return t;
        });

        masterThread = new Thread(this::run);
        masterThread.setName("proxy-server-master");
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
        if (host.isEmpty()) {
            serverSocket = new ServerSocket(port, 128);
        } else {
            serverSocket = new ServerSocket(port, 128, InetAddress.getByName(host));
        }
        log.info("proxy server run at {}:{}", host, port);
        while (true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (SocketException e) {
                if (Thread.currentThread().isInterrupted()) {
                    // server be stopped
                    break;
                } else {
                    log.error("", e);
                }
                continue;
            }
            ProxyWorker worker;
            try {
                socket.setSoTimeout(2000);
                worker = new ProxyWorker(socket, httpMessageListener, keyStorePath);
            } catch (Exception e) {
                Closeables.closeQuietly(socket);
                log.error("Create new proxy worker failed.", e);
                continue;
            }
            log.debug("Accept new connection, from: {}", socket.getInetAddress());
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
            log.info("Stopping proxy server...");
            masterThread.interrupt();
            Closeables.closeQuietly(serverSocket);
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    public static void main(String[] args) {
        new ProxyServer(ProxyConfig.getDefault()).run();
    }
}
