package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.style.ComponentExtension
import swingtree.threading.EventProcessor
import utility.Utility

import swingtree.components.JBox

import javax.swing.JButton
import javax.swing.JLabel
import java.awt.Color

@Title("Style Render Caching")
@Narrative('''

    SwingTree's style API can express rather elaborate visuals: rounded
    backgrounds, gradients, shadows, painted text, noise, sized images,
    coloured borders, ... rendering all of that on every repaint is not
    free. SwingTree therefore transparently caches the *rendered output*
    of a layer's style configuration into a `BufferedImage`, keyed by the
    deeply-immutable style configuration object itself. As long as the
    style configuration of a layer is `equal` across paint calls, the
    cached image is simply blitted onto the component – no fresh rendering
    happens.

    This specification documents that behaviour **through the public
    SwingTree API only**, by:

      1. Building components the way an application would (`UI.button(...).withStyle(...)`),
      2. Painting them with the regular paint pipeline (`Utility.renderSingleComponent(...)`
         which ultimately calls `JComponent.paint(g)`),
      3. Observing the resulting cache state via `ComponentExtension.cachedRendering(layer).isNotEmpty()`,
         `ComponentExtension.cacheHitCount(layer)` and `ComponentExtension.cacheMissCount(layer)`.

    Crucially, this spec does **not** instantiate any of SwingTree's internal
    style classes directly, nor does it mock a `Graphics2D` to count cache
    side effects. The point is that the caching mechanism is observable
    from the outside and that this living documentation can therefore
    survive aggressive internal refactors.

    A note on the single fixture poke we still do: the `setupSpec` block
    pins `CacheBudget.UNITS_OVERRIDE` to a deterministic value. That global
    controls how eagerly small images get allocated and is otherwise derived
    from the configured cache mode and system RAM, which
    would make tests flaky across CI runners. Pinning it removes the
    *only* coupling and is purely a fixture concern – nothing in the
    actual scenarios below speaks to internal classes.

''')
@Subject([ComponentExtension, UI])
class Style_Render_Caching_Spec extends Specification
{
    def setupSpec() {
        // Pin caching to a deterministic level, see narrative above.
        swingtree.style.CacheBudget.UNITS_OVERRIDE = 10
    }

    def cleanupSpec() {
        swingtree.style.CacheBudget.UNITS_OVERRIDE = -1
    }

    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        SwingTree.get().setUiScaleFactor(1f)
    }

    def cleanup() {
        SwingTree.clear()
    }

    def 'A heavily styled component caches its background after the first paint.'()
    {
        reportInfo """
            Caching only kicks in for layers that contain at least one *heavy*
            style ingredient. A rounded background colour together with a
            non-zero margin is enough to qualify, but in this scenario we go
            for an unmistakably heavy style so that the test passes regardless
            of any threshold tuning: a rounded background, a shadow and an
            explicit size.

            We render the component once, observe that the background layer
            has produced a cached image, then render it again and observe
            that the *next* paint was served from the cache rather than
            re-running the renderer (i.e. the hit count went up while the
            miss count did not).
        """
        given : 'A button styled with a rounded background.'
            var button =
                UI.button("Hello!")
                  .withStyle( it -> it
                        .size(120, 60)
                        .borderRadius(20)
                        .backgroundColor(Color.BLUE)
                        .foundationColor(Color.WHITE)
                  )
                  .get(JButton)
        and : 'We grab the public extension associated with the component.'
            var ext = ComponentExtension.from(button)

        when : 'We render the component once through the regular paint pipeline.'
            Utility.renderSingleComponent(button)
        then : 'The background layer has produced a cached rendering after that first paint.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
        and  : 'The renderer was invoked at least once to produce the cached image.'
            ext.cacheMissCount(UI.Layer.BACKGROUND) >= 1
            ext.cacheHitCount(UI.Layer.BACKGROUND)  == 0

        when : 'We render the same component a second time.'
            int missesBeforeRepaint = ext.cacheMissCount(UI.Layer.BACKGROUND)
            Utility.renderSingleComponent(button)
        then : 'The hit counter went up – the second paint was served from the cache.'
            ext.cacheHitCount(UI.Layer.BACKGROUND) >= 1
        and  : 'And the miss counter did *not* increase – no fresh rendering was needed.'
            ext.cacheMissCount(UI.Layer.BACKGROUND) == missesBeforeRepaint
        and  : 'The cached rendering is, of course, still there.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
    }

    def 'The cache reports a layer as a tuple of images, one per separately cached part.'()
    {
        reportInfo """
            A layer is not limited in how much style it may carry, and not all
            style caches the same way. A flat colour or a shadow is constant
            along the component edges, so it can be kept as one small image and
            stretched back to any size, whereas a gradient or painted text has
            to be kept at the component's real size. When a single layer mixes
            such parts, one image cannot represent both, so `cachedRendering(..)`
            hands back a *tuple*: empty when nothing is cached, one image for the
            ordinary case where the whole layer rasterizes together, and several
            when the cache had to split the layer up. They come in paint order.

            Here we look at the ordinary case, which is what a layer carrying a
            single kind of style produces.
        """
        given : 'A button whose background layer carries one kind of style.'
            var button =
                UI.button("Hello!")
                  .withStyle( it -> it
                        .size(120, 60)
                        .borderRadius(20)
                        .backgroundColor(Color.BLUE)
                        .foundationColor(Color.WHITE)
                  )
                  .get(JButton)
        and : 'We grab the public extension associated with the component.'
            var ext = ComponentExtension.from(button)

        when : 'We paint it through the regular paint pipeline.'
            2.times { Utility.renderSingleComponent(button) }

        then : 'The background layer rasterized into a single cached image.'
            ext.cachedRendering(UI.Layer.BACKGROUND).size() == 1
        and  : 'A layer without any style of its own has nothing to show.'
            ext.cachedRendering(UI.Layer.FOREGROUND).isEmpty()
    }

    def 'The hit and miss counts keep counting when a layer changes how it is cached.'()
    {
        reportInfo """
            `cacheHitCount(..)` and `cacheMissCount(..)` are cumulative for the
            lifetime of a component: together they say how often this component
            painted the layer, split into the paints a cache served and the
            paints it had to render. Nothing that happens to the cache
            underneath may take a recorded paint back, or a monitoring view
            watching these numbers would see a component un-paint itself.

            That is worth pinning down, because a layer is not always cached in
            the same shape. A layer carrying a noise is cached as one image at a
            settled size, but while the component is being resized it is cached
            as the part *under* the noise and the part *over* it, with the noise
            replayed in between, so that everything around it resizes for free.
            Those are different cache entries, entered and left mid-life, and the
            counters have to run straight through it.

            Here a button big enough for that to happen is painted, dragged, and
            left alone again, and we simply count paints.
        """
        given : 'A button with a noise over a rounded background.'
            var button =
                UI.button("Grain me")
                  .withStyle( it -> it
                        .borderRadius(16)
                        .backgroundColor(Color.BLUE)
                        .noise("grain", n -> n.colors(Color.BLACK, Color.WHITE))
                  )
                  .get(JButton)
            button.setSize(400, 240)
            var ext = ComponentExtension.from(button)

        when : 'We paint it three times at a settled size.'
            3.times { Utility.renderSingleComponent(button) }
        then : 'Every one of those paints was counted, as a hit or as a miss.'
            ext.cacheHitCount(UI.Layer.BACKGROUND) + ext.cacheMissCount(UI.Layer.BACKGROUND) == 3
        and  : 'And the cache did serve some of them, so there is something to lose.'
            ext.cacheHitCount(UI.Layer.BACKGROUND) >= 1

        when : 'It is dragged, so the layer is cut around its noise.'
            [[440, 260], [480, 280], [520, 300]].each { w, h ->
                button.setSize(w, h)
                Utility.renderSingleComponent(button)
            }
        then : 'Not one of the earlier paints was forgotten, and the new ones were counted too.'
            ext.cacheHitCount(UI.Layer.BACKGROUND) + ext.cacheMissCount(UI.Layer.BACKGROUND) == 6

        when : 'The drag ends and the layer becomes a single rasterization again.'
            12.times { Utility.renderSingleComponent(button) }
        then : 'Still nothing was taken back.'
            ext.cacheHitCount(UI.Layer.BACKGROUND) + ext.cacheMissCount(UI.Layer.BACKGROUND) == 18
    }

    def 'A paint no cache took part in is counted as a miss, not lost.'()
    {
        reportInfo """
            The counters must add up to the number of paints even for the layer
            that gives the cache the least to work with: one whose entire style
            is a noise.

            While such a component is resized, the noise is lifted out and
            replayed, and what is left under and over it is *nothing at all* —
            so there is nothing to cache, nothing to render, and the layer is
            painted entirely by that replay. It would be easy for such a paint
            to fall between the two counters and simply vanish, and it would be
            wrong to call it a hit, since no cache took part in it. It counts as
            a miss, which is also the honest reading of these numbers: the cache
            did nothing for this paint.
        """
        given : 'A box whose background layer is a noise and absolutely nothing else.'
            var box =
                UI.box()
                  .withStyle( it -> it.noise("grain", n -> n.colors(Color.BLACK, Color.WHITE)) )
                  .get(JBox)
            var ext = ComponentExtension.from(box)
            box.setSize(300, 200)

        when : 'It is painted at a settled size, and then dragged.'
            3.times { Utility.renderSingleComponent(box) }
            int countedWhenSettled = ext.cacheHitCount(UI.Layer.BACKGROUND) + ext.cacheMissCount(UI.Layer.BACKGROUND)
            [[320, 210], [340, 220], [360, 230]].each { w, h ->
                box.setSize(w, h)
                Utility.renderSingleComponent(box)
            }

        then : 'Every paint of both phases is accounted for.'
            countedWhenSettled == 3
            ext.cacheHitCount(UI.Layer.BACKGROUND) + ext.cacheMissCount(UI.Layer.BACKGROUND) == 6
        and : 'And the drag paints, which no cache served, are misses.'
            ext.cacheMissCount(UI.Layer.BACKGROUND) >= 3
        and : 'Which is consistent with there being nothing cached for that layer mid-drag.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isEmpty()
    }

    def 'A plain, undecorated component is *never* cached.'()
    {
        reportInfo """
            Caching is opt-in: SwingTree only allocates a cache image for a
            given layer when that layer contains a *heavy* style ingredient
            (a rounded base colour with a margin, a gradient, a shadow,
            painted text, sized images, ...). For an undecorated component
            this is never the case, so no allocation should ever happen –
            no matter how many times it is repainted.

            This is important to assert because it documents the trade-off
            of the caching mechanism: it specifically targets the
            workloads where rendering is expensive enough that paying for
            a `BufferedImage` is worth it. For trivial paints, the cache
            machinery deliberately stays out of the way.
        """
        given : 'A bog-standard label without any SwingTree style.'
            var label = UI.label("Hello!").get(JLabel)
            var ext   = ComponentExtension.from(label)

        when : 'We render it several times in a row.'
            Utility.renderSingleComponent(label)
            Utility.renderSingleComponent(label)
            Utility.renderSingleComponent(label)
        then : 'No layer ever produced a cached rendering.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isEmpty()
            ext.cachedRendering(UI.Layer.CONTENT).isEmpty()
            ext.cachedRendering(UI.Layer.BORDER).isEmpty()
            ext.cachedRendering(UI.Layer.FOREGROUND).isEmpty()
        and  : 'And the cache hit counter for the background never increased.'
            ext.cacheHitCount(UI.Layer.BACKGROUND)  == 0
    }

    def 'Multiple components with the exact same heavy style share a single cached rendering.'()
    {
        reportInfo """
            Because cache entries are keyed by the (deeply immutable) style
            configuration object itself, components with structurally
            identical styles end up sharing a single rendered image in the
            global pool. Concretely: the first component to be painted
            populates the cache, and every subsequent component with the
            same style finds the rendering already there and goes straight
            to a hit on its very first paint, without ever invoking the
            style renderer itself.

            This is one of the most attractive properties of immutability-
            keyed caching and the reason why repeating rich styles across
            many components is essentially free.
        """
        given : 'A common styler shared by several buttons.'
            def common = { conf -> conf
                .size(140, 50)
                .borderRadius(16)
                .backgroundColor(new Color(40, 120, 200))
                .foundationColor(Color.WHITE)
            }
        and : 'Five buttons that all share the styler – like a row of toolbar buttons.'
            def buttons = (1..5).collect {
                UI.button("Btn " + it).withStyle(common as swingtree.api.Styler).get(JButton)
            }
        and : 'And the matching extensions to query the cache state through.'
            def exts = buttons.collect { ComponentExtension.from(it) }

        when : 'We render every button exactly once.'
            buttons.each { Utility.renderSingleComponent(it) }

        then : 'Every button reports that its background layer is cached.'
            exts.every { it.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty() }

        and : """
            The first button to render had to actually invoke the style
            renderer – there was nothing in the cache yet, so it counts
            as a miss with no hits:
        """
            exts[0].cacheMissCount(UI.Layer.BACKGROUND) >= 1
            exts[0].cacheHitCount(UI.Layer.BACKGROUND)  == 0

        and : """
            Every subsequent button, however, found the cached image
            already installed by an earlier sibling and was therefore
            served from the cache on its very first paint – no fresh
            rendering invocation needed:
        """
            exts[1..-1].every { it.cacheMissCount(UI.Layer.BACKGROUND) == 0 }
            exts[1..-1].every { it.cacheHitCount(UI.Layer.BACKGROUND)  >= 1 }
    }

    def 'Changing the style configuration invalidates the cached rendering.'()
    {
        reportInfo """
            The cache key is the (immutable) style configuration. So when a
            view-model-driven property changes the style produced by the
            styler, the resulting layer configuration changes too, the
            cached image for the *old* configuration is dropped from this
            component's local cache, and a fresh rendering happens on the
            next paint.

            We model this by having the styler read a colour from a
            `Var<Color>` view-model property. After warming the cache for
            RED, we mutate the property to GREEN and observe that:

              - the next paint records a fresh miss (a new entry was
                installed for the new style),
              - hits resume once we paint that same new style a second time.

            This is the documentation-quality demonstration that caching
            and reactivity compose naturally: there is no special integration
            code, just immutable values and equality.
        """
        given : 'A view-model-style property driving the background colour.'
            Var<Color> tint = Var.of(Color.RED)
        and : 'A button whose styler reads from that property.'
            var button =
                UI.button("Tint me")
                  .withStyle( it -> it
                        .size(120, 60)
                        .borderRadius(18)
                        .backgroundColor(tint.get())
                        .foundationColor(Color.WHITE)
                  )
                  .get(JButton)
            var ext = ComponentExtension.from(button)

        when : 'We render the component twice with the initial RED tint to warm the cache.'
            Utility.renderSingleComponent(button)
            Utility.renderSingleComponent(button)
        then : 'The cache is now populated and the second paint counted as a hit.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
            ext.cacheHitCount(UI.Layer.BACKGROUND)  >= 1

        when : 'The view model produces a new tint and we paint again.'
            int missesBeforeMutation = ext.cacheMissCount(UI.Layer.BACKGROUND)
            int hitsBeforeMutation   = ext.cacheHitCount(UI.Layer.BACKGROUND)
            tint.set(Color.GREEN)
            Utility.renderSingleComponent(button)
        then : 'A fresh miss is recorded for the new style configuration...'
            ext.cacheMissCount(UI.Layer.BACKGROUND) > missesBeforeMutation
        and  : '...and the hit count did *not* go up: invalidation forced a re-render, not a blit.'
            ext.cacheHitCount(UI.Layer.BACKGROUND) == hitsBeforeMutation

        when : 'We paint that same new (GREEN) style a second time.'
            int missesBeforeBlit = ext.cacheMissCount(UI.Layer.BACKGROUND)
            int hitsBeforeBlit   = ext.cacheHitCount(UI.Layer.BACKGROUND)
            Utility.renderSingleComponent(button)
        then : 'It is served from the cache again – the cache repopulated after invalidation.'
            ext.cacheHitCount(UI.Layer.BACKGROUND)  > hitsBeforeBlit
            ext.cacheMissCount(UI.Layer.BACKGROUND) == missesBeforeBlit
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
    }

    def 'Resizing a styled component does not invalidate its cached rendering.'()
    {
        reportInfo """
            The component size is part of the style configuration, so naively
            the cache would miss on every single frame of a live resize (think
            of a user dragging the window edge) - historically the most
            expensive repaint scenario for styled components.

            SwingTree avoids this through *stretch tiling*: for styles whose
            pixels are constant along the component edges (flat colours,
            borders, shadows - which is the vast majority of real styles),
            the cache key is made size independent and any component size is
            reconstructed from one small cached rendering by copying the
            corners and stretching the edges. The observable consequence,
            documented here: resizing produces cache *hits*, not misses.
        """
        given : 'A button with a rounded background, warmed up at its initial size.'
            var button =
                UI.button("Resize me")
                  .withStyle( it -> it
                        .borderRadius(20)
                        .backgroundColor(new Color(10, 80, 160))
                        .foundationColor(new Color(245, 245, 240))
                  )
                  .get(JButton)
            button.setSize(120, 60) // Size set on the component itself, so resizing below actually takes effect.
            var ext = ComponentExtension.from(button)
            Utility.renderSingleComponent(button)
            Utility.renderSingleComponent(button)
        expect : 'The cache is warm.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
            ext.cacheHitCount(UI.Layer.BACKGROUND) >= 1

        when : 'The component grows substantially and is painted again.'
            int missesBeforeResize = ext.cacheMissCount(UI.Layer.BACKGROUND)
            int hitsBeforeResize   = ext.cacheHitCount(UI.Layer.BACKGROUND)
            button.setSize(300, 90)
            Utility.renderSingleComponent(button)
        then : 'The resize actually took effect (the style engine did not override it).'
            button.width == 300 && button.height == 90
        and : 'The paint was served from the cache - no fresh rendering despite the new size!'
            ext.cacheHitCount(UI.Layer.BACKGROUND)  > hitsBeforeResize
            ext.cacheMissCount(UI.Layer.BACKGROUND) == missesBeforeResize
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()

        when : 'The component shrinks to yet another size and is painted again.'
            int missesBeforeShrink = ext.cacheMissCount(UI.Layer.BACKGROUND)
            button.setSize(150, 70)
            Utility.renderSingleComponent(button)
        then : 'Still no fresh rendering - every size maps onto the same cached rendering.'
            ext.cacheMissCount(UI.Layer.BACKGROUND) == missesBeforeShrink
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
    }

    def 'Shrinking a styled component to a zero size releases its cached rendering.'()
    {
        reportInfo """
            A cached style-layer image is only worth keeping while the component
            can actually be rendered. When a component collapses to a zero width
            or height - because a layout hid it, a split pane divider was dragged
            all the way over, a tab was deselected, ... - it is effectively
            non-renderable, and holding on to its (potentially large) cached
            `BufferedImage` for as long as the component lives would be a pure
            memory cost with no payoff.

            SwingTree therefore drops the local reference to the cached rendering
            the moment a component validates at a zero size, so the image can be
            reclaimed promptly. The observable consequence, documented here: after
            a component shrinks to `0` in either dimension, `cachedRendering(..)`
            reports that there is no cached image anymore. Should the component
            regain a real size later on, the cache simply repopulates from scratch.
        """
        given : 'A button with a rounded background, warmed up at a real size.'
            var button =
                UI.button("Collapse me")
                  .withStyle( it -> it
                        .borderRadius(20)
                        .backgroundColor(new Color(10, 80, 160))
                        .foundationColor(new Color(245, 245, 240))
                  )
                  .get(JButton)
            button.setSize(120, 60) // Size set on the component itself, so collapsing below actually takes effect.
            var ext = ComponentExtension.from(button)
            Utility.renderSingleComponent(button)
        expect : 'The cache is warm - the background layer produced a cached rendering.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()

        when : '''
            The component collapses to a zero height and its style is re-validated -
            exactly what happens when a layout resizes it to nothing (a paint alone
            would not do, since `JComponent.paint` bails out at a zero size).
        '''
            button.setSize(120, 0)
            ext.gatherApplyAndInstallStyle(true)
        then : 'The cached rendering was released - nothing keeps the image reachable through this component anymore.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isEmpty()

        when : 'The component regains a real size and is painted once more.'
            button.setSize(120, 60)
            Utility.renderSingleComponent(button)
        then : 'The cache repopulates from scratch - the rendering is available again.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
    }

    def 'Components of different sizes but the same style share a single cached rendering.'()
    {
        reportInfo """
            Cache sharing between equally styled components used to require
            them to also have exactly equal sizes. With size independent
            (stretch tiled) cache keys, a whole toolbar of differently sized
            buttons with one common style shares one single cached rendering:
            only the very first one to paint invokes the style renderer,
            every other one goes straight to a cache hit on its first paint.
        """
        given : 'Two buttons sharing a styler but deliberately sized differently.'
            def common = { conf -> conf
                .borderRadius(14)
                .backgroundColor(new Color(120, 60, 10))
                .foundationColor(new Color(250, 248, 244))
            }
            var first  = UI.button("First").withStyle(common as swingtree.api.Styler).get(JButton)
            var second = UI.button("Second").withStyle(common as swingtree.api.Styler).get(JButton)
            first.setSize(200, 80)
            second.setSize(340, 120)
            var firstExt  = ComponentExtension.from(first)
            var secondExt = ComponentExtension.from(second)

        when : 'Both are rendered once, the differently sized one second.'
            Utility.renderSingleComponent(first)
            Utility.renderSingleComponent(second)

        then : 'The first paint of the first button populated the shared cache entry.'
            firstExt.cacheMissCount(UI.Layer.BACKGROUND) >= 1
            firstExt.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
        and : 'The second button was served from the cache on its very first paint, despite its different size.'
            secondExt.cacheMissCount(UI.Layer.BACKGROUND) == 0
            secondExt.cacheHitCount(UI.Layer.BACKGROUND)  >= 1
    }

    def 'Styles which cannot be stretch tiled still re-render on every resize.'()
    {
        reportInfo """
            Not every style survives being cut into nine stretchable tiles:
            a gradient for example spans the full component, so its pixels
            genuinely differ at every size. Such styles keep the classic
            exact-size cache key, and resizing them re-renders - exactly
            the behaviour all styles had before stretch tiling existed.
        """
        given : 'A button with a gradient background (heavy, cacheable, but not tileable).'
            var button =
                UI.button("Gradient")
                  .withStyle( it -> it
                        .borderRadius(10)
                        .gradient( g -> g.colors(new Color(200, 30, 70), new Color(30, 70, 200)) )
                  )
                  .get(JButton)
            button.setSize(120, 60) // Size set on the component itself, so resizing below actually takes effect.
            var ext = ComponentExtension.from(button)
            Utility.renderSingleComponent(button)
            Utility.renderSingleComponent(button)
        expect : 'The gradient is cached and served from the cache at a stable size.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
            ext.cacheHitCount(UI.Layer.BACKGROUND) >= 1

        when : 'The component is resized and painted again.'
            int missesBeforeResize = ext.cacheMissCount(UI.Layer.BACKGROUND)
            int hitsBeforeResize   = ext.cacheHitCount(UI.Layer.BACKGROUND)
            button.setSize(300, 90)
            Utility.renderSingleComponent(button)
        then : 'The new size required a fresh rendering (a miss), not a cache hit.'
            ext.cacheMissCount(UI.Layer.BACKGROUND) > missesBeforeResize
            ext.cacheHitCount(UI.Layer.BACKGROUND)  == hitsBeforeResize
    }

    def 'A large exact-size rendering is not allocated while the component is being resized.'()
    {
        reportInfo """
            A cached rendering earns its keep by being blitted more than once. An entry keyed
            on the exact component size, for a component whose size is currently changing, is
            the one case where that cannot happen: it is allocated, rendered into, blitted
            once, and invalidated by the very next frame. For a large image that is strictly
            more work than having drawn straight onto the destination - a multi megabyte
            allocation per frame, and a software blit rather than an accelerated one, because
            Java2D only promotes an image to a server side pixmap after several uses.

            So while the size is in flux, a *large* exact-size image is not allocated at all.
            Note the two qualifiers: a stretch tileable style is keyed size independently and
            is not affected, and small images are always allocated - see the scenario after this
            one for why that second exception is essential.
        """
        given : 'A radial gradient styled button - heavy enough to cache, too two dimensional to stretch tile.'
            // Radial on purpose: a gradient running straight down the component varies along one
            // axis only and is keyed size independently across the other, which would put it in
            // the very group this scenario needs a counterexample to.
            var button =
                UI.button("Wide")
                  .withStyle( it -> it
                        .borderRadius(10)
                        .gradient( g -> g.type(UI.GradientType.RADIAL).colors(new Color(200, 30, 70), new Color(30, 70, 200)) )
                  )
                  .get(JButton)
            var ext = ComponentExtension.from(button)
        and : 'It is large: above the size up to which an image is worth allocating eagerly.'
            button.setSize(600, 400)

        when : 'It is painted a few times at its first size, which is a birth rather than a resize.'
            8.times { Utility.renderSingleComponent(button) }
        then : 'It is cached, as any heavy style of this size would be.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()

        when : 'The component is then resized and painted again.'
            button.setSize(620, 400)
            Utility.renderSingleComponent(button)
        then : 'No image was allocated for the new size.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isEmpty()

        when : 'The size settles and the component keeps being painted.'
            8.times { Utility.renderSingleComponent(button) }
        then : 'Caching resumes, because the reason to suppress it is gone.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
    }

    def 'A component dragged through fresh sizes leaves no finished renderings behind.'()
    {
        reportInfo """
            A cached rendering pays for itself by being blitted more than once. During a live
            resize of a style that cannot be stretch tiled, an entry keyed on the exact
            component size can almost never manage that: it would be allocated, rendered into,
            blitted once, and thrown away by the very next frame, which arrives at a width no
            frame has had before. Measured on one of the example UIs shipped with SwingTree,
            that came to roughly three megabytes of freshly allocated image memory *per drag
            frame*, of which more than nine tenths was never read a second time.

            So an entry whose key contains a size that is currently changing is not allocated
            on the spot any more. The entry itself is still created, and it still waits in the
            shared pool where any other component with the very same style can find it - but
            the pixel memory behind it is only committed once a second user actually asks for
            it. A component being dragged on its own never produces that second user, and
            therefore never pays for an image it would immediately discard.

            Below, the component is first painted at a settled size, where the ordinary rule
            applies and it does get a cached rendering. It is then dragged across four widths
            it has never had, and none of those frames leaves a rendering behind. As soon as
            the size stops changing, caching returns of its own accord.
        """
        given : 'A radial gradient styled button, heavy enough to be cached, but not stretch tileable.'
            // Radial on purpose - see the scenario above.
            var button =
                UI.button("Drag me")
                  .withStyle( it -> it
                        .borderRadius(10)
                        .gradient( g -> g.type(UI.GradientType.RADIAL).colors(new Color(200, 30, 70), new Color(30, 70, 200)) )
                  )
                  .get(JButton)
            var ext = ComponentExtension.from(button)
        and : 'It sits at a settled size, painted often enough to be cached there.'
            button.setSize(100, 60)
            2.times { Utility.renderSingleComponent(button) }
        expect : 'A rendering exists, because nothing about this size is in flux.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()

        when : 'The component is dragged across four widths it has never had before.'
            var renderingsPerFrame = []
            [120, 140, 160, 180].each { width ->
                button.setSize(width, 60)
                Utility.renderSingleComponent(button)
                renderingsPerFrame << ext.cachedRendering(UI.Layer.BACKGROUND).size()
            }
        then : 'The component really did arrive at the last of those widths.'
            button.width == 180
        and  : 'Not one of those frames left a finished rendering behind.'
            renderingsPerFrame == [0, 0, 0, 0]

        when : 'The drag ends and the component keeps being painted at its final size.'
            2.times { Utility.renderSingleComponent(button) }
        then : 'Caching resumes on its own, because the reason to hold back is gone.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
    }

    def 'A style cached size independently along one axis only is not allocated while the other axis is dragged.'()
    {
        reportInfo """
            The scenario above holds back renderings whose key contains a size that is
            currently changing. Deciding whether a key contains one used to be a yes or no
            question, because a style was either cached at its exact size or at a size
            independent exemplar. A gradient running straight down a component is neither: its
            width collapses into the exemplar while its height is carried in the key.

            Dragging such a component sideways is free, and the scenario after this one is
            about that. Dragging it *downwards* changes the key on every frame, exactly like an
            exact-size key, and it has to be held back for exactly the same reason - otherwise
            each frame allocates an image, blits it once and throws it away.

            The trap here is that "the key differs from the component size" is true for this
            style even while the height is being dragged, so a check phrased that way concludes
            the key is stable and allocates on every frame. What has to be asked instead is whether
            *both* dimensions were dropped, because either one still carried is a dimension this
            drag may be moving.
        """
        given : 'A button with a gradient running straight down it, at a settled size.'
            var button =
                UI.button("Taller")
                  .withStyle( it -> it
                        .borderRadius(10)
                        .gradient( g -> g.span(UI.Span.TOP_TO_BOTTOM).colors(new Color(200, 30, 70), new Color(30, 70, 200)) )
                  )
                  .get(JButton)
            var ext = ComponentExtension.from(button)
        and : """
            Tall enough that its exemplar is above the size up to which an image is allocated
            eagerly - the small entry exception of the scenario above applies here too, and a
            short component would be allocated on every frame quite legitimately.
        """
            button.setSize(300, 500)
            4.times { Utility.renderSingleComponent(button) }
        expect : 'It really is cached, and at a width far below its own.'
            button.width == 300 && button.height == 500
            ext.cachedRendering(UI.Layer.BACKGROUND).every( image -> image.width < 100 )

        when : 'The component is dragged through four heights it has never had before.'
            var renderingsPerFrame = []
            [520, 540, 560, 580].each { height ->
                button.setSize(300, height)
                Utility.renderSingleComponent(button)
                renderingsPerFrame << ext.cachedRendering(UI.Layer.BACKGROUND).size()
            }
        then : 'The component really did arrive at the last of those heights.'
            button.height == 580
        and  : 'Not one of those frames left a finished rendering behind.'
            renderingsPerFrame == [0, 0, 0, 0]

        when : 'The drag ends and the component keeps being painted at its final height.'
            2.times { Utility.renderSingleComponent(button) }
        then : 'Caching resumes, just as it does for a key of any other shape.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
    }

    def 'The axis a one axis gradient does not vary along is still dragged for free.'()
    {
        reportInfo """
            The other side of the scenario above, and the reason it may not simply treat a one
            axis key as an exact-size key everywhere. Holding a rendering back is only right
            while the key is actually moving. Dragged along the axis its gradient does not vary
            along, this very same style keeps one and the same key from frame to frame, and
            every one of those frames is served from the cache without re-rendering anything.

            Both halves have to be pinned together: a fix for the scenario above which
            suppressed this one would have cured the churn by giving up the feature.
        """
        given : 'The same style, settled at a size where it is cached.'
            var button =
                UI.button("Wider")
                  .withStyle( it -> it
                        .borderRadius(10)
                        .gradient( g -> g.span(UI.Span.TOP_TO_BOTTOM).colors(new Color(200, 30, 70), new Color(30, 70, 200)) )
                  )
                  .get(JButton)
            var ext = ComponentExtension.from(button)
            button.setSize(300, 500)
            4.times { Utility.renderSingleComponent(button) }
        expect : 'A rendering exists to be dragged against.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()

        when : 'The component is dragged through four widths it has never had before.'
            int missesBefore = ext.cacheMissCount(UI.Layer.BACKGROUND)
            int hitsBefore   = ext.cacheHitCount(UI.Layer.BACKGROUND)
            [340, 380, 420, 460].each { width ->
                button.setSize(width, 500)
                Utility.renderSingleComponent(button)
            }
        then : 'The component really did arrive at the last of those widths.'
            button.width == 460 && button.height == 500
        and  : 'Not one of those frames re-rendered the style - all four were cache hits.'
            ext.cacheMissCount(UI.Layer.BACKGROUND) == missesBefore
            ext.cacheHitCount(UI.Layer.BACKGROUND)  == hitsBefore + 4
        and  : 'And they were all served from the one exemplar, which never changed.'
            ext.cachedRendering(UI.Layer.BACKGROUND).every( image -> image.width < 100 )
    }

    def 'Equally styled components resizing together still end up sharing one rendering.'()
    {
        reportInfo """
            This is the guard rail on the scenario above, and the reason the entry is still
            created rather than skipped altogether. Renderings are keyed on the style
            configuration and not on the component, so a window full of identically styled
            cells produces *one* rendering that all of them blit. That kind of sharing happens
            within a single frame and survives a resize perfectly well - refusing it would
            turn one rendering per frame into dozens.

            What "wait for a second user" therefore means in a UI of equally styled siblings
            is only this: the first sibling to be painted renders straight onto the screen,
            and the second one commits the shared image. Every sibling after that blits it,
            and - because they were all pointed at the same entry - the first one finds it
            waiting on its next repaint too, without having asked for it again.
        """
        given : 'Two buttons carrying the exact same style.'
            // Radial on purpose - see 'A large exact-size rendering is not allocated while the
            // component is being resized.' for why a gradient down the component would not do.
            def styler = { conf -> conf
                .borderRadius(10)
                .gradient( g -> g.type(UI.GradientType.RADIAL).colors(new Color(200, 30, 70), new Color(30, 70, 200)) )
            }
            var first  = UI.button("A").withStyle(styler as swingtree.api.Styler).get(JButton)
            var second = UI.button("B").withStyle(styler as swingtree.api.Styler).get(JButton)
            var firstExt  = ComponentExtension.from(first)
            var secondExt = ComponentExtension.from(second)
        and : 'Both are painted once at a common size, so that neither is newborn any more.'
            first.setSize(100, 60)
            second.setSize(100, 60)
            Utility.renderSingleComponent(first)
            Utility.renderSingleComponent(second)

        when : 'Both are resized to a common new size, and the first one is painted there.'
            first.setSize(120, 60)
            second.setSize(120, 60)
            Utility.renderSingleComponent(first)
        then : 'It rendered onto the screen without committing an image, as the scenario above showed.'
            firstExt.cachedRendering(UI.Layer.BACKGROUND).isEmpty()

        when : 'The second one is painted at that very same size.'
            Utility.renderSingleComponent(second)
        then : 'It found the waiting entry and committed the shared image.'
            secondExt.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
        and  : 'Which the first component now has as well, without having painted again.'
            firstExt.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()

        when : 'Both are painted once more.'
            Utility.renderSingleComponent(first)
            Utility.renderSingleComponent(second)
        then : 'Both were served from that one shared rendering.'
            firstExt.cacheHitCount(UI.Layer.BACKGROUND)  >= 1
            secondExt.cacheHitCount(UI.Layer.BACKGROUND) >= 1
    }

    def 'A resizing component still uses a large rendering that already exists.'()
    {
        reportInfo """
            What the rule above suppresses is *allocation*, and only that. If a finished rendering
            for exactly this style and size happens to be there already, using it costs no
            allocation and no rendering at all - it is a blit, which is strictly cheaper than
            the fresh render that refusing it would force. There is nothing to save by saying
            no.

            And this is not a corner case, because renderings are keyed globally on the style
            rather than per component: a component being dragged through a size at which an
            identically styled sibling already sits finds that sibling's rendering waiting for
            it. Refusing it would re-render a page sized layer in order to avoid an allocation
            that was never going to happen.
        """
        given : 'A large gradient style, too gradient-y to be stretch tiled, in two components.'
            def styler = { conf -> conf
                .borderRadius(10)
                .gradient( g -> g.colors(new Color(40, 160, 90), new Color(160, 40, 90)) )
            }
            var settled  = UI.button("Settled").withStyle(styler as swingtree.api.Styler).get(JButton)
            var resizing = UI.button("Resizing").withStyle(styler as swingtree.api.Styler).get(JButton)
            var resizingExt = ComponentExtension.from(resizing)

        and : """
            The first one sits still at a large size until its rendering exists. It is kept
            alive for the rest of the scenario on purpose: renderings are held weakly by the
            style they belong to, so a sibling that went out of scope would take the very
            entry this scenario is about with it.
        """
            settled.setSize(400, 200)
            6.times { Utility.renderSingleComponent(settled) }
            assert ComponentExtension.from(settled).cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()

        when : 'The second component is dragged, arriving at the size the first one holds.'
            resizing.setSize(360, 200)
            Utility.renderSingleComponent(resizing)
            resizing.setSize(400, 200)
            Utility.renderSingleComponent(resizing)

        then : 'It was served from the existing rendering rather than rendering the layer again.'
            resizingExt.cacheHitCount(UI.Layer.BACKGROUND) >= 1
        and : 'The sibling is still alive, which is what kept that rendering reachable.'
            settled.width == 400
    }

    def 'A layer whose only content is a cacheable painter is cached.'()
    {
        reportInfo """
            A painter is ordinarily opaque to the library: it is user code, so what it draws
            cannot be assumed to be a function of anything SwingTree can compare, and a layer
            carrying one is therefore not cached.

            `Painter.of(data, painter)` is the user lifting exactly that restriction: it
            promises that the painting is a pure function of the supplied data object, which is
            immutable and has proper `equals`/`hashCode`. That promise is what makes the painter
            usable as part of a cache key - so a layer holding nothing but such a painter is
            cached like any other heavy style, and the painter runs once instead of on every
            paint.
        """
        given : 'A component whose only styling is a cacheable painter.'
            var runs = new java.util.concurrent.atomic.AtomicInteger()
            var painter = swingtree.api.Painter.of("the-key", { g ->
                                runs.incrementAndGet()
                                g.setColor(new Color(240, 120, 30))
                                g.fillOval(10, 10, 40, 30)
                            })
            var box = UI.box().withStyle( it -> it.painter(UI.Layer.BACKGROUND, "mark", painter) ).get(JBox)
            box.setSize(200, 120)
            var ext = ComponentExtension.from(box)

        when : 'It is painted a handful of times.'
            5.times { Utility.renderSingleComponent(box) }

        then : 'There is a cached rendering of that layer.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
        and : 'And the painter really did run only while that rendering was being made.'
            runs.get() == 1
            ext.cacheHitCount(UI.Layer.BACKGROUND) >= 3
    }

    def 'A cacheable painter ahead of an uncacheable one is baked into the cached image.'()
    {
        reportInfo """
            A user painter is arbitrary code, so SwingTree cannot know what it draws and cannot
            cache it - which would ordinarily sink the whole layer it sits on, a layer being
            cached as a single rasterization. Such a layer is therefore cut in two: everything
            cacheable is rendered into an image, and the painter is replayed over that image on
            every paint.

            `Painter.of(data, painter)` changes which side a painter falls on. It is the user
            promising that the painting is a pure function of an immutable value, so a painter
            declared that way can go *into* the cached image - and that is what this pins: it
            runs once, while the uncacheable painter beside it keeps running on every paint.

            Where exactly the cut falls is decided by order. The renderer runs painters in the
            order of their names, and the cut is taken at the *first* uncacheable one:
            everything before it is baked in, everything from it onwards is replayed. That
            keeps every painter in its original position relative to every other, which is what
            makes the cut incapable of changing a single pixel. A cacheable painter which sorts
            *after* an uncacheable one is therefore left where it is.
        """
        given : 'A style with a cacheable painter named ahead of an uncacheable one.'
            var cacheableRuns = new java.util.concurrent.atomic.AtomicInteger()
            var lambdaRuns    = new java.util.concurrent.atomic.AtomicInteger()
            var cacheable = swingtree.api.Painter.of("key", { g ->
                                cacheableRuns.incrementAndGet()
                                g.setColor(new Color(240, 120, 30)); g.fillOval(20, 20, 60, 40)
                            })
            var box = UI.box().withStyle( it -> it
                            .backgroundColor("#2f4f6f").borderRadius(14)
                            .painter(UI.Layer.BACKGROUND, "a-cacheable", cacheable)
                            .painter(UI.Layer.BACKGROUND, "b-uncacheable", { g ->
                                lambdaRuns.incrementAndGet()
                                g.setColor(new Color(30, 200, 160)); g.fillRect(40, 30, 90, 50)
                            })
                        ).get(JBox)
            box.setSize(300, 160)

        when : 'It is painted five times.'
            5.times { Utility.renderSingleComponent(box) }

        then : 'The cacheable painter ran once, into the cached image.'
            cacheableRuns.get() == 1
        and : 'While the uncacheable one ran on every single paint, as it must.'
            lambdaRuns.get() == 5
    }

    def 'A cacheable painter behind an uncacheable one keeps running, so that order is preserved.'()
    {
        reportInfo """
            SwingTree cannot cache what a user painter draws, because it is arbitrary code, so
            a layer carrying one is cut in two: what can be cached is rendered into an image,
            and the painter is replayed over that image on every paint. A painter created with
            `Painter.of(data, painter)` is the exception - there the user promises that the
            painting is a pure function of an immutable value, so it can be baked into the
            image instead of being replayed.

            That raises the question of *which* cacheable painters are taken into the image,
            and what this scenario pins is the deliberate limit on the answer: only those which
            sort ahead of every uncacheable one.

            The reason is that painters on a layer are run in the order of their names and may
            overlap, so which of them end up inside the image decides what the component looks
            like. Taking a cacheable painter out from behind an uncacheable one would move it
            underneath, changing the picture - silently, and in code the library never sees. So
            a painter which sorts *after* an uncacheable one is left where it is and keeps
            being replayed, even though its promise would have allowed caching it.

            A user who wants such a painter cached can have it, by naming it so that it sorts
            ahead of the uncacheable one - which is the same thing as saying it should be
            painted first.
        """
        given : 'A cacheable and an uncacheable painter, the cacheable one named so that it sorts second.'
            var cacheableRuns = new java.util.concurrent.atomic.AtomicInteger()
            var cacheable = swingtree.api.Painter.of("key", { g ->
                                cacheableRuns.incrementAndGet()
                                g.setColor(new Color(240, 120, 30)); g.fillOval(20, 20, 60, 40)
                            })
            var box = UI.box().withStyle( it -> it
                            .backgroundColor("#2f4f6f").borderRadius(14)
                            .painter(UI.Layer.BACKGROUND, "a-uncacheable", { g ->
                                g.setColor(new Color(30, 200, 160)); g.fillRect(40, 30, 90, 50)
                            })
                            .painter(UI.Layer.BACKGROUND, "b-cacheable", cacheable)
                        ).get(JBox)
            box.setSize(300, 160)

        when : 'It is painted five times.'
            5.times { Utility.renderSingleComponent(box) }

        then : 'The cacheable painter ran every time, because caching it would have reordered it.'
            cacheableRuns.get() == 5
    }

    def 'Two identically styled components share a cached rendering even if they name their painters differently.'()
    {
        reportInfo """
            Cached renderings are keyed on the style configuration and shared globally, so a UI
            full of identically styled components renders one image and blits it for all of
            them. A layer carrying an uncacheable painter is cut in two, and what is cached is
            then everything *except* the painter - so what is cached is identical for two
            components which differ only in their painter.

            The *name* of the painter must therefore not survive into that key. A name is how a
            style is addressed while it is being configured, not something the user expects to
            change what is drawn - so two components whose only difference is that one calls its
            painter "mark" and the other calls it "logo" have to share the one cached image.
            Were they not to, each would allocate an image of its own, and since the number of
            entries is capped, that debris would lock other components out of the cache too.
        """
        given : 'Two boxes with the same background, each with an uncacheable painter of its own name.'
            var first = UI.box().withStyle( it -> it
                            .backgroundColor(new Color(35, 95, 125)).borderRadius(11).margin(4)
                            .painter(UI.Layer.BACKGROUND, "mark", { g ->
                                g.setColor(new Color(240, 120, 30)); g.fillOval(10, 10, 40, 30)
                            })
                        ).get(JBox)
            var second = UI.box().withStyle( it -> it
                            .backgroundColor(new Color(35, 95, 125)).borderRadius(11).margin(4)
                            .painter(UI.Layer.BACKGROUND, "logo", { g ->
                                g.setColor(new Color(30, 200, 160)); g.fillRect(20, 20, 30, 20)
                            })
                        ).get(JBox)
            first.setSize(260, 140)
            second.setSize(260, 140)

        when : 'The first one is painted until its background is cached.'
            3.times { Utility.renderSingleComponent(first) }
        then : 'It is: the painter was cut out of the layer, so the rest of it could be cached.'
            ComponentExtension.from(first).cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()

        when : 'The second one is painted for the very first time.'
            Utility.renderSingleComponent(second)
        then : 'It found the image the first one had already rendered, instead of rendering again.'
            ComponentExtension.from(second).cacheHitCount(UI.Layer.BACKGROUND)  == 1
            ComponentExtension.from(second).cacheMissCount(UI.Layer.BACKGROUND) == 0
    }

    def 'A painter baked into a cached image does not tie it to the names of the painters replayed over it.'()
    {
        reportInfo """
            Rendered layers are cached globally and keyed on the style itself, so any number of
            identically styled components render once between them and blit that single image.
            Anything which makes two equivalent styles compare as *different* therefore costs
            both memory and rendering - and because the number of cached images is capped, it
            pushes other components out of the cache as well.

            A user painter complicates this, because SwingTree cannot know what arbitrary code
            draws and so cannot cache it. A layer carrying one is cut in two: everything else
            goes into the cached image, and the painter is replayed on top of it on every
            paint. `Painter.of(data, painter)` is the exception - it is the user promising that
            the painting is a pure function of an immutable value, and that promise is what
            lets such a painter be baked into the image like any other style.

            One layer can therefore hold both kinds at once, which is the case below, and what
            this scenario pins is which parts of the style may decide whether two components
            share the resulting image: only the parts the image actually contains. Both
            components here draw the same background and the same cacheable painter, so it is
            the same image. All that differs is the *name* each gave the painter that is
            replayed on top - and a name that contributes no pixel to an image has no business
            deciding whether that image can be found again.
        """
        given : 'Two components sharing a background and a cacheable painter, differing only in the name of an uncacheable one.'
            var cacheable = swingtree.api.Painter.of("shared-key", { g ->
                                g.setColor(new Color(250, 200, 60)); g.fillOval(12, 12, 50, 34)
                            })
            def build = { String lambdaName, Color color -> UI.box().withStyle( it -> it
                                .backgroundColor(new Color(45, 80, 115)).borderRadius(13).margin(5)
                                .painter(UI.Layer.BACKGROUND, "a-cacheable", cacheable)
                                .painter(UI.Layer.BACKGROUND, lambdaName, { g ->
                                    g.setColor(color); g.fillRect(24, 24, 34, 22)
                                })
                            ).get(JBox)
            }
            var first  = build("m-mark", new Color(240, 120, 30))
            var second = build("z-logo", new Color(30, 200, 160))
            first.setSize(280, 150)
            second.setSize(280, 150)

        when : 'The first one is painted until its background is cached.'
            3.times { Utility.renderSingleComponent(first) }
        then : 'It is - the cut put the background and the cacheable painter into one image.'
            ComponentExtension.from(first).cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()

        when : 'The second one is painted for the very first time.'
            Utility.renderSingleComponent(second)
        then : 'It found that very image, rather than allocating one of its own.'
            ComponentExtension.from(second).cacheHitCount(UI.Layer.BACKGROUND)  == 1
            ComponentExtension.from(second).cacheMissCount(UI.Layer.BACKGROUND) == 0
    }

    def 'Caching is per-layer: a heavy background does not imply a cached foreground.'()
    {
        reportInfo """
            SwingTree's style engine keeps a separate cache per layer
            (`BACKGROUND`, `CONTENT`, `BORDER`, `FOREGROUND`). A heavy
            style on one layer must not pull a sibling layer into the
            cache; if a layer carries no expensive ingredients of its
            own, it stays uncached.

            This is what lets SwingTree make targeted decisions: if you
            give a button a fancy gradient background but a plain
            foreground, only the background pays the cost-and-benefit
            of caching. Conversely, layers like `FOREGROUND` are *only*
            cached when the styler explicitly puts heavy ingredients on
            them (e.g. via `it.shadow(UI.Layer.FOREGROUND, ...)` or
            `it.painter(UI.Layer.FOREGROUND, ...)`).
        """
        given : 'A button with a heavy *background* style and nothing on the foreground.'
            var button =
                UI.button("Bg only")
                  .withStyle( it -> it
                        .size(120, 60)
                        .borderRadius(18)
                        .backgroundColor(new Color(80, 30, 150))
                        .foundationColor(Color.WHITE)
                  )
                  .get(JButton)
            var ext = ComponentExtension.from(button)

        when : 'We render the component once.'
            Utility.renderSingleComponent(button)

        then : 'The background layer is cached, just like in the other scenarios.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()

        and : 'But the foreground layer was skipped by the cache because it carries no heavy ingredients.'
            ext.cachedRendering(UI.Layer.FOREGROUND).isEmpty()
    }
}