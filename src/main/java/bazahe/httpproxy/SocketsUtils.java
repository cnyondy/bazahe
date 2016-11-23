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

    static final int HOST_TYPE_IPV6 = 0;
    static final int HOST_TYPE_IPV4 = 1;
    static final int HOST_TYPE_DOMAIN = 2;

    static int getHostType(String host) {
        if (host.contains(":") && !host.contains(".")) {
            return HOST_TYPE_IPV6;
        }
        if (host.matches("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")) {
            return HOST_TYPE_IPV4;
        }
        return HOST_TYPE_DOMAIN;
    }

    static boolean isIp(String host) {
        int type = getHostType(host);
        return type == HOST_TYPE_IPV4 || type == HOST_TYPE_IPV6;
    }

    static boolean isDomain(String host) {
        int type = getHostType(host);
        return type == HOST_TYPE_DOMAIN;
    }

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
