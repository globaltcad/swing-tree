package swingtree.threading;

import swingtree.UI;

final class AWTEventProcessor extends BasicSingleThreadedEventProcessor
{
    @Override protected void _tryRunning( Runnable runnable ) {
        try {
            _checkIfThreadIsCorrect();
        } catch (Exception e) {
            e.printStackTrace(); // If a user wants better logging, they can implement their own EventProcessor.
        }
        try {
            runnable.run();
        } catch (Exception e) {
            e.printStackTrace(); // If a user wants better logging, they can implement their own EventProcessor.
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
