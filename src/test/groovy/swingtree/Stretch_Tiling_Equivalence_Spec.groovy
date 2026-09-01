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

    Some style renderings are reconstructible along one axis only: a
    gradient running straight down a component varies from top to bottom,
    but every pixel strip along its y axis is identical, so we can stretch
    it sideways while its height comes from the component itself. Those
    are compacted along one axis only. The requirement stated below
    applies to them exactly as it does to a style compacted in both
    dimensions.

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

    /** A parentless styled box, sized directly (never through the styler, so the size sticks). */
    private static JBox boxWith( int width, int height, Closure styler ) {
        var box = UI.box().withStyle(conf -> styler(conf)).get(JBox)
        box.setSize(width, height)
        return box
    }

    /** Paints a fresh component the classic way: stretch tiling off, so this
     *  size is rendered from scratch by the style renderer. */
    private static BufferedImage renderedClassically( int width, int height, Closure styler ) {
        SwingTree.get().setCacheTilingEnabled(false)
        var box = boxWith(width, height, styler)
        var image = Utility.renderSingleComponent(box)
        assert box.width == width && box.height == height
        return image
    }

    /** Paints a fresh box with stretch tiling on, twice, so it warms the shared exemplar
     *  and any further paint of it is served purely from that cache. Returns the *live box*
     *  on purpose: the exemplar lives in a weakly keyed global pool and survives only while
     *  a component still holds its cache key, so the caller must keep this box referenced for
     *  as long as it relies on the entry being warm (e.g. while a sibling reads it). */
    private static JBox tiledAndWarmed( int width, int height, Closure styler ) {
        SwingTree.get().setCacheTilingEnabled(true)
        var box = boxWith(width, height, styler)
        Utility.renderSingleComponent(box)
        Utility.renderSingleComponent(box)
        assert box.width == width && box.height == height
        return box
    }

    /** The warm, cache-served rendering of a stretch-tiled box (see {@link #tiledAndWarmed}). */
    private static BufferedImage renderedTiled( int width, int height, Closure styler ) {
        return Utility.renderSingleComponent(tiledAndWarmed(width, height, styler))
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
            var classic = renderedClassically(width, height, styler)
        and : 'An identically styled box painted with stretch tiling enabled and warmed into the shared cache:'
            var tiledBox = tiledAndWarmed(width, height, styler)
        and : """
            Proof that the comparison is not vacuous, i.e. that the style really
            is being cached size independently and not just re-rendered: a
            differently sized sibling finds the shared cache entry already
            populated and is served from it on its very first paint.
        """
            var sibling = boxWith(width + 16, height + 12, styler)
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
            "per-edge border colors, rounded, wide"     | UI.Layer.BORDER     | 480   | 140    | { it.borderWidths(4, 4, 4, 4).borderColors("#a03030", "#30a030", "#3030a0", "#a0a030").borderRadius(20) }
            "per-edge border colors, rounded, tall"     | UI.Layer.BORDER     | 140   | 480    | { it.borderWidths(4, 4, 4, 4).borderColors("#a03030", "#30a030", "#3030a0", "#a0a030").borderRadius(20) }
            "per-edge colors, rounded, thin and round"  | UI.Layer.BORDER     | 520   | 180    | { it.borderWidths(2, 2, 2, 2).borderColors("#a03030", "#30a030", "#3030a0", "#a0a030").borderRadius(48) }
            "per-edge colors, rounded, margined"        | UI.Layer.BORDER     | 420   | 200    | { it.margin(10).borderWidths(12, 4, 10, 4).borderColors("#a03030", "#30a030", "#3030a0", "#a0a030").borderRadius(8) }
            "per-edge colors, rounded, side margins"    | UI.Layer.BORDER     | 420   | 200    | { it.margin(2, 8, 2, 8).borderWidths(4, 4, 4, 4).borderColors("#a03030", "#30a030", "#3030a0", "#a0a030").borderRadius(12) }
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

    def 'A style compacted along one dimension only paints what a full rendering paints. (#description)'(
        String description, UI.Layer layer, int width, int height, int siblingWidth, int siblingHeight, Closure styler
    ) {
        reportInfo """
            A style is compacted along one dimension and carried at the component's
            own measurement along the other for two different reasons, and this
            table holds rows for both.

            The first reason is the style. A gradient running down a component
            varies from top to bottom but paints every pixel strip along the y axis
            the same, so it may be stretched sideways and not downwards. Its
            exemplar is a handful of pixels wide and as tall as the component, which
            makes the sideways scale factor larger than anywhere else in this file.

            The second reason is the component. A bar 400 pixels wide and 20 tall is
            larger than its exemplar across and smaller than it downwards, so even a
            flat rounded fill, which repeats in both dimensions, has room to be
            stretched sideways only. The reconstruction then cuts the width alone and
            copies the height one to one.

            The sibling in each row differs only in the compacted dimension, because
            that is the whole claim: such a style may be resized along that one
            dimension without re-rendering, and may not be resized along the other
            without re-rendering. A sibling differing in the other dimension would
            miss the cache and prove nothing.

            What is compared is the worst single colour channel of any single pixel,
            alpha included, rather than an average over the image. An average absorbs
            a seam where two tiles meet and ignores transparency altogether, and a
            miscut is exactly what would produce those.
        """
        given : 'The component painted the classic way, with stretch tiling disabled:'
            var classic = renderedClassically(width, height, styler)
        and : 'An identically styled box painted with stretch tiling enabled and warmed into the shared cache:'
            var tiledBox = tiledAndWarmed(width, height, styler)
        and : """
            Proof that the comparison is not vacuous: a sibling differing only along
            the compacted dimension finds the shared entry already populated and is
            served from it on its first paint.
        """
            var sibling = boxWith(siblingWidth, siblingHeight, styler)
            Utility.renderSingleComponent(sibling)
            assert ComponentExtension.from(sibling).cacheMissCount(layer) == 0
            assert ComponentExtension.from(sibling).cacheHitCount(layer)  >= 1

        expect : 'Not one channel of one pixel deviates between the two switch positions, alpha included:'
            var tiled = Utility.renderSingleComponent(tiledBox)
            Utility.worstChannelDelta(classic, tiled) <= 2

        where :
            description                              | layer               | width | height | siblingWidth | siblingHeight | styler
            "down the component, widened"            | UI.Layer.BACKGROUND | 400   | 160    | 560          | 160           | { it.borderRadius(14).gradient(g -> g.span(UI.Span.TOP_TO_BOTTOM).colors("#d14a4a", "#1a3d6d")) }
            "up the component, widened"              | UI.Layer.BACKGROUND | 400   | 160    | 560          | 160           | { it.borderRadius(14).gradient(g -> g.span(UI.Span.BOTTOM_TO_TOP).colors("#d14a4a", "#1a3d6d")) }
            "across the component, made taller"      | UI.Layer.BACKGROUND | 200   | 340    | 200          | 470           | { it.borderRadius(14).gradient(g -> g.span(UI.Span.LEFT_TO_RIGHT).colors("#d14a4a", "#1a3d6d")) }
            "across right to left, made taller"      | UI.Layer.BACKGROUND | 200   | 340    | 200          | 470           | { it.borderRadius(14).gradient(g -> g.span(UI.Span.RIGHT_TO_LEFT).colors("#d14a4a", "#1a3d6d")) }
            "a gloss over a flat fill, widened"      | UI.Layer.BACKGROUND | 420   | 150    | 610          | 150           | { it.borderRadius(14).backgroundColor("#123048")
                                                                                                                                .gradient(g -> g.colors(new Color(255, 255, 255, 90), new Color(255, 255, 255, 0))) }
            "three colour stops, widened"            | UI.Layer.BACKGROUND | 380   | 200    | 505          | 200           | { it.borderRadius(18).margin(6).gradient(g -> g.colors("#c81e46", "#1e46c8", "#46c81e")) }
            "a gradient under a shadow, widened"     | UI.Layer.BACKGROUND | 400   | 180    | 545          | 180           | { it.borderRadius(16)
                                                                                                                                .shadow("halo", s -> s.color("#0a0a12").blurRadius(7).spreadRadius(2))
                                                                                                                                .gradient(g -> g.colors("#d14a4a", "#1a3d6d")) }
            "a gradient inside a rounded border"     | UI.Layer.BACKGROUND | 440   | 190    | 600          | 190           | { it.borderRadius(20).border(4, "#20242e").margin(5)
                                                                                                                                .gradient(g -> g.colors("#d14a4a", "#1a3d6d")) }
            "an extreme stretch, very wide"          | UI.Layer.BACKGROUND | 260   | 150    | 1400         | 150           | { it.borderRadius(12).gradient(g -> g.colors("#d14a4a", "#1a3d6d")) }
            "a flat bar, wide and short"             | UI.Layer.BACKGROUND | 400   | 20     | 560          | 20            | { it.borderRadius(10).backgroundColor("#123048") }
            "a flat bar, tall and narrow"            | UI.Layer.BACKGROUND | 20    | 400    | 20           | 560           | { it.borderRadius(10).backgroundColor("#123048") }
            "background, foundation and margin, short"| UI.Layer.BACKGROUND| 400   | 24     | 545          | 24            | { it.borderRadius(14).margin(4).backgroundColor("#5d1738").foundationColor("#f0ead6") }
            "a rounded border on a short bar"        | UI.Layer.BORDER     | 400   | 20     | 560          | 20            | { it.border(3, "#20242e").borderRadius(10) }
            "a shadow on a short bar"                | UI.Layer.CONTENT    | 400   | 24     | 560          | 24            | { it.shadowColor("#101010").shadowBlurRadius(6).shadowSpreadRadius(2).borderRadius(12) }
            "a gradient down a short bar"            | UI.Layer.BACKGROUND | 400   | 20     | 560          | 20            | { it.borderRadius(10).gradient(g -> g.span(UI.Span.TOP_TO_BOTTOM).colors("#d14a4a", "#1a3d6d")) }
    }

    def 'A layer cut around its noise paints what the whole layer paints. (#description)'(
        String description, int width, int height, Closure styler
    ) {
        reportInfo """
            A layer carrying a noise is cached differently from every other
            layer: while the component resizes, the noise is lifted out and
            replayed straight onto the destination, and only what sits under and
            over it is cached — as two size independent exemplars rather than
            one exact-size image. Those exemplars are compacted in whichever
            dimensions the rest of the layer allows, so the rows carrying a
            gradient under the noise are compacted in width alone and keep the
            component's own height.

            That is three drawing operations where there used to be one, so it
            is worth demanding that they add up to the same picture. They do,
            because the renderer draws by kind in a fixed order and source-over
            compositing is associative, which makes drawing the pieces one after
            another identical to drawing the layer in one go.

            So this asks for more than an overall resemblance. An average over
            the whole image would happily absorb a one pixel seam where two
            pieces meet, or a corner that came out of the wrong part of an
            exemplar, and it would not look at the alpha channel at all — which
            is exactly where compositing three pieces could go wrong. Every
            single channel of every single pixel is therefore checked, and it
            measures *exactly* equal here; the one unit of slack is left only
            for a graphics pipeline that rounds a blit differently.
        """
        given : 'The component painted the classic way, with stretch tiling disabled:'
            SwingTree.get().setCacheTilingEnabled(false)
            var classicBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            classicBox.setSize(width, height)
            var classic = Utility.renderSingleComponent(classicBox)
        and : """
            An identically styled box which is *dragged* to that size with stretch tiling
            enabled, because that is what makes its layer be cut around the noise. The last
            paint of the drag lands on the very size the classic box was painted at.
        """
            SwingTree.get().setCacheTilingEnabled(true)
            var tiledBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            [[width - 40, height - 20], [width - 20, height - 10], [width, height]].each { w, h ->
                tiledBox.setSize(w, h)
                Utility.renderSingleComponent(tiledBox)
            }
        and : """
            Proof that the comparison is not vacuous: what is cached for that layer is a size
            independent exemplar, which it could only be with the noise lifted out of it.
        """
            var cached = ComponentExtension.from(tiledBox).cachedRendering(UI.Layer.BACKGROUND)
            assert !cached.isEmpty()
            assert cached.all( image -> image.width < width )

        when : 'We look for the single worst deviating colour channel of the whole image:'
            var tiled = Utility.renderSingleComponent(tiledBox)

        then : 'Not one channel of one pixel deviates, alpha included:'
            Utility.worstChannelDelta(classic, tiled) <= 1

        where :
            description                                 | width | height | styler
            "a noise over a rounded background"         | 400   | 200    | { it.backgroundColor("#2f4f6f").borderRadius(14).margin(6)
                                                                              .noise(UI.Layer.BACKGROUND, "grain", n -> n.colors("#101010", "#e0e0e0")) }
            "a noise between a background and a shadow" | 360   | 240    | { it.backgroundColor("#6f4f2f").borderRadius(18)
                                                                              .noise(UI.Layer.BACKGROUND, "grain", n -> n.function(UI.NoiseType.FABRIC).colors("#201000", "#f0e0d0"))
                                                                              .shadow(UI.Layer.BACKGROUND, "glow", s -> s.color("#0a0a14").blurRadius(7).spreadRadius(1)) }
            "a noise over a gradient down the layer"    | 400   | 150    | { it.backgroundColor("#2f4f6f").borderRadius(14)
                                                                              .gradient(UI.Layer.BACKGROUND, "sheen", g -> g.span(UI.Span.TOP_TO_BOTTOM).colors("#1a3d6d", "#6f4f2f"))
                                                                              .noise(UI.Layer.BACKGROUND, "grain", n -> n.colors("#101010", "#e0e0e0")) }
            "a noise over a gradient, under a shadow"   | 360   | 140    | { it.backgroundColor("#4f2f6f").borderRadius(12)
                                                                              .gradient(UI.Layer.BACKGROUND, "sheen", g -> g.span(UI.Span.BOTTOM_TO_TOP).colors("#2f1a4d", "#6f4f2f"))
                                                                              .noise(UI.Layer.BACKGROUND, "grain", n -> n.function(UI.NoiseType.FABRIC).colors("#201000", "#f0e0d0"))
                                                                              .shadow(UI.Layer.BACKGROUND, "glow", s -> s.color("#0a0a14").blurRadius(6).spreadRadius(1)) }
    }

    def 'A layer cut around its painters paints what the whole layer paints. (#description)'(
        String description, UI.Layer layer, int width, int height, Closure styler
    ) {
        reportInfo """
            A user painter is arbitrary code, so SwingTree cannot cache what it draws - and
            because a layer is cached as one rasterization, a single painter used to make the
            *whole* layer uncacheable: every shadow, gradient and fill sharing it was
            re-rendered at full size on every paint. Such a layer is therefore cut in two, the
            painters replayed straight onto the destination on top of a cached image of
            everything else. A painter created with `Painter.of(..)` is the exception, being a
            promise that the painting is a pure function of an immutable value, so it can go
            into that image - and a layer may hold both kinds at once. The cut then falls at
            the first uncacheable painter, so that every painter keeps its position relative to
            every other; the deliberately overlapping rows below are what pin that.

            That turns one drawing operation into two, which is only acceptable if they add up
            to the very same picture. They do, because the renderer draws by kind in a fixed
            order with the painters last, and source-over compositing is associative.

            As with the noise cut, an overall similarity score would be too weak a claim: it
            would absorb a seam, a missing shadow or an alpha channel that composited twice.
            Every channel of every pixel is compared instead, against the same style painted
            with caching switched off entirely - which is the switch position in which the
            layer is drawn whole, in one go, exactly as it was before this cut existed. It
            measures *exactly* equal here; the one unit of slack below is left only for a
            graphics pipeline that rounds a blit differently.
        """
        given : '''
            The component painted whole, with the render cache switched off. Note that this
            has to go through the budget rather than through `setCacheMode(DISABLED)`: the
            `UNITS_OVERRIDE` hook this spec pins in `setupSpec` takes precedence over the
            mode, so setting the mode alone would leave caching (and the cut) fully active and
            compare the cut rendering against itself.
        '''
            CacheBudget.UNITS_OVERRIDE = 0 // A maximally constrained machine: no caching at all.
            var wholeBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            wholeBox.setSize(width, height)
            var whole = Utility.renderSingleComponent(wholeBox)

        and : 'An identically styled box painted with the cache on, so that its layer is cut:'
            CacheBudget.UNITS_OVERRIDE = 10
            var cutBox = UI.box().withStyle(conf -> styler(conf)).get(JBox)
            cutBox.setSize(width, height)
            Utility.renderSingleComponent(cutBox)
            Utility.renderSingleComponent(cutBox)

        and : """
            Proof that the comparison is not vacuous: there is a cached image for that layer
            at all. A layer carrying an uncacheable painter is refused by the cache as a whole,
            so an image can only exist for it if the painters really were cut out of it.
        """
            assert !ComponentExtension.from(cutBox).cachedRendering(layer).isEmpty()

        when : 'We look for the single worst deviating colour channel of the whole image:'
            var cut = Utility.renderSingleComponent(cutBox)

        then : 'Not one channel of one pixel deviates, alpha included:'
            Utility.worstChannelDelta(whole, cut) <= 1

        cleanup : 'The budget goes back to what the rest of this specification expects.'
            // Restored here rather than only in the `given` block above, so that a failure
            // between the two assignments cannot leave every later scenario running uncached.
            CacheBudget.UNITS_OVERRIDE = 10

        where :
            description                            | layer               | width | height | styler
            "a painter over a rounded background"  | UI.Layer.BACKGROUND | 300   | 160    | { it.backgroundColor("#2f4f6f").borderRadius(14).margin(6)
                                                                                               .painter(UI.Layer.BACKGROUND, "mark", { g ->
                                                                                                    g.setColor(new Color(240, 120, 30, 200))
                                                                                                    g.fillOval(20, 20, 60, 40)
                                                                                               }) }
            "overlapping cacheable and lambda painters" | UI.Layer.BACKGROUND | 300 | 160  | { it.backgroundColor("#3f5f2f").borderRadius(12)
                                                                                               .painter(UI.Layer.BACKGROUND, "a-cacheable",
                                                                                                    swingtree.api.Painter.of("k", { g ->
                                                                                                        g.setColor(new Color(240, 120, 30, 190))
                                                                                                        g.fillOval(20, 20, 120, 90)
                                                                                                    }))
                                                                                               .painter(UI.Layer.BACKGROUND, "b-lambda", { g ->
                                                                                                    g.setColor(new Color(30, 200, 160, 150))
                                                                                                    g.fillRect(60, 40, 120, 70)
                                                                                               }) }
            "overlapping lambda painter *first*"   | UI.Layer.BACKGROUND | 300   | 160    | { it.backgroundColor("#3f2f5f").borderRadius(12)
                                                                                               .painter(UI.Layer.BACKGROUND, "a-lambda", { g ->
                                                                                                    g.setColor(new Color(30, 200, 160, 150))
                                                                                                    g.fillRect(60, 40, 120, 70)
                                                                                               })
                                                                                               .painter(UI.Layer.BACKGROUND, "b-cacheable",
                                                                                                    swingtree.api.Painter.of("k2", { g ->
                                                                                                        g.setColor(new Color(240, 120, 30, 190))
                                                                                                        g.fillOval(20, 20, 120, 90)
                                                                                                    })) }
            "a painter over a shadow"              | UI.Layer.CONTENT    | 320   | 180    | { it.backgroundColor("#6f4f2f").borderRadius(18)
                                                                                               .shadow(UI.Layer.CONTENT, "glow", s -> s.color("#0a0a14").blurRadius(7).spreadRadius(1))
                                                                                               .painter(UI.Layer.CONTENT, "mark", { g ->
                                                                                                    g.setColor(new Color(30, 200, 160, 180))
                                                                                                    g.fillRect(40, 30, 90, 50)
                                                                                               }) }
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
        given : 'A flat background with a foundation frame, painted both ways:'
            var styler  = { it.backgroundColor("#4a6fb1").foundationColor("#404040").margin(1, 2, 3, 4) }
            var classic = renderedClassically(300, 200, styler)
            var tiled   = renderedTiled(300, 200, styler)
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
            A dimension is stretched only where the component is larger than the
            style's minimal exemplar in it; where it is not, that dimension is
            painted at the component's own measurement. The exact threshold is an
            internal detail, so instead of referring to it we bracket it from the
            outside: we walk a range of small sizes that safely straddles it for
            this style and demand pixel equivalence at every step. The square
            sizes cross the threshold in both dimensions at once, and the lopsided
            ones cross it in one dimension only. This covers the trickiest sizes
            of all — the ones where the stretched bands are just a single pixel
            wide.
        """
        given :
            var styler = { it.backgroundColor("#7a4ab1").foundationColor("#efe6d8").borderRadius(16).margin(6) }
        expect : 'Classic and stretch tiled painting agree at every size in the bracket:'
            for ( var size : [[44, 44], [48, 48], [52, 52], [56, 56], [60, 60], [64, 64],
                              [96, 52], [52, 96], [96, 44], [44, 96], [96, 48], [48, 96]] ) {
                var classic = renderedClassically(size[0], size[1], styler)
                var tiled   = renderedTiled(size[0], size[1], styler)
                assert Utility.worstChannelDelta(classic, tiled) <= 1 : "at ${size[0]}x${size[1]}"
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
            var classic = paintScaled(boxWith(width, height, styler), deviceWidth, deviceHeight, scale)
        and : 'The stretch tiled rendering, painted the same way, twice, so it is cache served:'
            SwingTree.get().setCacheTilingEnabled(true)
            var box = boxWith(width, height, styler)
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
            var uncached = paintTransformed(boxWith(300, 200, styler), transformer)

        when : 'The same component is painted through that transform with stretch tiling fully enabled.'
            CacheBudget.UNITS_OVERRIDE = 10
            SwingTree.get().setCacheTilingEnabled(true)
            ComponentExtension.updateAllCachesFromLibraryConfig()
            var box = boxWith(300, 200, styler)
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
            var box = boxWith(400, 300, styler)
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
            var classic = renderedClassically(30, 24, styler)
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
            var classic = renderedClassically(W, H, styler)
        and : 'A stretch tiled component, painted repeatedly onto an accelerated VolatileImage:'
            SwingTree.get().setCacheTilingEnabled(true)
            var box = boxWith(W, H, styler)
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
