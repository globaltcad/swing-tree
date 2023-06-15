package swingtree.examples

import com.alexandriasoftware.swing.JSplitButton
import example.*
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.UI
import swingtree.Utility
import swingtree.examples.advanced.AdvancedUI
import swingtree.examples.comparison1.MadeWithSwingTree
import swingtree.examples.simple.Calculator
import swingtree.examples.simple.ListRendering
import swingtree.examples.simple.TableUI
import swingtree.examples.slim.TodoApp

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
            var ui = new swingtree.examples.simple.Form()
        expect :
            new Utility.Query(ui).find(JButton, "hover-icon-button").isPresent()
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().text == ""
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().icon != null
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().pressedIcon != null
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().rolloverIcon != null
        and :
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().cursor.type == Cursor.HAND_CURSOR
    }

    def 'The login example UI defined in the examples can be created.'()
    {
        expect : new LoginView(new LoginViewModel())
    }

    def 'The user registration example UI defined in the examples can be created.'()
    {
        expect : new UserRegistrationView(new UserRegistrationViewModel())
    }

    def 'The list examples UI defined in the examples can be created.'()
    {
        expect : new ListSearchView(new ListSearchViewModel())
    }

    def 'The box shadow picker example UI defined in the examples can be created.'()
    {
        expect : new BoxShadowPickerView(new BoxShadowPickerViewModel())
    }

    def 'The note guesser example UI defined in the examples can be created.'()
    {
        reportInfo """
            The guesser example view looks like this:
            ![note-guesser-UI.png](../src/test/resources/snapshots/note-guesser-UI.png)
            This is actually a nice little demo application you can play with
            yourself. Just navigate to the class and run it.
        """
        given : 'We create an instance of the UI.'
            var ui = new NoteGuesserView(new NoteGuesserViewModel())
        expect : 'It is rendered as shown in the image.'
            Utility.similarityBetween(ui, "/snapshots/note-guesser-UI.png", 99.9) > 99.9
    }

    def 'The symbol guesser example UI defined in the examples can be created.'()
    {
        expect : new SymbolGuesserView(new SymbolGuesserViewModel())
    }

    def 'The "Well rounded" style example UI defined in the examples can be created.'()
    {
        reportInfo """
            The well rounded UI looks like this:
            ![well-rounded-UI.png](../src/test/resources/snapshots/well-rounded-UI.png)
            
            Here we will only test the UI, if you want to see the code, just
            navigate to the class and run it yourself, it has 
            a main method for that purpose already.
        """
        given : 'We create the UI.'
            var ui = new WellRoundedView()
        expect : 'It is rendered as shown in the image.'
            Utility.similarityBetween(ui, "/snapshots/well-rounded-UI.png", 99.9) > 99.9
    }

    def 'The soft example UI is rendered as expected.'()
    {
        reportInfo """
            The soft example UI looks like this:
            ![soft-example-UI.png](../src/test/resources/snapshots/soft-example-UI.png)
           
            Here we will only test the soft UI, if you want to see the code, just
            navigate to the class and run it yourself, it has 
            a main method for that purpose already.
        """
        given : 'We create the UI.'
            var ui = new SoftUIView()
        expect : 'It is rendered as shown in the image.'
            Utility.similarityBetween(ui, "/snapshots/soft-example-UI.png") > 99.9
    }

    def 'The animated buttons view examples UI defined in the examples looks as expected.'()
    {
        reportInfo """
            The animated buttons view looks like this:
            ![animated-buttons-UI.png](../src/test/resources/snapshots/animated-buttons-UI.png)
            
            Unfortunately this is just a snapshot of the UI, so you can't play with 
            the animations right here. But you can run the example found in the test suite
            of SwingTree and try it out yourself.
            There is a main method in the class, so you can just run it.
        """
        given : 'We create the UI.'
            var ui = new AnimatedButtonsView()
        expect : 'It is rendered as shown in the image.'
            Utility.similarityBetween(ui, "/snapshots/animated-buttons-UI.png", 99.9) > 99.9
    }

    def 'The animation example view can be created.'()
    {
        expect : new AnimatedView(Mock(JFrame))
    }

    def 'The settings example UI defined in the examples looks as expected.'()
    {
        reportInfo """
            The vertical settings view in the examples looks like this:
            ![vertical-settings-UI.png](../src/test/resources/snapshots/vertical-settings-UI.png)
            
            It demonstrates how UI components can easily be placed vertically
            and with some slight indentation, to indicate a certain grouping
            which is especially useful for settings dialogs.
            
            Here we will only test the UI, if you want to see the code, just
            navigate to the class and run it yourself, it has 
            a main method for that purpose already.
        """
        given :
            var view = new SomeSettingsView(new SomeSettingsViewModel())
            var speedTextField = new Utility.Query(view).find(JTextField, "speed-text-field").orElse(null)
        expect :
            view != null
            speedTextField != null
        and :
            speedTextField.text == "42.0"
            speedTextField.background == Color.WHITE
        when : 'We rerender the view offscreen...'
            var similarity = Utility.similarityBetween(view, "/snapshots/vertical-settings-UI.png", 99.9)
        then : '...it looks as expected.'
            similarity > 99.9

        when : 'We simulate the user entering an invalid number:'
            speedTextField.text = "§"
            UI.sync()
        then : 'The UI is updated to reflect the invalid value.'
            speedTextField.text == "§"
            speedTextField.background == Color.RED

        when : 'We rerender the view offscreen again because the background color changed...'
            similarity = Utility.similarityBetween(view, "/snapshots/vertical-settings-UI.png")
        then : 'The UI is no longer rendered as expected.'
            similarity < 99.9
    }

    def 'The spinners example UI defined in the examples can be created.'()
    {
        expect : new SomeComponentsView(new SomeComponentsViewModel())
    }

    def 'The list rendering example UI defined in the examples can be created.'()
    {
        expect : new ListRendering()
    }

    def 'The "MadeWithSwingTree" example UI defined in the examples can be created.'()
    {
        expect : new MadeWithSwingTree()
    }

    def 'The calculator UI defined in the examples has the expected state and looks.'()
    {
        reportInfo """
            The calculator view looks like this:
            ![calculator-UI.png](../src/test/resources/snapshots/calculator-UI.png)
            
            Here we will only test the UI, if you want to see the code, just
            navigate to the class and run it yourself, it has 
            a main method for that purpose already.
        """
        given : 'We create the UI.'
            var ui = new Calculator()
        expect :
            new Utility.Query(ui).find(JTextArea, "input-text-area").isPresent()
            new Utility.Query(ui).find(JTextArea, "input-text-area").get().componentOrientation == ComponentOrientation.RIGHT_TO_LEFT
        and : 'Its render state is as expected.'
            Utility.similarityBetween(ui, "/snapshots/calculator-UI.png") > 99.9
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
