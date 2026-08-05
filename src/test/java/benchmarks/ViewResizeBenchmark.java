package benchmarks;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import examples.almanack.mvi.AlmanackView;
import examples.almanack.mvi.AlmanackViewModel;
import examples.chat.mvi.ChatView;
import examples.chat.mvi.ChatViewModel;
import examples.scribe.CelestialScribe;
import examples.scribe.CosmosViewModel;
import examples.sequencer.SequencerView;
import examples.sequencer.SequencerViewModel;
import examples.team.mvi.TeamView;
import examples.trains.mvi.TrainStyle;
import examples.trains.mvi.TrainsView;
import examples.trains.mvi.TrainsViewModel;
import examples.zen.ThemeGardenView;
import sprouts.Var;
import swingtree.SwingTree;
import swingtree.UI;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 *  Measures what a live window resize of one of the example UIs costs, split into the two
 *  things a resize actually consists of: laying the component tree out again, and painting
 *  it again.
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
 *  Which UI is measured is chosen with {@code -Dbenchmark.view=<name>}, where the name is one
 *  of {@code chat}, {@code sequencer}, {@code trains}, {@code team}, {@code scribe},
 *  {@code almanack}, {@code garden} - or {@code all}, or a comma separated list, which
 *  measures several of them back to back and prints one comparison table at the end. That is
 *  the interesting mode when profiling, because a bottleneck that only one styling idiom
 *  triggers is indistinguishable from a universal one until a second UI disagrees.
 *  <p>
 *  Run it with {@code ./gradlew runResizeBenchmark -Dbenchmark.view=..}, or as a plain main
 *  method. It needs a display; there is nothing to measure without one.
 */
public final class ViewResizeBenchmark
{
    /**
     *  Builds one example UI, look and feel included - the look and feel has to be installed
     *  before the view is built, because the view reads colors out of it as it is assembled.
     *  The frame is handed in for the sake of the views that want to drive it (the theme
     *  garden re-packs the window whenever its skin changes).
     */
    private interface ViewFactory {
        JComponent create( JFrame frame );
    }

    /**
     *  The examples that can be measured. Each one is a different <i>styling idiom</i> rather
     *  than merely a different picture: a soft-shadowed neumorphic grid, a stylesheet driven
     *  one, a gradient heavy one, a plain-ish one. Which of them a change helps is the whole
     *  question, so they are all here.
     */
    private enum Example
    {
        CHAT("chat", frame -> {
            Var<ChatViewModel> vm = Var.of(new ChatViewModel());
            if ( vm.get().theme().isDark() ) FlatDarkLaf.setup(); else FlatLightLaf.setup();
            return new ChatView(vm);
        }),
        SEQUENCER("sequencer", frame -> {
            FlatDarkLaf.setup();
            return new SequencerView(Var.of(new SequencerViewModel()));
        }),
        TRAINS("trains", frame -> {
            FlatLightLaf.setup();
            return new TrainsView(Var.of(TrainsViewModel.initial()), new TrainStyle());
        }),
        TEAM("team", frame -> {
            FlatDarkLaf.setup();
            return TeamView.createView();
        }),
        SCRIBE("scribe", frame -> {
            FlatDarkLaf.setup();
            return new CelestialScribe(Var.of(new CosmosViewModel()));
        }),
        ALMANACK("almanack", frame -> {
            FlatLightLaf.setup();
            return new AlmanackView(Var.of(AlmanackViewModel.initial()));
        }),
        GARDEN("garden", frame -> {
            FlatDarkLaf.setup();
            return new ThemeGardenView(frame::pack);
        });

        private final String name;
        /* The factory is a stateless lambda; error-prone cannot see that through the interface: */
        @SuppressWarnings("ImmutableEnumChecker")
        private final ViewFactory factory;

        Example( String name, ViewFactory factory ) {
            this.name    = name;
            this.factory = factory;
        }

        static List<Example> parse( String spec ) {
            List<Example> selected = new ArrayList<>();
            for ( String part : spec.split(",", -1) ) {
                String wanted = part.trim().toLowerCase(Locale.ROOT);
                if ( wanted.isEmpty() )
                    continue;
                if ( wanted.equals("all") ) {
                    for ( Example example : values() )
                        if ( !selected.contains(example) ) selected.add(example);
                    continue;
                }
                Example found = null;
                for ( Example example : values() )
                    if ( example.name.equals(wanted) ) found = example;
                if ( found == null )
                    throw new IllegalArgumentException(
                        "There is no example UI named '" + wanted + "'. Known names: " + namesAsString() + ", all"
                    );
                if ( !selected.contains(found) )
                    selected.add(found);
            }
            if ( selected.isEmpty() )
                throw new IllegalArgumentException("No example UI selected, see -Dbenchmark.view=..");
            return selected;
        }

        static String namesAsString() {
            StringBuilder result = new StringBuilder();
            for ( Example example : values() )
                result.append(result.length() == 0 ? "" : ", ").append(example.name);
            return result.toString();
        }
    }

    /*
     *  The window geometry is overridable, because it decides *which* regime is measured,
     *  not merely how much of it. A style layer is only admitted to the render cache below
     *  a pixel area budget, so at a large size almost every layer of a view is too big to
     *  cache and is drawn straight onto the destination - the exact-size caching that an
     *  ordinary window leans on hardly runs at all, and a change to it measures as nothing
     *  here while mattering a great deal in a real window. Sweep more than one geometry
     *  before believing a cache result.
     *
     *  Left unset, each example is measured at its own preferred size, which is the size its
     *  author designed it for and the only geometry that is comparable across examples that
     *  are not equally large. Setting the properties forces every example to one geometry.
     */
    private static final int UI_SCALE      = Integer.getInteger("benchmark.scale",  2);
    private static final int WINDOW_WIDTH  = Integer.getInteger("benchmark.width",  0);
    private static final int WINDOW_HEIGHT = Integer.getInteger("benchmark.height", 0);
    private static final int NARROW_WIDTH  = Integer.getInteger("benchmark.narrow", 0);
    /** The fraction of the full width the narrowest swept width sits at, when not given. */
    private static final double NARROW_FRACTION = 0.46;
    /*
     *  Raise these when profiling rather than measuring: a JFR execution sampler ticks
     *  every few milliseconds, and the default sweep counts amount to about a second of
     *  work in total, which is not enough sample mass to rank anything by.
     */
    private static final int WARMUP_SWEEPS = Integer.getInteger("benchmark.warmup", 24);
    private static final int TIMED_SWEEPS  = Integer.getInteger("benchmark.sweeps", 40);
    private static final int CLIP_HEIGHT   = Integer.getInteger("benchmark.clip",   300);

    public static void main( String[] args ) throws Exception
    {
        String spec = ( args.length > 0 ? args[0] : System.getProperty("benchmark.view", "chat") );
        List<Example> examples = Example.parse(spec);

        SwingTree.initializeUsing( it -> it.uiScaleFactor(UI_SCALE) );

        Map<Example, Result> results = new LinkedHashMap<>();
        for ( Example example : examples )
            results.put(example, measure(example));

        printTable(results);

        System.exit(0);
    }

    /** What one example measured; all times are medians in milliseconds. */
    private static final class Result
    {
        final Geometry geometry;
        final double   layout;
        final double   paintAfterResize;
        final double   paintAtUnchangedSize;
        final double   paintOfNarrowClip;

        Result( Geometry geometry, double layout,
                double paintAfterResize, double paintAtUnchangedSize, double paintOfNarrowClip
        ) {
            this.geometry             = geometry;
            this.layout               = layout;
            this.paintAfterResize     = paintAfterResize;
            this.paintAtUnchangedSize = paintAtUnchangedSize;
            this.paintOfNarrowClip    = paintOfNarrowClip;
        }

        double dragFrame() { return layout + paintAfterResize; }
    }

    private static Result measure( Example example ) throws Exception
    {
        JFrame[] frameBox = new JFrame[1];
        JComponent[] rootBox = new JComponent[1];
        UI.runNow(() -> {
            JFrame frame = new JFrame(example.name + " resize benchmark");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JComponent view = example.factory.create(frame);
            /*
             *  The view goes into a holder with no layout manager rather than straight into
             *  the content pane, because the geometry has to be *ours*. A window manager is
             *  free to refuse a size - a tiling one always does, and a floating one clamps
             *  to the screen, which on a 1920px display silently pins every sweep of a wider
             *  benchmark to the same width and turns the fresh-width sweep below (the one
             *  thing that makes this measure a drag rather than a cache hit) into a no-op.
             *  Sizing the view directly inside an unmanaged holder takes the window manager
             *  out of the measurement entirely; the frame is then only what makes the tree
             *  displayable and supplies the graphics configuration.
             */
            JPanel holder = new JPanel(null);
            holder.add(view);
            frame.setContentPane(holder);
            frame.setSize(800, 600);
            frame.setVisible(true);
            frameBox[0] = frame;
            rootBox[0]  = view;
        });
        JFrame frame = frameBox[0];
        JComponent root = rootBox[0];
        // Give the toolkit a moment to actually map the window, or the first frames
        // would be measuring the window manager instead of us:
        Thread.sleep(1500);

        Geometry geometry = geometryFor(root);

        System.out.println();
        System.out.println("  measuring '" + example.name + "' at " + geometry.width + "x" + geometry.height +
                           " (UI scale " + UI_SCALE + ")");

        double layoutMillis         = measureLayoutSweep(root, geometry);
        double paintAfterResize     = measurePaintSweep(frame, root, geometry, true,  geometry.height);
        double paintAtUnchangedSize = measurePaintSweep(frame, root, geometry, false, geometry.height);
        double paintOfNarrowClip    = measurePaintSweep(frame, root, geometry, false, CLIP_HEIGHT);

        UI.runNow(frame::dispose);

        return new Result(geometry, layoutMillis, paintAfterResize, paintAtUnchangedSize, paintOfNarrowClip);
    }

    /** The size the sweeps run at, and the widths they visit on the way there. */
    private static final class Geometry
    {
        final int width;
        final int height;
        final int narrow;
        final int stride;

        Geometry( int width, int height, int narrow, int stride ) {
            this.width  = width;
            this.height = height;
            this.narrow = narrow;
            this.stride = stride;
        }

        /**
         *  The width of the {@code index}-th sweep, which is a width no earlier sweep has used. <br>
         *  <br>
         *  This matters far more than it looks. Sweeping back and forth between two widths lets the
         *  style caches keep an entry for each of them, so after the warmup every second frame is a
         *  cache <i>hit</i> - and a sweep meant to measure what a drag frame costs would instead be
         *  measuring the one thing a drag never gets. A real drag emits a new width per frame and
         *  never revisits one, which is what the stride reproduces: it is coprime with the width
         *  range, so all {@code WARMUP_SWEEPS + TIMED_SWEEPS} widths are distinct.
         */
        int widthForSweep( int index ) {
            return narrow + ( index * stride ) % ( width - narrow );
        }
    }

    /**
     *  The geometry of an example: whatever the properties say, and otherwise the size the
     *  view itself asks for, which is the only size that is fair to compare across examples
     *  that were not designed equally large.
     */
    private static Geometry geometryFor( JComponent root ) throws Exception {
        Dimension[] box = new Dimension[1];
        UI.runNow(() -> box[0] = root.getPreferredSize());
        int width  = ( WINDOW_WIDTH  > 0 ? WINDOW_WIDTH  : Math.max(320, box[0].width ) );
        int height = ( WINDOW_HEIGHT > 0 ? WINDOW_HEIGHT : Math.max(240, box[0].height) );
        int narrow = ( NARROW_WIDTH  > 0 ? NARROW_WIDTH  : Math.max(64, (int) ( width * NARROW_FRACTION )) );
        if ( narrow >= width )
            narrow = width / 2;
        /*
         *  The stride has to be coprime with the swept range for the widths to stay distinct,
         *  and the range depends on the example, so it cannot be a constant:
         */
        int stride = 1;
        for ( int candidate : new int[]{ 17, 19, 23, 29, 31, 37 } )
            if ( ( width - narrow ) % candidate != 0 ) { stride = candidate; break; }

        return new Geometry(width, height, narrow, stride);
    }

    /**
     *  Times a full re-layout of the tree, each sweep at a fresh width. The tree is invalidated
     *  explicitly because a size change alone leaves most of it valid, and a valid tree
     *  validates in nanoseconds, which would measure nothing at all.
     */
    private static double measureLayoutSweep( JComponent root, Geometry geometry ) throws Exception
    {
        for ( int i = 0; i < WARMUP_SWEEPS; i++ )
            layoutOnce(root, geometry, i);

        long[] samples = new long[TIMED_SWEEPS];
        for ( int i = 0; i < TIMED_SWEEPS; i++ )
            samples[i] = layoutOnce(root, geometry, WARMUP_SWEEPS + i);

        return medianMillis(samples);
    }

    private static long layoutOnce( JComponent root, Geometry geometry, int index ) throws Exception
    {
        int sweepWidth = geometry.widthForSweep(index);
        long[] nanos = new long[1];
        UI.runNow(() -> {
            root.setBounds(0, 0, sweepWidth, geometry.height);
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
    private static double measurePaintSweep(
        JFrame frame, JComponent root, Geometry geometry, boolean resizeFirst, int clipHeight
    ) throws Exception {
        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
    /*
        The buffer is held in a one element array because a VolatileImage may have to be
        *replaced* mid run: its surface lives in video memory and the windowing system is free
        to reclaim it, after which it returns either restored-but-blank or altogether
        incompatible. Rendering into an unvalidated surface can quietly fall back to a software
        one, which for a benchmark comparing rendering paths is not noise but a wrong answer.
        See RoundedGradientBenchmark.paintOnce for the full reasoning.
    */
        VolatileImage[] buffer = { gc.createCompatibleVolatileImage(geometry.width, geometry.height) };

        for ( int i = 0; i < WARMUP_SWEEPS; i++ )
            paintOnce(frame, root, buffer, geometry, resizeFirst, clipHeight, i);

        long[] samples = new long[TIMED_SWEEPS];
        for ( int i = 0; i < TIMED_SWEEPS; i++ )
            samples[i] = paintOnce(frame, root, buffer, geometry, resizeFirst, clipHeight, WARMUP_SWEEPS + i);

        buffer[0].flush();
        return medianMillis(samples);
    }

    private static long paintOnce(
        JFrame frame, JComponent root, VolatileImage[] buffer,
        Geometry geometry, boolean resizeFirst, int clipHeight, int index
    ) throws Exception {
        int sweepWidth = ( resizeFirst ? geometry.widthForSweep(index) : geometry.width );
        long[] nanos = new long[1];
        UI.runNow(() -> {
            if ( resizeFirst || root.getWidth() != sweepWidth || root.getHeight() != geometry.height ) {
                root.setBounds(0, 0, sweepWidth, geometry.height);
                invalidateTree(root);
                root.validate();
            }
            final GraphicsConfiguration gc = frame.getGraphicsConfiguration();
            do {
                if ( buffer[0].validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE ) {
                    buffer[0].flush();
                    buffer[0] = gc.createCompatibleVolatileImage(geometry.width, geometry.height);
                    buffer[0].validate(gc);
                }
                Graphics2D g = buffer[0].createGraphics();
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
            }
            while ( buffer[0].contentsLost() );
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

    private static void printTable( Map<Example, Result> results ) {
        System.out.println();
        System.out.println("  View resize benchmark, UI scale " + UI_SCALE);
        System.out.println("  median of " + TIMED_SWEEPS + " sweeps after " + WARMUP_SWEEPS + " warmup sweeps, in milliseconds");
        System.out.println("  ---------------------------------------------------------------------------------------------");
        System.out.printf ("  %-10s %11s %9s %9s %9s %9s %9s%n",
                           "view", "size", "layout", "paint±", "paint=", "clip" + CLIP_HEIGHT, "drag");
        for ( Map.Entry<Example, Result> entry : results.entrySet() ) {
            Result r = entry.getValue();
            System.out.printf("  %-10s %11s %9.2f %9.2f %9.2f %9.2f %9.2f%n",
                              entry.getKey().name, r.geometry.width + "x" + r.geometry.height,
                              r.layout, r.paintAfterResize, r.paintAtUnchangedSize,
                              r.paintOfNarrowClip, r.dragFrame());
        }
        System.out.println();
        System.out.println("    layout   full re-layout at a width never laid out before");
        System.out.println("    paint±   repaint after a size change (style caches missing)");
        System.out.println("    paint=   repaint at an unchanged size (style caches warm)");
        System.out.println("    clip     repaint of a " + CLIP_HEIGHT + "px high clip, as a caret blink or a hover would");
        System.out.println("    drag     layout + paint±, which is what one frame of a window drag costs");
        System.out.println();
    }

    private ViewResizeBenchmark() {}
}
