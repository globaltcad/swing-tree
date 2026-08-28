package examples.stylish;

import sprouts.Var;
import swingtree.UI;
import swingtree.UIForLabel;
import swingtree.UIForPanel;
import swingtree.UIForScrollPane;
import swingtree.api.Configurator;
import swingtree.api.Styler;
import swingtree.layout.FlowCell;
import swingtree.layout.Size;
import swingtree.style.FilterConf;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.util.ArrayList;
import java.util.List;

import static swingtree.UI.*;

/**
 *  A bench for {@code parentFilter(..)}: every way of configuring the filter, side by side over
 *  the same backdrop, with the shared parts of the configuration on live sliders.
 *  <p>
 *  A component with a parent filter shows its own parent through itself, put through a filter on
 *  the way. {@link GlassUIView} shows the idiom in its simplest form - one frosted label. This
 *  view is the opposite: it exists to make the filter <em>easy to look at wrongly</em>, so that a
 *  defect in one configuration is visible next to a dozen configurations that are fine.
 *  <p>
 *  Each tile is a specimen: a titled panel with no background of its own, sitting inside a
 *  backdrop of hard-edged noise over a diagonal gradient. Everything the specimen shows arrives
 *  through its filter, and the backdrop stays crisp all around it, so the two can be compared
 *  without moving one's eyes. The noise is what makes a blur radius legible, and the gradient is
 *  what makes an offset or a scale legible.
 *  <p>
 *  What varies from tile to tile is the part of a filter that cannot be put on a slider: the blur
 *  radius, the convolution matrix, and which area of the component the result may show in. Scale
 *  and offset apply to every tile at once and are driven from the panel at the top, because they
 *  are continuous - a tile per useful combination would run into the hundreds.
 *  <p>
 *  Two things worth knowing while reading a tile:
 *  <ul>
 *      <li>A wide blur is not convolved at full resolution. Past a radius of 8 the parent is
 *          shrunk first, blurred with a proportionally smaller kernel, and stretched back out.
 *          The blur ladder crosses that threshold twice over, so a tile which disagrees with its
 *          neighbours points straight at it.</li>
 *      <li>A convolution matrix of one's own is never shrunk - a matrix says what to do with
 *          neighbouring pixels, and at a coarser raster those would be other pixels. The tile
 *          which combines a matrix with a wide blur is the one that proves it.</li>
 *  </ul>
 *  <b>Repaint in patches</b> paints every specimen again in small squares rather than in one go,
 *  which is what Swing does when a pointer passes over a window. A filter reads the parent for a
 *  good distance around each pixel it produces, so a filter which lets the size of the repainted
 *  rectangle reach its output leaves seams along the squares - and a user would meet that as a
 *  pane which shifts whenever they touch it.
 *
 *  @see GlassUIView
 */
public final class ParentFilterView extends Panel
{
    private static final int PAGE_REFERENCE_WIDTH     = 1180;
    private static final int CONTROLS_REFERENCE_WIDTH = 1000;

    /** The full row at every size - the building block of a stacked layout. */
    private static final FlowCell FULL_ROW = AUTO_SPAN( it -> it
            .verySmall(12).small(12).medium(12).large(12).veryLarge(12).oversize(12) );

    /** One specimen: a single column when narrow, up to six across when the window is wide. */
    private static final FlowCell TILE = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(6).medium(4).large(3).veryLarge(3).oversize(2) );

    /** One labelled slider in the control panel. */
    private static final FlowCell KNOB = AUTO_SPAN( it -> it.fill(true)
            .verySmall(12).small(6).medium(6).large(3).veryLarge(3).oversize(3) );

    private static final Color INK        = Color.ofRgb(0xF2, 0xEC, 0xE3);
    private static final Color FAINT_INK  = Color.ofRgb(0x9A, 0x93, 0x8A);
    private static final Color PAGE       = Color.ofRgb(0x16, 0x18, 0x1D);
    private static final Color CARD       = Color.ofRgb(0x22, 0x25, 0x2C);
    private static final Color EDGE       = Color.ofRgb(0x33, 0x38, 0x42);
    private static final Color ACCENT     = Color.ofRgb(0xE8, 0xA1, 0x4B);
    private static final Color BACKDROP_A = Color.ofRgb(0x2B, 0x4F, 0xE0);
    private static final Color BACKDROP_B = Color.ofRgb(0xE0, 0x3A, 0x86);

    /**
     *  The scale and the offset every specimen shares, held as one value so that a tile's title
     *  and its filter are always describing the same thing.
     */
    private static final class Warp
    {
        static final Warp NEUTRAL = new Warp(100, 100, 0, 0);

        private final int scaleXPercent;
        private final int scaleYPercent;
        private final int offsetX;
        private final int offsetY;

        Warp( int scaleXPercent, int scaleYPercent, int offsetX, int offsetY ) {
            this.scaleXPercent = scaleXPercent;
            this.scaleYPercent = scaleYPercent;
            this.offsetX       = offsetX;
            this.offsetY       = offsetY;
        }

        int    scaleXPercent()             { return scaleXPercent; }
        int    scaleYPercent()             { return scaleYPercent; }
        int    offsetX()                   { return offsetX; }
        int    offsetY()                   { return offsetY; }
        double scaleX()                    { return scaleXPercent / 100d; }
        double scaleY()                    { return scaleYPercent / 100d; }
        Warp   withScaleXPercent( int p )  { return new Warp(p, scaleYPercent, offsetX, offsetY); }
        Warp   withScaleYPercent( int p )  { return new Warp(scaleXPercent, p, offsetX, offsetY); }
        Warp   withOffsetX( int x )        { return new Warp(scaleXPercent, scaleYPercent, x, offsetY); }
        Warp   withOffsetY( int y )        { return new Warp(scaleXPercent, scaleYPercent, offsetX, y); }

        /** How the shared part of every filter currently reads, for a tile to append to its own. */
        String describe() {
            return "Scale (" + trim(scaleX()) + ", " + trim(scaleY()) + ")"
                 + " · Offset (" + offsetX + ", " + offsetY + ")";
        }

        private static String trim( double value ) {
            return String.valueOf(Math.round(value * 100) / 100d);
        }
    }

    /** One tile's own share of a filter: what a slider cannot express. */
    private static final class Recipe
    {
        final String                    title;
        final Configurator<FilterConf>  own;
        final boolean                   filters;

        private Recipe( String title, boolean filters, Configurator<FilterConf> own ) {
            this.title   = title;
            this.filters = filters;
            this.own     = own;
        }

        static Recipe of( String title, Configurator<FilterConf> own ) {
            return new Recipe(title, true, own);
        }

        /** A tile which does not filter at all, so that every other tile has a reference. */
        static Recipe unfiltered( String title ) {
            return new Recipe(title, false, Configurator.none());
        }
    }

    private final Var<Warp>        warp      = Var.of(Warp.NEUTRAL);
    private final List<JComponent> specimens = new ArrayList<>();

    public ParentFilterView() {
        UI.of(this).withLayout("fill, wrap 1, ins 0, gap 0").withPrefSize(1180, 820)
        .withStyle( it -> it.backgroundColor(PAGE) )
        .add("growx, wmin 0", header())
        .add("grow, push, wmin 0", page());
    }

    // ── Header ───────────────────────────────────────────────────────────────
    //
    // Plain MigLayout, not a flow grid: a grid declares its reference width through an explicit
    // preferred size, and a MigLayout parent would read that height literally.

    private UIForPanel<JPanel> header() {
        return UI.panel("fill, wrap 1, ins 22 26 18 26, gap 4")
            .withStyle( it -> it.backgroundColor(PAGE).borderWidthAt(Edge.BOTTOM, 1).borderColor(EDGE) )
            .add("growx, wmin 0",
                UI.label("Parent filter bench")
                .withStyle( it -> it.fontSize(21).fontColor(INK).componentFont( f -> f.weight(1.6f) ) )
            )
            .add("growx, wmin 0",
                UI.label(
                    "Every tile shows the same backdrop through a different filter. " +
                    "Scale and offset below apply to all of them."
                )
                .withStyle( it -> it.fontSize(13).fontColor(FAINT_INK) )
            );
    }

    // ── The page: a scrolling 12-column grid holding the controls and every specimen ──────────
    //
    // A flow grid gives every row the height of its tallest child, so once the tiles stack the
    // page outgrows the window - hence the scroll pane.

    private UIForScrollPane<JScrollPane> page() {
        return UI.scrollPane( conf -> conf.fitWidth(true) )
            .withHorizontalScrollBarPolicy(UI.Active.NEVER)
            .withVerticalScrollIncrement(28)
            .withStyle( it -> it.backgroundColor(PAGE).borderWidth(0).padding(0) )
            .add(grid());
    }

    private UIForPanel<JPanel> grid() {
        UIForPanel<JPanel> grid =
                UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 16, 16)
                .withMinSize(0, 0) // a grid reports the SUM of its children's minimums
                .withPrefSize(PAGE_REFERENCE_WIDTH, 0) // the reference width, see the spans above
                .withStyle( it -> it.backgroundColor(PAGE).padding(20) )
                .add(FULL_ROW, controls());
        for ( Recipe recipe : recipes() )
            grid = grid.add(TILE, tile(recipe));
        return grid;
    }

    // ── The shared half of every filter ───────────────────────────────────────

    // The card is itself a flow grid rather than a MigLayout panel holding one: a grid carries its
    // reference width as an explicit preferred size, and a MigLayout parent reads that height
    // literally - which lays the sliders out at zero height and shows nothing at all.

    private UIForPanel<JPanel> controls() {
        return UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 14, 12)
            .withMinSize(0, 0)
            .withPrefSize(CONTROLS_REFERENCE_WIDTH, 0)
            .withStyle( cardStyle() )
            .add(KNOB, knob("Scale X", 25, 300, warp.zoomTo(Warp::scaleXPercent, Warp::withScaleXPercent), "%"))
            .add(KNOB, knob("Scale Y", 25, 300, warp.zoomTo(Warp::scaleYPercent, Warp::withScaleYPercent), "%"))
            .add(KNOB, knob("Offset X", -80, 80, warp.zoomTo(Warp::offsetX, Warp::withOffsetX), "px"))
            .add(KNOB, knob("Offset Y", -80, 80, warp.zoomTo(Warp::offsetY, Warp::withOffsetY), "px"))
            .add(FULL_ROW,
                UI.panel("fill, ins 0, gap 10", "[][]push")
                .withStyle( it -> it.backgroundColor(Color.TRANSPARENT) )
                .add(UI.button("Reset").onClick( it -> warp.set(Warp.NEUTRAL) ))
                .add(UI.button("Repaint in patches").onClick( it -> UI.run(this::repaintInPatches) ))
            )
            .add(FULL_ROW,
                UI.label(
                    "Patches repaint each specimen in small squares, the way a hover does. Seams " +
                    "along the squares mean the filter is reading the repainted rectangle rather " +
                    "than the parent."
                )
                .withStyle( it -> it.fontSize(12).fontColor(FAINT_INK) )
            );
    }

    private UIForPanel<JPanel> knob(
        String name, int min, int max, Var<Integer> value, String unit
    ) {
        return UI.panel("fill, wrap 1, ins 0, gap 2")
            .withStyle( it -> it.backgroundColor(Color.TRANSPARENT) )
            .add("growx, wmin 0",
                UI.label(value.viewAsString( v -> name + "   " + v + unit ))
                .withStyle( it -> it.fontSize(12).fontColor(FAINT_INK) )
            )
            .add("growx, wmin 0", UI.slider(UI.Align.HORIZONTAL, min, max, value));
    }

    // ── One specimen ──────────────────────────────────────────────────────────
    //
    // The specimen has no background of its own, so everything inside it arrives through the
    // filter. Its box model is the same in every tile - a margin, a thick border and a padding -
    // because that is what tells the four `area(..)` tiles apart.

    private UIForPanel<JPanel> tile( Recipe recipe ) {
        JPanel specimen = specimen(recipe);
        specimens.add(specimen);
        return card("fill, wrap 1, ins 10, gap 3")
            .add("growx, wmin 0", title(recipe))
            .add("growx, wmin 0", subtitle(recipe))
            .add("grow, push, gaptop 5",
                UI.panel("fill, ins 0")
                .withPrefSize(250, 150)
                .withStyle( it -> it
                    .borderRadius(10)
                    .backgroundColor(BACKDROP_A)
                    .gradient( g -> g
                        .colors(BACKDROP_A, BACKDROP_B)
                        .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                    )
                    .noise( n -> n
                        .function(UI.NoiseType.TILES)
                        .scale(2.5)
                        .colors(Color.ofRgba(255, 255, 255, 120), Color.ofRgba(0, 0, 0, 120))
                    )
                )
                .add("grow", UI.of(specimen))
            );
    }

    /** What this tile alone does - the half of the filter no slider can express. */
    private UIForLabel<JLabel> title( Recipe recipe ) {
        return UI.label(recipe.title)
            .withStyle( it -> it
                .fontSize(12)
                .fontColor(recipe.filters ? INK : ACCENT)
                .componentFont( f -> f.family("Monospaced").weight(1.4f) )
            );
    }

    /** The half of the filter the sliders drive, which every tile shares. */
    private UIForLabel<JLabel> subtitle( Recipe recipe ) {
        return UI.label(recipe.filters ? warp.viewAsString(Warp::describe) : Var.of("-"))
            .withStyle( it -> it
                .fontSize(10)
                .fontColor(FAINT_INK)
                .componentFont( f -> f.family("Monospaced") )
            );
    }

    private JPanel specimen( Recipe recipe ) {
        UIForPanel<JPanel> builder =
                UI.panel("fill, ins 0")
                .withStyle( it -> it
                    .backgroundColor(Color.TRANSPARENT)
                    .margin(14)
                    .border(4, Color.ofRgba(255, 255, 255, 90))
                    .padding(12)
                    .borderRadius(12)
                );
        if ( recipe.filters )
            builder = builder.withStyle(warp, (w, it) -> it
                .parentFilter( f -> recipe.own.configure(f)
                                        .scale(w.scaleX(), w.scaleY())
                                        .offset(w.offsetX(), w.offsetY()) )
            );
        return builder.get(JPanel.class);
    }

    private UIForPanel<JPanel> card( String layout ) {
        return UI.panel(layout).withStyle( cardStyle() );
    }

    private static Styler<JPanel> cardStyle() {
        return it -> it
                .backgroundColor(CARD)
                .border(1, EDGE)
                .borderRadius(14)
                .padding(16)
                .shadowColor(Color.ofRgba(0, 0, 0, 120))
                .shadowBlurRadius(10)
                .shadowSpreadRadius(-2)
                .shadowOffset(0, 3);
    }

    // ── What the tiles are ────────────────────────────────────────────────────
    //
    // The blur ladder crosses the radius at which the filter stops convolving at full resolution
    // (8) and the radius at which it stops shrinking further (64), because a defect in the
    // shrinking shows up as one rung disagreeing with the rungs on either side of it.

    private static List<Recipe> recipes() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(Recipe.unfiltered("No filter (reference)"));
        recipes.add(Recipe.of("Blur 0",   f -> f.blur(0)));
        recipes.add(Recipe.of("Blur 3",   f -> f.blur(3)));
        recipes.add(Recipe.of("Blur 7",   f -> f.blur(7)));
        recipes.add(Recipe.of("Blur 8",   f -> f.blur(8)));
        recipes.add(Recipe.of("Blur 16",  f -> f.blur(16)));
        recipes.add(Recipe.of("Blur 32",  f -> f.blur(32)));
        recipes.add(Recipe.of("Blur 64",  f -> f.blur(64)));
        recipes.add(Recipe.of("Kernel identity 3x3",
                f -> f.kernel(Size.of(3, 3), 0, 0, 0,
                                             0, 1, 0,
                                             0, 0, 0)));
        recipes.add(Recipe.of("Kernel box blur 3x3",
                f -> f.kernel(Size.of(3, 3), 1/9d, 1/9d, 1/9d,
                                             1/9d, 1/9d, 1/9d,
                                             1/9d, 1/9d, 1/9d)));
        recipes.add(Recipe.of("Kernel sharpen 3x3",
                f -> f.kernel(Size.of(3, 3),  0, -1,  0,
                                             -1,  5, -1,
                                              0, -1,  0)));
        recipes.add(Recipe.of("Kernel edges 3x3",
                f -> f.kernel(Size.of(3, 3), -1, -1, -1,
                                             -1,  8, -1,
                                             -1, -1, -1)));
        recipes.add(Recipe.of("Kernel emboss 3x3",
                f -> f.kernel(Size.of(3, 3), -2, -1, 0,
                                             -1,  1, 1,
                                              0,  1, 2)));
        recipes.add(Recipe.of("Kernel gauss 5x5",
                f -> f.kernel(Size.of(5, 5), 1/256d,  4/256d,  6/256d,  4/256d, 1/256d,
                                             4/256d, 16/256d, 24/256d, 16/256d, 4/256d,
                                             6/256d, 24/256d, 36/256d, 24/256d, 6/256d,
                                             4/256d, 16/256d, 24/256d, 16/256d, 4/256d,
                                             1/256d,  4/256d,  6/256d,  4/256d, 1/256d)));
        recipes.add(Recipe.of("Kernel sharpen + Blur 24",
                f -> f.kernel(Size.of(3, 3),  0, -1,  0,
                                             -1,  5, -1,
                                              0, -1,  0).blur(24)));
        recipes.add(Recipe.of("Blur 24 · area ALL",      f -> f.blur(24).area(ComponentArea.ALL)));
        recipes.add(Recipe.of("Blur 24 · area BODY",     f -> f.blur(24).area(ComponentArea.BODY)));
        recipes.add(Recipe.of("Blur 24 · area INTERIOR", f -> f.blur(24).area(ComponentArea.INTERIOR)));
        recipes.add(Recipe.of("Blur 24 · area BORDER",   f -> f.blur(24).area(ComponentArea.BORDER)));
        recipes.add(Recipe.of("Blur 24 · area EXTERIOR", f -> f.blur(24).area(ComponentArea.EXTERIOR)));
        return recipes;
    }

    /**
     *  Paints every specimen again in small squares instead of in one go, which is what Swing
     *  does when a pointer arrives on a component: it hands the paint a clip, and redraws nothing
     *  outside it. A filter has to read the parent well beyond the square it is drawing into, so
     *  a filter which lets the square decide what it reads leaves a seam at every square's edge.
     */
    private void repaintInPatches() {
        for ( JComponent specimen : specimens ) {
            int width  = specimen.getWidth();
            int height = specimen.getHeight();
            if ( width <= 0 || height <= 0 || !specimen.isShowing() )
                continue;
            int patch = 24;
            for ( int y = 0; y < height; y += patch )
                for ( int x = 0; x < width; x += patch )
                    specimen.paintImmediately(
                            x, y,
                            Math.min(patch, width - x),
                            Math.min(patch, height - y)
                        );
        }
    }

    public static void main( String... args ) {
        UI.show("Parent filter bench", f -> new ParentFilterView());
    }
}
