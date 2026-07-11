package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import swingtree.threading.DecoupledEventProcessor
import swingtree.threading.EventProcessor
import utility.ApplicationThread
import utility.LogSpy
import utility.SwingTreeTestConfigurator

import javax.swing.JButton
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@Title("Decoupled Event Processing")
@Narrative('''

    The decoupled threading mode, activated by wrapping a UI declaration
    in `UI.use(EventProcessor.DECOUPLED, ...)`, splits your application
    in two worlds: the UI thread (AWT's event dispatch thread), which owns
    all Swing components, and an application thread, which owns your
    business logic and view models.

    The bridge between the two worlds is the `DecoupledEventProcessor`,
    which at its heart is simply a thread safe event queue:
    user events like button clicks do not execute your handler code
    on the UI thread, instead they enqueue it as a task
    to be picked up by the application thread.

    An important consequence of this design is that SwingTree does
    **not** create the application thread for you. Any thread of your
    choosing, usually the main thread, becomes the application thread
    simply by draining this queue, typically by calling
    `EventProcessor.DECOUPLED.join()` after the UI was shown.
    In this specification, the test thread itself will often take on
    that role, which makes the handover between the two worlds
    fully deterministic and easy to follow.

''')
@Subject([DecoupledEventProcessor, EventProcessor])
@Timeout(30)
@CompileDynamic
class Decoupled_Event_Processing_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // The global default remains strict, the decoupled mode is
        // activated per UI declaration through `UI.use(EventProcessor.DECOUPLED, ...)`.
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def setup() {
        // We make sure that every feature starts with an empty application event queue,
        // so that no leftovers from previous tests can interfere.
        try {
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
        } catch (Exception ignored) {}
    }

    def 'The decoupled event processor does not own a thread, any thread draining its queue becomes the application thread.'()
    {
        reportInfo """
            This is the fundamental contract of the decoupled mode:
            a user event, like a button click, merely *enqueues* your handler.
            Nothing runs until some thread volunteers to process the queue.

            In this test, the test thread itself takes on the role of
            the application thread by calling `joinFor(1)`, and we verify
            that the handler is executed by exactly this thread,
            and not by the UI thread.
        """
        given : 'A trace list into which the event handler reports what happened where.'
            var trace = new CopyOnWriteArrayList<String>()
        and : 'A button declared in the decoupled threading mode.'
            var ui =
                    UI.use(EventProcessor.DECOUPLED, ()->
                        UI.button("Do it!")
                        .onClick({
                            trace << "handled on '${Thread.currentThread().name}', UI thread: ${UI.thisIsUIThread()}".toString()
                        })
                    )
        and : 'We build the button on the UI thread, as required by the decoupled mode.'
            var button = UI.runAndGet({ ui.get(JButton) })

        when : 'The user clicks the button (which naturally happens on the UI thread).'
            UI.runNow({ button.doClick() })
        then : 'The handler has not run! It sits in the application event queue, waiting.'
            trace.isEmpty()

        when : 'This test thread now volunteers to process a single application event.'
            EventProcessor.DECOUPLED.joinFor(1)
        then : 'The handler was executed by this very thread, far away from the UI thread.'
            trace == ["handled on '${Thread.currentThread().name}', UI thread: false".toString()]
    }

    def 'Application events are processed in the order they occurred, exactly as many as requested.'()
    {
        reportInfo """
            The application event queue is a FIFO queue, and the various
            `join` methods give you precise control over how much of it
            you want to process:

            - `joinFor(n)` processes exactly `n` events (and waits for them if need be),
            - `joinUntilDoneOrException()` drains the queue until it is empty,
            - `join()` processes events forever, which is what a real
              application main thread typically does.
        """
        given : 'A counter based trace and a decoupled button which reports each click.'
            var clickCounter = new AtomicInteger()
            var trace = new CopyOnWriteArrayList<Integer>()
            var ui =
                    UI.use(EventProcessor.DECOUPLED, ()->
                        UI.button("Click me thrice!")
                        .onClick({ trace << clickCounter.incrementAndGet() })
                    )
            var button = UI.runAndGet({ ui.get(JButton) })

        when : 'The button is clicked three times in a row.'
            UI.runNow({
                button.doClick()
                button.doClick()
                button.doClick()
            })
        then : 'All three events are still queued up, none of them was handled.'
            trace.isEmpty()

        when : 'We process exactly two of them.'
            EventProcessor.DECOUPLED.joinFor(2)
        then : 'The first two events were handled, in the order they were fired.'
            trace == [1, 2]

        when : 'We drain whatever is left in the queue.'
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
        then : 'The remaining third event was handled as well.'
            trace == [1, 2, 3]
    }

    def 'Use `registerAndRunAppEventNow(Runnable)` to hand a task to the application thread and wait for its completion.'()
    {
        reportInfo """
            Sometimes the UI side needs to hand work over to the application
            thread and continue only after that work is done.
            The `registerAndRunAppEventNow(Runnable)` method does exactly that:
            it enqueues the task and blocks until the application thread
            has processed it.

            **Beware:** this only works if some application thread is actually
            draining the queue! If no such thread exists, the call will
            wait forever. In this test we therefore first start a background
            thread which continuously processes application events,
            just like the main thread of a real decoupled application would.
        """
        given : 'A continuously draining application thread and a trace list.'
            var app = ApplicationThread.startDraining()
            var trace = new CopyOnWriteArrayList<String>()

        when : 'We hand a task over to the application thread and wait for it.'
            EventProcessor.DECOUPLED.registerAndRunAppEventNow({
                trace << "executed on '${Thread.currentThread().name}'".toString()
            })
        then : 'The moment the call returns, the task is guaranteed to have been executed, on the application thread.'
            trace == ["executed on 'application-thread'"]

        cleanup :
            app.stop()
    }

    def 'The UI thread itself is not allowed to join the application event queue.'()
    {
        reportInfo """
            The whole point of the decoupled mode is that the UI thread stays
            responsive while the application thread does the heavy lifting.
            If the UI thread were to join the application event queue, it
            would block itself forever and freeze the UI.
            SwingTree protects you from this mistake by failing fast.
        """
        when : 'We ask the UI thread to join the application event queue...'
            var error = UI.runAndGet({
                try {
                    EventProcessor.DECOUPLED.join()
                    return null
                } catch (Exception e) {
                    return e
                }
            })
        then : '...which it refuses with an exception.'
            error instanceof IllegalStateException
            error.message.contains("UI thread")
    }

    def 'A failing event task is logged by the lenient `joinFor`, but rethrown by `joinUntilExceptionFor`.'()
    {
        reportInfo """
            What happens when an application event task throws an exception
            depends on how the queue is drained. The lenient methods,
            `join()` and `joinFor(n)`, log the exception and keep the queue
            going, because a single failing event should not bring down
            the whole application.
            The `joinUntilException...` variants on the other hand rethrow
            the exception to the calling thread, which is useful
            for debugging and for tests.

            Note that this test uses a log spy to prove that the leniently
            handled exception is not simply lost, it lands in the log.
        """
        given : 'A spy on the log, so we can observe silently handled failures.'
            var log = LogSpy.attach()
        and : 'Two application events, the first of which misbehaves.'
            var trace = []
            EventProcessor.DECOUPLED.registerAppEvent({ throw new IllegalStateException("Something went wrong!") })
            EventProcessor.DECOUPLED.registerAppEvent({ trace << "all good here" })

        when : 'We process both events with the lenient `joinFor`.'
            EventProcessor.DECOUPLED.joinFor(2)
        then : 'No exception reached us, and the queue kept going after the failure.'
            noExceptionThrown()
            trace == ["all good here"]
        and : 'The failure was not lost though, it was recorded in the log.'
            log.errors().size() == 1
            log.errors()[0].contains("Something went wrong!")

        when : 'We enqueue another misbehaving event, but drain with `joinUntilExceptionFor` this time.'
            EventProcessor.DECOUPLED.registerAppEvent({ throw new IllegalStateException("Again!") })
            EventProcessor.DECOUPLED.joinUntilExceptionFor(1)
        then : 'This time the exception is rethrown right into our face.'
            var e = thrown(IllegalStateException)
            e.message == "Again!"

        cleanup :
            log.detach()
    }

    def 'An exception in a UI event handler is logged, it does not disturb event processing.'()
    {
        reportInfo """
            The event handlers you register on your components, like `onClick`
            actions, are guarded by SwingTree itself. If your handler throws,
            the exception is caught and logged, and neither the UI thread nor
            the application thread is harmed.

            This is very convenient in production, but be aware of it during
            development: a bug in your handler will not fail loudly!
            It only leaves an ERROR entry in the log, which is why the
            log spy used by this test is such a valuable tool.
        """
        given : 'A log spy and a decoupled button whose handler always fails.'
            var log = LogSpy.attach()
            var ui =
                    UI.use(EventProcessor.DECOUPLED, ()->
                        UI.button("Kaboom")
                        .onClick({ throw new IllegalStateException("Kaboom!") })
                    )
            var button = UI.runAndGet({ ui.get(JButton) })

        when : 'The button is clicked and the event is processed by this thread.'
            UI.runNow({ button.doClick() })
            EventProcessor.DECOUPLED.joinFor(1)
        then : 'No exception surfaced, neither here nor on the UI thread.'
            noExceptionThrown()
        and : 'But the log tells the true story.'
            log.errors().size() == 1
            log.errors()[0].contains("Kaboom!")

        cleanup :
            log.detach()
    }
}
