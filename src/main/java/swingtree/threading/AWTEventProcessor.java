package swingtree.threading;

import org.slf4j.Logger;
import swingtree.UI;

final class AWTEventProcessor extends BasicSingleThreadedEventProcessor
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AWTEventProcessor.class);

    @Override protected void _tryRunning( Runnable runnable ) {
        try {
            _checkIfThreadIsCorrect();
        } catch (Exception e) {
            // If a user wants better logging, they can do it through SLF4J or implement their own EventProcessor.
            log.error("The current thread is not the UI thread!", e);
        }
        try {
            runnable.run();
        } catch (Exception e) {
            // If a user wants better logging, they can do it through SLF4J or implement their own EventProcessor.
            log.error("An exception occurred while running a task in the UI thread!", e);
        }
    }

    private void _checkIfThreadIsCorrect() {
        if ( !UI.thisIsUIThread() ) {
            Thread currentThread = Thread.currentThread();
            String threadName    = currentThread.getName();
            String problem       = "Encountered the wrong thread instead of the expected UI thread!";
            String requirement   = "The currently used '" + EventProcessor.class.getName() + "' is the '" + AWTEventProcessor.class.getSimpleName() + "' which " +
                                   "expects tasks to be registered by the UI thread. The current thread however, \nis the '" + threadName + "' which " +
                                   "is not recognized as the UI thread (AWT's EDT thread).";
            String consequence   = "This problem is not fatal, but it may be caused by a bug and it may also cause bugs. Continuing anyway...";
            throw new RuntimeException( problem + "\n" + requirement + "\n" + consequence );
        }
    }
}
