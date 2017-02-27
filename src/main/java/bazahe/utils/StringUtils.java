package bazahe.utils;

/**
 * @author Liu Dong
 */
public class StringUtils {

    public static String substringBefore(String str, String sep) {
        int idx = str.indexOf(sep);
        if (idx == -1) {
            return str;
        }
        return str.substring(0, idx);
    }

    public static int toInt(String numberStr) {
        try {
            return Integer.parseInt(numberStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
