package bazahe.ui.formater;

import lombok.SneakyThrows;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Format www-form-encoded content text
 *
 * @author Liu Dong
 */
public class FormURLEncodedFormatter implements Function<String, String> {

    private Charset charset;

    public FormURLEncodedFormatter(Charset charset) {
        this.charset = charset;
    }

    @Override

    public String apply(String s) {
        if (s.isEmpty()) {
            return s;
        }
        String[] items = s.split("&");
        List<String> lines = new ArrayList<>(items.length);
        for (String item : items) {
            int idx = item.indexOf('=');
            String name, value;
            if (idx < 0) {
                lines.add(decode(item));
            } else {
                name = item.substring(0, idx);
                value = item.substring(idx + 1);
                lines.add(decode(name) + "=" + decode(value));
            }
        }
        return String.join("\n", lines);
    }

    @SneakyThrows
    private String decode(String item) {
        return URLDecoder.decode(item, charset.name());
    }
}
