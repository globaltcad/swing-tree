package swingtree.style;

import org.jspecify.annotations.Nullable;
import swingtree.SwingTree;

import javax.swing.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 *  A self-clearing, HiDPI-correct cache for rasterised label/button text, shared
 *  across every look-and-feel SwingTree forwards painting to (see
 *  {@link DynamicLaF} and {@link CachingTextGraphics2D}).
 *
 *  <p>Re-rasterising the same string in the same font and colour on every repaint
 *  is, in practice, the single biggest cost of a Swing UI under load (e.g. a
 *  window resize): the look-and-feel rebuilds a {@code TextLayout}, re-shapes the
 *  glyphs and composites each one in software, every frame. This cache renders a
 *  given <i>appearance</i> of a string once and then blits the resulting image.
 *
 *  <h2>Lifecycle: weak references do the real work, the budget is a backstop</h2>
 *  Entries are keyed by a {@link Pooled}-interned {@link TextKey}; equal keys
 *  collapse to one canonical instance. The canonical key is kept alive only by a
 *  {@link KeyHolder} the component keeps in its {@link ComponentExtension} extra-state
 *  store (which is itself a client property of the component, so it stays invisible
 *  from outside). While a component keeps drawing the same text it retains that key,
 *  keeping the image alive; the moment the text/font/colour changes it retains a
 *  different key and the stale image becomes weakly reachable and is collected. When
 *  the component itself is collected, its {@link ComponentExtension} — and therefore
 *  its {@link KeyHolder} and the keys it held — go with it. Two components drawing
 *  identical text share one image until the last of them stops. So the cache tracks
 *  live appearance exactly, with no invalidation to wire.
 *
 *  <p>On top of that self-clearing lifecycle sits a coarse memory-budget cap, shared
 *  with the style {@link LayerCache} via {@link LayerCache#DYNAMIC_CACHE_AGGRESSIVENESS()}
 *  (derived from system RAM): it bounds both the number of cached appearances
 *  ({@link #maxEntries()}) and the device-pixel size of any single cached image
 *  ({@link #maxImageArea()}). This never affects correctness — anything not cached is
 *  simply drawn directly — it only caps worst-case memory on machines with little of
 *  it, and lets a constrained device opt out of text caching entirely (budget 0).
 *
 *  <h2>Conservative by construction</h2>
 *  Only plain {@code drawString} with a solid {@link Color} paint and an
 *  axis-aligned transform is cached; everything else is forwarded so the result
 *  is always pixel-faithful. The buffer is rendered with forced grayscale
 *  antialiasing (subpixel/LCD AA assumes a known opaque backdrop, which a
 *  transparent buffer composited over an arbitrary background does not have).
 *
 *  <p>EDT-confined: any paint arriving off the Event Dispatch Thread (printing,
 *  image export) bypasses the cache, so the plain {@link WeakHashMap}s are never
 *  touched concurrently.
 */
final class TextRenderCache {

    private TextRenderCache() {}

    /** Paint number at which an appearance is promoted from direct drawing to a cached image. */
    private static final int MATERIALISE_AT = 3;

    /** User-space padding around the glyph box (absorbs left-side bearing, glyph
     *  overhang and antialiasing bleed so nothing is clipped by the image edge). */
    private static final int PAD = 2;

    /** Per unit of the shared {@link LayerCache#DYNAMIC_CACHE_AGGRESSIVENESS()} memory
     *  budget, the device-pixel area a single cached text image may occupy. Text is
     *  thin but can be wide, so a unit buys a generous strip of pixels. */
    private static final long PIXELS_PER_AGGRESSIVENESS = 512L * 256L; // 131072 device px / unit

    /** Absolute device-pixel ceiling for one cached text image, independent of how
     *  aggressively caching is configured (a final sanity guard against pathological text). */
    private static final long MAX_IMAGE_AREA = 2048L * 2048L;

    /** Per unit of the shared {@link LayerCache#DYNAMIC_CACHE_AGGRESSIVENESS()} memory
     *  budget, how many distinct text appearances may be materialised in the global
     *  cache. Text images are far cheaper than full style-layer images (a thin glyph
     *  strip vs. a filled component rectangle), so a unit buys many more entries than
     *  the style {@link LayerCache} grants. */
    private static final int ENTRIES_PER_AGGRESSIVENESS = 64;

    /** Absolute ceiling on the number of cached text entries, independent of aggressiveness. */
    private static final int MAX_ENTRIES = 4096;

    /** Maximum device-pixel area a single cached text image may occupy, scaled by the
     *  shared {@link LayerCache#DYNAMIC_CACHE_AGGRESSIVENESS()} budget so that
     *  memory-constrained machines cache more conservatively. */
    private static long maxImageArea() {
        return Math.min(MAX_IMAGE_AREA, PIXELS_PER_AGGRESSIVENESS * LayerCache.DYNAMIC_CACHE_AGGRESSIVENESS());
    }

    /** Maximum number of distinct text appearances the global cache will hold, scaled
     *  by the shared {@link LayerCache#DYNAMIC_CACHE_AGGRESSIVENESS()} budget. Once this
     *  is reached new appearances are drawn directly (never cached) until the
     *  weak-reference lifecycle frees room again. */
    private static int maxEntries() {
        return Math.min(MAX_ENTRIES, ENTRIES_PER_AGGRESSIVENESS * LayerCache.DYNAMIC_CACHE_AGGRESSIVENESS());
    }

    private static final AffineTransform IDENTITY = new AffineTransform();

    private static final Map<Pooled<TextKey>, Entry>  CACHE = new WeakHashMap<>();

    /** Component classes whose look-and-feel paint code broke when handed the proxy
     *  graphics (typically a cast to a concrete graphics type). They are never
     *  proxied again — graceful, self-healing degradation. Keyed by the painted
     *  component's own class ({@code c.getClass()}), which is what both
     *  {@link #isProxyable(JComponent)} and {@link #markUnsafe(Class)} use. */
    private static final java.util.Set<Class<?>> UNSAFE = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());


    /**
     *  Whether the given component's paint may be wrapped in the text-caching proxy.
     *  We deliberately keep the set of proxied components as small as it can be while
     *  still covering essentially all real text rendering — proxying more than that
     *  only adds cost and risk for no gain:
     *  <ol>
     *      <li><b>Globally enabled &amp; not self-healed away.</b> A component class
     *          whose paint once broke under the proxy is recorded in {@link #UNSAFE}
     *          (keyed by class) and is never proxied again.</li>
     *      <li><b>Leaf text components only.</b> Only components that paint their own
     *          text are eligible, so the proxy can never propagate across a child
     *          subtree (see {@link CachingTextGraphics2D#create()}); proxying a
     *          container was observed to cause flaky single-frame render dropouts.</li>
     *      <li><b>It must actually have text right now.</b> An icon-only label or an
     *          empty text field draws no cacheable {@code drawString}, so wrapping its
     *          paint would pay the proxy cost for nothing. This is re-checked on every
     *          paint, so a component that gains or loses text is picked up immediately.</li>
     *  </ol>
     *  The one exception to rule&nbsp;3 is {@link JComboBox}: it paints its display
     *  value through a <em>renderer</em> component (typically a {@link JLabel}) on a
     *  graphics derived from its own, so the proxy reaches that text via
     *  {@link CachingTextGraphics2D#create() create()}-propagation rather than the
     *  combo's own {@code getText()}. Gating it on the combo's text would wrongly
     *  disable it, so it is allowed unconditionally (subject only to rule&nbsp;1).
     *
     * @param c The component about to be painted.
     * @return {@code true} if its paint should be routed through the text-caching proxy.
     */
    static boolean isProxyable(JComponent c) {
        // Off-EDT paints (printing, image export) bypass the cache anyway (see
        // paintString), and installing the proxy would still mutate the component's
        // extra-state via beginPaint(), which is not safe off the EDT. So we never
        // proxy off the EDT: such a paint uses the original graphics untouched.
        if ( !SwingUtilities.isEventDispatchThread() )
            return false;

        final Class<?> componentClass = c.getClass();

        // Rule 1: global switch + per-class self-healing opt-out (keyed by component class).
        if ( !SwingTree.get().isTextCachingEnabled() || UNSAFE.contains(componentClass) )
            return false;

        // Exception to rule 3: a combo box's text is painted by a propagated renderer,
        // not by its own getText(), so we never apply the has-text gate to it.
        if ( c instanceof JComboBox )
            return true;

        // Rule 2: recognise the leaf text components and grab the text each paints itself.
        final String ownText;
        if ( c instanceof JLabel )
            ownText = ((JLabel) c).getText();
        else if ( c instanceof AbstractButton )   // JButton, JToggleButton, JCheckBox, JRadioButton, JMenuItem, ...
            ownText = ((AbstractButton) c).getText();
        else if ( c instanceof JTextField )       // incl. JPasswordField, JFormattedTextField
            ownText = ((JTextField) c).getText();
        else
            return false;                         // not a recognised leaf text component -> never proxy

        // Rule 3: only worth wrapping while there is actually text to draw.
        return ownText != null && !ownText.isEmpty();
    }

    /** Permanently disables proxying for a component class after its paint failed once. */
    static void markUnsafe(Class<?> componentClass) { UNSAFE.add(componentClass); }

    // ─────────────────────────── introspection (for tests / living documentation) ──

    /** Number of distinct text appearances the given component retained during its
     *  most recent paint (the size of its {@link KeyHolder}). */
    static int retainedKeyCount(JComponent c) {
        KeyHolder h = holderOf(c);
        return h == null ? 0 : h._keys.size();
    }

    /** {@code true} if at least one of the component's currently retained text
     *  appearances has been promoted to a cached image (and is therefore blitted
     *  rather than re-rendered). */
    static boolean hasMaterialisedText(JComponent c) {
        KeyHolder h = holderOf(c);
        if ( h == null ) return false;
        for ( Pooled<TextKey> key : h._keys ) {
            Entry e = CACHE.get(key);
            if ( e != null && e.image != null ) return true;
        }
        return false;
    }

    /** Total number of live entries in the global text cache. Stale, garbage-collected
     *  keys are expunged by {@link WeakHashMap#size()} before counting. No bookkeeping
     *  poke is needed: a component's {@link KeyHolder} lives in its
     *  {@link ComponentExtension}, so it is collected together with the component, which
     *  releases its keys and lets their cache entries become reclaimable directly. */
    static int globalEntryCount() {
        return CACHE.size();
    }

    /** Drops every cached entry. Intended for tests that need a clean, deterministic
     *  starting point. (Per-component {@link KeyHolder}s live in each component's
     *  {@link ComponentExtension} and simply repopulate on the next paint.) */
    static void clearForTesting() {
        CACHE.clear();
    }

    /** The {@link KeyHolder} a component keeps in its {@link ComponentExtension}
     *  extra-state store, or {@code null} if it has never been painted with text
     *  caching active. */
    private static @Nullable KeyHolder holderOf(JComponent c) {
        return ComponentExtension.from(c).get(KeyHolder.class).orElse(null);
    }

    /**
     *  Returns the component's {@link KeyHolder}, reset for a fresh paint pass.
     *  The holder lives in the component's {@link ComponentExtension} extra-state store
     *  (which is itself a client property of the component), so it lives and dies with
     *  the component and keeps this paint's cache entries alive while the component
     *  still draws them.
     */
    static KeyHolder beginPaint(JComponent c) {
        KeyHolder h = ComponentExtension.from(c).getOrSet(KeyHolder.class, KeyHolder::new);
        h.reset();
        return h;
    }

    /**
     *  Attempts to satisfy {@code delegate.drawString(text, x, y)} from the cache.
     *
     * @return {@code true} if the text was drawn (from a cached image); {@code false}
     *         if the caller should perform the original {@code drawString} itself.
     */
    static boolean paintString(
            Graphics2D delegate, KeyHolder holder, @Nullable String text, float x, float y
    ) {
        // No enabled-check needed: the proxy is only created when isProxyable() was true.
        if ( text == null || text.isEmpty() )          return false;
        if ( !SwingUtilities.isEventDispatchThread() ) return false;

        Paint paint = delegate.getPaint();
        if ( !(paint instanceof Color) ) return false;      // gradient/texture text -> can't key on it

        AffineTransform tx = delegate.getTransform();
        if ( !isAxisAligned(tx) ) return false;             // rotated/sheared -> can't blit 1:1

        Font font = delegate.getFont();
        Color color = (Color) paint;

        long visualHash = visualHash(delegate, tx, font, color);
        Pooled<TextKey> key = new Pooled<>(new TextKey(text, visualHash)).intern();

        Entry entry = CACHE.get(key);
        if ( entry == null ) {
            if ( CACHE.size() >= maxEntries() )
                return false;   // global cache is at its memory-budget cap -> draw directly, don't grow
            entry = new Entry();
            CACHE.put(key, entry);
        }
        holder.retain(key);     // an existing appearance is always retained; a new one only once admitted

        /*
            Fast path — the appearance is already materialised, so just blit it.
            We deliberately keep FontMetrics OUT of this path: FontMetrics.stringWidth(text)
            builds a TextLayout to measure the glyph advances and, per real benchmarks, costs
            several times more than the blit itself. We don't need it here: the cached image
            already encodes the text's width and height, and to position the blit we only need
            the ascent (a font-level constant, so it is the same on every paint of this entry
            because the font is part of the cache key) which we captured when materialising.
        */
        if ( entry.image != null ) {
            int boundsX = (int) Math.floor(x);
            int boundsY = (int) Math.floor(y - entry.ascent);
            blit(delegate, tx, entry.image, boundsX - PAD, boundsY - PAD);
            return true;
        }

        // Warm-up — draw directly (no measuring) until the appearance proves stable.
        if ( ++entry.hits < MATERIALISE_AT ) return false;

        // Materialise — runs once per appearance; this is the only place we measure the string.
        FontMetrics fm = delegate.getFontMetrics(font);
        int ascent  = fm.getAscent();
        int boundsX = (int) Math.floor(x);
        int boundsY = (int) Math.floor(y - ascent);
        int boundsW = fm.stringWidth(text);
        int boundsH = fm.getHeight();
        if ( boundsW <= 0 || boundsH <= 0 ) return false;

        BufferedImage img = materialise(delegate, tx, font, color, text,
                                        x, y, boundsX, boundsY, boundsW, boundsH);
        if ( img == null ) return false;                            // too large -> stay direct
        entry.image  = img;
        entry.ascent = ascent;                                      // captured for the fast path above

        blit(delegate, tx, img, boundsX - PAD, boundsY - PAD);
        return true;
    }

    private static @Nullable BufferedImage materialise(
            Graphics2D delegate, AffineTransform tx, Font font, Color color, String text,
            float x, float y, int boundsX, int boundsY, int boundsW, int boundsH
    ) {
        double sx = tx.getScaleX();
        double sy = tx.getScaleY();
        int w = boundsW + 2 * PAD;
        int h = boundsH + 2 * PAD;
        int imgW = Math.max(1, (int) Math.ceil(w * sx));
        int imgH = Math.max(1, (int) Math.ceil(h * sy));
        if ( (long) imgW * imgH > maxImageArea() ) return null;

        BufferedImage image = newCompatibleImage(delegate, imgW, imgH);
        Graphics2D bg = image.createGraphics();
        try {
            bg.setRenderingHints(delegate.getRenderingHints());        // inherit fractional metrics etc.
            // Subpixel/LCD text AA assumes a known opaque backdrop, which our transparent
            // buffer (composited later over an arbitrary background) does not have, so we
            // downgrade to plain grayscale AA to avoid colour fringing. But an *explicit*
            // "AA off" is honoured, otherwise we would blit antialiased pixels where the
            // look-and-feel asked for crisp, aliased text.
            Object textAA = delegate.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            if ( !RenderingHints.VALUE_TEXT_ANTIALIAS_OFF.equals(textAA) )
                bg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            bg.scale(sx, sy);
            bg.translate(-(boundsX - PAD), -(boundsY - PAD));
            bg.setFont(font);
            bg.setColor(color);
            bg.drawString(text, x, y);                                 // identical glyph layout to the delegate
        } finally {
            bg.dispose();
        }
        return image;
    }

    private static void blit(Graphics2D g, AffineTransform tx, BufferedImage image, double userX, double userY) {
        Point2D p = tx.transform(new Point2D.Double(userX, userY), null);
        int devX = (int) Math.round(p.getX());
        int devY = (int) Math.round(p.getY());
        // The clip is unaffected by this temporary identity transform: Java2D holds the
        // active clip in *device* space, so setTransform only changes how future user-space
        // coordinates map to it — it does not move the existing clipped region. Drawing the
        // image at the device coordinates of the glyph box is therefore clipped by exactly
        // the same pixels the original drawString(text, x, y) would have been.
        g.setTransform(IDENTITY);                                       // 1:1 device blit (no resample)
        try {
            g.drawImage(image, devX, devY, null);
        } finally {
            g.setTransform(tx);
        }
    }

    private static BufferedImage newCompatibleImage(Graphics2D g, int w, int h) {
        GraphicsConfiguration gc = g.getDeviceConfiguration();
        BufferedImage image = ( gc != null )
                ? gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT) // device color model -> no per-blit conversion
                : new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        /*
            This image is rendered once and then blitted on every repaint (e.g. every
            frame of a resize), and its pixels are never modified or read back again.
            That is exactly the managed-image pattern Java2D accelerates: it keeps a
            copy of the image in video memory (an OpenGL/D3D texture or an X11 pixmap)
            and serves the repeated drawImage calls from there. Raising the acceleration
            priority from the 0.5 default to the maximum tells Java2D to prioritise
            keeping that accelerated copy resident when video memory is contended.
        */
        image.setAccelerationPriority(1.0f);
        return image;
    }

    private static boolean isAxisAligned(AffineTransform tx) {
        int allowed = AffineTransform.TYPE_TRANSLATION
                    | AffineTransform.TYPE_UNIFORM_SCALE
                    | AffineTransform.TYPE_GENERAL_SCALE;
        return (tx.getType() & ~allowed) == 0;
    }

    /** All pixel-affecting inputs of a {@code drawString} call folded into a well-mixed 64-bit value. */
    private static long visualHash(Graphics2D g, AffineTransform tx, Font font, Color color) {
        long h = 1469598103934665603L;                                  // FNV-1a offset basis
        h = (h ^ font.hashCode())                              * 1099511628211L;
        h = (h ^ color.getRGB())                               * 1099511628211L;
        h = (h ^ hint(g, RenderingHints.KEY_TEXT_ANTIALIASING))* 1099511628211L;
        h = (h ^ hint(g, RenderingHints.KEY_FRACTIONALMETRICS))* 1099511628211L;
        h = (h ^ hint(g, RenderingHints.KEY_TEXT_LCD_CONTRAST))* 1099511628211L;
        h = (h ^ Double.hashCode(tx.getScaleX()))             * 1099511628211L;
        h = (h ^ Double.hashCode(tx.getScaleY()))             * 1099511628211L;
        return mix(h);
    }

    private static int hint(Graphics2D g, RenderingHints.Key key) {
        return Objects.hashCode(g.getRenderingHint(key));
    }

    /** SplitMix64 finalizer — avalanches the bits so the 64-bit hash behaves like
     *  a random value, making accidental collisions astronomically unlikely. */
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Mutable per-appearance slot. EDT-confined, so no synchronisation. */
    private static final class Entry {
        int                                  hits;
        @Nullable BufferedImage              image;
        int                                  ascent; // font ascent captured at materialise time; positions the blit
    }

    /** Holds the <i>distinct</i> keys used during the current paint of one component,
     *  so their cache entries are not collected while that component is still painting
     *  them. {@link #retain(Pooled)} is idempotent: a look-and-feel that issues the same
     *  appearance more than once in a single paint (the cache key ignores position, so
     *  the same text/font/colour at two spots is one appearance) is recorded once, which
     *  is what {@link #retainedKeyCount(JComponent)} reports as the distinct count. The
     *  list is tiny (typically one entry), so the linear de-duplication is negligible. */
    static final class KeyHolder {
        private final List<Pooled<TextKey>> _keys = new ArrayList<>(2);
        void reset()                          { _keys.clear(); }
        void retain(Pooled<TextKey> key)      { if ( !_keys.contains(key) ) _keys.add(key); }
    }

    /** The value-object cache key: the exact string plus a 64-bit hash of all of
     *  its visual properties. The string is compared exactly, so a wrong cache hit
     *  would require two identical strings whose visual hashes also collide. */
    static final class TextKey {
        private final String text;
        private final long   visualHash;
        private final int    hash;

        TextKey(String text, long visualHash) {
            this.text       = text;
            this.visualHash = visualHash;
            this.hash       = text.hashCode() * 31 + Long.hashCode(visualHash);
        }

        @Override public boolean equals(@Nullable Object o) {
            if ( this == o ) return true;
            if ( !(o instanceof TextKey) ) return false;
            TextKey other = (TextKey) o;
            return this.hash == other.hash
                && this.visualHash == other.visualHash
                && this.text.equals(other.text);
        }
        @Override public int hashCode() { return hash; }
    }
}
