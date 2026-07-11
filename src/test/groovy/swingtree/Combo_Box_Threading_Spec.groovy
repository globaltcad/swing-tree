package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import sprouts.Tuple
import sprouts.Var
import sprouts.Vars
import swingtree.threading.EventProcessor
import utility.ApplicationThread
import utility.LogSpy
import utility.SwingTreeTestConfigurator

import javax.swing.JComboBox
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

@Title("Combo Boxes Across Threads")
@Narrative('''

    The data model of a SwingTree combo box comes in five flavours, depending
    on what you hand to the `comboBox(..)` factories or the `withItems(..)`
    methods: a plain array, a plain list, a `Vars` property list, a read only
    `Vals` property list, or a `Tuple` based property.

    The three property based models follow the thread safe publication
    protocol of the decoupled threading mode: the combo box works with a
    UI thread owned snapshot of the options, which is refreshed through the
    change events of the bound properties, while everything the user does
    in the view is handed over to the application thread, which writes it
    into the properties. The two threads never wait for one another.

    The plain array and list models are different by (documented) design:
    a bare collection has no change events, so the combo box reads it live,
    whenever it is rendered. This is convenient for static option sets, but
    it is shared mutable state, which is why dynamic, application thread
    owned options should be modelled with the property based flavours.

    The features in this specification are first and foremost regression
    guards against deadlocks and data races: every feature runs under a
    timeout, so if a blocking handover between the two threads ever sneaks
    back into the combo box machinery, this specification fails loudly
    instead of hanging the build.

''')
@Subject([UIForCombo, AbstractComboModel, EventProcessor])
@Timeout(60)
@CompileDynamic
class Combo_Box_Threading_Spec extends Specification
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

    def 'Changing property based combo box options from the application thread never waits for the UI thread. [#kind]'(
        String kind, Closure declare, Closure addOption
    ) {
        reportInfo """
            The application thread must be able to update the option properties
            of a combo box at any time, even while the UI thread is completely
            unresponsive. To prove this, we park the UI thread behind a latch
            and mutate the options from this thread: the mutation must return
            immediately (if it secretly waited for the UI thread, this feature
            would hang and trip its timeout), and the combo box must keep
            offering its old snapshot of the options until the UI thread
            is released and catches up.
        """
        given : 'A selection property and a combo box built in decoupled mode. (See the `where` block for the current model flavour!)'
            var selection = Var.of("B")
            var combo = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> declare(selection)).get(JComboBox)
            })
        expect : 'The combo box offers the three initial options.'
            combo.itemCount == 3

        when : 'We park the UI thread and add an option from this thread, peeking at the combo box before releasing the UI thread.'
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            var itemCountWhileParked = -1
            try {
                addOption()
                itemCountWhileParked = combo.itemCount
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'The mutation returned while the UI thread was still parked, and the combo box still offered the old options...'
            itemCountWhileParked == 3
        and : '...but once the UI thread caught up, the new option appeared.'
            combo.itemCount == 4
            combo.getItemAt(3) == "D"

        where : 'All three property based option models behave identically:'
            [kind, declare, addOption] << [
                _tupleOptionsScenario(),
                _varsOptionsScenario(),
                _valsOptionsScenario()
            ]
    }

    def 'Plain array and list based options are shared live state, visible without any UI thread involvement. [#kind]'(
        String kind, Closure declare, Closure mutate, String expectedFirstItem
    ) {
        reportInfo """
            The plain array and list models are the deliberate exception to
            the snapshot protocol: a bare collection has no change events,
            so the combo box model reads it live. This means a mutation is
            visible to whoever asks the model *immediately*, even while the
            UI thread is parked, and it also means the collection is shared
            mutable state whose safe publication is the responsibility
            of the application. For dynamic options, prefer the property
            based models, which are fully thread safe.
        """
        given : 'A selection property and a combo box built in decoupled mode around a plain collection.'
            var selection = Var.of("B")
            var combo = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> declare(selection)).get(JComboBox)
            })
        expect :
            combo.getItemAt(0) == "A"

        when : 'We park the UI thread and mutate the raw collection from this thread.'
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            var firstItemWhileParked = null
            try {
                mutate()
                firstItemWhileParked = combo.getItemAt(0)
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'The mutation was visible immediately, no UI thread required, because the model reads the collection live.'
            firstItemWhileParked == expectedFirstItem
            combo.getItemAt(0) == expectedFirstItem

        where : 'Both plain collection models share this live read contract:'
            [kind, declare, mutate, expectedFirstItem] << [
                _arrayOptionsScenario(),
                _listOptionsScenario()
            ]
    }

    def 'The UI thread never waits for the application thread, even when nobody is processing application events. [#kind]'(
        String kind, Closure declare
    ) {
        reportInfo """
            This is the mirror image of the previous deadlock guards:
            everything the UI thread does with a combo box must complete
            immediately, out of its own UI thread owned state, even when the
            application thread is stuck or does not exist at all.
            In this feature, nobody drains the application event queue while
            the user selects an option and the combo box is read like a
            renderer would during painting. If any of this secretly waited
            for the application thread, the feature would hang and
            trip its timeout.

            Only after this thread volunteers to process the queue does the
            new selection reach the bound property, which is also the
            guarantee that view model listeners never run on the UI thread.
        """
        given : 'A selection property and a combo box built in decoupled mode. (See the `where` block for the current model flavour!)'
            var selection = Var.of("B")
            var combo = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> declare(selection)).get(JComboBox)
            })

        when : 'With no application thread in sight, the user selects another option, and we read the combo box the way a renderer would.'
            var itemsSeenByRenderer = UI.runAndGet({
                combo.setSelectedItem("C")
                return (0..<combo.itemCount).collect({ combo.getItemAt(it) })
            })
        then : 'All of it completed immediately: the combo box shows the new selection and served all options from its own state.'
            combo.selectedItem == "C"
            itemsSeenByRenderer == ["A", "B", "C"]
        and : 'The bound property however is still untouched, its update is waiting in the application event queue.'
            selection.is("B")

        when : 'This test thread takes the role of the application thread and processes the queue.'
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
        then : 'Now the new selection arrived in the view model.'
            selection.is("C")

        where : 'This contract holds for every single combo box model flavour:'
            [kind, declare] << [
                _arrayOptionsScenario(),
                _listOptionsScenario(),
                _tupleOptionsScenario(),
                _varsOptionsScenario(),
                _valsOptionsScenario()
            ].collect({ it.subList(0, 2) })
    }

    def 'Rendering a combo box always sees an internally consistent snapshot, even during an option mutation storm. [#kind]'(
        String kind, Closure declare, Closure mutateStep
    ) {
        reportInfo """
            Swing consults `getSize()` and `getElementAt(i)` whenever it
            paints a combo box, and it may do so at any time. Historically
            these reads went straight into the live property state, so a
            mutation on the application thread could shift the items between
            the size query and the element access, producing phantom nulls,
            torn option lists or even exceptions in the middle of painting.

            With the snapshot protocol, all reads within one UI thread task
            are served from a single immutable snapshot. This feature hammers
            the options from this thread while the UI thread continuously
            reads the model like a renderer would, and asserts that not a
            single read round ever observes an inconsistency.
        """
        given : 'A log spy, so that silently logged errors fail this test.'
            var log = LogSpy.attach()
        and : 'A combo box built in decoupled mode, with its options under mutation pressure.'
            var selection = Var.of("B")
            var combo = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> declare(selection)).get(JComboBox)
            })
        and : 'A list collecting every inconsistency a render pass may observe.'
            var violations = new CopyOnWriteArrayList<String>()

        when : 'We fire 60 mutation steps from this thread, and after each one, a full renderer style read pass runs on the UI thread.'
            60.times { step ->
                mutateStep(step)
                UI.runNow({
                    int count = combo.itemCount
                    for ( int i = 0; i < count; i++ ) {
                        if ( combo.getItemAt(i) == null )
                            violations.add("Phantom null at index $i of $count in step $step!".toString())
                    }
                })
            }
            UI.sync()
        then : 'Not a single render pass observed a torn snapshot.'
            violations.isEmpty()
        and : 'No exception was silently logged along the way.'
            log.errors().isEmpty()
        and : 'After everything settled, the combo box agrees with the final state of the options.'
            combo.itemCount >= 1

        cleanup :
            log.detach()

        where : 'The mutation storm is survived by all three property based models:'
            [kind, declare, mutateStep] << [
                _tupleStormScenario(),
                _varsStormScenario(),
                _valsStormScenario()
            ]
    }

    def 'An option edit which arrives after the options changed is dropped gracefully.'()
    {
        reportInfo """
            When the user edits an option through an editable combo box, the
            new item value is applied to the UI owned snapshot right away,
            while the write into the bound property list travels through the
            application event queue. By the time it arrives there, the world
            may have moved on: if the edited position no longer exists,
            the stale edit must be dropped silently instead of corrupting
            unrelated state or throwing in the middle of the event queue.
        """
        given : 'A log spy, a selection property, an option list, and an editable combo box in decoupled mode.'
            var log = LogSpy.attach()
            var selection = Var.of("C")
            var options = Vars.of("A", "B", "C")
            var combo = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.comboBox(selection, options).isEditableIf(true)
                ).get(JComboBox)
            })

        when : 'The user edits the selected option, which sits at the very end of the list.'
            UI.runNow({ combo.editor.item = "XY" })
        and : 'Before any application thread processed the edit, the option list shrinks below the edited position.'
            options.removeLast(2) // [A, B, C] -> [A]
        and : 'Only now does this thread process the application event queue, and we let both worlds settle.'
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
            letBothWorldsSettle()
        then : 'The stale edit was dropped: the shrunken option list does not contain it.'
            options.toList() == ["A"]
        and : 'The combo box agrees with the final state of the options.'
            combo.itemCount == 1
            combo.getItemAt(0) == "A"
        and : 'The edit still went through as the current selection, because selection and options are independent states.'
            selection.is("XY")
        and : 'Nothing was silently logged as an error.'
            log.errors().isEmpty()

        cleanup :
            log.detach()
    }

    def 'Selections and option mutations racing from both sides of the thread boundary always converge.'(int run)
    {
        reportInfo """
            The final exam: a continuously draining application thread, this
            thread enqueueing a stream of option list mutations, and the UI
            thread selecting options at the same time, all interleaving
            freely. No matter how the threads interleave, once all queues
            have run dry, the combo box and the view model must agree on
            *everything*: the offered options and the current selection.
            The feature is repeated to explore different interleavings.
        """
        given : 'A log spy and a continuously draining application thread.'
            var log = LogSpy.attach()
            var app = ApplicationThread.startDraining()
        and : 'A selection property, an option list, and a combo box in decoupled mode.'
            var selection = Var.of("start")
            var options = Vars.of("start")
            var combo = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> UI.comboBox(selection, options)).get(JComboBox)
            })

        when : 'We enqueue 100 option mutations for the application thread, while the UI thread performs 50 user selections.'
            100.times { step ->
                EventProcessor.DECOUPLED.registerAppEvent({
                    if ( step % 2 == 0 )
                        options.add("option-$step".toString())
                    else if ( options.size() > 1 )
                        options.removeAt(0)
                })
            }
            50.times { step ->
                UI.runNow({
                    int count = combo.itemCount
                    if ( count > 0 )
                        combo.setSelectedIndex(step % count)
                })
            }
        and : 'We wait for the application thread to work off the queue, and let both worlds settle.'
            app.awaitAllProcessed()
            letBothWorldsSettle()
        then : 'The combo box offers exactly the final options of the view model.'
            combo.itemCount == options.size()
            (0..<combo.itemCount).collect({ combo.getItemAt(it) }) == options.toList()
        and : 'Both worlds agree on the selection.'
            combo.selectedItem == selection.orElseNull()
        and : 'No event task failed and nothing was silently logged as an error.'
            app.failures().isEmpty()
            log.errors().isEmpty()

        cleanup :
            app.stop()
            log.detach()

        where : 'We repeat this race a few times to explore different thread interleavings.'
            run << (1..5)
    }

    /*
        The scenarios below each create their own option holder and return
        the name of the model flavour, a declaration function (taking the
        selection property), and a mutation function operating on the holder.
    */

    private static List _tupleOptionsScenario() {
        var options = Var.of(Tuple.of("A", "B", "C"))
        return [
            'Tuple property based options',
            { Var sel -> UI.comboBox().withItems(sel, options) },
            { options.update( tuple -> tuple.add("D") ) }
        ]
    }

    private static List _varsOptionsScenario() {
        var options = Vars.of("A", "B", "C")
        return [
            'Vars based options',
            { Var sel -> UI.comboBox(sel, options) },
            { options.add("D") }
        ]
    }

    private static List _valsOptionsScenario() {
        var options = Vars.of("A", "B", "C")
        return [
            'Vals based options',
            { Var sel -> UI.comboBox().withModel(new ValsBasedComboModel<>(sel, options)) },
            // ^ We construct the model directly so that dynamic dispatch cannot pick the `Vars` flavour.
            { options.add("D") }
        ]
    }

    private static List _arrayOptionsScenario() {
        var options = new String[]{ "A", "B", "C" }
        return [
            'plain array based options',
            { Var sel -> UI.comboBox().withItems(sel, options) },
            { options[0] = "D" },
            "D"
        ]
    }

    private static List _listOptionsScenario() {
        var options = new ArrayList<>(["A", "B", "C"])
        return [
            'plain list based options',
            { Var sel -> UI.comboBox(sel, options) },
            { options.add(0, "D") },
            "D"
        ]
    }

    private static List _tupleStormScenario() {
        var options = Var.of(Tuple.of("A", "B", "C"))
        return [
            'Tuple property based options',
            { Var sel -> UI.comboBox().withItems(sel, options) },
            { int step -> options.update( tuple -> tuple.size() < 6 ? tuple.add("option-$step".toString()) : tuple.removeAt(0, 3) ) }
        ]
    }

    private static List _varsStormScenario() {
        var options = Vars.of("A", "B", "C")
        return [
            'Vars based options',
            { Var sel -> UI.comboBox(sel, options) },
            { int step -> if ( options.size() < 6 ) options.add("option-$step".toString()) else options.removeAt(0, 3) }
        ]
    }

    private static List _valsStormScenario() {
        var options = Vars.of("A", "B", "C")
        return [
            'Vals based options',
            { Var sel -> UI.comboBox().withModel(new ValsBasedComboModel<>(sel, options)) },
            { int step -> if ( options.size() < 6 ) options.add("option-$step".toString()) else options.removeAt(0, 3) }
        ]
    }

    /**
     *  In the decoupled mode, an update may need several hops to come to rest:
     *  a property change hops to the UI thread, where applying it may trigger
     *  a write-back which hops to the application event queue, and so on.
     *  This helper alternates between flushing the UI thread and draining the
     *  application event queue until such ping-pong has provably come to rest.
     */
    private static void letBothWorldsSettle() {
        3.times {
            UI.sync()
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
        }
        UI.sync()
    }
}
