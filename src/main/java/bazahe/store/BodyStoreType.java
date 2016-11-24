package bazahe.store;

import lombok.Getter;

/**
 * @author Liu Dong
 */
public enum BodyStoreType {
    plainText(true), html(true), xml(true), json(true), formEncoded(true),
    jpeg(false), png(false), bmp(false), gif(false), image(false),
    binary(false), unknown(false);

    @Getter
    private final boolean text;

    BodyStoreType(boolean text) {
        this.text = text;
    }
}
