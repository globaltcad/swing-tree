package swingtree.threading;

class CoupledEventProcessor implements EventProcessor
{
    @Override public void registerAppEvent( Runnable runnable ) { _tryRunning( runnable ); }

    @Override public void registerAndRunAppEventNow( Runnable runnable ) { _tryRunning( runnable ); }

    @Override public void registerUIEvent( Runnable runnable ) { _tryRunning( runnable ); }

    @Override public void registerAndRunUIEventNow( Runnable runnable ) { _tryRunning( runnable ); }

    private void _tryRunning( Runnable runnable ) {
        try {
            runnable.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
