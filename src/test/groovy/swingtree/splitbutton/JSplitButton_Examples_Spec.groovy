package swingtree.splitbutton

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.Event
import sprouts.From
import sprouts.Observer
import sprouts.Var
import swingtree.SwingTree
import swingtree.UI
import swingtree.components.JSplitButton
import swingtree.threading.EventProcessor

import javax.swing.*

@CompileDynamic
@Title("A Button of Buttons")
@Narrative('''

    The Swing-Tree split button component allows 
    you to easily create split buttons in your UIs.
    A split button is a button that has a drop down menu attached to it.
    This allows you to easily add additional functionality to your buttons.
    The split button component is a wrapper around the AlexAndria Software
    JSplitButton component.
    
''')
class JSplitButton_Examples_Spec extends Specification
{
    enum Size {
        S, M, L;
        @Override String toString() {
            switch (this) {
                case S: return "Small"
                case M: return "Medium"
                case L: return "Large"
            }
            return "?"
        }
    }

    enum Colour {
        R, G, B;
        @Override String toString() {
            switch (this) {
                case R: return "Red"
                case G: return "Green"
                case B: return "Blue"
            }
            return "?"
        }
    }

    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def 'The most simple kind of split button can be built like so:'()
    {
        given : 'We create a basic split button which will not have any behaviour.'
            var ui =
                UI.splitButton("I am displayed on the button!")
                .add(UI.splitItem("I am the first drop down item."))
                .add(UI.splitItem("I am the second item."))
                .add(UI.splitItem("And I am the third."))

        expect : 'The component wrapped by the UI builder is in fact a split button:'
            ui.component instanceof JSplitButton
        and : 'This button should have a popup menu with 3 components.'
            ui.popupMenu.components.length == 3
        and : 'They have the expected names:'
            ((JMenuItem)ui.popupMenu.getComponent(0)).getText() == "I am the first drop down item."
            ((JMenuItem)ui.popupMenu.getComponent(1)).getText() == "I am the second item."
            ((JMenuItem)ui.popupMenu.getComponent(2)).getText() == "And I am the third."
    }


    def 'We can easily build a split button whose text becomes the current user selection:'()
    {
        given : 'We create split button displaying the current selection.'
            var ui =
                UI.splitButton("I will be replaced!")
                .onSelection( it -> it.displayCurrentItemText() )
                .add(UI.splitItem("first"))
                .add(UI.splitItem("second"))
                .add(UI.splitItem("third"))
        expect : 'The split button has the correct text displayed'
            ui.component.text == "I will be replaced!"

        when :
            ((JMenuItem)ui.popupMenu.getComponent(1)).doClick()
            UI.sync()
        then :
            ui.component.text == "second"
    }


    def 'We can easily build a split button where only one item text will have its text displayed:'()
    {
        given : 'We create split button displaying only selection "second".'
            var ui =
                UI.splitButton("I may be replaced!")
                .add(UI.splitItem("first"))
                .add(UI.splitItem("second").onSelection( it -> it.displayCurrentItemText() ))
                .add(UI.splitItem("third"))
        expect : 'The split button has the correct text displayed'
            ui.component.text == "I may be replaced!"

        when : 'We simulate a user selecting the first button item'
            UI.runNow(()->((JMenuItem)ui.popupMenu.getComponent(0)).doClick())
        then : 'The displayed button should still be as it was previously.'
            ui.component.text == "I may be replaced!"

        when : 'We now select the third button item'
            ((JMenuItem)ui.popupMenu.getComponent(2)).doClick()
        then : 'Again the button text is still the same.'
            ui.component.text == "I may be replaced!"

        when : 'We now simulate a selection on the second item (for which we registered an action).'
            UI.runNow(()->((JMenuItem)ui.popupMenu.getComponent(1)).doClick())
        then : 'The displayed button text will have changed because of our selection lambda'
            ui.component.text == "second"
    }


    def 'We can register button click events for button items as well as the split button as a whole.'()
    {
        given : 'We create split button with 2 different kinds of events.'
            var ui =
                UI.splitButton("I may be replaced!")
                .onSelection( it -> it.setButtonText("default text")  )
                .add(UI.splitItem("first"))
                .add(UI.splitItem("second").onButtonClick( it -> it.setButtonText("text by second item") ))
                .add(UI.splitItem("third"))
        expect : 'The split button has the correct text displayed'
            ui.component.text == "I may be replaced!"

        when : 'We select the first item and click the button.'
            ((JMenuItem)ui.popupMenu.getComponent(0)).doClick()
            UI.runNow( () -> ui.component.doClick() );
        then : 'We expect that the button has the default text displayed according to the first action'
            ui.component.text == "default text"

        when : 'We select and click the third button.'
            ((JMenuItem)ui.popupMenu.getComponent(2)).doClick()
            UI.runNow( () -> ui.component.doClick() );
        then : 'This should have the same effect as previously.'
            ui.component.text == "default text"

        when : 'We now select and click the second button item.'
            ((JMenuItem)ui.popupMenu.getComponent(1)).doClick()
            UI.runNow( () -> ui.component.doClick() );
        then : 'The split button text will be different because the button item action fired last.'
            ui.component.text == "text by second item"
    }


    def 'We can specify which item should be initially selected.'()
    {
        given : 'We create split button with 3 button click events.'
            var ui =
                UI.splitButton("I may be replaced!")
                .add(UI.splitItem("first").onButtonClick( it -> it.setButtonText("1")) )
                .add(UI.splitItem("second").makeSelected().onButtonClick( it -> it.setButtonText("2") ))
                .add(UI.splitItem("third").onButtonClick( it -> it.setButtonText("3")) )

        when : 'We click the button.'
            UI.runNow( () -> ui.component.doClick() );
        then : 'The button has now "2" displayed on it, because of the second split item action.'
            ui.component.text == "2"
    }


    def 'It is possible to select more than 1 item.'()
    {
        given : 'We create split button with 3 button click events.'
            var ui =
                UI.splitButton("triggered:")
                .add(UI.splitItem("first").onButtonClick( it -> it.appendToButtonText(" 1") ))
                .add(UI.splitItem("second").makeSelected().onButtonClick( it -> it.appendToButtonText(" 2") ))
                .add(UI.splitItem("third").makeSelected().onButtonClick( it -> it.appendToButtonText(" 3") ))

        when : 'We click the button.'
            UI.runNow( () -> ui.component.doClick() );
        then : 'The button text now indicates which items were selected and triggered!'
            ui.component.text == "triggered: 2 3"
    }


    def 'A button item can undo any multi-selection.'()
    {
        given : 'We create split button with 3 button click events and a selection action.'
            var ui =
                UI.splitButton("triggered:")
                .add(UI.splitItem("first").makeSelected().onButtonClick( it -> it.appendToButtonText(" 1") ))
                .add(
                    UI.splitItem("second")
                    .onButtonClick( it -> it.setButtonText(it.buttonText+" 2") )
                    .onSelection( it -> it.selectOnlyCurrentItem() )
                )
                .add(UI.splitItem("third").makeSelected().onButtonClick( it -> it.appendToButtonText(" 3") ))

        when : 'We click the button.'
            UI.runNow( () -> ui.component.doClick() );
        then : 'The button text now indicates which items were selected and triggered!'
            ui.component.text == "triggered: 1 3"

        when : 'We now select the second button item.'
            UI.runNow(()->((JMenuItem)ui.popupMenu.getComponent(1)).doClick())
        then : 'The split button text will not have changed (internally the selection should be different however).'
            ui.component.text == "triggered: 1 3"

        when : 'We now click the second button item.'
            UI.runNow( () -> ui.component.doClick() );
        then : 'The split button text will indicate that now only the second split item button action was triggered!'
            ui.component.text == "triggered: 1 3 2"
    }


    def 'We can build a JSplitButton and add components to it.'()
    {
        reportInfo """
            Note that adding components to a JSplitButton is not the same as 
            in a normal JButton. The components are added to the popup menu
            and not to the button itself.
            This is because the JSplitButton is a composite component and
            the button itself is not a container.
        """

        given : 'We create a UI builder node containing a simple split button.'
            var ui = UI.splitButton("")
        expect : 'It wraps a JSplitButton.'
            ui.component instanceof JSplitButton

        when : 'We add a menu item to the split button.'
            ui.add(new JMenuItem("First"))
        then : 'The split button has a popup menu with one component.'
            ui.popupMenu.components.length == 1

        when : 'We add another menu item to the split button.'
            ui.add(UI.menuItem("Second"))
        then : 'The split button has a popup menu with two components.'
            ui.popupMenu.components.length == 2

        when : 'We add a split item to the split button.'
            ui.add(UI.splitItem("Third"))
        then : 'The split button has a popup menu with three components.'
            ui.popupMenu.components.length == 3

        when : 'We add a split radio button to the split button.'
            ui.add(UI.splitRadioItem("Fourth"))
        then : 'The split button has a popup menu with four components.'
            ui.popupMenu.components.length == 4
        and :
            ui.popupMenu.components.findAll({it instanceof JRadioButtonMenuItem}).size() == 1
            ui.popupMenu.components.findAll({it instanceof JMenuItem}).size() == 4
    }


    def 'A JSplitButton and all of its options can be created from a simple enum property.'()
    {
        reportInfo """
            For this example we use the following enum declaration:
            ```
                enum Size { 
                    S, M, L;
                    @Override String toString() {  
                        switch (this) {
                            case S: return "Small"
                            case M: return "Medium"
                            case L: return "Large"
                        }
                        return "?"
                    }
                }
            ```
        """

        given : 'A Size enum based property.'
            var selection = Var.of(Size.M)

        and : 'We create a UI builder node for the enum states.'
            var ui = UI.splitButton(selection)

        expect : 'It wraps a JSplitButton.'
            ui.component instanceof JSplitButton
        and : 'There are 3 options.'
            ui.popupMenu.components.length == 3
            ui.popupMenu.components[0].text == "Small"
            ui.popupMenu.components[1].text == "Medium"
            ui.popupMenu.components[2].text == "Large"
    }


    def 'A JSplitButton and all of its options can be bound to and created from a simple enum property and bound to an event.'()
    {
        reportInfo """
                For this example we use the following enum declaration:
                ```
                    enum Colour { 
                        RED, GREEN, BLUE;
                        @Override String toString() {  
                            switch (this) {
                                case RED: return "Red"
                                case GREEN: return "Green"
                                case BLUE: return "Blue"
                            }
                            return "?"
                        }
                    }
                ```
            """
        given : 'A Colour enum based property and an event.'
            var tracker = []
            var selection = Var.of(Colour.B).onChange(From.VIEW, {tracker << "#"})
            var event = Event.of((Observer){tracker << "!"})
        and : 'We create a UI builder node for the enum states.'
            var ui = UI.splitButton(selection, event)
        expect : 'It wraps a JSplitButton.'
            ui.component instanceof JSplitButton
        and : 'There are 3 options.'
            ui.popupMenu.components.length == 3
            ui.popupMenu.components[0].text == "Red"
            ui.popupMenu.components[1].text == "Green"
            ui.popupMenu.components[2].text == "Blue"
        and : 'The button text is initially set to the current selection.'
            ui.component.text == "Blue"
        when : 'We select the second option.'
            UI.runNow(()->((JMenuItem)ui.popupMenu.getComponent(1)).doClick())
        then : 'The tracker will have recorded a selection action event.'
            tracker == ["#"]
        and : 'The button text will have changed to the new selection.'
            ui.component.text == "Green"
        when : 'We click the button.'
            UI.runNow( () -> ui.component.doClick() );
        then : 'The tracker will have recorded a button click event.'
            tracker == ["#", "!"]
    }
}