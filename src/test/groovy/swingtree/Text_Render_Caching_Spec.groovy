package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title

import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import java.lang.ref.WeakReference
import swingtree.style.ComponentExtension
import swingtree.threading.EventProcessor
import utility.Utility

import javax.swing.JButton
import javax.swing.JLabel
import java.awt.Color

@Title("Text Render Caching")
@Narrative('''

    Re-rendering text is, perhaps surprisingly, one of the most expensive
    things a Swing UI does on a repaint. Every time a label, button or text
    field is painted, the installed look-and-feel re-shapes the glyphs and
    composites them onto the surface – and under a window resize that happens
    for every component, every frame. Yet the *same* string in the *same*
    font and colour produces byte-for-byte identical pixels each time.

    SwingTree therefore ships a **text-render cache**: it transparently
    intercepts the `drawString` calls the look-and-feel makes, rasterises a
    given *appearance* of a piece of text once into an image, and then blits
    that image on subsequent paints instead of laying the glyphs out again.
    Because the interception happens at the `Graphics.drawString` boundary,
    it works for **any** look-and-feel (FlatLaf, Metal, Nimbus, …) and for
    components SwingTree never styled, without the look-and-feel having to
    know anything about it.

    This optimisation is **enabled by default** and can be turned off – either
    globally during library setup via
    `SwingTree.initializeUsing(cfg -> cfg.withTextCaching(false))`, at runtime
    via `SwingTree.get().setTextCachingEnabled(false)`, or through the
    `-Dswingtree.textCache=false` system property.

    This specification documents the *operational* behaviour of the cache –
    **when** something ends up cached and **when** it is automatically
    discarded again – purely through the public `ComponentExtension`
    introspection API:

      - `ComponentExtension.from(c).hasCachedText()` – does this component have
        a rasterised, blittable image of its text right now?
      - `ComponentExtension.from(c).cachedTextEntryCount()` – how many distinct
        text appearances is it keeping alive?
      - `ComponentExtension.totalTextCacheSize()` – how many text appearances
        are live in the shared global cache (used to observe sharing and the
        automatic, garbage-collection-driven cleanup).

    As with the style render caching, this is intentionally a black-box,
    public-API-only account so that it can survive aggressive internal
    refactors and serve as a guide to the feature.

''')
@Subject([ComponentExtension, SwingTree, UI])
@Timeout(value = 25, unit = TimeUnit.SECONDS)
class Text_Render_Caching_Spec extends Specification
{
    def setupSpec() {
        // The text cache shares the unified memory budget (`CacheBudget`, scaled by the
        // configured cache mode and system RAM), which would otherwise make the
        // size-related scenarios flaky across CI runners. We pin it to a generous, deterministic
        // value so that none of the *ordinary* scenarios below ever brush against
        // the cap. The one scenario that deliberately exercises the cap overrides
        // this locally and restores it again.
        swingtree.style.CacheBudget.UNITS_OVERRIDE = 10
    }

    def cleanupSpec() {
        swingtree.style.CacheBudget.UNITS_OVERRIDE = -1
    }

    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        SwingTree.get().setUiScaleFactor(1f)
        SwingTree.get().setTextCachingEnabled(true) // the default, made explicit
        ComponentExtension.clearTextCache()         // deterministic starting point
    }

    def cleanup() {
        ComponentExtension.clearTextCache()
        SwingTree.clear()
    }

    /** Sizes a parentless component to its preferred size (so it actually has
     *  room to draw its text) and then paints it through the regular pipeline.
     *  A minimum of 1x1 keeps the offscreen image valid even for empty text. */
    private void paint(java.awt.Component c) {
        def pref = c.getPreferredSize()
        c.setSize(Math.max(1, (int) pref.width), Math.max(1, (int) pref.height))
        Utility.renderSingleComponent(c)
    }

    /**
     * Polls {@code condition} up to {@code numberOfCycles} times, forcing a
     * *guaranteed* garbage collection between checks. This is how the GC-driven
     * cleanup of the (weakly held) cache entries is observed reliably, instead
     * of the unreliable {@code System.gc()}-and-sleep dance.
     */
    static boolean eventually(int numberOfCycles, Supplier<Boolean> condition) {
        if ( condition.get() )
            return true
        while ( numberOfCycles > 0 ) {
            waitForGarbageCollection()
            if ( condition.get() )
                return true
            else
                numberOfCycles--
        }
        return condition.get()
    }

    /** Guarantees a garbage collection actually completed (unlike {@link System#gc()}). */
    static void waitForGarbageCollection() {
        Object obj = new Object()
        WeakReference ref = new WeakReference<>(obj)
        obj = null
        while ( ref.get() != null ) {
            System.gc()
        }
    }

    def 'A label\'s text is cached after a short warm-up, not on the very first paint.'()
    {
        reportInfo """
            The cache deliberately does **not** rasterise text on its first
            sighting. Doing so would waste an image on text that is only ever
            drawn once – think of the constantly changing strings a single
            `JTable`/`JList` cell renderer pushes through one shared label.

            Instead, an appearance is drawn directly for the first couple of
            paints and only *promoted* to a cached image once it has proven to
            be stable (it is drawn a third time). From then on the image is
            simply blitted.

            We can observe this warm-up directly: the appearance is *tracked*
            from the first paint (`cachedTextEntryCount() == 1`), but a
            blittable image (`hasCachedText()`) only appears on the third paint.
        """
        given : 'A plain label – the kind of component the cache exists for.'
            var label = UI.label("Departure 14:25").get(JLabel)
            var ext   = ComponentExtension.from(label)

        when : 'We paint it once.'
            paint(label)
        then : 'Its single text appearance is being tracked...'
            ext.cachedTextEntryCount() == 1
        and  : '...but it has not yet been promoted to a cached image (still warming up).'
            !ext.hasCachedText()

        when : 'We paint it a second time.'
            paint(label)
        then : 'Still warming up – no cached image yet.'
            !ext.hasCachedText()

        when : 'We paint it a third time.'
            paint(label)
        then : 'The appearance is now promoted: a blittable image exists and is reused from here on.'
            ext.hasCachedText()
            ext.cachedTextEntryCount() == 1
    }

    def 'With text caching turned off, nothing is ever cached.'()
    {
        reportInfo """
            The whole mechanism is gated behind a single switch. When it is
            turned off – `SwingTree.get().setTextCachingEnabled(false)`,
            `cfg.withTextCaching(false)` during setup, or
            `-Dswingtree.textCache=false` – SwingTree never installs its
            text-caching proxy, so no appearance is ever tracked and no image
            is ever allocated, no matter how many times a component is painted.

            This is the control case for every other scenario here: it proves
            that the cached state we observe elsewhere is genuinely caused by
            the feature and not by some other part of the pipeline.
        """
        given : 'Text caching is switched off.'
            SwingTree.get().setTextCachingEnabled(false)
        and : 'A perfectly ordinary label.'
            var label = UI.label("Departure 14:25").get(JLabel)
            var ext   = ComponentExtension.from(label)

        when : 'We paint it many times – well past the warm-up threshold.'
            5.times { paint(label) }

        then : 'No text appearance was ever tracked for the component...'
            ext.cachedTextEntryCount() == 0
        and  : '...no blittable image exists...'
            !ext.hasCachedText()
        and  : '...and the shared global cache stayed empty.'
            ComponentExtension.totalTextCacheSize() == 0
    }

    def 'Components that draw the exact same text share a single cached image.'()
    {
        reportInfo """
            Cache entries are keyed by the text *appearance* (the string plus
            its font, colour, antialiasing hints and HiDPI scale) and interned,
            so structurally identical appearances collapse onto one shared
            image in the global pool. A row of identical buttons therefore
            costs a single rasterisation.

            We demonstrate this by fully warming one label and then painting a
            *second*, freshly created label with the exact same text only
            once: it is served from the already-warmed shared entry on its
            very first paint, and the global cache still holds just one entry.
        """
        given : 'Two independently created labels that happen to show identical text.'
            var first  = UI.label("OK").get(JLabel)
            var second = UI.label("OK").get(JLabel)
            var extFirst  = ComponentExtension.from(first)
            var extSecond = ComponentExtension.from(second)

        when : 'We fully warm up the first label (three paints).'
            3.times { paint(first) }
        then : 'It now has a cached image, and the global cache holds exactly one entry.'
            extFirst.hasCachedText()
            ComponentExtension.totalTextCacheSize() == 1

        when : 'We paint the second label just once.'
            paint(second)
        then : 'It is immediately served from the shared entry – no warm-up needed.'
            extSecond.hasCachedText()
        and  : 'And no second entry was allocated: both labels share one image.'
            ComponentExtension.totalTextCacheSize() == 1
    }

    def 'Text that looks different is cached separately, even for the same string.'()
    {
        reportInfo """
            The cache key is the full visual appearance, not just the
            characters. Two labels showing the same string but in a different
            colour (or font, or at a different HiDPI scale) are genuinely
            different pixels, so they get separate cache entries. This is what
            guarantees the cache can never blit the wrong-looking text.
        """
        given : 'Two labels with the same text but different foreground colours.'
            var red  = UI.label("Total").get(JLabel)
            var blue = UI.label("Total").get(JLabel)
            red.setForeground(Color.RED)
            blue.setForeground(Color.BLUE)

        when : 'We warm both of them up.'
            3.times { paint(red)  }
            3.times { paint(blue) }

        then : 'Both are cached...'
            ComponentExtension.from(red).hasCachedText()
            ComponentExtension.from(blue).hasCachedText()
        and  : '...but as two distinct entries, because their appearance differs.'
            ComponentExtension.totalTextCacheSize() == 2
    }

    def 'A component that draws no text never populates the cache.'()
    {
        reportInfo """
            The cache only ever engages for actual text drawing. A label with
            an empty string draws nothing, so it is never tracked and never
            allocates an image – the machinery conservatively stays out of the
            way for components that have no text to cache.
        """
        given : 'A label with no text at all.'
            var label = UI.label("").get(JLabel)
            var ext   = ComponentExtension.from(label)

        when : 'We paint it several times.'
            3.times { paint(label) }

        then : 'Nothing was tracked and nothing was cached.'
            ext.cachedTextEntryCount() == 0
            !ext.hasCachedText()
            ComponentExtension.totalTextCacheSize() == 0
    }

    def 'A label begins caching only once it actually has text, and that is re-checked every paint.'()
    {
        reportInfo """
            Wrapping a component's paint in the text-caching proxy only pays off if the
            component actually draws text. An icon-only or as-yet-empty label has
            nothing to rasterise, so SwingTree does not bother wrapping its paint at
            all. Crucially this is decided *afresh on every paint*, so a label that is
            given text later transparently starts being cached from then on (and would
            stop again, freeing its entry, if its text were cleared).

            This is the dynamic counterpart to the empty-label control case above: same
            "no text, nothing cached" outcome to begin with, but here we watch caching
            switch on the moment real text appears.
        """
        given : 'A label created without any text yet.'
            var label = UI.label("").get(JLabel)
            var ext   = ComponentExtension.from(label)

        when : 'We paint the still-empty label well past the warm-up threshold.'
            4.times { paint(label) }
        then : 'Nothing is tracked or cached: there was no text worth wrapping the paint for.'
            ext.cachedTextEntryCount() == 0
            !ext.hasCachedText()
            ComponentExtension.totalTextCacheSize() == 0

        when : 'The label is later given some text, which we then warm up.'
            label.setText("Now visible")
            3.times { paint(label) }
        then : 'From now on it caches its text exactly like any other label.'
            ext.hasCachedText()
            ext.cachedTextEntryCount() == 1
    }

    def 'When a component changes its text, the stale entry is reclaimed automatically.'()
    {
        reportInfo """
            This is the heart of why the cache needs neither a size cap nor any
            manual invalidation: entries are held **weakly**, kept alive only
            for as long as some component is still drawing that appearance.

            A component retains the key for the text it is *currently* drawing.
            The moment its text changes, it retains the *new* key instead and
            lets go of the old one. With nothing else referencing it, the stale
            entry becomes eligible for garbage collection and disappears from
            the global cache on the next GC cycle – entirely on its own.

            We warm up an appearance, change the label's text (warming the new
            one), and then observe the global cache shrink back as the old
            entry is collected.
        """
        given : 'A label we will warm up and then re-text.'
            var label = UI.label("AAAA").get(JLabel)
            var ext   = ComponentExtension.from(label)

        when : 'We warm up the original text.'
            3.times { paint(label) }
        then : 'There is one live entry, cached for this component.'
            ext.hasCachedText()
            ComponentExtension.totalTextCacheSize() == 1

        when : 'The label is given a different text, which we warm up too.'
            label.setText("BBBB")
            3.times { paint(label) }
        then : 'Briefly, both the old and the new appearance live in the cache.'
            ComponentExtension.totalTextCacheSize() == 2
        and  : 'The component itself now caches the new appearance.'
            ext.hasCachedText()

        when : 'We let the garbage collector run.'
            boolean shrankBack = eventually(10, { ComponentExtension.totalTextCacheSize() == 1 })
        then : 'The stale entry for the old text was reclaimed automatically – only the live one remains.'
            shrankBack
        and  : 'The component still has its current text cached.'
            ext.hasCachedText()
    }

    def 'A cache entry never outlives the components that use it.'()
    {
        reportInfo """
            The same weak-reference lifecycle means an entry is also reclaimed
            once the component(s) using it are themselves discarded – so the
            cache can never grow without bound, even across a long-lived
            application that creates and throws away many short-lived,
            text-bearing components.

            We warm up a throwaway label, drop every reference to it, and
            observe the entry it owned disappear from the global cache once the
            label is collected.
        """
        given : 'A throwaway label which we warm up so it owns a cache entry.'
            var label = UI.label("Ephemeral").get(JLabel)
            3.times { paint(label) }
        and : 'The global cache now holds that one entry.'
            ComponentExtension.totalTextCacheSize() == 1

        when : 'We drop every reference to the label and let the GC run.'
            label = null
        then : 'The entry is gone: nothing keeps it alive once its component is collected.'
            eventually(10, { ComponentExtension.totalTextCacheSize() == 0 })
    }

    def 'A shared entry lives until the *last* component using it is gone.'()
    {
        reportInfo """
            Because identical text shares a single entry, that entry's lifetime
            is tied to *all* of its users together: it survives as long as any
            one of them is still around, and is only reclaimed once the last one
            has been discarded. This is the precise, reference-counting-like
            behaviour you get "for free" from weak keys and interning.
        """
        given : 'Two labels showing identical text, both warmed up.'
            var a = UI.label("Shared").get(JLabel)
            var b = UI.label("Shared").get(JLabel)
            3.times { paint(a) }
            3.times { paint(b) }
        and : 'They share a single cache entry.'
            ComponentExtension.totalTextCacheSize() == 1

        when : 'We discard the first label and let the GC run.'
            a = null
        then : 'The shared entry survives – the second label is still using it.'
            !eventually(3, { ComponentExtension.totalTextCacheSize() == 0 })
            ComponentExtension.totalTextCacheSize() == 1
        and : 'And the surviving label still has its text cached.'
            ComponentExtension.from(b).hasCachedText()

        when : 'We discard the second (last) label as well.'
            b = null
        then : 'Only now is the shared entry reclaimed.'
            eventually(10, { ComponentExtension.totalTextCacheSize() == 0 })
    }

    def 'On a machine with no spare memory budget, text caching backs off entirely.'()
    {
        reportInfo """
            The text cache does not allocate without regard for the host machine:
            it shares the very same memory budget as the style render cache
            (`LayerCache`'s dynamic *cache aggressiveness*, which is derived from
            the amount of system RAM). That budget bounds both how many distinct
            text appearances may be held and how large any single cached image may
            be.

            At the extreme – a budget of zero, i.e. a machine SwingTree considers
            too memory-constrained to spend anything on caching – the text cache
            turns itself off completely: appearances are drawn directly, exactly
            as if the feature were disabled, and nothing is ever tracked or
            allocated. Crucially this never changes *what* is drawn, only whether
            it is cached, so correctness is unaffected.

            We simulate such a machine by pinning the shared budget to zero and
            confirm that no amount of repainting populates the cache.
        """
        given : 'We pin the shared cache memory budget to zero (a maximally constrained machine).'
            swingtree.style.CacheBudget.UNITS_OVERRIDE = 0
        and : 'An ordinary label that would normally be cached after warm-up.'
            var label = UI.label("Departure 14:25").get(JLabel)
            var ext   = ComponentExtension.from(label)

        when : 'We paint it well past the usual warm-up threshold.'
            5.times { paint(label) }

        then : 'Nothing was tracked, nothing was cached, the global cache stayed empty.'
            ext.cachedTextEntryCount() == 0
            !ext.hasCachedText()
            ComponentExtension.totalTextCacheSize() == 0

        cleanup : 'Restore the generous, deterministic budget for the remaining scenarios.'
            swingtree.style.CacheBudget.UNITS_OVERRIDE = 10
    }

    def 'Buttons are cached the same way labels are.'()
    {
        reportInfo """
            The cache is not label-specific. Because it works at the
            `drawString` boundary, every text-bearing SwingTree component –
            buttons, text fields, and so on – benefits from it in exactly the
            same way. Here we confirm it for a button.
        """
        given : 'A plain button.'
            var button = UI.button("Submit").get(JButton)
            var ext    = ComponentExtension.from(button)

        when : 'We warm it up.'
            3.times { paint(button) }

        then : 'Its text is cached, just like a label\'s.'
            ext.hasCachedText()
            ext.cachedTextEntryCount() >= 1
    }
}
