package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import sprouts.Var
import swingtree.threading.EventProcessor
import utility.ApplicationThread
import utility.LogSpy
import utility.SwingTreeTestConfigurator

import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

@Title("Property Binding Across Threads")
@Narrative('''

    The single most important boundary in a decoupled SwingTree application
    is the one between your property based view models, which live on the
    application thread, and the Swing components, which live on the UI thread.

    Whenever a bound property changes, the new state has to travel from the
    application thread to the UI thread, and whenever the user interacts
    with a component, the event has to travel from the UI thread to the
    application thread. Both directions cross a thread boundary,
    which raises delicate questions:
    Do updates get lost? Can a stale value overtake a newer one?
    Which thread executes what?

    This specification answers these questions. To keep the concurrency
    easy to reason about, most tests use two simple tricks:
    the test thread itself plays the role of the application thread
    (by draining the application event queue on demand), and where
    necessary the UI thread is temporarily parked behind a latch so that
    in-flight updates can be observed before they are applied.
    No mocks are involved, everything runs through the real
    event processor, real components and real properties.

''')
@Subject([UIForAnySwing, EventProcessor])
@Timeout(60)
@CompileDynamic
class Decoupled_Property_Binding_Spec extends Specification
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

    def 'A property change from the application thread reaches the component asynchronously, on the UI thread.'()
    {
        reportInfo """
            When a bound property is changed by the application thread,
            the component is **not** updated synchronously as part of the
            `set(..)` call. Instead, the new state is dispatched to the
            UI thread, and the component catches up as soon as
            the UI thread gets around to it.

            To prove this deterministically, we briefly park the UI thread
            behind a latch: the property change then demonstrably happens
            *before* the component has updated, and the component follows
            once the UI thread is released, without us touching
            the component ourselves.
        """
        given : 'A text property, as it would live in one of your view models.'
            var text = Var.of("Hello!")
        and : 'A label built in the decoupled mode, with its text bound to the property.'
            var label = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> UI.label(text)).get(JLabel)
            })
        expect : 'Initially, the label shows the current property value.'
            label.text == "Hello!"

        when : 'We park the UI thread behind a gate, so that we can catch the next update in flight.'
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
        and : 'We change the property from this thread and immediately look at the label, then release the UI thread.'
            var textRightAfterSet = null
            try {
                text.set("Goodbye!")
                textRightAfterSet = label.text
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'Right after the property change, the label was still untouched...'
            textRightAfterSet == "Hello!"
        and : '...but once the UI thread caught up, the new state arrived in the component.'
            label.text == "Goodbye!"
    }

    def 'Rapid property changes are all replayed on the UI thread, in the order they were made.'()
    {
        reportInfo """
            When the application thread fires several property changes in
            quick succession, each change is shipped to the UI thread together
            with the value it carried *at the time of the change*.
            The UI thread then replays them in their original order.

            This has two important consequences which this test pins down:
            no intermediate value is skipped, and no stale value can
            overtake a newer one, even though the property already holds
            the final value long before the UI thread wakes up.

            To observe the individual updates as they land on the component,
            this test uses a label which records every `setText(..)` call
            together with the thread that performed it.
        """
        given : 'A recording label and a text property bound to it in decoupled mode.'
            var underlying = new RecordingLabel()
            var text = Var.of("initial")
            var label = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> UI.of(underlying).withText(text)).get(JLabel)
            })
        and : 'We forget the text assignments made while building the declaration.'
            underlying.applied.clear()

        when : 'We park the UI thread, fire three quick changes from this thread, and release the UI thread again.'
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            try {
                text.set("A")
                text.set("B")
                text.set("C")
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'Every single value arrived on the component, in order, and all of them on the UI thread.'
            underlying.applied == [
                "'A' (UI thread: true)",
                "'B' (UI thread: true)",
                "'C' (UI thread: true)"
            ]
        and : 'The component settled on the final property value.'
            label.text == "C"
            text.is("C")
    }

    def 'A button click travels to the application thread, updates the view model, and the new state travels back to the UI.'()
    {
        reportInfo """
            This is the full MVVM round trip of a decoupled SwingTree
            application, in slow motion:

            1. The user clicks a button, on the UI thread.
            2. The click handler is enqueued for the application thread,
               where it may safely work with the view model.
            3. The view model property changes, which enqueues a display
               update for the UI thread.
            4. The UI thread applies the new state to the component.

            The test thread plays the application thread, so every one
            of these four steps happens exactly when the test says so.
        """
        given : 'A minimal view model: an integer counter property.'
            var counter = Var.of(0)
        and : 'A trace which records where the business logic runs.'
            var trace = new CopyOnWriteArrayList<String>()
        and : 'A decoupled view: a button which increments the counter, and a label displaying it.'
            var view = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.panel()
                    .add(
                        UI.button("+").onClick({
                            counter.set(counter.get() + 1)
                            trace << "incremented on '${Thread.currentThread().name}'".toString()
                        })
                    )
                    .add(
                        UI.label(counter.viewAsString())
                    )
                ).get(JPanel)
            })
            var button = (JButton) view.getComponent(0)
            var label  = (JLabel)  view.getComponent(1)
        expect : 'The label starts out with the initial counter value.'
            label.text == "0"

        when : 'The user clicks the button, on the UI thread.'
            UI.runNow({ button.doClick() })
        then : 'Nothing has happened yet, the business logic is waiting in the application event queue.'
            trace.isEmpty()
            counter.is(0)

        when : 'This test thread, playing the application thread, processes the event.'
            EventProcessor.DECOUPLED.joinFor(1)
        then : 'The handler ran on this thread and updated the view model.'
            trace == ["incremented on '${Thread.currentThread().name}'".toString()]
            counter.is(1)

        when : 'We wait for the UI thread to catch up with the state change.'
            UI.sync()
        then : 'The label displays the new counter value.'
            label.text == "1"
    }

    def 'Under real concurrent load, from multiple threads, not a single update is lost.'(int run)
    {
        reportInfo """
            The previous features scripted the thread handover step by step.
            This one lets go of the reins: a continuously draining application
            thread runs in the background while the UI thread and two
            additional producer threads all funnel counter increments into
            the application event queue, concurrently and as fast as they can.

            Because all view model mutations are executed by the single
            application thread, they are serialized, and so, no matter how
            the threads interleave, the final counter must be exact and
            the label must display it. The feature is repeated several times
            to explore different thread interleavings.

            Also note that a log spy watches the whole ride: if anything
            inside SwingTree fails silently and merely logs an error,
            this test fails.
        """
        given : 'A spy on the log, so that even silently logged errors fail this test.'
            var log = LogSpy.attach()
        and : 'A continuously draining application thread, like in a real decoupled application.'
            var app = ApplicationThread.startDraining()
        and : 'A view model counter, and a decoupled view with an incrementing button and a label.'
            var counter = Var.of(0)
            var view = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.panel()
                    .add(UI.button("+").onClick({ counter.set(counter.get() + 1) }))
                    .add(UI.label(counter.viewAsString()))
                ).get(JPanel)
            })
            var button = (JButton) view.getComponent(0)
            var label  = (JLabel)  view.getComponent(1)

        when : 'Two producer threads each enqueue 150 increments, while the UI thread fires 150 button clicks.'
            var producers = (1..2).collect { producerNumber ->
                new Thread({
                    150.times {
                        EventProcessor.DECOUPLED.registerAppEvent({ counter.set(counter.get() + 1) })
                    }
                }, "producer-$producerNumber")
            }
            producers.each { it.start() }
            150.times {
                UI.runNow({ button.doClick() })
            }
            producers.each { it.join(10_000) }
        and : 'We wait for the application thread to work off the queue, and for the UI thread to catch up.'
            app.awaitAllProcessed()
            UI.sync()
        then : 'Every single one of the 450 increments arrived in the view model.'
            counter.is(450)
        and : 'The label displays the exact final state.'
            label.text == "450"
        and : 'No event task failed, and nothing was silently logged as an error along the way.'
            app.failures().isEmpty()
            log.errors().isEmpty()

        cleanup :
            app.stop()
            log.detach()

        where : 'We repeat this race a few times to explore different thread interleavings.'
            run << (1..5)
    }
}

/**
 *  A label which records every text update applied to it, together with
 *  the information whether it happened on the UI thread. This allows the
 *  specification above to observe the exact order in which bound property
 *  changes land on a component.
 */
@CompileDynamic
class RecordingLabel extends JLabel
{
    final List<String> applied = new CopyOnWriteArrayList<>()

    @Override
    void setText( String text ) {
        // Note: `applied` is still null while the superclass constructor runs.
        // Also note the fully qualified `swingtree.UI` here, because inside
        // a `JLabel` subclass, a bare `UI` resolves to the `getUI()` bean property!
        applied?.add("'$text' (UI thread: ${swingtree.UI.thisIsUIThread()})".toString())
        super.setText(text)
    }
}
