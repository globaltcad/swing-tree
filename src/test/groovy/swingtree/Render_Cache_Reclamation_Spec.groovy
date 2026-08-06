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

import javax.swing.JComponent
import java.awt.Color
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

@Title("The Render Cache Cleans Up After Itself")
@Narrative('''

    SwingTree caches the rendered pixels of a component's style in a process wide
    pool, shared by every component whose style is `equal`. Sharing is the point:
    a table of two hundred identically styled cells renders one image and blits it
    two hundred times.

    A pool like that has to know when to let go, and SwingTree's answer is that it
    never decides at all — the entries are keyed *weakly* by the style
    configuration, so an entry lives exactly as long as some live component still
    holds that configuration, and not a moment longer. Close a dialog, drop a view,
    switch a theme, and the images those components were using become unreachable
    and are reclaimed by the garbage collector like any other object.

    That is easy to break and expensive to break. One strong reference held
    somewhere in the style engine — a listener, a registry, a "last rendered"
    field — would pin every image any component ever painted for the lifetime of
    the application, and because the cache is bounded, a pool full of images
    belonging to components that no longer exist stops admitting the ones that do.
    A leak here does not merely waste memory, it silently turns the cache off.

    These scenarios therefore build components, paint them, drop them, and insist
    that the memory comes back — across every shape the cache has: images keyed on
    the exact component size, images keyed size independently, and the two cases
    where a single layer is cached in more than one piece.

''')
@Subject([ComponentExtension])
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class Render_Cache_Reclamation_Spec extends Specification
{
    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        SwingTree.get().setUiScaleFactor(1f)
        CacheBudget.UNITS_OVERRIDE = 10 // A deterministic budget, independent of the runner's RAM.
        UI.runNow( () -> ComponentExtension.updateAllCachesFromLibraryConfig() ) // Every scenario starts with empty caches.
    }

    def cleanup() {
        CacheBudget.UNITS_OVERRIDE = -1
        SwingTree.clear()
    }

    def 'A component that goes out of scope takes its cached renderings with it. (#description)'(
        String description, Closure builder
    ) {
        reportInfo """
            This is the whole contract, exercised on one styling idiom at a time. A component
            is built, sized and painted until its style really is in the cache; then the last
            reference to it is dropped and the garbage collector is given its chance.

            Both halves are asserted, and both matter. That the cache *grew* is what stops this
            from being a scenario about an empty cache, which any implementation passes. That
            it then came back to where it started is the contract itself — measured in bytes as
            well as in entries, because the cache is bounded by bytes, so an entry that lingers
            is an entry that keeps a future component out.
        """
        given : 'An empty cache to measure against.'
            var entriesBefore = styleLayerEntries()
            var bytesBefore = UI.runAndGet({ComponentExtension.globalStyleLayerCacheBytesReserved()})

        when : """
            A component with this style is painted often enough to be cached, and then dropped.
            It is built inside a helper rather than in a local variable on purpose: a local
            would still be reachable from this scenario's own stack frame when the collector
            runs, and the scenario would then be pinning the very reference it is testing.
        """
            var ghost = paintAndForget(builder)
        then : 'While it was alive, its style really was cached - so there is something to reclaim.'
            styleLayerEntries() > entriesBefore
            UI.runAndGet({ComponentExtension.globalStyleLayerCacheBytesReserved()}) > bytesBefore

        when : 'The garbage collector gets its chance.'
            var componentWasCollected = eventually(24, { ghost.get() == null })
        then : 'The component itself is gone, which is the precondition for everything below.'
            componentWasCollected
        and : """
            And so are its cached renderings: no entry, and no byte of image, outlives the
            component it belonged to.
        """
            eventually(24, { styleLayerEntries() <= entriesBefore })
            UI.runAndGet({ComponentExtension.globalStyleLayerCacheBytesReserved()}) <= bytesBefore

        where :
            description                        | builder
            "a flat rounded background"        | { UI.box().withStyle({ it.backgroundColor("#2f4f6f").borderRadius(14).margin(6) }).get(JBox) }
            "a gradient"                       | { UI.box().withStyle({ it.borderRadius(9).gradient( g -> g.colors(new Color(200, 30, 70), new Color(30, 70, 200)) ) }).get(JBox) }
            "a shadow"                         | { UI.box().withStyle({ it.backgroundColor("#6f4f2f").borderRadius(18)
                                                        .shadow(UI.Layer.CONTENT, "glow", s -> s.color("#0a0a14").blurRadius(7).spreadRadius(1)) }).get(JBox) }
            "painted text"                     | { UI.box().withStyle({ it.text( t -> t.content("Reclaim me") ) }).get(JBox) }
            // The declaration is built inside the closure, not held in a field: icon declarations
            // are pooled process wide, and a field here would pin one for every other specification
            // in the run (`Icon_Caching_Spec` counts them).
            "a background image"               | { UI.box().withStyle({ it.borderRadius(10)
                                                        .image( img -> img.image(swingtree.api.IconDeclaration.of("img/swing.png")) ) }).get(JBox) }
            "a coloured border"                | { UI.box().withStyle({ it.border(4, "#903030").borderRadius(10).backgroundColor("#e8e0d0") }).get(JBox) }
            "a noise"                          | { UI.box().withStyle({ it.backgroundColor("#2f4f6f").borderRadius(12)
                                                        .noise(UI.Layer.BACKGROUND, "grain", n -> n.colors("#101010", "#e0e0e0")) }).get(JBox) }
            "a cacheable painter"              | { UI.box().withStyle({ it.painter(UI.Layer.BACKGROUND, "mark", swingtree.api.Painter.of("k", { g ->
                                                        g.setColor(new Color(240, 120, 30)); g.fillOval(10, 10, 60, 40)
                                                     })) }).get(JBox) }
            "a layer cut around its painter"   | { UI.box().withStyle({ it.backgroundColor("#2f4f6f").borderRadius(14)
                                                        .painter(UI.Layer.BACKGROUND, "mark", { g ->
                                                            g.setColor(new Color(30, 200, 160)); g.fillRect(20, 20, 60, 40)
                                                        }) }).get(JBox) }
            "several layers at once"           | { UI.box().withStyle({ it.backgroundColor("#3f5f2f").foundationColor("#e0e8d0").borderRadius(16).margin(8)
                                                        .gradient(UI.Layer.BACKGROUND, "sky", g -> g.colors(new Color(80, 120, 200), new Color(20, 30, 60)))
                                                        .shadow(UI.Layer.CONTENT, "glow", s -> s.color("#0a0a14").blurRadius(6)) }).get(JBox) }
    }

    def 'A layer cut into pieces while resizing releases every piece.'()
    {
        reportInfo """
            A layer holding a noise is not cached as one image while the component is being
            resized: the noise is lifted out and replayed, and what sits under and over it is
            cached separately. One layer, several entries - and a reclamation bug that only
            drops the first of them would be invisible to a scenario that never resizes
            anything.

            So this one drags the component through a range of sizes, exactly as a window edge
            would, before dropping it.
        """
        given : 'An empty cache to measure against.'
            var entriesBefore = styleLayerEntries()
            var bytesBefore = UI.runAndGet({ComponentExtension.globalStyleLayerCacheBytesReserved()})

        when : 'A noise bearing component is dragged through a range of sizes, and then dropped.'
            var ghost = dragAndForget({ it.backgroundColor("#2f4f6f").borderRadius(12).margin(5)
                                          .noise(UI.Layer.BACKGROUND, "grain", n -> n.colors("#101010", "#e0e0e0"))
                                          .shadow(UI.Layer.BACKGROUND, "glow", s -> s.color("#0a0a14").blurRadius(6)) })
        then : 'The drag really did fill the cache with more than a single entry.'
            styleLayerEntries() > entriesBefore + 1

        when : 'The garbage collector gets its chance.'
            var componentWasCollected = eventually(24, { ghost.get() == null })
        then : 'Every piece went with the component, not just the first.'
            componentWasCollected
            eventually(24, { styleLayerEntries() <= entriesBefore })
            UI.runAndGet({ComponentExtension.globalStyleLayerCacheBytesReserved()}) <= bytesBefore
    }

    def 'Dropping one of two identically styled components does not disturb the other.'()
    {
        reportInfo """
            The flip side of the contract, and the reason it cannot simply be "free the entry
            when a component is disposed": entries are *shared*. Two identically styled
            components have one image between them, so it must survive the first of them being
            dropped and only go when the last one does.

            A cache that reclaimed per component rather than per reachable style would fail
            this while passing every scenario above.
        """
        given : 'Two identically styled components, both painted until the style is cached.'
            var styler = { conf -> conf.borderRadius(11).margin(4)
                                       .gradient( g -> g.colors(new Color(90, 30, 140), new Color(20, 90, 140)) ) }
            var survivor = UI.box().withStyle(styler as swingtree.api.Styler).get(JBox)
            survivor.setSize(260, 150)
            var ghost = paintAndForgetLike(styler)
            8.times { Utility.renderSingleComponent(survivor) }

        expect : 'They shared a single entry rather than minting one each.'
            ComponentExtension.from(survivor).cachedRendering(UI.Layer.BACKGROUND).isNotEmpty()
            ComponentExtension.from(survivor).cacheHitCount(UI.Layer.BACKGROUND) >= 1

        when : 'One of the two is dropped and collected.'
            var twinWasCollected = eventually(24, { ghost.get() == null })
        then : 'It really is gone.'
            twinWasCollected
        and : """
            The shared rendering stayed, because the survivor still holds that style - and it
            keeps being served from the cache rather than having to render again.
        """
            var hitsBefore = ComponentExtension.from(survivor).cacheHitCount(UI.Layer.BACKGROUND)
            var missesBefore = ComponentExtension.from(survivor).cacheMissCount(UI.Layer.BACKGROUND)
            Utility.renderSingleComponent(survivor)
            ComponentExtension.from(survivor).cacheHitCount(UI.Layer.BACKGROUND) == hitsBefore + 1
            ComponentExtension.from(survivor).cacheMissCount(UI.Layer.BACKGROUND) == missesBefore
    }

    /**
     *  Builds a component with the supplied style, paints it until its style is cached, and
     *  returns only a weak handle on it. Everything strong is confined to this frame, which is
     *  what lets the caller assert that the component can be collected.
     */
    private static WeakReference<JComponent> paintAndForget( Closure builder ) {
        JComponent component = (JComponent) builder.call()
        component.setSize(260, 150)
        12.times { Utility.renderSingleComponent(component) }
        return new WeakReference<>(component)
    }

    /** As {@link #paintAndForget}, for a style handed in as a `Styler` so that a caller can
     *  build a second, identically styled component from the very same style. */
    private static WeakReference<JBox> paintAndForgetLike( Closure styler ) {
        var box = UI.box().withStyle(styler as swingtree.api.Styler).get(JBox)
        box.setSize(260, 150)
        12.times { Utility.renderSingleComponent(box) }
        return new WeakReference<>(box)
    }

    /** As {@link #paintAndForget}, but dragging the component through a range of sizes, which
     *  is what makes a noise bearing layer be cached in pieces rather than as one image. */
    private static WeakReference<JBox> dragAndForget( Closure styler ) {
        var box = UI.box().withStyle( conf -> styler(conf) ).get(JBox)
        (0..9).each { i ->
            box.setSize(220 + i * 12, 150 + i * 7)
            Utility.renderSingleComponent(box)
        }
        return new WeakReference<>(box)
    }

    private static int styleLayerEntries() {
        return UI.runAndGet({ComponentExtension.globalRenderCacheEntryCounts().toMap()["style layers"]})
    }

    /**
     *  Retries a condition with a real garbage collection in between, because
     *  {@link System#gc()} is only ever a suggestion and a single call proves nothing.
     */
    private static boolean eventually( int cycles, Closure<Boolean> condition ) {
        if ( condition.call() ) return true
        while ( cycles > 0 ) {
            waitForGarbageCollection()
            if ( condition.call() ) return true
            cycles--
        }
        return false // The last call above already ran after the last collection.
    }

    /**
     *  Attempts to trigger a collection and waits briefly until an object of our own is reclaimed,
     *  as evidence that the collector ran at least once (bounded to keep CI from spinning indefinitely).
     *  If the JVM keeps ignoring the hint, this method may return without having observed a collection.
     */
    private static void waitForGarbageCollection() {
        Object canary = new Object()
        var ref = new WeakReference<Object>(canary)
        canary = null
        for ( int attempt = 0; attempt < 100 && ref.get() != null; attempt++ ) {
            System.gc()
            Thread.sleep(10)
        }
    }
}
