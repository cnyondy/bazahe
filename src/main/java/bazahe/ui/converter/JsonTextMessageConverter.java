package bazahe.ui.converter;

import bazahe.httpparse.ContentType;
import bazahe.ui.TextMessageConverter;

/**
 * @author Liu Dong
 */
public class JsonTextMessageConverter implements TextMessageConverter {

    @Override
    public boolean accept(ContentType contentType) {
        return contentType.getMimeType().getSubType().equalsIgnoreCase("json");
    }
}
