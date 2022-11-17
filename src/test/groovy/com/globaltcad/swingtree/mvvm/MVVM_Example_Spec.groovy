package com.globaltcad.swingtree.mvvm

import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.api.mvvm.Var
import example.FormViewModel
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

@Title("MVVM Introduction")
@Narrative('''

    Swing-Tree allows you to create a Model-View-ViewModel (MVVM) architecture
    based on 2 simple property interfaces: `Val`, and `Var`!
    
    `Val` is a read-only property, and `Var` is a read-write property.

    The state of both properties can be observed by the view using simple listeners.
    This happens automatically when you pass them to the Swing-Tree view.
    If you want to trigger an action when the property changes, you 
    have to pass the `Var` property to the view and define a action
    for it inside of your view model.

''')
class MVVM_Example_Spec extends Specification
{

    def 'We can create a property based view model and build a view for it.'()
    {
        reportInfo """
            Note that we use a pre-made example view model here.
            Feel free to look at the source code of the view model
            to see what it is doing.
        """
        given : 'We instantiate the view model.'
            var vm = new FormViewModel()
        when : 'We create a view for our view model...'
            var node =
                UI.panel("fill, wrap 2")
                .add( UI.label( "Username:" ) )
                .add( "grow", UI.textField(vm.username()))
                .add( UI.label( "Password:" ) )
                .add( "grow", UI.passwordField(vm.password()))
                .add( "span",
                    UI.label(vm.validity())
                )
                .add( "span",
                    UI.button( "Login" )
                    .isEnabledIf(vm.loginEnabled())
                    .onClick( it -> vm.login() )
                )
        then : 'The view was successfully created.'
            node != null
    }

    def 'We can bind a boolean property to a button, and when the user presses it, we notice it.'()
    {
        given : 'We instantiate the "view model" in the form of a single property.'
            boolean actionPerformed = false
            Var<Boolean> buttonPressed = Var.of(false).withAction({actionPerformed = true})
        when : 'We create a view for our view model...'
            var node =
                UI.button("Press me!").isSelectedIf(buttonPressed)

        then : 'The view was successfully created.'
            node != null
        when : 'We press the button.'
            node.component.doClick()
        then : 'The property was updated.'
            actionPerformed == true
    }

    def 'We can bind a boolean property to a checkbox, and when the user presses it, we notice it.'()
    {
        given : 'We instantiate the "view model" in the form of a single property.'
            Var<Boolean> checkBoxSelected = Var.of(false)
        when : 'We create a view for our view model...'
            var node =
                UI.checkBox("Press me!").isSelectedIf(checkBoxSelected)

        then : 'The view was successfully created.'
            node != null
        when : 'We press the button.'
            node.component.doClick()
        then : 'The property was updated.'
            checkBoxSelected.get() == true
    }

    def 'We can bind a boolean property to a radio button, and when the user presses it, we notice it.'()
    {
        given : 'We instantiate the "view model" in the form of a single property.'
            Var<Boolean> radioButton = Var.of(false)
        when : 'We create a view for our view model...'
            var node =
                UI.radioButton("Press me!").isSelectedIf(radioButton)

        then : 'The view was successfully created.'
            node != null
        when : 'We press the radio button.'
            node.component.doClick()
        then : 'The property was updated.'
            radioButton.get() == true
    }

    def 'We can bind a boolean property to a toggle button, and when the user presses it, we notice it.'()
    {
        given : 'We instantiate the "view model" in the form of a single property.'
            Var<Boolean> isToggled = Var.of(false)
        when : 'We create a view for our view model...'
            var node =
                UI.toggleButton("Toggle me!").isSelectedIf(isToggled)

        then : 'The view was successfully created.'
            node != null
        when : 'We press the toggle button.'
            node.component.doClick()
        then : 'The property was updated.'
            isToggled.get() == true
    }

}
