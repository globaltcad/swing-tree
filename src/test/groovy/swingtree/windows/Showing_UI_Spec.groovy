package swingtree.windows

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.threading.EventProcessor
import swingtree.UI
import swingtree.UIForJDialog
import swingtree.UIForJFrame
import swingtree.input.Keyboard

import javax.swing.JDialog
import javax.swing.JFrame

@Title("Showing UIs")
@Narrative('''

    Any user interface needs a way to be shown to the user. 
    In Swing, this is done through various kinds of classes, 
    namely, the JFrame, JDialog and JWindow classes.
    
    Swing-Tree allows you to instantiate and configure these
    instances in a declarative fashion, and then show them
    to the user.

''')
@Subject([UI, UIForJFrame, UIForJDialog])
class Showing_UI_Spec extends Specification
{
    def setupSpec() {
        UI.SETTINGS().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def 'Use the "frame()" factory method to build a JFrame.'()
    {
        reportInfo """
            The "frame()" factory method returns a builder instance
            which can be used to configure the JFrame instance
            using method chaining.
            Use the "show()" method at the end of your chain to
            show the JFrame to the user.
        """
        given :
            var record = []
        and : 'Then we build a declarative JFrame wrapping a button.'
            var frame =
                UI.frame()
                .withTitle("My JFrame")
                .onFocusGained({ record << "focus gained" })
                .onFocusLost({ record << "focus lost" })
                .add(
                    UI.button().withText("My Button")
                    .onClick({ record << "button clicked" })
                )
                .get(JFrame)

        expect :
            frame.title == "My JFrame"
            frame.focusListeners.size() == 2
            frame.contentPane.componentCount == 1
            frame.contentPane.getComponent(0).text == "My Button"
            frame.contentPane.getComponent(0).actionListeners.size() == 1

        when :
            frame.requestFocus()
            frame.contentPane.getComponent(0).doClick()

        then :
            record == ["button clicked"]
    }

    def 'Use the "dialog()" factory method to build a JDialog.'()
    {
        reportInfo """
            The "dialog()" factory method returns a builder instance
            which can be used to configure the JDialog instance
            using method chaining.
            Use the "show()" method at the end of your chain to
            show the JDialog to the user.
        """
        given :
            var record = []
        and : 'Then we build a declarative JDialog wrapping a label and a toggle button.'
            var dialog =
                UI.dialog()
                .withTitle("My JDialog")
                .onPressed(Keyboard.Key.ESCAPE, { record << 'esc pressed' })
                .add(
                    UI.panel("wrap 1")
                    .add(UI.label("Hey I am in a JDialog!"))
                    .add(
                        UI.toggleButton().withText("Toggle Me!")
                        .onClick({ record << "toggled" })
                    )
                )
                .get(JDialog)

        expect :
            dialog.title == "My JDialog"
            dialog.contentPane.componentCount == 1
            dialog.contentPane.getComponent(0).componentCount == 2
            dialog.contentPane.getComponent(0).getComponent(0).text == "Hey I am in a JDialog!"
            dialog.contentPane.getComponent(0).getComponent(1).text == "Toggle Me!"
            dialog.contentPane.getComponent(0).getComponent(1).actionListeners.size() == 1

        when :
            dialog.contentPane.getComponent(0).getComponent(1).doClick()

        then :
            record == ["toggled"]
    }

    def 'The "show" method causes a JFrame to be displayed to the user.'()
    {
        given : 'We mock the JFrame to inspect if it is set to visible.'
            def frame = Mock(JFrame)
            frame.components >> []
        when : 'We build a declarative JFrame and show it to the user.'
            UI.of(frame)
            .withTitle("My JFrame")
            .add(UI.button().withText("My Button"))
            .show()
        then : 'We verify that the JFrame was shown.'
            1 * frame.setVisible(true)
        and : 'We also expect the title to be set.'
            1 * frame.setTitle('My JFrame')
    }

    def 'The "show" method causes a JDialog to be displayed to the user.'()
    {
        given : 'We mock the JDialog to inspect if it is set to visible.'
            def dialog = Mock(JDialog)
            dialog.components >> []
        when : 'We build a declarative JDialog and show it to the user.'
            UI.of(dialog)
            .withTitle("My JDialog")
            .add(UI.button().withText("My Button"))
            .show()
        then : 'We verify that the JDialog was shown.'
            1 * dialog.setVisible(true)
        and : 'We also expect the title to be set.'
            1 * dialog.setTitle('My JDialog')
    }
}
