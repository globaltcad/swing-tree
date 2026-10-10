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
import swingtree.style.TextConf
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.*
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.image.BufferedImage
import java.util.List
import java.util.concurrent.atomic.AtomicInteger

@Title("Text Heights in a Layout")
@Narrative("""

    `TextConf.autoPreferredHeight(true)` makes the preferred height of a component the height
    of its wrapped text at the width the component has. A narrower component needs more lines
    for its text, so its preferred height changes whenever its width changes. A layout must
    therefore give each such component the preferred height which belongs to the width the
    layout gives the component.

    Swing has no built-in way for a layout to ask a component for its preferred height at a
    given width. A layout reads the preferred heights first, then sets the widths. So SwingTree
    measures the text of a component again during the layout, as soon as the layout has given
    the component a new width. When the measured height differs from the height the layout
    used, SwingTree has the layout run again with the new preferred height:

      - Inside a SwingTree scroll pane (`UI.ScrollPane`, which `JScrollPanels` extends), the
        scroll pane lays out its content a second time before its own layout is finished.
        Swing paints only after that, so a box is painted at the height of its text right away.
      - Outside a SwingTree scroll pane, SwingTree calls `revalidate()` on the component in a
        later task on Swing's event thread. Swing paints before that task runs, so a visible
        box is painted once at its old height, and at its new height after the layout which
        that later `revalidate()` call requests.

    Only SwingTree's own component classes, which implement `StylableComponent`, measure their
    text during the layout. A plain Swing component styled through `UI.of(...)`, for example a
    `JPanel`, measures its text only when its style is computed, which happens when it is
    painted or when a property its style is bound to changes.

    Two terms are used throughout the scenarios below:

      - **One task on Swing's event thread**: Swing runs everything on a single thread, one
        queued task after the other. When something calls `revalidate()`, Swing's
        `RepaintManager` queues one task which first lays out the invalid components (through
        `RepaintManager.validateInvalidComponents()`) and then paints. Whatever the layout in
        that task decides is what the user sees in the paint of that task.
      - **The text height of a box**: the preferred height SwingTree computes for the text of
        the box at the current width of the box. A scenario gets the text height by measuring
        a separate box with the same text and font at that width, see `textHeightOf`.

    The scenarios check that boxes have their text height:

      - when a box is added outside the visible part of a scroll pane,
      - when a component inside the window makes a scroll pane narrower,
      - when a box is added to a `JScrollPanels` list,
      - when the window becomes narrower,
      - for boxes outside any scroll pane, after the layout which SwingTree's later
        `revalidate()` call requests,
      - and, for a box inside a plain `JScrollPane` inside a SwingTree scroll pane, after the
        layout which SwingTree's later `revalidate()` call requests.

""")
@Subject([TextConf, UI.ScrollPane, JScrollPanels])
class Text_Height_Layout_Spec extends Specification
{
    /** About 480 characters, which wrap into several lines in a box that is a few hundred pixels wide. */
    private static final String TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor. " * 6

    static record Message(String id, String text) implements HasId<String> {}

    def setup() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setUiScaleFactor(1f)
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
    }

    def cleanup() {
        SwingTree.clear()
    }

    def 'A box added outside the visible part of a scroll pane gets the height of its text.'()
    {
        reportInfo """
            A scroll pane shows a column of eight boxes. Each box has 8 pixels of padding on
            every side and a long text which wraps into several lines. The window is only 300
            pixels tall, so most boxes lie outside the visible part of the scroll pane.

            A ninth box is added at the bottom of the column, far outside the visible part of
            the scroll pane. Swing never paints a component which is not visible. If SwingTree
            measured the text of the ninth box only while painting the ninth box, the ninth box
            would keep the height of its padding, 16 pixels, until the user scrolls to it. The
            scroll bar would then show a content height which is too small, and the ninth box
            would become taller while the user scrolls to it.

            SwingTree measures the text of the ninth box as soon as the layout gives the ninth
            box its width, so the ninth box has its text height without ever being painted.
        """
        given : 'A window, 400 by 300 pixels, with a scroll pane whose content follows the width of the scroll pane.'
            var column = UI.panel("wrap 1, fillx, ins 0, gap 0", "[grow]").get(JPanel)
            UI.runNow { 8.times { column.add(wrappedTextBox(), "growx, wmin 0") } }
            var frame = new JFrame()
            UI.runNow {
                frame.setContentPane(UI.scrollPane(conf -> conf.fitWidth(true)).add(column).get(JScrollPane))
                frame.setSize(400, 300)
            }
            showAndWaitUntilTheWindowHasSettled(frame)

        when : """
            We add a ninth box at the bottom of the column, call `revalidate()` on the column,
            and let Swing run the layouts which the `revalidate()` call requested.
        """
            JBox ninth = UI.runAndGet({ wrappedTextBox() })
            UI.runNow {
                column.add(ninth, "growx, wmin 0")
                column.revalidate()
            }
            runTheRequestedLayouts(column)
            int height = UI.runAndGet({ ninth.getHeight() })
            int textHeight = textHeightOf(ninth)
        then : 'The ninth box lies outside the visible part of the scroll pane.'
            UI.runAndGet({ ninth.getVisibleRect().isEmpty() })
        and : 'The ninth box has its text height, which is much more than the 16 pixels of its padding.'
            height == textHeight
            height > 16

        cleanup :
            UI.runNow { frame.dispose() }
    }

    def 'When a scroll pane becomes narrower, every box in it gets its text height before anything is painted.'()
    {
        reportInfo """
            A scroll pane shows a column of eight boxes with wrapped text, in a window which is
            400 by 300 pixels. The scroll pane sits in a panel, and we give the panel an empty
            border of 100 pixels on its right side. The border makes the scroll pane, and every
            box in the scroll pane, 100 pixels narrower. A narrower box needs more lines for its
            text, so every box needs to become taller.

            After the border changes, Swing lays out the window and then paints, in one task on
            its event thread. Right after the layout in that task, two things must be true:

            1. The boxes outside the visible part of the scroll pane have their text height for
               the new width. Swing does not paint those boxes, so if SwingTree measured the text
               of a box only while painting the box, those boxes would keep the height which
               belongs to the old width.
            2. The visible boxes have their text height as well. Otherwise Swing would paint the
               visible boxes once at their old height and correct them in the next task, which
               the user sees as a flicker.

            SwingTree measures the text of each box during the layout, as soon as the box gets
            its new width. When a box then prefers a different height, the scroll pane lays out
            its content a second time before the layout of the scroll pane is finished.

            This scenario and the scenario 'When a window becomes narrower, ...' check the same
            result through two different routes in Swing. Here, a component inside the window
            changed, so Swing starts the layout at the scroll pane, by calling `validate()` on
            the scroll pane. When the window is resized, Swing starts the layout at the window
            and reaches the scroll pane through `validateTree()`. SwingTree lays out the content
            of the scroll pane a second time in both routes, so both scenarios are needed.
        """
        given : 'A panel holding a scroll pane, whose content is a column of eight boxes that follows the width of the scroll pane.'
            var column = UI.panel("wrap 1, fillx, ins 0, gap 0", "[grow]").get(JPanel)
            UI.runNow { 8.times { column.add(wrappedTextBox(), "growx, wmin 0") } }
            var holder = new JPanel(new BorderLayout())
            var frame = new JFrame()
            UI.runNow {
                holder.add(UI.scrollPane(conf -> conf.fitWidth(true)).add(column).get(JScrollPane))
                frame.setContentPane(holder)
                frame.setSize(400, 300)
            }
            showAndWaitUntilTheWindowHasSettled(frame)
            int widthBefore = UI.runAndGet({ column.getComponent(0).getWidth() })

        when : """
            We give the panel an empty border of 100 pixels on its right side and call
            `revalidate()` on the panel. In the same task on Swing's event thread, we then call
            `RepaintManager.validateInvalidComponents()`, which is the layout Swing runs right
            before it paints. We read the sizes of all boxes right after that call, so nothing
            has been painted at the new width yet.
        """
            List<Dimension> sizes = UI.runAndGet({
                holder.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 100))
                holder.revalidate()
                RepaintManager.currentManager(holder).validateInvalidComponents()
                column.getComponents().collect { it.getSize() }
            })
            List<Integer> textHeights = UI.runAndGet({ column.getComponents() }).collect { textHeightOf(it as JBox) }

        then : 'Every box is now 100 pixels narrower than before.'
            sizes.every { it.width == widthBefore - 100 }
        and : 'Every box, visible or not, already has its text height for its new width.'
            sizes.collect { it.height as int } == textHeights

        cleanup :
            UI.runNow { frame.dispose() }
    }

    def 'A box added to a `JScrollPanels` list has its text height in the first layout, before it is painted.'()
    {
        reportInfo """
            A `JScrollPanels` list shows one box per message. The list is bound to a property
            holding a tuple of messages, through `addAll(Var<Tuple<M>>, BoundViewSupplier)`.
            Each box shows the text of its message, wrapped, with 8 pixels of padding on every
            side. A chat view is built this way.

            When a message is added to the tuple, the list builds a new box for the message.
            A box which was just built has no width yet, so SwingTree cannot measure the text of
            the new box, and the preferred height of the new box is the height of its padding:
            16 pixels. Then, in one task on Swing's event thread, Swing's `RepaintManager` lays
            out the list, which gives the new box its width, and paints. If SwingTree measured
            the text of the new box only while painting the new box, the new box would be laid
            out and painted at 16 pixels in that task, and get its text height only in the next
            task. The user would see the new box at 16 pixels for one frame, and then see every
            box below the new box move down.

            SwingTree measures the text of the new box during the layout, as soon as the new box
            gets its width, and the scroll pane of the list lays out its content a second time
            before the layout of the scroll pane is finished. So the first layout already gives
            the new box its text height.
        """
        given : 'A `JScrollPanels` list whose content follows the width of the list, holding one message, in a window of 400 by 300 pixels.'
            var messages = Var.of(Tuple.of(Message, new Message("1", TEXT)))
            JScrollPanels list =
                    UI.scrollPanels(conf -> conf.fitWidth(true))
                    .addAll(messages, message -> UI.of(wrappedTextBox(message.get().text())))
                    .get(JScrollPanels)
            var frame = new JFrame()
            UI.runNow {
                frame.setContentPane(list)
                frame.setSize(400, 300)
            }
            showAndWaitUntilTheWindowHasSettled(frame)

        when : """
            We add a second message to the tuple. In the same task on Swing's event thread, we
            then call `RepaintManager.validateInvalidComponents()`, which is the layout Swing runs
            right before it paints. We read the size of the new box right after that call, so the
            new box has not been painted yet.
        """
            JBox newBox = null
            Dimension size = UI.runAndGet({
                messages.update(tuple -> tuple.add(new Message("2", TEXT)))
                RepaintManager.currentManager(list).validateInvalidComponents()
                var entry = list.getContentPanel().getComponent(1) as JScrollPanels.EntryPanel
                newBox = entry.getLastState() as JBox
                newBox.getSize()
            })

        then : 'The new box has a width from the layout.'
            size.width > 0
        and : 'The new box already has its text height for that width, not the 16 pixels of its padding.'
            size.height as int == textHeightOf(newBox)
            size.height > 16

        cleanup :
            UI.runNow { frame.dispose() }
    }

    def 'When a window becomes narrower, the boxes in its scroll pane have their text height before the window is painted.'()
    {
        reportInfo """
            A scroll pane fills a window of 400 by 300 pixels and shows a column of eight boxes
            with wrapped text. Then the window is made 80 pixels narrower, as when the user drags
            the edge of the window. Swing handles a resized window by calling `validate()` on the
            window, which lays out the whole window, starting at the window, and then paints.

            Right after that call to `validate()`, every box, visible or not, has its text height
            for its new width. SwingTree measures the text of each box during the layout, and the
            scroll pane lays out its content a second time while the window is being laid out.

            This scenario and the scenario 'When a scroll pane becomes narrower, ...' check the
            same result through two different routes in Swing. Here, the window was resized, so
            Swing starts the layout at the window and reaches the scroll pane through
            `validateTree()`. When a component inside the window changes, Swing starts the layout
            at the scroll pane, by calling `validate()` on the scroll pane. SwingTree lays out the
            content of the scroll pane a second time in both routes, so both scenarios are needed.
        """
        given : 'A window with a scroll pane whose content is a column of eight boxes that follows the width of the scroll pane.'
            var column = UI.panel("wrap 1, fillx, ins 0, gap 0", "[grow]").get(JPanel)
            UI.runNow { 8.times { column.add(wrappedTextBox(), "growx, wmin 0") } }
            var frame = new JFrame()
            UI.runNow {
                frame.setContentPane(UI.scrollPane(conf -> conf.fitWidth(true)).add(column).get(JScrollPane))
                frame.setSize(400, 300)
            }
            showAndWaitUntilTheWindowHasSettled(frame)
            int widthBefore = UI.runAndGet({ column.getComponent(0).getWidth() })

        when : """
            We make the window 80 pixels narrower and call `validate()` on the window once, in
            the same task on Swing's event thread, which is what Swing does for a resized window
            before it paints. We read the sizes of all boxes right after that call.
        """
            List<Dimension> sizes = UI.runAndGet({
                frame.setSize(320, 300)
                frame.validate()
                column.getComponents().collect { it.getSize() }
            })
            List<Integer> textHeights = UI.runAndGet({ column.getComponents() }).collect { textHeightOf(it as JBox) }

        then : 'Every box is now 80 pixels narrower than before.'
            sizes.every { it.width == widthBefore - 80 }
        and : 'Every box, visible or not, already has its text height for its new width.'
            sizes.collect { it.height as int } == textHeights

        cleanup :
            UI.runNow { frame.dispose() }
    }

    def 'Boxes outside any scroll pane have their text height after the layout which SwingTree requests once their width changed.'()
    {
        reportInfo """
            A column of two boxes with wrapped text sits directly in a window of 400 by 600
            pixels, without a scroll pane. The window is tall enough for both boxes at every
            height the boxes need in this scenario. We give the panel around the column an empty
            border of 150 pixels on its right side, which makes every box 150 pixels narrower,
            so every box needs more lines and more height.

            This scenario checks that a height measured during the layout is laid out afterwards.
            SwingTree measures the text of a box during the layout of the column, which is the
            parent of the box. A `revalidate()` called while the parent is being laid out has no
            effect, because Swing marks the parent as laid out when the layout of the parent
            finishes. So SwingTree calls `revalidate()` on the box again, in a later task on
            Swing's event thread, after the layout of the parent has finished. Without that later
            call, the boxes would keep their old height, although their preferred height is
            larger.

            This is also the limit of measuring during the layout outside a SwingTree scroll
            pane: no scroll pane lays out the column a second time, so Swing paints the visible
            boxes once at their old height, in the task which ran the first layout, and at their
            text height only after the layout which the later `revalidate()` call requests.
            Before SwingTree measured during the layout, Swing also painted the visible boxes
            once at their old height, because only painting measured their text.
        """
        given : 'A panel holding a column of two boxes, in a window of 400 by 600 pixels.'
            var column = UI.panel("wrap 1, fillx, ins 0, gap 0", "[grow]").get(JPanel)
            UI.runNow { 2.times { column.add(wrappedTextBox(), "growx, wmin 0") } }
            var holder = new JPanel(new BorderLayout())
            var frame = new JFrame()
            UI.runNow {
                holder.add(column, BorderLayout.NORTH)
                frame.setContentPane(holder)
                frame.setSize(400, 600)
            }
            showAndWaitUntilTheWindowHasSettled(frame)
            int widthBefore = UI.runAndGet({ column.getComponent(0).getWidth() })

        when : """
            We give the panel an empty border of 150 pixels on its right side, call `revalidate()`
            on the panel, and let Swing run the layouts which the `revalidate()` calls requested,
            including the later `revalidate()` call of SwingTree.
        """
            UI.runNow {
                holder.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 150))
                holder.revalidate()
            }
            runTheRequestedLayouts(column)
            List<Dimension> sizes = UI.runAndGet({ column.getComponents().collect { it.getSize() } })
            List<Integer> textHeights = UI.runAndGet({ column.getComponents() }).collect { textHeightOf(it as JBox) }

        then : 'Every box is now 150 pixels narrower than before.'
            sizes.every { it.width == widthBefore - 150 }
        and : 'Every box has its text height for its new width.'
            sizes.collect { it.height as int } == textHeights

        cleanup :
            UI.runNow { frame.dispose() }
    }

    def 'A box inside a plain `JScrollPane` inside a SwingTree scroll pane has its text height after the layout which SwingTree requests.'()
    {
        reportInfo """
            A SwingTree scroll pane (`UI.ScrollPane`) holds a plain Swing `JScrollPane`, which is
            250 pixels tall and shows a column of three boxes with wrapped text. Each box is about
            150 pixels tall, so the third box already lies partly outside the visible part of the
            plain `JScrollPane`. A fourth box is added at the bottom of the column, outside the
            visible part of the plain `JScrollPane`.

            A `JScrollPane` is a validate root, so the `revalidate()` call on the column makes
            Swing start the layout at the plain `JScrollPane`, not at the SwingTree scroll pane.
            The SwingTree scroll pane is therefore not being laid out when SwingTree measures the
            text of the fourth box, and cannot lay out its content a second time. So the layout
            which gives the fourth box its width gives the fourth box the height of its padding,
            16 pixels. SwingTree then calls `revalidate()` on the fourth box in a later task on
            Swing's event thread, and the layout which that `revalidate()` call requests gives the
            fourth box its text height.

            The fourth box is never painted in this scenario. If SwingTree measured the text of the
            fourth box only while painting the fourth box, the fourth box would keep 16 pixels.
        """
        given : """
            A window of 400 by 400 pixels with a SwingTree scroll pane, which holds a plain
            `JScrollPane` of 250 pixels height, which shows a column of three boxes.
        """
            var column = UI.panel("wrap 1, fillx, ins 0, gap 0", "[grow]").get(JPanel)
            UI.runNow { 3.times { column.add(wrappedTextBox(), "growx, wmin 0") } }
            var frame = new JFrame()
            UI.runNow {
                var plainScrollPane = new JScrollPane(column)
                plainScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
                plainScrollPane.setPreferredSize(new Dimension(300, 250))
                var holder = new JPanel(new BorderLayout())
                holder.add(plainScrollPane)
                frame.setContentPane(UI.scrollPane(conf -> conf.fitWidth(true)).add(holder).get(JScrollPane))
                frame.setSize(400, 400)
            }
            showAndWaitUntilTheWindowHasSettled(frame)

        when : """
            We add a fourth box at the bottom of the column and call `revalidate()` on the column.
            In the same task on Swing's event thread, we then call
            `RepaintManager.validateInvalidComponents()`, which is the layout Swing runs right
            before it paints, and read the size of the fourth box right after that call.
        """
            JBox fourth = UI.runAndGet({ wrappedTextBox() })
            Dimension sizeAfterTheFirstLayout = UI.runAndGet({
                column.add(fourth, "growx, wmin 0")
                column.revalidate()
                RepaintManager.currentManager(column).validateInvalidComponents()
                fourth.getSize()
            })
        then : 'The fourth box has a width from the first layout, and the 16 pixels of its padding as its height.'
            sizeAfterTheFirstLayout.width > 0
            sizeAfterTheFirstLayout.height == 16

        when : 'We let Swing run the layouts which the `revalidate()` calls requested, including the later `revalidate()` call of SwingTree.'
            runTheRequestedLayouts(column)
            int height = UI.runAndGet({ fourth.getHeight() })
        then : 'The fourth box lies outside the visible part of the plain `JScrollPane`.'
            UI.runAndGet({ fourth.getVisibleRect().isEmpty() })
        and : 'The fourth box has its text height, which is much more than the 16 pixels of its padding.'
            height == textHeightOf(fourth)
            height > 16

        cleanup :
            UI.runNow { frame.dispose() }
    }

    private static JBox wrappedTextBox( String text = TEXT ) {
        return UI.box()
                .withStyle(it -> it
                    .padding(8)
                    .text(t -> t
                        .content(text)
                        .placement(UI.Placement.TOP_LEFT)
                        .wrapLines(true)
                        .autoPreferredHeight(true)
                    )
                )
                .get(JBox)
    }

    /**
     *  Returns the text height of the given box: the preferred height SwingTree computes for the
     *  text of the box at the current width of the box. This is SwingTree's reference measurement.
     *  It builds a separate box with the same text and the font of the given box, sets the width
     *  of the separate box directly with {@code setSize}, outside any layout, and paints the
     *  separate box once, which makes SwingTree compute its style and measure its text at that width.
     *  <p>
     *  Using the measurement while painting as the reference is not circular. The scenarios do not
     *  check when or how the text is measured, but whether the boxes in the window have the height
     *  which SwingTree computes for their text at their width. Measuring while painting is the
     *  computation SwingTree has always done, and it is correct for a box whose width is known.
     *  The scenarios fail when SwingTree measures the boxes in the window only while painting,
     *  because those boxes are then either not painted at all or painted only after the layout
     *  which the scenarios check.
     */
    private static int textHeightOf( JBox box ) {
        return UI.runAndGet({
            JBox twin = wrappedTextBox()
            twin.setFont(box.getFont())
            twin.setSize(box.getWidth(), 10)
            twin.paintComponent(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
            twin.getPreferredSize().height
        })
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
        runTheRequestedLayouts(frame.getRootPane())
        UI.runNow { frame.validate() }
    }

    /**
     *  Calls {@code RepaintManager.validateInvalidComponents()} three times, each time in a new task
     *  on Swing's event thread. Swing first runs every task which was queued before that new task,
     *  for example a later {@code revalidate()} call of SwingTree, or a paint. Each call then lays out
     *  every component whose layout a {@code revalidate()} call requested. Three calls are made because
     *  a layout can request another layout in a later task.
     *  This method never lays out a component which no {@code revalidate()} call asked to be laid out.
     */
    private static void runTheRequestedLayouts( JComponent anyComponentOfTheWindow ) {
        3.times {
            UI.runNow { RepaintManager.currentManager(anyComponentOfTheWindow).validateInvalidComponents() }
        }
    }
}
