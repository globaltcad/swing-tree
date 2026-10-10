package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.HasId
import sprouts.Tuple
import sprouts.Var
import swingtree.components.JBox
import swingtree.components.JScrollPanels
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.*
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.List
import java.util.concurrent.atomic.AtomicInteger

@Title("The Scroll Position of a JScrollPanels List")
@Narrative("""

    A `JScrollPanels` list bound to a property holding a tuple, through
    `addAll(Var<Tuple<M>>, BoundViewSupplier)`, changes its rows whenever the tuple in
    the property changes. A chat view is built this way: one row per message. When the
    user has scrolled the list, a change of the tuple must not move the scroll position,
    unless the list became too short for the scroll position.

    Two terms are used throughout the scenarios below:

      - **The scroll position** of a list is the y coordinate of the view position of the
        `JViewport` inside the list (`list.getViewport().getViewPosition().y`): the number
        of pixels of the column of rows which lie above the visible part of the list.
      - **One task on Swing's event thread**: Swing runs everything on a single thread, one
        queued task after the other. When something calls `revalidate()`, Swing's
        `RepaintManager` queues one task which first lays out the invalid components (through
        `RepaintManager.validateInvalidComponents()`) and then paints. Whatever the layout in
        that task decides is what the user sees in the paint of that task.

    Every layout of the viewport runs Swing's `ViewportLayout`, which keeps the visible
    part of the list inside the column of rows: when the scroll position plus the height of
    the visible part is more than the height of the column, `ViewportLayout` lowers the
    scroll position to the height of the column minus the height of the visible part, or
    to 0. A layout which sees the column shorter than it really is therefore moves the
    scroll position up, and a later layout which sees the full column does not move the
    scroll position back down.

    The scenarios check that the scroll position stays where the user left it:

      - when the property gets a new tuple which is not derived from the previous tuple,
        while the list is scrolled to the middle,
      - when the property gets a new tuple which is not derived from the previous tuple,
        while the list is scrolled to the end,
      - and when the last message of the tuple is replaced by a new message, through
        `Tuple.setAt`, while the list is scrolled to the end.

""")
@Subject([JScrollPanels, UI.ScrollPane])
class Scroll_Panels_Scroll_Position_Spec extends Specification
{
    static record Message(String id, String text) implements HasId<String> {}

    def setup() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setUiScaleFactor(1f)
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
    }

    def cleanup() {
        SwingTree.clear()
    }

    def 'A new tuple which is not derived from the previous tuple keeps the scroll position in the middle of a `JScrollPanels` list.'()
    {
        reportInfo """
            A `JScrollPanels` list shows twenty messages, one box per message, in a window of
            400 by 300 pixels. The user has scrolled the list to the scroll position 600, in
            the middle of the column of boxes.

            Then the application puts a new tuple into the property, which holds the same
            twenty messages and a twenty-first message at the end, for example because the
            application built the tuple again from its own data after a message arrived. A tuple
            made by `Tuple.of(...)` carries no record of how it differs from the previous
            tuple, so the list replaces all its rows: it removes every row and adds a row for
            every message of the new tuple. The box of a message whose id is in both tuples is
            reused, so every row keeps its height.

            If the list laid out its viewport right after removing every row, `ViewportLayout`
            would see an empty column of height 0 and set the scroll position to 0. The rows
            added afterwards make the column taller than before, but the scroll position would
            stay at 0: the user would see the list jump to the first message.

            The list lays out its viewport once, after all rows are added, so the scroll
            position stays at 600.
        """
        given : """
            About 240 characters of text, and a closure which builds a box with 8 pixels of
            padding on every side that shows a text wrapped into lines from its top left corner,
            with the height of the wrapped text as the preferred height of the box
            (`autoPreferredHeight`).
        """
            var text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor. " * 3
            var newTextBox = { String content ->
                UI.box()
                .withStyle(it -> it
                    .padding(8)
                    .text(t -> t
                        .content(content)
                        .placement(UI.Placement.TOP_LEFT)
                        .wrapLines(true)
                        .autoPreferredHeight(true)
                    )
                )
                .get(JBox)
            }
        and : """
            A property holding a tuple of twenty messages with the ids "1" to "20", and a
            `JScrollPanels` list bound to the property, whose content follows the width of the
            list, in a window of 400 by 300 pixels.
        """
            List<Message> twentyMessages = (1..20).collect { new Message("$it", "$it: $text") }
            var messages = Var.of(Tuple.of(Message, twentyMessages))
            JScrollPanels list =
                    UI.scrollPanels(conf -> conf.fitWidth(true))
                    .addAll(messages, message -> UI.of(newTextBox(message.get().text())))
                    .get(JScrollPanels)
            var frame = new JFrame()
            UI.runNow {
                frame.setContentPane(list)
                frame.setSize(400, 300)
            }
            showAndWaitUntilTheWindowHasSettled(frame)
        and : 'The list is scrolled to the scroll position 600.'
            UI.runNow { list.getViewport().setViewPosition(new Point(0, 600)) }
            int columnHeightBefore = UI.runAndGet({ list.getViewport().getView().getHeight() })

        when : """
            We put a new tuple, made by `Tuple.of(...)`, into the property, which holds the same
            twenty messages and a twenty-first message with the id "21". In the same task on
            Swing's event thread, we then call `RepaintManager.validateInvalidComponents()`,
            which is the layout Swing runs right before it paints, and read the scroll position
            and the height of the column right after that call.
        """
            Point positionAfter = null
            int columnHeightAfter = 0
            UI.runNow {
                messages.set(Tuple.of(Message, twentyMessages + [new Message("21", "21: $text")]))
                RepaintManager.currentManager(list).validateInvalidComponents()
                positionAfter = list.getViewport().getViewPosition()
                columnHeightAfter = list.getViewport().getView().getHeight()
            }

        then : 'The column of boxes is taller than before the new tuple, because the column now has a box for the twenty-first message.'
            columnHeightAfter > columnHeightBefore
        and : 'The scroll position of the list is still 600.'
            positionAfter.y == 600

        cleanup :
            UI.runNow { frame.dispose() }
    }

    def 'A new tuple which is not derived from the previous tuple keeps a `JScrollPanels` list scrolled to its end.'()
    {
        reportInfo """
            A `JScrollPanels` list shows twenty messages, one box per message, in a window of
            400 by 300 pixels. The user has scrolled the list to its end, so the last message
            is at the bottom of the visible part of the list, as in a chat.

            Then the application puts a new tuple into the property, made by `Tuple.of(...)`,
            in which the last message is replaced by an edited message with a new id and a text
            of the same length. A tuple made by `Tuple.of(...)` carries no record of how it
            differs from the previous tuple, so the list removes every row and adds a row for
            every message of the new tuple. The boxes of the first nineteen messages are reused
            and keep their heights. The box of the edited message is new.

            Two layouts could move the scroll position up here:

            1. A layout of the viewport right after removing every row. `ViewportLayout` would
               see an empty column of height 0 and set the scroll position to 0.
            2. The first layout of the column with the new box. A box which was just built has
               no width yet, so SwingTree cannot measure the text of the new box before the
               layout gives the new box its width, and the layout gives the new box the height
               of its padding, 16 pixels. The column is then shorter than its real height in
               that layout, and `ViewportLayout` lowers the scroll position by the same amount.
               SwingTree measures the text of the new box during that layout, and the scroll
               pane lays out its content a second time, which gives the new box its text height.

            The list lays out its viewport once, after all rows are added, and before the second
            layout the scroll pane puts the scroll position back where it was before the first
            layout. So the list stays scrolled to its end.
        """
        given : """
            About 240 characters of text, and a closure which builds a box with 8 pixels of
            padding on every side that shows a text wrapped into lines from its top left corner,
            with the height of the wrapped text as the preferred height of the box
            (`autoPreferredHeight`).
        """
            var text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor. " * 3
            var newTextBox = { String content ->
                UI.box()
                .withStyle(it -> it
                    .padding(8)
                    .text(t -> t
                        .content(content)
                        .placement(UI.Placement.TOP_LEFT)
                        .wrapLines(true)
                        .autoPreferredHeight(true)
                    )
                )
                .get(JBox)
            }
        and : """
            A property holding a tuple of twenty messages with the ids "1" to "20", and a
            `JScrollPanels` list bound to the property, whose content follows the width of the
            list, in a window of 400 by 300 pixels.
        """
            List<Message> twentyMessages = (1..20).collect { new Message("$it", "$it: $text") }
            var messages = Var.of(Tuple.of(Message, twentyMessages))
            JScrollPanels list =
                    UI.scrollPanels(conf -> conf.fitWidth(true))
                    .addAll(messages, message -> UI.of(newTextBox(message.get().text())))
                    .get(JScrollPanels)
            var frame = new JFrame()
            UI.runNow {
                frame.setContentPane(list)
                frame.setSize(400, 300)
            }
            showAndWaitUntilTheWindowHasSettled(frame)
        and : """
            The list is scrolled to its end: the scroll position is the height of the column of
            boxes minus the height of the visible part of the list.
        """
            int endPosition = UI.runAndGet({
                JViewport viewport = list.getViewport()
                int end = viewport.getView().getHeight() - viewport.getExtentSize().height
                viewport.setViewPosition(new Point(0, end))
                end
            })
            int columnHeightBefore = UI.runAndGet({ list.getViewport().getView().getHeight() })

        when : """
            We put a new tuple, made by `Tuple.of(...)`, into the property, which holds the first
            nineteen messages and, instead of the twentieth message, a message with the new id
            "20 edited" and a text of the same length. In the same task on Swing's event thread,
            we then call `RepaintManager.validateInvalidComponents()`, which is the layout Swing
            runs right before it paints, and read the scroll position and the height of the
            column right after that call.
        """
            List<Message> editedMessages = twentyMessages.subList(0, 19) + [new Message("20 edited", "20: $text")]
            Point positionAfter = null
            int columnHeightAfter = 0
            UI.runNow {
                messages.set(Tuple.of(Message, editedMessages))
                RepaintManager.currentManager(list).validateInvalidComponents()
                positionAfter = list.getViewport().getViewPosition()
                columnHeightAfter = list.getViewport().getView().getHeight()
            }

        then : 'The column of boxes is as tall as before the new tuple, because the edited message has a text of the same length.'
            columnHeightAfter == columnHeightBefore
        and : 'The scroll position of the list is still the end position, so the edited message is at the bottom of the visible part of the list.'
            endPosition > 1000
            positionAfter.y == endPosition

        cleanup :
            UI.runNow { frame.dispose() }
    }

    def 'Replacing the last message through `Tuple.setAt` keeps a `JScrollPanels` list scrolled to its end.'()
    {
        reportInfo """
            A `JScrollPanels` list shows twenty messages, one box per message, in a window of
            400 by 300 pixels. The user has scrolled the list to its end, so the last message
            is at the bottom of the visible part of the list, as in a chat.

            Then the application replaces the last message with an edited message with a new
            id and a text of the same length, through `tuple.setAt(19, editedMessage)`. A tuple
            made by `setAt` records which position changed, so the list replaces only the last
            row, with a new box for the edited message.

            A box which was just built has no width yet, so SwingTree cannot measure the text of
            the new box before the layout gives the new box its width. The first layout gives
            the new box the height of its padding, 16 pixels, so in that layout the column of
            boxes is shorter than its real height, and `ViewportLayout` lowers the scroll
            position by the same amount. SwingTree measures the text of the new box during that
            layout, and the scroll pane lays out its content a second time, which gives the new
            box its text height. If the scroll pane did not put the scroll position back before
            the second layout, the list would stay scrolled up by the difference, and the edited
            message would be cut off at the bottom of the visible part of the list.

            Before the second layout, the scroll pane puts the scroll position back where it was
            before the first layout, so `ViewportLayout` checks the scroll position against the
            real height of the column, and the list stays scrolled to its end.
        """
        given : """
            About 240 characters of text, and a closure which builds a box with 8 pixels of
            padding on every side that shows a text wrapped into lines from its top left corner,
            with the height of the wrapped text as the preferred height of the box
            (`autoPreferredHeight`).
        """
            var text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor. " * 3
            var newTextBox = { String content ->
                UI.box()
                .withStyle(it -> it
                    .padding(8)
                    .text(t -> t
                        .content(content)
                        .placement(UI.Placement.TOP_LEFT)
                        .wrapLines(true)
                        .autoPreferredHeight(true)
                    )
                )
                .get(JBox)
            }
        and : """
            A property holding a tuple of twenty messages with the ids "1" to "20", and a
            `JScrollPanels` list bound to the property, whose content follows the width of the
            list, in a window of 400 by 300 pixels.
        """
            List<Message> twentyMessages = (1..20).collect { new Message("$it", "$it: $text") }
            var messages = Var.of(Tuple.of(Message, twentyMessages))
            JScrollPanels list =
                    UI.scrollPanels(conf -> conf.fitWidth(true))
                    .addAll(messages, message -> UI.of(newTextBox(message.get().text())))
                    .get(JScrollPanels)
            var frame = new JFrame()
            UI.runNow {
                frame.setContentPane(list)
                frame.setSize(400, 300)
            }
            showAndWaitUntilTheWindowHasSettled(frame)
        and : """
            The list is scrolled to its end: the scroll position is the height of the column of
            boxes minus the height of the visible part of the list.
        """
            int endPosition = UI.runAndGet({
                JViewport viewport = list.getViewport()
                int end = viewport.getView().getHeight() - viewport.getExtentSize().height
                viewport.setViewPosition(new Point(0, end))
                end
            })
            int columnHeightBefore = UI.runAndGet({ list.getViewport().getView().getHeight() })

        when : """
            We replace the twentieth message of the tuple in the property, through
            `tuple.setAt(19, ...)`, with a message with the new id "20 edited" and a text of the
            same length. In the same task on Swing's event thread, we then call
            `RepaintManager.validateInvalidComponents()`, which is the layout Swing runs right
            before it paints, and read the scroll position and the height of the column right
            after that call.
        """
            Point positionAfter = null
            int columnHeightAfter = 0
            UI.runNow {
                messages.update(tuple -> tuple.setAt(19, new Message("20 edited", "20: $text")))
                RepaintManager.currentManager(list).validateInvalidComponents()
                positionAfter = list.getViewport().getViewPosition()
                columnHeightAfter = list.getViewport().getView().getHeight()
            }

        then : 'The column of boxes is as tall as before the edit, because the edited message has a text of the same length.'
            columnHeightAfter == columnHeightBefore
        and : 'The scroll position of the list is still the end position, so the edited message is at the bottom of the visible part of the list.'
            endPosition > 1000
            positionAfter.y == endPosition

        cleanup :
            UI.runNow { frame.dispose() }
    }

    private static void showAndWaitUntilTheWindowHasSettled( JFrame frame ) {
        var windowEvents = new AtomicInteger(0)
        var countWindowEvents = new ComponentAdapter() {
            @Override void componentResized( ComponentEvent e ) { windowEvents.incrementAndGet() }
            @Override void componentMoved( ComponentEvent e ) { windowEvents.incrementAndGet() }
        }
        UI.runNow {
            frame.addComponentListener(countWindowEvents)
            frame.setVisible(true)
            frame.validate()
        }
        int quietRounds = 0
        int eventsSeen = -1
        for ( int round = 0; round < 60 && quietRounds < 4; round++ ) {
            Thread.sleep(50)
            int eventsNow = UI.runAndGet({ windowEvents.get() })
            quietRounds = eventsNow == eventsSeen ? quietRounds + 1 : 0
            eventsSeen = eventsNow
        }
        UI.runNow {
            frame.removeComponentListener(countWindowEvents)
            frame.validate()
        }
        3.times {
            UI.runNow { RepaintManager.currentManager(frame).validateInvalidComponents() }
        }
        UI.runNow { frame.validate() }
    }
}
