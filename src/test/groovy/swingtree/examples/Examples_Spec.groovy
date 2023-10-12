package swingtree.examples

import examples.Calculator
import examples.animated.AnimatedButtonsView
import examples.animated.AnimatedView
import examples.comparisons.comparison1.MadeWithSwingTree
import examples.games.NoteGuesserView
import examples.games.NoteGuesserViewModel
import examples.games.SymbolGuesserView
import examples.games.SymbolGuesserViewModel
import examples.mvvm.*
import examples.simple.Form
import examples.simple.ListRendering
import examples.simple.TodoApp
import examples.stylish.BoxShadowPickerView
import examples.stylish.BoxShadowPickerViewModel
import examples.stylish.SoftUIView
import examples.stylish.WellRoundedView
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.SwingTree
import swingtree.UI
import swingtree.components.JSplitButton
import swingtree.examples.advanced.AdvancedUI
import swingtree.examples.simple.TableUI
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.*
import java.awt.*

@Title("Examples UIs")
@Narrative('''

    This is an overview of the various example UIs
    in the test suite.
   
    This specification also ensures that the
    various UI examples in the test suite, 
    run successfully and also produce
    UIs with expected state and appearance.

''')
class Examples_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def setup() {
        // We reset to the default look and feel:
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
        // This is to make sure that the tests are not influenced by
        // other look and feels that might be used in the example code...
    }

    def cleanup() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }


    def 'The advanced UI define in the examples has the expected state.'()
    {
        reportInfo """
            The ${Utility.link('advanced UI  example', AdvancedUI)} is absolutely packed with things to see.
            Yet, at the same time, it is not too much code
            and also not too complicated. 
            It really shows you
            how boilerplate code can be reduced to a minimum
            when using SwingTree.
        """

        given : 'We create the UI.'
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
            new Utility.Query(ui).find(JSplitButton, "con-split-button").get().popupMenu.components.length == 4
    }

    def 'The form UI defined in the examples has the expected state.'()
    {
        reportInfo """
            The form UI looks like this:
            ${Utility.linkSnapshot('views/simple-form-UI.png')}
            
            This is the ${Utility.link('oldest example in the test suite', Form)} and also a great
            first example of how to write declarative UI code with SwingTree.
        """
        given : 'We get the UI.'
            var ui = new Form()
        expect : """
            We should be able to find the hover icon button and it should have the expected state.
        """
            new Utility.Query(ui).find(JButton, "hover-icon-button").isPresent()
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().text == ""
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().icon != null
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().pressedIcon != null
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().rolloverIcon != null
        and :
            new Utility.Query(ui).find(JButton, "hover-icon-button").get().cursor.type == Cursor.HAND_CURSOR
        and :
            Utility.similarityBetween(ui, "views/simple-form-UI.png", 99.9) > 99.9
    }

    def 'The login example UI defined in the examples, a good MVVM demonstration.'()
    {
        reportInfo """
            The ${Utility.link('login example view', LoginView)}, together
            with the ${Utility.link('login example view model', LoginViewModel)}, is
            a really basic example of how to do MVVM with SwingTree.
            For a more advanced example, see the ${Utility.link('user registration example', UserRegistrationView)}.
        """
        expect : new LoginView(new LoginViewModel())
    }

    def 'The user registration example UI defined in the examples, a good MVVM demonstration.'()
    {
        reportInfo """
            The ${Utility.link('user registration example', UserRegistrationView)}, together
            with the ${Utility.link('user registration example view model', UserRegistrationViewModel)}, is
            a more advanced example of how to do the MVVM design pattern with SwingTree.
            It involves a lot of input validation that is done in the view model.
            For a more basic example, see the ${Utility.link('login example view', LoginView)}.
        """
        expect : new UserRegistrationView(new UserRegistrationViewModel())
    }

    def 'The list examples UI defined in the examples can be created.'()
    {
        expect : new ListSearchView(new ListSearchViewModel())
    }

    def 'The box shadow picker example UI defined in the examples can be created.'()
    {
        reportInfo """
            Not only is the ${Utility.link('box shadow picker example', BoxShadowPickerView)} 
            an advanced example of how to write MVVM applications with SwingTree,
            it is also a nice little tool that shows you how to use the 
            SwingTree style API to override the default look and feel of Swing components.
        """
        expect : new BoxShadowPickerView(new BoxShadowPickerViewModel())
    }

    def 'The note guesser example UI defined in the examples can be created.'()
    {
        reportInfo """
            The guesser example view looks like this:
            ${Utility.linkSnapshot('views/note-guesser-UI.png')}
            This not only demonstrates how to do some custom 2D rendering on a JPanel,
            it is also a nice little game you can play to test your knowledge of music theory.

            Just ${Utility.link('navigate to the class', NoteGuesserView)} and run
            it yourself and play around with it.
            The ${Utility.link('view model', NoteGuesserViewModel)} is also a nice example of how to
            do MVVM with SwingTree.
        """
        given : 'We create an instance of the UI.'
            var ui = new NoteGuesserView(new NoteGuesserViewModel())
        expect : 'It is rendered as shown in the image.'
            Utility.similarityBetween(ui, "views/note-guesser-UI.png", 93) > 93
    }

    def 'The symbol guesser example UI defined in the examples can be created.'()
    {
        expect : new SymbolGuesserView(new SymbolGuesserViewModel())
    }

    def 'The "Well rounded" style example UI defined in the examples can be created.'()
    {
        reportInfo """
            The well rounded UI looks like this:
            ${Utility.linkSnapshot('views/well-rounded-UI.png')}

            This is the most basic example of how to use the SwingTree style API to
            override the default look and feel of Swing components.
            As you can see, the style engine of SwingTree supports shadows, rounded corners,
            gradients and much much more out of the box.
            
            Here we will only test looks of the UI, if you want to see the code, 
            ${Utility.link('click here', WellRoundedView)} to jump to the class.
            And also feel free to run 
            it yourself, it has a main method for that purpose already.
        """
        given : 'We create the UI.'
            var ui = new WellRoundedView()
        expect : 'It is rendered as shown in the image.'
            Utility.similarityBetween(ui, "views/well-rounded-UI.png", 98.1) > 98.1
    }

    def 'The soft example UI is rendered as expected.'()
    {
        reportInfo """
            The soft example UI looks like this:
            ${Utility.linkSnapshot('views/soft-example-UI.png')}
            
            This is the most advanced example of how to use the SwingTree style API to
            customize the look and feel of your entire application.
            You can turn a plain old metal look and feel into a modern 
            soft UI with just a few lines of code.
           
            Here we will only test looks of the UI, if you want to see the code,
            ${Utility.link('click here', SoftUIView)} to visit the class.
            And also feel free to run
            it yourself, it has a main method for that purpose already.
        """
        given : 'We create the UI.'
            var ui = new SoftUIView()
        expect : 'It is rendered as shown in the image.'
            Utility.similarityBetween(ui, "views/soft-example-UI.png", 98.7) > 98.7
    }

    def 'The animated buttons view examples UI defined in the examples looks as expected.'()
    {
        reportInfo """
            The animated buttons view looks like this:
            ${Utility.linkSnapshot('views/animated-buttons-UI.png')}
            
            Unfortunately this is just a snapshot of the UI, so you can't play with 
            the animations right here. But you can run the 
            ${Utility.link('example found in the test suite', AnimatedButtonsView)}
            of SwingTree and try it out yourself.
            There is a main method in the class, so you can just run it.
        """
        given : 'We create the UI.'
            var ui = new AnimatedButtonsView()
            ui.setBackground(new Color(242, 242, 242))
        expect : 'It is rendered as shown in the image.'
            Utility.similarityBetween(ui, "views/animated-buttons-UI.png", 94.3) > 94.3
    }

    def 'The animation example view can be created.'()
    {
        expect : new AnimatedView(Mock(JFrame))
    }

    def 'The settings example UI defined in the examples looks as expected.'()
    {
        reportInfo """
            The vertical settings view from the examples looks like this:
            ${Utility.linkSnapshot('views/vertical-settings-UI.png')}
            
            It demonstrates how UI components can easily be placed vertically
            and with some slight indentation, to indicate a certain grouping
            which is especially useful for settings dialogs.
            
            If you want to see the code, just
            ${Utility.link('navigate to the class', SomeSettingsView)} and run it yourself, 
            it has a main method for that purpose already.
            
            Here we will only test the UI.
        """
        given :
            SwingTree.reinitialiseUsing( it -> it.defaultFont(new Font("DejaVu Sans", Font.PLAIN, 12)))
            var view = new SomeSettingsView(new SomeSettingsViewModel())
            var speedTextField = new Utility.Query(view).find(JTextField, "speed-text-field").orElse(null)
        expect :
            view != null
            speedTextField != null
        and :
            speedTextField.text == "42.0"
            speedTextField.background == Color.WHITE
        when : 'We rerender the view offscreen...'
            var similarity = Utility.similarityBetween(view, "views/vertical-settings-UI.png", 99)
        then : '...it looks as expected.'
            similarity > 99

        when : 'We simulate the user entering an invalid number:'
            speedTextField.text = "§"
            UI.sync()
        then : 'The UI is updated to reflect the invalid value.'
            speedTextField.text == "§"
            speedTextField.background == Color.RED

        when : 'We rerender the view offscreen again because the background color changed...'
            similarity = Utility.similarityBetween(view, "views/vertical-settings-UI.png")
        then : 'The UI is no longer rendered as expected.'
            similarity < 99
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
            ${Utility.linkSnapshot('views/calculator-UI.png')}
            
            Here we only check that it is rendered as expected and we will not go further into detail 
            with respect to its code, if you want to see the code, just 
            ${Utility.link('navigate to the class', Calculator)} 
            and run it yourself, it has 
            a main method for that purpose already.
        """
        given : 'We create the UI.'
            var ui = new Calculator()
            ui.setBackground(new Color(242, 242, 242))
        expect :
            new Utility.Query(ui).find(JTextField, "input-text-area").isPresent()
            new Utility.Query(ui).find(JTextField, "input-text-area").get().horizontalAlignment == JTextField.RIGHT
        and : 'Its render state is as expected.'
            Utility.similarityBetween(ui, "views/calculator-UI.png", 97.5) > 97.5
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
