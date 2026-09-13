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
            A scroll pane with round corners keeps its content inside those corners, even when
            nothing else would. Here there is no border and no padding, so the viewport covers
            the whole 200 by 160 pixel scroll pane from corner to corner, and the red view fills
            the viewport. `borderRadius(40)` rounds each corner with an arc 40 pixels wide, so
            the curve takes up the outermost 20 pixels of every corner. The red has to follow
            that curve: the pixel at (1, 1) lies more than 26 pixels from the centre of the top left
            curve at (20, 20), which is outside its radius of 20, so it must not be red.
            
            You may wonder why a scenario that only reads pixels is named after a clip. SwingTree
            paints the children of a round scroll pane through a clip in the shape of its rounded
            outline, and that is what cuts the red off at the corner. But it also has a small
            optimization: when every visible child already lies completely inside the rounded
            outline, the clip could not remove a single pixel from any of them, so SwingTree
            leaves it out. It does this only for speed, because Java2D builds a rounded clip one
            row of pixels at a time, and every child painted inside it pays for an intersection
            with that shape.
            
            Here we look at the situation in which that optimization must *not* kick in. The
            viewport's corner at (0, 0) lies outside the curve. Suppose the check behind the
            optimization were one day written a little too generously, for instance by testing
            the children against the scroll pane's rectangle instead of its rounded outline.
            Then the clip would be left out, the red would be painted straight into the corner,
            and the pixel at (1, 1) would be red.
            
            We check the pixel rather than the clip because the clip is set on a `Graphics` deep
            inside SwingTree's painting and taken off again before painting returns, so there is
            nothing left to look at afterwards. And what matters to a user is whether the red
            reaches past the curve, which we can read straight off the painted image, whichever
            way SwingTree got there.
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
            A scroll pane with round corners paints content that stays clear of those corners
            in full, right up to the content's own corners. Here the scroll pane is 200 by 160
            pixels and `borderRadius(40)` rounds each corner with an arc 40 pixels wide, so the
            curve takes up only the outermost 20 pixels of every corner. A padding of 20 pixels
            puts the viewport's top left corner at (20, 20), where the curve has already ended,
            and the same holds at the other three corners. So all four corners of the red view
            must be red.
            
            You may wonder why a scenario that only reads pixels is named after a clip. SwingTree
            normally paints the children of a round scroll pane through a clip in the shape of its
            rounded outline. But it also has a small optimization: when every visible child
            already lies completely inside the rounded outline, the clip could not remove a single
            pixel from any of them, so SwingTree leaves it out. It does this only for speed,
            because Java2D builds a rounded clip one row of pixels at a time, and every child
            painted inside it pays for an intersection with that shape.
            
            Here we look at the situation in which that optimization *does* kick in, and we make
            sure that it is safe: the image has to be exactly what clipping would have produced,
            which in this case is the whole view. If a later change started cutting into content
            it should leave alone, for instance by clipping the children to a shape smaller than
            the rounded outline, the view's corner pixels at (20, 20), (179, 20), (20, 139) and
            (179, 139) would stop being red, and this scenario would tell us.
            
            We check pixels rather than the clip because the clip is set on a `Graphics` deep
            inside SwingTree's painting and taken off again before painting returns, and in this
            situation there is not even a clip to find. What matters to a user is whether every
            pixel of the view arrives, which we can read straight off the painted image,
            whichever way SwingTree got there.
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
