package utility

import swingtree.threading.EventProcessor

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 *  A test fixture which plays the role of the application main thread
 *  in specifications about the {@link EventProcessor#DECOUPLED} threading mode.
 *  <p>
 *  In a real SwingTree application using the decoupled event processor,
 *  the main thread (or a dedicated worker thread) continuously drains the
 *  application event queue, typically by calling
 *  {@code EventProcessor.DECOUPLED.join()} after the UI was shown.
 *  This fixture does exactly that on a background thread, with two
 *  test friendly twists:
 *  <ul>
 *      <li>it can be stopped cleanly through {@link #stop()},</li>
 *      <li>it records any exception thrown by an event task in {@link #failures()}
 *          instead of dying silently.</li>
 *  </ul>
 */
final class ApplicationThread
{
    private final Thread _thread
    private final List<Throwable> _failures

    private ApplicationThread( Thread thread, List<Throwable> failures ) {
        _thread   = thread
        _failures = failures
    }

    /**
     *  Starts a background thread which continuously processes the
     *  application events of the {@link EventProcessor#DECOUPLED} queue,
     *  just like the main thread of a real decoupled SwingTree application would.
     *  @return The running fixture, to be stopped through {@link #stop()} in a cleanup block.
     */
    static ApplicationThread startDraining() {
        var failures = new CopyOnWriteArrayList<Throwable>()
        var thread = new Thread({
            while ( !Thread.currentThread().isInterrupted() ) {
                try {
                    EventProcessor.DECOUPLED.joinUntilExceptionFor(1)
                } catch (InterruptedException ignored) {
                    break // A thread interruption is our signal to stop draining.
                } catch (Throwable e) {
                    failures.add(e)
                }
            }
        }, "application-thread")
        thread.daemon = true
        thread.start()
        return new ApplicationThread(thread, failures)
    }

    /**
     *  Interrupts the drainer thread and waits for it to die.
     *  This is robust against the calling thread itself being interrupted
     *  (which happens when a test trips its timeout and Spock interrupts it),
     *  so that this cleanup step never masks the actual test failure.
     */
    void stop() {
        _thread.interrupt()
        try {
            _thread.join(5_000)
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt() // Preserve the interrupt for whoever interrupted us.
        }
    }

    /**
     *  @return The name of the underlying thread, for comparing against
     *          thread names recorded inside event handlers.
     */
    String name() { return _thread.getName() }

    /**
     *  @return All exceptions which escaped an application event task,
     *          empty if event processing went smoothly.
     */
    List<Throwable> failures() { return _failures }

    /**
     *  Enqueues a no-op application event and waits for the drainer thread
     *  to have processed it. Because the queue is processed in FIFO order,
     *  this constitutes a barrier: when this method returns, every event
     *  registered before this call has been fully processed.
     *  @param timeoutSeconds How long to wait before failing loudly.
     */
    void awaitAllProcessed( long timeoutSeconds = 10 ) {
        var barrier = new CountDownLatch(1)
        EventProcessor.DECOUPLED.registerAppEvent({ barrier.countDown() })
        if ( !barrier.await(timeoutSeconds, TimeUnit.SECONDS) )
            throw new AssertionError(
                "The application thread did not finish processing its event queue " +
                "within $timeoutSeconds seconds! It may be dead or deadlocked."
            )
    }
}
