package bazahe.httpproxy;

import net.dongliu.commons.Strings;

import javax.annotation.Nullable;

/**
 * @author Liu Dong
 */
class AddressUtils {
    static String getHostFromTarget(String target) {
        return Strings.before(target, ":");
    }

    static int getPortFromTarget(String target) {
        return Integer.parseInt(Strings.after(target, ":"));
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
