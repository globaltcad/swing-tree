package stml

import spock.lang.Ignore
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

import javax.swing.*
import java.awt.*

@Title("The UI builder makes UI building fun again!")
@Narrative('''


''')
class UI_Builder_Spec extends Specification
{

    def 'The UI builder nests components'()
    {
        given : 'A regular swing object.'
            var panel = new JPanel()
        and : 'A simple UI builder instance.'
            var node = UI.of(panel)

        expect :
            node.component === panel
        and :
            panel.components.length == 0

        when : 'We now add something to our UI node...'
            node.add(UI.label("Hey! I am a wrapped JLabel."))

        then : 'The panel will have received a new component.'
            panel.components.length == 1
        and : 'This component is a label with the expected text.'
            panel.components[0] instanceof JLabel
            panel.components[0].text == "Hey! I am a wrapped JLabel."

        when : 'We add 2 more components...'
            node.add(UI.button("Button 1"), UI.button("Button 2"))

        then : 'The panel will have 3 components in total.'
            panel.components.length == 3
    }


    def 'We can use the UIMaker to build a valid Swing GUI tree.'()
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
                                         .make(
                                             button ->
                                                         button.setPreferredSize(new Dimension(200, 100))
                                         )
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
                             .getResulting(JPanel)
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

    /**
     *  Use this to take a look at what the UIMake produces...
     */
    @Ignore
    def 'UI Make makes us a viewable window!'() {

        expect :
            UI.of(new JFrame("Test"))
                .make( frame -> frame.add(
                        UI.of(new JPanel())
                                .add(BorderLayout.PAGE_START, new JButton("Button 1 (PAGE_START)"))
                                .add(
                                        BorderLayout.CENTER,
                                        UI.of(new JRadioButton("Button 2 (CENTER)"))
                                                .make(
                                                        button ->
                                                                button.setPreferredSize(new Dimension(200, 100))
                                                )
                                )
                                .add(BorderLayout.LINE_START, new JButton("Button 3 (LINE_START)"))
                                .add(BorderLayout.PAGE_END, new JButton("Long-Named Button 4 (PAGE_END)"))
                                .add(BorderLayout.LINE_END, new JButton("5 (LINE_END)"))
                                .getResulting(JPanel)
                ))
                .make(
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
