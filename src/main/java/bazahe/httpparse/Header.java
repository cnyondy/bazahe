package bazahe.httpparse;

import jdk.nashorn.internal.ir.annotations.Immutable;
import lombok.Getter;

import java.util.Map;

/**
 * Header with name and value
 *
 * @author Liu Dong
 */
@Immutable
public class Header implements Map.Entry<String, String> {
    @Getter
    private final String name;
    @Getter
    private final String value;


    public Header(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Parse header from header string
     */
    public static Header parse(String str) {
        int idx = str.indexOf(':');
        if (idx >= 0) {
            return new Header(str.substring(0, idx).trim(), str.substring(idx + 1).trim());
        } else {
            return new Header(str.trim(), "");
        }
    }

    @Override
    public String toString() {
        return "Header(" + name + "=" + value + ")";
    }

    @Override
    public String getKey() {
        return name;
    }

    @Override
    public String setValue(String value) {
        throw new UnsupportedOperationException();
    }
}
