package swingtree;

class CoupledEventProcessor implements EventProcessor
{
    @Override public void registerAppEvent(Runnable runnable ) { runnable.run(); }

    @Override public void registerAndRunAppEventNow(Runnable runnable ) { runnable.run(); }

    @Override public void registerUIEvent(Runnable runnable ) { runnable.run(); }

    @Override public void registerAndRunUIEventNow(Runnable runnable ) { runnable.run(); }
}
