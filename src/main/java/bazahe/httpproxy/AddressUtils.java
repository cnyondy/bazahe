package bazahe.httpproxy;

import javax.annotation.Nullable;

/**
 * @author Liu Dong
 */
class AddressUtils {
    static String getHostFromTarget(String target) {
        int idx = target.indexOf(":");
        if (idx > 0) {
            return target.substring(0, idx);
        }
        return target;
    }

    static int getPortFromTarget(String target) {
        int idx = target.indexOf(":");
        if (idx > 0) {
            return Integer.parseInt(target.substring(idx + 1));
        }
        throw new RuntimeException("Target has no port: " + target);
    }

    static String getUrl(boolean ssl, @Nullable String upgrade, String host, int port, String path) {
        String protocol;
        if ("websocket".equals(upgrade)) {
            protocol = ssl ? "wss" : "ws";
        } else {
            protocol = ssl ? "https" : "http";
        }
        StringBuilder sb = new StringBuilder(protocol).append("://").append(host);
        if (!(port == 443 && ssl || port == 80 && !ssl)) {
            sb.append(":").append(port);
        }
        sb.append(path);
        return sb.toString();
    }
}
