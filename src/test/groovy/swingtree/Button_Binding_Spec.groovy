package swingtree

import spock.lang.Subject
import swingtree.threading.EventProcessor
import sprouts.Val
import sprouts.Var
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JRadioButton


@Title("Button Binding")
@Narrative('''
    
    As a developer, I want to bind a button to a property so 
    that is is updated when the property changes.
    Binding is a powerful feature that allows us to create
    UIs which are not only decoupled from the business logic of
    an application, but also make it easy to create UIs which
    are dynamic and reactive.
    
    A typical use case for button types is to bind
    their selection state to a property in the view model.
    This property may be boolean based, or something entirely
    different.

''')
@Subject([UIForAnyButton, UIForToggleButton, UIForRadioButton])
class Button_Binding_Spec extends Specification
{
    enum SelectionState { SELECTED, NOT_SELECTED }

    enum Size { SMALL, MEDIUM, LARGE }

    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def 'We can bind to the text of a button.'()
    {
        reportInfo """
            Note that for a binding to work, the property must be a `Var` or `Val`
            implementation. All you have to do to then change the state of the UI component
            is changing the state of the property by calling its "set" method.
            Internally it will then call the "show()" method for you 
            which triggers the observers registered by the UI.
        """

        given : 'We create a simple swing-tree property for modelling the text.'
            Val<String> text = Var.of("Hello World")

        when : 'We create and bind to a button UI node...'
            var ui = UI.button("").withText(text)
        and : 'Build the component:'
            var button = ui.get(JButton)

        then : 'The button should be updated when the property is changed and shown.'
            button.text == "Hello World"

        when : 'We change the property value...'
            text.set("Goodbye World")
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The button should be updated.'
            button.text == "Goodbye World"
    }

    def 'You can bind to the selection state of a button.'()
    {
        reportInfo """
            Note that this works with all kinds of buttons, not just the JButton.
        """
        given : 'We create a simple swing-tree property for modelling the selection state.'
            Val<Boolean> selected = Var.of(false)

        when : 'We create and bind to a button UI node...'
            var ui = UI.button("").isSelectedIf(selected)
        and : 'Build the component:'
            var button = ui.get(JButton)
        then : 'The button should be updated when the property is changed and shown.'
            button.selected == false

        when : 'We change the property value...'
            selected.set(true)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The button should be updated.'
            button.selected == true
    }

    def 'You can bind to the enabled state of a button.'()
    {
        reportInfo """  
            Note that this works with all kinds of buttons, not just the JButton.
        """
        given : 'We create a simple swing-tree property for modelling the selection state.'
            Val<Boolean> enabled = Var.of(false)

        when : 'We create and bind to a button UI node...'
            var ui = UI.button("").isEnabledIf(enabled)
        and : 'Build the component:'
            var button = ui.get(JButton)

        then : 'The button should be updated when the property is changed and shown.'
            button.enabled == false

        when : 'We change the property value...'
            enabled.set(true)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The button should be updated.'
            button.enabled == true
    }

    def 'A button group will not only synchronize the selection state of radio buttons, but also bound properties.'()
    {
        reportInfo """
            Using a button group is a convenient way to synchronize the selection state of radio buttons.
            When one radio button is selected, all other radio buttons in the same group will be deselected.
            This is a very common pattern in UIs, and swing-tree makes it easy to implement
            along with the binding of properties.
            So the selection states of a radio buttons will also
            translate to the selection state of all bound properties.
        """
        given : 'Three simple boolean properties:'
            Val<Boolean> selected1 = Var.of(false)
            Val<Boolean> selected2 = Var.of(false)
            Val<Boolean> selected3 = Var.of(false)
        and : 'A button group for synchronizing the selection state of the radio buttons.'
            ButtonGroup buttonGroup = new ButtonGroup()
        and : 'We create three radio buttons and bind them to the properties.'
            var ui1 = UI.radioButton("1").withButtonGroup(buttonGroup).isSelectedIf(selected1)
            var ui2 = UI.radioButton("2").withButtonGroup(buttonGroup).isSelectedIf(selected2)
            var ui3 = UI.radioButton("3").withButtonGroup(buttonGroup).isSelectedIf(selected3)
        and : 'We create the three buttons:'
            var button1 = ui1.get(JRadioButton)
            var button2 = ui2.get(JRadioButton)
            var button3 = ui3.get(JRadioButton)

        when : 'We select the first radio button...'
            button1.selected = true
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'Both the buttons and the properties have the correct state:'
            button1.selected == true
            button2.selected == false
            button3.selected == false
            selected1.get() == true
            selected2.get() == false
            selected3.get() == false

        when : 'We select the second radio button...'
            button2.selected = true
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'Both the buttons and the properties have the correct state:'
            button1.selected == false
            button2.selected == true
            button3.selected == false
            selected1.get() == false
            selected2.get() == true
            selected3.get() == false

        when : 'We select the third radio button...'
            button3.selected = true
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'Both the buttons and the properties have the correct state:'
            button1.selected == false
            button2.selected == false
            button3.selected == true
            selected1.get() == false
            selected2.get() == false
            selected3.get() == true
    }

    def 'Bind the "isSelected" flag of a button to the equality between an enum and a enum property'()
    {
        reportInfo """
            This feature makes it so that the provided property will cause the button
            to be selected if its value is equal to the enum value passed to the method.
           
            This feature is especially useful for radio buttons.
            Here we are going to use the following enum:
            ```
            enum SelectionState { SELECTED, NOT_SELECTED }
            ```
        """
        given : 'We create a simple swing-tree property for modelling the selection state.'
            Val<SelectionState> selectionState = Var.of(SelectionState.NOT_SELECTED)

        when : 'We create and bind to a button UI node...'
            var ui = UI.button("test").isSelectedIf(SelectionState.SELECTED, selectionState)
        and : 'Build the component:'
            var button = ui.get(JButton)
        then : 'The button should be updated when the property is changed and shown.'
            button.selected == false
        and : 'The button has the expected text.'
            button.text == "test"

        when : 'We change the property value...'
            selectionState.set(SelectionState.SELECTED)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The button should be updated.'
            button.selected == true
    }

    def 'Bind the "isSelected" flag of a button to the inequality between an enum and a enum property'()
    {
        reportInfo """
            This feature makes it so that the provided property will cause the button
            to be selected if its value is NOT equal to the enum value passed to the method.
           
            Here we are going to use the following enum:
            ```
            enum SelectionState { SELECTED, NOT_SELECTED }
            ```
        """
        given : 'We create a simple swing-tree property for modelling the selection state.'
            Val<SelectionState> selectionState = Var.of(SelectionState.NOT_SELECTED)

        when : 'We create and bind to a button UI node...'
            var ui = UI.button("test").isSelectedIfNot(SelectionState.SELECTED, selectionState)
        and : 'Build the component:'
            var button = ui.get(JButton)

        then : 'The button should be updated when the property is changed and shown.'
            button.selected == true
        and : 'The button has the expected text.'
            button.text == "test"

        when : 'We change the property value...'
            selectionState.set(SelectionState.SELECTED)
            UI.sync()
        then : 'The button should be updated.'
            button.selected == false
    }


    def 'Directly bind any property to the selection state of a radio button.'()
    {
        reportInfo """
            A typical use-case is for a radio button
            to be selected in case of a property to
            be equal to a certain value and deselected
            otherwise.
            
            In this example, the property is String based.
            and the radio button is selected in case the
            property is equal to a specific target value.
        """
        given : """
            We first create the variables which are typically 
            used in the view model.
            A property and a target value.
        """
            var food = Var.of("おにぎり")
            var target = "ビーガン"
        and : """
            We then create a radio button and bind the selection
            state of the radio button to the property.
        """
            var radioButton =
                    UI.radioButton("Is Vegan", target, food)
                    .get(JRadioButton.class)
        expect : """
            The radio button is deselected initially
            because the property value is not equal to the target value.
        """
            !radioButton.isSelected()
        and : 'The radio button has the expected text.'
            radioButton.text == "Is Vegan"

        when : 'We then change the property to the target value.'
            food.set(target)
            UI.sync()
        then : 'The radio button is selected as the property value is equal to the target value.'
            radioButton.isSelected()
        when : 'We then change the property to a different value.'
            food.set("カレー")
            UI.sync()
        then : 'The radio button is deselected again.'
            !radioButton.isSelected()
    }

    def 'Directly bind an enum based property to the selection state of a radio button.'()
    {
        reportInfo """
            A typical use-case is for a radio button
            to be selected in case of an enum property to
            be equal to a certain value and deselected
            otherwise.
            
            In this example, the property is based on the following enum:
            ```
            enum Size { SMALL, MEDIUM, LARGE }
            ```
        """
        given : """
            We first create the variables which are typically 
            used in the view model.
            A property and a target value.
        """
            var size = Var.of(Size.SMALL)
            var target = Size.LARGE
        and : """
            We then create a radio button and bind the selection
            state of the radio button to the property.
        """
            var radioButton =
                    UI.radioButton( target, size )
                    .get(JRadioButton.class)
        expect : """
            The radio button is deselected initially
            because the property value is not equal to the target value.
        """
            !radioButton.isSelected()
        and : 'The radio button has the expected text.'
            radioButton.text == "LARGE"

        when : 'We then change the property to the target value.'
            size.set(target)
            UI.sync()
        then : 'The radio button is selected as the property value is equal to the target value.'
            radioButton.isSelected()
        when : 'We then change the property to a different value.'
            size.set(Size.MEDIUM)
            UI.sync()
        then : 'The radio button is deselected again.'
            !radioButton.isSelected()
    }
}
