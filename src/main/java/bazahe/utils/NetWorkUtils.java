package bazahe.utils;

import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Utils methods for deal with network
 *
 * @author Liu Dong
 */
@Log4j2
public class NetWorkUtils {

    public static final int HOST_TYPE_IPV6 = 0;
    public static final int HOST_TYPE_IPV4 = 1;
    public static final int HOST_TYPE_DOMAIN = 2;

    /**
     * Ipv4, ipv6, or domain
     */
    public static int getHostType(String host) {
        if (host.contains(":") && !host.contains(".")) {
            return HOST_TYPE_IPV6;
        }
        if (host.matches("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")) {
            return HOST_TYPE_IPV4;
        }
        return HOST_TYPE_DOMAIN;
    }

    public static boolean isIp(String host) {
        int type = getHostType(host);
        return type == HOST_TYPE_IPV4 || type == HOST_TYPE_IPV6;
    }

    public static boolean isDomain(String host) {
        int type = getHostType(host);
        return type == HOST_TYPE_DOMAIN;
    }


    public static String getHost(String target) {
        return StringUtils.substringBefore(target, ":");
    }

    public static int getPort(String target) {
        int idx = target.indexOf(":");
        if (idx > 0) {
            return Integer.parseInt(target.substring(idx + 1));
        }
        throw new RuntimeException("Target has no port: " + target);
    }
    

    @Nonnull
    public static List<NetworkInfo> getAddresses() {
        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            logger.warn("cannot get local network interface ip address", e);
            return ImmutableList.of();
        }
        List<NetworkInfo> list = new ArrayList<>();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            try {
                if (!networkInterface.isUp()) {
                    continue;
                }
            } catch (SocketException e) {
                logger.warn("", e);
            }
            String name = networkInterface.getName();
            String ip = "";
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (inetAddress instanceof Inet4Address) {
                    // TODO: may have zero or multi ipv4 address. fix this
                    ip = inetAddress.getHostAddress();
                }
            }
            if (!ip.isEmpty()) {
                list.add(new NetworkInfo(name, ip));
            }
        }
        return list;
    }


    /**
     * For uniq multi cdns, only with different index.
     * img1.fbcdn.com -> img*.fbcdn.com
     */
    public static String genericMultiCDNS(String host) {
        int idx = host.indexOf(".");
        if (idx < 2) {
            return host;
        }
        String first = host.substring(0, idx);
        if (!Character.isLetter(first.charAt(0))) {
            return host;
        }
        char c = first.charAt(first.length() - 1);
        if (!Character.isDigit(c)) {
            return host;
        }
        int firstEnd = first.length() - 2;
        while (Character.isDigit(first.charAt(firstEnd))) {
            firstEnd--;
        }
        return first.substring(0, firstEnd + 1) + "*." + host.substring(idx + 1);
    }


}
