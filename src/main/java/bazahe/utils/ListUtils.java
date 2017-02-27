package bazahe.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Liu Dong
 */
public class ListUtils {

    /**
     * eagle convert list
     */
    public static <S, T> List<T> convert(List<S> list, Function<S, T> convter) {
        List<T> result = new ArrayList<>(list.size());
        for (S item : list) {
            result.add(convter.apply(item));
        }
        return result;
    }

    /**
     * eagle filter list
     */
    public static <T> List<T> filter(List<T> list, Predicate<T> predicate) {
        List<T> result = new ArrayList<>(Math.min(8, list.size()));
        for (T item : list) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }
}
