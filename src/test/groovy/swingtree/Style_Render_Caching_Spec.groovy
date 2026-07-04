package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.style.ComponentExtension
import swingtree.threading.EventProcessor
import utility.Utility

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
      3. Observing the resulting cache state via `ComponentExtension.hasCachedRendering(layer)`,
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
            ext.hasCachedRendering(UI.Layer.BACKGROUND)
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
            ext.hasCachedRendering(UI.Layer.BACKGROUND)
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
            !ext.hasCachedRendering(UI.Layer.BACKGROUND)
            !ext.hasCachedRendering(UI.Layer.CONTENT)
            !ext.hasCachedRendering(UI.Layer.BORDER)
            !ext.hasCachedRendering(UI.Layer.FOREGROUND)
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
            exts.every { it.hasCachedRendering(UI.Layer.BACKGROUND) }

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
            ext.hasCachedRendering(UI.Layer.BACKGROUND)
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
            ext.hasCachedRendering(UI.Layer.BACKGROUND)
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
            ext.hasCachedRendering(UI.Layer.BACKGROUND)

        and : 'But the foreground layer was skipped by the cache because it carries no heavy ingredients.'
            !ext.hasCachedRendering(UI.Layer.FOREGROUND)
    }
}