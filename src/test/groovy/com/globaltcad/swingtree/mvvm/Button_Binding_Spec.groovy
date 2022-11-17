package com.globaltcad.swingtree.mvvm

import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.api.mvvm.Val
import com.globaltcad.swingtree.api.mvvm.Var
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
            Note that for a binding to work, the property must be a Var or Val
            implementation and it is required to call the "show()" method on the
            property to apply its value to the view.
        """

        given : 'We create a simple swing-tree property for modelling the text.'
            Val<String> text = Var.of("Hello World")

        when : 'We create and bind to a button UI node...'
            var node = UI.button("").withText(text)

        then : 'The button should be updated when the property changes.'
            node.component.text == "Hello World"

        when : 'We change the property value...'
            text.set("Goodbye World")
        then : 'The button should NOT be updated.'
            node.component.text == "Hello World"

        when : 'We call the "view" method on the property...'
            text.show()
        then : 'The button should be updated.'
            node.component.text == "Goodbye World"
    }

    def 'You can bind to the selection state of a button.'()
    {
        reportInfo """
            Note that this works with all kinds of buttons, not just the JButton.
        """
        given : 'We create a simple swing-tree property for modelling the selection state.'
            Val<Boolean> selected = Var.of(false)

        when : 'We create and bind to a button UI node...'
            var node = UI.button("").isSelectedIf(selected)

        then : 'The button should be updated when the property changes.'
            node.component.selected == false

        when : 'We change the property value...'
            selected.set(true)
        then : 'The button should NOT be updated.'
            node.component.selected == false

        when : 'We call the "view" method on the property...'
            selected.show()
        then : 'The button should be updated.'
            node.component.selected == true
    }

    def 'You can bind to the enabled state of a button.'()
    {
        reportInfo """  
            Note that this works with all kinds of buttons, not just the JButton.
        """
        given : 'We create a simple swing-tree property for modelling the selection state.'
            Val<Boolean> enabled = Var.of(false)

        when : 'We create and bind to a button UI node...'
            var node = UI.button("").isEnabledIf(enabled)

        then : 'The button should be updated when the property changes.'
            node.component.enabled == false

        when : 'We change the property value...'
            enabled.set(true)
        then : 'The button should NOT be updated.'
            node.component.enabled == false

        when : 'We call the "view" method on the property...'
            enabled.show()
        then : 'The button should be updated.'
            node.component.enabled == true
    }
    
}
