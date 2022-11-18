package com.globaltcad.swingtree.common

import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.input.Keyboard
import spock.lang.Ignore
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

import javax.swing.*
import javax.swing.event.ListSelectionListener
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyListener

@Title("Swing tree makes UI building fun again!")
@Narrative('''

    The Swing-Tree library allows you to build UIs declaratively, which you can think of
    as a more dynamic type of HTML but for swing.
    It is inspired by frameworks like Jetpack Compose, Flutter, React and SwiftUI
    and allows for very readable declarative builder pattern based UI design.
    In this specification we cover the utter most basic properties of swing tree.

''')
class Basic_UI_Builder_Examples_Spec extends Specification
{
    def 'We can add a list of components to the swing tree API and get a builder node in return.'()
    {
        given : 'We have a simple JPanel UI node.'
            var ui = UI.panel()

        expect : 'At the beginning the wrapped component will have no children.'
            ui.component.components.length == 0

        when : 'We add a list of panels...'
            ui.add([new JPanel(), new JPanel(), new JPanel()])

        then : 'The wrapped component will have the expected amount of child components.'
            ui.component.components.length == 3
    }


    def 'Swing tree nests components (trough builder nodes).'()
    {
        given : 'A regular swing object.'
            var panel = new JPanel()
        and : 'A simple UI builder instance.'
            var ui = UI.of(panel)

        expect :
            ui.component === panel
        and :
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
            ui.with(UI.Cursor.RESIZE_SOUTH_EAST)
        then : 'This will lead to the correct cursor being chosen.'
            ui.component.cursor.type == Cursor.SE_RESIZE_CURSOR
    }

    def 'We can use the swing tree to build a valid Swing GUI tree.'()
    {
        when :
             def tree =
                     Tree.of(
                         UI.of(new JPanel()).id("Root")
                         .add(
                             BorderLayout.PAGE_START,
                             UI.of(new JButton("Button 1 (PAGE_START)")).id("B1")
                         )
                         .add(
                             BorderLayout.CENTER,
                             UI.of(new JRadioButton("Button 2 (CENTER)")).id("B2")
                             .peek(button -> button.setPreferredSize(new Dimension(200, 100)) )
                         )
                         .add(
                             BorderLayout.LINE_START,
                             UI.of(new JButton("Button 3 (LINE_START)")).id("B3")
                         )
                         .add(
                             BorderLayout.PAGE_END,
                             UI.of(new JButton("Long-Named Button 4 (PAGE_END)")).id("B4")
                         )
                         .add(
                             BorderLayout.LINE_END,
                             UI.of(new JButton("5 (LINE_END)")).id("B5")
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
            tree['Root'] == 'javax.swing.JPanel[Root,0,0,0x0,invalid,layout=java.awt.BorderLayout,alignmentX=0.0,alignmentY=0.0,border=,flags=9,maximumSize=,minimumSize=,preferredSize=]'
            tree['B1'  ] == 'javax.swing.JButton[B1,0,0,0x0,invalid,alignmentX=0.0,alignmentY=0.5,border=javax.swing.plaf.BorderUIResource$CompoundBorderUIResource,flags=296,maximumSize=,minimumSize=,preferredSize=,defaultIcon=,disabledIcon=,disabledSelectedIcon=,margin=javax.swing.plaf.InsetsUIResource[top=2,left=14,bottom=2,right=14],paintBorder=true,paintFocus=true,pressedIcon=,rolloverEnabled=true,rolloverIcon=,rolloverSelectedIcon=,selectedIcon=,text=Button 1 (PAGE_START),defaultCapable=true]'
            tree['B2'  ] == 'javax.swing.JRadioButton[B2,0,0,0x0,invalid,alignmentX=0.0,alignmentY=0.5,border=javax.swing.plaf.BorderUIResource$CompoundBorderUIResource,flags=360,maximumSize=,minimumSize=,preferredSize=java.awt.Dimension[width=200,height=100],defaultIcon=,disabledIcon=,disabledSelectedIcon=,margin=javax.swing.plaf.InsetsUIResource[top=2,left=2,bottom=2,right=2],paintBorder=false,paintFocus=true,pressedIcon=,rolloverEnabled=true,rolloverIcon=,rolloverSelectedIcon=,selectedIcon=,text=Button 2 (CENTER)]'
            tree['B3'  ] == 'javax.swing.JButton[B3,0,0,0x0,invalid,alignmentX=0.0,alignmentY=0.5,border=javax.swing.plaf.BorderUIResource$CompoundBorderUIResource,flags=296,maximumSize=,minimumSize=,preferredSize=,defaultIcon=,disabledIcon=,disabledSelectedIcon=,margin=javax.swing.plaf.InsetsUIResource[top=2,left=14,bottom=2,right=14],paintBorder=true,paintFocus=true,pressedIcon=,rolloverEnabled=true,rolloverIcon=,rolloverSelectedIcon=,selectedIcon=,text=Button 3 (LINE_START),defaultCapable=true]'
            tree['B4'  ] == 'javax.swing.JButton[B4,0,0,0x0,invalid,alignmentX=0.0,alignmentY=0.5,border=javax.swing.plaf.BorderUIResource$CompoundBorderUIResource,flags=296,maximumSize=,minimumSize=,preferredSize=,defaultIcon=,disabledIcon=,disabledSelectedIcon=,margin=javax.swing.plaf.InsetsUIResource[top=2,left=14,bottom=2,right=14],paintBorder=true,paintFocus=true,pressedIcon=,rolloverEnabled=true,rolloverIcon=,rolloverSelectedIcon=,selectedIcon=,text=Long-Named Button 4 (PAGE_END),defaultCapable=true]'
            tree['B5'  ] == 'javax.swing.JButton[B5,0,0,0x0,invalid,alignmentX=0.0,alignmentY=0.5,border=javax.swing.plaf.BorderUIResource$CompoundBorderUIResource,flags=296,maximumSize=,minimumSize=,preferredSize=,defaultIcon=,disabledIcon=,disabledSelectedIcon=,margin=javax.swing.plaf.InsetsUIResource[top=2,left=14,bottom=2,right=14],paintBorder=true,paintFocus=true,pressedIcon=,rolloverEnabled=true,rolloverIcon=,rolloverSelectedIcon=,selectedIcon=,text=5 (LINE_END),defaultCapable=true]'
    }

    def 'We can register various keyboard events in swing tree nodes.'()
    {
        when :
            def panel =
                    UI.of(new JPanel()).id("Root")
                    .onKeyPressed(it -> {/*something*/})
                    .onPressed(Keyboard.Key.H, it -> {/*something*/})
                    .onKeyReleased(it -> {/*something*/})
                    .onReleased(Keyboard.Key.X, it -> {/*something*/})
                    .onKeyTyped(it -> {/*something*/})
                    .onTyped(Keyboard.Key.X, it -> {/*something*/})
                    .get(JPanel)

        then :
            panel.getListeners(KeyListener.class).size() == 6
    }

    def 'We can register various UI focus events in swing tree nodes.'()
    {
        when :
            def panel =
                    UI.of(new JPanel()).id("Root")
                    .onFocusGained(it -> {/*something*/})
                    .onFocusLost(it -> {/*something*/})
                    .get(JPanel)
        then :
            panel.getListeners(FocusListener.class).size() == 2
    }

    def 'We can register list selection events on a JList based swing tree node.'()
    {
        when :
            def list =
                    UI.of(new JList<>())
                    .onSelection(it -> {/*something*/})
                    .onSelection(it -> {/*something*/})
                    .onSelection(it -> {/*something*/})
                    .get(JList)
        then :
            list.getListeners(ListSelectionListener.class).size() == 3
    }

    def 'A tabbed pane can be created and populated in a declarative way.'()
    {
        when :
            def tabbedPane =
                UI.tabbedPane(UI.Position.LEFT).id("Tabs")
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
        then :
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
        when :
            def tabbedPane =
                UI.tabbedPane(UI.Position.RIGHT).id("Tabs")
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
        then :
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
        when :
            def tabbedPane =
                UI.tabbedPane(UI.Position.BOTTOM).id("Tabs")
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
        then :
            tabbedPane.getTabCount() == 3
            tabbedPane.getTitleAt(0) == ""
            tabbedPane.getTitleAt(1) == ""
            tabbedPane.getTitleAt(2) == ""
        and : 'The tabbed pane has header components:'
            tabbedPane.getTabComponentAt(0) instanceof JLabel
            tabbedPane.getTabComponentAt(1) instanceof JLabel
            tabbedPane.getTabComponentAt(2) instanceof JLabel
    }

    /**
     *  Use this to take a look at what the UIMake produces...
     */
    @Ignore
    def 'Swing tree makes us a viewable window!'()
    {
        expect :
            UI.of(new JFrame("Test"))
            .peek(frame -> frame.add(
                UI.of(new JPanel())
                .add(BorderLayout.PAGE_START, new JButton("Button 1 (PAGE_START)"))
                .add(
                    BorderLayout.CENTER,
                    UI.of(new JRadioButton("Button 2 (CENTER)"))
                    .peek(
                        button ->
                                button.setPreferredSize(new Dimension(200, 100))
                    )
                )
                .add(BorderLayout.LINE_START, new JButton("Button 3 (LINE_START)"))
                .add(BorderLayout.PAGE_END, new JButton("Long-Named Button 4 (PAGE_END)"))
                .add(BorderLayout.LINE_END, new JButton("5 (LINE_END)"))
                .get(JPanel)
            ))
            .peek(
                frame -> {
                    frame.setSize(300, 300)
                    frame.show()
                }
            )
            != null
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
