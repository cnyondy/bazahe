package bazahe.httpproxy;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Liu Dong
 */
public class AddressUtilsTest {
    @Test
    public void getHostFromTarget() throws Exception {
        assertEquals("mvnrepository.com", AddressUtils.getHost("mvnrepository.com:443"));
        assertEquals("mvnrepository.com", AddressUtils.getHost("mvnrepository.com"));
    }

    @Test
    public void getPortFromTarget() throws Exception {
        assertEquals(443, AddressUtils.getPort("mvnrepository.com:443"));
    }

}