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
        int code;
        String msg;
        int idx2 = str.indexOf(' ', idx + 1);
        if (idx2 < 0) {
            code = Integer.parseInt(str.substring(idx + 1));
            msg = "";
        } else {
            code = Integer.parseInt(str.substring(idx + 1, idx2));
            msg = str.substring(idx2 + 1);
        }

        return new StatusLine(version, code, msg);
    }
}
