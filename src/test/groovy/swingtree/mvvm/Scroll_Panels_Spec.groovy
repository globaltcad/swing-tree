package swingtree.mvvm

import examples.mvvm.ScrollPanelsViewModel
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Vars
import swingtree.SwingTree
import swingtree.components.JScrollPanels
import swingtree.threading.EventProcessor
import swingtree.UI
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
                    UI.scrollPanels().add(vm.entries(), evm ->
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
                    UI.scrollPanels().add(list, item ->
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

}
