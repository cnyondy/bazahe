package bazahe.utils;

import org.junit.Test;

import static bazahe.utils.NetWorkUtils.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Liu Dong
 */
public class NetWorkUtilsTest {
    @Test
    public void getAddresses() throws Exception {

    }


    @Test
    public void getHostFromTarget() throws Exception {
        assertEquals("mvnrepository.com", NetWorkUtils.getHost("mvnrepository.com:443"));
        assertEquals("mvnrepository.com", NetWorkUtils.getHost("mvnrepository.com"));
    }

    @Test
    public void getPortFromTarget() throws Exception {
        assertEquals(443, NetWorkUtils.getPort("mvnrepository.com:443"));
    }

    @Test
    public void genericMultiCDNS() throws Exception {
        String h = NetWorkUtils.genericMultiCDNS("img1.fbcdn.com");
        assertEquals("img*.fbcdn.com", h);
    }

    @Test
    public void getHostType() throws Exception {
        assertEquals(HOST_TYPE_IPV6, NetWorkUtils.getHostType("2031:0000:1F1F:0000:0000:0100:11A0:ADDF"));
        assertEquals(HOST_TYPE_IPV4, NetWorkUtils.getHostType("202.38.64.14"));
        assertEquals(HOST_TYPE_DOMAIN, NetWorkUtils.getHostType("v2ex.com"));
    }

}