package bazahe.httpparse;

import bazahe.exception.HttpParserException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * The http status line
 *
 * @author Liu Dong
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class StatusLine {
    private final String version;
    private final int code;
    private final String status;

    @Override
    public String toString() {
        return String.format("StatusLine(%s %d %s)", version, code, status);
    }

    public static StatusLine parse(String str) {
        int idx = str.indexOf(' ');
        if (idx < 0) {
            throw new HttpParserException("Invalid http status line: " + str);
        }
        String version = str.substring(0, idx);
        int idx2 = str.indexOf(' ', idx + 1);
        if (idx2 < 0) {
            throw new HttpParserException("Invalid http status line: " + str);
        }
        int code = Integer.parseInt(str.substring(idx + 1, idx2));
        String msg = str.substring(idx2 + 1);
        return new StatusLine(version, code, msg);
    }
}
