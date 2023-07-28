package swingtree.threading;

import java.util.function.Supplier;

/**
 *  This {@link EventProcessor} implementation is called lenient because it does not
 *  throw any exceptions when a task is not registered by the UI thread.
 *  When this processor is installed
 *  all the event handling will typically be done by the UI thead (the AWT event dispatch thread, short EDT)
 *  simply because all the Swing component events will be executed on the EDT,
 *  however this will not be enforced. <br>
 *  Note that this {@link EventProcessor} will effectively couple the UI
 *  to the business logic of you application with respect to the execution model. <br>
 *  This is not a big problem for simple demo applications,
 *  however, when building larger products it is important to not use the UI thread for
 *  business logic so that the UI stays responsive.
 *  This is why you should prefer the usage of the {@link DecoupledEventProcessor}
 *  alongside the registration of a worker thread through
 *  {@link DecoupledEventProcessor#join()} at {@link EventProcessor#DECOUPLED}.
 *  <br><br>
 *  See {@link swingtree.UI#use(EventProcessor, Supplier)} for more information about the
 *  usage of the {@link EventProcessor} interface.
 */
final class LenientAWTEventProcessor extends BasicSingleThreadedEventProcessor
{
    @Override protected void _tryRunning( Runnable runnable ) {
        try {
            runnable.run();
        } catch (Exception e) {
            e.printStackTrace(); // If a user wants better logging, they can implement their own EventProcessor.
        }
    }
}
