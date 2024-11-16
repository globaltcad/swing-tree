package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.Event
import sprouts.Var
import swingtree.threading.EventProcessor

import javax.swing.*
import java.awt.event.MouseEvent

@Title("Registering Event Handlers")
@Narrative('''
    
    An important part of a UI declaration is the ability to hook up event handlers
    to your components. In SwingTree you do not need to unpack the underlying Swing
    component to register a particular event listener, but instead you can register
    event handlers directly on the UI declaration.    
    This specification demonstrates how different types of event handlers for different
    types of events and components can be registered using simple lambda expressions.
        
''')
class Event_Handling_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def 'The "onChange" event handlers are triggered in the same order as they were registered.'()
    {
        reportInfo """
            This type of event occurs when the state of the radio button changes.
            Internally this is based on an `ItemListener` which
            will be triggered by the radio button and then call 
            your Swing-Tree event handler implementation.
        """

        given : 'A simple list where handlers are going to leave a trace.'
            var trace = []

        and : 'A simple radio button UI.'
            var ui =
                    UI.radioButton("CLICK ME")
                    .onChange( it -> trace.add("1") )
                    .onChange( it -> trace.add("2") )
                    .onChange( it -> trace.add("3") )
                    .onChange( it -> trace.add("4") )
                    .onChange( it -> trace.add("5") )
                    .onChange( it -> trace.add("6") )
                    .onChange( it -> trace.add("7") )

        when : 'The button is clicked.'
            ui.get(JRadioButton).doClick()

        then : 'The handlers are triggered in the same order as they were registered.'
            trace == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'The "onClick" event handlers are triggered in the same order as they were registered.'()
    {
        reportInfo """
            This type of event occurs when the user clicks the button.
            Internally this is based on an `ActionListener` which
            will be triggered by the button and then call 
            your Swing-Tree event handler implementation.
        """

        given : 'A simple list where handlers are going to leave a trace.'
            var trace = []

        and : 'A simple button UI.'
            var ui =
                    UI.button("CLICK ME")
                    .onClick( it -> trace.add("1") )
                    .onClick( it -> trace.add("2") )
                    .onClick( it -> trace.add("3") )
                    .onClick( it -> trace.add("4") )
                    .onClick( it -> trace.add("5") )
                    .onClick( it -> trace.add("6") )
                    .onClick( it -> trace.add("7") )

        when : 'The button is clicked.'
            ui.get(JButton).doClick()

        then : 'The handlers are triggered in the same order as they were registered.'
            trace == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'The "onResize" event handlers are triggered in the same order as they were registered.'()
    {
        reportInfo """
            This type of event occurs when the user changes the size of your component.
            Internally this is based on an `ComponentListener` which
            will be triggered by the window and then call 
            your Swing-Tree event handler implementation.
        """
        given : 'A simple list where handlers are going to leave a trace.'
            var trace = []

        and : 'A simple text field UI.'
            var ui =
                    UI.formattedTextField("Some content...")
                    .onResize( it -> trace.add("1") )
                    .onResize( it -> trace.add("2") )
                    .onResize( it -> trace.add("3") )
                    .onResize( it -> trace.add("4") )
                    .onResize( it -> trace.add("5") )
                    .onResize( it -> trace.add("6") )
                    .onResize( it -> trace.add("7") )

        when : 'The text field is resized.'
            ui.get(JFormattedTextField).setSize(100, 100)
            UI.sync()

        then : 'The handlers are triggered in the same order as they were registered.'
            trace == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'The "onShown" event handlers are triggered in the same order as they were registered.'()
    {
        reportInfo """
            This type of event occurs when the component is made visible.
            Internally this is based on an `ComponentListener` which
            will for example be triggered by the `setVisible(boolean)` method
           and then call your Swing-Tree event handler implementation.
        """
        given : 'A simple list where handlers are going to leave a trace.'
            var trace = []

        and : 'A simple text area UI.'
            var ui =
                    UI.textArea("Some content...")
                    .onShown( it -> trace.add("1") )
                    .onShown( it -> trace.add("2") )
                    .onShown( it -> trace.add("3") )
                    .onShown( it -> trace.add("4") )
                    .onShown( it -> trace.add("5") )
                    .onShown( it -> trace.add("6") )
                    .onShown( it -> trace.add("7") )
        and : 'We actually build the component:'
            var panel = ui.get(JTextArea)

        when : 'We try to trigger the shown event...'
            panel.setVisible(false)
            UI.sync()
            panel.setVisible(true)
            UI.sync()
        then : 'Nothing happens because the text area is not in a window.'
            trace == []

        when : 'We then put it into a window:'
            var frame = UI.frame("Test").add(panel).get(JFrame)
        and : 'Try again to trigger the shown event....'
            panel.setVisible(false)
            UI.sync()
            panel.setVisible(true)
            UI.sync()
        then : 'Again, nothing happens, because the frame is not visible.'
            trace == []

        when : 'We make the window visible.'
            frame.setVisible(true)
            UI.sync()
        then : 'The handlers are triggered in the same order as they were registered.'
            trace == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'The "onHidden" event handlers are triggered in the same order as they were registered.'()
    {
        reportInfo """
            This type of event occurs when the component is made invisible.
            Internally this is based on an `ComponentListener` which
            will for example be triggered by the `setVisible(boolean)` method
           and then call your Swing-Tree event handler implementation.
        """
        given : 'A simple list where handlers are going to leave a trace.'
            var trace = []

        and : 'A simple text area UI.'
            var ui =
                    UI.textArea("Some content...")
                    .onHidden( it -> trace.add("1") )
                    .onHidden( it -> trace.add("2") )
                    .onHidden( it -> trace.add("3") )
                    .onHidden( it -> trace.add("4") )
                    .onHidden( it -> trace.add("5") )
                    .onHidden( it -> trace.add("6") )
                    .onHidden( it -> trace.add("7") )
        and : 'We actually build the component:'
            var panel = ui.get(JTextArea)
        and : 'We make the panel visible initially:'
            panel.setVisible(true)
            UI.sync()

        expect : 'The trace is empty because the text area has not been hidden.'
            trace == []

        when : 'We then put it into a window:'
            var frame = UI.frame("Test").add(panel).get(JFrame)
        and : 'We trigger the hidden event by making the frame visible and then invisible.'
            frame.setVisible(true)
            UI.sync()
            frame.setVisible(false)
            UI.sync()

        then : 'The handlers are triggered in the same order as they were registered.'
            trace == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'The "onEnter" event handlers are triggered in the same order as they were registered.'()
    {
        reportInfo """
            This type of event occurs when the user presses the enter key.
            Internally this is actually based on an `ActionListener` which
            will be triggered by the text field and then call
            your Swing-Tree event handler implementation.
        """
        given : 'A simple list where handlers are going to leave a trace.'
            var trace = []

        and : 'A simple text field UI.'
            var ui =
                    UI.textField("Some content...")
                    .onEnter( it -> trace.add("1") )
                    .onEnter( it -> trace.add("2") )
                    .onEnter( it -> trace.add("3") )
                    .onEnter( it -> trace.add("4") )
                    .onEnter( it -> trace.add("5") )
                    .onEnter( it -> trace.add("6") )
                    .onEnter( it -> trace.add("7") )
        and : 'We build the text field:'
            var textField = ui.get(JTextField)

        when : 'Something is entered in the text field.'
            textField.setText("Some other content...")
            // We trigger the "enter" event by simulating an enter key press (which causes the action listener to be triggered).
            textField.postActionEvent()
            UI.sync()

        then : 'The handlers are triggered in the same order as they were registered.'
            trace == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'In a formatted text field, the "onEnter" event handlers are triggered in the same order as they were registered.'()
    {
        reportInfo """
            This type of event occurs when the user presses the enter key.
            Internally this is actually based on an `ActionListener` which
            will be triggered by the formatted text field and then call
            your Swing-Tree event handler implementation.
        """
        given : 'A simple list where handlers are going to leave a trace.'
            var trace = []

        and : 'A simple text field UI.'
            var ui =
                    UI.formattedTextField("Some content...")
                    .onEnter( it -> trace.add("1") )
                    .onEnter( it -> trace.add("2") )
                    .onEnter( it -> trace.add("3") )
                    .onEnter( it -> trace.add("4") )
                    .onEnter( it -> trace.add("5") )
                    .onEnter( it -> trace.add("6") )
                    .onEnter( it -> trace.add("7") )
        and : 'We build the formatted text field:'
            var formattedTextField = ui.get(JFormattedTextField)

        when : 'Something is entered in the text field.'
            formattedTextField.setText("Some other content...")
            // We trigger the "enter" event by simulating an enter key press (which causes the action listener to be triggered).
            formattedTextField.postActionEvent()
            UI.sync()

        then : 'The handlers are triggered in the same order as they were registered.'
            trace == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'The "onSelection" event handlers of a combo box are triggered in the same order as they were registered.'()
    {
        reportInfo """
            This type of event occurs when the user selects an item in the combo box.
            Internally this is actually based on an `ActionListener` which
            will be triggered by the combo box and then call
            your Swing-Tree event handler implementation.
        """
        given : 'A simple list where handlers are going to leave a trace.'
            var trace = []

        and : 'A simple combo box UI.'
            var ui =
                    UI.comboBox("Item 1", "Item 2", "Item 3")
                    .onSelection( it -> trace.add("1") )
                    .onSelection( it -> trace.add("2") )
                    .onSelection( it -> trace.add("3") )
                    .onSelection( it -> trace.add("4") )
                    .onSelection( it -> trace.add("5") )
                    .onSelection( it -> trace.add("6") )
                    .onSelection( it -> trace.add("7") )
        and : 'We build the combo box:'
            var comboBox = ui.get(JComboBox)

        when : 'An item is selected in the combo box.'
            comboBox.setSelectedIndex(1)
            UI.sync()

        then : 'The handlers are triggered in the same order as they were registered.'
            trace == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'The "onContentChange" event handler of a text area is triggered when the text area content changes.'()
    {
        reportInfo """
            This type of event occurs when the user changes the content of the text area.
            Internally this is actually based on a `DocumentListener`
            which will forward any one of its events to your `onContentChange` handler.
        """
        given : 'A simple text area UI.'
            var ui = UI.textArea("Some content...")

        and : 'A simple list where handlers are going to leave a trace.'
            var trace = []

        and : 'A handler that records the text area content.'
            ui = ui.onContentChange( it -> trace.add(it.component.text) )

        when : 'The text area content is changed.'
            UI.runNow { ui.get(JTextArea).setText("Some other content...") }
            UI.sync()

        then : 'The handler is triggered.'
            trace == ["", "Some other content..."]
    }

    def 'The "onTextChange" event handler of a text area is triggered when the text area content changes.'()
    {
        reportInfo """
            This type of event occurs when the user changes the text of the text area.
            Internally this is actually based on a `DocumentListener`
            which will forward the `insertUpdate` and `removeUpdate` method calls 
            to your `onTextChange` handler.
        """
        given : 'A simple text area UI.'
            var ui = UI.textArea("Some content...")

        and : 'A simple list where handlers are going to leave a trace.'
            var trace = []

        and : 'A handler that records the text area content.'
            ui = ui.onTextChange( it -> trace.add(it.component.text) )

        when : 'The text area content is changed.'
            UI.runNow { ui.get(JTextArea).setText("Some other content...") }
            UI.sync()

        then : 'The handler is triggered.'
            trace == ["", "Some other content..."]
    }

    def 'The "onTextInsert" event handler of a text area is triggered when the text area content changes.'()
    {
        reportInfo """
            This type of event occurs when the user inserts text into the text area.
            Internally this is actually based on a `DocumentListener`
            which will forward the `insertUpdate` method call 
            to your `onTextInsert` handler.
        """
        given : 'A simple text area UI.'
            var ui = UI.textArea("Some content...")

        and : 'A simple list where handlers are going to leave a trace.'
            var trace1 = []
            var trace2 = []

        and : 'A handler that records the text area content.'
            ui = ui.onTextInsert( it -> {
                    trace1.add(it.component.text)
                    trace2.add(it.textToBeInserted)
                })

        when : 'The text area content is changed.'
            UI.runNow { ui.get(JTextArea).document.insertString(5, "other ", null) }
            UI.sync()

        then : 'The handler is not triggered.'
            trace1 == ["Some content..."]
            trace2 == ["other "]
    }

    def 'The "onTextRemove" event handler of a text area is triggered when the text area content changes.'()
    {
        reportInfo """
            This type of event occurs when the user changes the content of the text area.
            Internally this is actually based on a `DocumentListener`
            which will forward the `removeUpdate` method call 
            to your `onTextRemove` handler.
        """
        given : 'A simple text area UI.'
            var ui = UI.textArea("Some long content...")

        and : 'A simple list where handlers are going to leave a trace.'
            var trace1 = []
            var trace2 = []

        and : 'A handler that records the text area content.'
            ui = ui.onTextRemove( it -> {
                    trace1.add(it.component.text)
                    trace2.add(it.textToBeRemoved)
                })

        when : 'The text area content is changed.'
            UI.runNow { ui.get(JTextArea).document.remove(5, 5) }
            UI.sync()

        then : 'The handler is not triggered.'
            trace1 == ["Some long content..."]
            trace2 == ["long "]
    }

    def 'The "onTextReplace" event handler of a text area is triggered when the text area content changes.'()
    {
        reportInfo """
            This type of event occurs when the user replaces text in the text area.
            Internally this is actually based on a `DocumentFilter`
            which will forward the `replace` method calls 
            to your `onTextReplace` handler.
        """
        given : 'A simple text area UI.'
            var ui = UI.textArea("Some content...")

        and : 'A simple list where handlers are going to leave a trace.'
            var trace1 = []
            var trace2 = []
            var trace3 = []

        and : 'A handler that records the text area content.'
            ui = ui.onTextReplace( it -> {
                    trace1.add(it.component.text)
                    trace2.add(it.replacementText)
                    trace3.add("$it.offset|$it.length")
                })
        and : 'Finally we build the text area:'
            var textArea = ui.get(JTextArea)

        when : 'The text area content is changed.'
            UI.runNow { textArea.setText("Some other content...") }
            UI.sync()

        then : 'The handler was triggered.'
            trace1 == ["Some content..."]
            trace2 == ["Some content..."] // We replace everything.
            trace3 == ["0|15"]
    }

    def 'The "onTextReplace" event handlers will be triggered in the correct order.'()
    {
        given : 'A simple text area UI.'
            var ui = UI.textArea("Some content...")

        and : 'A simple list where handlers are going to leave a trace.'
            var trace = []

        and : 'A handler that records the text area content.'
            ui = ui.onTextReplace( it -> trace.add("1") )
                   .onTextReplace( it -> trace.add("2") )
                   .onTextReplace( it -> trace.add("3") )
                   .onTextReplace( it -> trace.add("4") )
                   .onTextReplace( it -> trace.add("5") )
                   .onTextReplace( it -> trace.add("6") )
                   .onTextReplace( it -> trace.add("7") )
        and : 'Finally we build the text area:'
            var textArea = ui.get(JTextArea)

        when : 'The text area content is changed.'
            UI.runNow { textArea.setText("Some other content...") }
            UI.sync()

        then : 'The handlers are triggered in the correct order.'
            trace == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'Turn your view model `Event`s into custom component events.'()
    {
        reportInfo """
            Although there are many different types of events for which you can register
            event handlers, there are cases where you may want to create your own custom events
            for which you can register component event handlers.
            This is what the `on(Observable, Action<Observable>)` method is for.
            
            A common use case for this are events which are user input or system
            input events. If you want view model events to have an effect on your view
            when they are triggered, you can use the `onView(Observable, Action)` method,
            which executes the given action on the EDT.
            
            Here we demonstrate this using the `Observable` interface.
        """

        given : 'A list where handlers are going to leave a trace.'
            var trace = []

        and : 'An `Observable` instance that is going to be triggered.'
            var observable = Event.create() // An event is also an observable

        and : 'A little example UI consisting of a panel with a text field:'
            var ui =
                    UI.panel("fill, insets 3", "[grow][shrink]")
                    .add("grow",
                        UI.textField("I am a text field")
                        .on(observable, it -> trace.add("1"))
                    )
        and : 'We then actually trigger the component to be built:'
            var panel = ui.get(JPanel)

        when : 'The observable is triggered.'
            observable.fire()

        then : 'The handler is triggered.'
            trace == ["1"]
    }

    def 'Turn your view model properties into custom component events.'()
    {
        reportInfo """
            Although the regular SwingTree API exposes a wide variety of different types of events 
            for which you can register component specific event handlers, 
            there are cases where you may want to create your own custom events
            for which you can register component event handlers.
            This is what the `onView(Observable, Action)` method is for.
            
            A common use case for this concerns are the `Val` and `Var` property types
            which also implement the `Observable` interface, making them suitable
            for use as custom component events in your view.
            Properties are designed to be used for modelling or delegating the state of your view model
            and when they change, your view is supposed to produce some kind of effect,
            like an animation or a sound effect or something similar.
        """

        given : 'A list where handlers are going to leave a trace.'
            var trace = []

        and : 'A property that is going to be triggered.'
            var property = Var.of("I am a text field")

        and : 'A little example UI consisting of a panel with a text field:'
            var ui =
                    UI.panel("fill, insets 3", "[grow][shrink]")
                    .add("grow",
                        UI.textField(property)
                        .onView(property, it -> trace.add(it.event.get()))
                    )
        and : 'We actually build the component:'
            var panel = ui.get(JPanel)

        when : 'The property is triggered.'
            property.set("I am a different text field")

        then : 'The handler is triggered and the trace contains the new value.'
            trace == ["I am a different text field"]
    }

    def 'The "onMouseEnter" event handler receives an enter event when the mouse enters the component body.'()
    {
        reportInfo """
            This type of event occurs when the mouse enters the component body.
            Internally this is based on a `MouseListener` which will forward the `mouseEntered` event
            to your `onMouseEnter` handler.
        """
        given : 'A simple list where handlers are going to leave a trace.'
            var trace = []

        and : 'A simple text field UI.'
            var ui =
                    UI.panel("fill").withSize(100, 100)
                    .withStyle(it->it.margin(20))
                    .onMouseEnter( it -> trace.add("!") )
        and : 'We actually build the component:'
            var panel = ui.get(JPanel)

        when : 'We dispatch various fake global mouse events which land on the margin area of the component,'
            UI.runNow {
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_ENTERED, 0, 0, 10, 5, 0, false))
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_ENTERED, 1, 0, 95, 3, 0, false))
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_ENTERED, 2, 0, 19, 83, 0, false))
            }

        then : 'The handler is not triggered, because the mouse is not inside the component body.'
            trace == []

        when : 'We dispatch a fake global mouse event which lands inside the component.'
            UI.runNow {
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_ENTERED, 3, 0, 50, 50, 0, false))
            }

        then : 'The handler is triggered.'
            trace == ["!"]
    }

}
