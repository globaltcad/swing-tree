package swingtree;

class CoupledEventProcessor implements EventProcessor {

    @Override public void processAppEvent(Runnable runnable) { runnable.run(); }

    @Override public void processUIEvent(Runnable runnable) { runnable.run(); }

}
