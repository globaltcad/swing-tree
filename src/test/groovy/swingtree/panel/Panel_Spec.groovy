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
        SwingTree.clear()
    }

    def 'A panel node can be created using the UI.panel() factory method.'() {
        when : 'We create a panel...'
            def ui = UI.panel()
        and : 'We actually build the component:'
            def panel = ui.get(JPanel)
        then : 'The panel is not null.'
            panel != null
        and : 'We confirm that the panel is a JPanel.'
            panel instanceof JPanel
    }

    def 'A panel can be created with a layout manager.'() {
        when : 'We create a panel with a layout manager...'
            def ui = UI.panel().withLayout(new FlowLayout())
        and : 'We actually build the component:'
            def panel = ui.get(JPanel)
        then : 'The panel is not null.'
            panel != null
        and : 'The UI node wraps a JPanel.'
            panel instanceof JPanel
        and : 'The panel has a FlowLayout.'
            panel.layout instanceof FlowLayout
    }

    def 'A transparent panel can be created with a custom flow layout manager.'() {
        when : 'We create a panel with a layout manager...'
            def ui = UI.box().withLayout(new FlowLayout())
        and : 'We actually build the component:'
            def box = ui.get(JBox)
        then : 'The box is not null.'
            box != null
        and : 'The UI node wraps a JBox.'
            box instanceof JBox
        and : 'The panel has a FlowLayout.'
            box.layout instanceof FlowLayout
    }

    def 'All of the "box(..)" factory methods will create transparent panels without insets.'(
        UIForBox<JBox> ui
    ) {
        given : 'We create build the component...'
            var box = ui.get(JBox)
        expect : 'The panel is transparent.'
            box.isOpaque() == false
        and : 'The panel has no insets.'
            box.layout.layoutConstraints.contains("ins 0")

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
        and : 'We build the panel:'
            def panel = ui.get(JPanel)
        expect : 'The panel has a MigLayout.'
            panel.layout instanceof MigLayout
    }

    def 'Panels can be nested using the `add(..) method.'()
    {
        given : 'We create a panel of panels:'
            def ui =
                    UI.panel()
                    .add(UI.panel())
                    .add(UI.panel())
                    .add(UI.panel())
        and : 'We actually build the component:'
            def panel = ui.get(JPanel)

        expect : 'The panel has 3 children.'
            panel.components.size() == 3
        and : 'All children are JPanels.'
            panel.components.every { it instanceof JPanel }
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
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'A simple property modelling the width of a panel.'
            var width = Var.of(300)
        and : 'A simple property modelling the height of a panel.'
            var height = Var.of(200)
        and : 'A panel bound to both properties'
            def ui = UI.panel()
                        .withPrefWidth(width)
                        .withPrefHeight(height)
        and : 'We actually build the component:'
            def panel = ui.get(JPanel)

        expect : 'The panel has the correct width and height.'
            panel.preferredSize.width == (int) ( 300 * uiScale )
            panel.preferredSize.height == (int) ( 200 * uiScale )

        when : 'We change the width of the panel.'
            width.set(400)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The panel has the correct width and height.'
            panel.preferredSize.width == (int) ( 400 * uiScale )
            panel.preferredSize.height == (int) ( 200 * uiScale )

        when : 'We change the height of the panel.'
            height.set(300)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The panel has the correct width and height.'
            panel.preferredSize.width == (int) ( 400 * uiScale )
            panel.preferredSize.height == (int) ( 300 * uiScale )
        where :
            uiScale << [ 1.0f, 1.5f, 2.0f ]
    }

}
