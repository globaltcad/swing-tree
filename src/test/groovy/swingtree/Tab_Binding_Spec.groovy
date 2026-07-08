package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.From
import sprouts.Tuple
import sprouts.Var
import sprouts.Vars
import sprouts.Viewable
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
@Subject([UIForTabbedPane])
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
            UI.runNow(()->{ tabbedPane.selectedIndex = 0 })

        then : 'All properties reflect this change.'
            selectedIndex.get() == 0
            tab1Selected.get() == true
            tab2Selected.get() == false
            tab3Selected.get() == false

        when : 'We select the second tab.'
            UI.runNow(()->{ tabbedPane.selectedIndex = 1 })

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
            UI.runNow(()->{ tabbedPane.selectedIndex = 0 })

        then : 'The properties reflect this change.'
            tab1Selected.get() == true
            tab2Selected.get() == false
            tab3Selected.get() == false

        when : 'We select the second tab.'
            UI.runNow(()->{ tabbedPane.selectedIndex = 1 })

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

    def 'A tabbed pane may be bound to a selection index property whose value does not yet match any tab.'()
    {
        reportInfo """
            The `withSelectedIndex(Var)` method allows you to bind an integer property
            to the selection index of a tabbed pane even before the tabs it points to
            exist. The selection index is stored by the underlying selection model and
            applied automatically as soon as a matching tab is added.
            This means you may declare the selection index binding before adding any tabs
            at all, without running into an `IndexOutOfBoundsException`.
        """
        given : 'A selection index property pointing at the first tab, before any tab exists.'
            var index = Var.of(0)

        when : 'We build a tabbed pane whose selection index is bound before its tabs are added.'
            def tabbedPane =
                UI.tabbedPane().withSelectedIndex(index)
                .add(UI.tab("tab 1"))
                .add(UI.tab("tab 2"))
                .get(JTabbedPane)

        then : 'Building the tabbed pane does not throw an exception.'
            noExceptionThrown()
        and : 'The tabbed pane has the two tabs we added.'
            tabbedPane.getTabCount() == 2
        and : 'The selection index property was applied to the first, now existing, tab.'
            tabbedPane.getSelectedIndex() == 0
            index.get() == 0
    }

    def 'A selection index binding is applied only once the tab it points to actually exists.'()
    {
        reportInfo """
            When you bind a selection index property whose value points to a tab
            that does not exist yet, the selection is deferred until a tab with a
            matching index is added. Adding tabs with smaller indices does not
            select anything until the desired index becomes valid.
        """
        given : 'A selection index property pointing at the third tab.'
            var index = Var.of(2)
        and : 'A tabbed pane with the binding declared before any tab is added, and only two tabs added.'
            def tabbedPane =
                UI.tabbedPane().withSelectedIndex(index)
                .add(UI.tab("tab 1"))
                .add(UI.tab("tab 2"))
                .get(JTabbedPane)

        expect : 'With only two tabs present, the desired index (2) is not valid yet, so nothing is selected.'
            tabbedPane.getTabCount() == 2
            tabbedPane.getSelectedIndex() == -1
        and : 'The selection index property still holds the desired (not yet applicable) index.'
            index.get() == 2

        when : 'We build another tabbed pane, this time with a third tab that makes the desired index valid.'
            var completedIndex = Var.of(2)
            def completedPane =
                UI.tabbedPane().withSelectedIndex(completedIndex)
                .add(UI.tab("tab 1"))
                .add(UI.tab("tab 2"))
                .add(UI.tab("tab 3"))
                .get(JTabbedPane)

        then : 'The previously stored selection index is applied automatically to the matching tab.'
            completedPane.getTabCount() == 3
            completedPane.getSelectedIndex() == 2
            completedIndex.get() == 2
    }

    def 'A deferred selection index works together with tabs bound to a tuple property.'()
    {
        reportInfo """
            The selection index binding also cooperates with tabs which are dynamically
            generated from a `Tuple` based `Val` property through the `addAll(Val,..)` method.
            If the bound selection index points to a tab that does not exist yet, it is
            stored and applied automatically as soon as the tuple grows enough for the
            index to become valid.
        """
        given : 'A selection index property pointing at the 5th tab (index 4).'
            var index = Var.of(4)
        and : 'A tuple property initially holding only two tab models.'
            var models = Var.of(Tuple.of("Tab 1", "Tab 2"))
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
        and : 'A tabbed pane whose selection index is bound and whose tabs come from the tuple property.'
            def pane =
                UI.tabbedPane().withSelectedIndex(index)
                .addAll(models, supplier)
                .get(JTabbedPane)

        expect : 'Only two tabs exist, so the desired index (4) is not valid yet and nothing is selected.'
            pane.getTabCount() == 2
            pane.getSelectedIndex() == -1
        and : 'The selection index property still holds the desired (not yet applicable) index.'
            index.get() == 4

        when : 'We expand the tuple to six tab models, so that index 4 becomes valid.'
            models.update( it -> it.addAll("Tab 3", "Tab 4", "Tab 5", "Tab 6") )
            UI.sync()

        then : 'The tabbed pane now has six tabs and the stored selection index was applied automatically.'
            pane.getTabCount() == 6
            pane.getSelectedIndex() == 4
            index.get() == 4
    }

    def 'A deferred selection index works together with tabs bound to a property list.'()
    {
        reportInfo """
            The selection index binding also cooperates with tabs which are dynamically
            generated from a `Vars` property list through the `addAll(Vals,..)` method.
            If the bound selection index points to a tab that does not exist yet, it is
            stored and applied automatically as soon as the list grows enough for the
            index to become valid.
        """
        given : 'A selection index property pointing at the 5th tab (index 4).'
            var index = Var.of(4)
        and : 'A property list initially holding only two tab models.'
            var models = Vars.of("Tab 1", "Tab 2")
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
        and : 'A tabbed pane whose selection index is bound and whose tabs come from the property list.'
            def pane =
                UI.tabbedPane().withSelectedIndex(index)
                .addAll(models, supplier)
                .get(JTabbedPane)

        expect : 'Only two tabs exist, so the desired index (4) is not valid yet and nothing is selected.'
            pane.getTabCount() == 2
            pane.getSelectedIndex() == -1
        and : 'The selection index property still holds the desired (not yet applicable) index.'
            index.get() == 4

        when : 'We add four more tab models, so that index 4 becomes valid.'
            models.addAll("Tab 3", "Tab 4", "Tab 5", "Tab 6")
            UI.sync()

        then : 'The tabbed pane now has six tabs and the stored selection index was applied automatically.'
            pane.getTabCount() == 6
            pane.getSelectedIndex() == 4
            index.get() == 4
    }

    def 'Changing a bound selection index to a not-yet-existing tab at runtime defers the selection.'()
    {
        reportInfo """
            The deferral mechanism does not only apply at build time. If, at runtime, you
            set a bound selection index property to a value which points to a tab that does
            not exist yet, the tabbed pane deselects everything (since the targeted tab does
            not exist) while the desired index is stored and then applied automatically
            once the tabs (here coming from a property list) grow enough for the index to
            become valid.
        """
        given : 'A selection index property pointing at the first tab and a two element tab list.'
            var index = Var.of(0)
            var models = Vars.of("Tab 1", "Tab 2")
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
        and : 'A tabbed pane whose selection index is bound and whose tabs come from the property list.'
            def pane =
                UI.tabbedPane().withSelectedIndex(index)
                .addAll(models, supplier)
                .get(JTabbedPane)

        expect : 'Both tabs exist and the first one is selected right away.'
            pane.getTabCount() == 2
            pane.getSelectedIndex() == 0
            index.get() == 0

        when : 'At runtime we set the selection index to the 5th tab (index 4), which does not exist yet.'
            index.set(4)
            UI.sync()

        then : 'The targeted tab does not exist yet, so nothing is selected.'
            pane.getTabCount() == 2
            pane.getSelectedIndex() == -1
        and : 'The selection index property still holds the desired (deferred) index.'
            index.get() == 4

        when : 'We add four more tab models, so that index 4 becomes valid.'
            models.addAll("Tab 3", "Tab 4", "Tab 5", "Tab 6")
            UI.sync()

        then : 'The deferred selection index is now applied automatically.'
            pane.getTabCount() == 6
            pane.getSelectedIndex() == 4
            index.get() == 4
    }

    def 'Setting a bound selection index to a not-yet-existing tab deselects everything until it exists.'()
    {
        reportInfo """
            When you set a bound selection index property to a value which points to a
            tab that does not exist (yet), the targeted selection simply does not exist,
            so the tabbed pane resolves to "nothing selected" (index `-1`) and all tab
            `isSelectedIf(..)` booleans become `false`. The selection index property
            itself keeps the (deferred) desired value, which is applied automatically
            once a matching tab is added.
        """
        given : 'Three boolean selection properties, one per tab, and a selection index property.'
            var tab1Selected = Var.of(false)
            var tab2Selected = Var.of(false)
            var tab3Selected = Var.of(false)
            var selectedIndex = Var.of(0)
        and : 'A tabbed pane binding the index and the three tab selection booleans.'
            def tabbedPane =
                UI.tabbedPane(UI.Side.TOP).withSelectedIndex(selectedIndex)
                .add(UI.tab("Tab 1").isSelectedIf(tab1Selected))
                .add(UI.tab("Tab 2").isSelectedIf(tab2Selected))
                .add(UI.tab("Tab 3").isSelectedIf(tab3Selected))
                .get(JTabbedPane)
        expect : 'Initially the first tab is selected, reflected by the boolean properties.'
            tabbedPane.getSelectedIndex() == 0
            tab1Selected.get() == true
            tab2Selected.get() == false
            tab3Selected.get() == false

        when : 'We set the selection index to a value that is out of range (no such tab exists).'
            selectedIndex.set(4)
            UI.sync()

        then : 'Nothing is selected, because the targeted tab does not exist yet...'
            tabbedPane.getSelectedIndex() == -1
        and : '...all tab selection booleans resolve to false...'
            tab1Selected.get() == false
            tab2Selected.get() == false
            tab3Selected.get() == false
        and : '...and the selection index property keeps the (deferred) desired value.'
            selectedIndex.get() == 4
    }

    def 'Binding a selection index of -1 before adding tabs keeps the tabbed pane unselected.'()
    {
        reportInfo """
            The value `-1` is a meaningful selection index denoting "no tab selected".
            When you specify it explicitly (even before any tabs exist) the tabbed pane
            should honor it and stay unselected, instead of Swing auto-selecting the
            first tab as soon as it is added.
        """
        given : 'A tabbed pane whose selection index is fixed to -1 ("no selection") before adding tabs.'
            def tabbedPane =
                UI.tabbedPane().withSelectedIndex(-1)
                .add(UI.tab("Tab 1"))
                .add(UI.tab("Tab 2"))
                .add(UI.tab("Tab 3"))
                .get(JTabbedPane)

        expect : 'Even though tabs were added, no tab is selected.'
            tabbedPane.getTabCount() == 3
            tabbedPane.getSelectedIndex() == -1
    }

    def 'A deferred selection index is applied only once and does not override later selection.'()
    {
        reportInfo """
            A selection index bound before its tab exists is applied exactly once, as
            soon as the matching tab appears. Afterwards the selection remains free to
            change (for example through the user), and adding further tabs will not snap
            the selection back to the originally deferred index.
        """
        given : 'A selection index property pointing at the third tab (index 2) and a growing tab list.'
            var index = Var.of(2)
            var models = Vars.of("Tab 1", "Tab 2")
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
        and : 'A tabbed pane whose selection index is bound and whose tabs come from the property list.'
            def pane =
                UI.tabbedPane().withSelectedIndex(index)
                .addAll(models, supplier)
                .get(JTabbedPane)

        expect : 'The desired index (2) is not valid yet, so nothing is selected.'
            pane.getTabCount() == 2
            pane.getSelectedIndex() == -1

        when : 'We grow the list so the desired index becomes valid.'
            models.addAll("Tab 3", "Tab 4")
            UI.sync()

        then : 'The deferred selection index is applied to the now existing third tab.'
            pane.getSelectedIndex() == 2
            index.get() == 2

        when : 'A different tab is selected...'
            UI.runNow(()->{ pane.selectedIndex = 0 })

        then : 'the selection and the bound property follow along.'
            pane.getSelectedIndex() == 0
            index.get() == 0

        when : 'We add yet more tabs.'
            models.addAll("Tab 5", "Tab 6")
            UI.sync()

        then : 'The originally deferred index (2) does not snap back; the current selection is preserved.'
            pane.getSelectedIndex() == 0
            index.get() == 0
    }

    def 'Re-applying an already valid bound index does not let a later tab addition override a newer selection.'()
    {
        reportInfo """
            When a bound selection index is applied to a value which already points to an
            existing tab, the tabbed pane must not keep it around as a "desired" index to be
            re-applied later. Otherwise a subsequent tab addition would snap the selection
            back to that now outdated index, wrongly overriding a selection made in the
            meantime.
        """
        given : 'A selection index property and a growing tab list.'
            var index = Var.of(0)
            var models = Vars.of("Tab 1", "Tab 2")
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
        and : 'A tabbed pane whose selection index is bound and whose tabs come from the property list.'
            def pane =
                UI.tabbedPane().withSelectedIndex(index)
                .addAll(models, supplier)
                .get(JTabbedPane)

        expect : 'Both tabs exist and the first one is selected.'
            pane.getTabCount() == 2
            pane.getSelectedIndex() == 0

        when : 'We apply a valid index through the bound property while all targeted tabs already exist.'
            index.set(1)
            UI.sync()

        then : 'The second tab is selected accordingly.'
            pane.getSelectedIndex() == 1
            index.get() == 1

        when : 'A different tab is then selected...'
            UI.runNow(()->{ pane.selectedIndex = 0 })

        then : '...the selection and the bound property follow along.'
            pane.getSelectedIndex() == 0
            index.get() == 0

        when : 'We now add more tabs to the pane.'
            models.addAll("Tab 3", "Tab 4")
            UI.sync()

        then : 'The previously applied index (1) does not snap back; the newer selection is preserved.'
            pane.getTabCount() == 4
            pane.getSelectedIndex() == 0
            index.get() == 0
    }

    def 'A deferred selection index keeps the isSelectedIf boolean of its target tab in sync when it becomes valid.'()
    {
        reportInfo """
            When a bound selection index points to a tab that does not exist yet, it is
            applied automatically as soon as a matching tab is added. This application must
            go through the selection model's dedicated reconciliation path so that the tab
            `isSelectedIf(..)` booleans are kept in sync, while the bound selection index
            property keeps holding the desired value (no spurious write-back).
        """
        given : 'A selection index property pointing at the (not yet existing) third tab and a boolean for it.'
            var index = Var.of(2)
            var thirdSelected = Var.of(false)
            var models = Vars.of("Tab 1", "Tab 2")
            TabSupplier<String> supplier = (String title) ->
                title == "Tab 3" ? UI.tab(title).isSelectedIf(thirdSelected) : UI.tab(title)
        and : 'A tabbed pane whose selection index is bound and whose tabs come from the property list.'
            def pane =
                UI.tabbedPane().withSelectedIndex(index)
                .addAll(models, supplier)
                .get(JTabbedPane)

        expect : 'The desired index (2) is not valid yet, so nothing is selected and the boolean is false.'
            pane.getTabCount() == 2
            pane.getSelectedIndex() == -1
            thirdSelected.get() == false
            index.get() == 2

        when : 'We add the third tab, making the desired index valid.'
            models.add("Tab 3")
            UI.sync()

        then : 'The deferred index is applied and the target tab boolean is now in sync.'
            pane.getTabCount() == 3
            pane.getSelectedIndex() == 2
            thirdSelected.get() == true
        and : 'The bound selection index property still holds the (now applied) desired value.'
            index.get() == 2
    }

    def 'A fixed selection index does not override a newer user selection when more tabs are added.'()
    {
        reportInfo """
            The `withSelectedIndex(int)` overload configures an **initial** selection index.
            It is a one-time preference, not a permanent constraint: once it has been honored,
            the user is free to select a different tab, and adding further tabs must **not**
            snap the selection back to the originally configured index.
        """
        given : 'A growing tab list and a fixed initial selection index of 0.'
            var models = Vars.of("Tab 1", "Tab 2")
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
        and : 'A tabbed pane whose tabs come from the list and whose initial selection is fixed to index 0.'
            def pane =
                UI.tabbedPane().withSelectedIndex(0)
                .addAll(models, supplier)
                .get(JTabbedPane)

        expect : 'The fixed index selected the first tab.'
            pane.getTabCount() == 2
            pane.getSelectedIndex() == 0

        when : 'The user selects the second tab.'
            UI.runNow(()->{ pane.selectedIndex = 1 })

        then : 'The selection follows the user.'
            pane.getSelectedIndex() == 1

        when : 'More tabs are added afterwards.'
            models.addAll("Tab 3", "Tab 4")
            UI.sync()

        then : 'The fixed index does NOT snap the selection back to 0; the user selection is preserved.'
            pane.getTabCount() == 4
            pane.getSelectedIndex() == 1
    }

    def 'A boolean tab selection binding is not overridden by a fixed selection index.'()
    {
        reportInfo """
            When a fixed selection index (`withSelectedIndex(int)`) is combined with a tab
            whose selected state is bound to a boolean property (`isSelectedIf(..)`), the
            boolean binding, being the more specific and later applied preference, must win.
            The fixed index must not silently override it.
        """
        given : 'Two boolean selection properties, the second one requesting its tab to be selected.'
            var tab1Selected = Var.of(false)
            var tab2Selected = Var.of(true)
        and : 'A tabbed pane with a fixed index 0, but tab 2 bound to a TRUE selection boolean.'
            def pane =
                UI.tabbedPane().withSelectedIndex(0)
                .add(UI.tab("Tab 1").isSelectedIf(tab1Selected))
                .add(UI.tab("Tab 2").isSelectedIf(tab2Selected))
                .get(JTabbedPane)

        expect : 'The boolean-driven selection (tab 2) is respected, not overwritten by the fixed index 0.'
            pane.getSelectedIndex() == 1
            tab2Selected.get() == true
            tab1Selected.get() == false
    }

    def 'A fixed selection index does not override a boolean selection change made before more tabs are added.'()
    {
        reportInfo """
            A more dynamic variant of the previous scenario: after a fixed selection index has
            been honored, changing a tab's bound selection boolean must move the selection, and
            a subsequent tab addition must not snap it back to the fixed index.
        """
        given : 'Per-tab boolean selection properties and a growing tab list.'
            var tab1Selected = Var.of(false)
            var tab2Selected = Var.of(false)
            var tab3Selected = Var.of(false)
            var booleans = [tab1Selected, tab2Selected, tab3Selected]
            var models = Vars.of("Tab 1", "Tab 2")
            TabSupplier<String> supplier = (String title) ->
                UI.tab(title).isSelectedIf(booleans[Integer.parseInt(title.substring(4)) - 1])
        and : 'A tabbed pane with a fixed initial selection index of 0 and boolean-bound, list-driven tabs.'
            def pane =
                UI.tabbedPane().withSelectedIndex(0)
                .addAll(models, supplier)
                .get(JTabbedPane)

        expect : 'The fixed index selected the first tab.'
            pane.getSelectedIndex() == 0
            tab1Selected.get() == true

        when : 'We select the second tab through its boolean property.'
            tab2Selected.set(true)
            UI.sync()

        then : 'The second tab becomes selected.'
            pane.getSelectedIndex() == 1
            tab1Selected.get() == false
            tab2Selected.get() == true

        when : 'A third tab is added afterwards through the bound list.'
            models.add("Tab 3")
            UI.sync()

        then : 'The fixed index does NOT snap the selection back to 0; the boolean selection is preserved.'
            pane.getTabCount() == 3
            pane.getSelectedIndex() == 1
            tab2Selected.get() == true
    }

    def 'Inserting a tab before the current selection keeps the same tab selected and updates the bound index.'()
    {
        reportInfo """
            When a tab is inserted at or before the currently selected tab, Swing shifts
            the selection index so that the same tab stays selected. With a selection
            index property bound to the tabbed pane, this shift must not be suppressed:
            the selected tab must keep its selection and the bound property must be
            updated to the new index of that tab.
        """
        given : 'A selection index property pointing at the second tab and a three element tab list.'
            var index = Var.of(1)
            var models = Vars.of("Tab 1", "Tab 2", "Tab 3")
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
        and : 'A tabbed pane whose selection index is bound and whose tabs come from the property list.'
            def pane =
                UI.tabbedPane().withSelectedIndex(index)
                .addAll(models, supplier)
                .get(JTabbedPane)

        expect : 'The second tab is selected.'
            pane.getSelectedIndex() == 1
            pane.getTitleAt(pane.getSelectedIndex()) == "Tab 2"

        when : 'We insert a new tab at the front, before the selection.'
            models.addAt(0, "Tab 0")
            UI.sync()

        then : 'The same tab is still selected, now at its shifted index.'
            pane.getTabCount() == 4
            pane.getSelectedIndex() == 2
            pane.getTitleAt(pane.getSelectedIndex()) == "Tab 2"
        and : 'The bound property was updated to the new index of the selected tab.'
            index.get() == 2
    }

    def 'Inserting a tab before the current selection keeps the tab selection booleans in sync.'()
    {
        reportInfo """
            When a tab is inserted at or before the currently selected tab, Swing shifts
            the selection index so that the same tab stays selected. Tabs whose selected
            state is bound to a boolean property must stay in sync with this shift:
            the boolean of the selected tab remains true, all others remain false.
        """
        given : 'Per-tab boolean selection properties and a two element tab list.'
            var selectionFlags = [
                    "Tab 0" : Var.of(false),
                    "Tab 1" : Var.of(true),
                    "Tab 2" : Var.of(false)
                ]
            var models = Vars.of("Tab 1", "Tab 2")
            TabSupplier<String> supplier = (String title) -> UI.tab(title).isSelectedIf(selectionFlags[title])
        and : 'A tabbed pane with boolean-bound, list-driven tabs, where the first tab starts out selected.'
            def pane =
                UI.tabbedPane()
                .addAll(models, supplier)
                .get(JTabbedPane)

        expect : 'The first tab is selected, as requested by its boolean property.'
            pane.getSelectedIndex() == 0
            pane.getTitleAt(pane.getSelectedIndex()) == "Tab 1"

        when : 'We insert a new tab at the front, before the selection.'
            models.addAt(0, "Tab 0")
            UI.sync()

        then : 'The same tab is still selected, now at its shifted index.'
            pane.getTabCount() == 3
            pane.getSelectedIndex() == 1
            pane.getTitleAt(pane.getSelectedIndex()) == "Tab 1"
        and : 'The boolean properties still reflect which tab is selected.'
            selectionFlags["Tab 0"].get() == false
            selectionFlags["Tab 1"].get() == true
            selectionFlags["Tab 2"].get() == false
    }

    def 'Replacing the tuple item behind the selected tab neither moves the selection nor writes to the bound index.'()
    {
        reportInfo """
            When a tab model inside a bound tuple is replaced through `setAt(..)`,
            the corresponding tab is rebuilt in place. This is a same-size, same-position
            replacement, so the selection must not move — historically the naive
            remove + insert first shifted the selection off the removed tab and then
            past the inserted one, bumping it to the neighbouring tab and leaking
            these spurious intermediate indices into the bound selection property.
        """
        given : 'A tuple of tab models, a selection index property and a trace of every write to it.'
            var models = Var.of(Tuple.of("Alpha", "Beta", "Gamma"))
            var selectedIndex = Var.of(1)
            var trace = []
            Viewable.cast(selectedIndex).onChange(From.ALL, it -> trace << it.currentValue().orElseThrowUnchecked())
        and : 'A tabbed pane bound to both.'
            TabSupplier<String> supplier = model -> UI.tab(model).add(UI.label("content of " + model))
            def pane =
                UI.tabbedPane().withSelectedIndex(selectedIndex)
                .addAll(models, supplier)
                .get(JTabbedPane)
            UI.sync()
        expect : 'The second tab is selected, as requested by the property.'
            pane.getSelectedIndex() == 1
            pane.getTitleAt(1) == "Beta"

        when : 'We replace the model of the selected tab.'
            models.update( tuple -> tuple.setAt(1, "Beta 2.0") )
            UI.sync()

        then : 'The tab was rebuilt in place and is still the selected one.'
            pane.getTabCount() == 3
            pane.getTitleAt(1) == "Beta 2.0"
            pane.getSelectedIndex() == 1
        and : 'The bound index property never received a single write.'
            selectedIndex.get() == 1
            trace == []

        when : 'We also replace the model of a tab before the selection.'
            models.update( tuple -> tuple.setAt(0, "Alpha 2.0") )
            UI.sync()

        then : 'Again, the selection and the bound property are completely untouched.'
            pane.getTitleAt(0) == "Alpha 2.0"
            pane.getSelectedIndex() == 1
            selectedIndex.get() == 1
            trace == []
    }

    def 'An enum based tab selection stays consistent when jumping between distant tabs.'()
    {
        reportInfo """
            The enum overload `isSelectedIf(E, Var<E>)` bridges an enum property
            into the boolean selection flags of the individual tabs.
            Historically, changing the enum could bounce between the flag of the
            previously selected tab and the newly selected one, because the
            "deselect" event re-asserted the still current tab's flag, resurrecting
            the old enum value — a feedback loop which corrupted the property with
            stale view writes and could escalate into a StackOverflowError.
            Here we ensure that jumps across the tab strip settle in one
            consistent state, without any stale write backs to the enum property.
        """
        given : 'An enum property, a trace of all view-channel writes to it, and one tab per enum state.'
            var day = Var.of(DayOfWeek.MONDAY)
            var viewChannelWrites = []
            Viewable.cast(day).onChange(From.VIEW, it -> viewChannelWrites << it.currentValue().orElseThrowUnchecked())
            def pane =
                UI.tabbedPane()
                .add(UI.tab("Mon").isSelectedIf(DayOfWeek.MONDAY,    day))
                .add(UI.tab("Tue").isSelectedIf(DayOfWeek.TUESDAY,   day))
                .add(UI.tab("Wed").isSelectedIf(DayOfWeek.WEDNESDAY, day))
                .get(JTabbedPane)
        expect : 'The initial enum value selected the first tab.'
            pane.getSelectedIndex() == 0

        when : 'We jump from the first to the last state, on the UI thread.'
            UI.runNow(() -> day.set(DayOfWeek.WEDNESDAY))
            UI.sync()

        then : 'The pane and the property agree...'
            pane.getSelectedIndex() == 2
            day.get() == DayOfWeek.WEDNESDAY
        and : '...and the property never received a stale view write.'
            viewChannelWrites == []

        when : 'We jump all the way back.'
            UI.runNow(() -> day.set(DayOfWeek.MONDAY))
            UI.sync()

        then : 'Everything agrees again, still without stale writes.'
            pane.getSelectedIndex() == 0
            day.get() == DayOfWeek.MONDAY
            viewChannelWrites == []

        when : 'The user selects the middle tab in the view.'
            UI.runNow(() -> pane.setSelectedIndex(1))
            UI.sync()

        then : 'The property follows, and it was updated through the view channel exactly once.'
            day.get() == DayOfWeek.TUESDAY
            viewChannelWrites == [DayOfWeek.TUESDAY]
    }

    def 'Setting the selection flag of the currently selected tab to false deselects the whole pane.'()
    {
        reportInfo """
            A boolean selection flag is a two-way binding: `true` selects the tab,
            so `false` on the currently selected tab deselects it — leaving the
            pane with no selection at all instead of silently flipping the
            property back to `true`.
        """
        given : 'A tabbed pane with a boolean selection flag on the second tab.'
            var tab2Selected = Var.of(false)
            def pane =
                UI.tabbedPane()
                .add(UI.tab("Tab 1"))
                .add(UI.tab("Tab 2").isSelectedIf(tab2Selected))
                .add(UI.tab("Tab 3"))
                .get(JTabbedPane)

        when : 'We select the second tab through its flag.'
            tab2Selected.set(true)
            UI.sync()

        then : 'It is selected.'
            pane.getSelectedIndex() == 1

        when : 'We set the flag of that very tab to false.'
            tab2Selected.set(false)
            UI.sync()

        then : 'Nothing is selected anymore, and the flag stays false.'
            pane.getSelectedIndex() == -1
            tab2Selected.get() == false
    }

    def 'Properties bound to a removed tab are harmless zombies which neither throw nor affect the pane.'()
    {
        reportInfo """
            Properties bound to a tab (its title, tooltip, icon, enabled state or
            selection flag) may outlive the tab itself. This happens naturally when
            tabs are bound to a tuple or property list and one of the models is
            removed while its properties remain part of the application state —
            for example when a tab hosts a little "close" button in its header
            whose click removes the model, after which the model change makes
            derived views (like a title view) fire one last time.

            Historically these zombie bindings resolved their tab index to -1 and
            then blew up: title/tooltip/icon/enabled updates threw an
            `IndexOutOfBoundsException` (logged to the error output), and a
            selection flag update would even deselect the entire pane, while a
            full deselection of the pane would flip the removed tab's flag back
            to true. All of these must simply be ignored instead.
        """
        given : 'We remember the real error stream, so the cleanup block can always restore it.'
            var realErr = System.err
        and : 'Three tab models, each with its own title and selection flag property.'
            var models = Vars.of("A", "B", "C")
            var titles = [ "A": Var.of("Tab A"), "B": Var.of("Tab B"), "C": Var.of("Tab C") ]
            var flags  = [ "A": Var.of(false),   "B": Var.of(false),   "C": Var.of(false)   ]
        and : 'A tabbed pane bound to the models, where every tab binds its title and selection flag.'
            TabSupplier<String> supplier = model ->
                                                UI.tab(titles[model])
                                                .isSelectedIf(flags[model])
                                                .add(UI.label("content of " + model))
            def pane = UI.tabbedPane().addAll(models, supplier).get(JTabbedPane)
        and : 'The second tab is selected by the user.'
            UI.runNow(() -> pane.setSelectedIndex(1))
        expect :
            pane.getTabCount() == 3
            flags["B"].get() == true

        when : 'The first tab model is removed, its title and flag properties staying alive.'
            models.remove("A")
            UI.sync()

        then : 'The previously selected tab is still the selected one, at its shifted index.'
            pane.getTabCount() == 2
            pane.getSelectedIndex() == 0
            pane.getTitleAt(0) == "Tab B"

        when : 'The zombie title property fires again, while we capture the error output.'
            var errorOutput = new ByteArrayOutputStream()
            System.setErr(new PrintStream(errorOutput))
            try {
                titles["A"].set("Zombie A")
                UI.sync()
            } finally {
                System.setErr(realErr)
            }

        then : 'No IndexOutOfBoundsException was logged and the remaining tabs are untouched.'
            !errorOutput.toString().contains("IndexOutOfBoundsException")
            pane.getTitleAt(0) == "Tab B"
            pane.getTitleAt(1) == "Tab C"

        when : 'The zombie selection flag is set to true.'
            flags["A"].set(true)
            UI.sync()

        then : 'It does not hijack the pane: the selection and the other flags are unaffected.'
            pane.getSelectedIndex() == 0
            flags["B"].get() == true

        when : 'The user deselects everything (and we reset the zombie flag first).'
            flags["A"].set(false)
            UI.sync()
            UI.runNow(() -> pane.setSelectedIndex(-1))

        then : 'The zombie flag is not flipped back to true by the pane wide deselection.'
            pane.getSelectedIndex() == -1
            flags["A"].get() == false
            flags["B"].get() == false

        when : 'The title binding of a surviving tab is used.'
            titles["B"].set("Tab B (renamed)")
            UI.sync()

        then : 'It still works as usual.'
            pane.getTitleAt(0) == "Tab B (renamed)"

        cleanup : 'The global error stream is restored no matter how this feature ends.'
            System.setErr(realErr)
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
                    .add(UI.tabbedPane().id("super").addAll(vms, viewer))
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

    def 'Content rich tabs can be represented dynamically from a tuple property.'() {
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
            you can bind it to a view using a `Tuple` based `Val` property wrapping your tabs.
            When the tuple property changes, the view will be updated automatically.
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
        and : 'A tuple property storing the view models.'
            var models = Tuple.of(vm1, vm2, vm3, vm4)
            var vms = Var.of(models)
        and : 'Finally a view which binds to the view model property list.'
            var ui = UI.panel()
                    .add(UI.label("Dynamic Super View:"))
                    .add(UI.tabbedPane().id("super").addAll(vms, viewer))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'We query the UI for the views and verify that the "super" and "sub-1" views are present.'
            new Utility.Query(panel).find(JTabbedPane, "super").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-1").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-2").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-3").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-4").isPresent()
        when : 'We remove something from the view model property list.'
            vms.update( it -> it.remove(vm2) )
            UI.sync()
        then : 'We expect all views to be present except for the "sub-2" view.'
            new Utility.Query(panel).find(JTabbedPane, "super").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-1").isPresent()
            !new Utility.Query(panel).find(JPanel, "sub-2").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-3").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-4").isPresent()
        and : 'We remove something else from the view model property list but this time, for a change, use the index.'
            vms.update( it -> it.removeAt(2) ) // vm4
            UI.sync()
        then : 'We expect all views to be present except for the "sub-2" and "sub-4" views.'
            new Utility.Query(panel).find(JTabbedPane, "super").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-1").isPresent()
            !new Utility.Query(panel).find(JPanel, "sub-2").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-3").isPresent()
            !new Utility.Query(panel).find(JPanel, "sub-4").isPresent()
        when : 'We reintroduce "vm2"...'
            vms.update( it -> it.add(vm2) )
            UI.sync()
        then : 'We expect all views to be present except for the "sub-4" view.'
            new Utility.Query(panel).find(JTabbedPane, "super").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-1").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-2").isPresent()
            new Utility.Query(panel).find(JPanel, "sub-3").isPresent()
            !new Utility.Query(panel).find(JPanel, "sub-4").isPresent()

        when : 'We clear the view model property list.'
            vms.update( it -> it.clear() )
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
                            .addAll(tabs, supplier)
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

    def 'You can bind a tuple property and a tab supplier to dynamically add or remove tabs.'() {
        reportInfo """
            You can bind a string based tuple property and a tab supplier 
            to dynamically add or remove tabs.
        """
        given: 'A string tuple property, a tab supplier and a tabbed pane UI node.'
            var tuple = Tuple.of("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5")
            var tabs = Var.of(tuple)
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
            def tabbedPane =
                    UI.tabbedPane(UI.Side.TOP)
                            .addAll(tabs, supplier)
                            .get(JTabbedPane)

        when: 'We remove the tab at index 1.'
            tabs.update( it -> it.removeAt(1) )
            UI.sync()
        then: 'The tabbed pane is updated and the tab removed.'
            tabbedPane.getTabCount() == tabs.get().size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 3"
            tabbedPane.getTitleAt(2) == "Tab 4"
            tabbedPane.getTitleAt(3) == "Tab 5"

        when: 'We remove 2 tabs starting from index 1.'
            tabs.update( it -> it.removeAt(1, 2) )
            UI.sync()
        then: 'The tabbed pane is updated and the tabs removed.'
            tabbedPane.getTabCount() == tabs.get().size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 5"

        when: 'We update the tab at index 1.'
            tabs.update( it -> it.setAt(1, "Tab 2") )
            UI.sync()
        then: 'The tabbed pane is updated and the tab updated.'
            tabbedPane.getTabCount() == tabs.get().size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 2"

        when: 'We add a tab.'
            tabs.update( it -> it.add("Tab 3") )
            UI.sync()
        then: 'The tabbed pane is updated and the tab added.'
            tabbedPane.getTabCount() == tabs.get().size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 2"
            tabbedPane.getTitleAt(2) == "Tab 3"

        when: 'We add 2 tabs.'
            tabs.update( it -> it.addAll("Tab 4", "Tab 5") )
            UI.sync()
        then: 'The tabbed pane is updated and the tabs added.'
            tabbedPane.getTabCount() == tabs.get().size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 2"
            tabbedPane.getTitleAt(2) == "Tab 3"
            tabbedPane.getTitleAt(3) == "Tab 4"
            tabbedPane.getTitleAt(4) == "Tab 5"

        when: 'We insert 1 tab.'
            tabs.update( it -> it.addAt(0, "Tab 0") )
            UI.sync()
        then: 'The tabbed pane is updated and the tabs inserted.'
            tabbedPane.getTabCount() == tabs.get().size()
            tabbedPane.getTitleAt(0) == "Tab 0"
            tabbedPane.getTitleAt(1) == "Tab 1"
            tabbedPane.getTitleAt(2) == "Tab 2"
            tabbedPane.getTitleAt(3) == "Tab 3"
            tabbedPane.getTitleAt(4) == "Tab 4"
            tabbedPane.getTitleAt(5) == "Tab 5"
    }

    def 'A property list is bound to a tabbed pane compute efficiently.'(
        List<Integer> diff, Closure<Tuple> operation
    ) {
        reportInfo """
            You can bind a string based property list and a tab supplier 
            to dynamically add or remove tabs. The GUI will only update the
            tabs that have changed.
        """
        given: 'A string based property list, a tab supplier and a tabbed pane UI node.'
            var tabs = Vars.of("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5")
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
            def pane =
                    UI.tabbedPane(UI.Side.TOP)
                            .addAll(tabs, supplier)
                            .get(JTabbedPane)
        and : 'We unpack the pane and the expected differences:'
            var iniComps = (0..<pane.getTabCount()).collect({pane.getComponentAt(it)})

        when: 'We run the operation on the tuple...'
            operation(tabs)
            UI.sync()
        and : 'We unpack the updated components:'
            var updatedComps = (0..<pane.getTabCount()).collect({pane.getComponentAt(it)})
        then: 'The tabbed pane is updated.'
            pane.getTabCount() == tabs.size()
            pane.getTabCount() == diff.findAll( it -> it == _ || it >= 0 ).size()
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
            [0, -1, 2, 3, 4]     | { it.removeAt(1) }
            [0, -1, -1, 3, 4]    | { it.removeAt(1, 2) }
            [0, _, 2, 3, 4]      | { it.setAt(1, "Tab X") }
            [0, _, _, 3, 4]      | { it.setAllAt(1, "Tab 3", "Tab 2") }
            [_, _, _, _, 4]      | { it.setAllAt(0, "Tab 4", "Tab 3", "Tab 2", "Tab 1") }
            [0, _, _, _, _]      | { it.setAllAt(1, "Tab 4", "Tab 3", "Tab 2", "Tab 5") }
            [0, 1, 2, _, _]      | { it.setAllAt(3, "Tab 5", "Tab 4") }
            [0, 1, 2, 3, 4, _]   | { it.add("Tab X") }
            [0, 1, 2, 3, 4, _, _]| { it.addAll("Tab X", "Tab Y") }
            [_, 0, 1, 2, 3, 4]   | { it.addAt(0, "Tab X") }
    }

    def 'You can bind a tuple property and a tab supplier to dynamically add or remove tabs.'() {
        reportInfo """
            You can bind a string based tuple property and a tab supplier 
            to dynamically add or remove tabs.
        """
        given: 'A string tuple property, a tab supplier and a tabbed pane UI node.'
            var tuple = Tuple.of("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5")
            var tabs = Var.of(tuple)
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
            def tabbedPane =
                    UI.tabbedPane(UI.Side.TOP)
                            .addAll(tabs, supplier)
                            .get(JTabbedPane)

        when: 'We remove the tab at index 1.'
            tabs.update( it -> it.removeAt(1) )
            UI.sync()
        then: 'The tabbed pane is updated and the tab removed.'
            tabbedPane.getTabCount() == tabs.get().size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 3"
            tabbedPane.getTitleAt(2) == "Tab 4"
            tabbedPane.getTitleAt(3) == "Tab 5"

        when: 'We remove 2 tabs starting from index 1.'
            tabs.update( it -> it.removeAt(1, 2) )
            UI.sync()
        then: 'The tabbed pane is updated and the tabs removed.'
            tabbedPane.getTabCount() == tabs.get().size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 5"

        when: 'We update the tab at index 1.'
            tabs.update( it -> it.setAt(1, "Tab 2") )
            UI.sync()
        then: 'The tabbed pane is updated and the tab updated.'
            tabbedPane.getTabCount() == tabs.get().size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 2"

        when: 'We add a tab.'
            tabs.update( it -> it.add("Tab 3") )
            UI.sync()
        then: 'The tabbed pane is updated and the tab added.'
            tabbedPane.getTabCount() == tabs.get().size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 2"
            tabbedPane.getTitleAt(2) == "Tab 3"

        when: 'We add 2 tabs.'
            tabs.update( it -> it.addAll("Tab 4", "Tab 5") )
            UI.sync()
        then: 'The tabbed pane is updated and the tabs added.'
            tabbedPane.getTabCount() == tabs.get().size()
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 2"
            tabbedPane.getTitleAt(2) == "Tab 3"
            tabbedPane.getTitleAt(3) == "Tab 4"
            tabbedPane.getTitleAt(4) == "Tab 5"

        when: 'We insert 1 tab.'
            tabs.update( it -> it.addAt(0, "Tab 0") )
            UI.sync()
        then: 'The tabbed pane is updated and the tabs inserted.'
            tabbedPane.getTabCount() == tabs.get().size()
            tabbedPane.getTitleAt(0) == "Tab 0"
            tabbedPane.getTitleAt(1) == "Tab 1"
            tabbedPane.getTitleAt(2) == "Tab 2"
            tabbedPane.getTitleAt(3) == "Tab 3"
            tabbedPane.getTitleAt(4) == "Tab 4"
            tabbedPane.getTitleAt(5) == "Tab 5"
    }

    def 'A tuple property is bound to a tabbed pane compute efficiently.'(
        List<Integer> diff, Closure<Tuple> operation
    ) {
        reportInfo """
            You can bind a string based tuple property and a tab supplier 
            to dynamically add or remove tabs. The GUI will only update the
            tabs that have changed.
        """
        given: 'A string tuple property, a tab supplier and a tabbed pane UI node.'
            var tuple = Tuple.of("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5")
            var models = Var.of(tuple)
            TabSupplier<String> supplier = (String title) -> UI.tab(title)
            def pane =
                    UI.tabbedPane(UI.Side.TOP)
                            .addAll(models, supplier)
                            .get(JTabbedPane)
        and : 'We unpack the pane and the expected differences:'
            var iniComps = (0..<pane.getTabCount()).collect({pane.getComponentAt(it)})

        when: 'We run the operation on the tuple...'
            models.update( it -> operation(it) )
            UI.sync()
        and : 'We unpack the updated components:'
            var updatedComps = (0..<pane.getTabCount()).collect({pane.getComponentAt(it)})
        then: 'The tabbed pane is updated.'
            pane.getTabCount() == models.get().size()
            pane.getTabCount() == diff.findAll( it -> it == _ || it >= 0 ).size()
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
            [0, _, 2, 3, 4]      | { it.setAt(1, "Tab X") }
            [0, _, _, 3, 4]      | { it.setAllAt(1, "Tab 3", "Tab 2") }
            [_, _, _, _, 4]      | { it.setAllAt(0, "Tab 4", "Tab 3", "Tab 2", "Tab 1") }
            [0, _, _, _, _]      | { it.setAllAt(1, "Tab 4", "Tab 3", "Tab 2", "Tab 5") }
            [0, 1, 2, _, _]      | { it.setAllAt(3, "Tab 5", "Tab 4") }
            [0, 1, 2, 3, 4, _]   | { it.add("Tab X") }
            [0, 1, 2, 3, 4, _, _]| { it.addAll("Tab X", "Tab Y") }
            [_, 0, 1, 2, 3, 4]   | { it.addAt(0, "Tab X") }
            [-1, 1, 2, 3, -1]    | { it.slice(1, 4) }
            [0, 1, -1, -1, -1]   | { it.sliceFirst(2) }
            [-1, -1, 2, 3, 4]    | { it.sliceLast(3) }
            [-1, -1, -1, -1, -1] | { it.clear() }
            [_, _, _, _, _]      | { Tuple.of("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5") }
            [_, _, _, _, _]      | { it.clear().addAll("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5") }
            [_, _, _, _, _]      | { Tuple.of("Tab a", "Tab b", "Tab c", "Tab d", "Tab e") }
            [_, _, _, _, _]      | { it.clear().addAll("Tab a", "Tab b", "Tab c", "Tab d", "Tab e") }
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
            def ui = UI.tabbedPane(UI.Side.TOP).addAll(tabs, supplier)

        when : 'We build the component.'
            var tabbedPane = ui.get(JTabbedPane)
        then : 'First of all, the exception does not leak to the outside.'
            noExceptionThrown()

        and : 'We expect the tabbed pane to have 5 tabs, even though one of them is an error tab.'
            tabbedPane.getTabCount() == 5
        and : 'We expect the error tab to have the correct title.'
            tabbedPane.getTitleAt(2).contains("Error")
    }

    def 'An exception in the tab supplier for a model tuple property, produces an error tab instead.'()
    {
        reportInfo """
            A fundamental requirement when it comes to binding a tuple of models to
            a set of UI components is that the number of models and the number of
            UI components must match. If they do not, the tuple property change listeners 
            will not know which model corresponds to which UI component.
            
            This is why in case of an exception, a sort of dummy tab is created.
            It indicates that something went wrong and the tab could not be created.
        """
        given : 'A tuple property and a tab supplier that throws an exception.'
            var tuple = Tuple.of("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5")
            var tabs = Var.of(tuple)
            TabSupplier<String> supplier = (String title) -> {
                if ( title == "Tab 3" ) {
                    throw new RuntimeException("This tab could not be created!")
                }
                return UI.tab(title)
            }
        and : 'A UI declaration with a tabbed pane bound to the tuple property and the tab supplier.'
            def ui = UI.tabbedPane(UI.Side.TOP).addAll(tabs, supplier)

        when : 'We build the component.'
            var tabbedPane = ui.get(JTabbedPane)
        then : 'First of all, the exception does not leak to the outside.'
            noExceptionThrown()

        and : 'We expect the tabbed pane to have 5 tabs, even though one of them is an error tab.'
            tabbedPane.getTabCount() == 5
        and : 'We expect the error tab to have the correct title.'
            tabbedPane.getTitleAt(2).contains("Error")
    }

    def 'If the tab supplier for a model property list returns `null`, a null tab is shown instead'()
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
            def ui = UI.tabbedPane(UI.Side.TOP).addAll(tabs, supplier)

        when : 'We build the component.'
            var tabbedPane = ui.get(JTabbedPane)
        then : 'We expect the tabbed pane to have 5 tabs, even though one of them is a null tab.'
            tabbedPane.getTabCount() == 5
        and : 'We expect the null tab to have a title which indicates that content is missing.'
            tabbedPane.getTitleAt(2).contains("Empty")
    }


    def 'If the tab supplier for a model tuple property returns `null`, a null tab is shown instead'()
    {
        reportInfo """
            A fundamental requirement when it comes to binding a tuple of models to
            a set of UI components is that the number of models and the number of
            UI components must match. If they do not, the tuple property change listeners 
            will not know which model corresponds to which UI component.
            
            So in case of a `null` return value, a sort of dummy tab is created and
            added to the tabbed pane so that the number of tabs and models match.
            It also indicates that something went wrong and the tab could not be created.
        """
        given : 'A tuple property and a tab supplier that returns `null`.'
            var tuple = Tuple.of("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5")
            var tabs = Var.of(tuple)
            TabSupplier<String> supplier = (String title) -> {
                if ( title == "Tab 3" ) {
                    return null
                }
                return UI.tab(title)
            }
        and : 'A UI declaration with a tabbed pane bound to the property list and the tab supplier.'
            def ui = UI.tabbedPane(UI.Side.TOP).addAll(tabs, supplier)

        when : 'We build the component.'
            var tabbedPane = ui.get(JTabbedPane)
        then : 'We expect the tabbed pane to have 5 tabs, even though one of them is a null tab.'
            tabbedPane.getTabCount() == 5
        and : 'We expect the null tab to have a title which indicates that content is missing.'
            tabbedPane.getTitleAt(2).contains("Empty")
    }
}
