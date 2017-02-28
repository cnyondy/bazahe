package bazahe.utils;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.InputStream;

/**
 * @author Liu Dong
 */
@Log4j2
public class ByteStreamUtils {

    private static final int BUFFER_SIZE = 1024;


    public static void tunnel(InputStream input1, InputStream input2) throws InterruptedException {
        Thread thread = new Thread(() -> {
            try {
                ByteStreamUtils.consumeAll(input1);
            } catch (Throwable t) {
                logger.warn("tunnel traffic failed", t);
            }
        });
        thread.start();
        ByteStreamUtils.consumeAll(input2);
        thread.join();
    }

    /**
     * Read all data from input and discard
     *
     * @return the bytes consumed
     */
    @SneakyThrows
    public static long consumeAll(InputStream input) {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) > 0) {
            total += read;
        }
        return total;
    }

}
