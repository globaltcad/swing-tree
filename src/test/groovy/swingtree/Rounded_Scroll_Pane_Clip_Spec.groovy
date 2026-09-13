package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.style.ComponentBackend
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.JScrollPane
import java.awt.Color
import java.awt.Component
import java.awt.Container

@Title("The Round Corners of a Scroll Pane")
@Narrative('''

    A scroll pane styled with round corners keeps its content inside those corners.
    Its viewport, its scroll bars and whatever they show are painted through a clip
    of the scroll pane's rounded body, so a view that runs all the way to the edge
    is cut off where the corner curves away, instead of poking out past it.

    That clip is not free. Java2D builds a clip region from a rounded shape one
    scanline at a time, and every child painted inside it then has its own
    rectangle intersected with that shape. So SwingTree only sets it up when some
    child actually reaches into a corner. When padding or a border already keeps
    every child inside the rounded body, a clip of that body could not remove a
    single pixel from them, and the children are painted without it.

    Both halves are pinned here in pixels: content that reaches the corners is
    cut off, and content that stays inside is painted in full.

''')
@Subject([UI, ComponentBackend])
class Rounded_Scroll_Pane_Clip_Spec extends Specification
{
    private static final Color CONTENT = new Color(0xc8, 0x1e, 0x28)

    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        SwingTree.get().setUiScaleFactor(1f)
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    private static void layOut( Component component ) {
        if ( component instanceof Container ) {
            ((Container) component).doLayout()
            for ( Component child : ((Container) component).getComponents() )
                layOut(child)
        }
    }

    private static boolean isContent( int argb ) {
        return (argb & 0xffffff) == (CONTENT.getRGB() & 0xffffff) && (argb >>> 24) == 0xff
    }

    def 'Content which reaches into the round corners of a scroll pane is cut off at the corner.'()
    {
        reportInfo """
            Nothing keeps this view away from the edge of the scroll pane: there is no border
            and no padding, so the viewport covers the scroll pane from corner to corner and
            the red view fills the viewport. The corners of the scroll pane curve away from
            that rectangle, and the red must follow the curve rather than the rectangle.
        """
        given : 'A scroll pane with round corners and nothing between its edge and its view.'
            var scrollPane =
                    UI.scrollPane(it -> it.verticalScrollBarPolicy(UI.Active.NEVER).horizontalScrollBarPolicy(UI.Active.NEVER))
                    .withStyle( it -> it.borderRadius(40) )
                    .add(
                        UI.panel().withStyle( it -> it.backgroundColor(CONTENT) ).withPrefSize(200, 160)
                    )
                    .get(JScrollPane)
            scrollPane.setSize(200, 160)
            layOut(scrollPane)

        when : 'We paint it.'
            var image = Utility.renderSingleComponent(scrollPane)

        then : 'The viewport really does reach the corner of the scroll pane...'
            scrollPane.getViewport().getBounds() == new java.awt.Rectangle(0, 0, 200, 160)
        and : '...and still the pixel in the very corner, outside the curve, shows no content:'
            !isContent(image.getRGB(1, 1))
            !isContent(image.getRGB(198, 158))
        and : 'While the middle of the view is painted as usual:'
            isContent(image.getRGB(100, 80))
    }

    def 'Content which stays clear of the round corners of a scroll pane is painted in full.'()
    {
        reportInfo """
            Here padding holds the view far enough in from the edge that the corners never
            curve into it. Clipping the children to the rounded body would not change one of
            their pixels, which is why SwingTree does not set that clip up at all in this
            situation. Whether it does or not, every pixel of the view has to arrive, right up
            to its own corners.
        """
        given : 'A scroll pane with round corners and enough padding to keep its view clear of them.'
            var scrollPane =
                    UI.scrollPane(it -> it.verticalScrollBarPolicy(UI.Active.NEVER).horizontalScrollBarPolicy(UI.Active.NEVER))
                    .withStyle( it -> it.borderRadius(40).padding(20) )
                    .add(
                        UI.panel().withStyle( it -> it.backgroundColor(CONTENT) ).withPrefSize(160, 120)
                    )
                    .get(JScrollPane)
            scrollPane.setSize(200, 160)
            layOut(scrollPane)

        when : 'We paint it.'
            var image = Utility.renderSingleComponent(scrollPane)
            var viewport = scrollPane.getViewport().getBounds()
            int left   = viewport.@x
            int top    = viewport.@y
            int right  = viewport.@x + viewport.@width  - 1
            int bottom = viewport.@y + viewport.@height - 1

        then : 'The padding really does keep the viewport away from the edge...'
            left >= 20 && top >= 20
        and : '...and the view is painted into every corner of the viewport, and across it:'
            isContent(image.getRGB(left,  top))
            isContent(image.getRGB(right, top))
            isContent(image.getRGB(left,  bottom))
            isContent(image.getRGB(right, bottom))
            isContent(image.getRGB(100, 80))
    }
}
