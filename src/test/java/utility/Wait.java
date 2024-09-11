package utility;

import swingtree.UI;

import java.util.function.Supplier;

/**
 *  A utility class making testing async code
 *  easier in SwingTree. More specifically, this
 *  is used to test SwingTree animations behave as expected.
 */
public class Wait {

    public static boolean until(Supplier<Boolean> condition, long timeout) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            if (condition.get()) {
                UI.sync();
                return true;
            }
            UI.sync();
        }
        return false;
    }

}
