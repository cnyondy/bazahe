package bazahe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Liu Dong
 */
public class ShutdownHooks {

    static final List<Runnable> tasks = Collections.synchronizedList(new ArrayList<>());

    public static void registerTask(Runnable runnable) {
        tasks.add(runnable);
    }
}
