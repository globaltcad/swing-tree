package swingtree.mvvm

import examples.mvvm.LoginViewModel
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.Var
import sprouts.Vars
import swingtree.SwingTreeContext
import swingtree.UI
import swingtree.api.mvvm.Viewer
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.*
import javax.swing.border.TitledBorder
import java.awt.*
import java.util.List

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
    def setupSpec() {
        SwingTreeContext.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

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
            Var<Boolean> buttonPressed = Var.of(false).onAct({pressedStates.add(it.get()) })
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
            Var<Boolean> buttonPressed = Var.of(false).onAct({actionPerformed = true})
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
            Var<Size> selected = Var.of(Size.SMALL).onAct({actionPerformed = true})
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
            Var<Option> selected = Var.of(Option.YES).onAct({actionPerformed = true})
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
            Var<String> selected = Var.of("Tofu").onAct({actionPerformed = true})
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

    def 'View Models can be represented by properties.'()
    {
        reportInfo """
            In larger GUIs usually consist views which themselves consist of multiple
            sub views. This is also true for their view models which are usually
            structured in the same tree like fashion. 
            Often times however, your views are highly dynamic and you want to
            be able to swap out sub views at runtime. In this case it is useful
            to represent your view models as properties. 
            Simply implement the 'Viewable' interface in your view model and
            you can bind it to a view.
            When the property changes, the view will be updated automatically.
        """
        given : 'We create a view model.'
            Var<String> name = Var.of("Tofu")
            Var<Integer> population = Var.of(4)
            var vm1 = "Dummy View Model 1"
            var vm2 = "Dummy View Model 2"
            Viewer<String> viewer = viewModel -> {
                switch (viewModel) {
                    case "Dummy View Model 1":
                            return UI.panel().id("sub-1")
                                    .add(UI.label("Name:"))
                                    .add(UI.textField(name))
                                    .add(UI.button("Update").onClick { name.set("Tempeh") })
                                    .component
                    case "Dummy View Model 2":
                            return UI.panel().id("sub-2")
                                    .add(UI.label("Population:"))
                                    .add(UI.slider(UI.Align.HORIZONTAL).withValue(population))
                                    .add(UI.button("Update").onClick { population.set(5) })
                                    .component
                }
            }
        and : 'A property storing the first view model.'
            Var<String> vm = Var.of(vm1)
        and : 'Finally a view which binds to the view model property.'
            var ui = UI.panel()
                    .add(UI.label("Dynamic Super View:"))
                    .add(UI.panel().id("super").add(vm, viewer))
        expect : 'We query the UI for the views and verify that the "super" and "sub-1" views are present.'
            new Utility.Query(ui).find(JPanel, "super").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-1").isPresent()
            !new Utility.Query(ui).find(JPanel, "sub-2").isPresent()
        when : 'We update the view model property.'
            vm.set(vm2)
            UI.sync()
        then : 'The "sub-1" view is removed and the "sub-2" view is added.'
            !new Utility.Query(ui).find(JPanel, "sub-1").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-2").isPresent()
        and : 'The "super" view is still present.'
            new Utility.Query(ui).find(JPanel, "super").isPresent()
    }


    def 'View Models can be represented by properties lists.'() {
        reportInfo """
            In larger GUIs usually consist views which themselves consist of multiple
            sub views. This is also true for their view models which are usually
            structured in the same tree like fashion. 
            Often times however, your views are highly dynamic and you want to
            be able to swap out sub views at runtime. In this case it is useful
            to represent your view models as property lists, especially if 
            one view consists of multiple sub views.
            Simply implement the 'Viewable' interface in your view model and
            you can bind it to a view using the "Vars" class wrapping your viewables.
            When the property list changes, the view will be updated automatically.
        """
        given : 'We create a view model.'
            Var<String> address = Var.of("123 Main Street")
            Var<String> title = Var.of("Mr.")
            Var<Integer> price = Var.of(1000000)
            Var<Option> option = Var.of(Option.YES)

        and : 'We create 4 view models with 4 locally created views:'
            var vm1 = "Dummy View Model 1"
            var vm2 = "Dummy View Model 2"
            var vm3 = "Dummy View Model 3"
            var vm4 = "Dummy View Model 4"
            Viewer<String> viewer = viewModel -> {
                switch ( viewModel ) {
                    case "Dummy View Model 1":
                            return UI.panel().id("sub-1")
                                    .add(UI.label("Address:"))
                                    .add(UI.textField(address))
                                    .add(UI.button("Update").onClick { address.set("456 Main Street") })
                                    .component
                    case "Dummy View Model 2":
                            return UI.panel().id("sub-2")
                                    .add(UI.label("Title:"))
                                    .add(UI.textField(title))
                                    .add(UI.button("Update").onClick { title.set("Mrs.") })
                                    .component
                    case "Dummy View Model 3":
                            return UI.panel().id("sub-3")
                                    .add(UI.label("Price:"))
                                    .add(UI.slider(UI.Align.HORIZONTAL).withValue(price))
                                    .add(UI.button("Update").onClick { price.set(2000000.0) })
                                    .component
                    case "Dummy View Model 4":
                                return UI.panel().id("sub-4")
                                    .add(UI.label("Option:"))
                                    .add(UI.comboBox(option, Option.values()))
                                    .add(UI.button("Update").onClick { option.set(Option.NO) })
                                    .component
                            }
                        }
        and : 'A property list storing the view models.'
            var vms = Vars.of(vm1, vm2, vm3, vm4)
        and : 'Finally a view which binds to the view model property list.'
            var ui = UI.panel()
                    .add(UI.label("Dynamic Super View:"))
                    .add(UI.panel().id("super").add(vms, viewer))
        expect : 'We query the UI for the views and verify that the "super" and "sub-1" views are present.'
            new Utility.Query(ui).find(JPanel, "super").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-1").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-2").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-3").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-4").isPresent()
        when : 'We remove something from the view model property list.'
            vms.remove(vm2)
            UI.sync()
        then : 'We expect all views to be present except for the "sub-2" view.'
            new Utility.Query(ui).find(JPanel, "super").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-1").isPresent()
            !new Utility.Query(ui).find(JPanel, "sub-2").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-3").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-4").isPresent()
        and : 'We remove something else from the view model property list but this time, for a change, use the index.'
            vms.removeAt(2) // vm4
            UI.sync()
        then : 'We expect all views to be present except for the "sub-2" and "sub-4" views.'
            new Utility.Query(ui).find(JPanel, "super").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-1").isPresent()
            !new Utility.Query(ui).find(JPanel, "sub-2").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-3").isPresent()
            !new Utility.Query(ui).find(JPanel, "sub-4").isPresent()
        when : 'We reintroduce "vm2"...'
            vms.add(vm2)
            UI.sync()
        then : 'We expect all views to be present except for the "sub-4" view.'
            new Utility.Query(ui).find(JPanel, "super").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-1").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-2").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-3").isPresent()
            !new Utility.Query(ui).find(JPanel, "sub-4").isPresent()

        when : 'We clear the view model property list.'
            vms.clear()
            UI.sync()
        then : 'We expect all views to be removed. (except for the "super" view)'
            !new Utility.Query(ui).find(JPanel, "sub-1").isPresent()
            !new Utility.Query(ui).find(JPanel, "sub-2").isPresent()
            !new Utility.Query(ui).find(JPanel, "sub-3").isPresent()
            !new Utility.Query(ui).find(JPanel, "sub-4").isPresent()
            new Utility.Query(ui).find(JPanel, "super").isPresent()
    }

    def 'A view model property may or may not exist, meaning its view may or may not be provided.'() {

        reportInfo """
            In larger GUIs usually consist views which themselves consist of multiple
            sub views. This is also true for their view models which are usually
            structured in the same tree like fashion. 
            Often times however, your views are highly dynamic and you not only want to
            be able to swap out but also enable or disable a sub view (and its underlying view model) at runtime. 
            You can for example wrap your view model in a property and if it implements the "Viewable" interface
            the view can bind to it to dynamically regenerate the view. 
            If the property is empty, the view will not be provided.
        """
        given : 'We create a view model.'
            Var<String> username = Var.of("123 Main Street")
            Var<String> password = Var.of("Mr.")
            Var<String> moreUI = Var.ofNullable(String, null)
        and : 'A view which binds to the view model and a viewer which provides the view.'
            var ui = UI.panel("fill, wrap 1")
                    .add("shrink", UI.label("Dynamic Super View:"))
                    .add("grow",
                            UI.panel("fill").id("super")
                            .add(UI.label("Username:"))
                            .add(UI.textField(username))
                            .add(UI.label("Password:"))
                            .add(UI.textField(password))
                            .add(moreUI, subViewModel ->
                                UI.panel().id("sub-1")
                                .add(UI.label("Admin Status Code: xyz"))
                                .add(UI.button("Do admin stuff!"))
                                .component
                            )
                    )
        expect : 'We query the UI for the views and verify that the "super" and "sub-1" views are present.'
            new Utility.Query(ui).find(JPanel, "super").isPresent()
            !new Utility.Query(ui).find(JPanel, "sub-1").isPresent()
        when : 'We set the "moreUI" property to a view model which implements the "Viewable" interface (a "view provider").'
            moreUI.set("I am a dummy view model!")
            UI.sync()
        then : 'We expect all views to be present.'
            new Utility.Query(ui).find(JPanel, "super").isPresent()
            new Utility.Query(ui).find(JPanel, "sub-1").isPresent()
    }

    def 'A boolean property can be used to switch between 2 foreground colors.'()
    {
        reportInfo """
            Using the 'withForegroundIf(Val<Boolean>, Color, Color)' method,
            you can switch between 2 foreground colors of a Swing component set dynamically
            if the boolean property is true or false.
        """
        given : 'We create a property modelling the color switch.'
            Var<Boolean> isRed = Var.of(true)
        and : 'We create a 2 colors.'
            Color red = Color.RED
            Color green = Color.GREEN
        and : 'We create a label with a red foreground color.'
            var ui = UI.label("Hello World!").withForegroundIf(isRed, red, green)
        expect : 'The label should have a red foreground color.'
            ui.component.foreground == red
        when : 'We change the boolean property to false.'
            isRed.set(false)
            UI.sync()
        then : 'The label should have a green foreground color.'
            ui.component.foreground == green
    }

    def 'A boolean property can be used to switch between 2 background colors.'()
    {
        reportInfo """
            Using the 'withBackgroundIf(Val<Boolean>, Color, Color)' method,
            you can switch between 2 background colors of a Swing component set dynamically
            if the boolean property is true or false.
        """
        given : 'We create a property modelling the color switch.'
            Var<Boolean> isRed = Var.of(true)
        and : 'We create a 2 colors.'
            Color red   = Color.RED
            Color green = Color.GREEN
        and : 'We create a label with a red background color.'
            var ui = UI.label("Hello World!").withBackgroundIf(isRed, red, green)
        expect : 'The label should have a red background color.'
            ui.component.background == red
        when : 'We change the boolean property to false.'
            isRed.set(false)
            UI.sync()
        then : 'The label should have a green background color.'
            ui.component.background == green
    }

    def 'A boolean property can be used to set or reset a foreground color.'()
    {
        reportInfo """
            Using the 'withForegroundIf(Val<Boolean>, Color)' method,
            you can set or reset the foreground color of a Swing component dynamically
            if the boolean property is true or false.
        """
        given : 'We create a property modelling the color switch.'
            Var<Boolean> isRed = Var.of(false)
        and : 'We create a label with a yellow background color.'
            var ui = UI.label("Hello World!").withForegroundIf(isRed, Color.YELLOW)
        and : 'We remember the original foreground color.'
            Color originalColor = ui.component.foreground
        expect : 'The label should have the original foreground color.'
            ui.component.foreground == originalColor
        when : 'We change the boolean property to true.'
            isRed.set(true)
            UI.sync()
        then : 'The label should have a yellow foreground color because the boolean property is true.'
            ui.component.foreground == Color.YELLOW
        when : 'We change the boolean property to false.'
            isRed.set(false)
            UI.sync()
        then : 'Again, the label should have the original foreground color.'
            ui.component.foreground == originalColor
    }

    def 'A boolean property can be used to set or reset a background color.'()
    {
        reportInfo """
            Using the 'withBackgroundIf(Val<Boolean>, Color)' method,
            you can set or reset the background color of a Swing component dynamically
            if the boolean property is true or false.
        """
        given : 'We create a property modelling the color switch.'
            Var<Boolean> isRed = Var.of(false)
        and : 'We create a label with a yellow background color.'
            var ui = UI.label("Hello World!").withBackgroundIf(isRed, Color.YELLOW)
        and : 'We remember the original background color.'
            Color originalColor = ui.component.background
        expect : 'The label should have the original background color.'
            ui.component.background == originalColor
        when : 'We change the boolean property to true.'
            isRed.set(true)
            UI.sync()
        then : 'The label should have a yellow background color because the boolean property is true.'
            ui.component.background == Color.YELLOW
        when : 'We change the boolean property to false.'
            isRed.set(false)
            UI.sync()
        then : 'Again, the label should have the original background color.'
            ui.component.background == originalColor
    }

    def 'The foreground color of a Swing component can be modelled using a boolean and a Color property.'()
    {
        reportInfo """
            Using the 'withForegroundIf(Val<Boolean>, Val<Color>)' method,
            the foreground color of a Swing component is set dynamically
            if the boolean property is true.
        """
        given : 'A boolean property.'
            var displayColor = Var.of(false)
        and : 'A color property.'
            var color = Var.of(Color.RED)
        and : 'A Swing UI with a simple label bound to the properties:'
            var ui = UI.panel()
                    .add(
                        UI.label("Hi!").id("XYZ")
                        .withForeground(Color.GREEN) // default color
                        .withForegroundIf(displayColor, color)
                    )
        expect : 'The label should have the default foreground color.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().getForeground() == Color.GREEN
        when : 'We set the boolean property to true.'
            displayColor.set(true)
            UI.sync()
        then : 'The label should have a foreground color.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().getForeground() == Color.RED
        when : 'We set the color property to blue.'
            color.set(Color.BLUE)
            UI.sync()
        then : 'The label should have a foreground color of blue.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().getForeground() == Color.BLUE
        when : 'We set the boolean property to false.'
            displayColor.set(false)
            UI.sync()
        then : 'The label should have the default foreground color, green.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().getForeground() == Color.GREEN
    }

    def 'The background color of a Swing component can be modelled using a boolean and a Color property.'()
    {
        reportInfo """
            Using the 'withBackgroundIf(Val<Boolean>, Val<Color>)' method,
            the background color of a Swing component is set dynamically
            if the boolean property is true.
        """
        given : 'A boolean property.'
            var displayColor = Var.of(false)
        and : 'A color property.'
            var color = Var.of(Color.RED)
        and : 'A Swing UI with a simple label bound to the properties:'
            var ui = UI.panel()
                    .add(
                        UI.label("Hi!").id("XYZ")
                        .withBackground(Color.GREEN) // default color
                        .withBackgroundIf(displayColor, color)
                    )
        expect : 'The label should have the default background color.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().getBackground() == Color.GREEN
        when : 'We set the boolean property to true.'
            displayColor.set(true)
            UI.sync()
        then : 'The label should have a background color.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().getBackground() == Color.RED
        when : 'We set the color property to blue.'
            color.set(Color.BLUE)
            UI.sync()
        then : 'The label should have a background color of blue.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().getBackground() == Color.BLUE
        when : 'We set the boolean property to false.'
            displayColor.set(false)
            UI.sync()
        then : 'The label should have the default background color, green.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().getBackground() == Color.GREEN
    }

    def 'The tooltip of a component can be modelled using a String property.'()
    {
        reportInfo """
            Using the 'withTooltip(Val<String>)' method,
            the tooltip of a Swing component is set dynamically
            if the String property changes.
        """
        given : 'A String property.'
            var tooltip = Var.of("Hello World!")
        and : 'A Swing UI with a simple label bound to the property:'
            var ui = UI.panel()
                    .add(
                        UI.label("Hi!").id("XYZ")
                        .withTooltip(tooltip)
                    )
        expect : 'The label should have the default tooltip.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().getToolTipText() == "Hello World!"
        when : 'We set the String property to "Hello Universe!".'
            tooltip.set("Hello Universe!")
            UI.sync()
        then : 'The label should have a tooltip.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().getToolTipText() == "Hello Universe!"
        when : 'We set the String property to an empty String.'
            tooltip.set("")
            UI.sync()
        then : 'The label should have no tooltip.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().getToolTipText() == null
    }

    def 'A border title can be modelled using properties.'()
    {
        reportInfo """
            Using the 'withBorderTitle(Val<String>)' method,
            the title of a Swing component's border is set dynamically
            if the String property changes.
        """
        given : 'A String property.'
            var title = Var.of("Hello World!")
        and : 'A Swing UI with a simple label bound to the property:'
            var ui = UI.panel().id("My-Panel")
                    .withBorderTitled(title)
                    .add(
                        UI.label("Hi!")
                        //... some other stuff
                    )
        expect : 'The panel should have the expected default border title.'
            ((TitledBorder)new Utility.Query(ui).find(JPanel, "My-Panel").get().border).getTitle() == "Hello World!"
        when : 'We set the String property to "Hello Universe!".'
            title.set("Hello Universe!")
            UI.sync()
        then : 'The panel should have a border title.'
            ((TitledBorder)new Utility.Query(ui).find(JPanel, "My-Panel").get().border).getTitle() == "Hello Universe!"
        when : 'We set the String property to an empty String.'
            title.set("")
            UI.sync()
        then : 'The panel should have no border title.'
            ((TitledBorder)new Utility.Query(ui).find(JPanel, "My-Panel").get().border).getTitle() == ""
    }

    def 'The type of cursor displayed over a component can be modelled using properties.'()
    {
        reportInfo """
            Using the 'withCursor(Val<Cursor>)' method,
            the cursor displayed over a Swing component is set dynamically
            if the Cursor property changes.
        """
        given : 'A Cursor property.'
            var cursor = Var.of(UI.Cursor.DEFAULT)
        and : 'A Swing UI with a simple label bound to the property:'
            var ui = UI.panel()
                    .add(
                        UI.label("Hi!").id("XYZ")
                        .withCursor(cursor)
                    )
        expect : 'The label should have the default cursor.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().cursor.type == UI.Cursor.DEFAULT.type
        when : 'We set the Cursor property to a crosshair cursor.'
            cursor.set(UI.Cursor.CROSS)
            UI.sync()
        then : 'The label should have a crosshair cursor.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().cursor.type == UI.Cursor.CROSS.type
        when : 'We set the Cursor property to a hand cursor.'
            cursor.set(UI.Cursor.HAND)
            UI.sync()
        then : 'The label should have a hand cursor.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().cursor.type == UI.Cursor.HAND.type
        when : 'We set the Cursor property to a text cursor.'
            cursor.set(UI.Cursor.TEXT)
            UI.sync()
        then : 'The label should have a text cursor.'
            new Utility.Query(ui).find(JLabel, "XYZ").get().cursor.type == UI.Cursor.TEXT.type
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
