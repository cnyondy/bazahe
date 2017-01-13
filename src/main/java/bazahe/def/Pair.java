package bazahe.def;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable pair, the key and value cannot be null
 *
 * @author Liu Dong
 */
public class Pair<A, B> implements Map.Entry<A, B>, Serializable {
    private static final long serialVersionUID = -6525353427059094141L;
    private final A first;
    private final B second;

    public Pair(A first, B second) {
        this.first = Objects.requireNonNull(first);
        this.second = Objects.requireNonNull(second);
    }

    public static <K, V> Pair<K, V> of(K key, V value) {
        return new Pair<>(key, value);
    }

    public A first() {
        return first;
    }

    public B second() {
        return second;
    }

    public A getKey() {
        return first;
    }

    /**
     * Create a new pair with new key
     */
    public Pair<A, B> withKey(A newKey) {
        return new Pair<>(newKey, second);
    }

    public A getName() {
        return first;
    }

    /**
     * Create a new pair with new key
     */
    public Pair<A, B> withName(A newKey) {
        return new Pair<>(newKey, second);
    }

    public B getValue() {
        return second;
    }

    @Override
    public B setValue(B value) {
        throw new UnsupportedOperationException("Pair is read only");
    }

    /**
     * Create a new pair with new value
     */
    public Pair<A, B> withValue(B newValue) {
        return new Pair<>(first, newValue);
    }

    /**
     * To String pair
     */
    @SuppressWarnings("unchecked")
    public Pair<String, String> toStringPair() {
        if ((first instanceof String) && (second instanceof String)) {
            return (Pair<String, String>) this;
        }
        return Pair.of(first.toString(), second.toString());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;

        if (!first.equals(pair.first)) return false;
        return second.equals(pair.second);

    }

    @Override
    public int hashCode() {
        int result = first.hashCode();
        result = 31 * result + second.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "(" + first + "," + second + ")";
    }


}
