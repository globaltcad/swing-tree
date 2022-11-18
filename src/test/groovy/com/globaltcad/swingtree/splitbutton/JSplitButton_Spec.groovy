package com.globaltcad.swingtree.splitbutton

import com.alexandriasoftware.swing.JSplitButton
import com.globaltcad.swingtree.UI
import spock.lang.Specification

import javax.swing.*

class JSplitButton_Spec extends Specification
{

    def 'We can build a JSplitButton and add components to it.'()
    {
        given : 'We create a UI builder node containing a simple split button.'
            var ui = UI.splitButton("")
        expect : 'It wraps a JSplitButton.'
            ui.component instanceof JSplitButton

        when : 'We add a menu item to the split button.'
            ui.add(new JMenuItem("First"))
        then : 'The split button has a popup menu with one component.'
            ui.popupMenu.components.length == 1

        when : 'We add another menu item to the split button.'
            ui.add(UI.menuItem("Second"))
        then : 'The split button has a popup menu with two components.'
            ui.popupMenu.components.length == 2

        when : 'We add a split item to the split button.'
            ui.add(UI.splitItem("Third"))
        then : 'The split button has a popup menu with three components.'
            ui.popupMenu.components.length == 3

        when : 'We add a split radio button to the split button.'
            ui.add(UI.splitRadioItem("Fourth"))
        then : 'The split button has a popup menu with four components.'
            ui.popupMenu.components.length == 4
        and :
            ui.popupMenu.components.findAll({it instanceof JRadioButtonMenuItem}).size() == 1
            ui.popupMenu.components.findAll({it instanceof JMenuItem}).size() == 4
    }

}
