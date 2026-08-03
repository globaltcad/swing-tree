package benchmarks;

import swingtree.SwingTree;
import swingtree.UI;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.VolatileImage;

/**
 *  Measures what a component styled with a large noise gradient costs to paint, in the two
 *  situations that pull the style cache in opposite directions:
 *  <ul>
 *      <li><b>while resizing</b> - every sweep uses a width no earlier sweep used, so the
 *          layer cache misses and the noise is actually rasterized;</li>
 *      <li><b>at an unchanged size</b> - the caches are warm, which is what the overwhelming
 *          majority of a UI's paints look like.</li>
 *  </ul>
 *  Both numbers are needed to judge {@code StyleLayerCache}'s decision to lift a noise out of
 *  its layer: the cut buys the first at the expense of the second.
 *  <p>
 *  Opaque colours (the default) let {@code NoiseGradientPaint} declare itself
 *  {@link java.awt.Transparency#OPAQUE} and the pre-rendered tiles be allocated without an
 *  alpha channel, which picks a specialised blit loop; {@code -Dbenchmark.noise=translucent}
 *  measures the same style with one colour carrying an alpha channel, to check that.
 *  <p>
 *  <b>One case per process, on purpose:</b> two windows would compete for the one global
 *  layer cache, which this deliberately misses on every resize sweep, so they thrash each
 *  other. Compare runs instead, several of them alternating, because the absolute figure on
 *  a desktop machine drifts by half between one minute and the next.
 *  <p>
 *  Run with {@code ./gradlew runNoiseBenchmark}. It needs a display.
 */
public final class NoisePaintBenchmark
{
    /* Settable, because how big the component is decides whether cutting a layer around its
       noise pays at all - see StyleLayerCache. */
    private static final int WIDE_WIDTH   = Integer.getInteger("benchmark.width",  1600);
    private static final int HEIGHT       = Integer.getInteger("benchmark.height", 1000);
    private static final int NARROW_WIDTH = Math.max(40, WIDE_WIDTH * 11 / 16);

    private static final int WARMUP_SWEEPS = Integer.getInteger("benchmark.warmup", 30);
    private static final int TIMED_SWEEPS  = Integer.getInteger("benchmark.sweeps", 80);

    /** Fully opaque, so every pixel the gradient produces is opaque. */
    private static final Color[] OPAQUE_COLORS = {
            new Color(38, 54, 47), new Color(96, 124, 96)
    };
    /** The same two colours, except that one carries an alpha channel. Nothing else differs. */
    private static final Color[] TRANSLUCENT_COLORS = {
            new Color(38, 54, 47), new Color(96, 124, 96, 254)
    };

    public static void main( String[] args ) throws Exception
    {
        String  which  = System.getProperty("benchmark.noise", "opaque");
        boolean opaque = !"translucent".equalsIgnoreCase(which);
        Color[] colors = opaque ? OPAQUE_COLORS : TRANSLUCENT_COLORS;

        /*
            Checked before anything is shown: past the number of distinct widths a resize
            sweep would repaint one already painted and measure the cache hit path instead.
            Throwing after the frame is up would hang the JVM on the event dispatch thread
            instead of reporting this.
        */
        final int distinctWidths = WIDE_WIDTH - NARROW_WIDTH;
        if ( WARMUP_SWEEPS + TIMED_SWEEPS > distinctWidths )
            throw new IllegalArgumentException(
                "This benchmark needs one unused width per resize sweep, but " + (WARMUP_SWEEPS + TIMED_SWEEPS)
                + " sweeps were asked of a " + distinctWidths + " wide range. "
                + "Raise -Dbenchmark.width, or lower -Dbenchmark.warmup / -Dbenchmark.sweeps."
            );

        SwingTree.initializeUsing( it -> it.uiScaleFactor(1) );

        JFrame[] frameBox = new JFrame[1];
        UI.runNow(() -> {
            JFrame frame = new JFrame("noise benchmark");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(
                UI.panel("fill")
                .withStyle( it -> it
                    .backgroundColor(new Color(28, 34, 31))
                    .borderRadius(16)
                    .noise("grain", n -> n
                        .function(UI.NoiseType.FABRIC)
                        .colors(colors)
                        .scale(0.6)
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

        JComponent    root   = (JComponent) frame.getContentPane();
        VolatileImage buffer = frame.getGraphicsConfiguration()
                                    .createCompatibleVolatileImage(WIDE_WIDTH, HEIGHT);

        // The unchanged-size sweep goes first: the resize sweep deliberately misses the layer
        // cache on every one of its widths, which leaves that cache full of debris the
        // unchanged-size sweep would then have to compete with.
        double unchangedSize = measureSweep(frame, root, buffer, false);
        double whileResizing = measureSweep(frame, root, buffer, true);

        buffer.flush();

        System.out.println();
        System.out.println("  Noise benchmark, " + (opaque ? "OPAQUE" : "TRANSLUCENT") + " colours, "
                         + NARROW_WIDTH + ".." + WIDE_WIDTH + "x" + HEIGHT
                         + ", median of " + TIMED_SWEEPS + " sweeps");
        System.out.printf ("  repaint while resizing       %8.3f ms%n", whileResizing);
        System.out.printf ("  repaint at an unchanged size %8.3f ms%n", unchangedSize);
        System.out.println();

        System.exit(0);
    }

    private static double measureSweep( JFrame frame, JComponent root, VolatileImage buffer, boolean resize )
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
     *  One paint, either at a width no earlier sweep used - the way dragging a window edge
     *  behaves, and the only way to make a noise layer miss its cache - or at the one fixed
     *  width, where the caches stay warm.
     */
    private static long paintOnce( JFrame frame, JComponent root, VolatileImage buffer, boolean resize, int index )
    throws Exception {
        int width = resize ? NARROW_WIDTH + index : WIDE_WIDTH;
        long[] nanos = new long[1];
        UI.runNow(() -> {
            if ( root.getWidth() != width ) {
                frame.setSize(width, HEIGHT);
                root.invalidate();
                root.validate();
            }
            Graphics2D g = buffer.createGraphics();
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

    private NoisePaintBenchmark() {}
}
