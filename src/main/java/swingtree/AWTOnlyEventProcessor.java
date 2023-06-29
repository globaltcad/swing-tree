package swingtree;

class AWTOnlyEventProcessor implements EventProcessor
{
    @Override public void registerAppEvent( Runnable runnable ) { _tryRunning( runnable ); }

    @Override public void registerAndRunAppEventNow( Runnable runnable ) { _tryRunning( runnable ); }

    @Override public void registerUIEvent( Runnable runnable ) { _tryRunning( runnable ); }

    @Override public void registerAndRunUIEventNow( Runnable runnable ) { _tryRunning( runnable ); }

    private void _tryRunning( Runnable runnable ) {
        if ( UI.thisIsUIThread() ) {
            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            Thread currentThread = Thread.currentThread();
            String threadName    = currentThread.getName();
            String problem       = "The current thread is not the UI thread!";
            String requirement   = "The current event processor is the '" + AWTOnlyEventProcessor.class.getSimpleName() + "' which " +
                                   "only supports running tasks in the UI thread.  The current thread however is '" + threadName + "' which " +
                                   "is not the UI thread (the AWT thread).";

            throw new RuntimeException( problem + "  " + requirement );
        }
    }
}
