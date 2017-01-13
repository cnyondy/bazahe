package bazahe.httpproxy;

import com.google.common.base.Stopwatch;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Liu Dong
 */
@Log4j2
public class SSLContextManager {

    @Getter
    private String keyStorePath;
    @Getter
    private AppKeyStoreGenerator appKeyStoreGenerator;
    private BigInteger lastCaCertSerialNumber;
    // ssl context cache
    private final ConcurrentHashMap<String, SSLContext> sslContextCache = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public SSLContextManager(String keyStorePath, char[] keyStorePassword) {
        this.keyStorePath = keyStorePath;
        Stopwatch stopWatch = Stopwatch.createStarted();
        AppKeyStoreGenerator appKeyStoreGenerator = new AppKeyStoreGenerator(keyStorePath, keyStorePassword);
        logger.info("Initialize AppKeyStoreGenerator cost {} ms", stopWatch.stop().elapsed(TimeUnit.MILLISECONDS));
        BigInteger caCertSerialNumber = appKeyStoreGenerator.getCACertSerialNumber();

        lock.writeLock().lock();
        try {
            if (caCertSerialNumber.equals(lastCaCertSerialNumber)) {
                // do nothing
                return;
            }
            this.appKeyStoreGenerator = appKeyStoreGenerator;
            this.lastCaCertSerialNumber = caCertSerialNumber;
            this.sslContextCache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SneakyThrows
    public SSLContext createSSlContext(String host) {
//        if (SocketsUtils.isDomain(host)) {
//            String[] items = host.split("\\.");
//            if (items.length >= 3) {
//                items[0] = "*";
//                host = String.join(".", (CharSequence[]) items);
//            }
//        }
        lock.readLock().lock();
        try {
            return sslContextCache.computeIfAbsent(host, this::getSslContextInner);
        } finally {
            lock.readLock().unlock();
        }
    }

    @SneakyThrows
    private SSLContext getSslContextInner(String host) {
        char[] appKeyStorePassword = "123456".toCharArray();
        Stopwatch stopWatch = Stopwatch.createStarted();
        KeyStore keyStore = appKeyStoreGenerator.generateKeyStore(host, 365, appKeyStorePassword);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, appKeyStorePassword);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagers, null, new SecureRandom());
        logger.info("Create ssh context for {}, cost {} ms", host, stopWatch.stop().elapsed(TimeUnit.MILLISECONDS));
        return sslContext;
    }

}
