package swingtree.tabs

import swingtree.EventProcessor
import swingtree.UI
import sprouts.Var
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

import javax.swing.JTabbedPane

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
        UI.SETTINGS().setEventProcessor(EventProcessor.COUPLED)
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
                UI.tabbedPane(UI.Position.TOP).withSelectedIndex(selectedIndex)
                .add(UI.tab("Tab 1").isSelectedIf(tab1Selected))
                .add(UI.tab("Tab 2").isSelectedIf(tab2Selected))
                .add(UI.tab("Tab 3").isSelectedIf(tab3Selected))
                .get(JTabbedPane)

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

        when : 'We change the selected index property to an invalid selection.'
            selectedIndex.set(-1)
            UI.sync()

        then : 'All boolean properties are false.'
            tabbedPane.selectedIndex == -1
            tab1Selected.get() == false
            tab2Selected.get() == false
            tab3Selected.get() == false
    }

    def 'A string property can model the title of a tab!'()
    {
        reportInfo """
            You can bind a string property to the title of a tab.
        """
        given : 'A string property and a tabbed pane UI node.'
            var title = Var.of("Tab 1")
            def tabbedPane =
                UI.tabbedPane(UI.Position.TOP)
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
        """
        given : 'An icon property and a tabbed pane UI node.'
            var icon = Var.of(UI.icon("swing.png"))
            def tabbedPane =
                UI.tabbedPane(UI.Position.TOP)
                .add(UI.tab("Tab 1").withIcon(icon))
                .get(JTabbedPane)

        when : 'We change the icon.'
            var newIcon = UI.icon("seed.png")
            icon.set(newIcon)
            UI.sync()

        then : 'The icon of the tab is updated.'
            tabbedPane.getIconAt(0) == newIcon
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
                UI.tabbedPane(UI.Position.TOP)
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
                UI.tabbedPane(UI.Position.TOP)
                .add(UI.tab("Tab 1").withTip(tooltip))
                .get(JTabbedPane)

        when : 'We change the tooltip.'
            tooltip.set("I am a new tooltip!")
            UI.sync()

        then : 'The tooltip of the tab is updated.'
            tabbedPane.getToolTipTextAt(0) == "I am a new tooltip!"
    }

}
