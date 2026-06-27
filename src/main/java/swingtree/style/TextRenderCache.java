package swingtree.style;

import org.jspecify.annotations.Nullable;
import swingtree.SwingTree;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
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
 *  <h2>Lifecycle and why it never needs a size cap</h2>
 *  Entries are keyed by a {@link Pooled}-interned {@link TextKey}; equal keys
 *  collapse to one canonical instance. The canonical key is kept alive only by a
 *  {@link KeyHolder} the component stores on itself as a client property (keyed by
 *  the package-private {@link KeyHolder} class, so it is invisible from outside).
 *  While a component keeps drawing the same text it retains that key, keeping the
 *  image alive; the moment the text/font/colour changes it retains a different key
 *  and the stale image becomes weakly reachable and is collected. When the component
 *  itself is collected, its client properties — and therefore its {@link KeyHolder}
 *  and the keys it held — go with it. Two components drawing identical text share one
 *  image until the last of them stops. So the cache tracks live appearance exactly —
 *  no cap to tune, no invalidation to wire.
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

    /** Per-image device-pixel sanity limit (not a cache-size cap). */
    private static final long MAX_IMAGE_AREA = 2048L * 2048L;

    private static final AffineTransform IDENTITY = new AffineTransform();

    private static final Map<Pooled<TextKey>, Entry>  CACHE = new WeakHashMap<>();

    /** Look-and-feel UI classes whose paint code broke when handed the proxy
     *  graphics (typically a cast to a concrete graphics type). They are never
     *  proxied again — graceful, self-healing degradation. */
    private static final java.util.Set<Class<?>> UNSAFE = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());


    /** Whether painting through the given former-UI class may be proxied for text caching. */
    static boolean isProxyable(Class<?> formerUiClass) {
        return SwingTree.get().isTextCachingEnabled() && !UNSAFE.contains(formerUiClass);
    }

    /** Permanently disables proxying for a former-UI class after it failed once. */
    static void markUnsafe(Class<?> formerUiClass) { UNSAFE.add(formerUiClass); }

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
     *  poke is needed: a component's {@link KeyHolder} lives on the component itself
     *  (as a client property), so it is collected together with the component, which
     *  releases its keys and lets their cache entries become reclaimable directly. */
    static int globalEntryCount() {
        return CACHE.size();
    }

    /** Drops every cached entry. Intended for tests that need a clean, deterministic
     *  starting point. (Per-component {@link KeyHolder}s live on the components as
     *  client properties and simply repopulate on the next paint.) */
    static void clearForTesting() {
        CACHE.clear();
    }

    /** The {@link KeyHolder} a component stores on itself, or {@code null} if it has
     *  never been painted with text caching active. */
    private static @Nullable KeyHolder holderOf(JComponent c) {
        Object h = c.getClientProperty(KeyHolder.class);
        return (h instanceof KeyHolder) ? (KeyHolder) h : null;
    }

    /**
     *  Returns the component's {@link KeyHolder}, reset for a fresh paint pass.
     *  The holder is stored on the component itself as a client property (keyed by the
     *  package-private {@link KeyHolder} class), so it lives and dies with the component
     *  and keeps this paint's cache entries alive while the component still draws them.
     */
    static KeyHolder beginPaint(JComponent c) {
        KeyHolder h = holderOf(c);
        if ( h == null ) {
            h = new KeyHolder();
            c.putClientProperty(KeyHolder.class, h);
        }
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
            Graphics2D delegate, KeyHolder holder, String text, float x, float y
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
        holder.retain(key);

        Entry entry = CACHE.get(key);
        if ( entry == null ) { entry = new Entry(); CACHE.put(key, entry); }
        entry.hits++;

        FontMetrics fm = delegate.getFontMetrics(font);
        int ascent = fm.getAscent();
        int boundsX = (int) Math.floor(x);
        int boundsY = (int) Math.floor(y - ascent);
        int boundsW = fm.stringWidth(text);
        int boundsH = fm.getHeight();
        if ( boundsW <= 0 || boundsH <= 0 ) return false;

        if ( entry.image == null ) {
            if ( entry.hits < MATERIALISE_AT ) return false;            // still warming up
            BufferedImage img = materialise(delegate, tx, font, color, text,
                                            x, y, boundsX, boundsY, boundsW, boundsH);
            if ( img == null ) return false;                            // too large -> stay direct
            entry.image = img;
        }
        blit(delegate, tx, entry.image, boundsX - PAD, boundsY - PAD);
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
        if ( (long) imgW * imgH > MAX_IMAGE_AREA ) return null;

        BufferedImage image = newCompatibleImage(delegate, imgW, imgH);
        Graphics2D bg = image.createGraphics();
        try {
            bg.setRenderingHints(delegate.getRenderingHints());        // inherit fractional metrics etc.
            bg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  // force grayscale to avoid LCD fringing
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
        g.setTransform(IDENTITY);                                       // 1:1 device blit (no resample)
        try {
            g.drawImage(image, devX, devY, null);
        } finally {
            g.setTransform(tx);
        }
    }

    private static BufferedImage newCompatibleImage(Graphics2D g, int w, int h) {
        GraphicsConfiguration gc = g.getDeviceConfiguration();
        if ( gc != null )
            return gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
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
    }

    /** Holds the keys used during the current paint of one component, so their
     *  cache entries are not collected while that component is still painting them. */
    static final class KeyHolder {
        private final List<Pooled<TextKey>> _keys = new ArrayList<>(2);
        void reset()                          { _keys.clear(); }
        void retain(Pooled<TextKey> key)      { _keys.add(key); }
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
