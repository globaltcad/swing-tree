package com.globaltcad.swingtree.combo

import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.UIForCombo
import com.globaltcad.swingtree.api.mvvm.Var
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

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
            ui.component.editor.item = "XY"
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
}
