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
import swingtree.components.JBox
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.JButton
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

    def 'A cache mode is a memory promise: no amount of painting makes the style layer cache exceed it.'()
    {
        reportInfo """
            A `CacheMode` names an amount of memory (see its documentation for the figures),
            and this is what makes that a promise rather than a hope: however many differently
            styled components an application paints, and however big they are, what SwingTree
            retains stays within the budget the mode selected.

            The promise is worth stating precisely because it is not free. A rendering that
            does not fit is simply not cached, and a component whose rendering is not cached is
            re-rendered on every paint - so the budget is a real ceiling on speed as well as on
            memory, which is exactly why `CacheMode` is a knob an application can turn.

            Style layer images are what this is about: they are the only rendering cache whose
            entries are large (a page sized background is tens of megabytes, where a text
            layout is two kilobytes), so they are the only ones that can spend a budget.

            Note that the ceiling asserted against below is this one cache's *share* of the
            library budget rather than the whole of it. Measuring against the whole would leave
            room to overspend the share by more than half and still pass.
        """
        given : 'A deliberately small budget, and empty caches to start from.'
            CacheBudget.UNITS_OVERRIDE = 3
            ComponentExtension.updateAllCachesFromLibraryConfig()
            long budget = CacheBudget.totalBudgetBytes()
            long layerBudget = UI.runAndGet(()->ComponentExtension.globalStyleLayerCacheByteBudget())

        when : """
            Far more distinctly styled large components are painted than that budget could
            ever hold. Each carries a gradient, which is both heavy enough to be worth caching
            and *not* size independently cacheable - so each really does want an image of its
            own at its full size, rather than sharing a small exemplar with the others.

            Note that all of them are kept alive to the end of the scenario. Cache entries are
            keyed weakly, so a component that goes out of scope takes its entry with it - and a
            scenario which let that happen would be asserting that an empty cache fits in a
            budget, which every implementation manages.
        """
            var painted = (1..24).collect { n ->
                var box = UI.box().withStyle( it -> it
                                .borderRadius(9)
                                .gradient( g -> g.colors(new Color(9 * n, 40, 200 - 7 * n),
                                                         new Color(30, 5 * n, 90)) )
                            ).get(JBox)
                box.setSize(500, 400)
                8.times { Utility.renderSingleComponent(box) }
                return box
            }

        then : 'Together they wanted more memory than the whole library budget, let alone this cache\'s share of it.'
            painted.size() * 500 * 400 * 4 > budget
        and : 'The cache did fill up, so this is not a scenario that passes by painting nothing.'
            UI.runAndGet(()->ComponentExtension.globalStyleLayerCacheBytesReserved()) > 0
        and : 'And yet it stayed inside the budget its mode named.'
            UI.runAndGet(()->ComponentExtension.globalStyleLayerCacheBytesReserved()) <= layerBudget

        when : """
            The application then asks for a smaller budget and keeps painting.

            This is the harder half. Shrinking the budget empties the cache, but emptying it
            only releases the memory if the components still painting from those renderings let
            go of them as well - and these components never change their style or their size, so
            nothing about *them* prompts a second look. A cache that waited to be asked would
            leave every one of these images retained and unaccounted for, and would then report
            a comfortably small figure for the handful of entries it could still see.
        """
            CacheBudget.UNITS_OVERRIDE = 1.5
            ComponentExtension.updateAllCachesFromLibraryConfig()
            long smallerBudget = CacheBudget.totalBudgetBytes()
            long smallerLayerBudget = UI.runAndGet(()->ComponentExtension.globalStyleLayerCacheByteBudget())
            painted.each { box -> 12.times { Utility.renderSingleComponent(box) } }

        then : 'The smaller promise holds too.'
            smallerBudget < budget
            UI.runAndGet(()->ComponentExtension.globalStyleLayerCacheBytesReserved()) <= smallerLayerBudget
        and : """
            And the cache refilled itself under the smaller budget rather than simply staying
            empty - which is what stops everything asserted here from being a statement about
            nothing. A budget too small to admit even one of these renderings would satisfy
            every ceiling below without the cache doing any of the work they are about.
        """
            UI.runAndGet(()->ComponentExtension.globalStyleLayerCacheBytesReserved()) > 0
        and : """
            And the figure really is the whole story: every component either paints from an
            entry the cache still counts, or does not paint from an entry at all. A retained
            rendering that the accounting had lost sight of would show up right here.
        """
            var stillCached = painted.count { box ->
                ComponentExtension.from(box).cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
            }
            stillCached > 0
            stillCached * 500 * 400 * 4 <= smallerLayerBudget
        and : 'And the components are still alive, so their entries had every chance to survive.'
            painted.every { it.width == 500 }
    }

    def 'A full cache still serves a component whose rendering it already holds.'()
    {
        reportInfo """
            Cached renderings are keyed on the style configuration and shared process wide
            rather than owned per component, so a hundred identically styled table cells
            render one image between them and blit it a hundred times. That sharing is most
            of what the cache is for.

            Which is why running out of budget must not simply mean saying no. It means
            running out of budget for a *new* rendering; a component asking for one the cache
            already holds needs no budget of its own, because attaching to an image that
            exists allocates nothing at all.

            The distinction is the difference between a bounded cache and no cache. Without
            it, an application that fills its budget once stops caching every component
            created afterwards - including the ones asking for exactly what the cache is
            already holding, which would then re-render on every paint with their image
            sitting right there in the pool.
        """
        given : 'A deliberately small budget, and empty caches to start from.'
            CacheBudget.UNITS_OVERRIDE = 3
            ComponentExtension.updateAllCachesFromLibraryConfig()
        and : """
            A large component, built and painted until it either has a cached rendering or has
            been refused one. Its number picks its colours, which is what decides whether two
            of these wear the same style or not. The style is a gradient because that is heavy
            enough to be worth caching and *not* size independently cacheable - so each really
            does want a full sized image of its own rather than a small shared exemplar.
        """
            var paintLargeStyled = { int n ->
                var box = UI.box().withStyle( it -> it
                                .borderRadius(9)
                                .gradient( g -> g.colors(new Color(9 * n, 40, 200 - 7 * n),
                                                         new Color(30, 5 * n, 90)) )
                            ).get(JBox)
                box.setSize(500, 400)
                8.times { Utility.renderSingleComponent(box) }
                return box
            }

        when : """
            The budget is filled with two dozen distinctly styled ones - all kept alive to the
            end of the scenario, so that the entries they filled it with stay alive too.
        """
            var filling = (1..24).collect { n -> paintLargeStyled(n) }
        then : 'It really is full: a component wearing a style never seen before is refused.'
            var newcomer = paintLargeStyled(25)
            ComponentExtension.from(newcomer).cachedRendering(UI.Layer.BACKGROUND).isEmpty()

        when : 'A component is then created wearing a style the cache already holds.'
            var twin = paintLargeStyled(1)
        then : 'It is served from that existing rendering rather than being turned away.'
            ComponentExtension.from(twin).cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
            ComponentExtension.from(twin).cacheHitCount(UI.Layer.BACKGROUND) > 0
        and : """
            And sharing it really was free: the cache is still inside the budget it was already
            filling before the twin existed.
        """
            UI.runAndGet(()->ComponentExtension.globalStyleLayerCacheBytesReserved() <= ComponentExtension.globalStyleLayerCacheByteBudget())
        and : 'The components which filled it are all still alive, so their entries are too.'
            filling.every { it.width == 500 }
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
