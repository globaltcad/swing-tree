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
    rasterised text, noise tiles, shadow gradients and text layouts – draws its
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
            !ext.hasCachedRendering(UI.Layer.BACKGROUND)
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
            ComponentExtension.globalRenderCacheEntryCounts()["style layers"] == 0

        when : 'We raise the budget at runtime and paint a few more times.'
            CacheBudget.UNITS_OVERRIDE = 10
            5.times { paint(label) }
        then : 'Caching kicks in — the live limit reacted to the runtime change.'
            ComponentExtension.globalRenderCacheEntryCounts()["style layers"] > 0
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
            ComponentExtension.globalRenderCacheEntryCounts().keySet() as List ==
                ["style layers", "text layouts", "noise paints", "shadow gradients"]
            ComponentExtension.globalRenderCacheEntryCounts().values().every { it >= 0 }
    }
}
