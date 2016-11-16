package bazahe.ui.converter;

import bazahe.httpparse.ContentType;
import bazahe.ui.TextMessageConverter;

/**
 * @author Liu Dong
 */
public class AllTextMessageConverter implements TextMessageConverter {
    @Override
    public boolean accept(ContentType contentType) {
        return contentType.isText();
    }
}
