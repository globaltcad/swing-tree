package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import swingtree.components.JBox
import swingtree.style.ComponentExtension
import swingtree.threading.EventProcessor
import utility.Utility

import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit

@Title("Rounded Fill Equivalence")
@Narrative('''

    Antialiasing exists to soften the edge of a shape, and a rounded rectangle
    only *has* a soft edge inside its four corners. Everything between them is
    an axis aligned rectangle whose pixels are fully covered, so antialiasing
    changes nothing there — while costing a great deal, because an antialiased
    fill is rasterized in software even when it is drawn onto an accelerated
    surface.

    SwingTree therefore fills a large rounded area as three antialiasing-free
    interior bands plus four antialiased corners. That is a claim about *speed*,
    and it is only allowed to be a claim about speed: the pixels must not move.
    A seam between two of the parts, a corner drawn a pixel too small, or a band
    that reached into the curve would all show up as a visibly harder or softer
    edge on a component the user never asked to look different.

    These scenarios therefore hold one component at one size and paint it twice,
    once with the division forced on and once forced off, and demand that every
    channel of every pixel agree — alpha included, since that is where compositing
    several pieces would go wrong first. The one unit of slack is left only for a
    graphics pipeline that rounds a gradient raster differently between one tiling
    of the work and another.

    Nothing here says *when* SwingTree divides a fill; that is a performance
    trade-off which is free to change. What may never change is that dividing it
    is invisible.

''')
@Subject([UI, SwingTree])
@Timeout(value = 90, unit = TimeUnit.SECONDS)
class Rounded_Fill_Equivalence_Spec extends Specification
{
    def setupSpec() {
        // Pin the cache budget to a deterministic level, independent of the runner's RAM.
        swingtree.style.CacheBudget.UNITS_OVERRIDE = 10
    }

    def cleanupSpec() {
        swingtree.style.CacheBudget.UNITS_OVERRIDE = -1
    }

    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        SwingTree.get().setUiScaleFactor(1f)
        ComponentExtension.updateAllCachesFromLibraryConfig() // Every scenario starts with empty caches.
    }

    def cleanup() {
        swingtree.style.StyleRenderer.SMALLEST_SPLIT_AREA_OVERRIDE = -1
        SwingTree.clear()
    }

    def 'Dividing a rounded fill does not move a single pixel of it. (#description)'(
        String description, int width, int height, Closure styler
    ) {
        reportInfo """
            The style matrix below stresses what the division has to get right: the paints whose
            pixels vary across the shape (a radial and a linear gradient, where a misplaced band
            would show as a discontinuity rather than merely a hard edge), a flat fill, arcs from
            gentle to larger than half the component (where the interior bands collapse to
            nothing and the corners are the whole shape), and wide, tall and square components.
        """
        given : """
            The component painted with the division switched off, i.e. as one antialiased fill
            of the whole rounded shape. This is the reference: it is what the component looked
            like before the division existed.
        """
            swingtree.style.StyleRenderer.SMALLEST_SPLIT_AREA_OVERRIDE = Integer.MAX_VALUE
            var undividedBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            undividedBox.setSize(width, height)
            var undivided = Utility.renderSingleComponent(undividedBox)
            assert undividedBox.width == width && undividedBox.height == height

        and : """
            And an identically styled component painted with the division forced on for any
            size, so that the two differ in nothing but how the fill was tiled into work.
            The caches are emptied in between, or the second component would simply be handed
            the first one's rendered image and the comparison would be vacuous.
        """
            swingtree.style.StyleRenderer.SMALLEST_SPLIT_AREA_OVERRIDE = 0
            ComponentExtension.updateAllCachesFromLibraryConfig()
            var dividedBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            dividedBox.setSize(width, height)
            var divided = Utility.renderSingleComponent(dividedBox)

        when : 'We look for the single worst deviating colour channel of the whole image:'
            int worstChannelDelta = 0
            for ( int y = 0; y < height; y++ )
                for ( int x = 0; x < width; x++ )
                    for ( int shift : [0, 8, 16, 24] ) { // blue, green, red and alpha
                        int delta = Math.abs(
                                        ((undivided.getRGB(x, y) >> shift) & 0xff) -
                                        ((divided.getRGB(x, y)   >> shift) & 0xff)
                                    )
                        worstChannelDelta = Math.max(worstChannelDelta, delta)
                    }

        then : 'Not one channel of one pixel deviates, alpha included:'
            worstChannelDelta <= 1

        where :
            description                            | width | height | styler
            "a radial gradient, wide"              | 400   | 200    | { it.borderRadius(24).backgroundColor("#20303f")
                                                                            .gradient("g", g -> g.type(UI.GradientType.RADIAL)
                                                                                                 .colors("#78b48c", "#1e283c")
                                                                                                 .clipTo(UI.ComponentArea.BODY)) }
            "a linear gradient, tall"              | 200   | 400    | { it.borderRadius(18).backgroundColor("#20303f")
                                                                            .gradient("g", g -> g.span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                                                                                                 .colors("#d14a4a", "#efe6d8")
                                                                                                 .clipTo(UI.ComponentArea.BODY)) }
            "a flat rounded fill"                  | 300   | 300    | { it.borderRadius(20).backgroundColor("#7a4ab1") }
            "a gradient inside a rounded border"   | 360   | 180    | { it.borderRadius(16).border(4, "#101820")
                                                                            .backgroundColor("#2f4f6f")
                                                                            .gradient("g", g -> g.colors("#4ad1a1", "#20303f")
                                                                                                 .clipTo(UI.ComponentArea.BODY)) }
            "an arc wider than half the component" | 240   | 120    | { it.borderRadius(200).backgroundColor("#385d8a")
                                                                            .gradient("g", g -> g.colors("#a0d0ff", "#102030")
                                                                                                 .clipTo(UI.ComponentArea.BODY)) }
            "a barely rounded large component"     | 500   | 320    | { it.borderRadius(3).backgroundColor("#385d8a")
                                                                            .gradient("g", g -> g.type(UI.GradientType.RADIAL)
                                                                                                 .colors("#a0d0ff", "#102030")
                                                                                                 .clipTo(UI.ComponentArea.BODY)) }
            "a margin around the rounded body"     | 360   | 240    | { it.borderRadius(22).margin(9).backgroundColor("#8a5d38")
                                                                            .foundationColor("#efe6d8")
                                                                            .gradient("g", g -> g.colors("#ffd9a0", "#402810")
                                                                                                 .clipTo(UI.ComponentArea.BODY)) }
    }

    def 'A fill is not divided where the parts could not be placed. (#description)'(
        String description, Closure transformer
    ) {
        reportInfo """
            The interior bands are rectangles, and they are only interchangeable with the whole
            fill while the destination axes still line up with the component's own and the cut
            lines between the parts land on whole device pixels. Under a rotation or a shear
            they do neither: a naive division would paint the bands axis aligned and the
            component would come out partly unrotated, with the corners in the right place and
            everything between them in the wrong one.

            So SwingTree quietly abandons the division for such a paint. We pin that by painting
            through such a transform with the division forced on for every size, and demanding
            the result be bit identical - not merely similar - to the same paint with the
            division switched off, which is only possible if it never divided anything.

            Note the cache budget being taken away: with a layer cache to render into, the fill
            would happen inside that image, where the transform is the identity and none of
            these transforms is ever seen. Only a direct rendering puts the destination's own
            transform in front of the fill, which is the thing being guarded against here.
        """
        given : 'The reference: painted through the transform with the division switched off.'
            var styler = { it.borderRadius(21).backgroundColor("#3f6fa1")
                             .gradient("g", g -> g.colors("#a0d0ff", "#102030")
                                                  .clipTo(UI.ComponentArea.BODY)) }
            swingtree.style.CacheBudget.UNITS_OVERRIDE = 0 // No cache budget -> always render directly.
            ComponentExtension.updateAllCachesFromLibraryConfig()
            swingtree.style.StyleRenderer.SMALLEST_SPLIT_AREA_OVERRIDE = Integer.MAX_VALUE
            var undividedBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            undividedBox.setSize(300, 200)
            var undivided = Utility.createDeterministicImage(400, 300)
            UI.runNow {
                var g = Utility.createDeterministicGraphics(undivided)
                transformer(g)
                undividedBox.paint(g)
                g.dispose()
            }

        when : 'The same component is painted through that transform with the division forced on.'
            swingtree.style.StyleRenderer.SMALLEST_SPLIT_AREA_OVERRIDE = 0
            ComponentExtension.updateAllCachesFromLibraryConfig()
            var dividedBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            dividedBox.setSize(300, 200)
            var divided = Utility.createDeterministicImage(400, 300)
            UI.runNow {
                var g = Utility.createDeterministicGraphics(divided)
                transformer(g)
                dividedBox.paint(g)
                g.dispose()
            }

        then : 'The pixels are bit identical, so the fallback engaged for every fill.'
            for ( int y = 0; y < undivided.getHeight(); y++ )
                for ( int x = 0; x < undivided.getWidth(); x++ )
                    assert undivided.getRGB(x, y) == divided.getRGB(x, y)

        cleanup :
            swingtree.style.CacheBudget.UNITS_OVERRIDE = 10

        where :
            description  | transformer
            "rotation"   | { Graphics2D g -> g.rotate(Math.toRadians(20), 150, 100) }
            "shear"      | { Graphics2D g -> g.shear(0.2d, 0d) }
            "a fractional scale putting the cut lines between device pixels" |
                           { Graphics2D g -> g.scale(1.3d, 1.3d) }
    }
}
