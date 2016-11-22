package bazahe.httpproxy;

/**
 * @author Liu Dong
 */
class HttpUtils {
    static boolean respHasBody(String method, int statusCode) {
        if (method.equalsIgnoreCase("HEAD")) {
            return false;
        }
        if (statusCode >= 100 && statusCode < 200 || statusCode == 204 || statusCode == 304) {
            return false;
        }
        return true;
    }
}
