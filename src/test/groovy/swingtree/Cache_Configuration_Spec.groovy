package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title

import java.util.concurrent.TimeUnit
import swingtree.SwingTreeInitConfig.CacheMode
import swingtree.style.CacheBudget
import swingtree.style.ComponentExtension
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.JButton
import javax.swing.JLabel
import java.awt.Color

@Title("Configuring the Memory/CPU Trade-off")
@Narrative('''

    Every one of SwingTree's internal rendering caches – style layer images,
    noise tiles, shadow gradients and text layouts – draws its
    memory budget from a single, unified source: the `CacheMode` configured on
    the `SwingTree` library context, combined with the amount of system RAM.

    A higher mode keeps more rendered results in memory so subsequent repaints
    can blit them instead of re-rendering (faster, but more memory); the
    `DISABLED` mode turns caching off entirely. The mode can be selected at
    startup or changed at runtime, in which case the caches drop their now-excess
    memory immediately and repopulate under the new budget.

    These scenarios pin the contract down through the *public* API
    (`SwingTree.get().getCacheMode()` / `setCacheMode(..)` and the
    `ComponentExtension` cache observers) so they survive internal refactors; the
    one non-public touch-point is the `CacheBudget.UNITS_OVERRIDE` test hook, used
    purely to make the otherwise RAM-dependent budget deterministic regardless of
    the runner's physical memory.

''')
@Subject([SwingTree, SwingTreeInitConfig, ComponentExtension])
@Timeout(value = 25, unit = TimeUnit.SECONDS)
class Cache_Configuration_Spec extends Specification
{
    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        SwingTree.get().setUiScaleFactor(1f)
        CacheBudget.UNITS_OVERRIDE = -1            // no deterministic override unless a scenario asks for one
        SwingTree.get().setCacheMode(CacheMode.BALANCED) // the default, made explicit (also clears all caches)
    }

    def cleanup() {
        CacheBudget.UNITS_OVERRIDE = -1
        SwingTree.clear()
    }

    /** Sizes a parentless component to its preferred size and paints it through the
     *  regular pipeline (mirrors the helper in the other cache specs). */
    private void paint(java.awt.Component c) {
        def pref = c.getPreferredSize()
        c.setSize(Math.max(1, (int) pref.width), Math.max(1, (int) pref.height))
        Utility.renderSingleComponent(c)
    }

    def 'The default cache mode is BALANCED, and it can be changed at runtime.'()
    {
        expect : 'A freshly configured library context reports the balanced default.'
            SwingTree.get().getCacheMode() == CacheMode.BALANCED

        when : 'We pick a different mode at runtime...'
            SwingTree.get().setCacheMode(CacheMode.AGGRESSIVE)
        then : '...it is reflected immediately.'
            SwingTree.get().getCacheMode() == CacheMode.AGGRESSIVE

        when : 'And we turn caching off entirely.'
            SwingTree.get().setCacheMode(CacheMode.DISABLED)
        then : 'That too is reflected.'
            SwingTree.get().getCacheMode() == CacheMode.DISABLED
    }

    def 'The DISABLED mode turns style-layer caching off too.'()
    {
        given : 'Caching is off and a heavily styled button.'
            SwingTree.get().setCacheMode(CacheMode.DISABLED)
            var button =
                UI.button("Hello!")
                  .withStyle( it -> it
                        .size(120, 60)
                        .borderRadius(20)
                        .backgroundColor(Color.BLUE)
                        .foundationColor(Color.WHITE)
                  )
                  .get(JButton)
            var ext = ComponentExtension.from(button)

        when : 'We render it twice.'
            2.times { Utility.renderSingleComponent(button) }

        then : 'Its background layer is never promoted to a cached image.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isEmpty()
    }

    def 'Raising the budget at runtime takes effect without re-initializing the library.'()
    {
        reportInfo """
            Cache size limits are read *live* on every paint, not snapshotted once at
            class-load time. Observed through the style-layer cache: with a zero budget
            nothing is admitted, and raising the budget at runtime makes subsequent
            paints start caching — no re-initialization required.
        """
        given : 'A zero budget (a maximally constrained machine) and a styled label painted repeatedly.'
            CacheBudget.UNITS_OVERRIDE = 0
            ComponentExtension.updateAllCachesFromLibraryConfig() // clears every global rendering cache
            var label = UI.label("Platform 9")
                          .withStyle({ it.backgroundColor(java.awt.Color.BLUE).borderRadius(8) })
                          .get(javax.swing.JLabel)
            5.times { paint(label) }
        expect : 'Nothing was admitted to the style-layer cache while the budget was zero.'
            ComponentExtension.globalRenderCacheEntryCounts().toMap()["style layers"] == 0

        when : 'We raise the budget at runtime and paint a few more times.'
            CacheBudget.UNITS_OVERRIDE = 10
            5.times { paint(label) }
        then : 'Caching kicks in — the live limit reacted to the runtime change.'
            ComponentExtension.globalRenderCacheEntryCounts().toMap()["style layers"] > 0
    }

    def 'Stretch tiling can be turned off at runtime, restoring exact-size cache keys.'()
    {
        reportInfo """
            Size independent (stretch tiled) render caching is what makes
            resizing styled components cheap: eligible styles share one cached
            rendering across all sizes. Because it reconstructs pixels rather
            than re-rendering them, it also ships with a safety hatch:
            `SwingTree.get().setCacheTilingEnabled(false)` (or the system
            property `swingtree.cacheMode.tiling=false`) restores the classic
            exact-size cache keying, where every resize re-renders. This
            scenario pins the whole round trip: on -> off -> on again.
        """
        given : 'A deterministic budget and a styled button (flat colors: stretch tileable).'
            CacheBudget.UNITS_OVERRIDE = 10
            ComponentExtension.updateAllCachesFromLibraryConfig()
            var button =
                UI.button("Switch")
                  .withStyle( it -> it
                        .borderRadius(16)
                        .backgroundColor(new Color(60, 130, 90))
                        .foundationColor(new Color(240, 240, 235))
                  )
                  .get(JButton)
            button.setSize(140, 60)
            var ext = ComponentExtension.from(button)

        expect : 'Stretch tiling is enabled by default.'
            SwingTree.get().isCacheTilingEnabled()

        when : 'We warm the cache and then resize, with tiling enabled.'
            2.times { Utility.renderSingleComponent(button) }
            int missesWhileTiled = ext.cacheMissCount(UI.Layer.BACKGROUND)
            button.setSize(280, 90)
            Utility.renderSingleComponent(button)
        then : 'The resize did not require any fresh rendering.'
            ext.cacheMissCount(UI.Layer.BACKGROUND) == missesWhileTiled

        when : 'We flip the safety hatch and resize twice more.'
            SwingTree.get().setCacheTilingEnabled(false)
            button.setSize(310, 100)
            Utility.renderSingleComponent(button)
            int missesAfterFirstUntiledPaint = ext.cacheMissCount(UI.Layer.BACKGROUND)
            button.setSize(340, 110)
            Utility.renderSingleComponent(button)
        then : 'The flag is off, and now every new size requires a fresh rendering - the classic behavior.'
            !SwingTree.get().isCacheTilingEnabled()
            ext.cacheMissCount(UI.Layer.BACKGROUND) > missesAfterFirstUntiledPaint

        when : 'We re-enable tiling, let one paint repopulate the canonical rendering, and resize again.'
            SwingTree.get().setCacheTilingEnabled(true)
            button.setSize(360, 120)
            Utility.renderSingleComponent(button)
            int missesAfterRepopulation = ext.cacheMissCount(UI.Layer.BACKGROUND)
            button.setSize(400, 130)
            Utility.renderSingleComponent(button)
        then : 'Resizing is miss-resistant again.'
            SwingTree.get().isCacheTilingEnabled()
            ext.cacheMissCount(UI.Layer.BACKGROUND) == missesAfterRepopulation
    }

    def 'Turning stretch tiling off also stops layers being cut around their noises.'()
    {
        reportInfo """
            `setCacheTilingEnabled(false)` is the safety hatch for going back to
            the classic exact-size caching, and a layer painted in several pieces
            is not that either. So the switch covers both: with it off, a
            noise-bearing layer is one cached image at the component's own size,
            being dragged or not, exactly as it was before any of this existed.
        """
        given : 'A deterministic budget, and a big button with a noise over a rounded fill.'
            CacheBudget.UNITS_OVERRIDE = 10
            ComponentExtension.updateAllCachesFromLibraryConfig()
            var button =
                UI.button("Grain me")
                  .withStyle( it -> it
                        .borderRadius(16)
                        .backgroundColor(new Color(30, 90, 138))
                        .noise("grain", n -> n.colors(new Color(32, 32, 32), new Color(222, 222, 222)))
                  )
                  .get(JButton)
            var ext = ComponentExtension.from(button)

        when : 'It is dragged with the hatch open, which is when a layer gets cut.'
            [[500, 300], [560, 320], [620, 340]].each { w, h ->
                button.setSize(w, h)
                Utility.renderSingleComponent(button)
            }
        then : 'What is cached is the size independent exemplar of everything but the noise.'
            ext.cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
            ext.cachedRendering(UI.Layer.BACKGROUND).all( image -> image.width < 620 )

        when : 'The hatch is closed and it is dragged further.'
            SwingTree.get().setCacheTilingEnabled(false)
            [[660, 360], [700, 380]].each { w, h ->
                button.setSize(w, h)
                Utility.renderSingleComponent(button)
            }
        then : """
            The exemplar is gone: the layer was not cut, so there is nothing size independent
            left to cache, and a full sized image is not worth allocating for a size the drag
            is about to leave behind anyway.
        """
            ext.cachedRendering(UI.Layer.BACKGROUND).isEmpty()

        when : 'The drag ends, and it is painted a few times at that final size.'
            6.times { Utility.renderSingleComponent(button) }
        then : 'The layer is one whole image at the component size, noise and all.'
            ext.cachedRendering(UI.Layer.BACKGROUND).size() == 1
            ext.cachedRendering(UI.Layer.BACKGROUND).first().width  == 700
            ext.cachedRendering(UI.Layer.BACKGROUND).first().height == 380

        cleanup :
            SwingTree.get().setCacheTilingEnabled(true)
            CacheBudget.UNITS_OVERRIDE = -1
            ComponentExtension.updateAllCachesFromLibraryConfig()
    }

    def 'Flipping the stretch tiling hatch reaches a component that is not changing at all.'()
    {
        reportInfo """
            The tiling switch is a debugging hatch: someone who suspects a
            reconstructed rendering of being wrong flips it and looks at the very
            same component again. That component is typically sitting still - the
            window is not being dragged and nothing in its style is animating - so
            the switch has to reach a component whose configuration has not changed
            since its last paint, not merely one that is about to be resized.

            Which of the two cache keyings is in force is visible in the rendering
            the component holds on to: a stretch tiled layer caches one small
            exemplar that every size is reconstructed from, whereas the classic
            keying the hatch restores caches the component's own size.
        """
        given : 'A deterministic budget and a large, flatly coloured, stretch tileable button.'
            CacheBudget.UNITS_OVERRIDE = 10
            ComponentExtension.updateAllCachesFromLibraryConfig()
            var button =
                UI.button("Hold still")
                  .withStyle( it -> it
                        .borderRadius(20)
                        .backgroundColor(new Color(60, 130, 90))
                        .foundationColor(new Color(240, 240, 235))
                  )
                  .get(JButton)
            button.setSize(300, 120)
            var ext = ComponentExtension.from(button)

        when : 'It is painted a number of times, without ever changing in any way.'
            6.times { Utility.renderSingleComponent(button) }
        then : 'What it holds is the small exemplar that every size is reconstructed from.'
            ext.cachedRendering(UI.Layer.BACKGROUND).size() == 1
            ext.cachedRendering(UI.Layer.BACKGROUND).first().width < 300

        when : 'The hatch is closed, and it is painted again - still without changing anything.'
            SwingTree.get().setCacheTilingEnabled(false)
            6.times { Utility.renderSingleComponent(button) }
        then : 'It now holds a rendering of its own size, which is the classic keying.'
            ext.cachedRendering(UI.Layer.BACKGROUND).size() == 1
            ext.cachedRendering(UI.Layer.BACKGROUND).first().width  == 300
            ext.cachedRendering(UI.Layer.BACKGROUND).first().height == 120

        cleanup : 'We open the hatch again and hand the library back its own budget.'
            SwingTree.get().setCacheTilingEnabled(true)
            CacheBudget.UNITS_OVERRIDE = -1
            ComponentExtension.updateAllCachesFromLibraryConfig()
    }

    def 'The live cache monitoring snapshot covers every global rendering cache.'()
    {
        reportInfo """
            `ComponentExtension.globalRenderCacheEntryCounts()` is the observation point
            for cache monitoring — the dev tool displays it live, and tests pin the
            budget contract through it. Its keys are a stable, display-friendly
            inventory of SwingTree's global rendering caches.
        """
        expect : 'One entry per global rendering cache, in stable order, never negative.'
            ComponentExtension.globalRenderCacheEntryCounts().keySet().toList() == ["style layers", "text layouts", "noise paints", "shadow gradients"]
            ComponentExtension.globalRenderCacheEntryCounts().values().every { it >= 0 }
    }
}
