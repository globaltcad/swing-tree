package swingtree.mvvm

import swingtree.SwingTree
import swingtree.threading.EventProcessor
import swingtree.UI
import sprouts.Var
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

import javax.swing.ScrollPaneConstants

@Title("Scroll Pane Binding")
@Narrative('''

    Although a scroll pane does not hold information
    that is relevant to your core business logic, it certainly has certain properties
    relevant to the view and its usability. 
    For example, the scroll pane can be enabled or disabled, or it
    can have certain kinds of scroll policies.
    This specifications shows how to bind `Val` properties to scroll panes.
    
''')
class Scroll_Pane_Binding_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def 'A property holding a vertical scroll bar policy can be bound to the UI.'()
    {
        reportInfo """
            Not only should a view model contain state relevant for your business logic
            but it should also contain state relevant for your view and how it should
            behave and look like depending on the state of the view model
            and your business logic.
            This is why Swing-Tree allows you to bind properties to the scroll
            bar policy of your scroll panes.
        """
        given : 'A property holding a vertical scroll bar policy.'
            var policy = Var.of(UI.Active.NEVER)
        when : 'We create a scroll panel and bind the property to it.'
            var ui = UI.scrollPane().withVerticalScrollBarPolicy(policy)
        then : 'The property was successfully bound to the UI.'
            ui != null
        when : 'We change and show the property value.'
            policy.set(UI.Active.ALWAYS)
            UI.sync()
        then : 'The UI was updated.'
            ui.component.verticalScrollBarPolicy == ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
    }

    def 'You can model the horizontal scroll bar policy us ing a view model property dynamically.'()
    {
        reportInfo """
            Not only should a view model contain state relevant for your business logic
            but it should also contain state relevant for your view and how it should
            behave and look like depending on the state of the view model
            and your business logic.
            This is why Swing-Tree allows you to bind properties to the scroll
            bar policy of your scroll panes.
        """
        given : 'A property holding a horizontal scroll bar policy.'
            var policy = Var.of(UI.Active.ALWAYS)
        when : 'We create a scroll panel and bind the property to it.'
            var ui = UI.scrollPane().withHorizontalScrollBarPolicy(policy)
        then : 'The property was successfully bound to the UI.'
            ui != null
        when : 'We change and show the property value.'
            policy.set(UI.Active.AS_NEEDED)
            UI.sync()
        then : 'The UI was updated.'
            ui.component.horizontalScrollBarPolicy == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    }
}
