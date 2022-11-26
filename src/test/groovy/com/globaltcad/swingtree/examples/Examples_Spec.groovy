package com.globaltcad.swingtree.examples

import com.alexandriasoftware.swing.JSplitButton
import com.globaltcad.swingtree.examples.advanced.AdvancedUI
import com.globaltcad.swingtree.examples.simple.Calculator
import com.globaltcad.swingtree.examples.simple.Form
import com.globaltcad.swingtree.examples.simple.ListRendering
import com.globaltcad.swingtree.examples.simple.TableUI
import com.globaltcad.swingtree.examples.slim.TodoApp
import com.globaltcad.swingtree.utility.Utility
import example.LoginView
import example.LoginViewModel
import example.SomeSpinnersView
import example.SomeSpinnersViewModel
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

import javax.swing.*
import java.awt.*

@Title("Execution and Validation of Example Code")
@Narrative('''

    This specification ensures that the
    various UI examples in the test suite, 
    run successfully and also produce
    UIs with expected states.

''')
class Examples_Spec extends Specification
{

    def 'The advanced UI define in the examples has the expected state.'()
    {
        given : 'We get the UI.'
            var ui = AdvancedUI.of(null)
        expect :
            new Utility.Query(ui).find(JLabel, "APIC-label").isPresent()
            new Utility.Query(ui).find(JLabel, "APIC-label").get().text == "6.0"
        and :
            new Utility.Query(ui).find(JTabbedPane, "APIC-Tabs").isPresent()
            new Utility.Query(ui).find(JTabbedPane, "APIC-Tabs").get().getTabCount() == 9
        and :
            new Utility.Query(ui).find(JSpinner, "Light-Spinner").isPresent()
            new Utility.Query(ui).find(JSpinner, "Light-Spinner").get().getValue() == 0
        and :
            new Utility.Query(ui).find(JSplitButton, "con-split-button").isPresent()
            Utility.getSplitButtonPopup(new Utility.Query(ui).find(JSplitButton, "con-split-button").get()).components.length == 4
    }

    def 'The form UI define in the examples has the expected state.'()
    {
        given : 'We get the UI.'
            var ui = new Form()
        expect :
            new Utility.Query(ui).find(JButton, "hover-icon-button").isPresent()
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().text == ""
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().icon != null
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().pressedIcon != null
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().rolloverIcon != null
        and :
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().cursor.type == Cursor.HAND_CURSOR
    }

    def 'The login example UI define in the examples can be created.'()
    {
        expect : new LoginView(new LoginViewModel())
    }

    def 'The spinners example UI defined in the examples can be created.'()
    {
        expect : new SomeSpinnersView(new SomeSpinnersViewModel())
    }

    def 'The list rendering example UI defined in the examples can be created.'()
    {
        expect : new ListRendering()
    }

    def 'The calculator UI defined in the examples has the expected state.'()
    {
        given : 'We get the UI.'
            var ui = new Calculator()
        expect :
            new Utility.Query(ui).find(JTextArea, "input-text-area").isPresent()
            new Utility.Query(ui).find(JTextArea, "input-text-area").get().componentOrientation == ComponentOrientation.RIGHT_TO_LEFT
    }

    def 'The simple Table-UI example has the expected state.'()
    {
        given : 'We get the UI.'
            var ui = TableUI.create()
        expect : 'The UI contains 2 different JTables.'
            new Utility.Query(ui).find(JTable, "RM").isPresent()
            new Utility.Query(ui).find(JTable, "CM").isPresent()
        and :
            new Utility.Query(ui).find(JTable, "RM").get().getRowCount() == 2
            new Utility.Query(ui).find(JTable, "RM").get().getColumnCount() == 3
            new Utility.Query(ui).find(JTable, "RM").get().getValueAt(0, 0) == "A"
            new Utility.Query(ui).find(JTable, "RM").get().getValueAt(0, 1) == "B"
            new Utility.Query(ui).find(JTable, "RM").get().getValueAt(0, 2) == "C"
            new Utility.Query(ui).find(JTable, "RM").get().getValueAt(1, 0) == "a"
            new Utility.Query(ui).find(JTable, "RM").get().getValueAt(1, 1) == "b"
            new Utility.Query(ui).find(JTable, "RM").get().getValueAt(1, 2) == "c"
        and :
            new Utility.Query(ui).find(JTable, "CM").get().getRowCount() == 3
            new Utility.Query(ui).find(JTable, "CM").get().getColumnCount() == 2
            new Utility.Query(ui).find(JTable, "CM").get().getValueAt(0, 0) == "A"
            new Utility.Query(ui).find(JTable, "CM").get().getValueAt(1, 0) == "B"
            new Utility.Query(ui).find(JTable, "CM").get().getValueAt(2, 0) == "C"
            new Utility.Query(ui).find(JTable, "CM").get().getValueAt(0, 1) == "a"
            new Utility.Query(ui).find(JTable, "CM").get().getValueAt(1, 1) == "b"
            new Utility.Query(ui).find(JTable, "CM").get().getValueAt(2, 1) == "c"
        and :
            new Utility.Query(ui).find(JTable, "RM2").isPresent()
            new Utility.Query(ui).find(JTable, "CM2").isPresent()
        and :
            new Utility.Query(ui).find(JTable, "RM2").get().getRowCount() == 3
            new Utility.Query(ui).find(JTable, "RM2").get().getColumnCount() == 2
            new Utility.Query(ui).find(JTable, "RM2").get().getValueAt(0, 0) == "X"
            new Utility.Query(ui).find(JTable, "RM2").get().getValueAt(1, 0) == "1"
            new Utility.Query(ui).find(JTable, "RM2").get().getValueAt(2, 0) == "3"
            new Utility.Query(ui).find(JTable, "RM2").get().getValueAt(0, 1) == "Y"
            new Utility.Query(ui).find(JTable, "RM2").get().getValueAt(1, 1) == "2"
            new Utility.Query(ui).find(JTable, "RM2").get().getValueAt(2, 1) == "4"
        and :
            new Utility.Query(ui).find(JTable, "CM2").get().getRowCount() == 2
            new Utility.Query(ui).find(JTable, "CM2").get().getColumnCount() == 3
            new Utility.Query(ui).find(JTable, "CM2").get().getValueAt(0,0) == "X"
            new Utility.Query(ui).find(JTable, "CM2").get().getValueAt(0,1) == "1"
            new Utility.Query(ui).find(JTable, "CM2").get().getValueAt(0,2) == "3"
            new Utility.Query(ui).find(JTable, "CM2").get().getValueAt(1,0) == "Y"
            new Utility.Query(ui).find(JTable, "CM2").get().getValueAt(1,1) == "2"
            new Utility.Query(ui).find(JTable, "CM2").get().getValueAt(1,2) == "4"
    }


    def 'The todo app UI defined in the examples has the expected state.'()
    {
        given : 'We get the UI.'
            var ui = new TodoApp()
        expect :
            new Utility.Query(ui).find(JPanel, "task-1").isPresent()
            new Utility.Query(ui).find(JPanel, "task-2").isPresent()
            new Utility.Query(ui).find(JPanel, "task-3").isPresent()
    }


}
