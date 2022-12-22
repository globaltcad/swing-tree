package swingtree.mvvm

import swingtree.UI
import swingtree.api.mvvm.Var
import example.LoginViewModel
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
            var vm = new LoginViewModel()
        when : 'We create a view for our view model...'
            var ui =
                UI.panel("fill, wrap 2")
                .add( UI.label( "Username:" ) )
                .add( "grow", UI.textField(vm.username()))
                .add( UI.label( "Password:" ) )
                .add( "grow", UI.passwordField(vm.password()))
                .add( "span",
                    UI.label(vm.feedback())
                )
                .add( "span",
                    UI.button( "Login" )
                    .isEnabledIf(vm.buttonEnabled())
                    .onClick( it -> vm.login() )
                )
        then : 'The view was successfully created.'
            ui != null
    }

    def 'We can bind a boolean property to a button, and when the user presses it, we notice it.'()
    {
        given : 'We instantiate the "view model" in the form of a single property.'
            var pressedStates = []
            Var<Boolean> buttonPressed = Var.of(false).withAction({pressedStates.add(it.current().get()) })
        when : 'We create a view for our view model...'
            var ui = UI.button("Press me!").isPressedIf(buttonPressed)

        then : 'The view was successfully created.'
            ui != null
        when : 'We press the button.'
            ui.component.doClick()
        then : """
                The property was updated 2 times, because the pressed state switches 
                from false to true and then false again.
            """
            pressedStates == [true, false]
    }

    def 'Binding to the selection state of a button does nothing, because a JButton can only be pressed.'()
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
        then : 'The property was not updated because a JButton can only be pressed.'
            actionPerformed == false
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
            Var<Option> selected = Var.of(Option.YES).withAction({actionPerformed = true})
        expect : 'The enum we use for demonstration has the following values.'
            Option.values() == [Option.YES, Option.NO, Option.MAYBE]

        when : 'We create a view for our view model...'
            var ui = UI.comboBox(selected)

        then : 'The view was successfully created.'
            ui != null
        when : 'We select an item in the combo box.'
            ui.component.setSelectedItem(Option.MAYBE)
        then : 'The property was updated.'
            selected.orElseNull() == Option.MAYBE
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
            boolean actionPerformed = false
            Var<String> selected = Var.of("Tofu").withAction({actionPerformed = true})
        when : 'We create a view for our view model...'
            var ui = UI.comboBox("Tofu", "Tempeh", "Seitan").withSelectedItem(selected)

        then : 'The view was successfully created.'
            ui != null && !actionPerformed
        when : 'We select an item in the combo box.'
            ui.component.setSelectedIndex(1)
        then : 'The property was updated.'
            selected.orElseNull() == "Tempeh"
        and : 'The action was triggered.'
            actionPerformed == true
    }

    def 'A simple list of elements can be used as a data model for a combo box.'()
    {
        reportInfo """
            Always prefer Enums over Strings but if you have to model generic data
            you can always bind a simple list of elements as a data model to a combo box.
        """
        given : 'We create a simple list, holding the data.'
            List<String> data = ["Tofu", "Tempeh", "Seitan"]
        when : 'We create a combo box and bind it to the data.'
            var ui = UI.comboBox(data)
        then : 'The combo box was successfully created.'
            ui != null
        and : 'The combo box contains the data.'
            ui.component.itemCount == data.size()
            ui.component.getItemAt(0) == data.get(0)
            ui.component.getItemAt(1) == data.get(1)
            ui.component.getItemAt(2) == data.get(2)
        when : 'We modify the list...'
            data.add("Soy Milk")
        then : 'The combo box is updated.'
            ui.component.itemCount == data.size()
            ui.component.getItemAt(0) == data.get(0)
            ui.component.getItemAt(1) == data.get(1)
            ui.component.getItemAt(2) == data.get(2)
            ui.component.getItemAt(3) == data.get(3)
    }

    def 'A simple array of elements can be used as a data model for a combo box.'()
    {
        reportInfo """
            Always prefer Enums over Strings but if you have to model generic data
            you can always bind a simple array of elements as a data model to a combo box.
        """
        given : 'We create a simple array, holding the data.'
            String[] data = ["Tofu", "Tempeh", "Seitan"]
        when : 'We create a combo box and bind it to the data.'
            var ui = UI.comboBox(data)
        then : 'The combo box was successfully created.'
            ui != null
        and : 'The combo box contains the data.'
            ui.component.itemCount == data.size()
            ui.component.getItemAt(0) == data[0]
            ui.component.getItemAt(1) == data[1]
            ui.component.getItemAt(2) == data[2]
        when : 'We modify the array...'
            data[1] = "Soy Milk"
        then : 'The combo box is updated.'
            ui.component.itemCount == data.size()
            ui.component.getItemAt(0) == data[0]
            ui.component.getItemAt(1) == data[1]
            ui.component.getItemAt(2) == data[2]
    }

    def 'You can bind a property as the current selection as well as list of elements as options to a combo box.'()
    {
        given : 'We create a simple list, holding the data.'
            List<String> data = ["Tofu", "Tempeh", "Seitan"]
        and : 'We create a property holding the current selection.'
            Var<String> selected = Var.of("Tofu")
        when : 'We create a combo box and bind it to the data.'
            var ui = UI.comboBox(selected, data)
        then : 'The combo box was successfully created.'
            ui != null
        and : 'The combo box contains the data.'
            ui.component.itemCount == data.size()
            ui.component.getItemAt(0) == data.get(0)
            ui.component.getItemAt(1) == data.get(1)
            ui.component.getItemAt(2) == data.get(2)
        and : 'The combo box has the correct selection.'
            ui.component.selectedItem == selected.orElseNull()
        when : 'We modify the list...'
            data.add("Soy Milk")
        then : 'The combo box is updated.'
            ui.component.itemCount == data.size()
            ui.component.getItemAt(0) == data.get(0)
            ui.component.getItemAt(1) == data.get(1)
            ui.component.getItemAt(2) == data.get(2)
            ui.component.getItemAt(3) == data.get(3)
        and : 'The combo box has the correct selection.'
            ui.component.selectedItem == selected.orElseNull()
        when : 'We modify the selection...'
            selected.set("Seitan")
        then : 'The combo box has the correct selection.'
            ui.component.selectedItem == selected.orElseNull()
    }

    private static enum Size
    {
        SMALL, MEDIUM, LARGE
    }

    private static enum Option
    {
        YES, NO, MAYBE
    }

}
