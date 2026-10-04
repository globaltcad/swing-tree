package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.From
import sprouts.Val
import sprouts.Var
import swingtree.components.JSplitButton
import swingtree.layout.Size
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.JTextPane
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.AbstractDocument
import javax.swing.text.JTextComponent
import java.awt.*

@Title("Binding Properties to UI Components")
@Narrative('''

    SwingTree includes support for writing UIs using the MVVM pattern,
    by shipping with a set of properties that can be bound to UI components
    to model their state.
    This specification demonstrates how to bind the properties of
    you view model to the SwingTree UI.

''')
@Subject([Val, Var])
class Property_Binding_Spec extends Specification
{
    enum Accept { YES, NO, MAYBE }

    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def 'We can bind a property to the size of a swing component.'( float uiScale )
    {
        reportInfo"""
            Note that the binding of a Swing-Tree property will only have side effects
            when it is deliberately triggered to execute its side effects.
            This is important to allow you to decide yourself when
            the state of a property is "ready" for display in the UI.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)

        and : 'We create a property representing the size of a component.'
            Val<Size> size = Var.of(Size.of(100, 100))
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Hello World").withPrefSize(size))
                        .add(UI.button("Click Me").withMinSize(size))
                        .add(UI.textField("Hello World").withMaxSize(size))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The components will have the size of the property.'
            panel.components[0].preferredSize == new Dimension((int)(100 * uiScale), (int)(100 * uiScale))
            panel.components[1].minimumSize == new Dimension((int)(100 * uiScale), (int)(100 * uiScale))
            panel.components[2].maximumSize == new Dimension((int)(100 * uiScale), (int)(100 * uiScale))

        when : 'We change the value of the property.'
            size.set(Size.of(200, 200))
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The components will have the new sizes.'
            panel.components[0].preferredSize == new Dimension((int)(200 * uiScale), (int)(200 * uiScale))
            panel.components[1].minimumSize == new Dimension((int)(200 * uiScale), (int)(200 * uiScale))
            panel.components[2].maximumSize == new Dimension((int)(200 * uiScale), (int)(200 * uiScale))
        where : """
            We use the following integer scaling factors simulating different high DPI scenarios.
            Note that usually the UI is scaled by 1, 1.5 an 2 (for 4k screens for example).
            A scaling factor of 3 is rather unusual, however it is possible to scale it by 3 nonetheless.
        """ 
            uiScale << [3, 2, 1]
    }

    def 'Simple integer properties can be bound to the width or height of components.'( float uiScale )
    {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create properties representing the width and heights of a components.'
            Var<Integer> minWidth = Var.of(60)
            Var<Integer> prefHeight = Var.of(40)
            Var<Integer> maxWidth = Var.of(90)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Hello World").withMinWidth(minWidth))
                        .add(UI.button("Click Me").withPrefHeight(prefHeight))
                        .add(UI.textField("Hello World").withMaxWidth(maxWidth))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'The components will have the sizes of the properties.'
            panel.components[0].minimumSize.width == (int) ( 60 * uiScale )
            panel.components[1].preferredSize.height == (int) ( 40 * uiScale )
            panel.components[2].maximumSize.width == (int) ( 90 * uiScale )

        when : 'We change the value of the properties.'
            minWidth.set(100)
            prefHeight.set(80)
            maxWidth.set(120)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The components will have the new sizes.'
            panel.components[0].minimumSize.width == (int) ( 100 * uiScale )
            panel.components[1].preferredSize.height == (int) ( 80 * uiScale )
            panel.components[2].maximumSize.width == (int) ( 120 * uiScale )
        where :
            uiScale << [1f, 1.5f, 2f]
    }

    def 'Bind to both width and height independently if you want to.'( int uiScale )
    {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
        SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a property representing the width of a component.'
            Var<Integer> width = Var.of(60)
            Var<Integer> height = Var.of(40)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Hello World").withMinSize(width, height))
                        .add(UI.toggleButton("Click Me").withPrefSize(width, height))
                        .add(UI.textArea("Hello World").withMaxSize(width, height))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'The components will have the sizes of the properties.'
            panel.components[0].minimumSize == new Dimension(60 * uiScale, 40 * uiScale)
            panel.components[1].preferredSize == new Dimension(60 * uiScale, 40 * uiScale)
            panel.components[2].maximumSize == new Dimension(60 * uiScale, 40 * uiScale)

        when : 'We change the value of the properties.'
            width.set(100)
            height.set(80)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The components will have the new sizes.'
            panel.components[0].minimumSize == new Dimension(100 * uiScale, 80 * uiScale)
            panel.components[1].preferredSize == new Dimension(100 * uiScale, 80 * uiScale)
            panel.components[2].maximumSize == new Dimension(100 * uiScale, 80 * uiScale)
        where : """
            We use the following integer scaling factors simulating different high DPI scenarios.
            Note that usually the UI is scaled by 1, 1.5 an 2 (for 4k screens for example).
            A scaling factor of 3 is rather unusual, however it is possible to scale it by 3 nonetheless.
        """ 
            uiScale << [3, 2, 1]
    }

    def 'We can bind to the color of a component.'()
    {
        given : 'We create a property representing the color of a component.'
            Val<Color> property = Var.of(Color.RED)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.button("Click Me!"))
                        .add(UI.label("I have a Background").withBackground(property))
                        .add(UI.textField("Hello World"))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The label will have the background color of the property.'
            panel.components[1].background == Color.RED

        when : 'We change the value of the property.'
            property.set(Color.BLUE)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The label will have the new color.'
            panel.components[1].background == Color.BLUE
    }

    def 'We can bind to the text of a component.'()
    {
        given : 'We create a property representing the text of a component.'
            Val<String> property = Var.of("Hello World")
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.button("Click Me!"))
                        .add(UI.textField("Hello World").withText(property))
                        .add(UI.checkBox("Hello World"))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The text field will have the text of the property.'
            panel.components[1].text == "Hello World"

        when : 'We change the value of the property.'
            property.set("Goodbye World")
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The text field will have the new text.'
            panel.components[1].text == "Goodbye World"
    }

    def 'When a bound `Var<String>` replaces the text, the caret moves into the new text and typing keeps working.'()
    {
        reportInfo """
            A common use of `UI.textArea(Var<String>)` is a message box which
            the view model clears once the message is sent. The caret of the
            text area is managed by Swing, and it has to follow the text
            which the property puts into the area: when the old text is
            longer than the new one, a caret left at its old position would
            point past the end of the new text. Every key the user types
            after that would be rejected with an error beep, until a mouse
            click moves the caret back into the text.

            Note that we type into the text area on the Swing thread, like a
            real key press does, because a Swing caret only follows the
            edits which are made on that thread. And the text area hands
            what the user typed to the property in a later turn of the Swing
            thread, which is why we wait for it with `UI.sync()`.
        """
        given : 'A message property and a text area bound to it.'
            Var<String> message = Var.of("")
            var area = UI.textArea(message).get(JTextArea)

        when : 'The user types two lines into the text area.'
            UI.runNow { area.replaceSelection("Hello\nWorld") }
            UI.sync()
        then : 'The property holds what the user typed, and the caret sits at the end of it.'
            message.get() == "Hello\nWorld"
            area.caretPosition == "Hello\nWorld".length()

        when : 'The view model clears the message, as if it had just been sent.'
            UI.runNow { message.set(From.VIEW_MODEL, "") }
        then : 'The text area is empty, and its caret sits at the start of the empty text.'
            area.text == ""
            area.caretPosition == 0

        when : 'The user types the next character.'
            UI.runNow { area.replaceSelection("!") }
            UI.sync()
        then : 'The character lands in the text area and travels to the property.'
            area.text == "!"
            message.get() == "!"
    }

    def 'When a bound `Var<String>` replaces the text of a #componentType, the component takes the height of the new text.'(
        String componentType, Var<String> text, Closure<JTextComponent> build
    ) {
        reportInfo """
            Swing does not paint the text of a text component directly from
            the text. It builds a tree of views from the document, typically
            one view per line or paragraph, and those views compute the
            preferred size of the component and decide which strip of it
            needs to be repainted after a key press.

            So when a property replaces the text, the views have to be
            rebuilt for the new text. If they still described the old text,
            a component which wraps its lines would keep the height of the old
            text, and the characters typed afterwards would land outside the
            repainted strip, so they would only show up after something
            repaints the whole component.

            We use the height the component has while it is empty as the
            reference: after the property clears a two line text, the
            component must be exactly as tall as it was before anything
            was typed into it.
        """
        given : 'A component bound to a text property, with a fixed width so that its lines wrap. (See the `where` table for the current case!)'
            var component = build(text)
            component.setSize(200, 100)
        and : 'We remember how tall the component wants to be while it is empty.'
            int heightWhileEmpty = component.preferredSize.height

        when : 'The user types two lines into the component.'
            component.replaceSelection("Hello\nWorld")
        then : 'The component wants to be taller now.'
            component.preferredSize.height > heightWhileEmpty

        when : 'The view model clears the text.'
            text.set(From.VIEW_MODEL, "")
        then : 'The component wants to be exactly as tall as it was before anything was typed.'
            component.text == ""
            component.preferredSize.height == heightWhileEmpty

        where : 'We check two text components which build one view per line:'
            componentType       | text        | build
            'wrapping JTextArea'| Var.of("")  | { Var<String> t -> UI.textArea(t).peek(it -> it.setLineWrap(true)).get(JTextArea) }
            'JTextPane'         | Var.of("")  | { Var<String> t -> UI.textPane().withText(t).get(JTextPane) }
    }

    def 'A HTML `JEditorPane` bound to a `Val<String>` lays out the HTML which the property puts into it.'()
    {
        reportInfo """
            A `JEditorPane` with the content type "text/html" parses its text
            into a structured document, and Swing builds a view for every
            paragraph of that document. These views are what you actually
            see, so they have to be built for every page the property puts
            into the editor pane. Without them the editor pane would hold the
            right text, report the size of an empty page and paint nothing at all.
        """
        given : 'A property holding a HTML page, and a HTML editor pane bound to it.'
            Var<String> page = Var.of("")
            var editor = UI.editorPane()
                            .peek(it -> it.setContentType("text/html"))
                            .withText(page)
                            .get(JEditorPane)
            editor.setSize(300, 200)

        when : 'The property delivers a page with one paragraph.'
            page.set(From.VIEW_MODEL, "<html><body><p>Hello</p></body></html>")
            int heightOfOneParagraph = editor.preferredSize.height
        and : 'Then a page with two paragraphs.'
            page.set(From.VIEW_MODEL, "<html><body><p>Hello</p><p>World</p></body></html>")
            int heightOfTwoParagraphs = editor.preferredSize.height

        then : 'The editor pane holds the text of the second page...'
            editor.document.getText(0, editor.document.length).contains("World")
        and : '...and it wants to be taller for two paragraphs than for one.'
            heightOfTwoParagraphs > heightOfOneParagraph
    }

    def 'A `DocumentListener` of your own hears about text which a bound property puts into a text component.'()
    {
        reportInfo """
            SwingTree keeps its own listeners from reacting to text which a
            bound property puts into a text component, because they would
            only write that same text back into the property.
            Every other listener of the document is left alone:
            Swing's own listeners move the caret and rebuild the views,
            and a listener you add to the document yourself hears about the
            change just like it would for any other call to `setText`.

            The order of the listeners is left alone as well. A Swing document
            tells its newest listener first, so changing the order would change
            which listener sees a change before the others.
        """
        given : 'A list which our own listener leaves a trace in.'
            var trace = []
        and : 'A text property, and a text area bound to it, whose document gets a listener of our own.'
            Var<String> text = Var.of("Hello")
            var area = UI.textArea(text)
                        .peek(it -> it.document.addDocumentListener(new DocumentListener() {
                            @Override void insertUpdate(DocumentEvent e)  { trace << "insert" }
                            @Override void removeUpdate(DocumentEvent e)  { trace << "remove" }
                            @Override void changedUpdate(DocumentEvent e) { trace << "change" }
                        }))
                        .get(JTextArea)
        and : 'We remember which listeners the document has, in the order the document tells them.'
            var listenersBefore = (area.document as AbstractDocument).documentListeners.toList()

        when : 'The view model changes the text.'
            text.set(From.VIEW_MODEL, "World")

        then : 'Our listener heard the old text being removed and the new text being inserted.'
            trace == ["remove", "insert"]
        and : 'The document has the same listeners as before, in the same order.'
            (area.document as AbstractDocument).documentListeners.toList() == listenersBefore
    }

    def 'Text which a bound `Var<String>` puts into a text component is not reported back to the property, nor to `onTextChange`.'()
    {
        reportInfo """
            A text component bound to a `Var<String>` writes every change of
            its text into the property, and it shows every new value of the
            property. Text which came from the property must not travel back
            into it: the property would be told the same text again, or worse,
            the empty text in between, because Swing replaces a text by first
            removing the old text and then inserting the new one.
            For the same reason, the `onTextChange` handlers of the text
            component stay silent while the property sets the text.
            They react to the text the user types.
        """
        given : 'A list which the property and the `onTextChange` handler leave a trace in.'
            var trace = []
        and : 'A text property which records every change it is told about, and the channel the change came through.'
            Var<String> text = Var.of("Hello")
            text.onChange(From.ALL, it -> trace << "property ${it.channel()}: ${it.currentValue().orElseNull()}".toString())
        and : 'A text field bound to the property, with an `onTextChange` handler.'
            var field = UI.textField(text)
                         .onTextChange(it -> trace << "onTextChange: ${it.event.document.getText(0, it.event.document.length)}".toString())
                         .get(JTextField)

        when : 'The view model changes the text.'
            text.set(From.VIEW_MODEL, "World")
        then : 'The text field shows the new text...'
            field.text == "World"
        and : '...and the only trace is the change made by the view model.'
            trace == ["property VIEW_MODEL: World"]

        when : 'The user types a character at the end of the text, on the Swing thread, and we wait for the text to reach the property.'
            UI.runNow {
                field.caretPosition = field.text.length()
                field.replaceSelection("!")
            }
            UI.sync()
        then : 'Both the `onTextChange` handler and the property hear about it, the property through the view channel.'
            trace.drop(1).toSet() == ["onTextChange: World!", "property VIEW: World!"].toSet()
    }

    def 'We can bind to the `isEditable` flag of a text component.'()
    {
        given : 'We create a property representing the editable state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var ui =
                        UI.panel("fill, wrap 1")
                        .add(UI.button("Click Me!"))
                        .add(UI.textField("Hello World").isEditableIf(property))
                        .add(UI.textArea("Hello World").isEditableIfNot(property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The text field will be editable.'
            panel.components[1].editable == true
        and : 'The text area will be non-editable.'
            panel.components[2].editable == false

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The text field will be non-editable.'
            panel.components[1].editable == false
        and : 'The text area will be editable.'
            panel.components[2].editable == true
    }


    def 'We can bind to the `Font` property of a text component.'(
        float scalingFactor
    ) {
        given: 'We first initialise SwingTree using the given scaling factor'
            SwingTree.initializeUsing(it -> it.uiScaleFactor(scalingFactor))
        and : 'We create a property representing the font of a component.'
            Val<UI.Font> property = Var.of(UI.Font.of("Ubuntu", UI.FontStyle.PLAIN, 12))
            property.get().toAwtFont()
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.button("Click Me!"))
                        .add(UI.textField("Hello World").withFont(property))
                        .add(UI.textArea("Hello World").withFont(property.view(f->f.withStyle(UI.FontStyle.ITALIC))))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The text field will have the font of the property.'
            panel.components[1].font.toString() == new Font("Ubuntu", Font.PLAIN, Math.round(12 * scalingFactor) as int).toString()
        and : 'The text area will have the slightly derived font from the property.'
            panel.components[2].font.toString() == new Font("Ubuntu", Font.ITALIC, Math.round(12 * scalingFactor) as int).toString()

        when : 'We change the value of the property.'
            property.set(UI.Font.of("Buggie", UI.FontStyle.BOLD, 16))
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The text field will have the new font.'
            panel.components[1].font.toString() == new Font("Buggie", Font.BOLD, Math.round(16 * scalingFactor) as int).toString()
        and : 'The text area will again have the slightly derived font from the property.'
            panel.components[2].font.toString() == new Font("Buggie", Font.ITALIC, Math.round(16 * scalingFactor) as int).toString()
        where :
            scalingFactor << [1f, 1.25f, 1.5f, 1.75f, 2f]
    }

    def 'We can enable and disable a UI component dynamically through property binding.'()
    {
        given : 'We create a property representing the enabled state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a spinner!"))
                        .add(UI.spinner().isEnabledIf(property))
                        .add(UI.textArea("I am here for decoration..."))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The spinner will be enabled.'
            panel.components[1].enabled == true

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The spinner will be disabled.'
            panel.components[1].enabled == false
    }

    def 'We can select or unselect a UI component dynamically through properties.'()
    {
        given : 'We create a property representing the selected state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a checkbox!"))
                        .add(UI.checkBox("I am a checkbox").isSelectedIf(property))
                        .add(UI.textArea("I am here for decoration..."))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'The checkbox will be selected.'
           panel.components[1].selected == true

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The checkbox will be unselected.'
            panel.components[1].selected == false
    }

    def 'Enable or disable the split items of a JSplitButton through properties.'()
    {
        given : 'We create a property representing the enabled state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.splitButton("I am a split button")
                            .add(UI.splitItem("I am a button").isEnabledIf(property))
                            .add(UI.splitItem("I am a button"))
                            .add(UI.splitItem("I am a button"))
        and : 'We build the component:'
            var splitButton = ui.get(JSplitButton)

        expect : 'The first split item will be enabled.'
            splitButton.popupMenu.components[0].enabled == true

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The first split item will be disabled.'
            splitButton.popupMenu.components[0].enabled == false
    }

    def 'The visibility of a UI component can be modelled dynamically using boolean properties.'()
    {
        given : 'We create a property representing the visibility state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a spinner!"))
                        .add(UI.spinner().isVisibleIf(property))
                        .add(UI.textArea("I am here for decoration..."))
                        .add(UI.slider(UI.Axis.VERTICAL).isVisibleIfNot(property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'The spinner will be visible.'
            panel.components[1].visible == true
        and : 'The slider will be invisible.'
            panel.components[3].visible == false

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The spinner will be invisible.'
            panel.components[1].visible == false
        and : 'The slider will be visible.'
            panel.components[3].visible == true
    }

    def 'The visibility of a UI component can be modelled using an enum property.'()
    {
        reportInfo """
            Enums are a common tool for modelling user choices and settings in you view models
            because they are type safe and descriptive. 
            A common use case is to have certain UI components only visible if a certain enum value
            in your view model is selected.

            For this example, we will use the following enum:
            ```
                enum Accept { YES, NO, MAYBE }
            ```
        """
        given : 'We create a property representing the visibility state of a component.'
            Var<Accept> property = Var.of(Accept.YES)
        and : 'We create a UI with the enum property based binding.'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("If you accept the terms we can proceed!"))
                        .add(UI.button("Yes proceed!").isVisibleIf(Accept.YES, property))
                        .add(UI.label("Maybe or No is not enough :/").isVisibleIfNot(Accept.YES, property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'Initially the bound button will be visible.'
            panel.components[1].visible == true
        and : 'The bound label will be invisible.'
            panel.components[2].visible == false

        when : 'We change the value of the property.'
            property.set(Accept.NO)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The bound button will be invisible.'
            panel.components[1].visible == false
        and : 'The bound label will be visible.'
            panel.components[2].visible == true
    }

    def 'The enabled/disabled state of a UI component can be modelled using an enum property.'()
    {
        reportInfo """
            Enums are a common tool for modelling user choices and settings in you view models
            because they are type safe and descriptive. 
            A common use case is to have certain UI components only enabled if a certain enum value
            in your view model is selected.
            For this example, we will use the following enum:
            ```
                enum Accept { YES, NO, MAYBE }
            ```
        """
        given : 'We create a property representing the enabled state of a component.'
            Var<Accept> property = Var.of(Accept.YES)
        and : 'We create a UI with the enum property based binding.'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("If you accept the terms we can proceed!"))
                        .add(UI.button("Yes proceed!").isEnabledIf(Accept.YES, property))
                        .add(UI.label("Maybe or No is not enough :/").isEnabledIfNot(Accept.YES, property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'Initially the bound button will be enabled.'
            panel.components[1].enabled == true
        and : 'The bound label will be disabled.'
            panel.components[2].enabled == false

        when : 'We change the value of the property.'
            property.set(Accept.NO)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The bound button will be disabled.'
            panel.components[1].enabled == false
        and : 'The bound label will be enabled.'
            panel.components[2].enabled == true
    }

    def 'The focusability of a UI component can be modelled using an enum property.'()
    {
        reportInfo """
            Enums are a common tool for modelling user choices and settings in you view models
            because they are descriptive and type safe (not like string values). 
            A common use case is to have certain UI components only focused if a certain enum value
            in your view model is selected.
            For this example, we will use the following enum:
            ```
                enum Accept { YES, NO, MAYBE }
            ```
        """
        given : 'We create a property representing the focusability state of a component.'
            Var<Accept> property = Var.of(Accept.YES)
        and : 'We create a UI with the enum property based binding.'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("If you accept the terms we can proceed!"))
                        .add(UI.button("Yes proceed!").isFocusableIf(Accept.YES, property))
                        .add(UI.label("Maybe or No is not enough :/").isFocusableIfNot(Accept.YES, property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'Initially the bound button will be focusable.'
            panel.components[1].focusable == true
        and : 'The bound label will be unfocusable.'
            panel.components[2].focusable == false

        when : 'We change the value of the property.'
            property.set(Accept.NO)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The bound button will be unfocusable.'
            panel.components[1].focusable == false
        and : 'The bound label will be focusable.'
            panel.components[2].focusable == true
    }

    def 'The focusability of a UI component can be modelled dynamically using boolean properties.'()
    {
        given : 'We create a property representing the focusability state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a spinner!"))
                        .add(UI.spinner().isFocusableIf(property))
                        .add(UI.textArea("I am here for decoration..."))
                        .add(UI.slider(UI.Axis.VERTICAL).isFocusableIfNot(property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'The spinner will be focusable.'
            panel.components[1].focusable == true
        and : 'The slider will be unfocusable.'
            panel.components[3].focusable == false

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The spinner will be unfocusable.'
            panel.components[1].focusable == false
        and : 'The slider will be focusable.'
            panel.components[3].focusable == true
    }

    def 'Minimum as well as maximum height of UI components can be modelled using integer properties.'( int uiScale )
    {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a property representing the minimum and maximum height.'
            Val<Integer> property = Var.of(50)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a text area!"))
                        .add(UI.textArea("hi").withMinHeight(property))
                        .add(UI.label("Below me is another text area!"))
                        .add(UI.textArea("Hey").withMaxHeight(property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The minimum height of the first text area will be 50 * uiScale.'
            panel.components[1].minimumSize.height == 50 * uiScale
        and : 'The maximum height of the second text area will be 50 * uiScale.'
            panel.components[3].maximumSize.height == 50 * uiScale

        when : 'We change the value of the property.'
            property.set(100)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The minimum height of the first text area will be 100 * uiScale.'
            panel.components[1].minimumSize.height == 100 * uiScale
        and : 'The maximum height of the second text area will be 100 * uiScale.'
            panel.components[3].maximumSize.height == 100 * uiScale

        where : """
            We use the following integer scaling factors simulating different high DPI scenarios.
            Note that usually the UI is scaled by 1, 1.5 an 2 (for 4k screens for example).
            A scaling factor of 3 is rather unusual, however it is possible to scale it by 3 nonetheless.
        """ 
            uiScale << [3, 2, 1]
    }


    def 'The width and height of UI components can be modelled using integer properties.'( int uiScale )
    {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a property representing the width and height.'
            Val<Integer> widthProperty = Var.of(50)
            Val<Integer> heightProperty = Var.of(100)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a text area!"))
                        .add(UI.textArea("hi").withWidth(widthProperty).withHeight(heightProperty))
                        .add(UI.label("Below me is another text area!"))
                        .add(UI.textArea("Hey").withWidth(heightProperty).withHeight(widthProperty))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The width of the first text area will be 50 * uiScale.'
            panel.components[1].size.width == 50 * uiScale
        and : 'The height of the first text area will be 100 * uiScale.'
            panel.components[1].size.height == 100 * uiScale
        and : 'The width of the second text area will be 100 * uiScale.'
            panel.components[3].size.width == 100 * uiScale
        and : 'The height of the second text area will be 50 * uiScale.'
            panel.components[3].size.height == 50 * uiScale

        when : 'We change the value of the property.'
            widthProperty.set(100)
            heightProperty.set(50)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The dimensions of both UI components will be as expected.'
            panel.components[1].size.width == 100 * uiScale
            panel.components[1].size.height == 50 * uiScale
            panel.components[3].size.width == 50 * uiScale
            panel.components[3].size.height == 100 * uiScale

        where : """
            We use the following integer scaling factors simulating different high DPI scenarios.
            Note that usually the UI is scaled by 1, 1.5 an 2 (for 4k screens for example).
            A scaling factor of 3 is rather unusual, however it is possible to scale it by 3 nonetheless.
        """ 
            uiScale << [3, 2, 1]
    }

    def 'Bind the foreground of a component to a conditional property and 2 color properties.'()
    {
        reportInfo """
            A common use case is to have a foreground color switching between two colors depending on a condition.
            This can be achieved by using properties for the condition and the colors.
            If any of these change in the view model, the UI component will be updated accordingly.
        """
        given : 'We create 3 properties, 1 boolean one and 2 color properties.'
            Val<Boolean> conditionProperty = Var.of(true)
            Val<Color>   color1Property    = Var.of(Color.RED)
            Val<Color>   color2Property    = Var.of(Color.BLUE)
        and : 'We create a UI to which we want to bind:'
            var ui =
                        UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a text area!"))
                        .add(UI.textArea("hi").withForegroundIf(conditionProperty, color1Property, color2Property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : """
                The foreground of the text area will be red, because the condition is true, 
                meaning the first color is selected!
            """
            panel.components[1].foreground == Color.RED

        when : 'We change the value of the condition property.'
            conditionProperty.set(false)
            UI.sync() // Wait for the EDT to complete the UI modifications...
        then : 'The foreground of the text area will now switch to blue, because the condition is false.'
            panel.components[1].foreground == Color.BLUE

        when : 'We change the value of the color properties.'
            color1Property.set(Color.GREEN)
            color2Property.set(Color.YELLOW)
            UI.sync() // Wait for the EDT to complete the UI modifications...
        then : 'The foreground of the text area will be yellow, because the condition is false.'
            panel.components[1].foreground == Color.YELLOW

        when : 'We change the value of the condition property.'
            conditionProperty.set(true)
            UI.sync() // Wait for the EDT to complete the UI modifications...
        then : 'The foreground of the text area will be green, because the condition is true.'
            panel.components[1].foreground == Color.GREEN
    }

}
