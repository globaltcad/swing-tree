package swingtree.common

import swingtree.SwingTree
import swingtree.threading.EventProcessor
import swingtree.UI
import utility.Utility
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

import javax.swing.*

@Title("Swing Tree UI Query")
@Narrative('''
    
    The Swing-Tree UI builder allows you to easily build UIs and query them for
    specific components inside component event action lambdas! 
    This allows you to easily build UIs that are event driven and react to
    user input in a very flexible way.
    Note that this should be used with care as it can lead to very complex
    UIs that are hard to maintain.
    Please consider modelling your UIs logic using MVVM based on 
    property bindings (See the properties specification).
    
''')
class UI_Query_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def 'We can query the swing tree within an action lambda.'()
    {
        given : 'we have a fancy UI tree:'
            var root =
            UI.panel("fill, insets 3", "[grow][shrink]")
            .add("grow",
                UI.splitPane(UI.Align.VERTICAL)
                .withDividerAt(50)
                .add(
                    UI.scrollPane()
                    .add(
                        UI.tabbedPane()
                        .add(UI.tab("1").add(UI.panel("fill").add("grow", UI.button("A").id("B1"))))
                        .add(UI.tab("2").add(UI.panel("fill").add("grow", UI.button("B").id("B2"))))
                        .add(UI.tab("3").add(
                            UI.panel("fill").add("grow",
                                UI.button("C").id("B3")
                                .onClick( it -> { // The delegate lets us query the tree!
                                    it.find(JButton, "B2").ifPresent(b -> b.setText("I got hacked by B3"))
                                    it.find(JTextArea, "TA1").ifPresent(a -> a.setText("I got hacked by B3"))
                                    it.find(JTextField, "TF1").ifPresent(a -> a.setText("I got hacked by B3"))
                                })
                            )
                        ))
                    )
                )
                .add(
                    UI.scrollPane()
                    .add(
                        UI.panel("fill")
                        .add("grow,  wrap",
                            UI.scrollPane().add( UI.textArea("I am a text area!").id("TA1") )
                        )
                        .add("shrinky, growx",
                            UI.textField("I am a text field").id("TF1")
                        )
                    )
                )
            )
            .get(JPanel)

        expect : 'We can query the tree for different kinds of components.'
            new Utility.Query(root).find(JButton, "B1").isPresent()
            new Utility.Query(root).find(JButton, "B2").isPresent()
            new Utility.Query(root).find(JButton, "B3").isPresent()
            new Utility.Query(root).find(JTextArea, "TA1").isPresent()
            new Utility.Query(root).find(JTextField, "TF1").isPresent()

        when : 'We click on the button with the actions performing query actions...'
            UI.runNow(()->new Utility.Query(root).find(JButton, "B3").get().doClick())

        then : '...the text fields and text areas are updated.'
            new Utility.Query(root).find(JButton, "B1").get().text == "A"
            new Utility.Query(root).find(JButton, "B2").get().text == "I got hacked by B3"
            new Utility.Query(root).find(JButton, "B3").get().text == "C"
            new Utility.Query(root).find(JTextArea, "TA1").get().text == "I got hacked by B3"
            new Utility.Query(root).find(JTextField, "TF1").get().text == "I got hacked by B3"
    }



}
