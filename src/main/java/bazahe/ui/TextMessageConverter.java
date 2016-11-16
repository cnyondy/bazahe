package bazahe.ui;

import bazahe.httpparse.ContentType;
import net.dongliu.commons.RefValues;
import net.dongliu.commons.io.ReaderWriters;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Interface to convert http message to string
 *
 * @author Liu Dong
 */
public interface TextMessageConverter {

    /**
     * If can handle this content type, return true; else return false
     */
    boolean accept(ContentType contentType);

    /**
     * If can handle this content type, return non-null string; else return null
     */
    default String convert(InputStream inputStream, ContentType contentType) {
        Charset charset = RefValues.ifNullThen(contentType.getCharset(), StandardCharsets.UTF_8);
        return ReaderWriters.readAll(new InputStreamReader(inputStream, charset));
    }
}
