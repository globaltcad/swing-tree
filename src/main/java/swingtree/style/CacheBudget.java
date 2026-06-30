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
 *  <pre>{@code
 *      ramShape         = ln(max(1, systemRAM_GiB - 1))      // RAM scaling, mode-independent
 *      units            = modeMultiplier(mode) × ramShape    // == the historical "aggressiveness"
 *      totalBudgetBytes = units × BYTES_PER_UNIT
 *      bytesFor(kind)   = totalBudgetBytes × weight(kind)
 *  }</pre>
 *  The mode multipliers are {@code DISABLED=0, CONSERVATIVE=2, BALANCED=4, GENEROUS=8,
 *  AGGRESSIVE=16}. {@code BALANCED} uses the same multiplier ({@code 4}) the library
 *  always used, so the default reproduces SwingTree's historical, RAM-scaled behaviour.
 *
 *  <h2>Two complementary scalars</h2>
 *  <ul>
 *      <li>{@link #units()} — the raw RAM×mode scalar. Caches keep using it for the
 *          per-image <em>admission</em> decisions that are naturally expressed in
 *          pixels (e.g. "a single cached image may not exceed N device pixels"), so
 *          <em>which</em> components qualify for caching is unchanged at {@code BALANCED}.</li>
 *      <li>{@link #bytesFor(Kind)} — a cache's slice of the total byte budget, used for
 *          the <em>retention</em> caps (how many entries / how much total memory). This
 *          is what makes the overall footprint tangible.</li>
 *  </ul>
 *
 *  <h2>Lifecycle</h2>
 *  The current mode is resolved <em>lazily</em> from {@link SwingTree#getCacheMode()} on
 *  the first read after a {@link #markUnresolved() dirty} mark and then cached in a
 *  {@code volatile}, so the hot painting path never repeatedly consults the library
 *  singleton. {@link ComponentExtension#updateAllCachesFromLibraryConfig()} marks it
 *  dirty (and empties the caches) whenever the library configuration changes, so a
 *  runtime {@link SwingTree#setCacheMode(CacheMode)} shrinks memory immediately and the
 *  new budget takes effect on the next paint.
 */
final class CacheBudget {

    private static final Logger log = LoggerFactory.getLogger(CacheBudget.class);

    private CacheBudget() {}

    /** Bytes of total cache memory granted per unit of aggressiveness. The total budget
     *  is {@code units × BYTES_PER_UNIT}; with {@code BALANCED} this lands in the tens of
     *  MiB on a typical desktop, matching SwingTree's historical footprint order. */
    private static final long BYTES_PER_UNIT = 4L * 1024 * 1024; // 4 MiB / unit

    /** Sentinel for "the mode has not been resolved from the library config yet". */
    private static final double UNRESOLVED = -1.0;

    /** The resolved RAM×mode scalar, or {@link #UNRESOLVED}. Resolved lazily so that
     *  resolution never happens during {@link SwingTree}'s own construction. */
    private static volatile double _units = UNRESOLVED;

    /** Test hook: when {@code >= 0} it overrides the RAM/mode-derived {@link #units()},
     *  giving specs a deterministic budget independent of the CI machine's RAM. A value
     *  of {@code 0} simulates a maximally constrained machine (all caching off). */
    static double UNITS_OVERRIDE = -1;

    /** {@code ln(max(1, systemRAM_GiB - 1))} — the RAM-derived, mode-independent part of
     *  the budget. Computed once; the expensive RAM query is never repeated. */
    private static final double _RAM_SHAPE;
    static {
        _RAM_SHAPE = Math.log(Math.max(1, _detectSystemRamGiB() - 1));
    }

    /** The cache kinds the total byte budget is partitioned across, each with its weight
     *  (the fractions sum to 1) and a representative per-entry byte cost used to translate
     *  the kind's byte slice into a native entry/tile count. The per-entry costs are real
     *  estimates (a 256² tile really is ~256 KiB), which is what makes the budget tangible. */
    enum Kind {
        STYLE_LAYER    (0.40, 64L  * 1024),        // representative style layer image (~128² ARGB)
        TEXT_IMAGE     (0.20, 16L  * 1024),        // representative rasterised glyph strip
        NOISE_TILE     (0.25, 256L * 256 * 4),     // exact: one 256² ARGB noise tile (256 KiB)
        SHADOW_GRADIENT(0.05, 1L   * 1024),        // a blended gradient-stop array
        TEXT_LAYOUT    (0.10, 2L   * 1024);        // a cached paragraph layout

        final double weight;
        final long   bytesPerEntry;
        Kind( double weight, long bytesPerEntry ) {
            this.weight        = weight;
            this.bytesPerEntry = bytesPerEntry;
        }
    }

    /** The RAM×mode scalar, honouring the test override. Replaces the former
     *  {@code LayerCache.DYNAMIC_CACHE_AGGRESSIVENESS()}. */
    static double units() {
        return UNITS_OVERRIDE >= 0 ? UNITS_OVERRIDE : _resolvedUnits();
    }

    /** The total memory, in bytes, all caches together may spend right now. */
    static long totalBudgetBytes() {
        return (long) (units() * BYTES_PER_UNIT);
    }

    /** The slice of the total byte budget allotted to the given cache kind. */
    static long bytesFor( Kind kind ) {
        return (long) (totalBudgetBytes() * kind.weight);
    }

    /** The number of native entries (images / tiles / arrays) the kind's byte slice buys,
     *  using its representative per-entry cost. {@code 0} when caching is off. */
    static int maxEntriesFor( Kind kind ) {
        return (int) Math.max(0, bytesFor(kind) / kind.bytesPerEntry);
    }

    /** {@code true} once caching is effectively turned off (a zero budget). */
    static boolean isCachingDisabled() {
        return units() <= 0;
    }

    private static double _resolvedUnits() {
        double u = _units;
        if ( u < 0 ) {                       // UNRESOLVED -> resolve now (safe: never during construction)
            u = _unitsFor(_currentMode());
            _units = u;
        }
        return u;
    }

    /** Forces the next {@link #units()} read to re-resolve the mode from the live library
     *  configuration. Called whenever the configuration may have changed. */
    static void markUnresolved() {
        _units = UNRESOLVED;
    }

    private static CacheMode _currentMode() {
        try {
            return SwingTree.get().getCacheMode();
        } catch ( Throwable t ) {
            log.debug("Could not resolve the cache mode from the SwingTree context; assuming BALANCED.", t);
            return CacheMode.BALANCED;
        }
    }

    private static double _unitsFor( CacheMode mode ) {
        return _multiplierOf(mode) * _RAM_SHAPE;
    }

    private static int _multiplierOf( CacheMode mode ) {
        switch ( mode ) {
            case DISABLED:     return 0;
            case CONSERVATIVE: return 2;
            case BALANCED:     return 4;
            case GENEROUS:     return 8;
            case AGGRESSIVE:   return 16;
            default:           return 4;
        }
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
