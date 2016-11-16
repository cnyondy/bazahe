package bazahe.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Liu Dong
 */
class AppResources {
    static final List<Runnable> tasks = Collections.synchronizedList(new ArrayList<>());

    static void registerTask(Runnable runnable) {
        tasks.add(runnable);
    }
}
