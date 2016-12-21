package bazahe.ui.formater;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Liu Dong
 */
public class XMLFormatterTest {
    @Test
    public void apply() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><tag><nested>hello</nested></tag>";
        String formatted = new XMLFormatter(4).apply(xml);
//        System.out.println(formatted);
        assertTrue(formatted.split("\n").length >= 4);
    }

}