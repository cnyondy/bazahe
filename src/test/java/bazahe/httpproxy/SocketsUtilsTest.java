package bazahe.httpproxy;

import org.junit.Test;

import static bazahe.httpproxy.SocketsUtils.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Liu Dong
 */
public class SocketsUtilsTest {
    @Test
    public void getHostType() throws Exception {
        assertEquals(HOST_TYPE_IPV6, SocketsUtils.getHostType("2031:0000:1F1F:0000:0000:0100:11A0:ADDF"));
        assertEquals(HOST_TYPE_IPV4, SocketsUtils.getHostType("202.38.64.14"));
        assertEquals(HOST_TYPE_DOMAIN, SocketsUtils.getHostType("v2ex.com"));
    }
}