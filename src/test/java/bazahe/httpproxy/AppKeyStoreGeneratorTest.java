package bazahe.httpproxy;

import org.junit.Test;

import static bazahe.httpproxy.AppKeyStoreGenerator.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Liu Dong
 */
public class AppKeyStoreGeneratorTest {
    @Test
    public void getHostType() throws Exception {
        assertEquals(TYPE_IPV6, AppKeyStoreGenerator.getHostType("2031:0000:1F1F:0000:0000:0100:11A0:ADDF"));
        assertEquals(TYPE_IPV4, AppKeyStoreGenerator.getHostType("202.38.64.14"));
        assertEquals(TYPE_DOMAIN, AppKeyStoreGenerator.getHostType("v2ex.com"));
    }

}