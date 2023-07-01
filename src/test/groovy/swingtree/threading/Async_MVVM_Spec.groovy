package swingtree.threading

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.EventProcessor
import swingtree.UI

import javax.swing.JLabel

@Title("Asynchronous MVVM")
@Narrative('''

    Swing only knows 1 thread, the Event Dispatch Thread (EDT)
    which performs both the UIs rendering as well as event handling.
    This is a problem for Swing applications that need to perform
    long running tasks in the background, as the EDT will be blocked
    until the task is complete.
    
    SwingTree provides a mechanism for creating UIs which 
    dispatch events to a custom application thread as well as ensure that all UI related 
    operations are performed on the event dispatch thread!
    Not only does this allow your applications to perform long running tasks
    in the background without blocking the UI, it improves
    the performance and responsiveness of you desktop application as a whole.
    
    This specification demonstrates how this feature interacts with the MVVM pattern
    and the binding of properties to UI components.
    
''')
@Subject([UI, EventProcessor, Var])
class Async_MVVM_Spec extends Specification
{
    def setupSpec() {
        UI.SETTINGS().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
        // We will override the processor in the tests where we need to.
    }

    def 'The order in which the states of properties change is preserved when applied to the UI.'()
    {
        reportInfo """
            MVVM in Swing-Tree is based on the observability built into the Sprouts property objects.
            However, in a multi-threaded/asynchronous environment, the order in which the states of properties
            change is not guaranteed to be preserved when applied to the UI.
            
            Here a little explanation is in order:
            Consider a property that is bound to a UI component, and that property is changed by the application thread.
            This causes the property to notify its observers so that they can update the state of the UI component.
            Because Swing-tree is asynchronous, the app thread will put the change procedure 
            in the event queue of the EDT, and the EDT will ultimately apply the change to the UI.
            However, it might take a while before the EDT gets around to processing the event
            and until then the property might have changed again!
            In fact, the state of the property can change multiple times before the EDT
            gets a chance to apply them to the UI. 
            The problem that might arise is that the sequence of changes in the UI is not the same as the sequence
            in which the changes were made to the property.
            
            Swing-Tree provides a mechanism for ensuring that the order in which the states of properties
            change is preserved when applied to the UI.
        """
        given : 'We create a simple property storing a String.'
            var property = Var.of("Hello")
        and : 'A mocked JLabel, which we will use to verify the order in which the states of the property are applied to the UI.'
            var label = Mock(JLabel)
        and : 'We create a Swing thread decoupled UI component that is bound to the property.'
            var node = UI.use(EventProcessor.DECOUPLED,
                                       ()-> UI.of(label).withText(property)
                                       )
        when : 'We simply change the property once...'
            property.set("Hi")
        and : 'Sync with the AWT thread...'
            UI.sync()
        then : 'As expected, the UI component is changed to the new value of the property.'
            1 * label.setText("Hi")

        when : 'We do 4 changes in a row...'
            property.set("Hello")
            property.set("Hi")
            property.set("Hey")
            property.set("Hi")
        and : 'Sync with the AWT thread again...'
            UI.sync()
        then : 'All changes will be applied to the UI in the order in which they were made to the property.'
            1 * label.setText("Hello")
            1 * label.setText("Hi")
            1 * label.setText("Hey")
            1 * label.setText("Hi")
    }
}
