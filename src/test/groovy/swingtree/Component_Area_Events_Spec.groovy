package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import swingtree.style.ComponentExtension
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.JPanel
import java.awt.Color
import java.awt.Shape
import java.awt.event.MouseEvent
import java.awt.geom.Area
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Title("Component Areas and the Mouse Events Bound to Them")
@Narrative('''

    A style divides a component into areas, and `UI.ComponentArea` names them:
    `EXTERIOR` is the ring a margin leaves around the body, `BORDER` is the ring a border
    width fills, `INTERIOR` is what lies inside the border, `BODY` is the border and the
    interior together, and `ALL` is the whole component. A border of four different
    widths, or a corner radius, makes these areas shapes no rectangle describes, so a
    component has to be able to return each area as a `java.awt.Shape` when asked.
    `ComponentExtension.getComponentArea(..)` does that, `shapeOf(..)` on the event and
    style delegates passes the answer on to application code, and the style painters and
    mouse event handlers bound to an area use the very same shapes.

    Swing tells you when the cursor entered a component. SwingTree lets you ask a
    narrower question: `onMouseEnter(UI.ComponentArea.BORDER, ..)` fires when the cursor
    reaches the border ring, stays silent while the cursor is in the middle of the
    component, and `onMouseExit(UI.ComponentArea.BORDER, ..)` fires when the cursor leaves
    the ring again - also when it leaves by moving inward, which Swing itself never
    reports as an exit.

    Every scenario in this specification checks one requirement: **which shapes a
    component returns is decided by its style, and by nothing else.** In particular not by
    how, or whether, its rendering happens to be cached.

    See also: `Stretch_Tiling_Eligibility_Spec` for how renderings are cached,
    `Cache_Configuration_Spec` for the switches that govern the cache, and
    `Individual_Component_Styling_Spec` for the styles that define the areas in the first
    place.

''')
@Subject([UI, ComponentExtension])
@Timeout(value = 45, unit = TimeUnit.SECONDS)
class Component_Area_Events_Spec extends Specification
{
    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        SwingTree.get().setUiScaleFactor(1f)
        SwingTree.get().setCacheTilingEnabled(true) // The production default, made explicit.
    }

    def cleanup() {
        SwingTree.clear()
    }

    /** Moves the cursor to a point inside the component, the way a real mouse would travel
     *  across it. SwingTree derives its area enter and exit events from the motion itself,
     *  so a move is all a scenario needs in order to cross an area boundary. */
    private static void moveMouseTo( JPanel panel, int x, int y ) {
        UI.runNow(() -> panel.dispatchEvent(
            new MouseEvent(panel, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false)
        ))
    }

    /** How many pixels of a rendering are exactly {@link Color#CYAN}. */
    private static int cyanPixelsIn( BufferedImage image ) {
        int count = 0
        for ( int x = 0; x < image.getWidth(); x++ )
            for ( int y = 0; y < image.getHeight(); y++ )
                if ( image.getRGB(x, y) == Color.CYAN.getRGB() )
                    count++
        return count
    }


    def 'A component with a border returns the shape of its border area. (#description)'(
        String description, Closure styler
    ) {
        reportInfo """
            A border width turns the outermost pixels of a component into its
            `UI.ComponentArea.BORDER` area: a ring which follows every edge of the component
            and leaves the middle out. A component has to return that ring as a
            `java.awt.Shape` from `ComponentExtension.getComponentArea(UI.ComponentArea.BORDER)`,
            because `shapeOf(..)` on the delegates and the
            `onMouseEnter(UI.ComponentArea.BORDER, ..)` events are built on that answer.
            So we paint a panel of 300 by 200 pixels with a border at least 12 pixels wide,
            ask for the ring, and test three points: the point (6, 100) lies inside the left
            edge, the point (150, 6) lies inside the top edge, and the center point (150, 100)
            lies in the middle, outside of the ring.

            **Why this is fragile:** building a shape is expensive, so SwingTree builds the
            shape of an area only when something asks for it, and it shares the shapes
            between all components which have the same border widths, margin, corner
            rounding and size. That makes it tempting to answer "does this component have a
            border area?" with "has a border shape been built for it yet?". The two
            questions only agree as long as painting a component builds the shapes of that
            component. Commit "570 Improve Cache Hits and Reduce Memory Consumption through
            9 Patch Tiling" (#571) broke that: when a style looks the same along its edges
            at every size, SwingTree now paints it only once, at a small size of a few tens
            of pixels, and stretches the edges and corners of that small painting to the
            real size of the component. The shapes built while painting are those of the
            small painting. The shapes of the component itself are never built, and so, from
            its first paint onward, the component returned no border area at all.
        """
        given : 'A panel of 300 by 200 pixels with a border.'
            var panel = UI.panel().withStyle(styler).get(JPanel)
            panel.setSize(300, 200)
        and : 'It is painted once, the way any component on screen is.'
            Utility.renderSingleComponent(panel)

        when : 'We ask the panel for the shape of its border area.'
            Shape borderArea = ComponentExtension.from(panel).getComponentArea(UI.ComponentArea.BORDER).orElse(null)

        then : 'It returns a shape, because its style defines a border width.'
            borderArea != null
        and : 'The point 6 pixels into the left edge and the point 6 pixels into the top edge lie inside the shape...'
            borderArea.contains(6, 100)
            borderArea.contains(150, 6)
        and : '...and the center point of the panel lies outside of it.'
            !borderArea.contains(150, 100)

        where :
            description                            | styler
            'a border of one width and one color'  | { it.border(12, "black") }
            'a border with rounded corners'        | { it.border(12, "black").borderRadius(20) }
            'a border of four different widths'    | { it.borderWidths(14, 16, 18, 20).borderColor("black") }
    }


    def 'A component returns the shape of its interior, its exterior and its body as its style defines them. (#description)'(
        String description, UI.ComponentArea area, Closure styler, List<Integer> inside, List<Integer> outside
    ) {
        reportInfo """
            A style defines up to four areas of a component, and each one is a
            `java.awt.Shape` which `ComponentExtension.getComponentArea(..)` returns:
            a border width fills the `BORDER` ring, what lies inside that ring is the
            `INTERIOR`, a margin leaves an `EXTERIOR` ring around the body, and the `BODY`
            is the border and the interior together. This scenario paints a panel of 300 by
            200 pixels, asks for one of the three areas beside the border, and tests two
            points: one that lies in the area and one that lies outside of it. For the
            interior of a panel with a 13 pixel border, for example, the center point
            (150, 100) lies inside, and the point (6, 100) lies in the border, outside of it.

            **Why this is fragile:** `getComponentArea(INTERIOR)` and `getComponentArea(BODY)`
            return the full bounds of the component when SwingTree has no shape for the
            area, so a missing interior shape does not show up as an empty answer but as a
            wrong one: an `onMouseEnter(UI.ComponentArea.INTERIOR, ..)` handler on a
            bordered panel fired when the cursor reached the edge of the panel instead of
            when it crossed the border. The exterior went missing for a second reason.
            SwingTree used to build the shape of an area only while painting that area, and
            answered "does this area exist?" with "has its shape been built?". But when the
            background color is opaque, and the border, if there is one, is opaque too,
            SwingTree paints the background by filling the whole component with the
            foundation color, which is the color of the margin, and then filling the body
            with the background color on top of it, because that is cheaper than filling
            the exterior ring on its own. So painting never built the exterior shape, and a
            panel with an opaque background returned no exterior at all. Deciding from the
            style alone whether an area exists closes both gaps.
        """
        given : 'A panel of 300 by 200 pixels, styled so that it has the area in question.'
            var panel = UI.panel().withStyle(styler).get(JPanel)
            panel.setSize(300, 200)
        and : 'It is painted once, the way any component on screen is.'
            Utility.renderSingleComponent(panel)

        when : 'We ask the panel for the shape of that area.'
            Shape shape = ComponentExtension.from(panel).getComponentArea(area).orElse(null)

        then : 'It returns a shape, because its style defines the area.'
            shape != null
        and : 'The point that lies in the area is inside the shape, and the point that lies beside the area is outside of it.'
            shape.contains(inside[0], inside[1])
            !shape.contains(outside[0], outside[1])

        where :
            description                              | area                       | styler                                                                    | inside     | outside
            'the interior inside a 13 pixel border'  | UI.ComponentArea.INTERIOR  | { it.border(13, "black") }                                                | [150, 100] | [6, 100]
            'the exterior left by a 15 pixel margin' | UI.ComponentArea.EXTERIOR  | { it.margin(15).foundationColor("black").backgroundColor("white") }       | [7, 100]   | [150, 100]
            'the body inside a 17 pixel margin'      | UI.ComponentArea.BODY      | { it.margin(17).foundationColor("black").backgroundColor("white") }       | [150, 100] | [8, 100]
    }


    def 'Handlers bound to the border area fire when the cursor crosses into and out of the border ring.'()
    {
        reportInfo """
            `onMouseEnter(UI.ComponentArea.BORDER, ..)` must fire when the cursor moves onto
            the border of a panel, and `onMouseExit(UI.ComponentArea.BORDER, ..)` must fire
            when the cursor moves on into the middle of that same panel, even though the
            cursor never left the component. Swing reports neither. SwingTree derives both
            by asking the component for the shape of its border area on every mouse move
            and testing whether the cursor is inside that shape.
            `examples.hover.BorderHoverExample` was written to demonstrate this behaviour.

            The panel here is 300 by 200 pixels with a 12 pixel border, so the point
            (6, 100) lies in the border, the point (150, 100) lies in the middle, and the
            point (150, 6) lies in the border again. We move the cursor to these three
            points in that order and expect an enter, an exit, and a second enter.

            **Why this is fragile:** a handler that never runs looks like a broken listener
            registration, but the listener is not what fails here. When the component
            returns no shape for its border area, SwingTree treats the cursor as being
            outside of the area, so the handler never runs, and nothing is logged. That is
            how commit "570 Improve Cache Hits and Reduce Memory Consumption through 9 Patch
            Tiling" (#571) switched off every handler bound to a border area without a
            warning, a stack trace or a single failing rendering test. Until then, SwingTree
            built the shape of the border area while painting the border, and answered
            "does this component have a border area?" with "has its border shape been
            built?". Since then, when a style looks the same along its edges at every size,
            SwingTree paints it only once, at a small size of a few tens of pixels, and
            stretches the edges and corners of that small painting to the real size of the
            component. Painting the component no longer built the shapes of the component
            itself, only those of the small painting, and so the component returned no
            border shape.
        """
        given : 'A panel of 300 by 200 pixels with a 12 pixel border, counting the enter and exit events on that border.'
            var enterCount = new AtomicInteger(0)
            var exitCount  = new AtomicInteger(0)
            var panel = UI.panel()
                            .withStyle(it -> it.border(12, "black"))
                            .onMouseEnter(UI.ComponentArea.BORDER, it -> enterCount.incrementAndGet() )
                            .onMouseExit(UI.ComponentArea.BORDER, it -> exitCount.incrementAndGet() )
                            .get(JPanel)
        and : 'It is painted once, the way any component on screen is.'
            panel.setSize(300, 200)
            Utility.renderSingleComponent(panel)
        expect : 'Nothing has been counted yet, because the cursor has not moved anywhere.'
            enterCount.get() == 0
            exitCount.get() == 0

        when : 'The cursor moves to (6, 100), six pixels into the left border.'
            moveMouseTo(panel, 6, 100)
        then : 'That is an enter, and not yet an exit.'
            enterCount.get() == 1
            exitCount.get() == 0

        when : 'The cursor moves on to (150, 100), the middle of the same panel.'
            moveMouseTo(panel, 150, 100)
        then : 'It left the border ring, so this is an exit, although the cursor is still over the component.'
            enterCount.get() == 1
            exitCount.get() == 1

        when : 'The cursor moves to (150, 6), six pixels into the top border.'
            moveMouseTo(panel, 150, 6)
        then : 'The ring is entered a second time.'
            enterCount.get() == 2
            exitCount.get() == 1
    }


    def 'A painter started by a border enter handler shows up in the rendering of the component.'()
    {
        reportInfo """
            A handler bound to the border area of a panel can start an animation, and the
            animation can add a painter to the panel through
            `delegate.paint(UI.ComponentArea.BORDER, status, painter)`. What that painter
            draws must appear in the next rendering of the panel and in every rendering
            after it while the animation runs. `examples.hover.BorderHoverExample` does
            exactly this: when the cursor enters the border, it paints a cyan disc at the
            cursor for a quarter of a second.

            The panel here is 300 by 200 pixels with a 24 pixel border. Its enter handler
            starts a five second animation which paints a cyan disc 30 pixels wide around
            the cursor. We move the cursor to (12, 100), inside the left border, wait 100
            milliseconds for the animation to register its painter, and count the cyan
            pixels in three consecutive renderings of the panel. Nothing else in the panel
            is cyan.

            **Why this is fragile:** five things have to work in a row for one disc to
            appear: the panel returns the shape of its border area, SwingTree derives an
            enter event from that shape, the animation starts, its painter is registered on
            the panel, and the panel is not painted from a cached rendering which was made
            before the painter existed. A break anywhere in the row shows up as the same
            symptom: nothing
            appears. This scenario is deliberately end to end so that it fails whichever of
            the five breaks. When commit "570 Improve Cache Hits and Reduce Memory
            Consumption through 9 Patch Tiling" (#571) landed, the disc vanished from
            `examples.hover.BorderHoverExample` while the animation and the painter were
            intact: the panel no longer returned a border shape, so the enter handler never
            ran.
        """
        given : 'A panel of 300 by 200 pixels with a 24 pixel border, whose border enter handler paints a cyan disc around the cursor for five seconds.'
            var panel = UI.panel()
                            .withStyle(it -> it.border(24, "black"))
                            .withBackground(Color.WHITE)
                            .onMouseEnter(UI.ComponentArea.BORDER, it ->
                                it.animateFor(5, TimeUnit.SECONDS, status ->
                                    it.paint(UI.ComponentArea.BORDER, status, g -> {
                                        g.setColor(Color.CYAN)
                                        g.fillOval(it.mouseX() - 15, it.mouseY() - 15, 30, 30)
                                    })
                                )
                            )
                            .get(JPanel)
        and : 'It is painted once, the way any component on screen is.'
            panel.setSize(300, 200)
            Utility.renderSingleComponent(panel)
        expect : 'Nothing cyan has been painted so far.'
            cyanPixelsIn(Utility.renderSingleComponent(panel)) == 0

        when : 'The cursor moves to (12, 100), inside the left border, and the animation gets 100 milliseconds to start.'
            moveMouseTo(panel, 12, 100)
            Thread.sleep(100)
            UI.sync()

        then : 'The disc is in this rendering of the panel and in the two after it.'
            (1..3).every { cyanPixelsIn(Utility.renderSingleComponent(panel)) > 300 }
        and : 'The point the cursor stopped at is cyan, because it is the center of the disc.'
            Utility.renderSingleComponent(panel).getRGB(12, 100) == Color.CYAN.getRGB()
    }


    def 'The shape a component returns is the same with the stretch tiling cache switched on and off. (#description)'(
        String description, Closure styler
    ) {
        reportInfo """
            SwingTree caches the painting of a component. When a style looks the same along
            its edges at every size, SwingTree paints it only once, at a small size of a few
            tens of pixels, and stretches the edges and corners of that small painting to
            the real size of the component whenever the component is painted. That is a
            decision about pixels, taken to keep resizing cheap, and it must be invisible
            from the outside. `SwingTree.get().setCacheTilingEnabled(..)` switches it on and
            off at runtime, so the invisibility is something a test can state outright:
            what a component returns from `getComponentArea(UI.ComponentArea.BORDER)` has to
            be the same shape with the switch in either position.

            Here we paint a panel of 300 by 200 pixels with a border, first with the switch
            on and then with it off, and compare the border shape returned after each
            painting.

            **Why this is fragile:** with the switch on, what gets painted is not the
            component but a small painting of its style at a different size. Anything
            computed as a side effect of painting is therefore computed for the small
            painting, and a question answered from such side effects quietly turns into a
            question about a caching decision. The area shapes were such a side effect:
            SwingTree built them while painting, and answered "does this component have a
            border area?" with "has its border shape been built yet?". With the switch on
            the shape was never built and the panel returned no border shape; with the
            switch off it was built and the panel returned one.

            The two panels use border widths of 9 and 10 pixels, which no other scenario in
            this specification uses. SwingTree shares area shapes between all components
            which have the same border widths, margin, corner rounding and size, so a shape
            built by another scenario for an identically styled panel would be found here
            and returned as this panel's own, and the comparison would prove nothing.
        """
        given : 'A panel of 300 by 200 pixels with a border, far larger than the small painting the cache makes of its style.'
            var panel = UI.panel().withStyle(styler).get(JPanel)
            panel.setSize(300, 200)

        when : 'We paint it with the cache switch on, which is the production default, and ask for its border shape.'
            Utility.renderSingleComponent(panel)
            Shape whenTiled = ComponentExtension.from(panel).getComponentArea(UI.ComponentArea.BORDER).orElse(null)
        and : 'Then we paint it again with the cache switch off and ask once more.'
            SwingTree.get().setCacheTilingEnabled(false)
            Utility.renderSingleComponent(panel)
            Shape whenNotTiled = ComponentExtension.from(panel).getComponentArea(UI.ComponentArea.BORDER).orElse(null)

        then : 'The style defines a border width, so the panel returns a shape both times.'
            whenTiled != null
            whenNotTiled != null
        and : 'Both are the same shape, because the cache has no say in the matter.'
            new Area(whenTiled) == new Area(whenNotTiled)

        where :
            description                | styler
            'a straight border ring'   | { it.border(9, "black") }
            'a rounded border ring'    | { it.border(10, "black").borderRadius(17) }
    }
}
