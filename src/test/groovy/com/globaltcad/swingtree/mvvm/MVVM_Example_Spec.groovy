package com.globaltcad.swingtree.mvvm

import com.globaltcad.swingtree.UI
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
        given : 'Instantiate the view model.'
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

}
