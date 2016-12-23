package bazahe.utils;

import org.junit.Test;

import static bazahe.utils.NetUtils.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Liu Dong
 */
public class NetUtilsTest {
    @Test
    public void getAddresses() throws Exception {

    }

    @Test
    public void genericMultiCDNS() throws Exception {
        String h = NetUtils.genericMultiCDNS("img1.fbcdn.com");
        assertEquals("img*.fbcdn.com", h);
    }

    @Test
    public void getHostType() throws Exception {
        assertEquals(HOST_TYPE_IPV6, NetUtils.getHostType("2031:0000:1F1F:0000:0000:0100:11A0:ADDF"));
        assertEquals(HOST_TYPE_IPV4, NetUtils.getHostType("202.38.64.14"));
        assertEquals(HOST_TYPE_DOMAIN, NetUtils.getHostType("v2ex.com"));
    }

}