package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import swingtree.style.ComponentExtension
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.ImageIcon
import javax.swing.JButton
import java.util.concurrent.TimeUnit

@Title("Stretch Tiling Eligibility")
@Narrative('''

    SwingTree caches the rendered output of a component layer's style
    and, where possible, makes that cache *size independent*: instead of
    keying the cached image on the exact component size (which would miss
    on every frame of a live resize), it renders one small "exemplar" of
    the style and reconstructs any actual size from it by copying the four
    corners and stretching the edge bands and the center — a "nine slice",
    like Android 9-patch images or CSS border-image slicing.

    Stretching is only truthful for styles which look the same along their
    edges no matter the component size: flat background and foundation
    colors, borders and shadows. Content whose pixels genuinely depend on
    the full component bounds — gradients, noise, background images,
    styled text — cannot be reconstructed that way and falls back to the
    classic exact-size caching, where every resize re-renders.

    This specification pins down that boundary purely through the public
    API: it builds ordinary styled components, paints them through the
    regular paint pipeline and observes the cache through
    `ComponentExtension.cacheHitCount(layer)` / `cacheMissCount(layer)` /
    `hasCachedRendering(layer)`. Which styles resize for free and which
    re-render is the user visible requirement; how the machinery decides
    is deliberately not referenced anywhere in here.

    See also: `Style_Render_Caching_Spec` for the caching fundamentals and
    `Cache_Configuration_Spec` for the runtime safety hatch
    `SwingTree.get().setCacheTilingEnabled(..)`.

''')
@Subject([ComponentExtension, UI, SwingTree])
@Timeout(value = 45, unit = TimeUnit.SECONDS)
class Stretch_Tiling_Eligibility_Spec extends Specification
{
    /** A stable icon instance, so that repeated style gatherings produce equal style configurations. */
    private static final ImageIcon ICON = new ImageIcon(Utility.createDeterministicImage(8, 8))

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

    /** A styled button; note that the size is *not* part of the style, it is set
     *  through `setSize` so that the resizes below actually take effect. */
    private static JButton buttonWith( Closure styler ) {
        return UI.button("Tile me").withStyle(conf -> styler(conf)).get(JButton)
    }

    def 'Resizing does not re-render a style whose edges look the same at every size. (#description)'(
        String description, UI.Layer layer, Closure styler
    ) {
        reportInfo """
            Flat colors, borders and shadows all share one property: along any
            edge of the component their pixels form a constant band, so a larger
            component just has *more* of the same band, not different pixels.
            These styles are stretch tileable, and the observable promise is:
            once the cache is warm, resizing produces cache hits, never a fresh
            rendering.
        """
        given : 'A styled button, warmed up with two paints at its initial size.'
            var button = buttonWith(styler)
            button.setSize(220, 160)
            var ext = ComponentExtension.from(button)
            2.times { Utility.renderSingleComponent(button) }
        expect : 'The size stuck, the cache is populated and the second paint already hit it.'
            button.width == 220 && button.height == 160
            ext.hasCachedRendering(layer)
            ext.cacheHitCount(layer) >= 1

        when : 'The button grows substantially and is painted again.'
            int missesBeforeResize = ext.cacheMissCount(layer)
            int hitsBeforeResize   = ext.cacheHitCount(layer)
            button.setSize(420, 240)
            Utility.renderSingleComponent(button)
        then : 'The resize took effect...'
            button.width == 420 && button.height == 240
        and : '...and the paint at the new size was still served from the cache.'
            ext.cacheMissCount(layer) == missesBeforeResize
            ext.cacheHitCount(layer)  >  hitsBeforeResize
            ext.hasCachedRendering(layer)

        where :
            description                               | layer               | styler
            "flat rounded background and foundation"  | UI.Layer.BACKGROUND | { it.borderRadius(16).margin(6).backgroundColor("#385d8a").foundationColor("#eae6da") }
            "uniformly colored rounded border"        | UI.Layer.BORDER     | { it.border(3, "#233240").borderRadius(16) }
            "a different arc for every corner"        | UI.Layer.BACKGROUND | { it.backgroundColor("#8a5d38")
                                                                                   .borderRadiusAt(UI.Corner.TOP_LEFT, 0, 0)
                                                                                   .borderRadiusAt(UI.Corner.TOP_RIGHT, 8, 8)
                                                                                   .borderRadiusAt(UI.Corner.BOTTOM_LEFT, 16, 16)
                                                                                   .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, 24, 24) }
            "per-edge border colors, square corners"  | UI.Layer.BORDER     | { it.borderWidths(1, 2, 3, 4).borderColors("#a03030", "#30a030", "#3030a0", "#a0a030") }
            "an offset drop shadow"                   | UI.Layer.CONTENT    | { it.shadowColor("#101010").shadowBlurRadius(6).shadowSpreadRadius(2).shadowOffset(2, 3).borderRadius(12) }
            "an inset shadow"                         | UI.Layer.CONTENT    | { it.shadowColor("#242424").shadowBlurRadius(4).shadowIsInset(true).borderRadius(10) }
    }

    def 'A style whose pixels depend on the full component size re-renders on every resize. (#description)'(
        String description, UI.Layer layer, Closure styler
    ) {
        reportInfo """
            A gradient spans the whole component, noise varies with every pixel
            position, a background image is placed and fitted relative to the
            component bounds, and styled text is laid out within them. Their
            pixels at one size are genuinely different from their pixels at
            another size, so no amount of corner copying and edge stretching
            can reconstruct them. Such styles keep the classic exact-size
            cache key: resizing them re-renders, exactly as it did before
            stretch tiling existed.
        """
        given : 'A styled button, warmed up with two paints at its initial size.'
            var button = buttonWith(styler)
            button.setSize(220, 160)
            var ext = ComponentExtension.from(button)
            2.times { Utility.renderSingleComponent(button) }
        expect : 'The cache is populated and serving hits at this stable size.'
            button.width == 220 && button.height == 160
            ext.hasCachedRendering(layer)
            ext.cacheHitCount(layer) >= 1

        when : 'The button is resized and painted again.'
            int missesBeforeResize = ext.cacheMissCount(layer)
            int hitsBeforeResize   = ext.cacheHitCount(layer)
            button.setSize(420, 240)
            Utility.renderSingleComponent(button)
        then : 'The new size required a fresh rendering, not a cache hit.'
            button.width == 420 && button.height == 240
            ext.cacheMissCount(layer) >  missesBeforeResize
            ext.cacheHitCount(layer)  == hitsBeforeResize

        where :
            description          | layer               | styler
            "a gradient"         | UI.Layer.BACKGROUND | { it.borderRadius(10).gradient(g -> g.colors("#c81e46", "#1e46c8")) }
            "a noise texture"    | UI.Layer.BACKGROUND | { it.borderRadius(10).noise(n -> n.colors("#111111", "#eeeeee")) }
            "a background image" | UI.Layer.BACKGROUND | { it.borderRadius(10).image(img -> img.image(ICON)) }
            "styled text"        | UI.Layer.CONTENT    | { it.text(t -> t.content("Ninety-nine slices")) }
    }

    def 'Per edge border colors tolerate resizing only with square corners.'()
    {
        reportInfo """
            When each border edge has its own color, the edges meet in diagonal
            miter seams, like on a picture frame. With square corners those
            seams live entirely inside the fixed corner regions. But when the
            corners are *rounded*, the seams reach through the rounded arc
            towards the component center, and their slope follows the component
            aspect ratio — so the corner pixels themselves change with the
            component size. Rounded per-edge-colored borders therefore keep
            the classic behavior and re-render on resize, while square ones
            resize for free.
        """
        given : 'Two buttons with per-edge border colors, one square, one rounded, both warmed up.'
            var square  = buttonWith({ it.borderWidths(2, 3, 4, 5).borderColors("#8a1e1e", "#1e8a1e", "#1e1e8a", "#8a8a1e") })
            var rounded = buttonWith({ it.borderWidths(2, 3, 4, 5).borderColors("#8a1e1e", "#1e8a1e", "#1e1e8a", "#8a8a1e").borderRadius(16) })
            square.setSize(220, 160)
            rounded.setSize(220, 160)
            var squareExt  = ComponentExtension.from(square)
            var roundedExt = ComponentExtension.from(rounded)
            2.times { Utility.renderSingleComponent(square) }
            2.times { Utility.renderSingleComponent(rounded) }
        expect : 'Both are cached and serving hits at their initial size.'
            squareExt.hasCachedRendering(UI.Layer.BORDER)  && squareExt.cacheHitCount(UI.Layer.BORDER)  >= 1
            roundedExt.hasCachedRendering(UI.Layer.BORDER) && roundedExt.cacheHitCount(UI.Layer.BORDER) >= 1

        when : 'Both buttons are resized and painted again.'
            int squareMisses  = squareExt.cacheMissCount(UI.Layer.BORDER)
            int roundedMisses = roundedExt.cacheMissCount(UI.Layer.BORDER)
            square.setSize(420, 240)
            rounded.setSize(420, 240)
            Utility.renderSingleComponent(square)
            Utility.renderSingleComponent(rounded)
        then : 'The square cornered button was reconstructed from the cache...'
            squareExt.cacheMissCount(UI.Layer.BORDER) == squareMisses
        and : '...while the rounded one needed a fresh rendering.'
            roundedExt.cacheMissCount(UI.Layer.BORDER) > roundedMisses
    }

    def 'Small components keep the classic exact size caching until they grow large enough.'()
    {
        reportInfo """
            Reconstructing a component from corner tiles and stretched bands
            needs room: the component must be strictly larger than the
            style's minimal exemplar (all four corner regions plus a band to
            stretch), otherwise the corners would overlap. Below that
            style-dependent minimal size, components keep the classic
            behavior — every size is its own cache entry and resizing
            re-renders. Once the component grows past the minimal size,
            resizing becomes free.
        """
        given : 'A tiny button with a comparatively heavy style, warmed up.'
            var button = buttonWith({ it.borderRadius(16).margin(6).backgroundColor("#5a8a1e").foundationColor("#f4f0e8") })
            button.setSize(40, 40)
            var ext = ComponentExtension.from(button)
            2.times { Utility.renderSingleComponent(button) }
        expect :
            button.width == 40 && button.height == 40
            ext.hasCachedRendering(UI.Layer.BACKGROUND)
            ext.cacheHitCount(UI.Layer.BACKGROUND) >= 1

        when : 'The tiny button is resized ever so slightly, staying tiny.'
            int missesWhileTiny = ext.cacheMissCount(UI.Layer.BACKGROUND)
            button.setSize(44, 44)
            Utility.renderSingleComponent(button)
        then : 'That small resize required a fresh rendering — the classic exact-size behavior.'
            ext.cacheMissCount(UI.Layer.BACKGROUND) > missesWhileTiny

        when : 'The button grows well past the minimal reconstructable size and is painted once...'
            button.setSize(220, 160)
            Utility.renderSingleComponent(button)
        and : '...after which it is resized again.'
            int missesWhileLarge = ext.cacheMissCount(UI.Layer.BACKGROUND)
            button.setSize(260, 180)
            Utility.renderSingleComponent(button)
        then : 'Now resizing no longer re-renders: the style crossed into size independent caching.'
            button.width == 260 && button.height == 180
            ext.cacheMissCount(UI.Layer.BACKGROUND) == missesWhileLarge
    }

    def 'A warm style survives any sequence of resizes without ever re-rendering.'()
    {
        reportInfo """
            Size independence is not just 'the next resize is free': *every*
            size maps onto one and the same cached exemplar rendering, so a
            component can grow, shrink, stretch to extreme aspect ratios and
            return to its original size — and the style renderer is never
            invoked again. This is what makes live window resizing cheap,
            where dozens of sizes fly by in a second.
        """
        given : 'A styled button, warmed up at its initial size.'
            var button = buttonWith({ it.borderRadius(20).margin(4).backgroundColor("#1e5a8a").foundationColor("#efe9dc") })
            button.setSize(200, 150)
            var ext = ComponentExtension.from(button)
            2.times { Utility.renderSingleComponent(button) }
        expect :
            ext.hasCachedRendering(UI.Layer.BACKGROUND)
            ext.cacheHitCount(UI.Layer.BACKGROUND) >= 1

        when : 'The button is dragged through wildly different sizes and painted at every one of them.'
            int missesWhenWarm = ext.cacheMissCount(UI.Layer.BACKGROUND)
            var journey = [[500, 150], [200, 600], [3000, 200], [201, 151], [200, 150]]
            journey.each { w, h ->
                button.setSize(w, h)
                Utility.renderSingleComponent(button)
                assert button.width == w && button.height == h
            }
        then : 'Not a single one of those paints re-rendered the style.'
            ext.cacheMissCount(UI.Layer.BACKGROUND) == missesWhenWarm
            ext.hasCachedRendering(UI.Layer.BACKGROUND)
    }
}
