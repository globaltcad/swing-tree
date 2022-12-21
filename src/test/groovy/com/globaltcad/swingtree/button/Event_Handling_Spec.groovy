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

}
