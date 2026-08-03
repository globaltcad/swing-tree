package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import swingtree.SwingTreeInitConfig
import swingtree.components.JBox
import swingtree.style.ComponentExtension
import swingtree.threading.EventProcessor
import utility.Utility

import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.RoundRectangle2D
import java.util.concurrent.TimeUnit

@Title("Rounded Fill Equivalence")
@Narrative('''

    A component with a corner radius and a single background colour describes a
    very ordinary shape: a rounded rectangle, filled. Whatever SwingTree does to
    make that fast, it has to keep producing exactly that shape — so these
    scenarios paint the styled component and compare it against the same rounded
    rectangle filled with plain `Graphics2D.fill(..)`, drawn right here in the
    specification. Nothing about how the style engine rasterizes anything is
    referred to; the reference is one line of AWT that any reader can check by eye.

    That comparison is sharper than it looks, because SwingTree does *not* simply
    forward the shape to `fill`. A large rounded area is rasterized in pieces —
    the interior, which is fully covered and needs no antialiasing, separately
    from the four corners, which do — and the pieces have to reassemble into
    something indistinguishable from the undivided fill. A seam between two of
    them, a corner drawn a pixel small, or an interior band reaching into the
    curve would each show up here as a component that no longer matches the
    rectangle it claims to be.

    The scenarios therefore vary the two things that decide whether that division
    happens at all, both of them ordinary public API: the **size** of the
    component, and the **transform** it is painted through. Large and small, plain
    and rotated, whole and fractional scale — every one of them has to match the
    same hand drawn reference. Which of them SwingTree chooses to divide is a
    performance trade-off free to change; that you cannot tell from the pixels is
    the promise.

''')
@Subject([UI, SwingTree])
@Timeout(value = 90, unit = TimeUnit.SECONDS)
class Rounded_Fill_Equivalence_Spec extends Specification
{
    /** The one colour every scenario fills with, so the hand drawn reference is unambiguous. */
    private static final Color FILL = new Color(0x1e, 0x5a, 0x8a)

    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        SwingTree.get().setUiScaleFactor(1f)
        ComponentExtension.updateAllCachesFromLibraryConfig() // Every scenario starts with empty caches.
    }

    def cleanup() {
        SwingTree.clear()
    }

    def 'A rounded component is exactly the rounded rectangle it describes. (#description)'(
        String description, int width, int height, int arc, Closure styler
    ) {
        reportInfo """
            Two things are varied here. The **size**, because a large rounded fill is worth
            rasterizing in pieces and a small one is not, so the two sizes of each style take
            different routes to the same pixels. And the **kind of fill**: a plain background
            colour, and a *gradient between one colour and itself* - which looks identical but
            is not treated identically, because a gradient makes its layer depend on the
            component bounds and so denies it every size independent shortcut the style engine
            has. Between them the two cover both routes, and both must land on the reference.
        """
        given : 'A styled component at that size.'
            var box = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            box.setSize(width, height)

        and : """
            And the reference: the very same rounded rectangle, filled the ordinary way with
            one call to AWT, on an image of the same size and rendering hints the component is
            painted onto.
        """
            var reference   = Utility.createDeterministicImage(width, height)
            var referenceG2d = Utility.createDeterministicGraphics(reference)
            referenceG2d.setColor(FILL)
            referenceG2d.fill(new RoundRectangle2D.Float(0, 0, width, height, arc, arc))
            referenceG2d.dispose()

        when : 'The component is painted, and we look for the worst deviating colour channel:'
            var painted = Utility.renderSingleComponent(box)
            int worstChannelDelta = 0
            for ( int y = 0; y < height; y++ )
                for ( int x = 0; x < width; x++ )
                    for ( int shift : [0, 8, 16, 24] ) { // blue, green, red and alpha
                        int delta = Math.abs(
                                        ((reference.getRGB(x, y) >> shift) & 0xff) -
                                        ((painted.getRGB(x, y)   >> shift) & 0xff)
                                    )
                        worstChannelDelta = Math.max(worstChannelDelta, delta)
                    }

        then : 'Not one channel of one pixel deviates from the plain fill, alpha included:'
            worstChannelDelta <= 1

        where :
            description                                  | width | height | arc | styler
            "a flat fill, large"                         | 400   | 300    | 24  | { it.borderRadius(24).backgroundColor(FILL) }
            "a flat fill, small"                         | 120   |  90    | 24  | { it.borderRadius(24).backgroundColor(FILL) }
            "a gradient of one repeated colour, large"   | 400   | 300    | 24  | { it.borderRadius(24)
                                                                                       .gradient("g", g -> g.colors(FILL, FILL)
                                                                                                            .clipTo(UI.ComponentArea.BODY)) }
            "a gradient of one repeated colour, small"   | 120   |  90    | 24  | { it.borderRadius(24)
                                                                                       .gradient("g", g -> g.colors(FILL, FILL)
                                                                                                            .clipTo(UI.ComponentArea.BODY)) }
            "a gentle arc on a wide component"           | 500   | 320    |  6  | { it.borderRadius(6)
                                                                                       .gradient("g", g -> g.colors(FILL, FILL)
                                                                                                            .clipTo(UI.ComponentArea.BODY)) }
            "an arc larger than half the component"      | 240   | 120    | 200 | { it.borderRadius(200)
                                                                                       .gradient("g", g -> g.colors(FILL, FILL)
                                                                                                            .clipTo(UI.ComponentArea.BODY)) }
            "a tall component"                           | 160   | 420    | 30  | { it.borderRadius(30)
                                                                                       .gradient("g", g -> g.colors(FILL, FILL)
                                                                                                            .clipTo(UI.ComponentArea.BODY)) }
    }

    def 'A rounded component survives being painted through a transform. (#description)'(
        String description, Closure transformer
    ) {
        reportInfo """
            Rasterizing a rounded fill in pieces means placing rectangles, and rectangles can
            only stand in for parts of the shape while the pieces still meet exactly on whole
            device pixels. A scale, a translation or a quarter turn all move where those seams
            land, and a corner radius which halves to something fractional can put them between
            two pixels - at which point a component would come out with a visible seam across
            it, or with its corners a pixel adrift from its sides.

            So the same comparison is made again through each of those transforms, against a
            reference drawn through the identical transform. The component has to look like its
            rounded rectangle however it is painted.

            Two transforms are deliberately absent: an arbitrary rotation and a shear. A
            SwingTree component painted through those already differs wholesale from a plain
            fill of the same shape, for reasons that have nothing to do with this - measured, a
            worst channel deviation of 255 - so a comparison against this reference would be
            asserting something the library has never promised rather than anything about how a
            rounded fill is rasterized.
        """
        given : """
            The style caches turned off, through the public cache mode. This is what puts the
            destination's own transform in front of the fill: with a cache to render into, the
            fill happens inside that image under no transform at all, and what reaches the
            destination is a blit of the finished image - which under a scale or a rotation is a
            resampling of it, a different picture by design and not what is being asked about here.
        """
            SwingTree.get().setCacheMode(SwingTreeInitConfig.CacheMode.DISABLED)
            ComponentExtension.updateAllCachesFromLibraryConfig()

        and : 'A styled component, and the rounded rectangle it describes.'
            var arc   = 21 // Deliberately odd: halved, it lands between device pixels under a fractional scale.
            var box   = UI.box()
                          .withStyle( it -> it.borderRadius(arc)
                                              .gradient("g", g -> g.colors(FILL, FILL)
                                                                   .clipTo(UI.ComponentArea.BODY)) )
                          .get(JBox)
            box.setSize(300, 200)
            var shape = new RoundRectangle2D.Float(0, 0, 300, 200, arc, arc)

        and : 'The reference, filled the ordinary way through that same transform.'
            var reference = Utility.createDeterministicImage(500, 400)
            var refG = Utility.createDeterministicGraphics(reference)
            transformer(refG)
            refG.setColor(FILL)
            refG.fill(shape)
            refG.dispose()

        when : 'The component is painted through that transform.'
            var painted = Utility.createDeterministicImage(500, 400)
            UI.runNow {
                var g = Utility.createDeterministicGraphics(painted)
                transformer(g)
                box.paint(g)
                g.dispose()
            }

        and : 'We look for the worst deviating colour channel of the whole image:'
            int worstChannelDelta = 0
            for ( int y = 0; y < 400; y++ )
                for ( int x = 0; x < 500; x++ )
                    for ( int shift : [0, 8, 16, 24] ) {
                        int delta = Math.abs(
                                        ((reference.getRGB(x, y) >> shift) & 0xff) -
                                        ((painted.getRGB(x, y)   >> shift) & 0xff)
                                    )
                        worstChannelDelta = Math.max(worstChannelDelta, delta)
                    }

        then : 'It still is the rounded rectangle it describes:'
            worstChannelDelta <= 1

        where :
            description                    | transformer
            "no transform at all"          | { Graphics2D g -> }
            "a translation"                | { Graphics2D g -> g.translate(40, 30) }
            "a whole scale"                | { Graphics2D g -> g.scale(2d, 2d) }
            "a fractional scale"           | { Graphics2D g -> g.scale(1.3d, 1.3d) }
            "a half turn"                  | { Graphics2D g -> g.translate(300, 200); g.rotate(Math.PI) }
            "a quarter turn"               | { Graphics2D g -> g.translate(200, 0); g.rotate(Math.PI / 2) }
    }
}
