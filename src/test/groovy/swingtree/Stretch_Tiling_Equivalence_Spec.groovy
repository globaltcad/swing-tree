package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import swingtree.components.JBox
import swingtree.style.CacheBudget
import swingtree.style.ComponentExtension
import swingtree.threading.EventProcessor
import utility.Utility

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit

@Title("Stretch Tiling Pixel Equivalence")
@Narrative('''

    For styles whose pixels look the same along the component edges no
    matter the size — flat colors, borders and shadows — SwingTree makes
    its render cache *size independent*: it renders one small "exemplar"
    of the style and reconstructs any actual component size from it by
    copying the four corners and stretching the edge bands and the center
    (a "nine slice", like Android 9-patch images or CSS border-image).

    That optimization is only acceptable if it is invisible. Painting a
    component with stretch tiling enabled must produce (practically) the
    same pixels as painting it the classic way, where every size is
    rendered from scratch — otherwise resizing a component would visibly
    change it.

    This specification pins that promise entirely through the public API:
    it builds ordinary styled components, paints them through the regular
    paint pipeline (`JComponent.paint`), and compares the pixels produced
    with stretch tiling enabled against the pixels produced with it
    disabled via the public `SwingTree.get().setCacheTilingEnabled(..)`
    switch (see `Cache_Configuration_Spec` for that safety hatch itself).
    Nothing in here refers to how the reconstruction works internally —
    if the whole backend were rewritten, these scenarios would still
    express the same requirement: both switch positions, same pixels.

''')
@Subject([SwingTree, UI, ComponentExtension])
@Timeout(value = 90, unit = TimeUnit.SECONDS)
class Stretch_Tiling_Equivalence_Spec extends Specification
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
        SwingTree.clear()
    }

    def 'Stretch tiled painting is pixel equivalent to classic painting. (#description)'(
        String description, UI.Layer layer, int width, int height, Closure styler
    ) {
        reportInfo """
            The style matrix below deliberately stresses every ingredient the
            reconstruction has to get right: corner arcs (uniform, per-corner
            and extreme), margins (uniform and asymmetric), border widths and
            colors (uniform and per-edge), and shadows (outset, inset, offset
            and multiple named ones) — at wide, tall and large sizes.
        """
        given : 'The component painted the classic way, with stretch tiling disabled:'
            SwingTree.get().setCacheTilingEnabled(false)
            // The box is sized directly and never through the styler, so that the size sticks.
            var classicBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            classicBox.setSize(width, height)
            var classic = Utility.renderSingleComponent(classicBox)
            assert classicBox.width == width && classicBox.height == height
        and : """
            An identically styled box painted with stretch tiling enabled, twice, so that
            it warms the shared exemplar and any further paint of it is served purely from
            that cache. We keep the *live box* around on purpose: the exemplar lives in a
            weakly keyed global pool and survives only while a component still holds its
            cache key, so it has to stay referenced for as long as the sibling below relies
            on the entry being warm.
        """
            SwingTree.get().setCacheTilingEnabled(true)
            var tiledBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            tiledBox.setSize(width, height)
            Utility.renderSingleComponent(tiledBox)
            Utility.renderSingleComponent(tiledBox)
            assert tiledBox.width == width && tiledBox.height == height
        and : """
            Proof that the comparison is not vacuous, i.e. that the style really
            is being cached size independently and not just re-rendered: a
            differently sized sibling finds the shared cache entry already
            populated and is served from it on its very first paint.
        """
            var sibling = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            sibling.setSize(width + 16, height + 12)
            Utility.renderSingleComponent(sibling)
            assert ComponentExtension.from(sibling).cacheMissCount(layer) == 0
            assert ComponentExtension.from(sibling).cacheHitCount(layer)  >= 1

        expect : 'Both switch positions produced practically identical pixels:'
            // `tiledBox` is deliberately painted here, *after* the sibling check: this keeps
            // it (and thus the weakly held shared exemplar the sibling relies on) reachable
            // across that check, so the sibling can never race a GC into a cache miss.
            var tiled = Utility.renderSingleComponent(tiledBox)
            Utility.similarityBetween(classic, tiled) >= 99.9

        where :
            description                                 | layer               | width | height | styler
            "rounded background and foundation, wide"   | UI.Layer.BACKGROUND | 400   | 80     | { it.backgroundColor("#d14a4a").foundationColor("#1a1d22").borderRadius(12).margin(5) }
            "a different arc for every corner, tall"    | UI.Layer.BACKGROUND | 80    | 400    | { it.backgroundColor("#4ad1a1")
                                                                                                     .borderRadiusAt(UI.Corner.TOP_LEFT, 0, 0)
                                                                                                     .borderRadiusAt(UI.Corner.TOP_RIGHT, 8, 8)
                                                                                                     .borderRadiusAt(UI.Corner.BOTTOM_LEFT, 16, 16)
                                                                                                     .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, 24, 24) }
            "uniformly colored rounded border"          | UI.Layer.BORDER     | 400   | 100    | { it.border(3, "#202430").borderRadius(16) }
            "per-edge border colors, square corners"    | UI.Layer.BORDER     | 300   | 150    | { it.borderWidths(1, 2, 3, 4).borderColors("#a03030", "#30a030", "#3030a0", "#a0a030") }
            "outset drop shadow with offset"            | UI.Layer.CONTENT    | 350   | 120    | { it.shadowColor("#101010").shadowBlurRadius(6).shadowSpreadRadius(2).shadowOffset(2, 3).borderRadius(12) }
            "inset shadow"                              | UI.Layer.CONTENT    | 350   | 120    | { it.shadowColor("#242424").shadowBlurRadius(4).shadowIsInset(true).borderRadius(10) }
            "two named shadows, one in, one out"        | UI.Layer.CONTENT    | 320   | 300    | { it.borderRadius(14)
                                                                                                     .shadow("halo",  s -> s.color("#3a3a5c").blurRadius(8).spreadRadius(1).isOutset(true))
                                                                                                     .shadow("inner", s -> s.color("#14141c").blurRadius(5).isInset(true)) }
            "asymmetric margins, border widths and arcs"| UI.Layer.BACKGROUND | 500   | 300    | { it.backgroundColor("#b18f4a").foundationColor("#26221c")
                                                                                                     .margin(1, 2, 3, 4)
                                                                                                     .borderWidths(5, 6, 7, 8).borderColor("#223344")
                                                                                                     .borderRadiusAt(UI.Corner.TOP_LEFT, 10, 11)
                                                                                                     .borderRadiusAt(UI.Corner.TOP_RIGHT, 12, 13)
                                                                                                     .borderRadiusAt(UI.Corner.BOTTOM_LEFT, 14, 15)
                                                                                                     .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, 16, 17) }
            "strongly asymmetric margin"                | UI.Layer.BACKGROUND | 400   | 160    | { it.backgroundColor("#4a8fd1").foundationColor("#20242a").margin(0, 0, 0, 20).borderRadius(8) }
            "big margin, huge radius"                   | UI.Layer.BACKGROUND | 500   | 300    | { it.backgroundColor("#c19a3f").borderRadius(32).margin(10) }
    }

    def 'For a flat, arcless style the reconstruction is exactly pixel identical.'()
    {
        reportInfo """
            Without corner arcs and shadows there is no antialiasing anywhere,
            so we can demand bit-exact equality instead of a similarity
            threshold — which pins the reconstruction much harder than any
            threshold could (no off-by-one tile boundaries, no interpolation
            artifacts, no color drift).
        """
        given : 'A flat background with a foundation frame, painted the classic way, with stretch tiling disabled:'
            var styler = { it.backgroundColor("#4a6fb1").foundationColor("#404040").margin(1, 2, 3, 4) }
            SwingTree.get().setCacheTilingEnabled(false)
            var classicBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            classicBox.setSize(300, 200)
            var classic = Utility.renderSingleComponent(classicBox)
        and : 'And the same style painted with stretch tiling enabled, twice, so the second paint is cache served:'
            SwingTree.get().setCacheTilingEnabled(true)
            var tiledBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            tiledBox.setSize(300, 200)
            Utility.renderSingleComponent(tiledBox)
            var tiled = Utility.renderSingleComponent(tiledBox)
        expect :
            classic.getWidth()  == tiled.getWidth()
            classic.getHeight() == tiled.getHeight()
        and : 'Every single pixel is identical:'
            for ( int y = 0; y < classic.getHeight(); y++ )
                for ( int x = 0; x < classic.getWidth(); x++ )
                    assert classic.getRGB(x, y) == tiled.getRGB(x, y)
    }

    def 'The two switch positions agree at every size across the range where tiling begins to apply.'()
    {
        reportInfo """
            Size independent caching only applies to components strictly larger
            than the style's minimal exemplar; below that, painting falls back
            to the classic behavior. The exact threshold is an internal detail,
            so instead of referring to it we bracket it from the outside: we
            walk a range of small sizes that safely straddles it for this style
            and demand pixel equivalence at every step. This covers the
            trickiest sizes of all — the ones where the stretched bands are
            just a single pixel wide.
        """
        given :
            var styler = { it.backgroundColor("#7a4ab1").foundationColor("#efe6d8").borderRadius(16).margin(6) }
        expect : 'Classic and stretch tiled painting agree at every size in the bracket:'
            for ( var size : [[44, 44], [48, 48], [52, 52], [56, 56], [60, 60], [64, 64], [96, 52], [52, 96]] ) {
                SwingTree.get().setCacheTilingEnabled(false)
                var classicBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
                classicBox.setSize(size[0], size[1])
                var classic = Utility.renderSingleComponent(classicBox)

                SwingTree.get().setCacheTilingEnabled(true)
                var tiledBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
                tiledBox.setSize(size[0], size[1])
                Utility.renderSingleComponent(tiledBox) // The first paint warms the shared exemplar,
                var tiled = Utility.renderSingleComponent(tiledBox) // ...so this one is served from it.

                assert Utility.similarityBetween(classic, tiled) >= 99.9
            }
    }

    def 'Painting under a HiDPI scaling transform produces no seams and no drift. (scale #scale)'(
        double scale
    ) {
        reportInfo """
            On HiDPI displays the destination graphics carries a scaling
            transform (typically 1.25x, 1.5x or 2x). This is dangerous
            territory for any kind of tiled painting: if the tiles rounded
            their device pixel edges independently, fractional scales would
            produce one pixel gaps or double blended overlaps where tiles
            meet. So besides comparing against classic painting, we scan the
            reconstruction of a fully opaque style for transparent seams
            along its center lines, which cross every internal tile boundary.
        """
        given : 'An opaque rounded style and the device buffer dimensions for this scale:'
            var styler = { it.backgroundColor("#3f6fa1").borderRadius(12) }
            int width = 300, height = 140
            int deviceWidth  = Math.ceil(width * scale) as int
            int deviceHeight = Math.ceil(height * scale) as int
        and : 'The classic rendering, painted through a scaling transform:'
            SwingTree.get().setCacheTilingEnabled(false)
            var classicBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            classicBox.setSize(width, height)
            var classic = paintScaled(classicBox, deviceWidth, deviceHeight, scale)
        and : 'The stretch tiled rendering, painted the same way, twice, so it is cache served:'
            SwingTree.get().setCacheTilingEnabled(true)
            var box = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            box.setSize(width, height)
            paintScaled(box, deviceWidth, deviceHeight, scale)
            var tiled = paintScaled(box, deviceWidth, deviceHeight, scale)
            assert ComponentExtension.from(box).cacheHitCount(UI.Layer.BACKGROUND) >= 1

        expect : 'No transparent seam anywhere along the horizontal center line:'
            int centerY = (int) (deviceHeight / 2)
            for ( int x = 1; x < (int) Math.floor(width * scale) - 1; x++ )
                assert ((tiled.getRGB(x, centerY) >> 24) & 0xFF) == 255
        and : 'No transparent seam anywhere along the vertical center line:'
            int centerX = (int) (deviceWidth / 2)
            for ( int y = 1; y < (int) Math.floor(height * scale) - 1; y++ )
                assert ((tiled.getRGB(centerX, y) >> 24) & 0xFF) == 255
        and : 'The scaled reconstruction stays true to the scaled classic painting:'
            Utility.similarityBetween(classic, tiled) >= 99.5

        where :
            scale << [1.0d, 1.25d, 1.5d, 2.0d]
    }

    def 'A rotated or sheared graphics context is painted directly, never reconstructed. (#description)'(
        String description, Closure transformer
    ) {
        reportInfo """
            Reassembling a component from nine tiles only makes sense while the
            destination axes still line up with the component's own: the tiles
            are rectangles, and they are placed by snapping their edges to whole
            device pixels. Under a rotation, a shear or a flip that placement is
            meaningless - a naive implementation would happily paint the nine
            rectangles axis aligned and the component would come out *unrotated*.

            So whenever the graphics transform is not a plain positive scale and
            translation, SwingTree quietly abandons the cache for that paint and
            renders the style directly at the real component size. We pin this by
            painting through such a transform and demanding the result be
            *bit identical* to the same paint with caching switched off entirely -
            which is only possible if the very same direct rendering ran.
        """
        given : 'The reference: the transformed component painted with all caching switched off.'
            var styler = { it.backgroundColor("#3f6fa1").foundationColor("#1c2026").borderRadius(14).margin(5) }
            CacheBudget.UNITS_OVERRIDE = 0 // no cache budget at all -> always render directly
            var uncachedBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            uncachedBox.setSize(300, 200)
            var uncached = paintTransformed(uncachedBox, transformer)

        when : 'The same component is painted through that transform with stretch tiling fully enabled.'
            CacheBudget.UNITS_OVERRIDE = 10
            SwingTree.get().setCacheTilingEnabled(true)
            ComponentExtension.updateAllCachesFromLibraryConfig()
            var box = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            box.setSize(300, 200)
            var ext = ComponentExtension.from(box)
            paintTransformed(box, transformer)
            var tiled = paintTransformed(box, transformer)

        then : 'Not a single one of those paints was served from the cache - the fallback engaged every time.'
            ext.cacheHitCount(UI.Layer.BACKGROUND) == 0
            ext.cacheMissCount(UI.Layer.BACKGROUND) >= 2

        and : 'And the pixels are bit identical to the uncached direct rendering.'
            for ( int y = 0; y < uncached.getHeight(); y++ )
                for ( int x = 0; x < uncached.getWidth(); x++ )
                    assert uncached.getRGB(x, y) == tiled.getRGB(x, y)

        cleanup :
            CacheBudget.UNITS_OVERRIDE = 10

        where :
            description  | transformer
            "rotation"   | { Graphics2D g -> g.rotate(Math.toRadians(20), 150, 100) }
            "shear"      | { Graphics2D g -> g.shear(0.2d, 0d) }
            "vertical flip" | { Graphics2D g -> g.translate(0, 200); g.scale(1d, -1d) }
    }

    def 'A component which shrinks back below the reconstructable size returns to exact size rendering.'()
    {
        reportInfo """
            A component only gets reconstructed from tiles while it is strictly
            larger than the style's minimal exemplar. But a component does not
            only *grow* - a split pane divider or a collapsing panel can drag it
            back down below that threshold, and it has to survive the trip back
            without painting garbage (the tiles would have to overlap to fit).

            So we take a component up into stretch tiled territory, then shrink
            it far below the threshold, and demand that its pixels still match
            the classic rendering of that small size exactly - and that it is now
            cached at its real size again, not as an atlas.
        """
        given : 'A styled box which is first grown large (so it gets stretch tiled)...'
            var styler = { it.backgroundColor("#7a4ab1").foundationColor("#efe6d8").borderRadius(16).margin(6) }
            SwingTree.get().setCacheTilingEnabled(true)
            var box = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            box.setSize(400, 300)
            var ext = ComponentExtension.from(box)
            2.times { Utility.renderSingleComponent(box) }
        expect : 'It really is cached as a small atlas at this point.'
            ext.cachedRendering(UI.Layer.BACKGROUND).first().width < 400

        when : '...and is then dragged back down to a size far below the reconstructable minimum.'
            box.setSize(30, 24)
            Utility.renderSingleComponent(box)
            var shrunk = Utility.renderSingleComponent(box)
        then : 'The size took effect, and the style is cached at that real size again - the atlas is gone.'
            box.width == 30 && box.height == 24
            ext.cachedRendering(UI.Layer.BACKGROUND).first().width  == 30
            ext.cachedRendering(UI.Layer.BACKGROUND).first().height == 24
        and : 'Its pixels are exactly the classic rendering of a component which was never anything else.'
            SwingTree.get().setCacheTilingEnabled(false)
            var classicBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            classicBox.setSize(30, 24)
            var classic = Utility.renderSingleComponent(classicBox)
            for ( int y = 0; y < classic.getHeight(); y++ )
                for ( int x = 0; x < classic.getWidth(); x++ )
                    assert classic.getRGB(x, y) == shrunk.getRGB(x, y)
    }

    def 'Stretch tiled painting survives the accelerated graphics pipeline, even for extremely long edges.'()
    {
        reportInfo """
            Everything above paints into plain software images. Real
            applications paint onto *accelerated* window surfaces, where
            cached images gain GPU resident copies and image drawing takes
            completely different code paths inside the JDK. On the XRender
            pipeline (the Linux default) this once mattered a lot: stretched
            image blits sourced from a sub-rectangle of a larger cached image
            silently broke down at large stretch ratios, so very long
            components lost the shadows along their long edges — while every
            software surface test stayed pixel perfect.

            This scenario is the public regression net for that class of
            defect: it paints a very long, shadowed component onto an
            accelerated VolatileImage repeatedly (repetition matters, because
            the JDK only promotes cached images to the accelerated path after
            a few paints) and checks the result against the classic software
            rendering — including an explicit probe for the long-edge shadow
            pixels that used to vanish.
        """
        given : 'The soft-UI style recipe which first revealed the problem, on a very long panel:'
            int W = 3000, H = 900
            var styler = { it.borderRadius(28).margin(10)
                             .shadow("bright", s -> s.color(new Color(255, 255, 255, 40)).offset(-8, -8).type(UI.ShadowType.PENUMBRA))
                             .shadow("dark",   s -> s.color(new Color(0, 0, 0, 110)).offset(4, 4).type(UI.ShadowType.PENUMBRA))
                             .shadowBlurRadius(17).shadowSpreadRadius(-5).shadowIsInset(true) }
        and : 'The classic software rendering as the reference:'
            SwingTree.get().setCacheTilingEnabled(false)
            var classicBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            classicBox.setSize(W, H)
            var classic = Utility.renderSingleComponent(classicBox)
        and : 'A stretch tiled component, painted repeatedly onto an accelerated VolatileImage:'
            SwingTree.get().setCacheTilingEnabled(true)
            var box = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            box.setSize(W, H)
            var gc = GraphicsEnvironment.localGraphicsEnvironment.defaultScreenDevice.defaultConfiguration
            var volatileDestination = gc.createCompatibleVolatileImage(W, H, Transparency.TRANSLUCENT)
            BufferedImage accelerated = null
            UI.runNow {
                8.times {
                    var vg = volatileDestination.createGraphics()
                    vg.setComposite(AlphaComposite.Clear)
                    vg.fillRect(0, 0, W, H)
                    vg.setComposite(AlphaComposite.SrcOver)
                    box.paint(vg)
                    vg.dispose()
                }
                accelerated = volatileDestination.getSnapshot()
            }

        expect : 'The repeated paints really were served from the cache.'
            ComponentExtension.from(box).cachedRendering(UI.Layer.CONTENT).isNotEmpty()
            ComponentExtension.from(box).cacheHitCount(UI.Layer.CONTENT) >= 1
        and : 'The accelerated painting matches the classic software rendering.'
            Utility.similarityBetween(classic, accelerated) >= 99.9
        and : 'The long top edge really contains shadow pixels (exactly what the XRender defect used to erase).'
            (12..44).any { y -> ((accelerated.getRGB((int) (W / 2), y) >> 24) & 0xFF) > 0 }
    }

    /** Paints the component through an arbitrary graphics transform, applied by the
     *  supplied closure right before the component paints itself. */
    private static BufferedImage paintTransformed( java.awt.Component component, Closure transformer ) {
        var image = Utility.createDeterministicImage(400, 300)
        UI.runNow {
            var g = Utility.createDeterministicGraphics(image)
            transformer(g)
            component.paint(g)
            g.dispose()
        }
        return image
    }

    /** Paints the component onto a device sized buffer through a `scale` transform,
     *  the way a HiDPI display surface would. */
    private static BufferedImage paintScaled( java.awt.Component component, int deviceWidth, int deviceHeight, double scale ) {
        var image = Utility.createDeterministicImage(deviceWidth, deviceHeight)
        UI.runNow {
            var g = Utility.createDeterministicGraphics(image)
            g.scale(scale, scale)
            component.paint(g)
            g.dispose()
        }
        return image
    }
}
