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
            String problem       = "The current thread is not the UI thread!";
            String requirement   = "The current event processor is the '" + AWTEventProcessor.class.getSimpleName() + "' which " +
                                   "only supports running tasks in the UI thread.  The current thread however is '" + threadName + "' which " +
                                   "is not the UI thread (the AWT thread).";

            throw new RuntimeException( problem + "  " + requirement );
        }
    }
}
