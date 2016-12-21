package bazahe.ui.formater;

import org.json.JSONObject;

import java.util.function.Function;

/**
 * @author Liu Dong
 */
public class JSONFormatter implements Function<String, String> {
    private int indent;

    public JSONFormatter(int indent) {
        this.indent = indent;
    }

    @Override
    public String apply(String s) {
        JSONObject json = new JSONObject(s);
        return json.toString(indent);
    }
}
