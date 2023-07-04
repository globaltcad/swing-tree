package swingtree.threading;

import swingtree.UI;

import java.util.function.Supplier;

/**
 *  This {@link EventProcessor} implementation is called "coupled event processor"
 *  because when it is installed (see for example {@link swingtree.UI#use(EventProcessor, Supplier)})
 *  all the event handling will be done by the UI thead (the AWT event dispatch thread, short EDT),
 *  effectively coupling the UI to the business logic with respect to the execution model. <br>
 *  This is not a big problem for simple demo applications,
 *  however, when building larger products it is important to not use the UI thread for
 *  business logic so that the UI stays responsive.
 *  This is why you should prefer the usage of the {@link DecoupledEventProcessor}
 *  alongside the registration of a worker thread through {@link UI#joinDecoupledEventProcessor()}.
 */
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
