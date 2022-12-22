package swingtree.mvvm

import swingtree.UI
import swingtree.api.mvvm.Val
import swingtree.api.mvvm.Var
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title


@Title("Button Binding")
@Narrative('''
    
    As a developer, I want to bind a button to a property so 
    that is is updated when the property changes.
    Binding is a powerful feature that allows us to create
    UIs which are not only decoupled from the business logic of
    an application, but also make it easy to create UIs which
    are dynamic and reactive.
    
''')
class Button_Binding_Spec extends Specification
{
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

        then : 'The button should be updated when the property is changed and shown.'
            ui.component.text == "Hello World"

        when : 'We change the property value...'
            text.set("Goodbye World")
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The button should be updated.'
            ui.component.text == "Goodbye World"
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

        then : 'The button should be updated when the property is changed and shown.'
            ui.component.selected == false

        when : 'We change the property value...'
            selected.set(true)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The button should be updated.'
            ui.component.selected == true
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

        then : 'The button should be updated when the property is changed and shown.'
            ui.component.enabled == false

        when : 'We change the property value...'
            enabled.set(true)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()
        then : 'The button should be updated.'
            ui.component.enabled == true
    }
    
}
