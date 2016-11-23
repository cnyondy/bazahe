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
    private final String url;
    private String display;

    protected Message(String id, String url) {
        this.id = id;
        this.url = url;
    }

    /**
     * For show in abstract
     */
    public abstract String getDisplay();
}
