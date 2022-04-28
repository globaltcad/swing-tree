package com.globaltcad.swingtree.examples

import com.alexandriasoftware.swing.JSplitButton
import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.utility.Utility
import groovy.transform.CompileDynamic
import spock.lang.Specification

import javax.swing.*

@CompileDynamic
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
            ((JMenuItem)ui.popupMenu.components[0]).getText() == "I am the first drop down item."
            ((JMenuItem)ui.popupMenu.components[1]).getText() == "I am the second item."
            ((JMenuItem)ui.popupMenu.components[2]).getText() == "And I am the third."
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
        expect :
            Utility.getSplitButtonText(ui) == "I will be replaced!"

        when :
            ((JMenuItem)ui.popupMenu.getComponents()[1]).doClick()
        then :
            Utility.getSplitButtonText(ui) == "second"
    }

}
