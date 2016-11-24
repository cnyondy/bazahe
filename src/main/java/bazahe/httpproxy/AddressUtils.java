package bazahe.httpproxy;

import net.dongliu.commons.Strings;

/**
 * @author Liu Dong
 */
class AddressUtils {
    static String getHostFromTarget(String target) {
        return Strings.before(target, ":");
    }
}
