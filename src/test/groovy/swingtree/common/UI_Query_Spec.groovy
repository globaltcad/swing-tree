package swingtree.common

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.Event
import swingtree.SwingTree
import swingtree.UI
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.*
import java.awt.*

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

    def 'We can query the swing tree using the String id (name) of components.'()
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

    def 'We cane query a swing three using the group tags of components.'()
    {
        given :
            var traceA = []
            var traceB = []
            var traceC = []
            var traceD = []
		and : 'We create a custom event which we will use th query the tree.'
            var event = Event.create()
        and :
            var ui =
		        UI.panel("fill", "[][grow]")
		        .withBackground(Color.WHITE)
		        .add(UI.label("I am a generic SwingTree UI!"))
		        .add("grow",
		        	UI.panel("fill, insets 0","[grow][shrink]")
		        	.onMouseClick( e -> {/* does something */} )
		        	.add("cell 0 0",
		        		UI.boldLabel("Bold Label")
		        	)
		        	.add("cell 0 1, grow, pushy",
		        		UI.panel("fill, insets 0","[grow][shrink]")
		        		.add("cell 0 0, aligny top, grow x",
		        			UI.panel("fill, insets 9","grow")
		        			.withBackground(new Color(128, 234, 255))
		        			.add( "span",
		        				UI.label("<html><div style=\"width:275px;\"> Hey! :) </div></html>")
		        			)
		        			.add("shrink x",      UI.label("First Name").group("A", "C"))
		        			.add("grow",          UI.textField("John").group("A", "B"))
		        			.add("gap unrelated", UI.label("Last Name").group("B", "D"))
		        			.add("wrap, grow",    UI.textField("Smith").group("C"))
		        			.add("shrink",        UI.label("Address").group("C", "D"))
		        			.add("span, grow",    UI.textField("City").group("A", "C", "B"))
		        			.add( "span, grow",   UI.label("Here is a text field:").group("C", "A"))
		        			.add("span, grow",    UI.textField("Field").group("D", "B"))
		        		)
		        		.add("cell 1 0",
		        			UI.button("Some button!").group("A", "B", "C", "D")
		        			.withCursor(UI.Cursor.HAND)
		        			.makePlain()
		        			.onClick( e -> {/* does something */} )
		        		)
		        		.withBorder(BorderFactory.createMatteBorder(0,0,1,0,Color.LIGHT_GRAY))
		        	)
		        	.add("cell 0 2, grow",
		        		UI.panel("fill, insets 0 0 0 0","[grow][grow][grow]")
		        		.withBackground(Color.WHITE)
		        		.add("cell 1 0", UI.label("Label A").group("C"))
		        		.add("cell 2 0", UI.label("Label B").group("D"))
		        		.add("cell 3 0", UI.label("Label C").group("A"))
		        	)
		        	.add("cell 0 3, span 2, grow",
		        		UI.label("...here the UI comes to an end...").group("A", "B", "C", "D")
                        .withForeground(Color.LIGHT_GRAY)
		        	)
		        	.withBackground(Color.WHITE)
		        )
                .on(event, it -> {
                    traceA.addAll(it.findAllByGroup(JLabel, "A").collect(c -> c.text))
                    traceB.addAll(it.findAllByGroup(JLabel, "B").collect(c -> c.text))
                    traceC.addAll(it.findAllByGroup(JTextField, "C").collect(c -> c.text))
                    traceD.addAll(it.findAllByGroup(JTextField, "D").collect(c -> c.text))
                })

        when : 'We fire the event and query the tree for components with the group tag "A".'
            event.fire()

        then : 'We get the correct components.'
            traceA == ["First Name", "Here is a text field:", "Label C", "...here the UI comes to an end..."]
            traceB == ["Last Name", "...here the UI comes to an end..."]
            traceC == ["Smith", "City"]
            traceD == ["Field"]
    }



}
