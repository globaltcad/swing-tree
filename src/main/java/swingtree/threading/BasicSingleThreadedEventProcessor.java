package swingtree.threading;

abstract class BasicSingleThreadedEventProcessor implements EventProcessor
{
    @Override public final void registerAppEvent( Runnable runnable ) { _tryRunning( runnable, true ); }

    @Override public final void registerAndRunAppEventNow( Runnable runnable ) { _tryRunning( runnable, true ); }

    @Override public final void registerUIEvent( Runnable runnable ) { _tryRunning( runnable, false ); }

    @Override public final void registerAndRunUIEventNow( Runnable runnable ) { _tryRunning( runnable, false ); }

    abstract protected void _tryRunning( Runnable runnable, boolean expectedToBeInvokedFromUIThread );
}
