package swingtree

import groovy.transform.ImmutableOptions
import net.miginfocom.swing.MigLayout
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.*
import swingtree.api.mvvm.BoundViewSupplier
import swingtree.components.JScrollPanels
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.*


@Title("Bi-Directional Tuple Bound Views")
@Narrative('''

    SwingTree provides a powerful `addAll` binding mechanism that connects
    a `Var<Tuple<M>>` property to dynamically managed sub-views using a
    `BoundViewSupplier<M>`. This is designed for **value-based, immutable view models**
    that implement the `HasId` interface.
    
    Unlike the simpler `ViewSupplier` based `addAll` overloads, the `BoundViewSupplier`
    variant is **bi-directional**: each sub-view receives a `Var<M>` lens property
    that can both read from and write back to the parent tuple. Changes to
    individual items propagate back up through the lens to the root property.
    
    A key design goal of this binding is **efficiency**:
    When the tuple changes, sub-views are only rebuilt if their item's `id()` 
    (from the `HasId` interface) has changed. If an item at a given position 
    retains the same id but has different data (e.g. a text field was edited), 
    the existing sub-view is kept and its lens property simply reflects the new data.
    This avoids expensive GUI teardown/rebuild cycles for purely data-level changes.
    
    This specification serves as living documentation for the three overloads:
    - `addAll(Var<Tuple<M>>, BoundViewSupplier<M>)` — no layout constraints
    - `addAll(String, Var<Tuple<M>>, BoundViewSupplier<M>)` — with String layout constraints
    - `addAll(AddConstraint, Var<Tuple<M>>, BoundViewSupplier<M>)` — with typed layout constraints

''')
class Tuple_Bound_Views_Spec extends Specification
{
    static record Item(UUID id, String name) implements HasId<UUID> {
        Item withName(String name) { return new Item(id, name) }
        Item withId(UUID id) { return new Item(id, name) }
    }

    static record Tag(String id, String label) implements HasId<String> {
        Tag withLabel(String label) { return new Tag(id, label) }
        Tag withId(String id) { return new Tag(id, label) }
    }

    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }


    def 'A panel with `addAll` bound to a `Var<Tuple<M>>` shows one sub-view per item.'()
    {
        reportInfo """
            The most basic usage of `addAll` with a `BoundViewSupplier` is to bind
            a tuple of view models to a parent panel. Each item in the tuple gets
            its own sub-view, created by the supplier lambda. The supplier receives
            a `Var<M>` lens property that is focused on the item at the corresponding
            index in the tuple.
        """
        given : 'We prepare 3 items as our tuple-based view model.'
            var id1 = UUID.randomUUID()
            var id2 = UUID.randomUUID()
            var id3 = UUID.randomUUID()
            var tuple = Tuple.of(
                new Item(id1, "Alpha"),
                new Item(id2, "Beta"),
                new Item(id3, "Gamma")
            )
            var models = Var.of(tuple)

        and : 'A view supplier that creates a labelled panel for each item.'
            BoundViewSupplier<Item> supplier = { Var<Item> itemProp ->
                UI.panel().id(itemProp.get().name())
                    .add(UI.label(itemProp.viewAsString({ it.name() })))
            }

        when : 'We build a panel with the tuple binding.'
            var panel = UI.panel().addAll(models, supplier).get(JPanel)

        then : 'The panel has exactly 3 sub-components, one per item.'
            panel.componentCount == 3

        and : 'We can find each sub-view by its id.'
            new Utility.Query(panel).find(JPanel, "Alpha").isPresent()
            new Utility.Query(panel).find(JPanel, "Beta").isPresent()
            new Utility.Query(panel).find(JPanel, "Gamma").isPresent()
    }


    def 'Adding an item to the tuple creates a new sub-view without rebuilding the others.'()
    {
        reportInfo """
            When an item is appended to the tuple, a new sub-view is added
            at the end of the parent panel. The existing sub-views for previously
            existing items are left untouched — they are the exact same component
            instances as before the update.
        """
        given : 'A tuple with two items and a panel bound to it.'
            var id1 = UUID.randomUUID()
            var id2 = UUID.randomUUID()
            var models = Var.of(Tuple.of(
                new Item(id1, "First"),
                new Item(id2, "Second")
            ))
            BoundViewSupplier<Item> supplier = { Var<Item> p -> UI.label(p.viewAsString({ it.name() })) }
            var panel = UI.panel().addAll(models, supplier).get(JPanel)

        and : 'We capture the initial component instances.'
            var original0 = panel.getComponent(0)
            var original1 = panel.getComponent(1)

        when : 'We add a third item to the tuple.'
            models.update { it.add(new Item(UUID.randomUUID(), "Third")) }
            UI.sync()

        then : 'The panel now has 3 sub-components.'
            panel.componentCount == 3

        and : 'The first two components are the exact same object instances (not rebuilt).'
            panel.getComponent(0).is(original0)
            panel.getComponent(1).is(original1)
    }


    def 'Removing an item from the tuple removes its sub-view.'()
    {
        reportInfo """
            When an item is removed from the tuple property, its corresponding
            sub-view is removed from the parent panel. The remaining views
            keep their positions and are not recreated.
        """
        given : 'A tuple with three tagged items.'
            var models = Var.of(Tuple.of(
                new Tag("x", "TagX"),
                new Tag("y", "TagY"),
                new Tag("z", "TagZ")
            ))
            BoundViewSupplier<Tag> supplier = { Var<Tag> p -> UI.panel().id(p.get().id()) }
            var panel = UI.panel().addAll(models, supplier).get(JPanel)

        expect : 'All three sub-views are present initially.'
            panel.componentCount == 3
            new Utility.Query(panel).find(JPanel, "x").isPresent()
            new Utility.Query(panel).find(JPanel, "y").isPresent()
            new Utility.Query(panel).find(JPanel, "z").isPresent()

        when : 'We remove the middle item.'
            models.update { it.removeAt(1) }
            UI.sync()

        then : 'Only two sub-views remain, and "y" is gone.'
            panel.componentCount == 2
            new Utility.Query(panel).find(JPanel, "x").isPresent()
            !new Utility.Query(panel).find(JPanel, "y").isPresent()
            new Utility.Query(panel).find(JPanel, "z").isPresent()
    }


    def 'Clearing the tuple removes all sub-views from the panel.'()
    {
        reportInfo """
            Clearing the entire tuple results in all sub-views being removed
            from the parent panel, leaving it empty.
        """
        given : 'A panel bound to a tuple of two items.'
            var models = Var.of(Tuple.of(
                new Tag("a", "Alice"),
                new Tag("b", "Bob")
            ))
            BoundViewSupplier<Tag> supplier = { Var<Tag> p -> UI.label(p.get().label()) }
            var panel = UI.panel().addAll(models, supplier).get(JPanel)

        expect : 'Initially two components.'
            panel.componentCount == 2

        when : 'We clear the tuple.'
            models.update { it.clear() }
            UI.sync()

        then : 'The panel is empty.'
            panel.componentCount == 0
    }


    def "Changing an item's data but keeping its id does NOT rebuild the sub-view."()
    {
        reportInfo """
            This is the key efficiency optimization of the `BoundViewSupplier` based `addAll`:
            When an item in the tuple is replaced with a new instance that has the
            **same `id()`** but different data (e.g. a name change), the framework
            does NOT rebuild the sub-view for that item. Instead, the existing lens
            property is updated with the new data, and any bound UI components
            refresh themselves through the lens.
            
            This means the same `JComponent` instance survives the update — 
            only its displayed data changes.
        """
        given : 'A tuple with one item and a panel bound to it.'
            var itemId = UUID.randomUUID()
            var models = Var.of(Tuple.of(
                new Item(itemId, "Original Name")
            ))
            BoundViewSupplier<Item> supplier = { Var<Item> p -> UI.label(p.viewAsString({ it.name() })) }
            var panel = UI.panel().addAll(models, supplier).get(JPanel)

        and : 'We capture the component instance before the update.'
            var originalComponent = panel.getComponent(0)

        when : 'We replace the item with one that has the same id but a different name.'
            models.update { it.setAt(0, new Item(itemId, "Updated Name")) }
            UI.sync()

        then : 'The panel still has exactly one component.'
            panel.componentCount == 1

        and : 'It is the exact same component instance — no rebuild happened.'
            panel.getComponent(0).is(originalComponent)
    }


    def 'Changing an item`s id causes that specific sub-view to be rebuilt.'()
    {
        reportInfo """
            In contrast to a pure data change (same id), replacing an item with one
            that has a **different `id()`** signals to the framework that the item
            at that position is conceptually a different entity. The old sub-view
            is discarded and a new one is created.
        """
        given : 'A tuple with two items.'
            var id1 = UUID.randomUUID()
            var id2 = UUID.randomUUID()
            var models = Var.of(Tuple.of(
                new Item(id1, "Keep Me"),
                new Item(id2, "Replace Me")
            ))
            BoundViewSupplier<Item> supplier = { Var<Item> p -> UI.label(p.viewAsString({ it.name() })) }
            var panel = UI.panel().addAll(models, supplier).get(JPanel)

        and : 'We capture both component instances.'
            var kept = panel.getComponent(0)
            var replaced = panel.getComponent(1)

        when : 'We replace the second item with one that has a brand new id.'
            var newId = UUID.randomUUID()
            models.update { it.setAt(1, new Item(newId, "I Am New")) }
            UI.sync()

        then : 'The first sub-view is the same instance (its id did not change).'
            panel.getComponent(0).is(kept)

        and : 'The second sub-view is a different instance (it was rebuilt).'
            !panel.getComponent(1).is(replaced)
    }


    def 'The lens property provided to the view supplier reads from and writes back to the tuple.'()
    {
        reportInfo """
            Each sub-view receives a `Var<M>` lens that is bi-directionally
            bound to the parent tuple property. Reading the lens returns the
            current item at that index; writing to the lens updates the item
            in the tuple. This is what makes the pattern truly bi-directional
            and suitable for editable views like text fields.
        """
        given : 'A tuple with one item.'
            var itemId = UUID.randomUUID()
            var models = Var.of(Tuple.of(
                new Item(itemId, "Editable")
            ))
            Var<Item> capturedLens = null
            BoundViewSupplier<Item> supplier = { Var<Item> p ->
                capturedLens = p
                return UI.label(p.viewAsString({ it.name() }))
            }
            UI.panel().addAll(models, supplier).get(JPanel)

        expect : 'The captured lens reads the current item.'
            capturedLens.get().name() == "Editable"

        when : 'We write a new value through the lens.'
            capturedLens.set(new Item(itemId, "Modified"))
            UI.sync()

        then : 'The parent tuple property is updated with the new value.'
            models.get().get(0).name() == "Modified"

        and : 'The id is preserved — the same conceptual item, just different data.'
            models.get().get(0).id() == itemId
    }


    def 'The String constraint overload applies MigLayout constraints to each sub-view.'()
    {
        reportInfo """
            The `addAll(String, Var<Tuple<M>>, BoundViewSupplier<M>)` overload lets you
            specify a MigLayout constraint string that will be applied to every
            sub-view that is generated by the view supplier. This is useful for
            ensuring consistent layout across dynamically generated views.
        """
        given : 'A tuple with two items and a panel with MigLayout.'
            var models = Var.of(Tuple.of(
                new Tag("p", "Panel-P"),
                new Tag("q", "Panel-Q")
            ))
            BoundViewSupplier<Tag> supplier = { Var<Tag> p -> UI.panel().id(p.get().id()) }

        when : 'We build the panel with a String layout constraint.'
            var panel = UI.panel("wrap 1").addAll("growx", models, supplier).get(JPanel)

        then : 'Both sub-views are present.'
            new Utility.Query(panel).find(JPanel, "p").isPresent()
            new Utility.Query(panel).find(JPanel, "q").isPresent()

        and : 'Each sub-view has the "growx" constraint applied in the MigLayout.'
            var layout = (MigLayout) panel.getLayout()
            layout.getComponentConstraints(new Utility.Query(panel).find(JPanel, "p").get()) == "growx"
            layout.getComponentConstraints(new Utility.Query(panel).find(JPanel, "q").get()) == "growx"
    }


    def 'Layout constraints persist when items are added or removed dynamically.'()
    {
        reportInfo """
            When a tuple is mutated (items added or removed), the layout constraints
            specified in the `addAll` call are applied to newly generated sub-views
            just as they were to the initial ones. The layout manager stays consistent.
        """
        given : 'A tuple with one item and a constrained panel.'
            var models = Var.of(Tuple.of(
                new Tag("first", "First Tag")
            ))
            BoundViewSupplier<Tag> supplier = { Var<Tag> p -> UI.panel().id(p.get().id()) }
            var panel = UI.panel("wrap 1").addAll("growx", models, supplier).get(JPanel)
            var layout = (MigLayout) panel.getLayout()

        expect : 'Initially one constrained sub-view.'
            layout.constraintMap.size() == 1

        when : 'We add a second item.'
            models.update { it.add(new Tag("second", "Second Tag")) }
            UI.sync()

        then : 'Both sub-views have the "growx" constraint.'
            layout.constraintMap.size() == 2
            layout.getComponentConstraints(new Utility.Query(panel).find(JPanel, "first").get()) == "growx"
            layout.getComponentConstraints(new Utility.Query(panel).find(JPanel, "second").get()) == "growx"

        when : 'We remove the first item.'
            models.update { it.removeAt(0) }
            UI.sync()

        then : 'Only the second sub-view remains, still with its constraint.'
            layout.constraintMap.size() == 1
            !new Utility.Query(panel).find(JPanel, "first").isPresent()
            layout.getComponentConstraints(new Utility.Query(panel).find(JPanel, "second").get()) == "growx"
    }


    def 'Multiple data-only changes in a row never rebuild sub-views when ids are stable.'()
    {
        reportInfo """
            This test verifies that the id-based optimization works consistently
            across multiple successive updates. As long as the `HasId.id()` values
            at each position remain the same, no sub-views are rebuilt, regardless
            of how many times the data changes. This is essential for scenarios
            like live-editing text fields bound to a list of items.
        """
        given : 'A tuple with three items.'
            var ids = [UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()]
            var models = Var.of(Tuple.of(
                new Item(ids[0], "v1"),
                new Item(ids[1], "v1"),
                new Item(ids[2], "v1")
            ))
            BoundViewSupplier<Item> supplier = { Var<Item> p -> UI.label(p.viewAsString({ it.name() })) }
            var panel = UI.panel().addAll(models, supplier).get(JPanel)

        and : 'We capture the original component instances.'
            var originals = (0..<3).collect { panel.getComponent(it) }

        when : 'We update the data 5 times, always keeping the same ids.'
            (2..6).each { version ->
                models.update { tuple ->
                    tuple.setAt(0, new Item(ids[0], "v$version"))
                         .setAt(1, new Item(ids[1], "v$version"))
                         .setAt(2, new Item(ids[2], "v$version"))
                }
                UI.sync()
            }

        then : 'All three component instances are still the original ones.'
            (0..<3).every { panel.getComponent(it).is(originals[it]) }
    }


    def 'A bi-directionally bound tuple property computes efficiently across various operations.'(
        java.util.List<Integer> diff, Closure<Tuple> operation
    ) {
        reportInfo """
            This data-driven test verifies the efficiency of the `addAll` binding
            across many different tuple operations. Each operation is applied to an
            initial tuple of 5 items, and we check:
            
            - Component count matches the resulting tuple size
            - Components at positions mapped to original indices are reused (same instance)
            - Components at positions marked `-1` are brand new (not present before)
            - Components at positions marked `_` (wildcard) are newly created
            
            This ensures the framework performs minimal GUI work for each change.
        """
        given: 'A tuple of 5 items with distinct ids, and a panel bound to it.'
            var tuple = Tuple.of(
                new Tag("Comp 1", "Comp 1"),
                new Tag("Comp 2", "Comp 2"),
                new Tag("Comp 3", "Comp 3"),
                new Tag("Comp 4", "Comp 4"),
                new Tag("Comp 5", "Comp 5")
            )
            var models = Var.of(tuple)
            BoundViewSupplier<Tag> supplier = { Var<Tag> p -> UI.button(p.viewAsString({ it.label() })) }
            var panel = UI.panel().addAll(models, supplier).get(JPanel)

        and : 'We snapshot the initial components.'
            var iniComps = (0..<panel.componentCount).collect { panel.getComponent(it) }

        when: 'We apply the operation to the tuple.'
            models.update { it -> operation(it) }
            UI.sync()

        and : 'We snapshot the updated components.'
            var updatedComps = (0..<panel.componentCount).collect { panel.getComponent(it) }

        then: 'The panel has the expected number of components.'
            panel.componentCount == models.get().size()
            panel.componentCount == diff.findAll { it == _ || it >= 0 }.size()

        and : 'Components at mapped original indices are the same object instances.'
            diff.findAll({ it == _ || it >= 0 }).indexed().every {
                it.value == _ || iniComps[it.value].is(updatedComps[it.key])
            }

        and : 'Components at "-1" positions are completely new.'
            diff.indexed().every {
                it.value == _ || it.value >= 0 || !(iniComps[it.key] in updatedComps)
            }

        where : 'We test the following operations on the tuple:'
            diff                  | operation
            [0, -1, 2, 3, 4]      | { it.removeAt(1) }
            [0, -1, -1, 3, 4]     | { it.removeAt(1, 2) }
            [0,  _, 2, 3, 4]      | { it.setAt(1, new Tag("Comp X", "Comp X")) }
            [0, 1, 2, 3, 4, _]    | { it.add(new Tag("Comp X", "Comp X")) }
            [0, 1, 2, 3, 4, _, _] | { it.addAll(new Tag("Comp X", "Comp X"), new Tag("Comp Y", "Comp Y")) }
            [_, 0, 1, 2, 3, 4]    | { it.addAt(0, new Tag("Comp X", "Comp X")) }
            [-1, 1, 2, 3, -1]     | { it.slice(1, 4) }
            [0, 1, -1, -1, -1]    | { it.sliceFirst(2) }
            [-1, -1, 2, 3, 4]     | { it.sliceLast(3) }
            [-1, -1, -1, -1, -1]  | { it.clear() }
    }


    def 'When an item`s id stays the same during a SET operation, the sub-view of a `JPanel` is NOT rebuilt.'()
    {
        reportInfo """
            This test specifically isolates the `HasId` based optimization in the SET path,
            when changing an item at a specific index. The framework relies on the `id()` values to
            determine whether a sub-view needs to be rebuilt.
            
            More specifically, the implementation checks `!Objects.equals(oldTuple.get(i).id(), newTuple.get(i).id())`
            and only rebuilds the sub-view when the ids differ. So replacing an item
            with one that shares the same id but has different payload data must NOT
            trigger a view rebuild. The existing sub-view component instance must survive.
        """
        given : 'A tuple with 3 items and a panel.'
            var id1 = UUID.randomUUID()
            var id2 = UUID.randomUUID()
            var id3 = UUID.randomUUID()
            var models = Var.of(Tuple.of(
                new Item(id1, "A"), new Item(id2, "B"), new Item(id3, "C")
            ))
            int supplierInvocations = 0
            BoundViewSupplier<Item> supplier = (Var<Item> p) -> {
                supplierInvocations++
                return UI.label(p.viewAsString({ it.name() }))
            }
            var panel = UI.panel().addAll(models, supplier).get(JPanel)
            var comp0 = panel.getComponent(0)
            var comp1 = panel.getComponent(1)
            var comp2 = panel.getComponent(2)
        expect : 'The supplier was called exactly 3 times for the initial items.'
            supplierInvocations == 3

        when : 'We replace the middle item with one that has the same id but different name.'
            models.update { it.setAt(1, new Item(id2, "B-Updated")) }
            UI.sync()

        then : 'All three component instances are preserved.'
            panel.getComponent(0).is(comp0)
            panel.getComponent(1).is(comp1)
            panel.getComponent(2).is(comp2)
        and : 'The supplier was NOT called again (no rebuild).'
            supplierInvocations == 3

        when : 'We replace the middle item with one that has a DIFFERENT id.'
            models.update { it.setAt(1, new Item(UUID.randomUUID(), "Newcomer")) }
            UI.sync()

        then : 'The first and last components are preserved, but the middle one is new.'
            panel.getComponent(0).is(comp0)
            !panel.getComponent(1).is(comp1)
            panel.getComponent(2).is(comp2)
        and : 'The supplier was called once more for the new item.'
            supplierInvocations == 4
    }


    def 'When an item`s id stays the same during a SET operation, the sub-view of a `JScrollPanels` is NOT rebuilt.'()
    {
        reportInfo """
            This test specifically isolates the `HasId` based optimization in the SET path,
            for when the parent container is a `JScrollPanels` instead of a plain `JPanel`.
            
            The implementation checks `!Objects.equals(oldTuple.get(i).id(), newTuple.get(i).id())`
            and only rebuilds the sub-view when the ids differ. So replacing an item
            with one that shares the same id but has different payload data must NOT
            trigger a view rebuild. The existing sub-view component instance must survive.
        """
        given : 'A tuple with 3 items and a panel.'
            var id1 = UUID.randomUUID()
            var id2 = UUID.randomUUID()
            var id3 = UUID.randomUUID()
            var models = Var.of(Tuple.of(
                new Item(id1, "A"), new Item(id2, "B"), new Item(id3, "C")
            ))
            int supplierInvocations = 0
            BoundViewSupplier<Item> supplier = (Var<Item> p) -> {
                supplierInvocations++
                return UI.label(p.viewAsString({ it.name() }))
            }
            var panels = UI.scrollPanels().addAll(models, supplier).get(JScrollPanels)
            var internalWrapper = panels.getContentPanel()
            var comp0 = internalWrapper.getComponent(0).getComponent(0) // JScrollPanels have a wrapper panel for each item!
            var comp1 = internalWrapper.getComponent(1).getComponent(0)
            var comp2 = internalWrapper.getComponent(2).getComponent(0)
        expect : 'The supplier was called exactly 3 times for the initial items.'
            supplierInvocations == 3

        when : 'We replace the middle item with one that has the same id but different name.'
            models.update { it.setAt(1, new Item(id2, "B-Updated")) }
            UI.sync()

        then : 'All three component instances are preserved.'
            internalWrapper.getComponent(0).getComponent(0).is(comp0)
            internalWrapper.getComponent(1).getComponent(0).is(comp1)
            internalWrapper.getComponent(2).getComponent(0).is(comp2)
        and : 'The supplier was NOT called again (no rebuild).'
            supplierInvocations == 3

        when : 'We replace the middle item with one that has a DIFFERENT id.'
            models.update { it.setAt(1, new Item(UUID.randomUUID(), "Newcomer")) }
            UI.sync()

        then : 'The first and last components are preserved, but the middle one is new.'
            internalWrapper.getComponent(0).getComponent(0).is(comp0)
            !internalWrapper.getComponent(1).getComponent(0).is(comp1)
            internalWrapper.getComponent(2).getComponent(0).is(comp2)
        and : 'The supplier was called once more for the new item.'
            supplierInvocations == 4
    }


    def 'The view supplier is called once per item during initial binding.'()
    {
        reportInfo """
            When the `addAll` binding is first established, the `BoundViewSupplier`
            is invoked exactly once for each item currently in the tuple. No extra
            or duplicate calls should occur.
        """
        given : 'A counter to track invocations and a tuple with 4 items.'
            var callCount = 0
            var models = Var.of(Tuple.of(
                new Tag("a", "A"), new Tag("b", "B"),
                new Tag("c", "C"), new Tag("d", "D")
            ))
            BoundViewSupplier<Tag> supplier = { Var<Tag> p ->
                callCount++
                return UI.label(p.get().label())
            }

        when : 'We build the panel.'
            UI.panel().addAll(models, supplier).get(JPanel)

        then : 'The supplier was called exactly 4 times.'
            callCount == 4
    }


    def 'Replacing the entire tuple with a completely different set of items rebuilds all views.'()
    {
        reportInfo """
            When the tuple is replaced wholesale with items that have entirely
            different ids, all sub-views must be rebuilt from scratch.
            None of the original component instances should survive.
        """
        given : 'A tuple of 3 items and a panel.'
            var models = Var.of(Tuple.of(
                new Tag("old-1", "Old A"),
                new Tag("old-2", "Old B"),
                new Tag("old-3", "Old C")
            ))
            BoundViewSupplier<Tag> supplier = { Var<Tag> p -> UI.panel().id(p.get().id()) }
            var panel = UI.panel().addAll(models, supplier).get(JPanel)
            var originals = (0..<3).collect { panel.getComponent(it) }

        when : 'We replace the tuple with entirely new items.'
            models.set(Tuple.of(
                new Tag("new-1", "New X"),
                new Tag("new-2", "New Y")
            ))
            UI.sync()

        then : 'The panel has 2 components now (matching the new tuple size).'
            panel.componentCount == 2

        and : 'None of the original component instances are present.'
            originals.every { original -> !(original in (0..<panel.componentCount).collect { panel.getComponent(it) }) }
    }


    def 'An empty initial tuple creates no sub-views, and adding items later works correctly.'()
    {
        reportInfo """
            Starting from an empty tuple is a valid scenario. The panel should
            initially have no sub-components, and adding items later should
            populate the panel correctly through the binding.
        """
        given : 'An empty tuple and a panel bound to it.'
            var models = Var.of(Tuple.of(Item))
            BoundViewSupplier<Item> supplier = { Var<Item> p -> UI.label(p.viewAsString({ it.name() })) }
            var panel = UI.panel().addAll(models, supplier).get(JPanel)

        expect : 'The panel starts empty.'
            panel.componentCount == 0

        when : 'We add two items to the tuple.'
            models.update { it.add(new Item(UUID.randomUUID(), "First")).add(new Item(UUID.randomUUID(), "Second")) }
            UI.sync()

        then : 'The panel now has 2 sub-components.'
            panel.componentCount == 2
    }


    def 'Views bound bi-directionally to a tuple are reused efficiently across various transformations.'(
        Tuple<Object> initialModels, Closure<Tuple<Object>> operation
    ) {
        reportInfo """
            This data-driven test checks that view instances are correctly 
            reused or discarded depending on whether their underlying model
            items persist across a tuple transformation.
            
            Each model item uses its own value as its identity (via `SelfAsId`),
            so items that remain in the new tuple should map to reused views,
            while items that disappear should map to discarded views.
        """
        given: 'A tuple of SelfAsId items, a view supplier, and a panel.'
            var models = Var.of(initialModels.mapTo(SelfAsId, o -> new SelfAsId(o)))
            BoundViewSupplier<SelfAsId> supplier = { Var<SelfAsId> p ->
                UI.button(p.viewAsString({ Objects.toString(it.id()) }))
            }
            var panel = UI.panel().addAll(models, supplier).get(JPanel)
            var initialComponents = panel.components as java.util.List<JComponent>

        when: 'We apply the transformation.'
            models.update { it -> operation(it) }
            UI.sync()

        and : 'We evaluate which views were reused.'
            var newComponents = panel.components as java.util.List<JComponent>
            var whichViewReused = initialComponents.collect { newComponents.contains(it) }
            var whichModelsReused = initialModels.collect { models.get().contains(new SelfAsId(it)) }

        then: 'View reuse matches model reuse exactly.'
            whichModelsReused == whichViewReused

        where : 'We test the following transformations:'
            initialModels              | operation
            Tuple.of("a", "b")         | { Tuple.of("X", "a", "z").mapTo(SelfAsId, o -> new SelfAsId(o)) }
            Tuple.of(1, 2, 3)          | { Tuple.of(-1, 2, -3).mapTo(SelfAsId, o -> new SelfAsId(o)) }
            Tuple.of(1, 2, 3, 4, 5, 6) | { Tuple.of(-1, 2, -3, 4, 5, 42).mapTo(SelfAsId, o -> new SelfAsId(o)) }
            Tuple.of("a", "b")         | { it.reversed() }
            Tuple.of(1, 2, 3)          | { it.reversed() }
            Tuple.of(1, 2, 3, 4, 5, 6) | { it.reversed() }
            Tuple.of("a", "b")         | { it.removeFirst(1) }
            Tuple.of(1, 2, 3)          | { it.removeFirst(1) }
            Tuple.of(1, 2, 3, 4, 5, 6) | { it.removeFirst(1) }
            Tuple.of("a", "b")         | { it.removeLast(1) }
            Tuple.of(1, 2, 3)          | { it.removeLast(1) }
            Tuple.of(1, 2, 3, 4, 5, 6) | { it.removeLast(1) }
            Tuple.of(1, 2, 3, 4, 5, 6) | { it.setAt(1, new SelfAsId(42)) }
            Tuple.of(1, 2, 3, 4, 5, 6) | { it.setAllAt(2, new SelfAsId(42), new SelfAsId(73)) }
    }


    // Re-declare SelfAsId to mirror the existing test infrastructure
    @ImmutableOptions(knownImmutableClasses=[Object])
    static record SelfAsId(Object id) implements HasId<Object> {}
}
