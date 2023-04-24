package swingtree.common

import com.alexandriasoftware.swing.JSplitButton
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.UI
import swingtree.Utility

import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JSlider
import javax.swing.JTextArea
import javax.swing.JToggleButton

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
        and : 'We wait for the UI thread to do its thing.'
            UI.sync()
        expect :
            new Utility.Query(ui.component).find(JTextArea,     "C1").isPresent()
            new Utility.Query(ui.component).find(JCheckBox,     "C2").isPresent()
            new Utility.Query(ui.component).find(JButton,       "C3").isPresent()
            new Utility.Query(ui.component).find(JToggleButton, "C4").isPresent()
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
        given : 'We create a UI with a button that grabs the focus and some other components.'
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
        and : 'We wait for the UI thread to do its thing.'
            UI.sync()
        expect :
            new Utility.Query(ui.component).find(JTextArea,     "C1").isPresent()
            new Utility.Query(ui.component).find(JCheckBox,     "C2").isPresent()
            new Utility.Query(ui.component).find(JButton,       "C3").isPresent()
            new Utility.Query(ui.component).find(JToggleButton, "C4").isPresent()
        and :
            ui.component.rootPane.defaultButton == new Utility.Query(ui.component).find(JButton, "C3").get()
    }

    def 'The visibility of a component can be configured using various methods.'()
    {
        given : 'We create a UI with a button that grabs the focus and some other components.'
            var ui =
                UI.panel()
                .add(
                    UI.slider(UI.Align.HORIZONTAL).id("C1").isVisibleIf(true),
                    UI.editorPane().id("C2").isVisibleIf(false),
                    UI.splitButton("Button").id("C3").isVisibleIfNot(true),
                    UI.toggleButton("Toggle").id("C4").isVisibleIfNot(false)
                )
        expect : 'The components are visible or not depending on the configuration.'
            new Utility.Query(ui.component).find(JSlider, "C1").get().isVisible() == true
            new Utility.Query(ui.component).find(JEditorPane, "C2").get().isVisible() == false
            new Utility.Query(ui.component).find(JSplitButton, "C3").get().isVisible() == false
            new Utility.Query(ui.component).find(JToggleButton, "C4").get().isVisible() == true
    }
}

