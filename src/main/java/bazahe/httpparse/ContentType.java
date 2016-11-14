package bazahe.httpparse;

import lombok.Getter;
import net.dongliu.commons.Strings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.nio.charset.Charset;

/**
 * Http content type
 *
 * @author Liu Dong
 */
@Immutable
@Getter
public class ContentType {

    public static final ContentType UNKNOWN = new ContentType("", null);
    private final String type;
    @Nullable
    private final Charset charset;

    public ContentType(String type, @Nullable Charset charset) {
        this.type = type;
        this.charset = charset;
    }

    public static ContentType parse(String str) {
        String[] items = str.split(";");
        String type = "";
        String encoding = null;
        for (int i = 0; i < items.length; i++) {
            if (i == 0) {
                type = items[i];
                continue;
            }
            String item = items[i].trim();
            if (Strings.before(item, "=").trim().equalsIgnoreCase("charset")) {
                encoding = Strings.after(item, "=").trim();
                break;
            }
        }
        return new ContentType(type, encoding == null ? null : Charset.forName(encoding));
    }

    public boolean isText() {
        return Strings.containsAny(type, "html", "text", "json", "xml");
    }
}
