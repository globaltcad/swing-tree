package com.globaltcad.swingtree.button

import com.globaltcad.swingtree.UI
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

@Title("Registering Event Handlers")
@Narrative('''
        
    In this specification you can see how to register different kinds of event handlers
    on Swing components.
        
''')
class Event_Handling_Spec extends Specification
{
    def 'The "onChange" event handlers are triggered in the same order as they were registered.'()
    {
        given : 'A simple list where handlers are going to leave a trace.'
            var record = []

        and : 'A simple radio button UI.'
            var ui =
                    UI.radioButton("CLICK ME")
                    .onChange( it -> record.add("1") )
                    .onChange( it -> record.add("2") )
                    .onChange( it -> record.add("3") )
                    .onChange( it -> record.add("4") )
                    .onChange( it -> record.add("5") )
                    .onChange( it -> record.add("6") )
                    .onChange( it -> record.add("7") )

        when : 'The button is clicked.'
            ui.component.doClick()

        then : 'The handlers are triggered in the same order as they were registered.'
            record == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'The "onClick" event handlers are triggered in the same order as they were registered.'()
    {
        given : 'A simple list where handlers are going to leave a trace.'
            var record = []

        and : 'A simple button UI.'
            var ui =
                    UI.button("CLICK ME")
                    .onClick( it -> record.add("1") )
                    .onClick( it -> record.add("2") )
                    .onClick( it -> record.add("3") )
                    .onClick( it -> record.add("4") )
                    .onClick( it -> record.add("5") )
                    .onClick( it -> record.add("6") )
                    .onClick( it -> record.add("7") )

        when : 'The button is clicked.'
            ui.component.doClick()

        then : 'The handlers are triggered in the same order as they were registered.'
            record == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'The "onResize" event handlers are triggered in the same order as they were registered.'()
    {
        given : 'A simple list where handlers are going to leave a trace.'
            var record = []

        and : 'A simple text field UI.'
            var ui =
                    UI.formattedTextField("Some content...")
                    .onResize( it -> record.add("1") )
                    .onResize( it -> record.add("2") )
                    .onResize( it -> record.add("3") )
                    .onResize( it -> record.add("4") )
                    .onResize( it -> record.add("5") )
                    .onResize( it -> record.add("6") )
                    .onResize( it -> record.add("7") )

        when : 'The text field is resized.'
            ui.component.setSize(100, 100)
            UI.sync()

        then : 'The handlers are triggered in the same order as they were registered.'
            record == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'The "onShown" event handlers are triggered in the same order as they were registered.'()
    {
        given : 'A simple list where handlers are going to leave a trace.'
            var record = []

        and : 'A simple text area UI.'
            var ui =
                    UI.textArea("Some content...")
                    .onShown( it -> record.add("1") )
                    .onShown( it -> record.add("2") )
                    .onShown( it -> record.add("3") )
                    .onShown( it -> record.add("4") )
                    .onShown( it -> record.add("5") )
                    .onShown( it -> record.add("6") )
                    .onShown( it -> record.add("7") )

        when : 'The text area is set to visible.'
            ui.component.setVisible(true)
            UI.sync()
        then : 'Nothing happens because the text area is already shown.'
            record == []

        when : 'The text area is set to invisible and then visible again.'
            ui.component.setVisible(false)
            ui.component.setVisible(true)
            UI.sync()
        then : 'The handlers are triggered in the same order as they were registered.'
            record == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'The "onEnter" event handlers are triggered in the same order as they were registered.'()
    {
        given : 'A simple list where handlers are going to leave a trace.'
            var record = []

        and : 'A simple text field UI.'
            var ui =
                    UI.textField("Some content...")
                    .onEnter( it -> record.add("1") )
                    .onEnter( it -> record.add("2") )
                    .onEnter( it -> record.add("3") )
                    .onEnter( it -> record.add("4") )
                    .onEnter( it -> record.add("5") )
                    .onEnter( it -> record.add("6") )
                    .onEnter( it -> record.add("7") )

        when : 'Something is entered in the text field.'
            ui.component.setText("Some other content...")
            // We trigger the "enter" event by simulating an enter key press (which causes the action listener to be triggered).
            ui.component.postActionEvent()
            UI.sync()

        then : 'The handlers are triggered in the same order as they were registered.'
            record == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'In a formatted text field, the "onEnter" event handlers are triggered in the same order as they were registered.'()
    {
        given : 'A simple list where handlers are going to leave a trace.'
            var record = []

        and : 'A simple text field UI.'
            var ui =
                    UI.formattedTextField("Some content...")
                    .onEnter( it -> record.add("1") )
                    .onEnter( it -> record.add("2") )
                    .onEnter( it -> record.add("3") )
                    .onEnter( it -> record.add("4") )
                    .onEnter( it -> record.add("5") )
                    .onEnter( it -> record.add("6") )
                    .onEnter( it -> record.add("7") )

        when : 'Something is entered in the text field.'
            ui.component.setText("Some other content...")
            // We trigger the "enter" event by simulating an enter key press (which causes the action listener to be triggered).
            ui.component.postActionEvent()
            UI.sync()

        then : 'The handlers are triggered in the same order as they were registered.'
            record == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'The "onSelection" event handlers of a combo box are triggered in the same order as they were registered.'()
    {
        given : 'A simple list where handlers are going to leave a trace.'
            var record = []

        and : 'A simple combo box UI.'
            var ui =
                    UI.comboBox("Item 1", "Item 2", "Item 3")
                    .onSelection( it -> record.add("1") )
                    .onSelection( it -> record.add("2") )
                    .onSelection( it -> record.add("3") )
                    .onSelection( it -> record.add("4") )
                    .onSelection( it -> record.add("5") )
                    .onSelection( it -> record.add("6") )
                    .onSelection( it -> record.add("7") )

        when : 'An item is selected in the combo box.'
            ui.component.setSelectedIndex(1)
            UI.sync()

        then : 'The handlers are triggered in the same order as they were registered.'
            record == ["1", "2", "3", "4", "5", "6", "7"]
    }

    def 'The "onContentChange" event handler of a text area is triggered when the text area content changes.'()
    {
        given : 'A simple text area UI.'
            var ui = UI.textArea("Some content...")

        and : 'A simple list where handlers are going to leave a trace.'
            var record = []

        and : 'A handler that records the text area content.'
            ui.onContentChange( it -> record.add(it.component.text) )

        when : 'The text area content is changed.'
            UI.runNow { ui.component.setText("Some other content...") }
            UI.sync()

        then : 'The handler is triggered.'
            record == ["", "Some other content..."]
    }

    def 'The "onTextChange" event handler of a text area is triggered when the text area content changes.'()
    {
        given : 'A simple text area UI.'
            var ui = UI.textArea("Some content...")

        and : 'A simple list where handlers are going to leave a trace.'
            var record = []

        and : 'A handler that records the text area content.'
            ui.onTextChange( it -> record.add(it.component.text) )

        when : 'The text area content is changed.'
            UI.runNow { ui.component.setText("Some other content...") }
            UI.sync()

        then : 'The handler is triggered.'
            record == ["", "Some other content..."]
    }

    def 'The "onTextInsert" event handler of a text area is triggered when the text area content changes.'()
    {
        given : 'A simple text area UI.'
            var ui = UI.textArea("Some content...")

        and : 'A simple list where handlers are going to leave a trace.'
            var record = []

        and : 'A handler that records the text area content.'
            ui.onTextInsert( it -> record.add(it.component.text) )

        when : 'The text area content is changed.'
            UI.runNow { ui.component.setText("Some other content...") }
            UI.sync()

        then : 'The handler is not triggered.'
            record == []
    }

    def 'The "onTextRemove" event handler of a text area is triggered when the text area content changes.'()
    {
        given : 'A simple text area UI.'
            var ui = UI.textArea("Some content...")

        and : 'A simple list where handlers are going to leave a trace.'
            var record = []

        and : 'A handler that records the text area content.'
            ui.onTextRemove( it -> record.add(it.component.text) )

        when : 'The text area content is changed.'
            UI.runNow { ui.component.setText("Some other content...") }
            UI.sync()

        then : 'The handler is not triggered.'
            record == []
    }

    def 'The "onTextReplace" event handler of a text area is triggered when the text area content changes.'()
    {
        given : 'A simple text area UI.'
            var ui = UI.textArea("Some content...")

        and : 'A simple list where handlers are going to leave a trace.'
            var record = []

        and : 'A handler that records the text area content.'
            ui.onTextReplace( it -> record.add(it.component.text) )

        when : 'The text area content is changed.'
            UI.runNow { ui.component.setText("Some other content...") }
            UI.sync()

        then : 'The handler was triggered.'
            record == ["Some content..."]
    }

    def 'The "onTextReplace" event handlers will be triggered in the correct order.'()
    {
        given : 'A simple text area UI.'
            var ui = UI.textArea("Some content...")

        and : 'A simple list where handlers are going to leave a trace.'
            var record = []

        and : 'A handler that records the text area content.'
            ui.onTextReplace( it -> record.add("1") )
              .onTextReplace( it -> record.add("2") )
              .onTextReplace( it -> record.add("3") )
              .onTextReplace( it -> record.add("4") )
              .onTextReplace( it -> record.add("5") )
              .onTextReplace( it -> record.add("6") )
              .onTextReplace( it -> record.add("7") )

        when : 'The text area content is changed.'
            UI.runNow { ui.component.setText("Some other content...") }
            UI.sync()

        then : 'The handlers are triggered in the correct order.'
            record == ["1", "2", "3", "4", "5", "6", "7"]
    }

}
