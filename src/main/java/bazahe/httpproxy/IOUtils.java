package bazahe.httpproxy;

import lombok.SneakyThrows;

import java.io.InputStream;

/**
 * @author Liu Dong
 */
class IOUtils {

    private static final int BUFFER_SIZE = 1024;

    /**
     * Read all data from input and discard
     *
     * @return the bytes consumed
     */
    @SneakyThrows
    static long consumeAll(InputStream input) {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) > 0) {
            total += read;
        }
        return total;
    }
}
