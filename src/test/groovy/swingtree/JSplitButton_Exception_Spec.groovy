package swingtree

import spock.lang.Specification
import swingtree.components.JSplitButton
import swingtree.threading.EventProcessor

class JSplitButton_Exception_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def 'A JSplitButton does not accept null actions.'()
    {
        reportInfo """
            Here you can see that Swing-Tree tries to be as null-safe as possible.
            Because a 'null-action' is an undefined state, which may mean
            different things to different developers, so it is not allowed.
        """
        given : 'We create a UI builder node containing a simple split button.'
            var ui = UI.splitButton("")
        expect : 'It wraps a JSplitButton.'
            ui.get(JSplitButton) instanceof JSplitButton

        when :
            ui.onSplitClick(null)
        then :
            thrown(IllegalArgumentException)

        when :
            ui.onClick(null)
        then :
            thrown(IllegalArgumentException)

        when :
            ui.onChange(null)
        then :
            thrown(IllegalArgumentException)

        when :
            ui.onButtonClick(null)
        then :
            thrown(IllegalArgumentException)

        when :
            ui.onSelection(null)
        then :
            thrown(IllegalArgumentException)

        when :
            ui.onSplitClick(null)
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
