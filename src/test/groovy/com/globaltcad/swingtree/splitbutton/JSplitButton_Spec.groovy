package com.globaltcad.swingtree.splitbutton

import com.alexandriasoftware.swing.JSplitButton
import com.globaltcad.swingtree.UI
import spock.lang.Specification

import javax.swing.*

class JSplitButton_Spec extends Specification
{

    def 'We can build a JSplitButton.'()
    {
        given : 'We create a UI builder node containing a simple split button.'
            var node = UI.splitButton("")
        expect : 'It wraps a JSplitButton.'
            node.component instanceof JSplitButton

        when :
            node.add(new JMenuItem("First"))
        then :
            node.popupMenu.components.length == 1

        when :
            node.add(UI.menuItem("Second"))
        then :
            node.popupMenu.components.length == 2

        when :
            node.add(UI.splitItem("Third"))
        then :
            node.popupMenu.components.length == 3

        when :
            node.add(UI.splitRadioItem("Fourth"))
        then :
            node.popupMenu.components.length == 4
        and :
            node.popupMenu.components.findAll({it instanceof JRadioButtonMenuItem}).size() == 1
            node.popupMenu.components.findAll({it instanceof JMenuItem}).size() == 4
    }

}
