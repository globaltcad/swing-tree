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
import swingtree.api.mvvm.BoundViewSupplier
import swingtree.api.mvvm.EntryViewModel
import swingtree.components.JScrollPanels
import swingtree.threading.EventProcessor
import utility.LogSpy
import utility.SwingTreeTestConfigurator

import javax.swing.JLabel
import java.awt.event.MouseEvent

@Title("Scroll Panels Entry Bindings Across Threads")
@Narrative('''

    The `JScrollPanels` component supports two ways of binding its entries.

    The recommended pathway binds a `Var<Tuple<M extends HasId<ID>>>` property:
    the entries are immutable value objects owned by the application thread,
    each entry is handed to the view supplier as a property of its own, and
    all entry state - including a selection flag, if you need one - is simply
    data in the value objects. There is no UI owned entry state to synchronize,
    which is what makes this pathway fully thread safe in the decoupled mode.

    The older pathway hands `EntryViewModel` implementations to the component,
    whose contract requires the UI thread to write position and selection state
    directly into the view models while building their views. That handshake
    cannot be made thread safe, which is why the interface is deprecated:
    it keeps working in the coupled threading modes, but binding it in a
    declaration which uses a decoupled event processor logs a loud warning.

    This specification documents both pathways and their threading contracts.

''')
@Subject([UIForScrollPanels, JScrollPanels, EntryViewModel, EventProcessor])
@Timeout(60)
@CompileDynamic
class Scroll_Panels_Threading_Spec extends Specification
{
    /**
     *  An immutable entry for the tuple based pathway. Note that the selection
     *  is simply data in the value object, there is nothing view-specific about it.
     */
    static record Entry(String id, String text, boolean selected) implements sprouts.HasId<String> {
        Entry withSelected( boolean selected ) { new Entry(id, text, selected) }
    }

    /**
     *  A classic view model for the deprecated `EntryViewModel` pathway,
     *  exposing the mutable selection and position properties the old contract demands.
     */
    static class LegacyEntry implements EntryViewModel {
        final Var<Boolean> selected = Var.of(false)
        final Var<Integer> position = Var.of(0)
        final String text
        LegacyEntry(String text) { this.text = text }
        @Override Var<Boolean> isSelected() { return selected }
        @Override Var<Integer> position() { return position }
    }

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

    def 'Selection of tuple bound entries is plain application state, updated safely across the thread boundary.'()
    {
        reportInfo """
            With the tuple based binding there is no UI owned selection state
            at all: the selection flag is data in the immutable entry objects,
            owned by the application thread like everything else. Selecting one
            entry and deselecting all others is a single atomic tuple update.

            A click in the view therefore does not touch any state directly.
            It merely queues the tuple update on the application event queue,
            and once the application thread has applied it, the change events
            carry the new entry states back to the UI thread, where the
            existing sub-views are recycled (based on the entry ids) and
            restyled. This feature walks through that full round trip.
        """
        given : 'A log spy, so that both silent errors and unexpected warnings fail this test.'
            var log = LogSpy.attach()
        and : 'A tuple property of entries, of which the second one starts out selected.'
            var entries = Var.of(Tuple.of(
                new Entry("a", "Alpha", false),
                new Entry("b", "Beta",  true ),
                new Entry("c", "Gamma", false)
            ))
        and : 'A view supplier rendering each entry as a label, whose click selects only that entry.'
            BoundViewSupplier<Entry> supplier = { Var<Entry> entry ->
                String id = entry.get().id()
                UI.label(entry.viewAsString({ e -> (e.selected() ? "> " : "") + e.text() }))
                .onMouseClick({ it ->
                    entries.update({ tuple -> tuple.map({ e -> e.withSelected(e.id() == id) }) })
                })
            }
        and : 'Scroll panels built around the entries in decoupled mode.'
            var panels = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.scrollPanels().addAll(entries, supplier)
                ).get(JScrollPanels)
            })
        expect : 'The entries render with the initial selection marker on the second entry.'
            _entryTextsOf(panels) == ["Alpha", "> Beta", "Gamma"]

        when : 'We remember the sub-view of the first entry, and then the user clicks its label on the UI thread.'
            var viewOfEntryA = panels.getContentPanel().getComponent(0)
            UI.runNow({
                var label = (JLabel) ((java.awt.Container) panels.getContentPanel().getComponent(0)).getComponent(0)
                label.dispatchEvent(new MouseEvent(label, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 1, 1, 1, false))
            })
        then : 'The entries themselves have not been touched yet! The tuple update is waiting in the application event queue.'
            entries.get().toList().collect({ it.selected() }) == [false, true, false]

        when : 'This test thread, playing the application thread, processes the event queue.'
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
        then : 'The selection moved to the first entry, atomically, in a single tuple update.'
            entries.get().toList().collect({ it.selected() }) == [true, false, false]

        when : 'The UI thread catches up with the resulting change event.'
            UI.sync()
        then : 'The views show the new selection marker...'
            _entryTextsOf(panels) == ["> Alpha", "Beta", "Gamma"]
        and : '...and the sub-view of the first entry was recycled, not rebuilt, thanks to its stable entry id.'
            panels.getContentPanel().getComponent(0).is(viewOfEntryA)
        and : 'Nothing was logged along the way, this pathway is neither deprecated nor thread unsafe.'
            log.errors().isEmpty()
            log.warnings().isEmpty()

        cleanup :
            log.detach()
    }

    def 'The deprecated `EntryViewModel` pathway keeps working in the coupled threading modes, without any warning.'()
    {
        reportInfo """
            Deprecation is a migration aid, not a breaking change: view models
            implementing the old `EntryViewModel` contract continue to work
            exactly as before when the UI runs in a coupled threading mode,
            where the UI thread and the application thread are one and the
            same, so the contract's thread safety problem cannot bite.
            No warning is logged in this configuration.
        """
        given : 'A log spy and a property list of legacy view models.'
            var log = LogSpy.attach()
            var entries = Vars.of(new LegacyEntry("A"), new LegacyEntry("B"), new LegacyEntry("C"))
        and : 'Scroll panels built around them in the (strict) coupled mode, which is the global default here.'
            var panels = UI.runAndGet({
                UI.scrollPanels().addAll(entries, m -> UI.label(m.text)).get(JScrollPanels)
            })
        expect : 'The entries render.'
            panels.numberOfEntries == 3
            _entryTextsOf(panels) == ["A", "B", "C"]

        when : 'The legacy selection machinery is used to select the second entry.'
            UI.runNow({ panels.setSelectedFor(JLabel, { it.text == "B" }) })
        then : 'The selection arrived in the legacy view model, the old contract is fully intact.'
            entries.at(1).get().isSelected().is(true)

        when : 'A new legacy view model is added to the property list.'
            UI.runNow({ entries.add(new LegacyEntry("D")) })
        then : 'The new entry appears in the view.'
            panels.numberOfEntries == 4
        and : 'No deprecation warning and no error was logged, the coupled modes stay quiet.'
            log.warnings().findAll({ it.contains("EntryViewModel") }).isEmpty()
            log.errors().isEmpty()

        cleanup :
            log.detach()
    }

    def 'Binding `EntryViewModel`s in a decoupled UI declaration logs one loud warning, but still functions.'()
    {
        reportInfo """
            The `EntryViewModel` contract requires the UI thread to write
            position and selection state directly into the view models while
            building their entry views, and the view supplier reads that state
            back mid-construction. This handshake cannot be handed over to the
            application event queue without breaking it, which is why the
            combination of `EntryViewModel`s and a decoupled event processor
            is fundamentally thread unsafe.

            SwingTree does not break existing applications over this: the
            binding still works. But it logs a single loud warning at
            declaration time, pointing to the thread safe tuple based
            alternative documented on the `EntryViewModel` interface.
        """
        given : 'A log spy and a property list of legacy view models.'
            var log = LogSpy.attach()
            var entries = Vars.of(new LegacyEntry("A"), new LegacyEntry("B"), new LegacyEntry("C"))

        when : 'Scroll panels are built around them in decoupled mode.'
            var panels = UI.runAndGet({
                UI.use(EventProcessor.DECOUPLED, ()->
                    UI.scrollPanels().addAll(entries, m -> UI.label(m.text))
                ).get(JScrollPanels)
            })
        then : 'Exactly one loud warning was logged, at declaration time, explaining the problem and the migration path.'
            log.warnings().count({ it.contains("EntryViewModel") && it.contains("NOT thread safe") }) == 1
        and : 'The binding itself still works, the entries render.'
            panels.numberOfEntries == 3
            _entryTextsOf(panels) == ["A", "B", "C"]

        when : 'The application thread appends another legacy view model, and the UI thread catches up.'
            entries.add(new LegacyEntry("D"))
            UI.sync()
        then : 'The structural update was applied, the legacy pathway remains functional.'
            panels.numberOfEntries == 4
        and : 'No error was silently logged, and the warning was not repeated for the update.'
            log.errors().isEmpty()
            log.warnings().count({ it.contains("EntryViewModel") && it.contains("NOT thread safe") }) == 1

        cleanup :
            log.detach()
    }

    private static List<String> _entryTextsOf( JScrollPanels panels ) {
        // Scroll panels wrap every generated sub-view in an internal entry panel:
        var content = panels.getContentPanel()
        return (0..<content.componentCount).collect({
            var entryPanel = (java.awt.Container) content.getComponent(it)
            return ((JLabel) entryPanel.getComponent(0)).text
        })
    }
}
