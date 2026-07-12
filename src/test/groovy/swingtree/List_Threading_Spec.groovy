package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import sprouts.Var
import sprouts.Vars
import swingtree.threading.EventProcessor
import utility.LogSpy
import utility.SwingTreeTestConfigurator

import javax.swing.JList
import java.util.concurrent.CountDownLatch

@Title("Lists Across Threads")
@Narrative('''

    A `JList` may be bound to a `Vals`/`Vars` property list of entries,
    as well as to a selection property. Both bindings follow the thread
    safe publication protocol of the decoupled threading mode:
    the list model works with a UI thread owned snapshot of the entries,
    which is refreshed through the change events of the bound property
    list, while a selection made by the user in the view is handed over
    to the application thread, which writes it into the selection
    property. The two threads never wait for one another.

    The features in this specification are regression guards against
    deadlocks and data races: every feature runs under a timeout, so if
    a blocking handover between the two threads ever sneaks back into
    the list binding machinery, this specification fails loudly instead
    of hanging the build.

''')
@Subject([UIForList, EventProcessor])
@Timeout(60)
@CompileDynamic
class List_Threading_Spec extends Specification
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

    def 'Changing the bound entries of a list from the application thread never waits for the UI thread.'()
    {
        reportInfo """
            The application thread must be able to update the entry property
            list of a `JList` at any time, even while the UI thread is
            completely unresponsive. To prove this, we park the UI thread
            behind a latch and mutate the entries from this thread: the
            mutation must return immediately (if it secretly waited for the
            UI thread, this feature would hang and trip its timeout), and
            the list must keep offering its old snapshot of the entries
            until the UI thread is released and catches up.
        """
        given : 'A selection property, an entry property list, and a list view built in decoupled mode.'
            var selection = Var.of("B")
            var entries = Vars.of("A", "B", "C")
            var list = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> UI.list(selection, entries)).get(JList)
            })
        expect : 'The list model offers the three initial entries.'
            list.model.size == 3

        when : 'We park the UI thread and add an entry from this thread, peeking at the list model before releasing the UI thread.'
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            // The EDT may not have reached the `await` yet, but that does not matter:
            // `invokeLater` tasks run in FIFO order, so the snapshot update published
            // by the mutation below is fenced behind the gate task either way.
            var entryCountWhileParked = -1
            try {
                entries.add("D")
                entryCountWhileParked = list.model.size
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'The mutation returned while the UI thread was still parked, and the list still offered the old entries...'
            entryCountWhileParked == 3
        and : '...but once the UI thread caught up, the new entry appeared.'
            list.model.size == 4
            list.model.getElementAt(3) == "D"
    }

    def 'Rendering a list always sees an internally consistent snapshot, even during an entry mutation storm.'()
    {
        reportInfo """
            Swing consults `getSize()` and `getElementAt(i)` whenever it
            paints a `JList`, and it may do so at any time. Historically
            these reads went straight into the live property list, so a
            mutation on the application thread could shift the entries
            between the size query and the element access, producing
            phantom nulls, torn entry lists or even exceptions in the
            middle of painting.

            With the snapshot protocol, all reads within one UI thread task
            are served from a single immutable snapshot. This feature
            hammers the entries from this thread while the UI thread
            continuously reads the model like a renderer would, and asserts
            that not a single read round ever observes an inconsistency.
        """
        given : 'A log spy, so that silently logged errors fail this test.'
            var log = LogSpy.attach()
        and : 'An entry property list and a list view built in decoupled mode.'
            var entries = Vars.of("A", "B", "C")
            var list = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> UI.list(entries)).get(JList)
            })
        and : 'A list collecting every inconsistency a render pass may observe.'
            var violations = new java.util.concurrent.CopyOnWriteArrayList<String>()

        when : 'We fire 60 mutation steps from this thread, and after each one, a full renderer style read pass runs on the UI thread.'
            60.times { step ->
                if ( entries.size() < 6 )
                    entries.add("entry-$step".toString())
                else
                    entries.removeAt(0, 3)
                UI.runNow({
                    int count = list.model.size
                    for ( int i = 0; i < count; i++ ) {
                        if ( list.model.getElementAt(i) == null )
                            violations.add("Phantom null at index $i of $count in step $step!".toString())
                    }
                })
            }
            UI.sync()
        then : 'Not a single render pass observed a torn snapshot.'
            violations.isEmpty()
        and : 'No exception was silently logged along the way.'
            log.errors().isEmpty()
        and : 'After everything settled, the list agrees with the final state of the entries.'
            list.model.size == entries.size()
            (0..<list.model.size).collect({ list.model.getElementAt(it) }) == entries.toList()

        cleanup :
            log.detach()
    }

    def 'A selection made by the user in a list waits in the application event queue instead of touching the property directly.'()
    {
        reportInfo """
            When the user selects a list entry, the UI thread must not
            write into the bound selection property itself, because the
            property belongs to the application thread, whose listeners
            expect to never be invoked from the UI thread. Instead, the
            new selection is handed over to the application event queue.

            In this feature nobody processes that queue at first, which
            makes the handover visible: the list already shows the new
            selection, while the property still holds the old one. Only
            when this thread volunteers to process the events does the
            new selection reach the view model, on this very thread.
        """
        given : 'A selection property with a change listener which records the thread it runs on.'
            var trace = new java.util.concurrent.CopyOnWriteArrayList<String>()
            var selection = Var.of("B")
            selection.onChange(sprouts.From.VIEW, it ->
                trace << "changed to '${it.currentValue().orElseThrowUnchecked()}' on '${Thread.currentThread().name}'".toString()
            )
        and : 'A list view over three entries, built in decoupled mode.'
            var entries = Vars.of("A", "B", "C")
            var list = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> UI.list(selection, entries)).get(JList)
            })
        expect : 'The list starts out with the initial selection of the property.'
            UI.runAndGet({ list.selectedValue }) == "B"

        when : 'The user selects the third entry, on the UI thread.'
            UI.runNow({ list.setSelectedIndex(2) })
        then : 'The list itself shows the new selection right away, it is UI owned state...'
            UI.runAndGet({ list.selectedValue }) == "C"
        and : '...but the property has not been touched yet! The write is waiting in the application event queue.'
            selection.is("B")
            trace.isEmpty()

        when : 'This test thread, playing the application thread, processes the event queue.'
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
        then : 'Now the property was updated, and its change listener ran on the application thread.'
            selection.is("C")
            trace == ["changed to 'C' on '${Thread.currentThread().name}'".toString()]
    }
}
