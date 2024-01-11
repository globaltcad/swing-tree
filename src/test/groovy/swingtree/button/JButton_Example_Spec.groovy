package swingtree.button

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.SwingTree
import swingtree.UI
import swingtree.UIForButton
import swingtree.threading.EventProcessor

import javax.swing.*

@Title("Using JButton in SwingTree")
@Narrative('''

    This specification demonstrates how the SwingTree API
    can be used to create `JButton`s and how to register event handlers
    on them while still being able to integrate them
    in a declarative UI. 

''')
@Subject([UIForButton])
class JButton_Example_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def 'We can easily create a button with an associated action:'()
    {
        reportInfo """
            This is he most basic example of defining a button.
            It has some text displayed on top of it and a simple click action.
            Take a look:
        """

        given : 'We create a basic button which will a simple "onClick".'
            var ui =
                UI.button("I am displayed on the button!")
                .onClick( it -> it.component.setEnabled(false) )
        and : 'We actually build the component:'
            var button = ui.get(JButton)

        expect : 'The component wrapped by the UI builder is in fact a simple button:'
            button instanceof JButton
        and : 'The button is enabled by default!'
            button.isEnabled()

        when : 'We simulate a user click...'
            UI.runNow( ()-> button.doClick() )
        then : 'The button will be disabled because of the click action we specified!'
            !button.isEnabled()
    }

    def 'A button will delegate its siblings within actions:'()
    {
        reportInfo """
            The event handlers you register on a component receive a useful context 
            which offers all kinds of relevant information to your event action.
            This is also true for a click action handler registered on a 
            button, which can access the neighbouring sibling components. 
        """
        given : 'We create a panel with a label and a button!'
        var ui =
            UI.panel()
                .add(UI.label("Button:"))
                .add(
                    UI.button("Click me!")
                    .onClick( it -> it.siblings.each {s -> s.setEnabled(false)} )
                )
        and : 'We actually build the component:'
            var panel = ui.get(JPanel)

        expect : 'The components wrapped by the UI builder are as expected:'
            panel.getComponent(0) instanceof JLabel
            panel.getComponent(1) instanceof JButton
        and : 'They are enabled by default!'
            panel.getComponent(0).isEnabled()
            panel.getComponent(1).isEnabled()

        when : 'We simulate a user click...'
            UI.runNow( () -> panel.getComponent(1).doClick() )
        then :  'They are enabled by default!'
            !panel.getComponent(0).isEnabled()
            panel.getComponent(1).isEnabled()
    }

    def 'In a button event we can go through the entire siblinghood, including the current button!'()
    {
        reportInfo """
            The event handlers you register on a component receive a useful context 
            which offers all kinds of relevant information to your event action.
            This is also true for a click action handler registered on a 
            button, which can access the "siblinghood", which is all siblings
            including itself. 
        """
        given : 'We create a panel with some random components and a fancy click action'
            var ui =
                UI.panel()
                .add(UI.textField("text field"))
                .add(UI.label("innocent label"))
                .add(
                    UI.button("Click me!")
                    .onClick(it ->
                        it.siblinghood.each {s -> { if ( s instanceof JLabel ) s.setText("I got hacked!") }}
                    )
                )
                .add(UI.spinner())
        and : 'We actually build the component:'
            var panel = ui.get(JPanel)

        when : 'We simulate a user click...'
            UI.runNow( () -> panel.getComponent(2).doClick() )
        then :  'The label text changed!'
            panel.getComponent(1).getText() == "I got hacked!"
    }

}
