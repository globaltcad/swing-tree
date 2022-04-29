package com.globaltcad.swingtree.splitbutton

import com.alexandriasoftware.swing.JSplitButton
import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.utility.Utility
import groovy.transform.CompileDynamic
import spock.lang.Specification
import spock.lang.Title

import javax.swing.*

@CompileDynamic
@Title("A Button of Buttons")
class JSplitButton_Examples_Spec extends Specification
{
    def 'The most simple kind of split button can be built like so:'()
    {
        given : 'We create a basic split button which will not have any behaviour.'
            var ui =
                UI.splitButton("I am displayed on the button!")
                .add(UI.splitItem("I am the first drop down item."))
                .add(UI.splitItem("I am the second item."))
                .add(UI.splitItem("And I am the third."))

        expect : 'The component wrapped by the UI builder is in fact a split button:'
            ui.component instanceof JSplitButton
        and : 'This button should have a popup menu with 3 components.'
            ui.popupMenu.components.length == 3
        and : 'They have the expected names:'
            ((JMenuItem)ui.popupMenu.getComponent(0)).getText() == "I am the first drop down item."
            ((JMenuItem)ui.popupMenu.getComponent(1)).getText() == "I am the second item."
            ((JMenuItem)ui.popupMenu.getComponent(2)).getText() == "And I am the third."
    }

    def 'We can easily build a split button whose text becomes the current user selection:'()
    {
        given : 'We create split button displaying the current selection.'
            var ui =
                UI.splitButton("I will be replaced!")
                .onSelection( it -> it.delegate.displayCurrentItemText() )
                .add(UI.splitItem("first"))
                .add(UI.splitItem("second"))
                .add(UI.splitItem("third"))
        expect : 'The split button has the correct text displayed'
            Utility.getSplitButtonText(ui) == "I will be replaced!"

        when :
            ((JMenuItem)ui.popupMenu.getComponent(1)).doClick()
        then :
            Utility.getSplitButtonText(ui) == "second"
    }

    def 'We can easily build a split button where only one item text will have its text displayed:'()
    {
        given : 'We create split button displaying only selection "second".'
            var ui =
                UI.splitButton("I may be replaced!")
                .add(UI.splitItem("first"))
                .add(UI.splitItem("second").onSelection( it -> it.delegate.displayCurrentItemText() ))
                .add(UI.splitItem("third"))
        expect : 'The split button has the correct text displayed'
            Utility.getSplitButtonText(ui) == "I may be replaced!"

        when : 'We simulate a user selecting the first button item'
            ((JMenuItem)ui.popupMenu.getComponent(0)).doClick()
        then : 'The displayed button should still be as it was previously.'
            Utility.getSplitButtonText(ui) == "I may be replaced!"

        when : 'We now select the third button item'
            ((JMenuItem)ui.popupMenu.getComponent(2)).doClick()
        then : 'Again the button text is still the same.'
            Utility.getSplitButtonText(ui) == "I may be replaced!"

        when : 'We now simulate a selection on the second item (for which we registered an action).'
            ((JMenuItem)ui.popupMenu.getComponent(1)).doClick()
        then : 'The displayed button text will have changed because of our selection lambda'
            Utility.getSplitButtonText(ui) == "second"
    }


    def 'We can register button click events for button items as well as the split button as a whole.'()
    {
        given : 'We create split button with 2 different kinds of button click events.'
            var ui =
                UI.splitButton("I may be replaced!")
                .onSelection( it -> it.delegate.displayButtonText("default text")  )
                .add(UI.splitItem("first"))
                .add(UI.splitItem("second").onButtonClick( it -> it.delegate.displayButtonText("text by second item") ))
                .add(UI.splitItem("third"))
        expect : 'The split button has the correct text displayed'
            Utility.getSplitButtonText(ui) == "I may be replaced!"

        when : 'We select the first item and click the button.'
            ((JMenuItem)ui.popupMenu.getComponent(0)).doClick()
            Utility.click(ui)
        then : 'We expect that the button has the default text displayed according to the first action'
            Utility.getSplitButtonText(ui) == "default text"

        when : 'We select and click the third button.'
            ((JMenuItem)ui.popupMenu.getComponent(2)).doClick()
            Utility.click(ui)
        then : 'This should have the same effect as previously.'
            Utility.getSplitButtonText(ui) == "default text"

        when : 'We now select and click the second button item.'
            ((JMenuItem)ui.popupMenu.getComponent(1)).doClick()
            Utility.click(ui)
        then : 'The split button text will be different because the button item action fired last.'
            Utility.getSplitButtonText(ui) == "text by second item"
    }


}
