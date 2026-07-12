package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import sprouts.From
import sprouts.Tuple
import sprouts.Var
import sprouts.Vars
import swingtree.api.mvvm.BoundViewSupplier
import swingtree.api.mvvm.TabSupplier
import swingtree.threading.EventProcessor
import utility.ApplicationThread
import utility.LogSpy
import utility.SwingTreeTestConfigurator

import javax.swing.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

@Title("Property Binding Across Threads")
@Narrative('''

    Swing only knows one thread, the event dispatch thread (EDT),
    which performs both the rendering of the UI as well as the event handling.
    This is a problem for applications that need to perform long running
    tasks in the background, because the EDT is blocked until a task
    is complete, freezing the UI. The decoupled threading mode solves this
    by dispatching your business logic to a custom application thread, while
    ensuring that all UI related operations still happen on the EDT.
    This specification demonstrates how this threading model interacts
    with the MVVM pattern and the binding of properties to UI components.

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
@Subject([UIForAnySwing, UIForTabbedPane, EventProcessor])
@Timeout(60)
@CompileDynamic
class Decoupled_Property_Binding_Spec extends Specification
{
    /**
     *  A minimal identifiable view model for the tuple based sub-view bindings,
     *  which recycle their sub-views based on the `HasId.id()` of their items.
     */
    static record Item(String id, String text) implements sprouts.HasId<String> {}

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
            Here a little explanation is in order:
            Consider a property that is bound to a UI component, and that
            property is changed by the application thread. This notifies the
            property observers, which put the change procedure into the event
            queue of the UI thread. However, it might take a while before the
            UI thread gets around to processing the event, and until then the
            property might have changed again, maybe multiple times!
            Without further precautions, the sequence of changes arriving at
            the component might then differ from the sequence in which they
            were made to the property.

            SwingTree prevents this: each change is shipped to the UI thread
            together with the value the property carried *at the time of the
            change*, and the UI thread replays them in their original order.
            This test pins down both consequences: no intermediate value is
            skipped, and no stale value can overtake a newer one, even though
            the property already holds the final value long before the
            UI thread wakes up.

            To observe the individual updates as they land on the component,
            this test uses a label which records every `setText(..)` call
            together with the thread that performed it.
        """
        given : 'A recording label and a text property bound to it in decoupled mode.'
            var underlying = new RecordingLabel()
            var text = Var.of("Hello")
            var label = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> UI.of(underlying).withText(text)).get(JLabel)
            })
        and : 'We forget the text assignments made while building the declaration.'
            underlying.applied.clear()

        when : 'We change the property once and wait for the UI thread.'
            text.set("Hi")
            UI.sync()
        then : 'As expected, the component received exactly this one update.'
            underlying.applied == ["'Hi' (UI thread: true)"]

        when : 'We park the UI thread, fire four quick changes from this thread, and release the UI thread again.'
            underlying.applied.clear()
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            try {
                text.set("Hello")
                text.set("Hi")
                text.set("Hey")
                text.set("Hi")
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'Every single value arrived on the component, in its original order, and all of them on the UI thread.'
            underlying.applied == [
                "'Hello' (UI thread: true)",
                "'Hi' (UI thread: true)",
                "'Hey' (UI thread: true)",
                "'Hi' (UI thread: true)"
            ]
        and : 'The component settled on the final property value.'
            label.text == "Hi"
            text.is("Hi")
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

    def 'All kinds of bound widget state receive their property updates asynchronously, on the UI thread.'(
        String widgetState, Var property, Object newValue, Closure<JComponent> declare, Closure read
    ) {
        reportInfo """
            The SwingTree API offers countless ways to bind component state
            to properties: text, selection flags, slider values, selected
            combo box items, enabled and visible flags, colors, and much more.
            All of them cross the thread boundary through the same mechanism,
            and so all of them share the same contract, which this data driven
            feature verifies across a whole zoo of widgets and state types:

            A property change made by the application thread is **not**
            applied to the component synchronously. It is dispatched to the
            UI thread, and until that thread gets around to it, the component
            keeps its old state. Just like in the earlier features, we make
            this deterministic by parking the UI thread while we look
            at the in-flight update.
        """
        given : 'A widget bound to the property, built in decoupled mode. (See the `where` table for the current case!)'
            var component = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> declare(property))
            })
        expect : 'Initially the component state reflects the property value.'
            read(component) == property.get()

        when : 'We park the UI thread, change the property from this thread, and peek at the component before releasing the UI thread again.'
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            var stateRightAfterSet = null
            try {
                property.set(newValue)
                stateRightAfterSet = read(component)
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'Right after the property change, the component still had its old state...'
            stateRightAfterSet != newValue
        and : '...but as soon as the UI thread caught up, the new state arrived.'
            read(component) == newValue

        where : 'The same asynchronous update contract holds for all of the following widget states:'
            widgetState                  | property               | newValue      | declare                                                                                 | read
            'JLabel text'                | Var.of("Old")          | "New!"        | { p -> UI.label(p).get(JLabel) }                                                        | { c -> c.text }
            'JTextField text'            | Var.of("Old")          | "New!"        | { p -> UI.textField(p).get(JTextField) }                                                | { c -> c.text }
            'JCheckBox selection'        | Var.of(false)          | true          | { p -> UI.checkBox("Check me", p).get(JCheckBox) }                                      | { c -> c.selected }
            'JToggleButton selection'    | Var.of(false)          | true          | { p -> UI.toggleButton("Toggle me", p).get(JToggleButton) }                             | { c -> c.selected }
            'JSlider value'              | Var.of(10)             | 90            | { p -> UI.slider(UI.Align.HORIZONTAL).withMin(0).withMax(100).withValue(p).get(JSlider) } | { c -> c.value }
            'JProgressBar value'         | Var.of(10)             | 90            | { p -> UI.progressBar(0, 100, p).get(JProgressBar) }                                    | { c -> c.value }
            'JSpinner value'             | Var.of(5)              | 42            | { p -> UI.spinner(p).get(JSpinner) }                                                    | { c -> c.value }
            'JComboBox selection'        | Var.of("B")            | "C"           | { p -> UI.comboBox(p, ["A", "B", "C"]).get(JComboBox) }                                 | { c -> c.selectedItem }
            'JTabbedPane selected index' | Var.of(0)              | 1             | { p -> UI.tabbedPane().add(UI.tab("One")).add(UI.tab("Two")).withSelectedIndex(p).get(JTabbedPane) } | { c -> c.selectedIndex }
            'enabled flag'               | Var.of(true)           | false         | { p -> UI.button("Button").isEnabledIf(p).get(JButton) }                                | { c -> c.enabled }
            'visible flag'               | Var.of(true)           | false         | { p -> UI.button("Button").isVisibleIf(p).get(JButton) }                                | { c -> c.visible }
            'background color'           | Var.of(UI.Color.RED)   | UI.Color.BLUE | { p -> UI.label("Colorful!").withBackground(p).get(JLabel) }                            | { c -> c.background }
    }

    def 'A combo box selection made by the user travels to the application thread before it reaches the bound property.'()
    {
        reportInfo """
            The selection state of a combo box follows the same threading
            convention as every other bound widget state: the `JComboBox`
            works with a UI thread owned copy of the selection, and the bound
            property is only ever read and written by the application thread.

            So when the user picks an item, the combo box updates its own
            state immediately, but the new selection reaches the property
            asynchronously, through the application event queue. This has
            an important consequence for your view models: the `onChange`
            listeners of the selection property are guaranteed to run on
            the application thread, never on the UI thread, so your business
            logic cannot accidentally block or corrupt the UI.
        """
        given : 'A selection property with a change listener which records the thread it runs on.'
            var trace = new CopyOnWriteArrayList<String>()
            var selection = Var.of("B")
            selection.onChange(From.VIEW, it ->
                trace << "changed to '${it.currentValue().orElseThrowUnchecked()}' on '${Thread.currentThread().name}'".toString()
            )
        and : 'A combo box built around the property in decoupled mode.'
            var combo = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> UI.comboBox(selection, ["A", "B", "C"])).get(JComboBox)
            })
        expect : 'The combo box reports the initial selection.'
            combo.selectedItem == "B"

        when : 'The user selects "C", on the UI thread.'
            UI.runNow({ combo.selectedItem = "C" })
        then : 'The combo box itself shows the new selection right away, it is UI owned state...'
            combo.selectedItem == "C"
        and : '...but the property has not been touched yet! The write is waiting in the application event queue.'
            selection.is("B")
            trace.isEmpty()

        when : 'This test thread, playing the application thread, processes the event queue.'
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
        then : 'Now the property was updated, and its change listener ran on the application thread.'
            selection.is("C")
            trace == ["changed to 'C' on '${Thread.currentThread().name}'".toString()]
    }

    def 'The selectable options of a combo box, modelled as a tuple property, update asynchronously on the UI thread.'()
    {
        reportInfo """
            Not only the selection, but also the *options* of a combo box may
            live in your view model, for example as a `Var<Tuple<E>>` property
            bound through `withItems(selection, options)`.
            Since a `Tuple` is deeply immutable, the two threads simply publish
            their state to each other: the combo box works with a UI thread
            owned snapshot of the tuple, which is refreshed asynchronously
            whenever the property changes on the application thread.
        """
        given : 'A selection property and a tuple property of options.'
            var selection = Var.of("B")
            var options = Var.of(Tuple.of("A", "B", "C"))
        and : 'A combo box bound to both, built in decoupled mode.'
            var combo = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.comboBox().withItems(selection, options)
                ).get(JComboBox)
            })
        expect : 'The combo box offers the initial options.'
            combo.itemCount == 3

        when : 'We park the UI thread, add an option from this thread, and peek at the combo box before releasing the UI thread.'
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            // The EDT may not have reached the `await` yet, but that does not matter:
            // `invokeLater` tasks run in FIFO order, so the snapshot update published
            // by the mutation below is fenced behind the gate task either way.
            var itemCountWhileParked = -1
            try {
                options.update( it -> it.add("D") )
                itemCountWhileParked = combo.itemCount
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'While the UI thread was parked, the combo box still offered the old options...'
            itemCountWhileParked == 3
        and : '...but once it caught up, the new option appeared.'
            combo.itemCount == 4
            combo.getItemAt(3) == "D"
    }

    def 'The selectable options of a combo box, modelled as a `Vars` property list, update asynchronously on the UI thread.'()
    {
        reportInfo """
            The options of a combo box may also be modelled as a `Vars`
            property list, whose change events keep the UI thread owned
            snapshot of the combo box in sync, following the same
            asynchronous publication protocol as all other property
            based widget state.
        """
        given : 'A selection property and a property list of options.'
            var selection = Var.of("B")
            var options = Vars.of("A", "B", "C")
        and : 'A combo box bound to both, built in decoupled mode.'
            var combo = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.comboBox(selection, options)
                ).get(JComboBox)
            })
        expect : 'The combo box offers the initial options.'
            combo.itemCount == 3

        when : 'We park the UI thread, add an option from this thread, and peek at the combo box before releasing the UI thread.'
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            // The EDT may not have reached the `await` yet, but that does not matter:
            // `invokeLater` tasks run in FIFO order, so the snapshot update published
            // by the mutation below is fenced behind the gate task either way.
            var itemCountWhileParked = -1
            try {
                options.add("D")
                itemCountWhileParked = combo.itemCount
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'While the UI thread was parked, the combo box still offered the old options...'
            itemCountWhileParked == 3
        and : '...but once it caught up, the new option appeared.'
            combo.itemCount == 4
            combo.getItemAt(3) == "D"
    }

    def 'A tuple of tab models bound to a tabbed pane is kept in sync from the application thread.'()
    {
        reportInfo """
            Structural bindings are far more delicate than plain value
            bindings: when a tuple property of tab models changes, the
            tabbed pane is updated *incrementally*, based on a diff between
            the old and the new tuple, rather than being rebuilt from scratch.
            Across a thread boundary this only works if each change arrives
            on the UI thread together with the diff information belonging to
            exactly that change, and if the changes are replayed in their
            original order.

            To stress precisely that, this test parks the UI thread and fires
            a whole burst of structural changes, adding, removing and renaming
            tabs, from the application thread. Only then is the UI thread
            allowed to catch up and replay the queue.
        """
        given : 'A tuple property of tab titles bound to a tabbed pane in decoupled mode.'
            var models = Var.of(Tuple.of("Overview", "Details", "Settings"))
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
            var pane = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.tabbedPane().addAll(models, supplier)
                ).get(JTabbedPane)
            })
        expect : 'The pane starts out with one tab per model.'
            pane.tabCount == 3
            (0..2).collect({ pane.getTitleAt(it) }) == ["Overview", "Details", "Settings"]

        when : 'We park the UI thread and fire a burst of structural changes from this thread.'
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            var tabCountWhileParked = -1
            try {
                models.update( it -> it.add("Help") )
                models.update( it -> it.removeAt(0) )
                models.update( it -> it.setAt(0, "Detail View") )
                tabCountWhileParked = pane.tabCount
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'While the UI thread was parked, the pane still had its old structure.'
            tabCountWhileParked == 3
        and : 'After the UI thread replayed the queue, the pane matches the final tuple state exactly.'
            pane.tabCount == models.get().size()
            (0..2).collect({ pane.getTitleAt(it) }) == ["Detail View", "Settings", "Help"]
            models.get().toList() == ["Detail View", "Settings", "Help"]
    }

    def 'Boolean selection properties of individual tabs stay consistent when the selection crosses the thread boundary.'()
    {
        reportInfo """
            Each tab of a tabbed pane may be bound to its own boolean
            selection property. This is an especially tricky binding, because
            the properties are coupled to each other through the pane:
            selecting one tab must flip its property to true and the property
            of the previously selected tab to false.

            In the decoupled mode, this dance happens across threads, in both
            directions: a user selection on the UI thread must propagate
            outward to the properties, and a property change on the
            application thread must propagate inward to the pane, without
            the resulting updates echoing back and forth endlessly.
        """
        given : 'Three boolean selection properties, one per tab.'
            var overviewSelected = Var.of(false)
            var detailsSelected  = Var.of(false)
            var settingsSelected = Var.of(false)
        and : 'A tabbed pane in decoupled mode, each tab bound to its property.'
            var pane = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.tabbedPane()
                    .add(UI.tab("Overview").isSelectedIf(overviewSelected))
                    .add(UI.tab("Details").isSelectedIf(detailsSelected))
                    .add(UI.tab("Settings").isSelectedIf(settingsSelected))
                ).get(JTabbedPane)
            })
        expect : 'Since no property is true, nothing is selected initially.'
            pane.selectedIndex == -1

        when : 'The user selects the second tab on the UI thread, and both worlds settle.'
            UI.runNow({ pane.selectedIndex = 1 })
            letBothWorldsSettle()
        then : 'Exactly the second property is now true.'
            !overviewSelected.get() && detailsSelected.get() && !settingsSelected.get()

        when : 'The application thread flips the third selection flag, and both worlds settle.'
            settingsSelected.set(true)
            letBothWorldsSettle()
        then : 'The pane followed the property...'
            pane.selectedIndex == 2
        and : '...and the flag of the previously selected tab was flipped back to false.'
            !overviewSelected.get() && !detailsSelected.get() && settingsSelected.get()
    }

    def 'A bound selection index follows structural tab changes made by the application thread.'()
    {
        reportInfo """
            This scenario combines two bindings which must cooperate across
            the thread boundary: the tabs themselves are bound to a tuple of
            models, and the selection is bound to an integer index property.

            When the application thread inserts a new tab *before* the
            currently selected one, the selected tab shifts one position to
            the right. The pane must keep the same tab selected, and the new
            index must travel back into the index property, so that the view
            model never falls out of sync with what the user is looking at.
        """
        given : 'A tuple of tab models and a selection index property pointing at the last tab.'
            var models = Var.of(Tuple.of("A", "B", "C"))
            var selectedIndex = Var.of(2)
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
        and : 'A tabbed pane in decoupled mode with both bindings attached.'
            var pane = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.tabbedPane()
                    .withSelectedIndex(selectedIndex)
                    .addAll(models, supplier)
                ).get(JTabbedPane)
            })
        expect : 'The pane starts with tab "C" selected, as the index property demands.'
            pane.tabCount == 3
            pane.selectedIndex == 2
            pane.getTitleAt(pane.selectedIndex) == "C"

        when : 'The application thread inserts a new tab model at the very front, and both worlds settle.'
            models.update( it -> it.addAt(0, "Start") )
            letBothWorldsSettle()
        then : 'The pane now has four tabs and still shows the same selected tab, at its new position.'
            pane.tabCount == 4
            pane.selectedIndex == 3
            pane.getTitleAt(pane.selectedIndex) == "C"
        and : 'The new position also travelled back into the view model property.'
            selectedIndex.get() == 3
    }

    def 'A tuple of view models rendered as sub-views of a panel stays consistent under cross-thread updates.'()
    {
        reportInfo """
            Just like tabs, plain sub-views may be bound to a tuple property,
            with a view supplier creating one sub-view per item. The items are
            identified by their `HasId.id()` and every item is handed to the
            supplier as a property of its own, so there are two layers of
            binding at work here: structural changes to the tuple add or
            remove whole sub-views, while replacing an item with an updated
            model carrying the *same id* recycles the existing sub-view and
            channels the new state into it through its item property.
            All of it crosses the thread boundary in the decoupled mode.

            Note that just like in the other features, we park the UI thread
            while firing a burst of updates from the application thread.
            This proves two things at once: the updates are applied
            asynchronously and in order, and mutating the bound tuple never
            requires the application thread to wait for the UI thread.
            (Historically it did! The item properties used to locate their
            items by consulting the component tree, so a mere `update(..)`
            call could block on, or even deadlock with, the UI thread.)
        """
        given : 'A tuple property of little view models, rendered as labels inside a panel.'
            var models = Var.of(Tuple.of(
                new Item("a", "Alpha"),
                new Item("b", "Beta"),
                new Item("c", "Gamma")
            ))
            BoundViewSupplier<Item> supplier = { Var<Item> model -> UI.label(model.viewAsString({ it.text() })) }
            var panel = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.panel().addAll(models, supplier)
                ).get(JPanel)
            })
        expect : 'The panel renders one label per item.'
            panel.componentCount == 3
            _textsOf(panel) == ["Alpha", "Beta", "Gamma"]

        when : 'We remember the sub-view of item "a" and then park the UI thread while firing a burst of updates from this thread.'
            var viewOfItemA = panel.getComponent(0)
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            try {
                models.update( it -> it.removeAt(1) )
                models.update( it -> it.addAll(new Item("d", "Delta"), new Item("e", "Epsilon")) )
                models.update( it -> it.setAt(0, new Item("a", "Alef")) ) // <- same id, new text!
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'The panel replayed all changes in order and matches the final tuple state.'
            panel.componentCount == models.get().size()
            _textsOf(panel) == ["Alef", "Gamma", "Delta", "Epsilon"]
            models.get().toList().collect({ it.text() }) == ["Alef", "Gamma", "Delta", "Epsilon"]
        and : 'The update of item "a" was channeled into its already existing sub-view, no new view was created for it.'
            panel.getComponent(0).is(viewOfItemA)
    }

    def 'The `scrollPanels` component handles cross-thread tuple updates just like a plain panel.'()
    {
        reportInfo """
            The `UI.scrollPanels()` component is the scrollable sibling of a
            tuple bound panel: it wraps each generated sub-view in its own
            internal entry panel. It shares the id based item binding with
            the plain panel, so the same threading contract applies, which
            this feature verifies with the same parked burst of updates
            fired from the application thread.
        """
        given : 'A tuple property of view models, rendered as labels inside scroll panels.'
            var models = Var.of(Tuple.of(
                new Item("a", "Alpha"),
                new Item("b", "Beta"),
                new Item("c", "Gamma")
            ))
            BoundViewSupplier<Item> supplier = { Var<Item> model -> UI.label(model.viewAsString({ it.text() })) }
            var panels = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.scrollPanels().addAll(models, supplier)
                ).get(swingtree.components.JScrollPanels)
            })
        expect : 'The component renders one entry per item.'
            panels.numberOfEntries == 3

        when : 'We park the UI thread and fire a burst of structural changes and item updates from this thread.'
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            try {
                models.update( it -> it.removeAt(1) )
                models.update( it -> it.addAll(new Item("d", "Delta"), new Item("e", "Epsilon")) )
                models.update( it -> it.setAt(0, new Item("a", "Alef")) ) // <- same id, new text!
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'The entries replayed all changes in order and match the final tuple state.'
            panels.numberOfEntries == models.get().size()
            _entryTextsOf(panels) == ["Alef", "Gamma", "Delta", "Epsilon"]
    }

    /**
     *  In the decoupled mode, an update may need several hops to come to rest:
     *  a property change hops to the UI thread, where applying it may trigger
     *  a write-back which hops to the application event queue, and so on.
     *  This helper alternates between flushing the UI thread (`UI.sync()`) and
     *  draining the application event queue until such ping-pong has provably
     *  come to rest — three full rounds cover more hops than any healthy
     *  binding needs, and a binding which ping-pongs forever fails
     *  this specification's global timeout instead of hanging the build.
     */
    private static void letBothWorldsSettle() {
        3.times {
            UI.sync()
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
        }
        UI.sync()
    }

    private static List<String> _textsOf( JPanel panel ) {
        return (0..<panel.componentCount).collect({ ((JLabel) panel.getComponent(it)).text })
    }

    private static List<String> _entryTextsOf( swingtree.components.JScrollPanels panels ) {
        // Scroll panels wrap every generated sub-view in an internal entry panel:
        var content = panels.getContentPanel()
        return (0..<content.componentCount).collect({
            var entryPanel = (java.awt.Container) content.getComponent(it)
            return ((JLabel) entryPanel.getComponent(0)).text
        })
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
