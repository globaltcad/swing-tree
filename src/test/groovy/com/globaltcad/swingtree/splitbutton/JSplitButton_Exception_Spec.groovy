package com.globaltcad.swingtree.splitbutton

import com.alexandriasoftware.swing.JSplitButton
import com.globaltcad.swingtree.UI
import spock.lang.Specification

class JSplitButton_Exception_Spec extends Specification
{
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

        when :
            node.onButtonClick(null)
        then :
            thrown(IllegalArgumentException)

        when :
            node.onSelection(null)
        then :
            thrown(IllegalArgumentException)

        when :
            node.onSplitClick(null)
        then :
            thrown(IllegalArgumentException)
    }

    def 'Split buttons do not take null as an answer.'()
    {
        when : 'We create an invalid button state.'
            UI.splitButton("I will be replaced!")
            .onSelection(null)
        then :
            thrown(IllegalArgumentException)
        when : 'We commit some nonsense another way:'
            UI.splitButton("item").onButtonClick(null)
        then :
            thrown(IllegalArgumentException)
    }

    def 'Split button items do not take null as an answer.'()
    {
        when : 'We create an invalid button item state.'
            UI.splitItem("second").onSelection(null)
        then :
            thrown(IllegalArgumentException)
        when : 'We commit some nonsense another way:'
            UI.splitItem("item").onButtonClick(null)
        then :
            thrown(IllegalArgumentException)
    }

}
