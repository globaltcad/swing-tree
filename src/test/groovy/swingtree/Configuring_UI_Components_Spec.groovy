package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.threading.EventProcessor
import swingtree.components.JSplitButton
import utility.Utility

import javax.swing.*

@Title("Configuring UI withMethod Chaining")
@Narrative('''

    The declarative nature of Swing-Tree is enabled by 1 fundamental
    design patterns:
    
    > Composition and Method Chaining!
    
    So every method on a Swing-Tree UI builder node returns the node
    instance itself. 
    This is especially useful when you want to configure a UI component.
    
    In this specification we will see what this looks like.

''')
class Configuring_UI_Components_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def 'We can make a UI component grab the current input focus.'()
    {
        given : 'We create a UI with a button that grabs the focus and some other components.'
            var ui =
                UI.frame()
                .add(
                    UI.textArea("Text :)").id("C1"),
                    UI.checkBox("Check").id("C2"),
                    UI.button("Button").id("C3").makeFocused(),
                    UI.toggleButton("Toggle").id("C4")
                )
        and : 'We build the component tree.'
            var frame = ui.get(JFrame)
        and : 'We wait for the UI thread to do its thing.'
            UI.sync()
        expect :
            new Utility.Query(frame).find(JTextArea,     "C1").isPresent()
            new Utility.Query(frame).find(JCheckBox,     "C2").isPresent()
            new Utility.Query(frame).find(JButton,       "C3").isPresent()
            new Utility.Query(frame).find(JToggleButton, "C4").isPresent()
    }

    def 'Use the "makeDefaultButton()" method to make a button the default button.'()
    {
        reportInfo """
            The so called "default button" is the button that is activated when
            the user presses the "Enter" key.
            In Swing this is a bit tricky to configure. You have to set the
            "default button" property on the root pane of the UI.
            Swing-Tree does this for you automatically when you call the
            "makeDefaultButton()" method on a button.
        """
        given : 'We create a UI with a button that is made the default button.'
            var ui =
                UI.runAndGet( () -> // For technical reasons we execute this in the GUI thread.
                    UI.frame()
                    .add(
                        UI.textArea("Text :)").id("C1"),
                        UI.checkBox("Check").id("C2"),
                        UI.button("Button").id("C3").makeDefaultButton(),
                        UI.toggleButton("Toggle").id("C4")
                    )
                )
        and : 'We build the component tree.'
            var frame = ui.get(JFrame)
        and : 'We wait for the UI thread to do its thing.'
            UI.sync()
            Thread.sleep(1_000)
            UI.sync()
        expect :
            new Utility.Query(frame).find(JTextArea,     "C1").isPresent()
            new Utility.Query(frame).find(JCheckBox,     "C2").isPresent()
            new Utility.Query(frame).find(JButton,       "C3").isPresent()
            new Utility.Query(frame).find(JToggleButton, "C4").isPresent()
        and :
            frame.rootPane.defaultButton == new Utility.Query(frame).find(JButton, "C3").get()
    }

    def 'The visibility of a component can be configured using various methods.'()
    {
        given : 'We create a UI with a button which may or may not be visible.'
            var ui =
                UI.panel()
                .add(
                    UI.slider(UI.Align.HORIZONTAL).id("C1").isVisibleIf(true),
                    UI.editorPane().id("C2").isVisibleIf(false),
                    UI.splitButton("Button").id("C3").isVisibleIfNot(true),
                    UI.toggleButton("Toggle").id("C4").isVisibleIfNot(false)
                )
        and : 'We build the panel based component tree.'
            var panel = ui.get(JPanel)
        expect : 'The components are visible or not depending on the configuration.'
            new Utility.Query(panel).find(JSlider, "C1").get().isVisible() == true
            new Utility.Query(panel).find(JEditorPane, "C2").get().isVisible() == false
            new Utility.Query(panel).find(JSplitButton, "C3").get().isVisible() == false
            new Utility.Query(panel).find(JToggleButton, "C4").get().isVisible() == true
    }

    def 'We can configure a button to have no border.'()
    {
        given : 'We create a UI with buttons that may or may not have borders.'
            var ui =
                UI.panel()
                .add(
                    UI.button("Button").id("C1").isBorderPaintedIf(false),
                    UI.toggleButton("Toggle").id("C2").isBorderPaintedIf(true),
                    UI.splitButton("Split").id("C3").isBorderPaintedIf(false),
                )
        and : 'We build the panel based component tree.'
            var panel = ui.get(JPanel)
        expect : 'The components have borders or not depending on the configuration.'
            new Utility.Query(panel).find(JButton, "C1").get().isBorderPainted() == false
            new Utility.Query(panel).find(JToggleButton, "C2").get().isBorderPainted() == true
            new Utility.Query(panel).find(JSplitButton, "C3").get().isBorderPainted() == false
    }
}

