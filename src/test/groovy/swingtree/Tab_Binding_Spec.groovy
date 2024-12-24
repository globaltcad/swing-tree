package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.Var
import sprouts.Vars
import swingtree.api.IconDeclaration
import swingtree.api.mvvm.TabSupplier
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.*
import java.time.DayOfWeek

@Title("Binding Tabs to Properties")
@Narrative('''

    Tabs are a way to efficiently group related content in a single container.
    The tabs can be bound to a property, so that the selected tab is always
    the one that corresponds to the value of the property.
    You can also model other aspects of a tab using properties, such as
    whether it is enabled, visible, or has a tooltip.

''')
class Tab_Binding_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def 'The selection state of tabs can be modelled through various properties.'()
    {
        reportInfo """
            You can bind an integer property to the selected tab index, 
            and multiple boolean properties to the selected state of each tab.
        """
        given : '4 different properties, 1 for each tab and then the selected index property.'
            var tab1Selected = Var.of(false)
            var tab2Selected = Var.of(false)
            var tab3Selected = Var.of(false)
            var selectedIndex = Var.of(-1)
        and : 'We create a tabbed pane UI node and attach tabs with custom tab header components to it.'
            def tabbedPane =
                UI.tabbedPane(UI.Side.TOP).withSelectedIndex(selectedIndex)
                .add(UI.tab("Tab 1").isSelectedIf(tab1Selected))
                .add(UI.tab("Tab 2").isSelectedIf(tab2Selected))
                .add(UI.tab("Tab 3").isSelectedIf(tab3Selected))
                .get(JTabbedPane)
        expect :
            tabbedPane.getSelectedIndex() == -1
            tabbedPane.getTabCount() == 3

        when : 'We select the first tab.'
            tabbedPane.selectedIndex = 0

        then : 'All properties reflect this change.'
            selectedIndex.get() == 0
            tab1Selected.get() == true
            tab2Selected.get() == false
            tab3Selected.get() == false

        when : 'We select the second tab.'
            tabbedPane.selectedIndex = 1

        then : 'The selected index property is updated, and the boolean properties are correct too.'
            selectedIndex.get() == 1
            tab2Selected.get() == true
            tab1Selected.get() == false
            tab3Selected.get() == false

        when : 'We select the third tab using the boolean property.'
            tab3Selected.set(true)
            UI.sync()

        then : 'The boolean property is updated, and the selected index property is correct too.'
            selectedIndex.get() == 2
            tab3Selected.get() == true
            tab1Selected.get() == false
            tab2Selected.get() == false

        when : 'We change the selected index property to a valid selection.'
            selectedIndex.set(1)
            UI.sync()

        then : 'All boolean properties are false.'
            tabbedPane.selectedIndex == 1
            tab1Selected.get() == false
            tab2Selected.get() == true
            tab3Selected.get() == false

        when : 'We change the selected index property to an invalid selection (not tab selected).'
            selectedIndex.set(-1)
            UI.sync()

        then : 'All boolean properties are false and the selected index property is -1.'
            tabbedPane.selectedIndex == -1
            tab1Selected.get() == false
            tab2Selected.get() == false
            tab3Selected.get() == false
    }

    def 'The selection states of tabs can be modelled through boolean properties.'()
    {
        given : '3 different properties, 1 for each tab.'
            var tab1Selected = Var.of(false)
            var tab2Selected = Var.of(false)
            var tab3Selected = Var.of(false)
        and : 'We create a tabbed pane UI node and attach tabs with custom tab header components to the properties.'
            def tabbedPane =
                UI.tabbedPane(UI.Side.TOP)
                .add(UI.tab("Tab 1").isSelectedIf(tab1Selected))
                .add(UI.tab("Tab 2").isSelectedIf(tab2Selected))
                .add(UI.tab("Tab 3").isSelectedIf(tab3Selected))
                .get(JTabbedPane)
        expect :
            tabbedPane.getSelectedIndex() == -1
            tabbedPane.getTabCount() == 3

        when : 'We select the first tab.'
            tabbedPane.selectedIndex = 0

        then : 'The properties reflect this change.'
            tab1Selected.get() == true
            tab2Selected.get() == false
            tab3Selected.get() == false

        when : 'We select the second tab.'
            tabbedPane.selectedIndex = 1

        then : 'The properties reflect this change, only the second tab is selected.'
            tab1Selected.get() == false
            tab2Selected.get() == true
            tab3Selected.get() == false

        when : 'We select the third tab using the boolean property.'
            tab3Selected.set(true)
            UI.sync()

        then : 'The boolean properties change to match the selected tab.'
            tab1Selected.get() == false
            tab2Selected.get() == false
            tab3Selected.get() == true
    }

    def 'An unbound tabbed pane has the expect initial state.'()
    {
        given : 'We create a tabbed pane UI node and attach tabs with custom tab header components to it.'
            def tabbedPane =
                UI.tabbedPane(UI.Side.TOP)
                .add(UI.tab("Tab 1"))
                .add(UI.tab("Tab 2"))
                .add(UI.tab("Tab 3"))
                .get(JTabbedPane)
        expect :
            tabbedPane.getSelectedIndex() == 0
            tabbedPane.getTabCount() == 3
    }

    def 'A string property can model the title of a tab!'()
    {
        reportInfo """
            You can bind a string property to the title of a tab.
        """
        given : 'A string property and a tabbed pane UI node.'
            var title = Var.of("Tab 1")
            def tabbedPane =
                UI.tabbedPane(UI.Side.TOP)
                .add(UI.tab(title))
                .get(JTabbedPane)

        when : 'We change the title.'
            title.set("Tab 2")
            UI.sync()

        then : 'The title of the tab is updated.'
            tabbedPane.getTitleAt(0) == "Tab 2"
    }

    def 'Icons can be bound to tab headers dynamically.'()
    {
        reportInfo """
            You can bind an icon property to the icon of a tab.
            But note that you may not use the `Icon` or `ImageIcon` classes directly,
            instead you must use implementations of the `IconDeclaration` interface,
            which merely models the resource location of the icon.
            
            The reason for this distinction is the fact that traditional Swing icons
            are heavy objects whose loading may or may not succeed, and so they are
            not suitable for direct use in a property as part of your view model.
            Instead, you should use the `IconDeclaration` interface, which is a
            lightweight value object that merely models the resource location of the icon
            even if it is not yet loaded or even does not exist at all.
            
            This is especially useful in case of unit tests for you view model,
            where the icon may not be available at all, but you still want to test
            the behaviour of your view model.
        """
        given : 'We create an `IconDeclaration`, which is essentially just a resource location value object.'
            IconDeclaration iconDeclaration = IconDeclaration.of("swing.png")
        and : 'An icon property and a tabbed pane UI node.'
            var icon = Var.of(iconDeclaration)
            def tabbedPane =
                UI.tabbedPane(UI.Side.TOP)
                .add(UI.tab("Tab 1").withIcon(icon))
                .get(JTabbedPane)

        when : 'We change the icon.'
            IconDeclaration newIcon = IconDeclaration.of("seed.png")
            icon.set(newIcon)
            UI.sync()

        then : 'The icon of the tab is updated.'
            tabbedPane.getIconAt(0) == newIcon.find().get()
    }

    def 'Properties allow you to enable or disable individual tabs.'()
    {
        reportInfo """
            You can bind a boolean property to the enabled state of a tab.
        """
        given : '2 properties and a tabbed pane UI node.'
            var enabled1 = Var.of(true)
            var enabled2 = Var.of(true)
            def tabbedPane =
                UI.tabbedPane(UI.Side.TOP)
                .add(UI.tab("Tab 1").isEnabledIf(enabled1))
                .add(UI.tab("Tab 2").isEnabledIf(enabled2))
                .get(JTabbedPane)

        when : 'We disable the first tab.'
            enabled1.set(false)
            UI.sync()

        then : 'The first tab is disabled.'
            tabbedPane.isEnabledAt(0) == false

        when : 'We disable the second tab.'
            enabled2.set(false)
            UI.sync()

        then : 'The second tab is disabled.'
            tabbedPane.isEnabledAt(1) == false

        when : 'We enable the first tab.'
            enabled1.set(true)
            UI.sync()

        then : 'The first tab is enabled.'
            tabbedPane.isEnabledAt(0) == true

        when : 'We enable the second tab.'
            enabled2.set(true)
            UI.sync()

        then : 'The second tab is enabled.'
            tabbedPane.isEnabledAt(1) == true
    }

    def 'Modelling the tooltip of a tab is also possible.'()
    {
        reportInfo """
            You can bind a string property to the tooltip of a tab.
        """
        given : 'A string property and a tabbed pane UI node.'
            var tooltip = Var.of("I am a tooltip!")
            def tabbedPane =
                UI.tabbedPane(UI.Side.TOP)
                .add(UI.tab("Tab 1").withTip(tooltip))
                .get(JTabbedPane)

        when : 'We change the tooltip.'
            tooltip.set("I am a new tooltip!")
            UI.sync()

        then : 'The tooltip of the tab is updated.'
            tabbedPane.getToolTipTextAt(0) == "I am a new tooltip!"
    }

    def 'Content rich tabs can be represented dynamically from property lists.'() {
        reportInfo """
            In larger GUIs usually consist views which themselves consist of multiple
            sub views. This is also true for their view models which are usually
            structured in the same tree like fashion. 
            Often times however, your views are highly dynamic and you want to
            be able to swap out sub views at runtime. In this case it is useful
            to represent your view models as property lists, especially if 
            one view consists of multiple sub views.
            
            This is also true for the tabbed pane, whose sub-views
            are the tabs!
            To make this possible implement the 'TabSupplier' interface so
            you can bind it to a view using the "Vars" class wrapping your tabs.
            When the property list changes, the view will be updated automatically.
        """
        given : 'We create a view model.'
            Var<String> address = Var.of("123 Main Street")
            Var<String> title = Var.of("Mr.")
            Var<Integer> price = Var.of(1000000)
            Var<DayOfWeek> day = Var.of(DayOfWeek.MONDAY)

        and : 'We create 4 view models with 4 locally created views:'
            var vm1 = "Dummy View Model 1"
            var vm2 = "Dummy View Model 2"
            var vm3 = "Dummy View Model 3"
            var vm4 = "Dummy View Model 4"
            TabSupplier<String> viewer = viewModel -> {
                switch ( viewModel ) {
                    case "Dummy View Model 1":
                            return UI.tab("T1").add(
                                        UI.panel().id("sub-1")
                                        .add(UI.label("Address:"))
                                        .add(UI.textField(address))
                                        .add(UI.button("Update").onClick( it -> address.set("456 Main Street") ))
                                    )
                    case "Dummy View Model 2":
                            return UI.tab("T2").add(
                                        UI.panel().id("sub-2")
                                        .add(UI.label("Title:"))
                                        .add(UI.textField(title))
                                        .add(UI.button("Update").onClick( it -> title.set("Mrs.") ))
                                    )
                    case "Dummy View Model 3":
                            return UI.tab("T3").add(
                                        UI.panel().id("sub-3")
                                        .add(UI.label("Price:"))
                                        .add(UI.slider(UI.Align.HORIZONTAL).withValue(price))
                                        .add(UI.button("Update").onClick( it -> price.set(2000000.0) ))
                                    )
                    case "Dummy View Model 4":
                                return UI.tab("T4").add(
                                        UI.panel().id("sub-4")
                                        .add(UI.label("Option:"))
                                        .add(UI.comboBox(day, DayOfWeek.values()))
                                        .add(UI.button("Update").onClick( it -> day.set(DayOfWeek.WEDNESDAY) ))
                                    )
                            }
                        }
        and : 'A property list storing the view models.'
            var vms = Vars.of(vm1, vm2, vm3, vm4)
        and : 'Finally a view which binds to the view model property list.'
            var ui = UI.panel()
                    .add(UI.label("Dynamic Super View:"))
                    .add(UI.tabbedPane().id("super").add(vms, viewer))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'We query the UI for the views and verify that the "super" and "sub-1" views are present.'
            new Utility.Query(panel).find(JTabbedPane, "super").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-1").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-2").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-3").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-4").isPresent()
        when : 'We remove something from the view model property list.'
            vms.remove(vm2)
            UI.sync()
        then : 'We expect all views to be present except for the "sub-2" view.'
            new Utility.Query(panel).find(JTabbedPane, "super").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-1").isPresent()
            !new Utility.Query(panel).find(JPanel, "sub-2").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-3").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-4").isPresent()
        and : 'We remove something else from the view model property list but this time, for a change, use the index.'
            vms.removeAt(2) // vm4
            UI.sync()
        then : 'We expect all views to be present except for the "sub-2" and "sub-4" views.'
            new Utility.Query(panel).find(JTabbedPane, "super").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-1").isPresent()
            !new Utility.Query(panel).find(JPanel, "sub-2").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-3").isPresent()
            !new Utility.Query(panel).find(JPanel, "sub-4").isPresent()
        when : 'We reintroduce "vm2"...'
            vms.add(vm2)
            UI.sync()
        then : 'We expect all views to be present except for the "sub-4" view.'
            new Utility.Query(panel).find(JTabbedPane, "super").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-1").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-2").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-3").isPresent()
            !new Utility.Query(panel).find(JPanel, "sub-4").isPresent()

        when : 'We clear the view model property list.'
            vms.clear()
            UI.sync()
        then : 'We expect all views to be removed. (except for the "super" view)'
            new Utility.Query(panel).find(JTabbedPane, "super").isPresent()
            !new Utility.Query(panel).find(JPanel, "sub-1").isPresent()
            !new Utility.Query(panel).find(JPanel, "sub-2").isPresent()
            !new Utility.Query(panel).find(JPanel, "sub-3").isPresent()
            !new Utility.Query(panel).find(JPanel, "sub-4").isPresent()
    }

    def 'You can bind a property list and a tab supplier to dynamically add or remove tabs.'() {
        reportInfo """
            You can bind a string property list and a tab supplier to dynamically add or remove tabs.
        """
        given: 'A string property list, a tab supplier and a tabbed pane UI node.'
            Vars<String> tabs = Vars.of("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5")
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
            def tabbedPane =
                    UI.tabbedPane(UI.Side.TOP)
                            .add(tabs, supplier)
                            .get(JTabbedPane)

        when: 'We remove the tab at index 1.'
            tabs.removeAt(1)
            UI.sync()
        then: 'The tabbed pane is updated and the tab removed.'
            tabbedPane.getTabCount() == tabs.size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 3"
            tabbedPane.getTitleAt(2) == "Tab 4"
            tabbedPane.getTitleAt(3) == "Tab 5"

        when: 'We remove 2 tabs starting from index 1.'
            tabs.removeAt(1, 2)
            UI.sync()
        then: 'The tabbed pane is updated and the tabs removed.'
            tabbedPane.getTabCount() == tabs.size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 5"

        when: 'We update the tab at index 1.'
            tabs.setAt(1, "Tab 2")
            UI.sync()
        then: 'The tabbed pane is updated and the tab updated.'
            tabbedPane.getTabCount() == tabs.size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 2"

        when: 'We add a tab.'
            tabs.add("Tab 3")
            UI.sync()
        then: 'The tabbed pane is updated and the tab added.'
            tabbedPane.getTabCount() == tabs.size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 2"
            tabbedPane.getTitleAt(2) == "Tab 3"

        when: 'We add 2 tabs.'
            tabs.addAll("Tab 4", "Tab 5")
            UI.sync()
        then: 'The tabbed pane is updated and the tabs added.'
            tabbedPane.getTabCount() == tabs.size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 2"
            tabbedPane.getTitleAt(2) == "Tab 3"
            tabbedPane.getTitleAt(3) == "Tab 4"
            tabbedPane.getTitleAt(4) == "Tab 5"

        when: 'We insert 1 tab.'
            tabs.addAt(0, "Tab 0")
            UI.sync()
        then: 'The tabbed pane is updated and the tabs inserted.'
            tabbedPane.getTabCount() == tabs.size()
            tabbedPane.getTitleAt(0) == "Tab 0"
            tabbedPane.getTitleAt(1) == "Tab 1"
            tabbedPane.getTitleAt(2) == "Tab 2"
            tabbedPane.getTitleAt(3) == "Tab 3"
            tabbedPane.getTitleAt(4) == "Tab 4"
            tabbedPane.getTitleAt(5) == "Tab 5"
    }

    def 'An exception in the tab supplier for a model property list, produces an error tab instead.'()
    {
        reportInfo """
            A fundamental requirement when it comes to binding a list of models to
            a set of UI components is that the number of models and the number of
            UI components must match. If they do not, the list change listeners 
            will not know which model corresponds to which UI component.
            
            This is why in case of an exception, a sort of dummy tab is created.
            It indicates that something went wrong and the tab could not be created.
        """
        given : 'A property list and a tab supplier that throws an exception.'
            Vars<String> tabs = Vars.of("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5")
            TabSupplier<String> supplier = (String title) -> {
                if ( title == "Tab 3" ) {
                    throw new RuntimeException("This tab could not be created!")
                }
                return UI.tab(title)
            }
        and : 'A UI declaration with a tabbed pane bound to the property list and the tab supplier.'
            def ui = UI.tabbedPane(UI.Side.TOP).add(tabs, supplier)

        when : 'We build the component.'
            var tabbedPane = ui.get(JTabbedPane)
        then : 'First of all, the exception does not leak to the outside.'
            noExceptionThrown()

        and : 'We expect the tabbed pane to have 5 tabs, even though one of them is an error tab.'
            tabbedPane.getTabCount() == 5
        and : 'We expect the error tab to have the correct title.'
            tabbedPane.getTitleAt(2).contains("Error")
    }

    def 'If the tab supplier for a model property list return `null`, a null tab is shown instead'()
    {
        reportInfo """
            A fundamental requirement when it comes to binding a list of models to
            a set of UI components is that the number of models and the number of
            UI components must match. If they do not, the list change listeners 
            will not know which model corresponds to which UI component.
            
            So in case of a `null` return value, a sort of dummy tab is created and
            added to the tabbed pane. 
            It indicates that something went wrong and the tab could not be created.
        """
        given : 'A property list and a tab supplier that returns `null`.'
            Vars<String> tabs = Vars.of("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5")
            TabSupplier<String> supplier = (String title) -> {
                if ( title == "Tab 3" ) {
                    return null
                }
                return UI.tab(title)
            }
        and : 'A UI declaration with a tabbed pane bound to the property list and the tab supplier.'
            def ui = UI.tabbedPane(UI.Side.TOP).add(tabs, supplier)

        when : 'We build the component.'
            var tabbedPane = ui.get(JTabbedPane)
        then : 'We expect the tabbed pane to have 5 tabs, even though one of them is a null tab.'
            tabbedPane.getTabCount() == 5
        and : 'We expect the null tab to have a title which indicates that content is missing.'
            tabbedPane.getTitleAt(2).contains("Empty")
    }
}
