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
import java.awt.Color
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
    the full component bounds — gradients, background images, styled
    text — cannot be reconstructed that way and falls back to the
    classic exact-size caching, where every resize re-renders.

    Noise is the interesting exception. Its pixels vary with every pixel
    position, so it can never be stretched — but it is also the one kind
    of style that is nearly free to simply draw again, so instead of
    dragging its whole layer down to exact-size caching it is lifted out
    of the cached image and replayed on top of it. Everything around it in
    that layer keeps resizing for free.

    This specification pins down that boundary purely through the public
    API: it builds ordinary styled components, paints them through the
    regular paint pipeline and observes the cache through
    `ComponentExtension.cacheHitCount(layer)` / `cacheMissCount(layer)` /
    `cachedRendering(layer).isNotEmpty()`. Which styles resize for free and which
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
            // The size is set directly and never through the styler, so that the resizes below take effect.
            var button = UI.button("Tile me").withStyle(conf -> styler(conf)).get(JButton)
            button.setSize(220, 160)
            var ext = ComponentExtension.from(button)
            2.times { Utility.renderSingleComponent(button) }
        expect : 'The size stuck, the cache is populated and the second paint already hit it.'
            button.width == 220 && button.height == 160
            ext.cachedRendering(layer).isNotEmpty()
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
            ext.cachedRendering(layer).isNotEmpty()

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
            A gradient spans the whole component, a background image is placed
            and fitted relative to the component bounds, and styled text is laid
            out within them. Their
            pixels at one size are genuinely different from their pixels at
            another size, so no amount of corner copying and edge stretching
            can reconstruct them. Such styles keep the classic exact-size
            cache key: resizing them re-renders, exactly as it did before
            stretch tiling existed.
        """
        given : 'A styled button, warmed up with two paints at its initial size.'
            // The size is set directly and never through the styler, so that the resizes below take effect.
            var button = UI.button("Tile me").withStyle(conf -> styler(conf)).get(JButton)
            button.setSize(220, 160)
            var ext = ComponentExtension.from(button)
            2.times { Utility.renderSingleComponent(button) }
        expect : 'The cache is populated and serving hits at this stable size.'
            button.width == 220 && button.height == 160
            ext.cachedRendering(layer).isNotEmpty()
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
            "a background image" | UI.Layer.BACKGROUND | { it.borderRadius(10).image(img -> img.image(ICON)) }
            "styled text"        | UI.Layer.CONTENT    | { it.text(t -> t.content("Ninety-nine slices")) }
    }

    def 'While a component resizes, a noise is lifted out so the rest of its layer resizes for free.'()
    {
        reportInfo """
            Noise cannot be stretched: its pixels vary with every pixel position,
            so a corner copied and an edge band stretched would simply be the
            wrong pixels. Baking it into the cached image would therefore drag
            the *whole* layer down to exact-size caching, and a flat rounded
            background sharing that layer would start re-rendering on every
            frame of a resize just for having a texture over it.

            It does not, because noise is also the one kind of style that is
            practically free to draw again. So while the size is changing, the
            noise is lifted out of the cached image and replayed on top of it,
            and everything around it keeps the small size independent exemplar
            it would have had on its own.

            Here a single layer carries a rounded background, a noise over it
            and a shadow over that. Once the drag is under way, the background
            and the shadow are cached on either side of the noise, and no amount
            of further resizing re-renders them.
        """
        given : 'A button whose background layer is a rounded fill, a noise and a shadow.'
            var button =
                UI.button("Tile me")
                  .withStyle( it -> it
                        .borderRadius(16)
                        .backgroundColor("#1e5a8a")
                        .noise(UI.Layer.BACKGROUND, "grain", n -> n.colors("#202020", "#dedede"))
                        .shadow(UI.Layer.BACKGROUND, "glow", s -> s.color("#0a0a14").blurRadius(6))
                  )
                  .get(JButton)
            // Comfortably large, because a noise is only lifted out when it is big enough to be
            // drawn by blitting tiles - see the scenario after the next one.
            button.setSize(500, 300)
            var ext = ComponentExtension.from(button)
        and : 'A drag is started, which is what makes the layer worth cutting in two.'
            2.times { Utility.renderSingleComponent(button) }
            button.setSize(640, 360)
            2.times { Utility.renderSingleComponent(button) }

        expect : 'The layer is cached in two pieces: the part under the noise and the part over it.'
            ext.cachedRendering(UI.Layer.BACKGROUND).size() == 2
        and : 'Both are small exemplars, not full sized renderings of a 640x360 component.'
            ext.cachedRendering(UI.Layer.BACKGROUND).all( image -> image.width < 640 && image.height < 360 )

        when : 'The drag continues through a series of very different sizes.'
            int missesWhenWarm = ext.cacheMissCount(UI.Layer.BACKGROUND)
            var journey = [[700, 400], [400, 500], [900, 320], [501, 301], [500, 300]]
            journey.each { w, h ->
                button.setSize(w, h)
                Utility.renderSingleComponent(button)
                assert button.width == w && button.height == h
            }

        then : 'Not one of those paints re-rendered the style, despite the noise on the layer.'
            ext.cacheMissCount(UI.Layer.BACKGROUND) == missesWhenWarm
        and : 'And the two exemplars are still the ones being painted from.'
            ext.cachedRendering(UI.Layer.BACKGROUND).size() == 2
    }

    def 'Once the size settles again, the noise goes back into a single cached image.'()
    {
        reportInfo """
            Cutting a layer around its noise is a trade, and the currency is
            blits. A cut layer costs a stretch reconstruction per cached part
            plus a tile blit per noise tile, on *every* paint; an uncut one
            costs a single blit of one exact-size image. The cut only comes out
            ahead when that single blit would not have been possible anyway,
            which is precisely when the component is being resized.

            So the cut is not a property of the style but of what is happening
            to the component. A drag turns it on; a short tail of paints at a
            settled size turns it off again, and the layer is one cached image
            once more — which is what almost every paint of a real UI looks
            like, since hovering, focusing, blinking a caret and scrolling all
            happen at an unchanged size.
        """
        given : 'A dragged button with a noise over a rounded background.'
            var button =
                UI.button("Tile me")
                  .withStyle( it -> it
                        .borderRadius(16)
                        .backgroundColor("#1e5a8a")
                        .noise(UI.Layer.BACKGROUND, "grain", n -> n.colors("#202020", "#dedede"))
                  )
                  .get(JButton)
            var ext = ComponentExtension.from(button)
            [[500, 300], [560, 320], [620, 340]].each { w, h ->
                button.setSize(w, h)
                Utility.renderSingleComponent(button)
            }

        expect : """
            Mid-drag what is cached is the small size independent exemplar of the
            fill alone - the noise is not in it, and neither is the component size.
            (There is one image rather than two only because nothing on this style
            sits over the noise, so the part over it has nothing to cache.)
        """
            ext.cachedRendering(UI.Layer.BACKGROUND).size() == 1
            ext.cachedRendering(UI.Layer.BACKGROUND).first().width < 620

        when : 'The drag ends and the button is simply repainted at that size.'
            12.times { Utility.renderSingleComponent(button) }

        then : 'The layer is a single image again, holding the noise along with everything else.'
            ext.cachedRendering(UI.Layer.BACKGROUND).size() == 1
            ext.cachedRendering(UI.Layer.BACKGROUND).first().width == 620

        and : 'And repainting it is served from that image, not re-rendered.'
            int missesWhenSettled = ext.cacheMissCount(UI.Layer.BACKGROUND)
            5.times { Utility.renderSingleComponent(button) }
            ext.cacheMissCount(UI.Layer.BACKGROUND) == missesWhenSettled
            ext.cacheHitCount(UI.Layer.BACKGROUND) >= 5
    }

    def 'A noise is only lifted out when it is big enough to be drawn by blitting tiles.'()
    {
        reportInfo """
            The replayed noise has to be drawn again on every paint, so lifting
            it out only pays while drawing it is cheap — and whether it is cheap
            depends on its size. Above a certain area SwingTree draws a noise by
            blitting a handful of pre-rendered tiles, which the graphics pipeline
            is very good at. Below it, it fills through the per pixel paint
            pipeline instead, which is perfectly fine once into a cached image
            but far too slow to repeat on every paint. Such a noise stays baked
            in, and its layer keeps the classic exact-size caching even mid-drag.

            That crossover is much lower than one might guess — well under the
            size of a single tile — because the two ways of drawing a noise
            remember their work on completely different terms. Tiles are laid
            out in noise space, so they survive any resize; the paint pipeline
            keeps its rasters against the noise's centre, which moves with the
            component, so a resize throws them away.
        """
        given : 'Two buttons with the same rounded background and noise, one small, one large.'
            var styler = { it.borderRadius(16).backgroundColor("#1e5a8a")
                             .noise(UI.Layer.BACKGROUND, "grain", n -> n.colors("#202020", "#dedede")) }
            var small = UI.button("Tile me").withStyle(conf -> styler(conf)).get(JButton)
            var large = UI.button("Tile me").withStyle(conf -> styler(conf)).get(JButton)

        when : 'Both are dragged through a couple of sizes.'
            [[60, 50], [64, 54], [68, 58]].each { w, h ->      // under the crossover
                small.setSize(w, h); Utility.renderSingleComponent(small)
            }
            [[500, 300], [520, 310], [540, 320]].each { w, h -> // over it
                large.setSize(w, h); Utility.renderSingleComponent(large)
            }

        then : 'The large one had its noise lifted out, leaving the rounded fill cached as an exemplar.'
            ComponentExtension.from(large).cachedRendering(UI.Layer.BACKGROUND).size() == 1
            ComponentExtension.from(large).cachedRendering(UI.Layer.BACKGROUND).first().width < 500

        and : 'The small one kept its noise baked in, so its cached image is the whole component.'
            ComponentExtension.from(small).cachedRendering(UI.Layer.BACKGROUND).size() == 1
            ComponentExtension.from(small).cachedRendering(UI.Layer.BACKGROUND).first().width == 68
    }

    def 'A layer which is nothing but a noise is not cached while it resizes.'()
    {
        reportInfo """
            The flip side of lifting a noise out of the cached image: when the
            noise is *all* there is on a layer, what is left to cache is
            nothing, so mid-drag nothing is cached. That is the right outcome
            rather than a missing optimisation — an image holding a copy of the
            noise would have to be re-rendered at every new size, which is
            strictly more work than replaying the noise, which is cheap by
            construction. At a settled size the layer is cached whole again, as
            any other layer is.
        """
        given : 'A button whose background layer carries a noise and nothing else.'
            var button =
                UI.button("Tile me")
                  .withStyle( it -> it.noise(n -> n.colors("#111111", "#eeeeee")) )
                  .get(JButton)
            button.setSize(220, 160)
            var ext = ComponentExtension.from(button)

        when : 'We paint it a few times and then drag it.'
            5.times { Utility.renderSingleComponent(button) }
        then : 'At a settled size it is one cached image, like any other layer.'
            ext.cachedRendering(UI.Layer.BACKGROUND).size() == 1

        when : 'The drag begins, and there is suddenly nothing left to cache.'
            button.setSize(420, 240)
            Utility.renderSingleComponent(button)
        then : 'There is no cached image for that layer.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isEmpty()
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
            var square  = UI.button("Tile me").withStyle({ it.borderWidths(2, 3, 4, 5).borderColors("#8a1e1e", "#1e8a1e", "#1e1e8a", "#8a8a1e") }).get(JButton)
            var rounded = UI.button("Tile me").withStyle({ it.borderWidths(2, 3, 4, 5).borderColors("#8a1e1e", "#1e8a1e", "#1e1e8a", "#8a8a1e").borderRadius(16) }).get(JButton)
            square.setSize(220, 160)
            rounded.setSize(220, 160)
            var squareExt  = ComponentExtension.from(square)
            var roundedExt = ComponentExtension.from(rounded)
            2.times { Utility.renderSingleComponent(square) }
            2.times { Utility.renderSingleComponent(rounded) }
        expect : 'Both are cached and serving hits at their initial size.'
            squareExt.cachedRendering(UI.Layer.BORDER).isNotEmpty()  && squareExt.cacheHitCount(UI.Layer.BORDER)  >= 1
            roundedExt.cachedRendering(UI.Layer.BORDER).isNotEmpty() && roundedExt.cacheHitCount(UI.Layer.BORDER) >= 1

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
            var button = UI.button("Tile me").withStyle({ it.borderRadius(16).margin(6).backgroundColor("#5a8a1e").foundationColor("#f4f0e8") }).get(JButton)
            button.setSize(40, 40)
            var ext = ComponentExtension.from(button)
            2.times { Utility.renderSingleComponent(button) }
        expect :
            button.width == 40 && button.height == 40
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
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
            var button = UI.button("Tile me").withStyle({ it.borderRadius(20).margin(4).backgroundColor("#1e5a8a").foundationColor("#efe9dc") }).get(JButton)
            button.setSize(200, 150)
            var ext = ComponentExtension.from(button)
            2.times { Utility.renderSingleComponent(button) }
        expect :
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
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
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
    }

    def 'The dimensions of the cached rendering reveal how a style is cached: compressed atlas or full size. (#description)'(
        String description, UI.Layer layer, String cachedAs, Closure styler
    ) {
        reportInfo """
            `cachedRendering(layer)` hands out a copy of the actual cached
            image, and its dimensions tell the whole caching story from the
            outside: a stretch tileable style is stored as a small exemplar
            rendering - a compressed texture atlas whose size depends only on
            the style - while every other style is cached at exactly the
            component size. We pin both aspects by painting the same style on
            two differently sized components and comparing the two cached
            images: atlases must be identical in size (and much smaller than
            either component), full size renderings must each track their
            own component.
        """
        given : 'The same style, painted on two differently sized components.'
            var first  = UI.button("Tile me").withStyle(conf -> styler(conf)).get(JButton)
            var second = UI.button("Tile me").withStyle(conf -> styler(conf)).get(JButton)
            first.setSize(400, 300)
            second.setSize(500, 350)
            5.times { Utility.renderSingleComponent(first) }  // Large full-size renderings only get
            5.times { Utility.renderSingleComponent(second) } // allocated after a few warm-up paints.
        and : 'The two cached renderings.'
            var firstImage  = ComponentExtension.from(first).cachedRendering(layer).first()
            var secondImage = ComponentExtension.from(second).cachedRendering(layer).first()

        expect : 'Atlases are size independent and compressed, full size renderings track their component:'
            if ( cachedAs == "compressed atlas" ) {
                assert firstImage.width == secondImage.width && firstImage.height == secondImage.height
                assert firstImage.width < 400 && firstImage.height < 300
            } else {
                assert firstImage.width  == 400 && firstImage.height  == 300
                assert secondImage.width == 500 && secondImage.height == 350
            }

        where :
            description                                    | layer               | cachedAs           | styler
            "flat rounded background"                      | UI.Layer.BACKGROUND | "compressed atlas" | { it.borderRadius(16).backgroundColor("#175d38") }
            "background, foundation and margin"            | UI.Layer.BACKGROUND | "compressed atlas" | { it.borderRadius(20).margin(8).backgroundColor("#5d1738").foundationColor("#f0ead6") }
            "arcless flat colors with a margin"            | UI.Layer.BACKGROUND | "compressed atlas" | { it.margin(6).backgroundColor("#38175d").foundationColor("#e6f0d6") }
            "a different arc for every corner"             | UI.Layer.BACKGROUND | "compressed atlas" | { it.backgroundColor("#5d3817")
                                                                                                            .borderRadiusAt(UI.Corner.TOP_LEFT, 0, 0)
                                                                                                            .borderRadiusAt(UI.Corner.TOP_RIGHT, 8, 8)
                                                                                                            .borderRadiusAt(UI.Corner.BOTTOM_LEFT, 16, 16)
                                                                                                            .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, 24, 24) }
            "uniformly colored rounded border"             | UI.Layer.BORDER     | "compressed atlas" | { it.border(3, "#17385d").borderRadius(14) }
            "per-edge border widths, one color, rounded"   | UI.Layer.BORDER     | "compressed atlas" | { it.borderWidths(1, 2, 3, 4).borderColor("#0f2f4f").borderRadius(12) }
            "per-edge border colors, square corners"       | UI.Layer.BORDER     | "compressed atlas" | { it.borderWidths(2, 2, 2, 2).borderColors("#7a2020", "#207a20", "#20207a", "#7a7a20") }
            "an outset drop shadow"                        | UI.Layer.CONTENT    | "compressed atlas" | { it.shadowColor("#0a0a14").shadowBlurRadius(6).shadowSpreadRadius(2).borderRadius(12) }
            "an inset shadow"                              | UI.Layer.CONTENT    | "compressed atlas" | { it.shadowColor("#141414").shadowBlurRadius(5).shadowIsInset(true).borderRadius(10) }
            "an offset shadow"                             | UI.Layer.CONTENT    | "compressed atlas" | { it.shadowColor("#101018").shadowBlurRadius(4).shadowOffset(3, 5).borderRadius(8) }
            "two named shadows, one in, one out"           | UI.Layer.CONTENT    | "compressed atlas" | { it.borderRadius(14)
                                                                                                            .shadow("out", s -> s.color("#26264a").blurRadius(7).isOutset(true))
                                                                                                            .shadow("in",  s -> s.color("#0e0e16").blurRadius(4).isInset(true)) }
            "soft-UI penumbra shadows"                     | UI.Layer.CONTENT    | "compressed atlas" | { it.borderRadius(28).margin(10)
                                                                                                            .shadow("bright", s -> s.color(new Color(255, 255, 255, 40)).offset(-8, -8).type(UI.ShadowType.PENUMBRA))
                                                                                                            .shadow("dark",   s -> s.color(new Color(0, 0, 0, 110)).offset(4, 4).type(UI.ShadowType.PENUMBRA))
                                                                                                            .shadowBlurRadius(17).shadowSpreadRadius(-5).shadowIsInset(true) }
            "asymmetric margins, widths and arcs"          | UI.Layer.BACKGROUND | "compressed atlas" | { it.backgroundColor("#4f2f0f").margin(1, 2, 3, 4)
                                                                                                            .borderWidths(5, 6, 7, 8).borderColor("#2f4f0f")
                                                                                                            .borderRadiusAt(UI.Corner.TOP_LEFT, 10, 11)
                                                                                                            .borderRadiusAt(UI.Corner.TOP_RIGHT, 12, 13)
                                                                                                            .borderRadiusAt(UI.Corner.BOTTOM_LEFT, 14, 15)
                                                                                                            .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, 16, 17) }
            "a gradient"                                   | UI.Layer.BACKGROUND | "full size"        | { it.borderRadius(10).gradient(g -> g.colors("#b02050", "#2050b0")) }
            "a background image"                           | UI.Layer.BACKGROUND | "full size"        | { it.borderRadius(10).image(img -> img.image(ICON)) }
            "styled text"                                  | UI.Layer.CONTENT    | "full size"        | { it.text(t -> t.content("Full size")) }
            "per-edge border colors with rounded corners"  | UI.Layer.BORDER     | "full size"        | { it.borderWidths(2, 3, 4, 5).borderColors("#6a1010", "#106a10", "#10106a", "#6a6a10").borderRadius(16) }
            "a rounded background poisoned by a gradient"  | UI.Layer.BACKGROUND | "full size"        | { it.borderRadius(16).backgroundColor("#0f4f2f").gradient(g -> g.colors("#903060", "#309060")) }
    }

    def 'Enormous components are cached eagerly when their style fits a compressed atlas.'()
    {
        reportInfo """
            The memory gates of the render cache - the maximum image area worth
            caching and the lazy allocation warm-up which shields short-lived
            styles from paying for an image - are applied to what will actually
            be *allocated*. The compressed atlas turns this on its head: for a
            stretch tileable style the allocation is the small exemplar no
            matter how large the component, so a component far beyond the
            classic size limit is cached immediately, on its very first paint.
            Before stretch tiling, such a component could never be cached at
            all, and its equally sized gradient sibling still cannot: a
            full size rendering of it would blow the memory budget.
        """
        given : 'Two gigantic buttons: one with a tileable style, one with a gradient.'
            var tileable = UI.button("Tile me").withStyle({ it.borderRadius(24).margin(8).backgroundColor("#0b3d2e").foundationColor("#efe8d8") }).get(JButton)
            var gradient = UI.button("Tile me").withStyle({ it.borderRadius(24).gradient(g -> g.colors("#803060", "#306080")) }).get(JButton)
            tileable.setSize(3000, 1500)
            gradient.setSize(3000, 1500)

        when : 'Both are painted a single time.'
            Utility.renderSingleComponent(tileable)
            Utility.renderSingleComponent(gradient)
        then : 'The tileable style is already cached - as a tiny atlas, allocated eagerly on the first paint.'
            ComponentExtension.from(tileable).cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
            ComponentExtension.from(tileable).cachedRendering(UI.Layer.BACKGROUND).first().width < 100

        when : 'Both are painted several more times.'
            6.times { Utility.renderSingleComponent(tileable) }
            6.times { Utility.renderSingleComponent(gradient) }
        then : 'Every one of those paints of the tileable button was served from the cache...'
            ComponentExtension.from(tileable).cacheHitCount(UI.Layer.BACKGROUND) >= 6
        and : '...while the multi-megapixel gradient rendering is over budget and is never cached.'
            ComponentExtension.from(gradient).cachedRendering(UI.Layer.BACKGROUND).isEmpty()
            ComponentExtension.from(gradient).cacheHitCount(UI.Layer.BACKGROUND) == 0
    }

    def 'Eligibility is decided per layer, not per component.'()
    {
        reportInfo """
            A style is not tileable or non-tileable as a whole - each *layer* is
            judged on its own content. A panel with a gradient background and a
            drop shadow is the everyday case: the gradient spans the component
            and cannot be reconstructed, but the shadow lives on its own layer
            and can. So the very same component gets a full size cached rendering
            for its background and a compressed atlas for its shadow, and a
            resize re-renders only the former.
        """
        given : 'One button carrying a gradient background *and* a shadow, warmed up.'
            var button = UI.button("Tile me").withStyle({ it.borderRadius(18)
                                        .gradient(g -> g.colors("#a02050", "#2050a0"))
                                        .shadowColor("#0a0a12").shadowBlurRadius(7).shadowSpreadRadius(2) }).get(JButton)
            button.setSize(300, 200)
            var ext = ComponentExtension.from(button)
            3.times { Utility.renderSingleComponent(button) }

        expect : 'The gradient background is cached at the full component size...'
            ext.cachedRendering(UI.Layer.BACKGROUND).first().width  == 300
            ext.cachedRendering(UI.Layer.BACKGROUND).first().height == 200
        and : '...while the shadow on the content layer is a small atlas.'
            ext.cachedRendering(UI.Layer.CONTENT).first().width  < 300
            ext.cachedRendering(UI.Layer.CONTENT).first().height < 200

        when : 'The button is resized and painted again.'
            int backgroundMisses = ext.cacheMissCount(UI.Layer.BACKGROUND)
            int shadowMisses     = ext.cacheMissCount(UI.Layer.CONTENT)
            button.setSize(420, 260)
            Utility.renderSingleComponent(button)
        then : 'Only the gradient had to be re-rendered; the shadow was reconstructed for free.'
            ext.cacheMissCount(UI.Layer.BACKGROUND) > backgroundMisses
            ext.cacheMissCount(UI.Layer.CONTENT)   == shadowMisses
    }

    def 'A style animation churning through cache keys does not starve other components of caching.'()
    {
        reportInfo """
            Because a tileable style is cached as a tiny atlas, it always clears
            the memory gates and is allocated eagerly, on the very first paint.
            That is exactly what we want for a *stable* style - but an animation
            which shifts a color a little on every frame produces a brand new
            style configuration, and therefore a brand new cache entry, sixty
            times a second.

            The global cache must not fill up with that debris: its keys are held
            weakly, so a frame's entry dies the moment the animation moves on.
            This scenario hammers a component with a hundred distinct styles and
            then checks the two things that would actually hurt - that the entry
            count did not run away, and that an ordinary, stable component can
            still get itself cached afterwards.
        """
        given : 'A button whose background color is read from a variable the "animation" nudges every frame.'
            var frame  = new java.util.concurrent.atomic.AtomicInteger(0)
            var button = UI.button("Tile me").withStyle({ it.borderRadius(16).backgroundColor(new Color(10 + frame.get(), 80, 160)) }).get(JButton)
            button.setSize(240, 120)
            var ext = ComponentExtension.from(button)

        when : 'A hundred frames, each with its own unique style configuration, are painted.'
            (1..100).each { i ->
                frame.set(i)
                Utility.renderSingleComponent(button)
            }
        then : 'Every frame was a fresh rendering (a new style is a new key, by definition).'
            ext.cacheMissCount(UI.Layer.BACKGROUND) >= 100

        when : 'The garbage collector gets a chance to run.'
            System.gc()
            Thread.sleep(200)
            System.gc()
        then : """
            The debris is gone: a frame's entry is held only by the component which
            painted it, so once the animation moves on to the next frame the entry
            becomes unreachable and the weakly keyed global cache reclaims it. Had
            these entries been strongly held, a long animation would eventually fill
            the cache to its entry ceiling and lock every other component out of it.
        """
            ComponentExtension.globalRenderCacheEntryCounts().toMap()["style layers"] < 100

        when : 'An ordinary component with a stable style is painted afterwards.'
            var stable = UI.button("Tile me").withStyle({ it.borderRadius(12).margin(4).backgroundColor("#1e5a8a").foundationColor("#efe9dc") }).get(JButton)
            stable.setSize(260, 140)
            2.times { Utility.renderSingleComponent(stable) }
        then : 'It is cached and served from the cache, just as if no animation had ever run.'
            ComponentExtension.from(stable).cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
            ComponentExtension.from(stable).cacheHitCount(UI.Layer.BACKGROUND) >= 1
    }

    def 'The compressed atlas is a faithful miniature of the style.'()
    {
        reportInfo """
            The exemplar rendering is not some encoded artifact - it is a plain
            rendering of the very same style on a minimal component, which we
            can verify pixel by pixel through the copy that `cachedRendering`
            hands out: the center must carry the background color and the
            margin band along the outside must carry the foundation color,
            exactly like on the full sized component.
        """
        given : 'A button with an opaque background, foundation and margin, warmed up.'
            var button = UI.button("Tile me").withStyle({ it.borderRadius(10).margin(6).backgroundColor("#204080").foundationColor("#d0c8b8") }).get(JButton)
            button.setSize(400, 300)
            2.times { Utility.renderSingleComponent(button) }
        and : 'Its compressed atlas.'
            var atlas = ComponentExtension.from(button).cachedRendering(UI.Layer.BACKGROUND).first()
        expect : 'The atlas really is the compressed rendering, not the component sized one.'
            atlas.width < 60 && atlas.height < 60
        and : 'Its center pixel carries the background color.'
            new Color(atlas.getRGB((int)(atlas.width/2), (int)(atlas.height/2))) == new Color(0x20, 0x40, 0x80)
        and : 'The outer margin band carries the foundation color, all the way around.'
            new Color(atlas.getRGB((int)(atlas.width/2), 1))              == new Color(0xd0, 0xc8, 0xb8)
            new Color(atlas.getRGB((int)(atlas.width/2), atlas.height-2)) == new Color(0xd0, 0xc8, 0xb8)
            new Color(atlas.getRGB(1, (int)(atlas.height/2)))             == new Color(0xd0, 0xc8, 0xb8)
            new Color(atlas.getRGB(atlas.width-2, (int)(atlas.height/2))) == new Color(0xd0, 0xc8, 0xb8)
    }
}
