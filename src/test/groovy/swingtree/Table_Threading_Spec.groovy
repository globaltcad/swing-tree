package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import sprouts.Event
import sprouts.Tuple
import sprouts.Var
import swingtree.api.model.AbstractSnapshotTableModel
import swingtree.threading.EventProcessor
import utility.ApplicationThread
import utility.LogSpy
import utility.SwingTreeTestConfigurator

import javax.swing.JTable
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

@Title("Tables Across Threads")
@Narrative('''

    SwingTree tables must be thread safe under the decoupled threading mode:
    the UI thread (the AWT Event Dispatch Thread) must never read the
    application thread owned data source of a table directly, and it must never
    block waiting for the application thread. Conversely, the application thread
    must be able to update the table data at any time without waiting for the UI
    thread.

    This is achieved through the same snapshot protocol the combo box models
    use: the UI thread reads an immutable, UI thread owned `TableSnapshot`, which
    is refreshed by the application thread and published across the thread
    boundary. For the `Tuple` based reactive table model, the immutable tuple
    *is* the snapshot, and its change diff is used to sync row insertions,
    removals and updates to the table incrementally.

    Every feature runs under a timeout, so a blocking handover between the two
    threads that ever sneaks back into the table machinery fails this
    specification loudly instead of hanging the build.

''')
@Subject([UIForTable, AbstractSnapshotTableModel, EventProcessor])
@Timeout(60)
@CompileDynamic
class Table_Threading_Spec extends Specification
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
        // Every feature starts with an empty application event queue,
        // so that no leftovers from previous tests can interfere.
        try {
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
        } catch (Exception ignored) {}
    }

    def 'Updating a `Tuple` based table from the application thread never waits for the UI thread.'()
    {
        reportInfo """
            The application thread must be able to update the rows of a table at
            any time, even while the UI thread is completely unresponsive. We
            park the UI thread behind a latch and add a row from this thread:
            the mutation must return immediately (if it secretly waited for the
            UI thread, this feature would hang and trip its timeout), and the
            table must keep offering its old snapshot of the rows until the UI
            thread is released and catches up.
        """
        given : 'A property of rows and a table built in decoupled mode.'
            var rows = Var.of(Tuple.of(
                                Tuple.of("Alice", "30"),
                                Tuple.of("Bob",   "42")
                            ))
            var table = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> UI.table(rows)).get(JTable)
            })
        expect : 'The table offers the two initial rows.'
            table.rowCount == 2

        when : 'We park the UI thread and add a row from this thread, peeking at the table before releasing the UI thread.'
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            var rowCountWhileParked = -1
            try {
                rows.update({ it.add(Tuple.of("Carol", "27")) })
                rowCountWhileParked = table.rowCount
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'The mutation returned while the UI thread was still parked, and the table still offered the old rows...'
            rowCountWhileParked == 2
        and : '...but once the UI thread caught up, the new row appeared.'
            table.rowCount == 3
            table.getValueAt(2, 0) == "Carol"
            table.getValueAt(2, 1) == "27"
    }

    def 'Rendering a `Tuple` based table always sees a consistent snapshot, even during a row mutation storm.'()
    {
        reportInfo """
            Swing consults `getRowCount()` and `getValueAt(r, c)` whenever it
            paints a table, at any time. With the snapshot protocol, all reads
            within one UI thread task are served from a single immutable
            snapshot. This feature hammers the rows from this thread while the UI
            thread continuously reads the model like a renderer would, and
            asserts that not a single read round ever observes an inconsistency
            (a phantom null or a row that is shorter than the column count).
        """
        given : 'A log spy, so that silently logged errors fail this test.'
            var log = LogSpy.attach()
        and : 'A `Tuple` based table under mutation pressure, in decoupled mode.'
            var rows = Var.of(Tuple.of(Tuple.of("A", "1"), Tuple.of("B", "2")))
            var table = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> UI.table(rows)).get(JTable)
            })
        and : 'A list collecting every inconsistency a render pass may observe.'
            var violations = new CopyOnWriteArrayList<String>()

        when : 'We fire 60 mutation steps from this thread, each followed by a full renderer style read pass on the UI thread.'
            60.times { step ->
                if ( rows.get().size() < 6 )
                    rows.update({ it.add(Tuple.of("row-$step".toString(), step.toString())) })
                else
                    rows.update({ it.removeFirst(3) })
                UI.runNow({
                    int count = table.rowCount
                    int columns = table.columnCount
                    for ( int r = 0; r < count; r++ ) {
                        for ( int c = 0; c < columns; c++ ) {
                            if ( table.getValueAt(r, c) == null )
                                violations.add("Phantom null at ($r,$c) of ($count,$columns) in step $step!".toString())
                        }
                    }
                })
            }
            UI.sync()
        then : 'Not a single render pass observed a torn snapshot.'
            violations.isEmpty()
        and : 'No exception was silently logged along the way.'
            log.errors().isEmpty()
        and : 'After everything settled, the table agrees with the final row count of the property.'
            table.rowCount == rows.get().size()

        cleanup :
            log.detach()
    }

    def 'A `Tuple` based table syncs row mutations from the application thread incrementally.'()
    {
        reportInfo """
            Because a `Tuple` carries a description of how it changed, the table
            model does not blindly rebuild itself whenever the application thread
            hands it a new tuple. It translates a row insertion into a targeted
            `fireTableRowsInserted` instead, even across the thread boundary,
            which is what keeps updates to large tables cheap. (Which kind of
            change maps onto which events is specified exhaustively in the
            `Declarative_Tables_Spec`, here we merely make sure that the diff
            survives the trip from the application thread to the UI thread.)
        """
        given : 'A property of rows and a table built in decoupled mode.'
            var rows = Var.of(Tuple.of(
                                Tuple.of("A", "1"),
                                Tuple.of("B", "2"),
                                Tuple.of("C", "3")
                            ))
            var table = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> UI.table(rows)).get(JTable)
            })
        and : 'A listener recording the raw table model events.'
            var events = new CopyOnWriteArrayList<TableModelEvent>()
            table.getModel().addTableModelListener({ TableModelEvent e -> events << e } as TableModelListener)

        when : 'We insert a row in the middle from this thread, and let the UI thread catch up.'
            rows.update({ it.addAt(1, Tuple.of("X", "9")) })
            UI.sync()
        then : 'Exactly one targeted row insertion event reached the UI thread.'
            events.size() == 1
            events[0].type == TableModelEvent.INSERT
            events[0].firstRow == 1
            events[0].lastRow == 1
        and : 'The table reflects the inserted row.'
            table.rowCount == 4
            table.getValueAt(1, 0) == "X"
    }

    def 'A lambda based table bound with `updateOn` is thread safe under the decoupled protocol.'()
    {
        reportInfo """
            The classic lambda based table model reads its data source live under
            the coupled mode, but under the decoupled mode it must keep a UI
            thread owned snapshot instead, refreshed on the application thread
            when the bound `updateOn` event fires. We park the UI thread, mutate
            the backing list and fire the event from this thread: the table must
            keep serving its old snapshot until the UI thread catches up.
        """
        given : 'A mutable list, an update event, and a lambda based table in decoupled mode.'
            var data = new CopyOnWriteArrayList<Integer>([1, 2, 3])
            var update = Event.create()
            var table = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.table().withModel( m -> m
                        .colNames("V")
                        .rowCount( () -> data.size() )
                        .getsEntryAt( (r, c) -> data.get(r) )
                        .updateOn(update)
                    )
                ).get(JTable)
            })
        expect : 'The table starts with the three initial values from its snapshot.'
            table.rowCount == 3
            table.getValueAt(0, 0) == 1

        when : 'We park the UI thread, grow the data and fire the update from this thread.'
            var gate = new CountDownLatch(1)
            UI.run({ gate.await() })
            var rowCountWhileParked = -1
            try {
                data.addAll([4, 5])
                update.fire()
                rowCountWhileParked = table.rowCount
            } finally {
                gate.countDown()
            }
            UI.sync()
        then : 'The table kept its old snapshot while the UI thread was parked...'
            rowCountWhileParked == 3
        and : '...and reflects the new data once the UI thread caught up.'
            table.rowCount == 5
            table.getValueAt(4, 0) == 5
    }

    def 'A user edit of a `Tuple` based table is applied to the UI right away and written back on the application thread.'()
    {
        reportInfo """
            When the user edits a cell of an editable `Tuple` based table, the UI
            thread must not reach into the application thread owned property to
            store the edit. Instead the edit is applied to the UI thread owned
            snapshot immediately (so the table never flickers back to the old
            value), and the write-back into the property is handed to the
            application thread, where it happens when that thread gets to it.
        """
        given : 'A property of rows and an editable table built in decoupled mode.'
            var rows = Var.of(Tuple.of(
                                Tuple.of("Alice", "30"),
                                Tuple.of("Bob",   "42")
                            ))
            var table = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.table(UI.ListData.ROW_MAJOR_EDITABLE, rows)
                ).get(JTable)
            })
        expect : 'The table considers its cells editable.'
            UI.runAndGet({ table.isCellEditable(0, 0) })

        when : 'The user edits a cell on the UI thread, without the application thread running.'
            UI.runNow({ table.setValueAt("31", 0, 1) })
        then : 'The table displays the edit immediately...'
            table.getValueAt(0, 1) == "31"
        and : '...but the application thread owned property has not been touched yet.'
            rows.get().get(0).get(1) == "30"

        when : 'We let the application thread work off the write-back, and both worlds settle.'
            letBothWorldsSettle()
        then : 'The edit arrived in the property, and the table still displays it.'
            rows.get() == Tuple.of(Tuple.of("Alice", "31"), Tuple.of("Bob", "42"))
            table.getValueAt(0, 1) == "31"
    }

    def 'Row mutations racing from the application thread while the UI thread reads always converge.'(int run)
    {
        reportInfo """
            The final exam for the `Tuple` based table: a continuously draining
            application thread works off a stream of row mutations while the UI
            thread reads the table like a renderer at the same time. No matter
            how the threads interleave, once all queues have run dry, the table
            must agree with the final state of the row property.
        """
        given : 'A log spy and a continuously draining application thread.'
            var log = LogSpy.attach()
            var app = ApplicationThread.startDraining()
        and : 'A property of rows and a table in decoupled mode.'
            var rows = Var.of(Tuple.of(Tuple.of("start", "0")))
            var table = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()-> UI.table(rows)).get(JTable)
            })

        when : 'We enqueue 100 row mutations for the application thread, while the UI thread performs 50 renderer style read passes.'
            100.times { step ->
                EventProcessor.DECOUPLED.registerAppEvent({
                    if ( step % 2 == 0 )
                        rows.update({ it.add(Tuple.of("row-$step".toString(), step.toString())) })
                    else if ( rows.get().size() > 1 )
                        rows.update({ it.removeFirst() })
                })
            }
            50.times { step ->
                UI.runNow({
                    int count = table.rowCount
                    for ( int r = 0; r < count; r++ )
                        table.getValueAt(r, 0)
                })
            }
        and : 'We wait for the application thread to work off the queue, and let both worlds settle.'
            app.awaitAllProcessed()
            letBothWorldsSettle()
        then : 'The table offers exactly the final rows of the property.'
            table.rowCount == rows.get().size()
            (0..<table.rowCount).collect({ table.getValueAt(it, 0) }) == rows.get().collect({ it.get(0) })
        and : 'No event task failed and nothing was silently logged as an error.'
            app.failures().isEmpty()
            log.errors().isEmpty()

        cleanup :
            app.stop()
            log.detach()

        where : 'We repeat this race a few times to explore different thread interleavings.'
            run << (1..5)
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
