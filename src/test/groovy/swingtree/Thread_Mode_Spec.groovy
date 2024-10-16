package swingtree


import sprouts.Action
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.threading.EventProcessor

import javax.swing.*
import java.awt.event.ActionEvent

@Title("Thread Modes")
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
    
''')
class Thread_Mode_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
        // We will override the processor in the tests where we need to.
    }

    def 'We can use the decoupled thread mode to queue backend events.'()
    {
        reportInfo """
            Note that Swing-Tree will not create a new thread for you,
            you must process the events yourself using the "UI.processEvents()" method
            preferably by a custom thread or the main thread of your application.
        """
        given: 'A UI built with the decoupled thread mode.'
            var eventWasHandled = false
            var ui =
                    UI.use(EventProcessor.DECOUPLED,
                        ()-> UI.button("Click Me")
                                .onClick({ eventWasHandled = true })
                    )
        when: 'We click the button.'
            UI.runNow(()->ui.get(JButton).doClick())
        then: 'The event is queued up, waiting to be handled.'
            !eventWasHandled

        when : """
                We process the event queue, which in a real world
                application would be done by a custom thread, or the main thread
                of your application (everything but your GUI thread really).
            """
            EventProcessor.DECOUPLED.joinUntilDoneOrException()
            UI.sync()
        then: 'The event is handled.'
            eventWasHandled
    }

    def 'The default "coupled" thread mode will use the AWT thread for event handling.'()
    {
        reportInfo """
            This is the default thread mode, which means that
            the explicit specification of 
            `UI.use(EventProcessor.COUPLED, ...)` is not required.
            However, note that in this thread mode the UI will be blocked 
            until the event is handled. 
        """
        given: 'A UI built with the coupled thread mode.'
            var eventWasHandled = false
            var ui =
                    UI.use(EventProcessor.COUPLED, ()->
                        UI.button("Click Me")
                        .onClick({ eventWasHandled = true })
                    )
        and : 'Then we build the component.'
            var button = ui.get(JButton)
        when: 'We click the button.'
            button.doClick()
        then: 'The event is handled immediately by the swing thread.'
            eventWasHandled
    }

    def 'Inside an event lambda we can not access the UI from a background thread.'(
            String problem,
            Action<ComponentDelegate<JButton, ActionEvent>> unsafeAccess,
            Action<ComponentDelegate<JButton, ActionEvent>> safeAccess
    ) {
        reportInfo """
                The event delegate (which is passed to things like `onClick` actions) 
                throws an exception if you try to access the UI from a background thread.
                This is because SwingTree can not guarantee that the UI is thread safe.
                However, the delegate provides safe method which will execute the
                passed lambda on the event dispatch thread.
            """
        given: 'Two custom exception tracer lists:'
            var trace1 = []
            var trace2 = []
        and : '2 UIs built with the decoupled thread mode, one error prone and the other one safe.'
            var ui1 =
                    UI.use(EventProcessor.DECOUPLED, ()->
                        UI.button("X").onClick(it -> {
                            try {
                                unsafeAccess.accept(it)
                            } catch (Exception e) {
                                trace1 << e
                            }
                        })
                    )
            var ui2 =
                    UI.use(EventProcessor.DECOUPLED, ()->
                        UI.button("X").onClick({
                            try {
                                safeAccess.accept(it)
                            } catch (Exception e) {
                                trace2 << e
                            }
                        })
                    )
        when : 'We click the button and process the event queue (by this current non-swing thread).'
            UI.runNow( () -> ui1.get(JButton).doClick() )
            EventProcessor.DECOUPLED.joinUntilDoneOrException() // This is done by a custom thread in a real world application.

        then: 'The delegate throws an exception!'
            trace1.size() == 1
            trace1[0].message.contains(problem)

        when : 'We click the button second button and then process the event queue (by this current non-swing thread).'
            UI.runNow( () -> ui2.get(JButton).doClick() )
            EventProcessor.DECOUPLED.joinUntilDoneOrException() // This is done by a custom thread in a real world application.
        then: 'The delegate does not throw an exception!'
            trace2.isEmpty()

        where : 'We can use safe, as well as unsafe, ways to access the UI.'
            problem                                    | unsafeAccess                         | safeAccess
            "can only be accessed by the Swing thread" | { it.getComponent() }                | { it.forComponent(c -> {}) }
            "can only access the component from the"   | { it.get() }                         | { it.forComponent(c -> {}) }
            "can only be accessed by the Swing thread" | { it.getSiblings() }                 | { it.forSiblings(s -> {}) }
            "can only be accessed by the Swing thread" | { it.getSiblinghood() }              | { it.forSiblinghood(c -> {}) }
            "can only be accessed by the Swing thread" | { it.getSiblingsOfType(JButton) }    | { it.forSiblingsOfType(JButton, c -> {}) }
            "can only be accessed by the Swing thread" | { it.getSiblinghoodOfType(JButton) } | { it.forSiblinghoodOfType(JButton, c -> {}) }
    }

    def 'The application thread can safely effect the state of the UI by using the "UI.run(()->{..})" method.'()
    {
        given : 'A UI built with the decoupled thread mode.'
            var ui =
                    UI.use(EventProcessor.DECOUPLED, ()->
                        UI.checkBox("X").onClick( it ->{
                            UI.run(()-> it.get() )
                            // it.get() // This would throw an exception!
                        })
                    )
        when : 'We check the check box and process the event queue (by this current non-swing thread).'
            UI.runNow( () -> ui.get(JCheckBox).doClick() )
            EventProcessor.DECOUPLED.joinFor(1) // This is done by a custom thread in a real world application.
        then: 'The delegate does not throw an exception!'
            noExceptionThrown()
    }

    def 'Accessing the UI from something other than the UI thread, leads to an exception when in decoupled thread mode.'()
    {
        reportInfo """
            The UI you build in the decoupled thread mode will try to protect itself
            from being accessed from a potential application thread (so anything but the UI thread). 
            So if you try to access the UI from another thread, an exception will be thrown.
        """
        given : 'A UI built with the decoupled thread mode:'
            var ui = UI.use(EventProcessor.DECOUPLED, ()-> UI.panel())
        when : 'We try to access the component by this current non-swing thread.'
            var panel = ui.get(JPanel)
        then: 'An exception is thrown!'
            var e = thrown(Exception)
            e.message == "This UI is configured to be decoupled from the application thread, " +
                         "which means that it can only be modified from the EDT. " +
                         "Please use 'UI.run(()->...)' method to execute your modifications on the EDT."

        when : 'We want to still access the component from this current non-swing thread, we can use the "UI.run(()->{..})" method.'
            UI.run(()-> ui.get(JPanel) )
        and : 'Alternatively we can also use "UI.runAndGet(()->{..})".'
            panel = UI.runAndGet(()-> ui.get(JPanel) )
            panel.updateUI()
        then: 'No exception is thrown!'
            noExceptionThrown()
    }

}
