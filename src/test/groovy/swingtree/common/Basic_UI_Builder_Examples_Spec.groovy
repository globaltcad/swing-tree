package swingtree.common


import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.Var
import swingtree.SwingTree
import swingtree.components.JBox
import swingtree.threading.EventProcessor
import swingtree.UI
import swingtree.input.Keyboard

import javax.swing.*
import javax.swing.event.ListSelectionListener
import java.awt.*
import java.awt.event.ComponentListener
import java.awt.event.FocusListener
import java.awt.event.KeyListener

@Title("Writing Declarative GUI Code")
@Narrative('''

    Imperative code is code that describes how you want to achieve something,
    typically a procedure or a sequence of steps that needs to be executed.
    Declarative code on the other hand is code that 
    describes **what you want to achieve**,
    typically a state or data structure that you want to create.
    This is why declarative code is the perfect fit for UI code,
    and making it possible to write declarative UI code is the 
    main purpose and core motivation of Swing-Tree.
    
    Its API is designed so that the code you write with it looks 
    and feels a little bit like HTML, CSS and JavaScript.
    
    It is inspired by frameworks like 
    [Jetpack Compose](https://developer.android.com/jetpack/compose), [SwiftUI](https://developer.apple.com/xcode/swiftui/), 
    [Flutter](https://flutter.dev) and 
    [JetBrain's UI DSL](https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html#ui-dsl-basics)
    which are also based on using declarative builder patterns to design your UI.
    
    In this specification we cover the utter most basics of SwingTree,
    so that you can get a feeling for how it works and what it can do for you.

''')
class Basic_UI_Builder_Examples_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def 'We can nest JPanel UI nodes to structure UIs.'()
    {
        reportInfo """
            Just like in regular Swing, the JPanel is the most basic 
            yet most important type of component in Swing-Tree
            and you can create one using the `UI.panel()` factory method. 
            Don't hesitate to use as the main tool for grouping and structuring
            your UI, just like you would use the 'div' tag in HTML.
        """
        given : 'We create a simple swing tree of JPanel instances.'
            var ui =
                    UI.panel()
                    .add(UI.panel())
                    .add(
                        UI.panel()
                        .add(UI.panel())
                        .add(UI.panel())
                    )
                    .add(UI.panel())

        expect : 'The UI node contains a root JPanel with 3 children.'
            ui.component instanceof JPanel
            ui.component.components.length == 3
        and : 'Because this is a regular Swing UI, we traverse the tree and find the children.'
            ui.component.components[0] instanceof JPanel
            ui.component.components[1] instanceof JPanel
            ui.component.components[2] instanceof JPanel
        and : 'We can also traverse the tree to find the children of the children.'
            ui.component.components[1].components[0] instanceof JPanel
            ui.component.components[1].components[1] instanceof JPanel
    }

    def 'We can use the `box()` factory to group UIs seemlesly.'()
    {
        reportInfo """
            A regular JPanel will be opaque by default and also have 
            a small padding/inset around it's content.
            However, sometimes you just want to group some UI elements
            without any padding, margin or insets and without any background color.
            This is where the `JBox` component comes in handy and you can create one
            using the `UI.box()` factory method.
            Don't hesitate to use it as the main tool for grouping and structuring
            your UI, just like you would use the 'div' tag in HTML.
        """
        given : 'We create a simple swing tree of JPanel instances.'
            var ui =
                    UI.box()
                    .add(UI.box())
                    .add(
                        UI.box()
                        .add(UI.panel())
                        .add(UI.panel())
                    )
                    .add(UI.box())

        expect : 'The UI node contains a root JBox with 3 children.'
            ui.component instanceof JBox
            ui.component.components.length == 3
        and : 'Because this is a regular Swing UI, we traverse the tree and find the children.'
            ui.component.components[0] instanceof JBox
            ui.component.components[1] instanceof JBox
            ui.component.components[2] instanceof JBox
        and : 'We can also traverse the tree to find the children of the children.'
            ui.component.components[1].components[0] instanceof JPanel
            ui.component.components[1].components[1] instanceof JPanel
        and : 'All the JPanel instances created with the `box()` factory methods are non-opacque and without insets!'
            ui.component.isOpaque() == false
            ui.component.components[0].isOpaque() == false
            ui.component.components[1].isOpaque() == false
            ui.component.components[2].isOpaque() == false
            ui.component.layout.layoutConstraints == "ins 0, hidemode 2, gap 0"
            ui.component.components[0].layout.layoutConstraints == "ins 0, hidemode 2, gap 0"
            ui.component.components[1].layout.layoutConstraints == "ins 0, hidemode 2, gap 0"
            ui.component.components[2].layout.layoutConstraints == "ins 0, hidemode 2, gap 0"
        and : 'The 2 innermost panels are opaque and have insets:'
            ui.component.components[1].components[0].isOpaque() == true
            ui.component.components[1].components[1].isOpaque() == true
            ui.component.components[1].components[0].layout.layoutConstraints == "hidemode 2"
            ui.component.components[1].components[1].layout.layoutConstraints == "hidemode 2"
    }

    def 'We can add a list of components to the swing tree API and get a builder node in return.'()
    {
        reportInfo """
            Just like in regular Swing, the JPanel is the most basic 
            yet most important type of component in Swing-Tree
            and you can create one using the `UI.panel()` factory method. 
            Don't hesitate to use as the main tool for grouping and structuring
            your UI, just like you would use the 'div' tag in HTML.
        """
        given : 'We have a simple JPanel UI node.'
            var ui = UI.panel()

        expect : 'At the beginning the wrapped component will have no children.'
            ui.component.components.length == 0

        when : 'We add a list of panels...'
            var ui2 = ui.add([new JPanel(), new JPanel(), new JPanel()])
        then : 'We get the same UI node back, because Swing-Tree is based on the builder pattern.'
            ui == ui2

        and : 'The wrapped component will have the expected amount of child components.'
            ui.component.components.length == 3
    }

    def 'Swing tree nests all kinds of components (trough builder nodes).'()
    {
        reportInfo """
            Swing tree wraps you components in builder nodes, so you can
            use the same API to add any kind of component to your UI.
            Use the `UI.of(..)` factory method to wrap your components
            or simply use the various kinds of default factory methods like
            `UI.panel()`, `UI.button()`, `UI.label()` etc.
        """
        given : 'A regular swing object.'
            var panel = new JPanel()
        and : 'A simple UI builder instance wrapping the swing object.'
            var ui = UI.of(panel)

        expect : 'The UI node contains the swing object.'
            ui.component === panel
        and : 'The UI node is a JPanel without children.'
            ui.component instanceof JPanel
            panel.components.length == 0

        when : 'We now add something to our UI node...'
            ui.add(UI.label("Hey! I am a wrapped JLabel."))

        then : 'The panel will have received a new component.'
            panel.components.length == 1
        and : 'This component is a label with the expected text.'
            panel.components[0] instanceof JLabel
            panel.components[0].text == "Hey! I am a wrapped JLabel."

        when : 'We add 2 more components...'
            ui.add(UI.button("Button 1"), UI.button("Button 2"))

        then : 'The panel will have 3 components in total.'
            panel.components.length == 3
    }

    def 'We can easily define the cursor on a wrapped UI component'()
    {
        given : 'We create a UI builder node containing a simple button.'
            var ui = UI.button()
        expect : 'At the beginning the default cursor will be set.'
            ui.component.cursor.type == Cursor.DEFAULT_CURSOR

        when : 'We set the cursor of the button to be something else...'
            ui.withCursor(UI.Cursor.RESIZE_SOUTH_EAST)
        then : 'This will lead to the correct cursor being chosen.'
            ui.component.cursor.type == Cursor.SE_RESIZE_CURSOR
    }

    def 'An enum based combo box can have custom cell rendering.'()
    {
        reportInfo """
            Swing tree exposes a builder API for creating combo box cell renderers.
            This is a very powerful feature, because it allows you to customize
            the look and feel of your combo box in a very flexible way.
            So if the state of your combo box is based on a simple enum, whose
            instance names are all written in capital letters, you can use
            define a custom cell renderer to display the enum values in a more
            readable way.
        """
        given : 'We create a simple property to model the selection.'
            var sel = Var.of(Keyboard.Key.A)
        and : 'We creat a combo box with a cell renderer that renders the enum value as a lower case string.'
            var ui =
                    UI.comboBox(sel)
                    .withRenderer(
                        UI.renderComboItem(Keyboard.Key)
                        .asText( cell -> cell.value().map( k -> k.name().toLowerCase() ).orElse("") )
                    )

        expect : 'The combo box will have the correct amount of items.'
            ui.component.itemCount == Keyboard.Key.values().length
        and : 'The combo box will have the correct selected item.'
            ui.component.selectedItem == Keyboard.Key.A
        and : 'The combo box will have a renderer that renders the enum value as a lower case string.'
            ui.component.renderer instanceof DefaultListCellRenderer
            ui.component.renderer.getListCellRendererComponent(null, Keyboard.Key.A, 0, false, false).text == "a"

        when : 'We change the selection...'
            sel.set(Keyboard.Key.B)
        then : 'The combo box will have the correct selected item.'
            ui.component.selectedItem == Keyboard.Key.B
    }

    def 'We can create a border layout based Swing tree.'()
    {
        when :
             var tree =
                     Tree.of( // <- A test utility class used to find all the tree nodes...
                         UI.panel().id("Root")
                         .add(
                             BorderLayout.PAGE_START,
                             UI.button("Button 1 (PAGE_START)").id("B1")
                         )
                         .add(
                             BorderLayout.CENTER,
                             UI.radioButton("Button 2 (CENTER)").id("B2")
                             .peek(button -> button.setPreferredSize(new Dimension(200, 100)) )
                         )
                         .add(
                             BorderLayout.LINE_START,
                             UI.button("Button 3 (LINE_START)").id("B3")
                         )
                         .add(
                             BorderLayout.PAGE_END,
                             UI.button("Long-Named Button 4 (PAGE_END)").id("B4")
                         )
                         .add(
                             BorderLayout.LINE_END,
                             UI.button("5 (LINE_END)").id("B5")
                         )
                         .get(JPanel)
                     )
        and : 'We do this little trick to remove hash code...'
            tree.entrySet().each {
                entry -> tree[entry.key] = entry.value.replaceAll("(\\@([0-9]?[abcdef]?)+)", "")
            }
        
        then :
            'Root' in tree
            'B1' in tree
            'B2' in tree
            'B3' in tree
            'B4' in tree
            'B5' in tree
        and :
            tree['Root'].startsWith('swingtree.UI$Panel')
            tree['B1'  ].startsWith('swingtree.UI$Button')
            tree['B2'  ].startsWith('swingtree.UI$RadioButton')
            tree['B3'  ].startsWith('swingtree.UI$Button')
            tree['B4'  ].startsWith('swingtree.UI$Button')
            tree['B5'  ].startsWith('swingtree.UI$Button')
    }

    def 'We can register various kinds of different keyboard event handlers to swing tree frame builder.'()
    {
        reportInfo """
            The Swing-Tree API exposes various methods to register different kinds of Swing component
            or keyboard events. 
            All such event registration methods can be identified by the 'on' prefix.
        """
        when : 'We create a JFrame UI builder and attach various kinds of actions to it.'
            def frame =
                    UI.frame("Test Frame")
                    .onPressed(Keyboard.Key.H, it -> {/*something*/})
                    .onFocusGain( it -> {/*something*/} )
                    .onFocusLoss( it -> {/*something*/} )
                    .get(JFrame)

        then :
            frame.title == "Test Frame"
            frame.focusable == true
    }

    def 'We can register various kinds of different keyboard event handlers to swing tree nodes.'()
    {
        reportInfo """
            The Swing-Tree API exposes various methods to register different kinds of Swing component
            or keyboard events. 
            All such event registration methods can be identified by the 'on' prefix.
        """

        when : 'We create a panel UI node and attach various kinds of actions to it.'
            def panel =
                    UI.panel().id("Root")
                    .onKeyPressed(it -> {/*something*/})
                    .onPressed(Keyboard.Key.H, it -> {/*something*/})
                    .onKeyReleased(it -> {/*something*/})
                    .onReleased(Keyboard.Key.X, it -> {/*something*/})
                    .onKeyTyped(it -> {/*something*/})
                    .onTyped(Keyboard.Key.X, it -> {/*something*/})
                    .get(JPanel)

        then : 'We can verify that the panel has the expected number of listeners.'
            panel.getListeners(KeyListener.class).size() == 6
    }

    def 'We can register different UI focus event handlers to swing tree nodes.'()
    {
        reportInfo """
            The Swing-Tree API exposes various methods to register different kinds of Swing component
            events, like for example UI focus events. 
            All such event registration methods can be identified by the 'on' prefix.
        """
        when : 'We create a panel UI node and attach various kinds of actions to it.'
            def panel =
                    UI.of(new JPanel()).id("Root")
                    .onFocusGain( it -> {/*something*/} )
                    .onFocusLoss( it -> {/*something*/} )
                    .get(JPanel)
        then : 'We can verify that the panel has the expected number of listeners.'
            panel.getListeners(FocusListener.class).size() == 2
    }

    def 'We can register list selection events on a JList based swing tree node.'()
    {
        reportInfo """
            The Swing-Tree API exposes various methods to register different kinds of Swing component
            events, like for example list selection events. 
            All such event registration methods can be identified by the 'on' prefix.
        """
        when : 'We create a JList UI node and attach various kinds of actions to it.'
            def list =
                    UI.of(new JList<>())
                    .onSelection(it -> {/*something*/})
                    .onSelection(it -> {/*something*/})
                    .onSelection(it -> {/*something*/})
                    .get(JList)
        then : 'We can verify that the list has the expected number of listeners.'
            list.getListeners(ListSelectionListener.class).size() == 3
    }

    def 'Component events can be registered on swing tree nodes.'()
    {
        reportInfo """
            The Swing-Tree API exposes various methods to register different kinds of Swing component
            events, like for example resize events. 
            All such event registration methods can be identified by the 'on' prefix.
        """
        when : 'We create a panel UI node and attach various kinds of actions to it.'
            def panel =
                    UI.of(new JPanel()).id("my-panel")
                    .onResize(it -> {/*something*/})
                    .onMoved(it -> {/*something*/})
                    .onShown(it -> {/*something*/})
                    .onHidden(it -> {/*something*/})
                    .get(JPanel)
        then : 'We can verify that the panel has the expected number of listeners.'
            panel.getListeners(ComponentListener.class).size() == 4
    }

    def 'A tabbed pane can be created and populated in a declarative way.'()
    {
        reportInfo """
            Although Swing has the JTabbedPane component as a collection of tabs, 
            there is no abstraction defining a tab on its own.
            The Swing-Tree API provides a Tab class that allows you to define
            a tab and its content in a more intuitive as well as declarative manner
            with tab specific properties and event handlers.
        """
        when : 'We create a tabbed pane UI node and attach various kinds of tabs with custom actions to it.'
            def tabbedPane =
                UI.tabbedPane(UI.Side.LEFT).id("Tabs")
                .add(
                    UI.tab("Tab 1")
                    .onSelection(it -> {/*something*/})
                    .add(UI.label("Tab 1 content"))
                )
                .add(
                    UI.tab("Tab 2")
                    .withTip("Tab 2 tip")
                    .add(UI.label("Tab 2 content")))
                .add(
                    UI.tab("Tab 3")
                    .onMouseClick(it -> {/*something*/})
                    .add(UI.label("Tab 3 content")))
                .get(JTabbedPane)
        then : 'We can verify that the tabbed pane has the expected number of tabs.'
            tabbedPane.getTabCount() == 3
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 2"
            tabbedPane.getTitleAt(2) == "Tab 3"
        and : 'The tabbed pane has only titles but no header components:'
            tabbedPane.getTabComponentAt(0) == null
            tabbedPane.getTabComponentAt(1) == null
            tabbedPane.getTabComponentAt(2) == null
    }

    def 'The tab buttons of a tabbed pane can have custom components.'()
    {
        reportInfo """
            Although Swing has the JTabbedPane component as a collection of tabs, 
            there is no abstraction defining a tab on its own.
            The Swing-Tree API provides a Tab class that allows you to define
            a tab and its content in a more intuitive as well as declarative manner
            with tab specific properties and event handlers.
        """
        when : 'We create a tabbed pane UI node and attach tabs with custom tab header components to it.'
            def tabbedPane =
                UI.tabbedPane(UI.Side.RIGHT).id("Tabs")
                .add(
                    UI.tab("Tab 1")
                    .withHeader(UI.label("Tab 1 header"))
                    .add(UI.label("Tab 1 content"))
                )
                .add(
                    UI.tab("Tab 2")
                    .withHeader(UI.label("Tab 2 header"))
                    .add(UI.label("Tab 2 content")))
                .add(
                    UI.tab("Tab 3")
                    .withHeader(UI.label("Tab 3 header"))
                    .add(UI.label("Tab 3 content")))
                .get(JTabbedPane)
        then : 'We can verify that the tabbed pane has the expected number of tabs.'
            tabbedPane.getTabCount() == 3
            tabbedPane.getTitleAt(0) == "Tab 1"
            tabbedPane.getTitleAt(1) == "Tab 2"
            tabbedPane.getTitleAt(2) == "Tab 3"
        and : 'The tabbed pane has both title labels and header components:'
            tabbedPane.getTabComponentAt(0) instanceof JPanel // wrapping both the title and the header
            tabbedPane.getTabComponentAt(1) instanceof JPanel
            tabbedPane.getTabComponentAt(2) instanceof JPanel
            ((JPanel)tabbedPane.getTabComponentAt(0)).getComponentCount() == 2
            ((JPanel)tabbedPane.getTabComponentAt(1)).getComponentCount() == 2
            ((JPanel)tabbedPane.getTabComponentAt(2)).getComponentCount() == 2
    }

    def 'Tab header components can be passed to the "tab" factory method instead of the title.'()
    {
        reportInfo """
            Custom tab header components are often used to display icons or a tab close button. 
        """
        when : 'We create a tabbed pane UI node and attach tabs with custom tab header components to it.'
            def tabbedPane =
                UI.tabbedPane(UI.Side.BOTTOM).id("Tabs")
                .add(
                    UI.tab(UI.label("Tab 1 header"))
                    .add(UI.label("Tab 1 content"))
                )
                .add(
                    UI.tab(UI.label("Tab 2 header"))
                    .add(UI.label("Tab 2 content")))
                .add(
                    UI.tab(UI.label("Tab 3 header"))
                    .add(UI.label("Tab 3 content")))
                .get(JTabbedPane)
        then : 'We can verify that the tabbed pane has the expected number of tabs.'
            tabbedPane.getTabCount() == 3
            tabbedPane.getTitleAt(0) == ""
            tabbedPane.getTitleAt(1) == ""
            tabbedPane.getTitleAt(2) == ""
        and : 'The tabbed pane has header components:'
            tabbedPane.getTabComponentAt(0) instanceof JLabel
            tabbedPane.getTabComponentAt(1) instanceof JLabel
            tabbedPane.getTabComponentAt(2) instanceof JLabel
    }

    def 'Use the "peek( c -> {} )" method to access the wrapped Swing component in your Swing-Tree.'()
    {
        reportInfo """
            The Swing-Tree API provides a "peek( c -> {} )" method that allows you to access the wrapped Swing component
            in your Swing-Tree within the provided Peeker lambda. 
            This is useful for when you want more control over the Swing component than what the Swing-Tree API provides.
        """
        when : 'We create a panel UI node and set a custom location.'
            def panel =
                UI.panel()
                .peek(it -> it.setLocation(42, 73))
                .add(UI.label("Label 1"))
                .add(UI.label("Label 2"))
                .add(UI.label("Label 3"))
                .get(JPanel)
        then : 'We can verify that the panel has the expected location.'
            panel.getLocation() == new Point(42, 73)
    }

    /**
     *  This is a simple utility class which converts the swing tree into a hashmap
     *  of Swing component ids and their toString representations.
     */
    static class Tree
    {
        static Map<String, String> of(Component component) {
            def visitor = new Tree()
            visitor.visit(component)
            return visitor.tree
        }

        private Map<String, String> tree = new LinkedHashMap<>()

        private Tree() {}

        void visit(Component cmp) {
            // Add this component
            if ( cmp != null ) tree.put(cmp.getName(), cmp.toString())
            Container container = (Container) cmp
            if( container == null ) return // Not a container, return
            // Go visit and add all children
            for ( Component subComponent : container.getComponents() ) visit(subComponent)
        }
    }

}
