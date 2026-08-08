package swingtree.style;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.SwingTree;
import swingtree.SwingTreeInitConfig.CacheMode;

/**
 *  The single source of truth for how much memory <i>all</i> of SwingTree's internal
 *  rendering caches may spend, derived from the configured {@link CacheMode} and the
 *  amount of system RAM. It replaces the former loosely-defined "cache aggressiveness"
 *  integer with a concrete, partitioned <em>byte budget</em>, so each cache can size
 *  itself against a tangible memory figure rather than an opaque multiplier.
 *
 *  <h2>How the budget is computed</h2>
 *  The total budget is modelled the way real systems size their memory pools (database
 *  buffer caches, JVM heap ergonomics): as a small <em>fraction of physical RAM, clamped
 *  between a floor and a cap</em>. The {@link CacheMode} selects that triple:
 *  <pre>{@code
 *      totalBudgetBytes = clamp( systemRAM × fraction(mode), floor(mode), cap(mode) )
 *      bytesFor(kind)   = totalBudgetBytes × weight(kind)
 *  }</pre>
 *  <table border="1" cellpadding="4">
 *    <caption>Per-mode budget parameters</caption>
 *    <tr><th>Mode</th><th>Fraction of RAM</th><th>Floor</th><th>Cap (real-world anchor)</th></tr>
 *    <tr><td>{@code DISABLED}</td>    <td>0%</td>    <td>0&nbsp;MB</td>  <td>0&nbsp;MB &mdash; caching off</td></tr>
 *    <tr><td>{@code CONSERVATIVE}</td><td>0.25%</td> <td>8&nbsp;MB</td>  <td>64&nbsp;MB</td></tr>
 *    <tr><td>{@code BALANCED}</td>    <td>0.5%</td>  <td>16&nbsp;MB</td> <td>128&nbsp;MB &mdash; a light browser tab</td></tr>
 *    <tr><td>{@code GENEROUS}</td>    <td>1%</td>    <td>32&nbsp;MB</td> <td>256&nbsp;MB &mdash; a heavy browser tab</td></tr>
 *    <tr><td>{@code AGGRESSIVE}</td>  <td>2%</td>    <td>64&nbsp;MB</td> <td>512&nbsp;MB &mdash; a heavy Electron app</td></tr>
 *  </table>
 *
 *  <p>This shape is deliberate. A pure {@code log(RAM)} curve (which an earlier draft used)
 *  has two flaws: it collapses to <em>zero</em> on small machines (turning caching silently
 *  off), and it grows so slowly at the top that 32× more RAM buys barely 4× more cache while
 *  the absolute figure means nothing tangible. The <em>floor</em> guarantees a small but
 *  useful cache even on a 1&nbsp;GiB device; the <em>cap</em> &mdash; not a slow transcendental
 *  &mdash; is what delivers diminishing returns, so a 256&nbsp;GiB workstation does not dedicate
 *  gigabytes to a UI cache. The caps are anchored to familiar footprints (an Electron app's
 *  resident set, a browser tab) so the default {@code BALANCED} mode stays a lightweight,
 *  good-citizen alternative to those heavier stacks.
 *
 *  <h2>Reference: total cache budget (MB) by mode and physical RAM</h2>
 *  <pre>
 *    RAM     DISABLED  CONSERVATIVE  BALANCED  GENEROUS  AGGRESSIVE
 *    1 GiB        0           8         16         32         64     (all at floor)
 *    2 GiB        0           8         16         32         64     (all at floor)
 *    4 GiB        0          10         20         41         82
 *    8 GiB        0          20         41         82        164
 *   16 GiB        0          41         82        164        328
 *   32 GiB        0          64        128        256        512     (all at cap)
 *   64 GiB        0          64        128        256        512     (all at cap)
 *  128 GiB        0          64        128        256        512     (all at cap)
 *  256 GiB        0          64        128        256        512     (all at cap)
 *  </pre>
 *
 *  <h2>Reference: where the default ({@code BALANCED}) budget goes, per cache (MB)</h2>
 *  The total is partitioned across the four caches by the {@link Kind} weights below
 *  (style layers 60%, noise tiles 35%, text layouts 3%, shadows 2%):
 *  <pre>
 *    RAM      Total   Layers   Noise   Layouts   Shadows
 *    2 GiB      16      9.6      5.6      0.5       0.3
 *    4 GiB      20     12.3      7.2      0.6       0.4
 *    8 GiB      41     24.6     14.3      1.2       0.8
 *   16 GiB      82     49.2     28.7      2.5       1.6
 *  &gt;=32 GiB    128     76.8     44.8      3.8       2.6   (at cap)
 *  </pre>
 *  These are <em>ceilings on retention</em>, not pre-allocations: a cache only ever holds
 *  what the painted components actually produce, up to its slice. A small app on a big
 *  machine therefore costs little even though its ceiling is high.
 *
 *  <p><b>Why the weights are so lopsided.</b> They express what each cache <em>can</em> spend,
 *  not how important it is. Only the two image caches hold anything big: a style layer image
 *  runs to tens of megabytes, a noise tile is exactly 256 KiB. The other two are bounded by
 *  their own hard entry caps far below any budget &mdash; 128 text layouts is a quarter of a
 *  megabyte, 16 shadow gradient stop arrays is sixteen kilobytes &mdash; so a large slice for
 *  them is a slice that can never be spent. An earlier split gave those two a quarter of the
 *  whole budget, which measured out as 25% of it reserved for caches that between them could
 *  never exceed 0.3 MB.
 *
 *  <h2>Two complementary scalars</h2>
 *  <ul>
 *      <li>{@link #bytesFor(Kind)} &mdash; a cache's slice of the total byte budget. This is
 *          the real bound and what makes the overall footprint tangible; a cache holding
 *          unequally sized things (see {@link Kind#STYLE_LAYER}) measures itself against it
 *          directly.</li>
 *      <li>{@link #maxEntriesFor(Kind)} &mdash; that slice divided by a per-entry cost, for the
 *          caches whose entries are all about the same size and which can therefore be bounded
 *          by counting instead of measuring.</li>
 *  </ul>
 *
 *  <h2>Lifecycle</h2>
 *  The current mode is resolved <em>lazily</em> from {@link SwingTree#getCacheMode()} on the
 *  first read after a {@link #markUnresolved() dirty} mark and then cached in a {@code volatile},
 *  so the hot painting path never repeatedly consults the library singleton.
 *  {@link ComponentExtension#updateAllCachesFromLibraryConfig()} marks it dirty (and empties the
 *  caches) whenever the library configuration changes, so a runtime
 *  {@link SwingTree#setCacheMode(CacheMode)} shrinks memory immediately and the new budget takes
 *  effect on the next paint.
 */
final class CacheBudget {

    private static final Logger log = LoggerFactory.getLogger(CacheBudget.class);

    private CacheBudget() {}

    private static final long MiB = 1024L * 1024;

    /** The scale of the {@link #UNITS_OVERRIDE} test hook. */
    private static final long BYTES_PER_UNIT = 4L * MiB; // 4 MiB / unit

    /** Sentinel for "the budget has not been resolved from the library config yet". */
    private static final long UNRESOLVED = -1L;

    /** The resolved total byte budget, or {@link #UNRESOLVED}. Resolved lazily so that
     *  resolution never happens during {@link SwingTree}'s own construction. */
    private static volatile long _budgetBytes = UNRESOLVED;

    /** Test hook: when {@code >= 0} it overrides the RAM/mode-derived budget, giving specs a
     *  deterministic figure independent of the CI machine's RAM. The value is expressed in
     *  {@code 4 MiB} units (so {@code totalBudgetBytes() == UNITS_OVERRIDE * 4 MiB});
     *  {@code 0} simulates a maximally constrained machine (all caching off). */
    static volatile double UNITS_OVERRIDE = -1;

    /** Total physical RAM in bytes, queried once. The expensive query is never repeated. */
    private static final double _SYSTEM_RAM_BYTES;
    static {
        _SYSTEM_RAM_BYTES = _detectSystemRamGiB() * (double) (1L << 30);
    }

    /** Marks a {@link Kind} whose entries are too unequal in size for an entry count to mean
     *  anything; {@link #maxEntriesFor(Kind)} refuses to answer for such a kind. */
    private static final long NOT_COUNTED_BY_ENTRIES = -1L;

    /** The cache kinds the total byte budget is partitioned across, each with its weight
     *  (the fractions sum to 1) and a per-entry byte cost used to translate the kind's byte
     *  slice into an entry count. <br>
     *  <br>
     *  A per-entry cost is only meaningful for a cache whose entries are all <em>the same
     *  size</em> - a noise tile really is exactly 256 KiB, a gradient stop array really is
     *  about a kilobyte - which is why {@link #STYLE_LAYER} has none. Style layer images range
     *  from a few kilobytes to tens of megabytes, so any single figure for them is fiction and
     *  the entry count derived from it is meaningless: measured across the seven example UIs,
     *  a nominal 921 entry allowance corresponded to over a gigabyte of real images. That cache
     *  measures its own bytes instead, see {@link #bytesFor(Kind)}. */
    enum Kind {
        STYLE_LAYER    (0.60, NOT_COUNTED_BY_ENTRIES), // images from kilobytes to tens of megabytes
        NOISE_TILE     (0.35, 256L * 256 * 4),         // exact: one 256² ARGB noise tile (256 KiB)
        SHADOW_GRADIENT(0.02, 1L   * 1024),            // a blended gradient-stop array
        TEXT_LAYOUT    (0.03, 2L   * 1024);            // a cached paragraph layout

        final double weight;
        final long   bytesPerEntry;
        Kind( double weight, long bytesPerEntry ) {
            this.weight        = weight;
            this.bytesPerEntry = bytesPerEntry;
        }
    }

    /** The total memory, in bytes, all caches together may spend right now. */
    static long totalBudgetBytes() {
        if ( UNITS_OVERRIDE >= 0 )
            return (long) (UNITS_OVERRIDE * BYTES_PER_UNIT);
        long b = _budgetBytes;
        if ( b < 0 ) {                       // UNRESOLVED -> resolve now (safe: never during construction)
            b = _budgetFor(_currentMode());
            _budgetBytes = b;
        }
        return b;
    }

    /** The slice of the total byte budget allotted to the given cache kind. */
    static long bytesFor( Kind kind ) {
        return (long) (totalBudgetBytes() * kind.weight);
    }

    /** The number of native entries (tiles / arrays) the kind's byte slice buys, using its
     *  per-entry cost. {@code 0} when caching is off. Only valid for a kind whose entries are
     *  all about the same size; a kind marked {@link #NOT_COUNTED_BY_ENTRIES} has to measure
     *  its own bytes against {@link #bytesFor(Kind)} instead. */
    static int maxEntriesFor( Kind kind ) {
        if ( kind.bytesPerEntry == NOT_COUNTED_BY_ENTRIES )
            throw new IllegalArgumentException(
                    "The entries of the '" + kind + "' cache are too unequal in size to be counted; " +
                    "bound that cache by bytesFor(" + kind + ") instead."
                );
        return (int) Math.max(0, bytesFor(kind) / kind.bytesPerEntry);
    }

    /** Lazily resolved "is stretch tiling allowed" flag from the library configuration,
     *  cached here (like the byte budget) so the per-paint hot path never touches the
     *  {@link SwingTree} singleton. {@code null} means "not resolved yet". */
    private static volatile @org.jspecify.annotations.Nullable Boolean _tilingEnabled = null;

    /** Whether eligible style renderings may be cached independently of the component
     *  size using stretch tiling (see {@link SwingTree#setCacheTilingEnabled(boolean)}).
     *  Resolved lazily from the library configuration, like the byte budget. */
    static boolean tilingEnabled() {
        Boolean enabled = _tilingEnabled;
        if ( enabled == null ) {
            enabled = _resolveTilingEnabled();
            _tilingEnabled = enabled;
        }
        return enabled;
    }

    private static boolean _resolveTilingEnabled() {
        try {
            return SwingTree.get().isCacheTilingEnabled();
        } catch ( Throwable t ) {
            log.debug("Could not resolve the cache tiling flag from the SwingTree context; assuming enabled.", t);
            return true;
        }
    }

    /** Forces the next budget read to re-resolve the mode from the live library
     *  configuration. Called whenever the configuration may have changed. */
    static void markUnresolved() {
        _budgetBytes = UNRESOLVED;
        _tilingEnabled = null;
    }

    private static CacheMode _currentMode() {
        try {
            return SwingTree.get().getCacheMode();
        } catch ( Throwable t ) {
            log.debug("Could not resolve the cache mode from the SwingTree context; assuming BALANCED.", t);
            return CacheMode.BALANCED;
        }
    }

    /** {@code clamp(systemRAM × fraction(mode), floor(mode), cap(mode))} — see the class
     *  documentation for the per-mode parameters and the resulting reference table. */
    private static long _budgetFor( CacheMode mode ) {
        final double fraction;
        final long   floor;
        final long   cap;
        switch ( mode ) {
            case DISABLED:     return 0L;
            case CONSERVATIVE: fraction = 0.0025; floor =  8 * MiB; cap =  64 * MiB; break;
            case BALANCED:     fraction = 0.0050; floor = 16 * MiB; cap = 128 * MiB; break;
            case GENEROUS:     fraction = 0.0100; floor = 32 * MiB; cap = 256 * MiB; break;
            case AGGRESSIVE:   fraction = 0.0200; floor = 64 * MiB; cap = 512 * MiB; break;
            default:           fraction = 0.0050; floor = 16 * MiB; cap = 128 * MiB; break;
        }
        long raw = (long) (_SYSTEM_RAM_BYTES * fraction);
        return Math.min(cap, Math.max(floor, raw));
    }

    private static double _detectSystemRamGiB() {
        try {
            java.lang.management.OperatingSystemMXBean os = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if ( os instanceof com.sun.management.OperatingSystemMXBean ) {
                long totalBytes = ((com.sun.management.OperatingSystemMXBean) os).getTotalPhysicalMemorySize();
                if ( totalBytes > 0 )
                    return totalBytes / (double) (1L << 30);
            }
        } catch ( Throwable t ) {
            log.debug("Could not query system RAM, falling back to JVM max heap.", t);
        }
        // Fallback: JVM max heap (usually a fraction of physical RAM)
        double maxHeap = Runtime.getRuntime().maxMemory() / (double) (1L << 30);
        // We multiply by 4, since the system will have a lot more RAM:
        return maxHeap * 4;
    }
}
