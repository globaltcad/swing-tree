package swingtree.combo

import swingtree.UI
import swingtree.UIForCombo
import sprouts.Var
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Vars

import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox

@Title("Drop Downs, aka Combo Boxes")
@Narrative("""

    In Swing drop downs are called combo boxes, which
    is what they will be referred to in this specification as well.  
    This specification will show you how you can create them and
    how you can interact with them.

""")
@Subject([UIForCombo])
class Combo_Box_Specification extends Specification
{
    def 'Swing tree is a wrapper around Swing, which means you can create the combo box yourself.'()
    {
        given : 'We pass the combo box to the Swing-Tree factory method.'
            var ui = UI.of(new JComboBox<>(new DefaultComboBoxModel<>(new String[]{"A", "B", "C"})))
        expect : 'The underlying component is a combo box.'
            ui.component instanceof JComboBox
        and : 'It has the expected state:'
            ui.component.itemCount == 3
            ui.component.getItemAt(0) == "A"
            ui.component.getItemAt(1) == "B"
            ui.component.getItemAt(2) == "C"
            ui.component.model instanceof DefaultComboBoxModel
            ui.component.model.size == 3
            ui.component.model.getElementAt(0) == "A"
            ui.component.model.getElementAt(1) == "B"
            ui.component.model.getElementAt(2) == "C"
    }

    def 'The "comboBox" factory method allows you to easily create a combo box from an array.'()
    {
        given : 'We pass the combo box to the Swing-Tree factory method.'
            var ui = UI.comboBox("A", "B", "C")
        expect : 'The underlying component is a combo box.'
            ui.component instanceof JComboBox
        and : 'It has the expected state:'
            ui.component.itemCount == 3
            ui.component.getItemAt(0) == "A"
            ui.component.getItemAt(1) == "B"
            ui.component.getItemAt(2) == "C"
        and : 'It is backed by a built in model.'
            !(ui.component.model instanceof DefaultComboBoxModel)
            ui.component.model.size == 3
            ui.component.model.getElementAt(0) == "A"
            ui.component.model.getElementAt(1) == "B"
            ui.component.model.getElementAt(2) == "C"
    }

    def 'A combo box created by Swing-Tree uses the provided item array as data model.'()
    {
        reportInfo """
           You don't have to create a ComboBoxModel implementation to model the state
           of your combo box! If you pass an array or a list to a combo box
           factory method it will serve as a model of your combo box items.  
        """
        given : 'We create an array and pass it to the combo box factory method.'
            var items = new String[]{"A" , "B" , "C"}
            var ui = UI.comboBox(items)
        when : 'We modify the array...'
            items[0] = "D"
        then : 'The combo box is updated as well.'
            ui.component.itemCount == 3
            ui.component.getItemAt(0) == "D"
            ui.component.getItemAt(1) == "B"
            ui.component.getItemAt(2) == "C"
    }

    def 'A combo box created by Swing-Tree uses the provided item list as data model.'()
    {
        reportInfo """
           You don't have to create a ComboBoxModel implementation to model the state
           of your combo box! If you pass an array or a list to a combo box
           factory method it will serve as a model of your combo box items.  
        """
        given : 'We create an array and pass it to the combo box factory method.'
            var items = ["A" , "B" , "C"]
            var ui = UI.comboBox(items)
        when : 'We modify the list by adding an element in the middle...'
            items.add(1, "D")
        then : 'The combo box is updated as well.'
            ui.component.itemCount == 4
            ui.component.getItemAt(0) == "A"
            ui.component.getItemAt(1) == "D"
            ui.component.getItemAt(2) == "B"
            ui.component.getItemAt(3) == "C"
    }

    def 'A combo box can be made editable in a declarative way.'()
    {
        given : 'We create a combo box and make it editable.'
            var ui = UI.comboBox("A", "B", "C")
                            .isEditableIf(true)
                            .withSelectedItem("B")
        expect : 'The combo box is editable.'
            ui.component.isEditable()
        and : 'The selected item is set.'
            ui.component.getSelectedItem() == "B"

        when : 'We simulate the user typing "XY" into the combo box.'
            UI.runNow( () -> ui.component.editor.item = "XY" )
            UI.sync()
        then : 'The combo box is updated.'
            ui.component.getSelectedItem() == "XY"
        and : 'This change is reflected in the model.'
            ui.component.model.size == 3
            ui.component.model.getElementAt(0) == "A"
            ui.component.model.getElementAt(1) == "XY"
            ui.component.model.getElementAt(2) == "C"
    }

    def 'The options of an editable combo box are only editable if their items list is modifyable.'()
    {
        given : 'We create a combo box and make it editable.'
            var ui = UI.comboBox(Collections.unmodifiableList(["A", "B", "C"]))
                            .isEditableIf(true)
                            .withSelectedItem("B")
        expect : 'The combo box is editable.'
            ui.component.isEditable()
        and : 'The selected item is set.'
            ui.component.getSelectedItem() == "B"

        when : 'We simulate the user typing "XY" into the combo box.'
            ui.component.editor.item = "XY"
        then : 'The combo box is updated.'
            ui.component.getSelectedItem() == "XY"
        and : 'This change is NOT reflected in the model.'
            ui.component.model.size == 3
            ui.component.model.getElementAt(0) == "A"
            ui.component.model.getElementAt(1) == "B"
            ui.component.model.getElementAt(2) == "C"
    }

    def 'You can model both the current selection state as well as options of your combo box using a property and an array.'()
    {
        reportInfo """
           In essence, the state of a combo box consists of the current selection, and
           the options that are available for selection. You can model both of these
           aspects using a property and a list. The property will be used to model
           the current selection, and the list will be used to model the options.
        """
        given : 'We create our "model", a property and an array.'
            var selection = Var.of(42)
            var options = new Integer[]{ 73 , 42 , 17 }
        and : 'We create a combo box that is bound to the property and the list.'
            var ui = UI.comboBox(selection, options)
        expect : 'The combo box is initialized with the current selection.'
            ui.component.getSelectedItem() == 42
        and : 'It also reports the correct selection index.'
            ui.component.getSelectedIndex() == 1
        and : 'The there are all 3 options available.'
            ui.component.itemCount == 3
            ui.component.getItemAt(0) == 73
            ui.component.getItemAt(1) == 42
            ui.component.getItemAt(2) == 17

        when : 'We change the selection.'
            selection.set(17)
        then : 'This change translates from the property to the UI element.'
            ui.component.getSelectedItem() == 17
        and : 'The combo box options are still the same.'
            ui.component.itemCount == 3
            ui.component.getItemAt(0) == 73
            ui.component.getItemAt(1) == 42
            ui.component.getItemAt(2) == 17

        when : 'We change one of the options.'
            options[0] = 99
        and : 'We select this option...'
            ui.component.setSelectedItem(99)
        then : 'The selection is updated.'
            selection.get() == 99
        and : 'The combo box also reports the correct selection index!'
            ui.component.getSelectedIndex() == 0
    }


    def 'You can model both the current selection state as well as options of your combo box using a property and a list.'()
    {
        reportInfo """
           In essence, the state of a combo box consists of the current selection, and
           the options that are available for selection. You can model both of these
           aspects using a property and a list. The property will be used to model
           the current selection, and the list will be used to model the options.
        """
        given : 'We create our "model", a property and a list.'
            var selection = Var.of(42)
            var options = [73, 42, 17]
        and : 'We create a combo box that is bound to the property and the list.'
            var ui = UI.comboBox(selection, options)
        expect : 'The combo box is initialized with the current selection.'
            ui.component.getSelectedItem() == 42
        and : 'It also reports the correct selection index.'
            ui.component.getSelectedIndex() == 1
        and : 'The there are all 3 options available.'
            ui.component.itemCount == 3
            ui.component.getItemAt(0) == 73
            ui.component.getItemAt(1) == 42
            ui.component.getItemAt(2) == 17

        when : 'We change the selection.'
            selection.set(17)
        then : 'This change translates from the property to the UI element.'
            ui.component.getSelectedItem() == 17
        and : 'The combo box options are still the same.'
            ui.component.itemCount == 3
            ui.component.getItemAt(0) == 73
            ui.component.getItemAt(1) == 42
            ui.component.getItemAt(2) == 17

        when : 'We add another option somewhere in the middle.'
            options.add(1, 99)
        then : 'The combo box options are updated.'
            ui.component.itemCount == 4
            ui.component.getItemAt(0) == 73
            ui.component.getItemAt(1) == 99
            ui.component.getItemAt(2) == 42
            ui.component.getItemAt(3) == 17
        and : 'The selection is still the same.'
            ui.component.getSelectedItem() == 17
        and : 'The combo box also reports the correct selection index!'
            ui.component.getSelectedIndex() == 3
    }


    def 'You can model both the current selection state as well as options of your combo box using 2 properties.'()
    {
        reportInfo """
           In essence, the state of a combo box consists of the current selection, and
           the options that are available for selection. You can model both of these
           aspects using 2 properties. One modelling the current selection, and another one
           storing an array to model all available options. 
        """
        given : 'We create our "model", 2 properties.'
            var selection = Var.of(42)
            var options = Var.of([73, 42, 17] as Integer[])
        and : 'We create a combo box that is bound to the property and the list.'
            var ui = UI.comboBox(selection, options)
        expect : 'The combo box is initialized with the current selection.'
            ui.component.getSelectedItem() == 42
        and : 'It also reports the correct selection index.'
            ui.component.getSelectedIndex() == 1
        and : 'The there are all 3 options available.'
            ui.component.itemCount == 3
            ui.component.getItemAt(0) == 73
            ui.component.getItemAt(1) == 42
            ui.component.getItemAt(2) == 17

        when : 'We change the selection.'
            selection.set(17)
        then : 'This change translates from the property to the UI element.'
            ui.component.getSelectedItem() == 17

        when : 'We change the options property.'
            options.set([99, 17] as Integer[])
        then : 'The combo box options are updated.'
            ui.component.itemCount == 2
            ui.component.getItemAt(0) == 99
            ui.component.getItemAt(1) == 17
    }


    def 'You can model the options of your combo boxes using "Vars".'()
    {
        reportInfo """
           In essence, the state of a combo box consists of the current selection, and
           the options that are available for selection. You can model both of these
           aspects using a standalone property as well as a "Vars" instance
           representing multiple properties. 
           The single property models the current selection, and the "Vars"
           store a list of all available options. 
        """
        given : 'We create our "model", a property and a "Vars" instance.'
            var selection = Var.of(42)
            var options = Vars.of(73, 42, 17)
        and : 'We create a combo box that is bound to the property and the list.'
            var ui = UI.comboBox(selection, options)
        expect : 'The combo box is initialized with the current selection.'
            ui.component.getSelectedItem() == 42
        and : 'It also reports the correct selection index.'
            ui.component.getSelectedIndex() == 1
        and : 'The there are all 3 options available.'
            ui.component.itemCount == 3
            ui.component.getItemAt(0) == 73
            ui.component.getItemAt(1) == 42
            ui.component.getItemAt(2) == 17

        when : 'We change the selection.'
            selection.set(17)
        then : 'This change translates from the property to the UI element.'
            ui.component.getSelectedItem() == 17

        when : 'We change the options property.'
            options.clear().addAll(99, 17)
        then : 'The combo box options are updated.'
            ui.component.itemCount == 2
            ui.component.getItemAt(0) == 99
            ui.component.getItemAt(1) == 17

        when : 'We add another option somewhere in the middle.'
            options.addAll(16, 42)
        then : 'The combo box options are updated.'
            ui.component.itemCount == 4
            ui.component.getItemAt(0) == 99
            ui.component.getItemAt(1) == 17
            ui.component.getItemAt(2) == 16
            ui.component.getItemAt(3) == 42
    }

    def 'An editable combo box will try to parse user input to match bound properties.'()
    {
        reportInfo """
            When you bind a selection property to an editable combo box, the combo box
            will try to parse user input so that it can be converted to the type of the
            selection property. 
            If the String can be parsed, the combo box will update the selection property
            to the parsed value.
            On the other hand, if the String cannot be parsed, the combo box will not
            update the selection property.
        """
        given : 'We create a property of type Integer.'
            var selection = Var.of(42)
        and : 'We create a combo box that is bound to the property.'
            var ui =
                        UI.comboBox(selection, 73, 42, 17)
                        .isEditableIf(true)
        expect : 'The combo box is initialized with the current selection.'
            ui.component.getSelectedItem() == 42
        and : 'It also reports the correct selection index.'
            ui.component.getSelectedIndex() == 1

        when : 'We simulate the user editing the combo box.'
            ui.component.setSelectedItem('99')
        then : 'The combo box updates the selection property to the parsed value.'
            selection.get() == 99
    }


    def 'An editable combo box will try to parse any kind of user input to match bound properties.'(
        String input, Var<Object> selection, Object[] items, Object expectedSelection
    ) {
        reportInfo """
            When you bind a selection property to an editable combo box, the combo box
            will try to parse user input so that it can be converted to the type of the
            selection property. 
            If the String can be parsed, the combo box will update the selection property
            to the parsed value.
            On the other hand, if the String cannot be parsed, the combo box will not
            update the selection property.
        """
        given : 'We create a combo box that is bound to the property.'
            var ui =
                        UI.comboBox(selection, items)
                        .isEditableIf(true)
        expect : 'The combo box is initialized with the current selection.'
            ui.component.getSelectedItem() == selection.get()

        when : 'We simulate the user editing the combo box.'
            ui.component.setSelectedItem(input)
            UI.sync()
        then : 'The combo box updates the selection property to the parsed value.'
            selection.get() == expectedSelection

        where : 'We use the following data table to populate the used variables:'
            input   |   selection      |  items           | expectedSelection
            '99'    |   Var.of(42)     |  [73, 42, 17]    | 99
            '42'    |   Var.of(42)     |  [73, 42, 17]    | 42
            'true'  |   Var.of(false)  |  [true, false]   | true
            'false' |   Var.of(false)  |  [true, false]   | false
            'yes'   |   Var.of(false)  |  [true, false]   | true
            'no'    |   Var.of(false)  |  [true, false]   | false
            '3.14'  |   Var.of(3.14)   |  [3.14, 2.71]    | 3.14
            '2.71'  |   Var.of(3.14)   |  [3.14, 2.71]    | 2.71
            'foo'   |   Var.of('foo')  |  ['foo', 'bar']  | 'foo'
            'bar'   |   Var.of('foo')  |  ['foo', 'bar']  | 'bar'
            '666L'  |   Var.of(666L)   |  [666L, 777L]    | 666L
            '777L'  |   Var.of(666L)   |  [666L, 777L]    | 777L
    }
}
