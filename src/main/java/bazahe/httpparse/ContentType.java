package bazahe.httpparse;

import lombok.Getter;
import net.dongliu.commons.Strings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Http content type
 *
 * @author Liu Dong
 */
@Immutable
@Getter
public class ContentType {

    public static final ContentType UNKNOWN = new ContentType("", null);
    private final String rawMimeType;
    private final MimeType mimeType;
    @Nullable
    private final Charset charset;

    public ContentType(String rawMimeType, @Nullable Charset charset) {
        this.rawMimeType = rawMimeType;
        this.mimeType = MimeType.parse(rawMimeType);
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
        return new ContentType(type, toCharsetSafe(encoding));
    }

    @Nullable
    private static Charset toCharsetSafe(@Nullable String encoding) {
        if (encoding == null) {
            return null;
        }
        try {
            return Charset.forName(encoding);
        } catch (UnsupportedCharsetException e) {
            return null;
        }
    }

    public boolean isText() {
        return Strings.equalsAnyIgnoreCase(mimeType.getType(), "text")
                || Strings.equalsAnyIgnoreCase(mimeType.getSubType(), "json", "x-www-form-urlencoded", "xml");
    }

    public boolean isImage() {
        return mimeType.getType().equals("image");
    }
}
