package swingtree.style;

import org.jspecify.annotations.Nullable;
import swingtree.SwingTree;
import swingtree.SwingTreeInitConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

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
 *  <h2>Two layers behind one coarse key</h2>
 *  Entries are keyed coarsely by {@link TextKey text + font} only — nothing
 *  pixel-specific. Each {@link Entry} then holds two things:
 *  <ul>
 *    <li>the string's one layout <b>width</b> — a colour/scale-independent {@code int},
 *        filled the first time a look-and-feel measures the string (see
 *        {@link #stringWidth}); this is the cheap second layer that spares the
 *        per-paint {@code FontMetrics.stringWidth}/{@code TextLayout} the look-and-feel
 *        issues while laying text out; and</li>
 *    <li>a small fixed array of rasterised <b>image</b> variants ({@link SubEntry},
 *        up to {@link #MAX_SUBS}), one per distinct pixel appearance — colour,
 *        antialiasing, LCD contrast, fractional metrics, HiDPI scale — matched inside
 *        the entry by {@link #visualHash}.</li>
 *  </ul>
 *  So the cheap width lookup and the expensive glyph rasterisation share one entry;
 *  re-colouring a label reuses its width and merely adds an image variant.
 *
 *  <h2>Lifecycle: weak references do the real work, the budget is a backstop</h2>
 *  Each coarse key is a {@link Pooled}-interned {@link TextKey}; equal keys collapse to
 *  one canonical instance. The canonical key is kept alive only by a {@link KeyHolder}
 *  the component keeps in its {@link ComponentExtension} extra-state store (which is
 *  itself a client property of the component, so it stays invisible from outside). While
 *  a component keeps drawing the same text in the same font it retains that key, keeping
 *  the entry alive; the moment the text or font changes it retains a different key and the
 *  stale entry becomes weakly reachable and is collected (a colour or other pixel-only
 *  change keeps the same key and only adds a variant inside the entry). When the component
 *  itself is collected, its {@link ComponentExtension} — and therefore its {@link KeyHolder}
 *  and the keys it held — go with it. Two components drawing identical text in the same font
 *  share one entry until the last of them stops. So the cache tracks live appearance exactly,
 *  with no invalidation to wire.
 *
 *  <p>On top of that self-clearing lifecycle sits a coarse memory-budget cap, drawn from
 *  the shared {@link CacheBudget} (derived from the configured cache mode and system RAM):
 *  it bounds the number of cached {@code text + font} entries ({@link #maxEntries()}) and the
 *  device-pixel size of any single cached image ({@link #maxImageArea()}). Because each entry may
 *  hold up to {@link #MAX_SUBS} rendered variants, peak image memory can in principle be a small
 *  multiple of the entry count (in practice a string appears in one or two colours). This never
 *  affects correctness — anything not cached is simply drawn directly — it only caps worst-case
 *  memory on machines with little of it, and lets a constrained device (or the {@code DISABLED}
 *  cache mode) opt out of text caching entirely (budget 0).
 *
 *  <h2>Conservative by construction</h2>
 *  Only plain {@code drawString} with a solid {@link Color} paint and an
 *  axis-aligned transform is cached; everything else is forwarded so the result
 *  is always pixel-faithful. The buffer is rendered with forced grayscale
 *  antialiasing (subpixel/LCD AA assumes a known opaque backdrop, which a
 *  transparent buffer composited over an arbitrary background does not have).
 *
 *  <p>EDT-confined: any paint arriving off the Event Dispatch Thread (printing,
 *  image export) bypasses the cache, so the plain {@link WeakHashMap} is never
 *  touched concurrently.
 */
final class TextRenderCache {

    private TextRenderCache() {}

    /** Paint number at which an appearance is promoted from direct drawing to a cached image. */
    private static final int MATERIALISE_AT = 3;

    /** How many distinct rendered variants (colour/AA/LCD/fractional-metrics/scale) a single
     *  {@code text + font} {@link Entry} keeps side by side; once full, a newcomer replaces the
     *  least-warmed <em>un-materialised</em> variant, and only when all slots hold rasterised
     *  images does it rotate the oldest image out (see {@link Entry#addSub}). A real UI shows a
     *  given string in only a handful of appearances (normal, selected, disabled, …), so a tiny
     *  fixed array beats a per-entry map on both footprint and lookup cost, while eviction keeps
     *  a genuinely shifted appearance from going stale. */
    private static final int MAX_SUBS = 4;

    /** User-space padding around the glyph box (absorbs left-side bearing, glyph
     *  overhang and antialiasing bleed so nothing is clipped by the image edge). */
    private static final int PAD = 2;

    /** Per unit of the shared {@link CacheBudget#units()} scalar, the device-pixel area a
     *  single cached text image may occupy. Text is thin but can be wide, so a unit buys a
     *  generous strip of pixels. Kept in pixel form (rather than bytes) so that, at the
     *  default mode, <em>which</em> strings qualify for caching is exactly as it always was. */
    private static final long PIXELS_PER_AGGRESSIVENESS = 512L * 256L; // 131072 device px / unit

    /** Absolute device-pixel ceiling for one cached text image, independent of how
     *  aggressively caching is configured (a final sanity guard against pathological text). */
    private static final long MAX_IMAGE_AREA = 2048L * 2048L;

    /** Absolute ceiling on the number of cached text entries, independent of aggressiveness. */
    private static final int MAX_ENTRIES = 4096;

    /** Maximum device-pixel area a single cached text image may occupy, scaled by the
     *  shared {@link CacheBudget#units()} scalar so that memory-constrained machines cache
     *  more conservatively. */
    private static long maxImageArea() {
        return Math.min(MAX_IMAGE_AREA, (long) (CacheBudget.units() * PIXELS_PER_AGGRESSIVENESS));
    }

    /** Maximum number of distinct text appearances the global cache will hold, derived from
     *  this cache's slice of the shared {@link CacheBudget} byte budget. Once this is reached
     *  new appearances are drawn directly (never cached) until the weak-reference lifecycle
     *  frees room again. */
    private static int maxEntries() {
        return Math.min(MAX_ENTRIES, CacheBudget.maxEntriesFor(CacheBudget.Kind.TEXT_IMAGE));
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

        // Rule 0: text caching must be switched on at all (cache mode, toggle, non-zero budget).
        if ( !isTextCachingActive() )
            return false;

        // Rule 1: per-class self-healing opt-out (keyed by component class).
        final Class<?> componentClass = c.getClass();
        if ( UNSAFE.contains(componentClass) )
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
        else if ( c instanceof JPasswordField ) { // a JTextField subtype handled before the generic branch below
            // JPasswordField.getText() is deprecated and would materialise the cleartext
            // password into a lingering String just to answer "is there text?"; the
            // document length answers it without ever copying the password content.
            // (getDocument() is effectively always non-null, but this gate runs outside
            // the self-healing try/catch in ComponentExtension, so we never risk an NPE.)
            javax.swing.text.Document doc = ((JPasswordField) c).getDocument();
            return doc != null && doc.getLength() > 0;
        }
        else if ( c instanceof JTextField )       // incl. JFormattedTextField
            ownText = ((JTextField) c).getText();
        else
            return false;                         // not a recognised leaf text component -> never proxy

        // Rule 3: only worth wrapping while there is actually text to draw.
        return ownText != null && !ownText.isEmpty();
    }

    /** Permanently disables proxying for a component class after its paint failed once. */
    static void markUnsafe(Class<?> componentClass) { UNSAFE.add(componentClass); }

    /** Whether text caching is switched on at all: library cache mode, the text-caching toggle,
     *  and a non-zero memory budget (a budget of {@code 0} opts out of text caching entirely, so
     *  installing any proxy would be pure overhead — {@link #paintString} could never admit an
     *  entry anyway). Gates both the graphics proxy ({@link #isProxyable(JComponent)}) and the
     *  FontMetrics proxy SwingTree's own text components install via
     *  {@link ComponentExtension#getFontMetricsCacheBacked(FontMetrics)}; when off,
     *  {@code getFontMetrics} returns the real metrics untouched. */
    static boolean isTextCachingActive() {
        if ( SwingTree.get().getCacheMode() == SwingTreeInitConfig.CacheMode.DISABLED )
            return false;
        if ( !SwingTree.get().isTextCachingEnabled() )
            return false;
        return maxEntries() > 0;
    }

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
            if ( e != null && e.hasImage() ) return true;
        }
        return false;
    }

    /** {@code true} if at least one of the component's currently retained {@code text + font}
     *  entries carries a cached string width (served to the look-and-feel through the
     *  {@link CachingTextGraphics2D} FontMetrics proxy instead of being re-measured). The width is
     *  filled once a paint has established the entry, so it appears one paint before
     *  {@link #hasMaterialisedText} (whose image needs a longer warm-up). */
    static boolean hasCachedWidth(JComponent c) {
        KeyHolder h = holderOf(c);
        if ( h == null ) return false;
        for ( Pooled<TextKey> key : h._keys ) {
            Entry e = CACHE.get(key);
            if ( e != null && e.width >= 0 ) return true;
        }
        return false;
    }

    /** The number of distinct pixel renderings (colour / AA / LCD / fractional-metrics / scale
     *  variants) tracked across the component's currently retained {@code text + font} entries.
     *  Two components showing the same string in the same font share <em>one</em> entry (so
     *  {@link #globalEntryCount} counts it once), yet each visual variant is rendered separately
     *  inside it — which is what guarantees a red and a blue label never blit each other's pixels. */
    static int renderedVariantCount(JComponent c) {
        KeyHolder h = holderOf(c);
        if ( h == null ) return 0;
        int n = 0;
        for ( Pooled<TextKey> key : h._keys ) {
            Entry e = CACHE.get(key);
            if ( e != null ) n += e.subCount;
        }
        return n;
    }

    /** Total number of live entries in the global text cache. Stale, garbage-collected
     *  keys are expunged by {@link WeakHashMap#size()} before counting. No bookkeeping
     *  poke is needed: a component's {@link KeyHolder} lives in its
     *  {@link ComponentExtension}, so it is collected together with the component, which
     *  releases its keys and lets their cache entries become reclaimable directly. */
    static int globalEntryCount() {
        return CACHE.size();
    }

    /** Drops every cached entry. Used both by tests needing a clean, deterministic starting
     *  point and by {@link ComponentExtension#updateAllCachesFromLibraryConfig()} when the
     *  cache configuration changes. (Per-component {@link KeyHolder}s live in each component's
     *  {@link ComponentExtension} and simply repopulate on the next paint.) */
    static void clearGlobalCache() {
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

        // Coarse key: text + font only. The width path reconstructs this identically from the
        // component's metrics, so both paths meet at one Entry; the pixel-specific variants
        // (colour/AA/LCD/fractional-metrics/scale) are distinguished inside it by SubEntry.
        Pooled<TextKey> key = coarseKey(font, text).intern();

        Entry entry = CACHE.get(key);
        if ( entry == null ) {
            if ( CACHE.size() >= maxEntries() )
                return false;   // global cache is at its memory-budget cap -> draw directly, don't grow
            entry = new Entry();
            CACHE.put(key, entry);
        }
        holder.retain(key);     // an existing appearance is always retained; a new one only once admitted

        long visualHash = visualHash(delegate, tx, font, color);   // this exact rendering's pixel signature
        SubEntry sub = entry.findSub(visualHash);

        /*
            Fast path — this exact rendering is already materialised, so just blit it.
            We deliberately keep FontMetrics OUT of this path: FontMetrics.stringWidth(text)
            builds a TextLayout to measure the glyph advances and, per real benchmarks, costs
            several times more than the blit itself. We don't need it here: the cached image
            already encodes the text's width and height, and to position the blit we only need
            the ascent (a font-level constant, so it is the same on every paint of this rendering
            because the font is part of the cache key) which we captured when materialising.
        */
        if ( sub != null && sub.image != null ) {
            int boundsX = (int) Math.floor(x);
            int boundsY = (int) Math.floor(y - sub.ascent);
            blit(delegate, tx, sub.image, boundsX - PAD, boundsY - PAD);
            return true;
        }

        if ( sub == null )
            sub = entry.addSub(visualHash);   // always admits; evicts an un-warmed variant first, a materialised image only as a last resort

        // Warm-up — draw directly (no measuring) until this rendering proves stable.
        if ( ++sub.hits < MATERIALISE_AT ) return false;

        // Materialise — runs once per rendering; this is the only place the image path measures the string.
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
        sub.image  = img;
        sub.ascent = ascent;                                        // captured for the fast path above

        blit(delegate, tx, img, boundsX - PAD, boundsY - PAD);
        return true;
    }

    /**
     *  Attempts to satisfy {@code fontMetrics.stringWidth(text)} from the cache — the other
     *  major per-paint text cost besides the {@code drawString} itself. A look-and-feel
     *  measures a string (to lay it out, clip it or centre it) <i>before</i> drawing it, and
     *  {@link FontMetrics#stringWidth(String)} builds a {@link java.awt.font.TextLayout} to do
     *  so, which per benchmarks dominates the paint of a text component once its pixels are cached.
     *
     *  <p><b>This is the second cache layer, and it deliberately never creates an {@link Entry}.</b>
     *  Width depends only on the text and font (plus the component's stable fractional-metrics
     *  setting), <em>not</em> on colour or the pixel-only hints — so it is keyed by the same coarse
     *  {@code text + font} {@link #coarseKey key} the image path uses. We probe for the entry a paint
     *  has already established; if none exists yet, we just measure directly (the very first paint of
     *  a string has not created its entry at layout time). Crucially the width itself must be measured
     *  in <em>this</em> (component) context, not taken from the paint cycle: the image path measures
     *  with the graphics' metrics (typically fractional-metrics ON), whereas the look-and-feel lays
     *  out with the component's metrics (fractional-metrics OFF), and the two give different advances.
     *  So on the first miss we measure once via {@code realFm} and cache <em>that</em> value on the
     *  shared entry; every later paint of the same string is then a field read. No entry is created,
     *  nothing is retained (the image path already keeps the entry alive), and a miss is always a
     *  correct direct measure — so this can only accelerate, never diverge.
     *
     *  <p>The probe key is <em>not</em> interned: {@link Pooled#equals} compares by value, so it finds
     *  the interned entry without touching the object pool.
     *
     * @return the string's advance width, served from the cached measurement once established, and
     *         otherwise measured directly via {@code realFm}.
     */
    static int stringWidth(FontMetrics realFm, @Nullable String text) {
        if ( text == null || text.isEmpty() )          return 0;                        // nothing to measure
        if ( !SwingUtilities.isEventDispatchThread() ) return realFm.stringWidth(text); // never touch the map off-EDT

        Entry entry = CACHE.get(coarseKey(realFm.getFont(), text)); // probe (no intern needed for a read)
        if ( entry == null )       return realFm.stringWidth(text); // no paint has established this text+font yet
        if ( entry.width >= 0 )    return entry.width;              // already measured in this (component) context
        int w = realFm.stringWidth(text);                          // first miss: measure once...
        entry.width = w;                                           // ...and cache it on the shared entry (no new entry)
        return w;
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

    /** The coarse cache key shared by the image path ({@link #paintString}) and the width path
     *  ({@link #stringWidth}): exactly the text and its font, and nothing pixel-specific. Both paths
     *  reconstruct it identically — the image path from the graphics font, the width path from the
     *  metrics' font — so they meet at one {@link Entry}; the per-rendering pixel variants live in
     *  that entry's {@link SubEntry} array, keyed by {@link #visualHash}. Returned un-interned; the
     *  image path interns it before caching, the width path uses it as a by-value probe. */
    private static Pooled<TextKey> coarseKey(Font font, String text) {
        return new Pooled<>(new TextKey(text, font));
    }

    /** All pixel-affecting inputs of a {@code drawString} call folded into a well-mixed 64-bit value.
     *  Computed and compared only on the paint side (to pick a {@link SubEntry}), so it may draw on
     *  anything the graphics exposes; the font is redundant here (the {@link #coarseKey} already pins
     *  it) but is left in for defence in depth. */
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

    /** Mutable per-{@code text+font} slot. EDT-confined, so no synchronisation. Holds the one
     *  layout {@link #width} (colour/AA/LCD/scale-independent, filled by the width path) and a small
     *  fixed array of pixel {@link SubEntry renderings} (filled by the image path). */
    private static final class Entry {
        int            width = -1;                        // cached component-context stringWidth; -1 until measured
        final SubEntry[] subs = new SubEntry[MAX_SUBS];   // rendered variants; linear-scanned (MAX_SUBS is tiny)
        int            subCount;                          // slots in use (grows to MAX_SUBS, then stays)
        int            evict;                             // ring-buffer cursor: index of the oldest variant

        /** The rendering matching this pixel signature, or {@code null} if not present yet. */
        @Nullable SubEntry findSub(long visualHash) {
            for ( int i = 0; i < subCount; i++ )
                if ( subs[i].visualHash == visualHash ) return subs[i];
            return null;
        }
        /** Admits a new rendering. While there is room it simply appends. Once {@link #MAX_SUBS}
         *  are held, it overwrites the least-warmed slot that has <em>not</em> been materialised
         *  yet, so a burst of one-off transient appearances (hover shades, a drag highlight, an
         *  animated colour) only churns among itself and can never destroy an already rasterised
         *  image — without this preference, five recurring appearances would evict each other on
         *  every paint and nothing would ever warm up. Only when every slot holds a materialised
         *  image (a genuine appearance shift, e.g. a theme change) does the ring cursor rotate
         *  the oldest image out (it is dropped and so falls out of the image budget). */
        SubEntry addSub(long visualHash) {
            SubEntry s = new SubEntry(visualHash);
            if ( subCount < MAX_SUBS ) {
                subs[subCount++] = s;
                return s;
            }
            int victim = -1;
            for ( int i = 0; i < MAX_SUBS; i++ )          // prefer the least-warmed, un-materialised slot
                if ( subs[i].image == null && (victim < 0 || subs[i].hits < subs[victim].hits) )
                    victim = i;
            if ( victim < 0 ) {                           // all slots materialised -> rotate the oldest out
                victim = evict;
                evict = (evict + 1) % MAX_SUBS;
            }
            subs[victim] = s;
            return s;
        }
        /** Whether any of this text+font's renderings has been promoted to a blittable image. */
        boolean hasImage() {
            for ( int i = 0; i < subCount; i++ )
                if ( subs[i].image != null ) return true;
            return false;
        }
    }

    /** One rendered variant of a {@link Entry}'s text+font: a pixel signature plus, once warmed up,
     *  the rasterised image and the ascent that positions its blit. */
    private static final class SubEntry {
        final long                  visualHash; // colour + AA + LCD + fractional-metrics + transform scale
        int                         hits;       // warm-up counter; promoted to an image at MATERIALISE_AT
        int                         ascent;     // font ascent captured at materialise time; positions the blit
        @Nullable BufferedImage     image;
        SubEntry(long visualHash) { this.visualHash = visualHash; }
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

    /** The coarse value-object cache key: the exact string and its {@link Font}, and nothing
     *  pixel-specific. Both are compared by value ({@code String.equals}/{@code Font.equals}), so a
     *  wrong hit would require two equal strings in two equal fonts — i.e. it cannot happen. Pixel
     *  differences (colour, AA, LCD, fractional metrics, scale) are resolved <em>within</em> the
     *  matched {@link Entry} by its {@link SubEntry} array, not by the key. */
    static final class TextKey {
        private final String text;
        private final Font   font;
        private final int    hash;

        TextKey(String text, Font font) {
            this.text = text;
            this.font = font;
            this.hash = text.hashCode() * 31 + font.hashCode();
        }

        @Override public boolean equals(@Nullable Object o) {
            if ( this == o ) return true;
            if ( !(o instanceof TextKey) ) return false;
            TextKey other = (TextKey) o;
            return this.hash == other.hash
                && this.text.equals(other.text)
                && this.font.equals(other.font);
        }
        @Override public int hashCode() { return hash; }
    }
}
