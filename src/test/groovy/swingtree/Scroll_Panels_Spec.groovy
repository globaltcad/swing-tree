package swingtree

import examples.mvvm.ScrollPanelsViewModel
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Tuple
import sprouts.Var
import sprouts.Vars
import swingtree.api.mvvm.BoundViewSupplier
import swingtree.api.mvvm.ViewSupplier
import swingtree.components.JScrollPanels
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.JPanel

@Title("Scroll Panels")
@Narrative('''

    This specification is dedicated to showing how to use the
    `JScrollPanels` class, a custom SwingTree component that
    is designed to display a list of scrollable panels
    which can be populated with any kind of interactive UI
    based on any kind of sub-view model type.
    
''')
@Subject([JScrollPanels])
class Scroll_Panels_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def 'The `JScrollPanels` instance can visually represent a view model consisting of sub-view models.'()
    {
        reportInfo """
            Note that we use a pre-made example view model here.
            Feel free to look at the source code of the view model
            to see what it is doing.
            All you need to know is that it is a view model
            that contains a `Vars` based property list of sub-view models.
        """
        given : 'We instantiate the view model.'
            var vm = new ScrollPanelsViewModel()
        when : 'We create a view for our view model...'
            var ui =
                UI.panel("fill, wrap 1")
                .add( UI.label("Something to scroll:") )
                .add( UI.separator() )
                .add(
                    UI.scrollPanels().addAll(vm.entries(), evm ->
                        UI.panel("fill").id("sub-view")
                        .add("pushx", UI.label(evm.text()))
                        .add(UI.label(evm.position().viewAs(String.class, s -> "Position: " + s)))
                        .add(UI.label(evm.position().viewAs(String.class, s -> "Selected: " + s)))
                        .add(UI.button("Delete me!").onClick(it -> {
                            System.out.println("Deleting " + evm.text().get());
                            int i = evm.entries().indexOf(evm);
                            evm.entries().removeAt(i);
                            if ( i != evm.position().get() )
                                throw new IllegalStateException("Index mismatch: " + i + " != " + evm.position().get());
                        }))
                        .add(UI.button("Duplicate").onClick( it -> {
                            int i = evm.entries().indexOf(evm);
                            evm.entries().addAt(i, evm.createNew(evm.text().get() + " (copy)"));
                        }))
                        .add(UI.button("up").onClick( it -> {
                            int i = evm.entries().indexOf(evm);
                            if ( i > 0 ) {
                                evm.entries().removeAt(i);
                                evm.entries().addAt(i - 1, evm);
                            }
                        }))
                        .add(UI.button("down").onClick( it -> {
                            int i = evm.entries().indexOf(evm);
                            if ( i < evm.entries().size() - 1 ) {
                                evm.entries().removeAt(i);
                                evm.entries().addAt(i + 1, evm);
                            }
                        }))
                        .add(UI.button("replace").onClick( it -> {
                            int i = evm.entries().indexOf(evm);
                            evm.entries().setAt(i, evm.createNew("Replaced!"));
                        }))
                    )
                )
                .add( UI.separator() )
        and : 'We build the root component:'
            var panel = ui.get(JPanel)
        then : 'It was successfully created.'
            panel != null
        and : 'The view contains as many sub-views as the view model has entries.'
            new Utility.Query(panel).findAll("sub-view").size() == vm.entries().size()

        when : 'We remove an item from the entry list.'
            vm.entries().removeAt(2)
            UI.sync()
        then : 'The view is updated accordingly.'
            new Utility.Query(panel).findAll("sub-view").size() == vm.entries().size()
    }

    def 'Use a simple property list of Strings to populate a scroll panel.'()
    {
        reportInfo """
            Instead of using a view model, we use simple String instances
            to create views from each of the items in the list
            and populate a scroll panel with them.
        """
        given : 'A simple property list of Strings.'
            var list = Vars.of("One", "Two", "Three", "Four", "Five")
        when : 'We create a view for our list...'
            var ui =
                UI.panel("fill, wrap 1")
                .add( UI.label("Something to scroll:") )
                .add( UI.separator() )
                .add(
                    UI.scrollPanels().addAll(list, item ->
                        UI.panel("fill").id("sub-view")
                        .add("pushx", UI.label(item))
                        .add(UI.button("Delete me!").onClick(it -> {
                            System.out.println("Deleting " + item);
                            list.remove(item);
                        }))
                        .add(UI.button("Duplicate").onClick( it -> {
                            int i = list.indexOf(item);
                            list.addAt(i, item + " (copy)");
                        }))
                        .add(UI.button("up").onClick( it -> {
                            int i = list.indexOf(item);
                            if ( i > 0 ) {
                                list.remove(item);
                                list.addAt(i - 1, item);
                            }
                        }))
                        .add(UI.button("down").onClick( it -> {
                            int i = list.indexOf(item);
                            if ( i < list.size() - 1 ) {
                                list.remove(item);
                                list.addAt(i + 1, item);
                            }
                        }))
                        .add(UI.button("replace").onClick( it -> {
                            int i = list.indexOf(item);
                            list.setAt(i, "Replaced!");
                        }))
                    )
                )
                .add( UI.separator() )
        and : 'We build the root component:'
            var panel = ui.get(JPanel)
        then : 'The view was successfully created.'
            panel != null
        and : 'The view contains as many sub-views as the list has items.'
            new Utility.Query(panel).findAll("sub-view").size() == list.size()

        when : 'We remove an item from the list.'
            list.removeAt(2)
            UI.sync()
        then : 'The view is updated.'
            new Utility.Query(panel).findAll("sub-view").size() == list.size()

        when : 'We add an item to the list.'
            list.add("Six")
            UI.sync()
        then : 'The view is updated.'
            new Utility.Query(panel).findAll("sub-view").size() == list.size()
    }

    def 'A property list is bound to a scroll panel compute efficiently.'(
        List<Integer> diff, Closure<Tuple> operation
    ) {
        reportInfo """
            You can bind a string based tuple property and a view supplier 
            to dynamically add or remove tabs. The GUI will only update the
            tabs that have changed.
        """
        given: 'A string tuple property, a view supplier and a panel UI node.'
            var models = Vars.of("Comp 1", "Comp 2", "Comp 3", "Comp 4", "Comp 5")
            ViewSupplier<String> supplier = (String title) -> UI.button(title)
            var panels =
                        UI.scrollPanels()
                        .addAll(models, supplier)
                        .get(JScrollPanels)
            var panel = panels.getViewport().getComponent(0)
        and : 'We unpack the pane and the expected differences:'
            var iniComps = (0..<panel.getComponentCount()).collect({panel.getComponent(it)})

        when: 'We run the operation on the tuple...'
            operation(models)
            UI.sync()
        and : 'We unpack the updated components:'
            var updatedComps = (0..<panel.getComponentCount()).collect({panel.getComponent(it)})
        then: 'The tabbed pane is updated.'
            panel.getComponentCount() == models.size()
            panel.getComponentCount() == diff.findAll( it -> it == _ || it >= 0 ).size()
        and :
            diff.findAll({it == _ || it >= 0}).indexed().every({
                it.value == _ || iniComps[it.value] === updatedComps[it.key]
            })
        and : 'The components at `-1` are totally new.'
            diff.indexed().every({
                it.value == _ || it.value >= 0 || !(iniComps[it.key] in updatedComps)
            })

        where : 'We test the following operations:'
            diff                 | operation
            [0,-1, 2, 3, 4]      | { it.removeAt(1) }
            [0,-1,-1, 3, 4]      | { it.removeAt(1, 2) }
            [0, _, 2, 3, 4]      | { it.setAt(1, "Comp X") }
            [0, 1, 2, 3, 4, _]   | { it.add("Comp X") }
            [0, 1, 2, 3, 4, _, _]| { it.addAll("Comp X", "Comp Y") }
            [_, 0, 1, 2, 3, 4]   | { it.addAt(0, "Comp X") }
            [-1, -1, -1, -1, -1] | { it.clear() }
            //[-1, 1, 2, 3, -1]    | { it.retainRange(1, 4) }
            //[0, 1, -1, -1, -1]   | { it.retainFirst(2) }
            //[-1, -1, 2, 3, 4]    | { it.retainLast(3) }
    }

    def 'A tuple property is bound to a scroll panel compute efficiently.'(
        List<Integer> diff, Closure<Tuple> operation
    ) {
        reportInfo """
            You can bind a string based tuple property and a view supplier 
            to dynamically add or remove tabs. The GUI will only update the
            tabs that have changed.
        """
        given: 'A string tuple property, a view supplier and a panel UI node.'
            var tuple = Tuple.of("Comp 1", "Comp 2", "Comp 3", "Comp 4", "Comp 5")
            var models = Var.of(tuple)
            ViewSupplier<String> supplier = (String title) -> UI.button(title)
            var panels =
                        UI.scrollPanels()
                        .addAll(models, supplier)
                        .get(JScrollPanels)
            var panel = panels.getViewport().getComponent(0)
        and : 'We unpack the pane and the expected differences:'
            var iniComps = (0..<panel.getComponentCount()).collect({panel.getComponent(it)})

        when: 'We run the operation on the tuple...'
            models.update( it -> operation(it) )
            UI.sync()
        and : 'We unpack the updated components:'
            var updatedComps = (0..<panel.getComponentCount()).collect({panel.getComponent(it)})
        then: 'The tabbed pane is updated.'
            panel.getComponentCount() == models.get().size()
            panel.getComponentCount() == diff.findAll( it -> it == _ || it >= 0 ).size()
        and :
            diff.findAll({it == _ || it >= 0}).indexed().every({
                it.value == _ || iniComps[it.value] === updatedComps[it.key]
            })
        and : 'The components at `-1` are totally new.'
            diff.indexed().every({
                it.value == _ || it.value >= 0 || !(iniComps[it.key] in updatedComps)
            })

        when : 'We unpack the panel contents:'
            var viewedTexts = panels.getContentPanel().getComponents().collect({it.getComponent(0).text})
        then : 'The view texts a equal to the string representations of the tuple elements.'
            viewedTexts == models.get().mapTo(String, it -> Objects.toString(it)).toList()

        where : 'We test the following operations:'
            diff                 | operation
            [0,-1, 2, 3, 4]      | { it.removeAt(1) }
            [0,-1,-1, 3, 4]      | { it.removeAt(1, 2) }
            [0, _, 2, 3, 4]      | { it.setAt(1, "Comp X") }
            [0, 1, 2, 3, 4, _]   | { it.add("Comp X") }
            [0, 1, 2, 3, 4, _, _]| { it.addAll("Comp X", "Comp Y") }
            [_, 0, 1, 2, 3, 4]   | { it.addAt(0, "Comp X") }
            [-1, 1, 2, 3, -1]    | { it.slice(1, 4) }
            [0, 1, -1, -1, -1]   | { it.sliceFirst(2) }
            [-1, -1, 2, 3, 4]    | { it.sliceLast(3) }
            [-1, -1, -1, -1, -1] | { it.clear() }
            [_, _, _, _, _]      | { Tuple.of("Comp 1", "Comp 2", "Comp 3", "Comp 4", "Comp 5") }
            [_, _, _, _, _]      | { it.clear().addAll("Comp 1", "Comp 2", "Comp 3", "Comp 4", "Comp 5") }
            [_, _, _, _, _]      | { Tuple.of("Comp a", "Comp b", "Comp c", "Comp d", "Comp e") }
            [_, _, _, _, _]      | { it.clear().addAll("Comp a", "Comp b", "Comp c", "Comp d", "Comp e") }
    }

    def 'A tuple property can be bound to a scroll panel bi-directionally and compute efficiently.'(
        List<Integer> diff, Closure<Tuple> operation
    ) {
        reportInfo """
            You can bind a string based tuple property and a view supplier 
            to dynamically add or remove tabs. The GUI will only update the
            tabs that have changed.
        """
        given: 'A string tuple property, a view supplier and a panel UI node.'
            var tuple = Tuple.of("Comp 1", "Comp 2", "Comp 3", "Comp 4", "Comp 5")
            var models = Var.of(tuple)
            BoundViewSupplier<String> supplier = (Var<String> title) -> UI.button(title.view("", {it}))
            var panels =
                        UI.scrollPanels()
                        .addAll(models, supplier)
                        .get(JScrollPanels)
            var panel = panels.getViewport().getComponent(0)
        and : 'We unpack the pane and the expected differences:'
            var iniComps = (0..<panel.getComponentCount()).collect({panel.getComponent(it)})
        expect : 'The components have the correct initial text displayed on them:'
            panel.getComponents().collect({it.getComponent(0).text}) == ["Comp 1", "Comp 2", "Comp 3", "Comp 4", "Comp 5"]

        when: 'We run the operation on the tuple...'
            models.update( it -> operation(it) )
            UI.sync()
        and : 'We unpack the updated components:'
            var updatedComps = (0..<panel.getComponentCount()).collect({panel.getComponent(it)})
        then: 'The tabbed pane is updated.'
            panel.getComponentCount() == models.get().size()
            panel.getComponentCount() == diff.findAll( it -> it == _ || it >= 0 ).size()
        and :
            diff.findAll({it == _ || it >= 0}).indexed().every({
                it.value == _ || iniComps[it.value] === updatedComps[it.key]
            })
        and : 'The components at `-1` are totally new.'
            diff.indexed().every({
                it.value == _ || it.value >= 0 || !(iniComps[it.key] in updatedComps)
            })

        when : 'We unpack the panel contents:'
            var viewedTexts = panel.getComponents().collect({it.getComponent(0).text})
        then : 'The view texts a equal to the string representations of the tuple elements.'
            viewedTexts == models.get().mapTo(String, it -> Objects.toString(it)).toList()

        where : 'We test the following operations:'
            diff                 | operation
            [0,-1, 2, 3, 4]      | { it.removeAt(1) }
            [0,-1,-1, 3, 4]      | { it.removeAt(1, 2) }
            [0, _, 2, 3, 4]      | { it.setAt(1, "Comp X") }
            [0, 1, 2, 3, 4, _]   | { it.add("Comp X") }
            [0, 1, 2, 3, 4, _, _]| { it.addAll("Comp X", "Comp Y") }
            [_, 0, 1, 2, 3, 4]   | { it.addAt(0, "Comp X") }
            [-1, 1, 2, 3, -1]    | { it.slice(1, 4) }
            [0, 1, -1, -1, -1]   | { it.sliceFirst(2) }
            [-1, -1, 2, 3, 4]    | { it.sliceLast(3) }
            [-1, -1, -1, -1, -1] | { it.clear() }
            [_, _, _, _, _]      | { Tuple.of("Comp 1", "Comp 2", "Comp 3", "Comp 4", "Comp 5") }
            [_, _, _, _, _]      | { it.clear().addAll("Comp 1", "Comp 2", "Comp 3", "Comp 4", "Comp 5") }
            [_, _, _, _, _]      | { Tuple.of("Comp a", "Comp b", "Comp c", "Comp d", "Comp e") }
            [_, _, _, _, _]      | { it.clear().addAll("Comp a", "Comp b", "Comp c", "Comp d", "Comp e") }
    }

}
