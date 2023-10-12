package swingtree.panel


import swingtree.SwingTree
import swingtree.UIForBox
import swingtree.components.JBox
import swingtree.threading.EventProcessor
import swingtree.UI
import swingtree.UIForPanel
import sprouts.Var
import net.miginfocom.swing.MigLayout
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import javax.swing.JPanel
import java.awt.FlowLayout

@Title("Panels")
@Narrative('''

    Just like in regular Swing, the JPanel is the most basic 
    yet most important type of component in Swing-Tree
    and you can create one using the `UI.panel()` factory method. 
    Don't hesitate to use as the main tool for grouping and structuring
    your UI, just like you would use the 'div' tag in HTML.

''')
@Subject([UIForPanel])
class Panel_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def cleanupSpec() {
        SwingTree.reinitialize()
    }

    def 'A panel node can be created using the UI.panel() factory method.'() {
        when : 'We create a panel...'
            def ui = UI.panel()
        then : 'The panel UI is not null.'
            ui != null
        and : 'The UI node wraps a JPanel.'
            ui.component instanceof JPanel
    }

    def 'A panel can be created with a layout manager.'() {
        when : 'We create a panel with a layout manager...'
            def ui = UI.panel().withLayout(new FlowLayout())
        then : 'The panel UI is not null.'
            ui != null
        and : 'The UI node wraps a JPanel.'
            ui.component instanceof JPanel
        and : 'The panel has a FlowLayout.'
            ui.component.layout instanceof FlowLayout
    }

    def 'A transparent panel can be created with a custom flow layout manager.'() {
        when : 'We create a panel with a layout manager...'
            def ui = UI.box().withLayout(new FlowLayout())
        then : 'The panel UI is not null.'
            ui != null
        and : 'The UI node wraps a JBox.'
            ui.component instanceof JBox
        and : 'The panel has a FlowLayout.'
            ui.component.layout instanceof FlowLayout
    }

    def 'All of the "box(..)" factory methods will create transparent panels without insets.'(
        UIForBox<JBox> ui
    ) {
        expect : 'The panel is transparent.'
            ui.component.isOpaque() == false
        and : 'The panel has no insets.'
            ui.component.layout.layoutConstraints.contains("ins 0")

        where : 'The following UI nodes are created using the "box(..)" factory methods.'
            ui << [
                    UI.box(),
                    UI.box("fill"),
                    UI.box("fill", "[grow]12[shrink]"),
                    UI.box("fill", "[grow]12[shrink]", "[grow]12[shrink]"),
                    UI.box(UI.FILL & UI.FLOW_X),
                    UI.box(UI.FILL & UI.FLOW_X, "[grow]12[shrink]"),
                    UI.box(UI.FILL & UI.FLOW_X, "[grow]12[shrink]", "[grow]12[shrink]"),
                ]
    }

    def 'The default layout manager is always a MigLayout'()
    {
        given : 'We create a panel...'
            def ui = UI.panel()
        expect : 'The panel has a MigLayout.'
            ui.component.layout instanceof MigLayout
    }

    def 'Panels can be nested using the `add(..) method.'()
    {
        given : 'We create a panel of panels:'
            def ui =
                    UI.panel()
                    .add(UI.panel())
                    .add(UI.panel())
                    .add(UI.panel())

        expect : 'The panel has 3 children.'
            ui.component.components.size() == 3
        and : 'All children are JPanels.'
            ui.component.components.every { it instanceof JPanel }
    }

    def 'The dimensions of a panel can be bound to a property.'( float uiScale )
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
            SwingTree.get().getUIScale().setUserScaleFactor(uiScale)
        and : 'A simple property modelling the width of a panel.'
            var width = Var.of(300)
        and : 'A simple property modelling the height of a panel.'
            var height = Var.of(200)
        and : 'A panel bound to both properties'
            def ui = UI.panel()
                        .withPrefWidth(width)
                        .withPrefHeight(height)

        expect : 'The panel has the correct width and height.'
            ui.component.preferredSize.width == (int) ( 300 * uiScale )
            ui.component.preferredSize.height == (int) ( 200 * uiScale )

        when : 'We change the width of the panel.'
            width.set(400)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The panel has the correct width and height.'
            ui.component.preferredSize.width == (int) ( 400 * uiScale )
            ui.component.preferredSize.height == (int) ( 200 * uiScale )

        when : 'We change the height of the panel.'
            height.set(300)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The panel has the correct width and height.'
            ui.component.preferredSize.width == (int) ( 400 * uiScale )
            ui.component.preferredSize.height == (int) ( 300 * uiScale )
        where :
            uiScale << [ 1.0f, 1.5f, 2.0f ]
    }

}
