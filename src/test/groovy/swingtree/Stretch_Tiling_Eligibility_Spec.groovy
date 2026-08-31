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
    the full component bounds — background images, styled text — cannot
    be reconstructed that way and falls back to the classic exact-size
    caching, where every resize re-renders.

    A gradient sits in between. One running straight down a component
    varies from top to bottom, but every pixel strip along its y axis is
    identical, so we can stretch it sideways even though we cannot
    stretch it downwards. We cache such a style with one dimension
    compacted and the other taken from the component, so widening it is
    free and changing its height re-renders. A gradient running
    diagonally, radially or conically varies in both directions at once
    and stays on the classic exact-size cache.

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
            A radial gradient spans the whole component in both directions, a
            background image is placed and fitted relative to the component
            bounds, and styled text is laid out within them. Their pixels at one
            size are genuinely different from their pixels at another size, so no
            amount of corner copying and edge stretching can reconstruct them.
            Such styles keep the classic exact-size cache key: resizing them
            re-renders, exactly as it did before stretch tiling existed.

            Note that the resize below changes *both* dimensions, which is what
            makes it a fair question for a gradient of any kind: one running
            along a single axis is free to stretch across that axis, but never
            along it, so growing both always costs it a re-render too.
        """
        given : 'A styled button, warmed up with two paints at its initial size.'
            var button = buttonWith(styler)
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
            description            | layer               | styler
            "a vertical gradient"  | UI.Layer.BACKGROUND | { it.borderRadius(10).gradient(g -> g.colors("#c81e46", "#1e46c8")) }
            "a radial gradient"    | UI.Layer.BACKGROUND | { it.borderRadius(10).gradient(g -> g.type(UI.GradientType.RADIAL).colors("#c81e46", "#1e46c8")) }
            "a diagonal gradient"  | UI.Layer.BACKGROUND | { it.borderRadius(10).gradient(g -> g.span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT).colors("#c81e46", "#1e46c8")) }
            "a background image"   | UI.Layer.BACKGROUND | { it.borderRadius(10).image(img -> img.image(ICON)) }
            "styled text"          | UI.Layer.CONTENT    | { it.text(t -> t.content("Ninety-nine slices")) }
    }

    def 'A gradient along one axis lets the component grow for free across that axis. (#description)'(
        String description, Closure styler, int grownWidth, int grownHeight
    ) {
        reportInfo """
            A gradient running straight down a component is built from two points
            which share an x coordinate, so its colour depends on how far down a
            pixel is and on nothing else. Every pixel strip along its y axis is
            therefore identical, and widening the component simply needs more of
            them - which is exactly what stretching an edge band does.

            So such a style is cached at a size which is *independent along one
            axis and exact along the other*, and widening it is served from that
            cache without re-rendering. The same argument transposed applies to a
            gradient running left to right, which is free to grow taller.

            This is what makes a horizontal window drag cheap for the many themes
            whose panels and buttons are a vertical gradient - the whole
            Frutiger Aero and skeuomorphic idiom of "a gloss down every raised
            thing" resizes sideways for nothing.
        """
        given : 'A button styled with the gradient, warmed up with two paints at its initial size.'
            var button = buttonWith(styler)
            button.setSize(220, 160)
            var ext = ComponentExtension.from(button)
            2.times { Utility.renderSingleComponent(button) }
        expect : 'The size stuck and the cache is warm.'
            button.width == 220 && button.height == 160
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
            ext.cacheHitCount(UI.Layer.BACKGROUND) >= 1

        when : 'The component grows along the axis the gradient does not vary along.'
            int missesBefore = ext.cacheMissCount(UI.Layer.BACKGROUND)
            int hitsBefore   = ext.cacheHitCount(UI.Layer.BACKGROUND)
            button.setSize(grownWidth, grownHeight)
            Utility.renderSingleComponent(button)
        then : 'The resize took effect...'
            button.width == grownWidth && button.height == grownHeight
        and : '...and the paint at the new size was still served from the cache.'
            ext.cacheMissCount(UI.Layer.BACKGROUND) == missesBefore
            ext.cacheHitCount(UI.Layer.BACKGROUND)  >  hitsBefore

        where :
            description                        | styler                                                                                                              | grownWidth | grownHeight
            "top to bottom, grown wider"       | { it.borderRadius(12).gradient(g -> g.span(UI.Span.TOP_TO_BOTTOM).colors("#c81e46", "#1e46c8")) }                  | 460        | 160
            "bottom to top, grown wider"       | { it.borderRadius(12).gradient(g -> g.span(UI.Span.BOTTOM_TO_TOP).colors("#c81e46", "#1e46c8")) }                  | 460        | 160
            "left to right, grown taller"      | { it.borderRadius(12).gradient(g -> g.span(UI.Span.LEFT_TO_RIGHT).colors("#c81e46", "#1e46c8")) }                  | 220        | 340
            "right to left, grown taller"      | { it.borderRadius(12).gradient(g -> g.span(UI.Span.RIGHT_TO_LEFT).colors("#c81e46", "#1e46c8")) }                  | 220        | 340
            "a gloss over a flat fill, wider"  | { it.borderRadius(12).backgroundColor("#123048").gradient(g -> g.colors(new Color(255,255,255,90), new Color(255,255,255,0))) } | 460 | 160
            "a gradient under a shadow, wider" | { it.borderRadius(12).shadowColor("#0a0a12").shadowBlurRadius(6).gradient(g -> g.colors("#c81e46", "#1e46c8")) }    | 460        | 160
    }

    def 'A gradient along one axis still re-renders when the component grows along that same axis. (#description)'(
        String description, Closure styler, int grownWidth, int grownHeight
    ) {
        reportInfo """
            The counterpart to growing along the free direction, and the reason
            that direction is safe. A
            gradient running down a component genuinely has different pixels at
            every height: its colours are spread over the whole distance, so
            making the component taller does not add more of the same rows, it
            changes every row. That is not something a stretched edge band can
            reconstruct, and the cache does not pretend otherwise - the exact
            height is part of the key, so growing along it re-renders.

            Without this, a resize would smear a gradient across the new height
            instead of redrawing it.
        """
        given : 'A button styled with the gradient, warmed up with two paints at its initial size.'
            var button = buttonWith(styler)
            button.setSize(220, 160)
            var ext = ComponentExtension.from(button)
            2.times { Utility.renderSingleComponent(button) }
        expect : 'The cache is warm and serving hits at this settled size.'
            ext.cacheHitCount(UI.Layer.BACKGROUND) >= 1

        when : 'The component grows along the very axis the gradient varies along.'
            int missesBefore = ext.cacheMissCount(UI.Layer.BACKGROUND)
            int hitsBefore   = ext.cacheHitCount(UI.Layer.BACKGROUND)
            button.setSize(grownWidth, grownHeight)
            Utility.renderSingleComponent(button)
        then : 'The new size required a fresh rendering, not a cache hit.'
            button.width == grownWidth && button.height == grownHeight
            ext.cacheMissCount(UI.Layer.BACKGROUND) >  missesBefore
            ext.cacheHitCount(UI.Layer.BACKGROUND)  == hitsBefore

        where :
            description                    | styler                                                                                             | grownWidth | grownHeight
            "top to bottom, grown taller"  | { it.borderRadius(12).gradient(g -> g.span(UI.Span.TOP_TO_BOTTOM).colors("#c81e46", "#1e46c8")) } | 220        | 340
            "bottom to top, grown taller"  | { it.borderRadius(12).gradient(g -> g.span(UI.Span.BOTTOM_TO_TOP).colors("#c81e46", "#1e46c8")) } | 220        | 340
            "left to right, grown wider"   | { it.borderRadius(12).gradient(g -> g.span(UI.Span.LEFT_TO_RIGHT).colors("#c81e46", "#1e46c8")) } | 460        | 160
            "right to left, grown wider"   | { it.borderRadius(12).gradient(g -> g.span(UI.Span.RIGHT_TO_LEFT).colors("#c81e46", "#1e46c8")) } | 460        | 160
    }

    def 'A gradient which varies in two directions at once is never reconstructed. (#description)'(
        String description, Closure styler
    ) {
        reportInfo """
            We only compact an axis when the gradient's colour depends on the
            other coordinate alone. This pins every way of failing that
            condition: a radial or conic gradient varies with
            the distance or the angle from a point and so depends on both
            coordinates; a diagonal span varies along both by construction; a
            rotation turns an axis aligned span into a diagonal one; and
            measuring the gradient from the component centre derives its extent
            from the component size itself.

            Every one of these keeps the classic exact-size cache, so even
            growing along a single axis re-renders, although that is the cheap
            direction for a gradient which varies along one axis only. Letting
            any of them through would not raise an error; it would paint a
            smeared picture, which is why they are all listed here by name.
        """
        given : 'A button styled with the gradient, warmed up with two paints.'
            var button = buttonWith(styler)
            button.setSize(220, 160)
            var ext = ComponentExtension.from(button)
            2.times { Utility.renderSingleComponent(button) }
        expect : 'The cache is warm at this settled size.'
            ext.cacheHitCount(UI.Layer.BACKGROUND) >= 1

        when : 'The component grows in width alone, the direction a vertical gradient would get for free.'
            int missesBefore = ext.cacheMissCount(UI.Layer.BACKGROUND)
            button.setSize(460, 160)
            Utility.renderSingleComponent(button)
        then : 'It still had to be rendered again.'
            button.width == 460 && button.height == 160
            ext.cacheMissCount(UI.Layer.BACKGROUND) > missesBefore

        where :
            description                          | styler
            "radial"                             | { it.borderRadius(12).gradient(g -> g.type(UI.GradientType.RADIAL).colors("#c81e46", "#1e46c8")) }
            "conic"                              | { it.borderRadius(12).gradient(g -> g.type(UI.GradientType.CONIC).colors("#c81e46", "#1e46c8")) }
            "diagonal, top left to bottom right" | { it.borderRadius(12).gradient(g -> g.span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT).colors("#c81e46", "#1e46c8")) }
            "diagonal, bottom left to top right" | { it.borderRadius(12).gradient(g -> g.span(UI.Span.BOTTOM_LEFT_TO_TOP_RIGHT).colors("#c81e46", "#1e46c8")) }
            "vertical but rotated"               | { it.borderRadius(12).gradient(g -> g.span(UI.Span.TOP_TO_BOTTOM).rotation(37f).colors("#c81e46", "#1e46c8", "#46c81e")) }
            "vertical but measured from centre"  | { it.borderRadius(12).padding(12).gradient(g -> g.span(UI.Span.TOP_TO_BOTTOM).boundary(UI.ComponentBoundary.CENTER_TO_CONTENT).colors("#c81e46", "#1e46c8")) }
            "one down, one across the component" | { it.borderRadius(12)
                                                        .gradient("a", g -> g.span(UI.Span.TOP_TO_BOTTOM).colors(new Color(200,30,70,160), new Color(30,70,200,160)))
                                                        .gradient("b", g -> g.span(UI.Span.LEFT_TO_RIGHT).colors(new Color(30,200,70,120), new Color(200,200,30,120))) }
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

    def 'A noise is lifted out even when the layer under it is only free along one axis.'()
    {
        reportInfo """
            Cutting a layer around its noise is worth doing whenever the piece under the
            noise stays cached through a drag, and that piece does not have to stay cached in
            both directions. A layer whose background is a gradient running straight down it
            has its width compacted and its height taken from the component, so widening it is
            served from the cache while changing its height is not.

            Cutting such a layer buys cache hits whenever the drag is sideways, and costs a
            second cache entry when it is not. Requiring the piece to stay cached along *both*
            axes before cutting would be the tidier looking rule, and would turn every frame of
            the drag below back into a re-render.
        """
        given : 'A button whose background layer is a gradient down the component, a noise and a shadow.'
            var button =
                UI.button("Tile me")
                  .withStyle( it -> it
                        .borderRadius(16)
                        .gradient(UI.Layer.BACKGROUND, "gloss", g -> g.span(UI.Span.TOP_TO_BOTTOM).colors("#1e5a8a", "#8a5a1e"))
                        .noise(UI.Layer.BACKGROUND, "grain", n -> n.colors("#202020", "#dedede"))
                        .shadow(UI.Layer.BACKGROUND, "glow", s -> s.color("#0a0a14").blurRadius(6))
                  )
                  .get(JButton)
            button.setSize(500, 300)
            var ext = ComponentExtension.from(button)
        and : 'A drag is started along the axis the gradient does not vary along.'
            2.times { Utility.renderSingleComponent(button) }
            button.setSize(640, 300)
            2.times { Utility.renderSingleComponent(button) }

        expect : 'The layer was cut, and the piece under the noise is a compact exemplar.'
            button.width == 640 && button.height == 300
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
            ext.cachedRendering(UI.Layer.BACKGROUND).all( image -> image.width < 640 )

        when : 'The drag continues through a series of fresh widths.'
            int missesWhenWarm = ext.cacheMissCount(UI.Layer.BACKGROUND)
            int hitsWhenWarm   = ext.cacheHitCount(UI.Layer.BACKGROUND)
            [700, 780, 900, 1010].each { width ->
                button.setSize(width, 300)
                Utility.renderSingleComponent(button)
                assert button.width == width
            }

        then : 'Not one of those paints re-rendered the style, despite the noise on the layer.'
            ext.cacheMissCount(UI.Layer.BACKGROUND) == missesWhenWarm
            ext.cacheHitCount(UI.Layer.BACKGROUND)  == hitsWhenWarm + 4
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

        when : """
            Both are dragged through a couple of sizes. The small one ends at 55x55, which is
            3025 pixels of noise - comfortably below the crossover - while still being much
            larger than the roughly 38x38 exemplar this style would have, so it is the noise
            area deciding here and not the component being too small to map onto an exemplar
            (which the scenario after this one is about).
        """
            [[45, 45], [50, 50], [55, 55]].each { w, h ->      // under the crossover
                small.setSize(w, h); Utility.renderSingleComponent(small)
            }
            [[500, 300], [520, 310], [540, 320]].each { w, h -> // far over it
                large.setSize(w, h); Utility.renderSingleComponent(large)
            }
        and : """
            Each is then painted once more at the size it arrived at, without moving. A
            rendering tied to a size which is still changing does not commit its image to the
            first component that asks for it - see `Style_Render_Caching_Spec` for why - so
            this second paint is what makes the exact-size image below exist at all. It is
            still too few paints for the size to count as settled, so the noise decision
            this scenario is about is unchanged by it.
        """
            Utility.renderSingleComponent(small)
            Utility.renderSingleComponent(large)

        then : 'The large one had its noise lifted out, leaving the rounded fill cached as an exemplar.'
            ComponentExtension.from(large).cachedRendering(UI.Layer.BACKGROUND).size() == 1
            ComponentExtension.from(large).cachedRendering(UI.Layer.BACKGROUND).first().width < 500

        and : 'The small one kept its noise baked in, so its cached image is the whole component.'
            ComponentExtension.from(small).cachedRendering(UI.Layer.BACKGROUND).size() == 1
            ComponentExtension.from(small).cachedRendering(UI.Layer.BACKGROUND).first().width == 55
    }

    def 'A noise of a single colour is lifted out at any size, because it is only a fill.'()
    {
        reportInfo """
            The size rule of the previous scenario is really a rule about *cost*:
            a noise may be lifted out of its layer only while drawing it again is
            cheap, and for a noise made of several colours that means being big
            enough to be drawn by blitting pre-rendered tiles.

            A noise configured with a single colour never reaches that decision.
            There is nothing to interpolate between, so SwingTree draws it as one
            plain fill of the noise's area — which is cheaper than the tile blits
            the large case was allowed for, at every size. Such a noise is
            therefore lifted out however small it is, and the rest of its layer
            gets to keep the size independent exemplar.

            This matters more than a one-colour noise deserves, because it is the
            place where a shortcut in the renderer and a gate in the cache have to
            agree about the same style. A gate reading "is this noise big enough
            for tiles" gets this case backwards in both directions.
        """
        given : """
            Two small buttons with the same rounded background, well under the size at which
            a noise is drawn by blitting tiles. One carries a noise of two colours, the other
            the very same noise with a single colour.
        """
            var twoColoured =
                UI.button("Tile me")
                  .withStyle( it -> it
                        .borderRadius(16)
                        .backgroundColor("#1e5a8a")
                        .noise(UI.Layer.BACKGROUND, "grain", n -> n.colors("#202020", "#dedede"))
                  )
                  .get(JButton)
            var oneColoured =
                UI.button("Tile me")
                  .withStyle( it -> it
                        .borderRadius(16)
                        .backgroundColor("#1e5a8a")
                        .noise(UI.Layer.BACKGROUND, "grain", n -> n.colors("#202020"))
                  )
                  .get(JButton)

        when : 'Both are dragged through the same few small sizes.'
            [[45, 45], [50, 50], [55, 55]].each { w, h ->
                twoColoured.setSize(w, h); Utility.renderSingleComponent(twoColoured)
                oneColoured.setSize(w, h); Utility.renderSingleComponent(oneColoured)
            }
        and : """
            And each is painted once more without moving, which is what commits an image tied
            to a size that is still changing (see `Style_Render_Caching_Spec`). One paint is
            not enough for the size to count as settled, so nothing about the noise decision
            below changes.
        """
            Utility.renderSingleComponent(twoColoured)
            Utility.renderSingleComponent(oneColoured)

        then : 'The two coloured noise was too small to be worth replaying, so it stayed baked in.'
            ComponentExtension.from(twoColoured).cachedRendering(UI.Layer.BACKGROUND).size() == 1
            ComponentExtension.from(twoColoured).cachedRendering(UI.Layer.BACKGROUND).first().width == 55

        and : 'The single coloured one was lifted out anyway, leaving a size independent exemplar.'
            ComponentExtension.from(oneColoured).cachedRendering(UI.Layer.BACKGROUND).size() == 1
            ComponentExtension.from(oneColoured).cachedRendering(UI.Layer.BACKGROUND).first().width < 55
    }

    def 'A noise is not lifted out of a layer whose rest would not fit a smaller exemplar.'()
    {
        reportInfo """
            Lifting a noise out of its layer only earns anything if what is left
            behind then really does fit a smaller, size independent exemplar,
            because that exemplar is the entire yield of the operation. Merely
            *being the kind of style* that can be stretched is not enough.

            A dimension is compacted only when two things hold at once: the style
            repeats along that dimension, and the component is larger than the
            exemplar in it. The two can name different dimensions, and then
            nothing is compacted at all. The button below carries a gradient
            running from left to right, which paints every pixel strip along the
            x axis the same, so the dimension it lets us compact is the height -
            and the height is exactly where a button 440 pixels wide and 64 tall
            has no room.

            Cutting such a layer would be the worst of both worlds: two exact
            size images instead of one, both re-rendered at every new size, plus
            the noise replayed on top on every single paint. So it is not cut,
            even mid-drag.
        """
        given : 'A wide but short button whose gradient runs across it, over a noise and a shadow.'
            var button =
                UI.button("Grain me")
                  .withStyle( it -> it
                        .borderRadius(30)
                        .backgroundColor("#1e5a8a")
                        .gradient(UI.Layer.BACKGROUND, "sheen", g -> g.span(UI.Span.LEFT_TO_RIGHT).colors("#b02050", "#2050b0"))
                        .noise(UI.Layer.BACKGROUND, "grain", n -> n.colors("#202020", "#dedede"))
                        .shadow(UI.Layer.BACKGROUND, "glow", s -> s.color("#0a0a14").blurRadius(14))
                  )
                  .get(JButton)
            var ext = ComponentExtension.from(button)

        when : 'It is dragged, which is exactly when a layer would be cut around its noise.'
            [[400, 60], [420, 62], [440, 64]].each { w, h ->
                button.setSize(w, h)
                Utility.renderSingleComponent(button)
            }
        and : """
            And painted once more where it came to rest, which is what commits an image tied
            to a size that is still changing (see `Style_Render_Caching_Spec`); one paint is
            far too few for the size to count as settled.
        """
            Utility.renderSingleComponent(button)

        then : 'It was not cut: there is a single cached image, not one per side of the noise.'
            ext.cachedRendering(UI.Layer.BACKGROUND).size() == 1
        and : 'And that image is the plain exact size rendering it would have been anyway.'
            ext.cachedRendering(UI.Layer.BACKGROUND).first().width  == 440
            ext.cachedRendering(UI.Layer.BACKGROUND).first().height == 64

        when : """
            The very same style is dragged on a button turned on its side, where the room and
            the gradient name the same dimension, which is the case the cut exists for.
        """
            var upright =
                UI.button("Grain me")
                  .withStyle( it -> it
                        .borderRadius(30)
                        .backgroundColor("#1e5a8a")
                        .gradient(UI.Layer.BACKGROUND, "sheen", g -> g.span(UI.Span.LEFT_TO_RIGHT).colors("#b02050", "#2050b0"))
                        .noise(UI.Layer.BACKGROUND, "grain", n -> n.colors("#202020", "#dedede"))
                        .shadow(UI.Layer.BACKGROUND, "glow", s -> s.color("#0a0a14").blurRadius(14))
                  )
                  .get(JButton)
            [[60, 400], [62, 420], [64, 440]].each { w, h ->
                upright.setSize(w, h)
                Utility.renderSingleComponent(upright)
            }

        then : 'That one *is* cut, into two images whose heights no longer follow the component.'
            ComponentExtension.from(upright).cachedRendering(UI.Layer.BACKGROUND).size() == 2
            ComponentExtension.from(upright).cachedRendering(UI.Layer.BACKGROUND)
                              .all( image -> image.height < 440 && image.width == 64 )
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

    def 'A rounded border with a color per edge resizes for free when its edges are equally thick.'()
    {
        reportInfo """
            When each border edge has its own color, the edges meet in diagonal
            miter joints, like on a picture frame. A seam between two *adjacent*
            edges leaves the corner in a direction fixed by their two widths, so
            it sits in the same place whatever the component's size, and the
            corner can be copied from an exemplar.

            The seams between two *opposite* edges do move with the size: the one
            dividing the top from the bottom is a horizontal line, and it sits
            proportionally further down the taller the component gets. That is
            harmless while it stays clear of the corners, which it does whenever
            the two widths it divides are equal, since then it runs across the
            middle. Lopsided widths pull it towards the thinner edge, and a
            thin enough edge lets it wander into a corner — at which point the
            corners are no longer size independent and the style has to keep the
            classic behavior of re-rendering on every size.

            "The middle" here means the middle of the box the border encloses,
            not of the component: a margin holds the border away from the
            component's edges, so it is the margin box the seams divide, and a
            margin moves them along with it.
        """
        given : 'Two rounded buttons with per-edge border colors, one with even widths, one lopsided.'
            var even     = buttonWith({ it.borderWidths(4, 4, 4, 4).borderColors("#8a1e1e", "#1e8a1e", "#1e1e8a", "#8a8a1e").borderRadius(16) })
            var lopsided = buttonWith({ it.borderWidths(2, 3, 4, 5).borderColors("#8a1e1e", "#1e8a1e", "#1e1e8a", "#8a8a1e").borderRadius(16) })
            even.setSize(220, 160)
            lopsided.setSize(220, 160)
            var evenExt     = ComponentExtension.from(even)
            var lopsidedExt = ComponentExtension.from(lopsided)
            2.times { Utility.renderSingleComponent(even) }
            2.times { Utility.renderSingleComponent(lopsided) }
        expect : 'Both are cached and serving hits at their initial size.'
            evenExt.cachedRendering(UI.Layer.BORDER).isNotEmpty()     && evenExt.cacheHitCount(UI.Layer.BORDER)     >= 1
            lopsidedExt.cachedRendering(UI.Layer.BORDER).isNotEmpty() && lopsidedExt.cacheHitCount(UI.Layer.BORDER) >= 1

        when : 'Both buttons are resized and painted again.'
            int evenMisses     = evenExt.cacheMissCount(UI.Layer.BORDER)
            int lopsidedMisses = lopsidedExt.cacheMissCount(UI.Layer.BORDER)
            even.setSize(420, 240)
            lopsided.setSize(420, 240)
            Utility.renderSingleComponent(even)
            Utility.renderSingleComponent(lopsided)
        then : 'The evenly bordered button was reconstructed from the cache...'
            evenExt.cacheMissCount(UI.Layer.BORDER) == evenMisses
        and : '...while the lopsided one needed a fresh rendering.'
            lopsidedExt.cacheMissCount(UI.Layer.BORDER) > lopsidedMisses
    }

    def 'Small components keep the classic exact size caching until they grow large enough.'()
    {
        reportInfo """
            Reconstructing a component from corner tiles and stretched bands
            needs room: the component must be strictly larger than the
            style's minimal exemplar (all four corner regions plus a band to
            stretch), otherwise the corners would overlap. The component
            below is smaller than its exemplar in both dimensions, so it
            keeps the classic behavior — every size is its own cache entry
            and resizing re-renders. Once it grows past the minimal size in
            both, resizing becomes free.

            A component with room in one dimension and none in the other is
            compacted in the one it has room in; the scenario after this one
            covers that.
        """
        given : 'A tiny button with a comparatively heavy style, warmed up.'
            var button = buttonWith({ it.borderRadius(16).margin(6).backgroundColor("#5a8a1e").foundationColor("#f4f0e8") })
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

    def 'A component with room to stretch in one dimension only is compacted in that one. (#description)'(
        String description, int width, int height, int siblingWidth, int siblingHeight, String cachedAs, Closure styler
    ) {
        reportInfo """
            Reconstruction needs the component to be larger than the style's minimal
            exemplar, because a dimension the exemplar already fills has nothing left
            to stretch. That is measured per dimension, not for the component as a
            whole: a bar 400 pixels wide and 20 pixels tall has plenty of room across
            and none at all downwards, so its width is compacted down to the exemplar's
            and its height is carried at the component's own 20.

            Two conditions have to hold in a dimension for it to be compacted: the style
            has to repeat along it *and* the component has to be larger than the exemplar
            in it. The two can name different dimensions and then neither is compacted —
            a gradient running across a wide short bar repeats downwards, which is the
            one dimension that bar has no room in, so it is cached at its full size.
        """
        given : 'A styled component of the given size, painted until its cache is warm.'
            var component = buttonWith(styler)
            component.setSize(width, height)
            3.times { Utility.renderSingleComponent(component) }
            var image = ComponentExtension.from(component).cachedRendering(UI.Layer.BACKGROUND).first()

        expect : 'The table really names one of the three ways a rendering can be cached:'
            cachedAs in ["compact width", "compact height", "full size"]
        and : 'The compacted dimension came out smaller than the component, the other one is exactly it:'
            if ( cachedAs == "compact width" ) {
                assert image.width < width && image.height == height
            } else if ( cachedAs == "compact height" ) {
                assert image.height < height && image.width == width
            } else {
                assert image.width == width && image.height == height
            }

        when : 'A sibling of the same style is painted once, at a size differing in one dimension:'
            var sibling = buttonWith(styler)
            sibling.setSize(siblingWidth, siblingHeight)
            Utility.renderSingleComponent(sibling)
        then : 'A compacted style shares what the first component cached; a full size one cannot:'
            if ( cachedAs == "full size" ) {
                assert ComponentExtension.from(sibling).cacheMissCount(UI.Layer.BACKGROUND) >= 1
            } else {
                assert ComponentExtension.from(sibling).cacheMissCount(UI.Layer.BACKGROUND) == 0
                assert ComponentExtension.from(sibling).cacheHitCount(UI.Layer.BACKGROUND) >= 1
            }

        where :
            description                          | width | height | siblingWidth | siblingHeight | cachedAs         | styler
            "a flat bar, wide and short"         | 400   | 20     | 560          | 20            | "compact width"  | { it.borderRadius(10).backgroundColor("#175d38") }
            "a flat bar, tall and narrow"        | 20    | 400    | 20           | 560           | "compact height" | { it.borderRadius(10).backgroundColor("#175d38") }
            "a gradient down a wide short bar"   | 400   | 20     | 560          | 20            | "compact width"  | { it.borderRadius(10).gradient(g -> g.span(UI.Span.TOP_TO_BOTTOM).colors("#b02050", "#2050b0")) }
            "a gradient across a wide short bar" | 400   | 20     | 560          | 20            | "full size"      | { it.borderRadius(10).gradient(g -> g.span(UI.Span.LEFT_TO_RIGHT).colors("#b02050", "#2050b0")) }
            "a bar too small in both dimensions" | 20    | 20     | 24           | 20            | "full size"      | { it.borderRadius(10).backgroundColor("#175d38") }
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

    def 'The dimensions of the cached rendering reveal how a style is cached. (#description)'(
        String description, UI.Layer layer, String cachedAs, Closure styler
    ) {
        reportInfo """
            `cachedRendering(layer)` hands out a copy of the actual cached
            image, and its dimensions tell the whole caching story from the
            outside. There are three stories it can tell:

            A fully tileable style is stored as a small exemplar rendering - a
            compact texture atlas whose size depends only on the style, never
            on the component. A style which repeats along one axis only, such as
            a gradient running straight down the component, is compacted along
            that axis and carries the component's real measurement on the other.
            Everything else is cached at exactly the component size.

            We pin all three by painting the same style on two differently sized
            components and comparing the two cached images: a compacted
            dimension must be identical for both (and much smaller than either
            component), while a dimension that was not compacted must track each
            component's own size.

            Note that a noise sits in the full size group here even though it is
            the one style that gets lifted out of its layer's cached image: that
            only happens while the component is being resized, and these two are
            painted repeatedly at one settled size, which is what a real UI
            spends nearly all of its paints doing.
        """
        given : 'The same style, painted on two differently sized components.'
            var first  = buttonWith(styler)
            var second = buttonWith(styler)
            first.setSize(400, 300)
            second.setSize(500, 350)
            5.times { Utility.renderSingleComponent(first) }  // Large full-size renderings only get
            5.times { Utility.renderSingleComponent(second) } // allocated after a few warm-up paints.
        and : 'The two cached renderings.'
            var firstImage  = ComponentExtension.from(first).cachedRendering(layer).first()
            var secondImage = ComponentExtension.from(second).cachedRendering(layer).first()

        expect : 'The table really names one of the four ways a rendering can be cached:'
            cachedAs in ["compact atlas", "compact width", "compact height", "full size"]
        and : 'Every compacted dimension is size independent, every uncompacted one tracks its component:'
            var compactWidth  = cachedAs in ["compact atlas", "compact width"]
            var compactHeight = cachedAs in ["compact atlas", "compact height"]
            if ( compactWidth ) {
                assert firstImage.width == secondImage.width : "a compact width must not depend on the component"
                assert firstImage.width < 400
            } else {
                assert firstImage.width == 400 && secondImage.width == 500
            }
            if ( compactHeight ) {
                assert firstImage.height == secondImage.height : "a compact height must not depend on the component"
                assert firstImage.height < 300
            } else {
                assert firstImage.height == 300 && secondImage.height == 350
            }

        where :
            description                                    | layer               | cachedAs           | styler
            "flat rounded background"                      | UI.Layer.BACKGROUND | "compact atlas" | { it.borderRadius(16).backgroundColor("#175d38") }
            "background, foundation and margin"            | UI.Layer.BACKGROUND | "compact atlas" | { it.borderRadius(20).margin(8).backgroundColor("#5d1738").foundationColor("#f0ead6") }
            "arcless flat colors with a margin"            | UI.Layer.BACKGROUND | "compact atlas" | { it.margin(6).backgroundColor("#38175d").foundationColor("#e6f0d6") }
            "a different arc for every corner"             | UI.Layer.BACKGROUND | "compact atlas" | { it.backgroundColor("#5d3817")
                                                                                                            .borderRadiusAt(UI.Corner.TOP_LEFT, 0, 0)
                                                                                                            .borderRadiusAt(UI.Corner.TOP_RIGHT, 8, 8)
                                                                                                            .borderRadiusAt(UI.Corner.BOTTOM_LEFT, 16, 16)
                                                                                                            .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, 24, 24) }
            "uniformly colored rounded border"             | UI.Layer.BORDER     | "compact atlas" | { it.border(3, "#17385d").borderRadius(14) }
            "per-edge border widths, one color, rounded"   | UI.Layer.BORDER     | "compact atlas" | { it.borderWidths(1, 2, 3, 4).borderColor("#0f2f4f").borderRadius(12) }
            "per-edge border colors, square corners"       | UI.Layer.BORDER     | "compact atlas" | { it.borderWidths(2, 2, 2, 2).borderColors("#7a2020", "#207a20", "#20207a", "#7a7a20") }
            "per-edge border colors, rounded, even widths" | UI.Layer.BORDER     | "compact atlas" | { it.borderWidths(3, 3, 3, 3).borderColors("#7a2020", "#207a20", "#20207a", "#7a7a20").borderRadius(16) }
            "per-edge border colors, rounded, margined"    | UI.Layer.BORDER     | "compact atlas" | { it.margin(10).borderWidths(12, 4, 10, 4).borderColors("#7a2020", "#207a20", "#20207a", "#7a7a20").borderRadius(8) }
            "an outset drop shadow"                        | UI.Layer.CONTENT    | "compact atlas" | { it.shadowColor("#0a0a14").shadowBlurRadius(6).shadowSpreadRadius(2).borderRadius(12) }
            "an inset shadow"                              | UI.Layer.CONTENT    | "compact atlas" | { it.shadowColor("#141414").shadowBlurRadius(5).shadowIsInset(true).borderRadius(10) }
            "an offset shadow"                             | UI.Layer.CONTENT    | "compact atlas" | { it.shadowColor("#101018").shadowBlurRadius(4).shadowOffset(3, 5).borderRadius(8) }
            "two named shadows, one in, one out"           | UI.Layer.CONTENT    | "compact atlas" | { it.borderRadius(14)
                                                                                                            .shadow("out", s -> s.color("#26264a").blurRadius(7).isOutset(true))
                                                                                                            .shadow("in",  s -> s.color("#0e0e16").blurRadius(4).isInset(true)) }
            "soft-UI penumbra shadows"                     | UI.Layer.CONTENT    | "compact atlas" | { it.borderRadius(28).margin(10)
                                                                                                            .shadow("bright", s -> s.color(new Color(255, 255, 255, 40)).offset(-8, -8).type(UI.ShadowType.PENUMBRA))
                                                                                                            .shadow("dark",   s -> s.color(new Color(0, 0, 0, 110)).offset(4, 4).type(UI.ShadowType.PENUMBRA))
                                                                                                            .shadowBlurRadius(17).shadowSpreadRadius(-5).shadowIsInset(true) }
            "asymmetric margins, widths and arcs"          | UI.Layer.BACKGROUND | "compact atlas" | { it.backgroundColor("#4f2f0f").margin(1, 2, 3, 4)
                                                                                                            .borderWidths(5, 6, 7, 8).borderColor("#2f4f0f")
                                                                                                            .borderRadiusAt(UI.Corner.TOP_LEFT, 10, 11)
                                                                                                            .borderRadiusAt(UI.Corner.TOP_RIGHT, 12, 13)
                                                                                                            .borderRadiusAt(UI.Corner.BOTTOM_LEFT, 14, 15)
                                                                                                            .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, 16, 17) }
            "a gradient down the component"                | UI.Layer.BACKGROUND | "compact width" | { it.borderRadius(10).gradient(g -> g.colors("#b02050", "#2050b0")) }
            "a gradient up the component"                  | UI.Layer.BACKGROUND | "compact width" | { it.borderRadius(10).gradient(g -> g.span(UI.Span.BOTTOM_TO_TOP).colors("#b02050", "#2050b0")) }
            "a gradient across the component"              | UI.Layer.BACKGROUND | "compact height"| { it.borderRadius(10).gradient(g -> g.span(UI.Span.LEFT_TO_RIGHT).colors("#b02050", "#2050b0")) }
            "a gradient across, right to left"             | UI.Layer.BACKGROUND | "compact height"| { it.borderRadius(10).gradient(g -> g.span(UI.Span.RIGHT_TO_LEFT).colors("#b02050", "#2050b0")) }
            "two gradients, both down the component"       | UI.Layer.BACKGROUND | "compact width" | { it.borderRadius(10)
                                                                                                            .gradient("a", g -> g.span(UI.Span.TOP_TO_BOTTOM).colors(new Color(176, 32, 80, 160), new Color(32, 80, 176, 160)))
                                                                                                            .gradient("b", g -> g.span(UI.Span.BOTTOM_TO_TOP).colors(new Color(32, 176, 80, 120), new Color(176, 176, 32, 120))) }
            "two gradients along different axes"           | UI.Layer.BACKGROUND | "full size"        | { it.borderRadius(10)
                                                                                                            .gradient("a", g -> g.span(UI.Span.TOP_TO_BOTTOM).colors(new Color(176, 32, 80, 160), new Color(32, 80, 176, 160)))
                                                                                                            .gradient("b", g -> g.span(UI.Span.LEFT_TO_RIGHT).colors(new Color(32, 176, 80, 120), new Color(176, 176, 32, 120))) }
            "a radial gradient"                            | UI.Layer.BACKGROUND | "full size"        | { it.borderRadius(10).gradient(g -> g.type(UI.GradientType.RADIAL).colors("#b02050", "#2050b0")) }
            "a conic gradient"                             | UI.Layer.BACKGROUND | "full size"        | { it.borderRadius(10).gradient(g -> g.type(UI.GradientType.CONIC).colors("#b02050", "#2050b0")) }
            "a diagonal gradient"                          | UI.Layer.BACKGROUND | "full size"        | { it.borderRadius(10).gradient(g -> g.span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT).colors("#b02050", "#2050b0")) }
            "a rotated vertical gradient"                  | UI.Layer.BACKGROUND | "full size"        | { it.borderRadius(10).gradient(g -> g.span(UI.Span.TOP_TO_BOTTOM).rotation(37f).colors("#b02050", "#2050b0", "#20b050")) }
            "a vertical gradient measured from the center" | UI.Layer.BACKGROUND | "full size"        | { it.borderRadius(10).padding(12).gradient(g -> g.span(UI.Span.TOP_TO_BOTTOM).boundary(UI.ComponentBoundary.CENTER_TO_CONTENT).colors("#b02050", "#2050b0")) }
            "a noise texture at a settled size"            | UI.Layer.BACKGROUND | "full size"        | { it.borderRadius(10).noise(n -> n.colors("#202020", "#dedede")) }
            "a background image"                           | UI.Layer.BACKGROUND | "full size"        | { it.borderRadius(10).image(img -> img.image(ICON)) }
            "styled text"                                  | UI.Layer.CONTENT    | "full size"        | { it.text(t -> t.content("Full size")) }
            "per-edge colors, rounded, lopsided widths"    | UI.Layer.BORDER     | "full size"        | { it.borderWidths(2, 3, 4, 5).borderColors("#6a1010", "#106a10", "#10106a", "#6a6a10").borderRadius(16) }
            "a rounded background under a radial gradient" | UI.Layer.BACKGROUND | "full size"        | { it.borderRadius(16).backgroundColor("#0f4f2f").gradient(g -> g.type(UI.GradientType.RADIAL).colors("#903060", "#309060")) }
            "a rounded background under a down gradient"   | UI.Layer.BACKGROUND | "compact width" | { it.borderRadius(16).backgroundColor("#0f4f2f").gradient(g -> g.colors("#903060", "#309060")) }
    }

    def 'Enormous components are cached eagerly when their style fits a compact atlas.'()
    {
        reportInfo """
            The memory gates of the render cache - the maximum image area worth
            caching and the lazy allocation warm-up which shields short-lived
            styles from paying for an image - are applied to what will actually
            be *allocated*. The compact atlas turns this on its head: for a
            stretch tileable style the allocation is the small exemplar no
            matter how large the component, so a component far beyond the
            classic size limit is cached immediately, on its very first paint.
            Before stretch tiling, such a component could never be cached at
            all, and its equally sized radial gradient sibling still cannot: a
            full size rendering of it would blow the memory budget. The foil has
            to be a *radial* gradient, because one running straight down the
            component compacts along its width and would comfortably fit the
            budget - which the scenario after this one is about.
        """
        given : 'Two gigantic buttons: one with a tileable style, one with a radial gradient.'
            var tileable = buttonWith({ it.borderRadius(24).margin(8).backgroundColor("#0b3d2e").foundationColor("#efe8d8") })
            var gradient = buttonWith({ it.borderRadius(24).gradient(g -> g.type(UI.GradientType.RADIAL).colors("#803060", "#306080")) })
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
            judged on its own content. A panel with a radial gradient background
            and a drop shadow is the everyday case: the gradient spans the
            component in both directions and cannot be reconstructed, but the
            shadow lives on its own layer and can. So the very same component
            gets a full size cached rendering for its background and a compact
            atlas for its shadow, and a resize re-renders only the former.
        """
        given : 'One button carrying a radial gradient background *and* a shadow, warmed up.'
            var button = buttonWith({ it.borderRadius(18)
                                        .gradient(g -> g.type(UI.GradientType.RADIAL).colors("#a02050", "#2050a0"))
                                        .shadowColor("#0a0a12").shadowBlurRadius(7).shadowSpreadRadius(2) })
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
            var button = buttonWith({ it.borderRadius(16).backgroundColor(new Color(10 + frame.get(), 80, 160)) })
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
            var stable = buttonWith({ it.borderRadius(12).margin(4).backgroundColor("#1e5a8a").foundationColor("#efe9dc") })
            stable.setSize(260, 140)
            2.times { Utility.renderSingleComponent(stable) }
        then : 'It is cached and served from the cache, just as if no animation had ever run.'
            ComponentExtension.from(stable).cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
            ComponentExtension.from(stable).cacheHitCount(UI.Layer.BACKGROUND) >= 1
    }

    def 'The compact atlas is a faithful miniature of the style.'()
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
            var button = buttonWith({ it.borderRadius(10).margin(6).backgroundColor("#204080").foundationColor("#d0c8b8") })
            button.setSize(400, 300)
            2.times { Utility.renderSingleComponent(button) }
        and : 'Its compact atlas.'
            var atlas = ComponentExtension.from(button).cachedRendering(UI.Layer.BACKGROUND).first()
        expect : 'The atlas really is the compact rendering, not the component sized one.'
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
