package bazahe.def;

import lombok.Getter;

/**
 * Message parent class
 *
 * @author Liu Dong
 */
public abstract class Message {
    @Getter
    private final String id;
    @Getter
    private final String host;
    @Getter
    private final String url;

    protected Message(String id, String host, String url) {
        this.id = id;
        this.host = host;
        this.url = url;
    }

    /**
     * For show in abstract
     */
    public abstract String getDisplay();
}
