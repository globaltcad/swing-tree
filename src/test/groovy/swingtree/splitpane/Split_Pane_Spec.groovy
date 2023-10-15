package swingtree.splitpane

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.SwingTree
import swingtree.threading.EventProcessor
import swingtree.UI
import sprouts.Var

import javax.swing.JSplitPane

@Title("Split Panes")
@Narrative('''

   In this specification you can can not only see how to use the Swing-Tree API to 
   create and configure split panes but also how to bind them to your view model model.
   The alignment of a split pane for example can be bound to a property in your view model.

''')
class Split_Pane_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def 'A horizontally aligned split pane can be created through the "splitPane" factory method.'()
    {
        given : 'We create a horizontally aligned split pane UI node.'
            var ui = UI.splitPane(UI.Align.HORIZONTAL)
        expect : 'The split pane is not null.'
            ui != null
        and : 'The split pane is a JSplitPane.'
            ui.component instanceof JSplitPane
        and : 'The split pane is horizontally aligned b, meaning it splits vertically.'
            ui.component.orientation == JSplitPane.VERTICAL_SPLIT
    }

    def 'A vertically aligned split pane can be created through the "splitPane" factory method.'()
    {
        given : 'We create a vertically aligned split pane UI node.'
            var ui = UI.splitPane(UI.Align.VERTICAL)
        expect : 'The split pane is not null.'
            ui != null
        and : 'The split pane is a JSplitPane.'
            ui.component instanceof JSplitPane
        and : 'The split pane is vertically aligned, meaning it splits horizontally.'
            ui.component.orientation == JSplitPane.HORIZONTAL_SPLIT
    }

    def 'An alignment property can be used to dynamically model the alignment of your split pane.'()
    {
        reportInfo """
            Note that the property shown in this example would be part of your view model.
            So you can simply modify it as part of your business logic and the split pane
            will automatically update its alignment.
        """
        given : 'We create a simple view model property holding the alignment of our split pane.'
            var alignment = Var.of(UI.Align.HORIZONTAL)
        and : 'We create a split pane UI node bound to the property.'
            var ui = UI.splitPane(alignment)
        expect : 'The split pane is not null.'
            ui != null
        and : 'The split pane is a JSplitPane.'
            ui.component instanceof JSplitPane
        and : 'The split pane is horizontally aligned, meaning it splits vertically.'
            ui.component.orientation == JSplitPane.VERTICAL_SPLIT
        when : 'We change the alignment property to "VERTICAL".'
            alignment.set(UI.Align.VERTICAL)
            UI.sync()
        then : 'The split pane is vertically aligned, meaning it splits horizontally.'
            ui.component.orientation == JSplitPane.HORIZONTAL_SPLIT
    }

    def 'A split pane can be configured with a divider size.'( float uiScale )
    {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a split pane UI node with a divider size of 10.'
            var ui = UI.splitPane(UI.Align.HORIZONTAL).withDividerSize(10)
        expect : 'The split pane is not null.'
            ui != null
        and : 'The split pane is a JSplitPane.'
            ui.component instanceof JSplitPane
        and : 'The split pane is horizontally aligned, meaning it splits vertically.'
            ui.component.orientation == JSplitPane.VERTICAL_SPLIT
        and : 'The divider size is 10.'
            ui.component.dividerSize == (int) ( 10 * uiScale )
        where :
            uiScale << [ 1.0f, 1.5f, 2.0f ]
    }

    def 'A split pane can be configured with a divider location.'()
    {
        given : 'We create a split pane UI node with a divider location of 10.'
            var ui = UI.splitPane(UI.Align.HORIZONTAL).withDividerAt(10)
        expect : 'The split pane is not null.'
            ui != null
        and : 'The split pane is a JSplitPane.'
            ui.component instanceof JSplitPane
        and : 'The split pane is horizontally aligned, meaning it splits vertically.'
            ui.component.orientation == JSplitPane.VERTICAL_SPLIT
        and : 'The divider location is 10.'
            ui.component.dividerLocation == 10
    }

    def 'The divider location of a split pane can dynamically be modelled using an integer property.'()
    {
        reportInfo """
            Note that the property shown in this example would be part of your view model.
            So you can simply modify it as part of your business logic and the split pane
            will automatically update its divider location based on an observer
            registered on the property.
        """
        given : 'We create a simple view model property holding the divider location of our split pane.'
            var dividerLocation = Var.of(10)
        and : 'We create a split pane UI node bound to the property.'
            var ui = UI.splitPane(UI.Align.HORIZONTAL)
                            .withDividerAt(dividerLocation)
        expect : 'The split pane exists and it is indeed a horizontally aligned split pane splitting the vertical axis.'
            ui != null
            ui.component instanceof JSplitPane
            ui.component.orientation == JSplitPane.VERTICAL_SPLIT
        and : 'The divider location is 10.'
            ui.component.dividerLocation == 10
        when : 'We change the divider location property to 20.'
            dividerLocation.set(20)
            UI.sync()
        then : 'The divider location is 20.'
            ui.component.dividerLocation == 20
    }

    def 'A horizontally aligned split pane can be configured with a divider location as a percentage.'( float uiScale )
    {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
        SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a split pane UI node with a division of 50%.'
            var ui = UI.splitPane(UI.Align.HORIZONTAL)
                        .withWidth(42)
                        .withHeight(100)
                        .withDivisionOf(0.5)
        expect : 'The split pane is not null.'
            ui != null
        and : 'The split pane is a JSplitPane.'
            ui.component instanceof JSplitPane
        and : 'The split pane is horizontally aligned, meaning it splits vertically.'
            ui.component.orientation == JSplitPane.VERTICAL_SPLIT
        and : 'The divider location is 50 * uiScale.'
            ui.component.dividerLocation == (int) ( 50 * uiScale )
        where :
            uiScale << [ 1.0f, 1.5f, 2.0f ]
    }

    def 'A vertically aligned split pane can be configured with a divider location as a percentage.'( float uiScale )
    {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a split pane UI node with a division of 50%.'
            var ui = UI.splitPane(UI.Align.VERTICAL)
                        .withWidth(100)
                        .withHeight(42)
                        .withDivisionOf(0.5)
        expect : 'The split pane is not null.'
            ui != null
        and : 'The split pane is a JSplitPane.'
            ui.component instanceof JSplitPane
        and : 'The split pane is vertically aligned, meaning it splits horizontally.'
            ui.component.orientation == JSplitPane.HORIZONTAL_SPLIT
        and : 'The divider location is 50 * uiScale.'
            ui.component.dividerLocation == (int) ( 50 * uiScale )

        where :
            uiScale << [ 1.0f, 1.5f, 2.0f ]
    }

    def 'The division of a split pane can dynamically be configured through a double property.'( float uiScale )
    {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)

        and :  'We create a simple view model property holding the division of our split pane.'
            var divisionPercentage = Var.of(0.5d)
        and : 'We create a split pane UI node bound to the property.'
            var ui = UI.splitPane(UI.Align.HORIZONTAL)
                        .withWidth(42)
                        .withHeight(100)
                        .withDivisionOf(divisionPercentage)

        expect : 'The split pane is not null.'
            ui != null
        and : 'The split pane exists and it is indeed a horizontally aligned split pane splitting the vertical axis.'
            ui.component instanceof JSplitPane
            ui.component.orientation == JSplitPane.VERTICAL_SPLIT
            ui.component.dividerLocation == (int)(50 * uiScale)

        when : 'We change the division property to 0.25.'
            divisionPercentage.set(0.25d)
            UI.sync()
        then : 'The divider location is 25.'
            ui.component.dividerLocation == (int)(25 * uiScale)

        when : 'We change the division property to 0.75.'
            divisionPercentage.set(0.75d)
            UI.sync()
        then : 'The divider location is 75.'
            ui.component.dividerLocation == (int)(75 * uiScale)

        where:
            uiScale << [ 1f, 1.5f, 2f ]
    }

    def 'You can dynamically model the divider size of a split pane in your view model.'( float uiScale )
    {
        reportInfo """
            In a fully application, the divider size property in this example would be part of your view model
            containing the state of the UI as well as the business logic. So you can simply modify it as part of
            your business logic and the split pane will automatically update its divider size based on an observer
            registered on the property.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a simple view model property holding the divider size of our split pane.'
            var dividerSize = Var.of(10)
        and : 'We create a vertically aligned split pane UI node bound to the property.'
            var ui = UI.splitPane(UI.Align.VERTICAL).withDividerSize(dividerSize)
        expect : 'The split pane exists and it is indeed a vertically aligned split pane splitting the horizontal axis.'
            ui != null
            ui.component instanceof JSplitPane
            ui.component.orientation == JSplitPane.HORIZONTAL_SPLIT
        and : 'The divider size is 10.'
            ui.component.dividerSize == (int) ( 10 * uiScale )
        when : 'We change the divider size property to 42.'
            dividerSize.set(42)
            UI.sync()
        then : 'The divider size is 42.'
            ui.component.dividerSize == (int) ( 42 * uiScale )
        where:
            uiScale << [ 1f, 1.5f, 2f ]
    }
}
