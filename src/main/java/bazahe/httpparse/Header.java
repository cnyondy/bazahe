package bazahe.httpparse;

import net.dongliu.commons.collection.Pair;

/**
 * Header with name and value
 *
 * @author Liu Dong
 */
public class Header extends Pair<String, String> {
    public Header(String first, String second) {
        super(first, second);
    }

    /**
     * Parse header from header string
     */
    public static Header parse(String str) {
        int idx = str.indexOf(':');
        if (idx >= 0) {
            return new Header(str.substring(0, idx).trim(), str.substring(idx + 1).trim());
        } else {
            return new Header(str.trim(), "");
        }
    }

    @Override
    public String toString() {
        return "Header(" + getName() + "=" + getValue() + ")";
    }
}
