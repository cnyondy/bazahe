package bazahe.httpproxy;

import lombok.SneakyThrows;
import net.dongliu.commons.BinarySize;
import net.dongliu.commons.io.Closeables;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Liu Dong
 */
class SocketsUtils {

    @SneakyThrows
    static void tunnel(InputStream srcIn, OutputStream srcOut, InputStream destIn, OutputStream destOut) {
        try {
            Thread thread = new Thread(() -> copy(destIn, srcOut));
            thread.start();
            copy(srcIn, destOut);
            thread.join();
        } finally {
            Closeables.closeQuietly(srcIn, srcOut, destIn, destOut);
        }
    }

    @SneakyThrows
    private static void copy(InputStream input, OutputStream output) {
        byte[] buffer = new byte[(int) BinarySize.kilobyte(2)];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }
}
