package swingtree.tabs

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.SwingTree
import swingtree.threading.EventProcessor
import swingtree.UI
import swingtree.UIForTabbedPane

import javax.swing.JTabbedPane
import java.awt.Rectangle
import java.awt.event.MouseEvent

@Title("Tab Event Handling")
@Narrative('''

    When building a tabbed pane using SwingTree, you often want to react to
    events that occur on the tabs of a `JTabbedPane`. 
    For example, you may want trigger some action when a tab is selected.
    
    In this specification, we will see how to handle events on tabs.

''')
@Subject([UIForTabbedPane])
class Tab_Event_Handling_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def 'Use the "onTabMouseClick" to receive mouse click events on tabs.'()
    {
        reportInfo """
            This action event not only tells you when a user clicks on a tab,
            the delegate received by the event handler also tells you which tab
            was clicked and how many times it was clicked.
        """
        given : 'A list we use to record some event information.'
            var trace = []
        and : 'A tabbed pane with 3 tabs.'
            JTabbedPane ui =
                            UI.tabbedPane().withSize(280,120)
                            .onTabMouseClick( it -> trace << "T${it.tabIndex()+1}, clicked ${it.clickCount()} times" )
                            .add(UI.tab("T1"))
                            .add(UI.tab("T2"))
                            .add(UI.tab("T3"))
                            .get(JTabbedPane)
        and : 'We get the bounding boxes of the 3 tab buttons.'
            Rectangle tab1Bounds = ui.getBoundsAt(0)
            Rectangle tab2Bounds = ui.getBoundsAt(1)
            Rectangle tab3Bounds = ui.getBoundsAt(2)

        when : 'We simulate a mouse click in the middle of the second tab.'
            int x = tab2Bounds.x + tab2Bounds.width / 2
            int y = tab2Bounds.y + tab2Bounds.height / 2
            var event = new MouseEvent(ui, MouseEvent.MOUSE_CLICKED, 0, 0, x, y, 1, false)
            ui.dispatchEvent(event)
        and : 'We wait for the AWT thread to do its thing.'
            UI.sync()

        then : 'The event handler is called.'
            trace == ["T2, clicked 1 times"]

        when : 'We do the same with the other 2 tabs.'
            x = tab1Bounds.x + tab1Bounds.width / 2
            y = tab1Bounds.y + tab1Bounds.height / 2
            event = new MouseEvent(ui, MouseEvent.MOUSE_CLICKED, 0, 0, x, y, 2, false)
            ui.dispatchEvent(event)
            x = tab3Bounds.x + tab3Bounds.width / 2
            y = tab3Bounds.y + tab3Bounds.height / 2
            event = new MouseEvent(ui, MouseEvent.MOUSE_CLICKED, 0, 0, x, y, 42, false)
            ui.dispatchEvent(event)
            UI.sync()

        then : 'The event handler is called for each tab.'
            trace == ["T2, clicked 1 times", "T1, clicked 2 times", "T3, clicked 42 times"]
    }

    def 'The "onTabMousePress" event handler is called when the mouse is pressed on a tab.'()
    {
        reportInfo """
            This action event is essentially the first part of a mouse click event.
            It tells you when a user presses the mouse button on a tab.
            So this will be called before the "onTabMouseClick" event handler
            and the "onTabMouseRelease" event handler.
        """
        given : 'A list we use to record some event information.'
            var trace = []
        and : 'A tabbed pane with 2 tabs.'
            JTabbedPane ui =
                            UI.tabbedPane().withSize(280,120)
                            .onTabMousePress( it -> trace << "T${it.tabIndex()+1}, pressed" )
                            .add(UI.tab("T1"))
                            .add(UI.tab("T2"))
                            .get(JTabbedPane)
        and : 'We get the bounding boxes of the 2 tab buttons.'
            Rectangle tab1Bounds = ui.getBoundsAt(0)
            Rectangle tab2Bounds = ui.getBoundsAt(1)

        when : 'We simulate a mouse press in the middle of the second tab.'
            int x = tab2Bounds.x + tab2Bounds.width / 2
            int y = tab2Bounds.y + tab2Bounds.height / 2
            var event = new MouseEvent(ui, MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false)
            ui.dispatchEvent(event)
        and : 'We wait for the AWT thread to do its thing.'
            UI.sync()

        then : 'The event handler is called.'
            trace == ["T2, pressed"]

        when : 'We do the same with the other tab.'
            x = tab1Bounds.x + tab1Bounds.width / 2
            y = tab1Bounds.y + tab1Bounds.height / 2
            event = new MouseEvent(ui, MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 2, false)
            ui.dispatchEvent(event)
            UI.sync()

        then : 'The event handler is called for each tab.'
            trace == ["T2, pressed", "T1, pressed"]
    }

    def 'The "onTabMouseRelease" event handler can be used to notice when the mouse press is released on a tab.'()
    {
        reportInfo """
            This action event is essentially the last part of a whole mouse click event.
            It tells you when a user releases the mouse button on a tab (after pressing it).
            So this will be called after the "onTabMousePress" event handler.
        """
        given : 'A list we use to record some event information.'
            var trace = []
        and : 'A tabbed pane with 2 tabs.'
            JTabbedPane ui =
                            UI.tabbedPane().withSize(280,120)
                            .onTabMouseRelease( it -> trace << "T${it.tabIndex()+1}, released" )
                            .add(UI.tab("T1"))
                            .add(UI.tab("T2"))
                            .get(JTabbedPane)
        and : 'We get the bounding boxes of the 2 tab buttons.'
            Rectangle tab1Bounds = ui.getBoundsAt(0)
            Rectangle tab2Bounds = ui.getBoundsAt(1)

        when : 'We simulate a mouse release in the middle of the second tab.'
            int x = tab2Bounds.x + tab2Bounds.width / 2
            int y = tab2Bounds.y + tab2Bounds.height / 2
            var event = new MouseEvent(ui, MouseEvent.MOUSE_RELEASED, 0, 0, x, y, 1, false)
            ui.dispatchEvent(event)
        and : 'We wait for the AWT thread to do its thing.'
            UI.sync()

        then : 'The event handler is called.'
            trace == ["T2, released"]

        when : 'We do the same with the other tab.'
            x = tab1Bounds.x + tab1Bounds.width / 2
            y = tab1Bounds.y + tab1Bounds.height / 2
            event = new MouseEvent(ui, MouseEvent.MOUSE_RELEASED, 0, 0, x, y, 2, false)
            ui.dispatchEvent(event)
            UI.sync()

        then : 'The event handler is called for each tab.'
            trace == ["T2, released", "T1, released"]
    }

    def 'Notice when the mouse enters a tab using the "onTabMouseEnter" event handler.'()
    {
        reportInfo """
            This action event is called when the mouse enters a tab.
            It is called before the "onTabMouseExit" event handler.
        """
        given : 'A list we use to record some event information.'
            var trace = []
        and : 'A tabbed pane with 2 tabs.'
            JTabbedPane ui =
                            UI.tabbedPane().withSize(280,120)
                            .onTabMouseEnter( it -> trace << "T${it.tabIndex()+1}, entered" )
                            .add(UI.tab("T1"))
                            .add(UI.tab("T2"))
                            .get(JTabbedPane)
        and : 'We get the bounding boxes of the 2 tab buttons.'
            Rectangle tab1Bounds = ui.getBoundsAt(0)
            Rectangle tab2Bounds = ui.getBoundsAt(1)

        when : 'We simulate a mouse enter in the middle of the second tab.'
            int x = tab2Bounds.x + tab2Bounds.width / 2
            int y = tab2Bounds.y + tab2Bounds.height / 2
            var event = new MouseEvent(ui, MouseEvent.MOUSE_ENTERED, 0, 0, x, y, 1, false)
            ui.dispatchEvent(event)
        and : 'We wait for the AWT thread to do its thing.'
            UI.sync()

        then : 'The event handler is called.'
            trace == ["T2, entered"]

        when : 'We do the same with the other tab.'
            x = tab1Bounds.x + tab1Bounds.width / 2
            y = tab1Bounds.y + tab1Bounds.height / 2
            event = new MouseEvent(ui, MouseEvent.MOUSE_ENTERED, 0, 0, x, y, 2, false)
            ui.dispatchEvent(event)
            UI.sync()

        then : 'The event handler is called for each tab.'
            trace == ["T2, entered", "T1, entered"]
    }

    def 'Notice when the mouse exits a tab using the "onTabMouseExit" event handler.'()
    {
        reportInfo """
            This action event is called when the mouse exits a tab.
            It is called after the "onTabMouseEnter" event handler.
        """
        given : 'A list we use to record some event information.'
            var trace = []
        and : 'A tabbed pane with 2 tabs.'
            JTabbedPane ui =
                            UI.tabbedPane().withSize(280,120)
                            .onTabMouseExit( it -> trace << "T${it.tabIndex()+1}, exited" )
                            .add(UI.tab("T1"))
                            .add(UI.tab("T2"))
                            .get(JTabbedPane)
        and : 'We get the bounding boxes of the 2 tab buttons.'
            Rectangle tab1Bounds = ui.getBoundsAt(0)
            Rectangle tab2Bounds = ui.getBoundsAt(1)

        when : 'We simulate a mouse exit in the middle of the second tab.'
            int x = tab2Bounds.x + tab2Bounds.width / 2
            int y = tab2Bounds.y + tab2Bounds.height / 2
            var event = new MouseEvent(ui, MouseEvent.MOUSE_EXITED, 0, 0, x, y, 1, false)
            ui.dispatchEvent(event)
        and : 'We wait for the AWT thread to do its thing.'
            UI.sync()

        then : 'The event handler is called.'
            trace == ["T2, exited"]

        when : 'We do the same with the other tab.'
            x = tab1Bounds.x + tab1Bounds.width / 2
            y = tab1Bounds.y + tab1Bounds.height / 2
            event = new MouseEvent(ui, MouseEvent.MOUSE_EXITED, 0, 0, x, y, 2, false)
            ui.dispatchEvent(event)
            UI.sync()

        then : 'The event handler is called for each tab.'
            trace == ["T2, exited", "T1, exited"]
    }
}
