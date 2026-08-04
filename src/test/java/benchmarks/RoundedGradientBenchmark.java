package benchmarks;

import swingtree.SwingTree;
import swingtree.SwingTreeInitConfig;
import swingtree.UI;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Toolkit;
import java.awt.image.VolatileImage;

/**
 *  Measures what a <b>rounded</b> component carrying a gradient costs to paint, which is the
 *  one common style shape that cannot take any of the shortcuts the style engine already has.
 *  <p>
 *  A gradient makes its layer ineligible for size independent caching (its pixels span the
 *  component bounds), so every frame of a resize rasterizes it again. A rounded fill area then
 *  makes that rasterization antialiased, and an antialiased fill is rasterized in software
 *  whatever it is drawn onto - on X11 {@code XRSurfaceData.validatePipe} only offers a
 *  {@link java.awt.Paint} to the X server inside an {@code antialiasHint != ANTIALIAS_ON}
 *  branch. A flat rounded fill escapes this by being cacheable, and a rectangular gradient
 *  escapes it because {@code StyleRenderer._fillShape} may switch antialiasing off for a
 *  whole-pixel rectangle. Rounded plus gradient escapes neither.
 *  <p>
 *  Two sweeps, for the two directions the style cache pulls in:
 *  <ul>
 *      <li><b>at an unchanged size</b> - caches warm, which is what nearly every paint of a
 *          real UI looks like, and the number any change here must not regress;</li>
 *      <li><b>while resizing</b> - every sweep at a width no earlier sweep used, so the layer
 *          cache misses and the fill is genuinely rasterized.</li>
 *  </ul>
 *  <b>{@code -Dbenchmark.cache=disabled} is the interesting one.</b> With caching on, a
 *  component small enough to be admitted to the layer cache is rasterized into a
 *  {@code BufferedImage}, where the antialiasing hint hardly changes which loop runs;
 *  disabling the cache sends every paint straight onto the window's accelerated surface,
 *  which is where the hint decides between the X server and software rasterization. Both are
 *  worth measuring - the first is the common case, the second is where the cost lives.
 *  <p>
 *  <b>One case per process, on purpose:</b> two windows would compete for the one global layer
 *  cache. Compare whole runs, several of them alternating, because the absolute figure on a
 *  desktop machine drifts by half between one minute and the next.
 *  <p>
 *  Run with {@code ./gradlew runRoundedGradientBenchmark}. It needs a display.
 */
public final class RoundedGradientBenchmark
{
    private static final int WIDE_WIDTH   = Integer.getInteger("benchmark.width",  1600);
    private static final int HEIGHT       = Integer.getInteger("benchmark.height", 1000);
    private static final int NARROW_WIDTH = Math.max(40, WIDE_WIDTH * 11 / 16);
    private static final int ARC          = Integer.getInteger("benchmark.arc",    32);

    /** How often the volatile surface had to be restored or replaced, reported with the
     *  results: a run which lost its surface repeatedly was not measuring one steady pipeline
     *  and its numbers should not be compared with another run's. Only ever touched on the
     *  event dispatch thread, which is where every paint happens. */
    private static int surfaceLosses = 0;

    private static final int WARMUP_SWEEPS = Integer.getInteger("benchmark.warmup", 30);
    private static final int TIMED_SWEEPS  = Integer.getInteger("benchmark.sweeps", 80);

    public static void main( String[] args ) throws Exception
    {
        final boolean cacheDisabled = "disabled".equalsIgnoreCase(System.getProperty("benchmark.cache", "default"));

        /*
            Checked before anything is shown: past the number of distinct widths a resize sweep
            would repaint one already painted and measure the cache hit path instead. Throwing
            after the frame is up would hang the JVM on the event dispatch thread.
        */
        // Inclusive at both ends: the sweeps use NARROW_WIDTH + 0 up to NARROW_WIDTH + (n - 1),
        // so n widths fit exactly when the last of them is still WIDE_WIDTH.
        final int distinctWidths = WIDE_WIDTH - NARROW_WIDTH + 1;
        if ( WARMUP_SWEEPS + TIMED_SWEEPS > distinctWidths )
            throw new IllegalArgumentException(
                "This benchmark needs one unused width per resize sweep, but " + (WARMUP_SWEEPS + TIMED_SWEEPS)
                + " sweeps were asked of a " + distinctWidths + " wide range. "
                + "Raise -Dbenchmark.width, or lower -Dbenchmark.warmup / -Dbenchmark.sweeps."
            );

        SwingTree.initializeUsing( it -> it.uiScaleFactor(1) );
        if ( cacheDisabled )
            SwingTree.get().setCacheMode(SwingTreeInitConfig.CacheMode.DISABLED);

        JFrame[] frameBox = new JFrame[1];
        UI.runNow(() -> {
            JFrame frame = new JFrame("rounded gradient benchmark");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(
                UI.panel("fill")
                .withStyle( it -> it
                    .backgroundColor(new Color(28, 34, 31))
                    .borderRadius(ARC)
                    .gradient("sheen", g -> g
                        .type(UI.GradientType.RADIAL)
                        .boundary(UI.ComponentBoundary.OUTER_TO_EXTERIOR)
                        .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                        .size(Math.max(WIDE_WIDTH, HEIGHT) * 0.85f)
                        .colors(new Color(120, 180, 140), new Color(30, 40, 60))
                        .clipTo(UI.ComponentArea.BODY)
                    )
                )
                .get(JPanel.class)
            );
            frame.setSize(WIDE_WIDTH, HEIGHT);
            frame.setVisible(true);
            frameBox[0] = frame;
        });
        JFrame frame = frameBox[0];
        // Give the toolkit a moment to actually map the window, or the first frames
        // would be measuring the window manager instead of us:
        Thread.sleep(1200);

        JComponent root = (JComponent) frame.getContentPane();
        /*
            Held in a one element array because a VolatileImage may have to be *replaced* mid
            run: its surface lives in video memory and the windowing system is free to take it
            away (a display mode change, a screen lock, memory pressure), after which it comes
            back either restored-but-blank or altogether incompatible. See paintOnce for the
            protocol, and why this benchmark in particular cannot skip it.
        */
        VolatileImage[] buffer = {
                frame.getGraphicsConfiguration().createCompatibleVolatileImage(WIDE_WIDTH, HEIGHT)
            };

        // The unchanged-size sweep goes first: the resize sweep deliberately misses the layer
        // cache on every one of its widths, which leaves that cache full of debris the
        // unchanged-size sweep would then have to compete with.
        double unchangedSize = measureSweep(frame, root, buffer, false);
        double whileResizing = measureSweep(frame, root, buffer, true);

        buffer[0].flush();

        System.out.println();
        System.out.println("  Rounded gradient benchmark, arc " + ARC + ", "
                         + NARROW_WIDTH + ".." + WIDE_WIDTH + "x" + HEIGHT
                         + ", cache " + ( cacheDisabled ? "DISABLED (straight onto the surface)" : "default" )
                         + ", median of " + TIMED_SWEEPS + " sweeps");
        System.out.printf ("  repaint while resizing       %8.3f ms%n", whileResizing);
        System.out.printf ("  repaint at an unchanged size %8.3f ms%n", unchangedSize);
        if ( surfaceLosses > 0 )
            System.out.println("  NOTE: the volatile surface was restored or replaced "
                             + surfaceLosses + " times during this run.");
        System.out.println();

        System.exit(0);
    }

    private static double measureSweep( JFrame frame, JComponent root, VolatileImage[] buffer, boolean resize )
    throws Exception {
        // The sweep index runs straight through both loops: restarting it at zero for the timed
        // half would replay widths the warmup already painted, i.e. the warm cache path.
        for ( int i = 0; i < WARMUP_SWEEPS; i++ )
            paintOnce(frame, root, buffer, resize, i);

        long[] samples = new long[TIMED_SWEEPS];
        for ( int i = 0; i < TIMED_SWEEPS; i++ )
            samples[i] = paintOnce(frame, root, buffer, resize, WARMUP_SWEEPS + i);

        return medianMillis(samples);
    }

    /**
     *  One timed paint, into a validated volatile buffer, repeated if the surface was lost
     *  underneath it. <br>
     *  <br>
     *  The {@code validate} / {@code contentsLost} protocol is not optional politeness here. A
     *  {@link VolatileImage}'s surface lives in video memory and the windowing system may
     *  reclaim it at any moment; rendering into a surface which has not been validated can
     *  quietly fall back to a software one, and this benchmark exists precisely to tell the
     *  accelerated path from the software path - a silent fallback would not make it noisy, it
     *  would make it wrong, and wrong in exactly the dimension being measured. So the buffer is
     *  validated before the clock starts (never inside it), replaced outright if it came back
     *  incompatible, and any paint whose contents were lost while it ran is timed again rather
     *  than counted.
     */
    private static long paintOnce( JFrame frame, JComponent root, VolatileImage[] buffer, boolean resize, int index )
    throws Exception {
        int width = resize ? NARROW_WIDTH + index : WIDE_WIDTH;
        long[] nanos = new long[1];
        UI.runNow(() -> {
            if ( root.getWidth() != width ) {
                frame.setSize(width, HEIGHT);
                root.invalidate();
                root.validate();
            }
            final GraphicsConfiguration gc = frame.getGraphicsConfiguration();
            do {
                if ( buffer[0].validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE ) {
                    buffer[0].flush();
                    buffer[0] = gc.createCompatibleVolatileImage(WIDE_WIDTH, HEIGHT);
                    buffer[0].validate(gc);
                    surfaceLosses++;
                }
                Graphics2D g = buffer[0].createGraphics();
                try {
                    g.setClip(0, 0, root.getWidth(), root.getHeight());
                    long start = System.nanoTime();
                    root.paint(g);
                    // Sync with the pipeline, or we would be timing how fast we can queue work:
                    Toolkit.getDefaultToolkit().sync();
                    nanos[0] = System.nanoTime() - start;
                } finally {
                    g.dispose();
                }
                if ( buffer[0].contentsLost() )
                    surfaceLosses++;
            }
            while ( buffer[0].contentsLost() );
        });
        return nanos[0];
    }

    /**
     *  The median rather than the mean, because a benchmark sharing a machine with a window
     *  manager and a garbage collector produces the occasional wild sample, and one such
     *  sample would move a mean by more than the changes we measure.
     */
    private static double medianMillis( long[] samples ) {
        long[] sorted = samples.clone();
        java.util.Arrays.sort(sorted);
        int middle = sorted.length / 2;
        double nanos = ( sorted.length % 2 == 0 )
                        ? ( sorted[middle - 1] + sorted[middle] ) / 2.0
                        : sorted[middle];
        return nanos / 1_000_000.0;
    }

    private RoundedGradientBenchmark() {}
}
