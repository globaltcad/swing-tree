package com.globaltcad.swingtree

import com.alexandriasoftware.swing.JSplitButton
import spock.lang.Specification

import javax.swing.*

class Split_Button_Spec extends Specification
{

    def 'We can build a JSplitButton.'()
    {
        given : 'We create a UI builder node containing a simple split button.'
            var node = UI.splitButton("")
        expect : 'It wraps a JSplitButton.'
            node.component instanceof JSplitButton

        when :
            node.withItem(new JMenuItem("First"))
        then :
            node.popupMenu.components.length == 1


        when :
            node.withItem(UI.menuItemSaying("Second"))
        then :
            node.popupMenu.components.length == 2
    }


    def 'A JSplitButton does not accept null actions.'()
    {
        given : 'We create a UI builder node containing a simple split button.'
            var node = UI.splitButton("")
        expect : 'It wraps a JSplitButton.'
            node.component instanceof JSplitButton

        when :
            node.onSplitClick(null)
        then :
            thrown(IllegalArgumentException)

        when :
            node.onClick(null)
        then :
            thrown(IllegalArgumentException)

        when :
            node.onChange(null)
        then :
            thrown(IllegalArgumentException)
    }

}
