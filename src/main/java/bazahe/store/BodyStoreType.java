package bazahe.store;

import lombok.Getter;

/**
 * @author Liu Dong
 */
public enum BodyStoreType {
    plainText(0), html(0), xml(0), json(0), css(0), javascript(0), formEncoded(0),
    jpeg(1), png(1), bmp(1), gif(1), icon(1), otherImage(1),
    binary(2), unknown(2);

    @Getter
    private final int type;

    BodyStoreType(int type) {
        this.type = type;
    }
}
