package benchmarks;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import examples.chat.mvi.ChatView;
import examples.chat.mvi.ChatViewModel;
import sprouts.Var;
import swingtree.SwingTree;
import swingtree.UI;

import javax.swing.JComponent;
import javax.swing.JFrame;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.image.VolatileImage;

/**
 *  Measures what a live window resize of the {@code ChatView} example costs, split into
 *  the two things a resize actually consists of: laying the component tree out again, and
 *  painting it again.
 *  <p>
 *  It is deliberately <b>not</b> an end to end harness. Dragging a window edge is at the
 *  mercy of the window manager, of coalesced expose events and of the compositor, none of
 *  which are reproducible from one run to the next. So instead the two halves are driven
 *  directly, each in the way the toolkit itself would drive it:
 *  <ul>
 *      <li><b>The layout sweep</b> sets a new width, invalidates the whole tree the way a
 *          reshape does, and then calls {@code validate()} on it. This is the honest cost,
 *          because an already valid tree would validate in nanoseconds.</li>
 *      <li><b>The paint sweep</b> paints the tree into a {@link VolatileImage}, which lives
 *          in the same place a window's back buffer does and goes through the same
 *          accelerated pipeline. Painting into a {@code BufferedImage} instead would route
 *          everything through the software loops and inflate the result by roughly an order
 *          of magnitude.</li>
 *  </ul>
 *  Three paint numbers are reported, because they answer different questions: painting after
 *  a size change (what a drag frame costs, style caches missing), painting at an unchanged
 *  size (what a repaint costs with the caches warm) and painting a narrow clip (what a
 *  cursor blink or a hover costs).
 *  <p>
 *  Run it with {@code ./gradlew runResizeBenchmark}, or as a plain main method. It needs a
 *  display; there is nothing to measure without one.
 */
public final class ChatViewResizeBenchmark
{
    private static final int   UI_SCALE      = 2;
    private static final int   WINDOW_WIDTH  = 2600;
    private static final int   WINDOW_HEIGHT = 1660;
    private static final int   NARROW_WIDTH  = 1200;
    private static final int   WARMUP_SWEEPS = 24;
    private static final int   TIMED_SWEEPS  = 40;
    private static final int   CLIP_HEIGHT   = 300;

    public static void main( String[] args ) throws Exception
    {
        SwingTree.initializeUsing( it -> it.uiScaleFactor(UI_SCALE) );

        Var<ChatViewModel> vm = Var.of(new ChatViewModel());
        if ( vm.get().theme().isDark() ) FlatDarkLaf.setup(); else FlatLightLaf.setup();

        JFrame[] frameBox = new JFrame[1];
        UI.runNow(() -> {
            JFrame frame = new JFrame("ChatView resize benchmark");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new ChatView(vm));
            frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
            frame.setVisible(true);
            frameBox[0] = frame;
        });
        JFrame frame = frameBox[0];
        // Give the toolkit a moment to actually map the window, or the first frames
        // would be measuring the window manager instead of us:
        Thread.sleep(1500);

        JComponent root = (JComponent) frame.getContentPane();

        double layoutMillis          = measureLayoutSweep(root, frame);
        double paintAfterResize      = measurePaintSweep(frame, root, true,  WINDOW_HEIGHT);
        double paintAtUnchangedSize  = measurePaintSweep(frame, root, false, WINDOW_HEIGHT);
        double paintOfNarrowClip     = measurePaintSweep(frame, root, false, CLIP_HEIGHT);

        System.out.println();
        System.out.println("  ChatView resize benchmark, UI scale " + UI_SCALE + ", " + WINDOW_WIDTH + "x" + WINDOW_HEIGHT);
        System.out.println("  median of " + TIMED_SWEEPS + " sweeps after " + WARMUP_SWEEPS + " warmup sweeps");
        System.out.println("  ------------------------------------------------------------");
        System.out.printf ("  full re-layout                  %8.2f ms%n", layoutMillis);
        System.out.printf ("  repaint after a size change     %8.2f ms%n", paintAfterResize);
        System.out.printf ("  repaint at an unchanged size    %8.2f ms%n", paintAtUnchangedSize);
        System.out.printf ("  repaint of a %d px clip        %8.2f ms%n", CLIP_HEIGHT, paintOfNarrowClip);
        System.out.printf ("  => a drag frame                 %8.2f ms%n", layoutMillis + paintAfterResize);
        System.out.println();

        System.exit(0);
    }

    /**
     *  Times a full re-layout of the tree at alternating widths. The tree is invalidated
     *  explicitly because {@code setSize} alone leaves most of it valid, and a valid tree
     *  validates in nanoseconds, which would measure nothing at all.
     */
    private static double measureLayoutSweep( JComponent root, JFrame frame ) throws Exception
    {
        for ( int i = 0; i < WARMUP_SWEEPS; i++ )
            layoutOnce(root, frame, i);

        long[] samples = new long[TIMED_SWEEPS];
        for ( int i = 0; i < TIMED_SWEEPS; i++ )
            samples[i] = layoutOnce(root, frame, i);

        return medianMillis(samples);
    }

    private static long layoutOnce( JComponent root, JFrame frame, int index ) throws Exception
    {
        int width = ( index % 2 == 0 ? NARROW_WIDTH : WINDOW_WIDTH );
        long[] nanos = new long[1];
        UI.runNow(() -> {
            frame.setSize(width, WINDOW_HEIGHT);
            invalidateTree(root);
            long start = System.nanoTime();
            root.validate();
            nanos[0] = System.nanoTime() - start;
        });
        return nanos[0];
    }

    /**
     *  Times painting the tree into a volatile image, optionally changing the size first,
     *  so that the style caches are either cold (a drag frame) or warm (an ordinary repaint).
     */
    private static double measurePaintSweep( JFrame frame, JComponent root, boolean resizeFirst, int clipHeight ) throws Exception
    {
        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        VolatileImage buffer = gc.createCompatibleVolatileImage(WINDOW_WIDTH, WINDOW_HEIGHT);

        for ( int i = 0; i < WARMUP_SWEEPS; i++ )
            paintOnce(frame, root, buffer, resizeFirst, clipHeight, i);

        long[] samples = new long[TIMED_SWEEPS];
        for ( int i = 0; i < TIMED_SWEEPS; i++ )
            samples[i] = paintOnce(frame, root, buffer, resizeFirst, clipHeight, i);

        buffer.flush();
        return medianMillis(samples);
    }

    private static long paintOnce(
        JFrame frame, JComponent root, VolatileImage buffer,
        boolean resizeFirst, int clipHeight, int index
    ) throws Exception {
        int width = ( resizeFirst ? ( index % 2 == 0 ? NARROW_WIDTH : WINDOW_WIDTH ) : WINDOW_WIDTH );
        long[] nanos = new long[1];
        UI.runNow(() -> {
            if ( resizeFirst || root.getWidth() != width ) {
                frame.setSize(width, WINDOW_HEIGHT);
                invalidateTree(root);
                root.validate();
            }
            Graphics2D g = buffer.createGraphics();
            try {
                g.setClip(0, 0, root.getWidth(), clipHeight);
                long start = System.nanoTime();
                root.paint(g);
                // Sync with the pipeline, or we would be timing how fast we can queue work:
                java.awt.Toolkit.getDefaultToolkit().sync();
                nanos[0] = System.nanoTime() - start;
            } finally {
                g.dispose();
            }
        });
        return nanos[0];
    }

    /**
     *  The median rather than the mean, because a benchmark sharing a machine with a
     *  window manager and a garbage collector produces the occasional wild sample, and
     *  one such sample would move a mean of forty by more than the changes we measure.
     */
    private static double medianMillis( long[] samples ) {
        long[] sorted = samples.clone();
        java.util.Arrays.sort(sorted);
        int middle = sorted.length / 2;
        double nanos = ( sorted.length % 2 == 0 )
                            ? ( sorted[middle - 1] + sorted[middle] ) / 2d
                            : sorted[middle];
        return nanos / 1_000_000d;
    }

    private static void invalidateTree( Component component ) {
        component.invalidate();
        if ( component instanceof Container ) {
            for ( Component child : ((Container) component).getComponents() )
                invalidateTree(child);
        }
    }

    private ChatViewResizeBenchmark() {}
}
