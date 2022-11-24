package com.globaltcad.swingtree.combo

import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.UIForCombo
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
}
