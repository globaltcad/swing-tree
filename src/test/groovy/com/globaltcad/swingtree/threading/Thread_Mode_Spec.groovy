package com.globaltcad.swingtree.threading

import com.globaltcad.swingtree.ThreadMode
import com.globaltcad.swingtree.UI
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title


@Title("Thread Modes")
@Narrative('''

    Swing only knows 1 thread, the Event Dispatch Thread (EDT)m
    which performs both the UIs rendering as well as event handling.
    This is a problem for Swing applications that need to perform
    long running tasks in the background, as the EDT will be blocked
    until the task is complete.
    SwingTree provides a mechanism for creating UIs which 
    dispatch events to a custom thread as well as ensure that all UI related 
    operations are performed on the event dispatch thread!
    This allows SwingTree applications to perform long running tasks
    in the background without blocking the UI.
    
''')
class Thread_Mode_Spec extends Specification
{
    def 'We can use the decoupled thread mode to queue backend events.'()
    {
        reportInfo """
            Note that Swing-Tree will not create a new thread for you,
            you must process the events yourself using the "UI.processEvents()" method
            preferably by a custom thread or the main thread of your application.
        """

        given: 'A UI built with the decoupled thread mode.'
            var eventWasHandled = false
            var node =
                    UI.use(ThreadMode.DECOUPLED,
                        ()-> UI.button("Click Me")
                                .onClick({ eventWasHandled = true })
                    )
        when: 'We click the button.'
            node.component.doClick()
        then: 'The event is queued up, waiting to be handled.'
            !eventWasHandled

        when : """
                We process the event queue, which in a real world
                application would be done by a custom thread, or the main thread
                of your application (everything but your GUI thread really).
            """
            UI.processEvents()
        then: 'The event is handled.'
            eventWasHandled
    }

    def 'The default "coupled" thread mode will use the AWT thread for event handling.'()
    {
        reportInfo """
            This is the default thread mode, which means that
            the explicit specification of 
            `UI.use(ThreadMode.COUPLED, ...)` is not required.
            However, note that in this thread mode the UI will be blocked 
            until the event is handled. 
        """
        given: 'A UI built with the coupled thread mode.'
            var eventWasHandled = false
            var node =
                    UI.use(ThreadMode.COUPLED,
                        ()-> UI.button("Click Me")
                                .onClick({ eventWasHandled = true })
                    )
        when: 'We click the button.'
            node.component.doClick()
        then: 'The event is handled immediately by the swing thread.'
            eventWasHandled
    }

}
