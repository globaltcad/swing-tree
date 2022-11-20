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
            var ui =
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
            ui != null
    }

    def 'We can bind a boolean property to a button, and when the user presses it, we notice it.'()
    {
        given : 'We instantiate the "view model" in the form of a single property.'
            boolean actionPerformed = false
            Var<Boolean> buttonPressed = Var.of(false).withAction({actionPerformed = true})
        when : 'We create a view for our view model...'
            var ui = UI.button("Press me!").isSelectedIf(buttonPressed)

        then : 'The view was successfully created.'
            ui != null
        when : 'We press the button.'
            ui.component.doClick()
        then : 'The property was updated.'
            actionPerformed == true
    }

    def 'We can bind a boolean property to a checkbox, and when the user presses it, we notice it.'()
    {
        given : 'We instantiate the "view model" in the form of a single property.'
            Var<Boolean> checkBoxSelected = Var.of(false)
        when : 'We create a view for our view model...'
            var ui = UI.checkBox("Press me!").isSelectedIf(checkBoxSelected)

        then : 'The view was successfully created.'
            ui != null
        when : 'We press the button.'
            ui.component.doClick()
        then : 'The property was updated.'
            checkBoxSelected.orElseNull() == true
    }

    def 'We can bind a boolean property to a radio button, and when the user presses it, we notice it.'()
    {
        given : 'We instantiate the "view model" in the form of a single property.'
            Var<Boolean> radioButton = Var.of(false)
        when : 'We create a view for our view model...'
            var ui = UI.radioButton("Press me!").isSelectedIf(radioButton)

        then : 'The view was successfully created.'
            ui != null
        when : 'We press the radio button.'
            ui.component.doClick()
        then : 'The property was updated.'
            radioButton.orElseNull() == true
    }

    def 'We can bind a boolean property to a toggle button, and when the user presses it, we notice it.'()
    {
        given : 'We instantiate the "view model" in the form of a single property.'
            Var<Boolean> isToggled = Var.of(false)
        when : 'We create a view for our view model...'
            var ui = UI.toggleButton("Toggle me!").isSelectedIf(isToggled)

        then : 'The view was successfully created.'
            ui != null
        when : 'We press the toggle button.'
            ui.component.doClick()
        then : 'The property was updated.'
            isToggled.orElseNull() == true
    }

    def 'We can bind a enum property to a combo box and when the user selects an item, we notice it.'()
    {
        reportInfo """
            You can conveniently bind a combo box to any type of property, 
            but there are 2 types of properties which should be used preferably:
            Enums and Strings. 
            And among these 2 you should always prefer enums over strings, because 
            they have a more manageable room of possible states...
            Only if the possible states are too many
            and not predictable during compile time, should you use strings.
        """
        given : 'We instantiate the "view model" in the form of a single property holding an example enum.'
            boolean actionPerformed = false
            Var<Size> selected = Var.of(Size.SMALL).withAction({actionPerformed = true})
        expect : 'The enum we use for demonstration has the following values.'
            Size.values() == [Size.SMALL, Size.MEDIUM, Size.LARGE]

        when : 'We create a view for our view model...'
            var ui = UI.comboBox(Size.values()).withSelectedItem(selected)

        then : 'The view was successfully created.'
            ui != null
        when : 'We select an item in the combo box.'
            ui.component.setSelectedIndex(1)
        then : 'The property was updated.'
            selected.orElseNull() == Size.MEDIUM
        and : 'The action was triggered.'
            actionPerformed == true
    }

    def 'An enum based combo box can infer its possible states directly from the binding property.'()
    {
        reportInfo """
            You can conveniently bind a combo box to any type of property, 
            but there are 2 types of properties which should be used preferably:
            Enums and Strings. 
            And among these 2 you should always prefer enums over strings, because 
            they have a more manageable room of possible states...
            Only if the possible states are too many
            and not predictable during compile time, should you use strings.
        """
        given : 'We instantiate the "view model" in the form of a single property holding an example enum.'
            boolean actionPerformed = false
            Var<Size> selected = Var.of(Size.SMALL).withAction({actionPerformed = true})
        expect : 'The enum we use for demonstration has the following values.'
            Size.values() == [Size.SMALL, Size.MEDIUM, Size.LARGE]

        when : 'We create a view for our view model...'
            var ui = UI.comboBox(selected)

        then : 'The view was successfully created.'
            ui != null
        when : 'We select an item in the combo box.'
            ui.component.setSelectedIndex(1)
        then : 'The property was updated.'
            selected.orElseNull() == Size.MEDIUM
        and : 'The action was triggered.'
            actionPerformed == true
    }

    def 'A string property can be bound to a combo box holding string elements.'()
    {
        reportInfo """
            You can conveniently bind a combo box to any type of property, 
            but there are 2 types of properties which should be used preferably:
            Enums and Strings. 
            And among these 2 you should always prefer enums over strings, because 
            they have a more manageable room of possible states...
            Only if the possible states are too many
            and not predictable during compile time, should you use strings.
            In the example below you can see how the usage of a string property
            makes sense, because there might be requirements to add new food items
            to the combo box at runtime.
        """
        given : 'We instantiate the "view model" in the form of a single property holding a string.'
            Var<String> selected = Var.of("Tofu")
        when : 'We create a view for our view model...'
            var ui = UI.comboBox("Tofu", "Tempeh", "Seitan").withSelectedItem(selected)

        then : 'The view was successfully created.'
            ui != null
        when : 'We select an item in the combo box.'
            ui.component.setSelectedIndex(1)
        then : 'The property was updated.'
            selected.orElseNull() == "Tempeh"
    }

    private static enum Size
    {
        SMALL, MEDIUM, LARGE
    }

}
