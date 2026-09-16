package examples.grids;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.UIForBox;
import swingtree.UIForPanel;
import swingtree.UIForTextArea;
import swingtree.api.Layout;
import swingtree.api.model.SliderTicks;
import swingtree.components.JBox;
import swingtree.layout.FlowCell;
import swingtree.layout.UniformGridLayout.CollapseEmpty;
import swingtree.layout.UniformGridLayout.OverflowGrowth;
import swingtree.layout.UniformGridLayout.Mode;
import swingtree.threading.EventProcessor;

import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import static swingtree.UI.*;

/**
 *  <b>The Tile Workshop — an interactive guide to the grid layout of SwingTree.</b>
 *  <p>
 *  A small application for testing {@code withGridLayout(..)} and {@code Layout.grid(..)}
 *  by hand. Every tab lays out numbered tiles with a {@code UniformGridLayout}, explains
 *  what the tab is about, shows the SwingTree code of its grid and offers a slider which
 *  adds or removes tiles, so you can watch the grid rearrange them:
 *  <ol>
 *      <li><b>Declared columns</b> — a grid of 2 rows and 5 columns keeps its 5 columns,
 *          next to a {@link GridLayout} of the JDK which does not.</li>
 *      <li><b>Empty space</b> — rows and columns which no tile occupies are left out.</li>
 *      <li><b>CollapseEmpty</b> — the four choices of what to leave out, side by side.</li>
 *      <li><b>Like GridLayout</b> — the settings which lay out tiles exactly like a
 *          {@link GridLayout}, next to one.</li>
 *      <li><b>Open counts</b> — a row or column count of 0.</li>
 *      <li><b>Workbench</b> — every setting chosen by hand, including how the grid grows when
 *          it holds more tiles than cells, bound through a {@code Val<Layout>}, optionally next
 *          to a {@link GridLayout} with the same counts.</li>
 *  </ol>
 *  The numbers on the tiles are the order in which they were added, which makes the
 *  order in which a grid fills its cells visible. Run {@link #main(String...)} to open it.
 *  <p>
 *  <b>It converges.</b> The controls, the explanation and the code of a tab are cards in a
 *  responsive 12 column grid (see {@link #TOP_REFERENCE_WIDTH}): on a very wide window they
 *  sit side by side, on a large one the controls take a row of their own above the
 *  explanation and the code, and on a narrow one everything stacks. The fields of the
 *  workbench are a grid of their own inside their card. The tiles below the cards get all
 *  the height that is left, and the whole window scrolls once it is too short for them.
 */
public final class TileWorkshopView extends JPanel
{
    private static final Color PAGE      = new Color(246, 243, 238);
    private static final Color CARD      = new Color(255, 253, 249);
    private static final Color STAGE     = new Color(236, 231, 222);
    private static final Color HAIRLINE  = new Color(222, 214, 200);
    private static final Color INK       = new Color( 46,  42,  36);
    private static final Color SOFT_INK  = new Color( 96,  89,  78);
    private static final Color ACCENT    = new Color(178,  86,  52);
    private static final Color CODE      = new Color( 43,  42,  51);
    private static final Color CODE_INK  = new Color(236, 232, 222);

    private static final int MAX_TILES = 16;

    /**
     *  The width at which the cards at the top of a tab consider their grid full. A tab
     *  of a window 1360 pixels wide is in the {@code VERY_LARGE} band of it, which puts
     *  the controls, the explanation and the code side by side.
     */
    private static final int TOP_REFERENCE_WIDTH    = 1500;
    /**
     *  The width at which the fields of the workbench consider their card full: 4 fields
     *  share a row from 800 pixels on, 2 from 400 pixels, and below that each field gets a
     *  row of its own.
     */
    private static final int FIELDS_REFERENCE_WIDTH = 1000;

    private static final FlowCell FULL_ROW =
            AUTO_SPAN( it -> it.fill(true).verySmall(12).small(12).medium(12).large(12).veryLarge(12).oversize(12) );
    private static final FlowCell CONTROLS_SPAN =
            AUTO_SPAN( it -> it.fill(true).verySmall(12).small(12).medium(12).large(12).veryLarge(3).oversize(3) );
    private static final FlowCell EXPLANATION_SPAN =
            AUTO_SPAN( it -> it.fill(true).verySmall(12).small(12).medium(12).large(7).veryLarge(5).oversize(5) );
    private static final FlowCell CODE_SPAN =
            AUTO_SPAN( it -> it.fill(true).verySmall(12).small(12).medium(12).large(5).veryLarge(4).oversize(4) );

    private static final FlowCell WORKBENCH_EXPLANATION_SPAN =
            AUTO_SPAN( it -> it.fill(true).verySmall(12).small(12).medium(12).large(7).veryLarge(7).oversize(7) );
    private static final FlowCell WORKBENCH_CODE_SPAN =
            AUTO_SPAN( it -> it.fill(true).verySmall(12).small(12).medium(12).large(5).veryLarge(5).oversize(5) );
    private static final FlowCell FIELD_SPAN =
            AUTO_SPAN( it -> it.fill(true).verySmall(12).small(12).medium(6).large(6).veryLarge(3).oversize(3) );

    private static final SliderTicks<Integer> TILE_TICKS =
            SliderTicks.of(Integer.class).withMajorSpacing(4).withMinorTicksBetween(3).withLabelsAtMajorTicks();

    private static final SliderTicks<Integer> GAP_TICKS =
            SliderTicks.of(Integer.class).withMajorSpacing(6).withMinorTicksBetween(5).withLabelsAtMajorTicks();

    private static final String INTRODUCTION =
            "Every tab of this workshop lays out numbered tiles with a UniformGridLayout, the layout manager " +
            "behind withGridLayout(..) and Layout.grid(..). Drag the slider of a tab to add or remove tiles, " +
            "read what the tab is about, and check that its grid behaves exactly as described. The tiles are " +
            "numbered in the order they were added, so you can follow how a grid fills its cells: row by row, " +
            "starting at the top. In the last tab, Workbench, you choose every setting yourself.";

    public TileWorkshopView( Var<TileWorkshopViewModel> vm ) {
        UI.of(this).withLayout("fill, ins 0")
        .withPrefSize(1360, 940)
        .withStyle( it -> it.backgroundColor(PAGE) )
        .add(GROW.and("wmin 0, hmin 0"), scrolling(
            box("fill, wrap 1, ins 18 22 18 22, gap 12", "[grow, fill]", "[][grow, fill]")
            .add("wmin 0, hmin pref", header())
            .add(GROW.and(PUSH).and("wmin 0"),
                tabbedPane()
                .add(tab("Declared columns").add(declaredColumnsPage(vm.zoomTo(TileWorkshopViewModel::declaredColumns, TileWorkshopViewModel::withDeclaredColumns))))
                .add(tab("Empty space").add(emptySpacePage(vm.zoomTo(TileWorkshopViewModel::emptySpace, TileWorkshopViewModel::withEmptySpace))))
                .add(tab("CollapseEmpty").add(collapseChoicesPage(vm.zoomTo(TileWorkshopViewModel::collapseChoices, TileWorkshopViewModel::withCollapseChoices))))
                .add(tab("Like GridLayout").add(likeGridLayoutPage(vm.zoomTo(TileWorkshopViewModel::likeGridLayout, TileWorkshopViewModel::withLikeGridLayout))))
                .add(tab("Open counts").add(openCountsPage(vm.zoomTo(TileWorkshopViewModel::openCounts, TileWorkshopViewModel::withOpenCounts))))
                .add(tab("Workbench").add(workbenchPage(vm.zoomTo(TileWorkshopViewModel::workbench, TileWorkshopViewModel::withWorkbench))))
            )
        ));
    }

    private static UIForBox<JBox> header() {
        return box("fillx, wrap 1, ins 0, gap 2", "[grow, fill]")
            .add("wmin 0",
                label("The Tile Workshop")
                .withStyle( it -> it.componentFont( f -> f.size(24).weight(2f).color(INK) ) )
            )
            .add("wmin 0",
                label("An interactive guide to the grid layout of SwingTree")
                .withStyle( it -> it.componentFont( f -> f.size(13).color(ACCENT) ) )
            )
            .add("wmin 0, gaptop 6", prose(INTRODUCTION));
    }

    // ── The scenario tabs ───────────────────────────────────────────────────────

    private static UIForAnySwing<?, ?> declaredColumnsPage( Var<Tuple<Tile>> tiles ) {
        return scenarioPage(
            tiles,
            "A grid of 2 rows and 5 columns keeps its 5 columns",
            "java.awt.GridLayout ignores the number of columns whenever the number of rows is greater " +
            "than 0. It spreads the tiles over its 2 rows instead, so 8 tiles end up in 4 columns and 13 " +
            "tiles in 7. The UniformGridLayout of withGridLayout(2, 5) starts a new row after every 5 " +
            "tiles: 8 tiles fill 2 rows of 5 columns, and an 11th tile starts a third row. With fewer " +
            "than 5 tiles, it leaves out the columns no tile occupies, which the tab Empty space explains.",
            "Try it: slide to 8, 10, 11 and 13 tiles, and compare the columns of both grids.",
            "// SwingTree: a new row after every 5 tiles\n" +
            "UI.panel().withGridLayout(2, 5, 6, 6)\n" +
            "\n" +
            "// The JDK, for comparison\n" +
            "UI.panel()\n" +
            ".withLayout(new GridLayout(2, 5, 6, 6))",
            stage("[grow, fill, sg][grow, fill, sg]", "[grow, fill]")
            .add(GROW.and("wmin 0, hmin 0"),
                gridFrame("withGridLayout(2, 5, 6, 6)",
                    box().withGridLayout(2, 5, 6, 6).addAll(tiles, TileWorkshopView::tile)
                )
            )
            .add(GROW.and("wmin 0, hmin 0"),
                gridFrame("new java.awt.GridLayout(2, 5, 6, 6)",
                    box().withLayout(new GridLayout(2, 5, UI.scale(6), UI.scale(6))).addAll(tiles, TileWorkshopView::tile)
                )
            )
        );
    }

    private static UIForAnySwing<?, ?> emptySpacePage( Var<Tuple<Tile>> tiles ) {
        return scenarioPage(
            tiles,
            "Rows and columns without a tile are left out",
            "By default, a grid only has the rows and columns its tiles occupy, so the tiles share the " +
            "whole grid instead of leaving empty space next to or below them. In a grid of 3 rows and 4 " +
            "columns, 1 tile fills the whole grid, 3 tiles share a single row of 3 columns, and 4 tiles fill " +
            "a row of 4 columns. A 5th tile starts a second row, and a 13th tile a fourth row. Because empty " +
            "rows are left out, the 3 declared rows make no difference here: withGridLayout(0, 4) looks the same.",
            "Try it: start at 1 tile and add one tile after another. The grid never shows a row or a column without a tile.",
            "UI.panel().withGridLayout(3, 4, 6, 6)\n" +
            "\n" +
            "// is the same as\n" +
            "UI.panel().withGridLayout(\n" +
            "    Mode.WRAP_AFTER_COLUMNS,\n" +
            "    CollapseEmpty.ROWS_AND_COLUMNS,\n" +
            "    3, 4, 6, 6\n" +
            ")\n" +
            "// both enums are nested in UniformGridLayout",
            stage("[grow, fill]", "[grow, fill]")
            .add(GROW.and("wmin 0, hmin 0"),
                gridFrame("withGridLayout(3, 4, 6, 6)",
                    box().withGridLayout(3, 4, 6, 6).addAll(tiles, TileWorkshopView::tile)
                )
            )
        );
    }

    private static UIForAnySwing<?, ?> collapseChoicesPage( Var<Tuple<Tile>> tiles ) {
        return scenarioPage(
            tiles,
            "Choose which empty rows and columns are left out",
            "CollapseEmpty decides what happens to the rows and columns no tile occupies. The four grids " +
            "below are all declared with 2 rows and 4 columns, and differ only in that setting. With 3 tiles, " +
            "NONE keeps the empty column and the empty row, COLUMNS leaves out the empty column, ROWS leaves " +
            "out the empty row, and ROWS_AND_COLUMNS, the default, leaves out both. From 5 tiles on, the " +
            "tiles reach every row and every column, so all four grids look alike.",
            "Try it: compare 1, 3 and 4 tiles. The cells of NONE keep their size until the grid is full.",
            "UI.panel().withGridLayout(\n" +
            "    Mode.WRAP_AFTER_COLUMNS,\n" +
            "    CollapseEmpty.NONE,\n" +
            "    2, 4, 6, 6\n" +
            ")\n" +
            "// or CollapseEmpty.COLUMNS,\n" +
            "// ROWS or ROWS_AND_COLUMNS",
            stage("wrap 2", "[grow, fill, sg][grow, fill, sg]", "[grow, fill, sg][grow, fill, sg]")
            .apply( stage -> {
                for ( CollapseEmpty setting : CollapseEmpty.values() )
                    stage.add(GROW.and("wmin 0, hmin 0"),
                        gridFrame("CollapseEmpty." + setting.name(),
                            box()
                            .withGridLayout(Mode.WRAP_AFTER_COLUMNS, setting, 2, 4, 6, 6)
                            .addAll(tiles, TileWorkshopView::tile)
                        )
                    );
            })
        );
    }

    private static UIForAnySwing<?, ?> likeGridLayoutPage( Var<Tuple<Tile>> tiles ) {
        return scenarioPage(
            tiles,
            "Keep the arrangement of java.awt.GridLayout",
            "If a screen of yours relies on how java.awt.GridLayout arranges its components, choose the mode " +
            "SPREAD_OVER_ROWS and collapse nothing. The grid then spreads its tiles over the declared rows and " +
            "ignores the declared columns, exactly like the JDK does, and keeps the rows its tiles do not reach. " +
            "Both grids below are declared with 3 rows and 5 columns, and they look identical for every number " +
            "of tiles. The JDK grid does not follow the UI scale factor, so this app scales its gaps by hand.",
            "Try it: slide through all numbers of tiles. With 2 tiles, both grids keep an empty third row, and with 13 tiles, both use 5 columns.",
            "UI.panel().withGridLayout(\n" +
            "    Mode.SPREAD_OVER_ROWS,\n" +
            "    CollapseEmpty.NONE,\n" +
            "    3, 5, 6, 6\n" +
            ")\n" +
            "\n" +
            "// lays out its children like\n" +
            "UI.panel()\n" +
            ".withLayout(new GridLayout(3, 5, 6, 6))",
            stage("[grow, fill, sg][grow, fill, sg]", "[grow, fill]")
            .add(GROW.and("wmin 0, hmin 0"),
                gridFrame("withGridLayout(SPREAD_OVER_ROWS, NONE, 3, 5, 6, 6)",
                    box().withGridLayout(Mode.SPREAD_OVER_ROWS, CollapseEmpty.NONE, 3, 5, 6, 6).addAll(tiles, TileWorkshopView::tile)
                )
            )
            .add(GROW.and("wmin 0, hmin 0"),
                gridFrame("new java.awt.GridLayout(3, 5, 6, 6)",
                    box().withLayout(new GridLayout(3, 5, UI.scale(6), UI.scale(6))).addAll(tiles, TileWorkshopView::tile)
                )
            )
        );
    }

    private static UIForAnySwing<?, ?> openCountsPage( Var<Tuple<Tile>> tiles ) {
        return scenarioPage(
            tiles,
            "A count of 0 means as many as the tiles need",
            "Leave one of the two counts open with a 0. withGridLayout(0, 3) starts a new row after every " +
            "3 tiles and has as many rows as the tiles need. withGridLayout(2, 0) spreads the tiles over 2 " +
            "rows, in as many columns as they need, so 5 tiles get 3 columns. With a single tile it only has " +
            "1 row, because the empty second row is left out. Only one of the two counts may be 0.",
            "Try it: add tiles, and watch the left grid grow downwards and the right grid grow sideways.",
            "// 3 columns, as many rows as needed\n" +
            "UI.panel().withGridLayout(0, 3, 6, 6)\n" +
            "\n" +
            "// 2 rows, as many columns as needed\n" +
            "UI.panel().withGridLayout(2, 0, 6, 6)",
            stage("[grow, fill, sg][grow, fill, sg]", "[grow, fill]")
            .add(GROW.and("wmin 0, hmin 0"),
                gridFrame("withGridLayout(0, 3, 6, 6)",
                    box().withGridLayout(0, 3, 6, 6).addAll(tiles, TileWorkshopView::tile)
                )
            )
            .add(GROW.and("wmin 0, hmin 0"),
                gridFrame("withGridLayout(2, 0, 6, 6)",
                    box().withGridLayout(2, 0, 6, 6).addAll(tiles, TileWorkshopView::tile)
                )
            )
        );
    }

    // ── The workbench ───────────────────────────────────────────────────────────

    private static UIForAnySwing<?, ?> workbenchPage( Var<Workbench> bench ) {
        Var<Tuple<Tile>>    tiles         = bench.zoomTo(Workbench::tiles,                  Workbench::withTiles);
        Var<Integer>        tileCount     = bench.zoomTo(Workbench::tileCount,              Workbench::withTileCount);
        Var<Mode>           mode          = bench.zoomTo(Workbench::mode,                   Workbench::withMode);
        Var<CollapseEmpty>  collapseEmpty = bench.zoomTo(Workbench::collapseEmpty,          Workbench::withCollapseEmpty);
        Var<OverflowGrowth> growth        = bench.zoomTo(Workbench::overflowGrowth,         Workbench::withOverflowGrowth);
        Var<Integer>        rows          = bench.zoomTo(Workbench::rows,                   Workbench::withDeclaredRows);
        Var<Integer>        columns       = bench.zoomTo(Workbench::columns,                Workbench::withDeclaredColumns);
        Var<Integer>        gap           = bench.zoomTo(Workbench::gap,                    Workbench::withGap);
        Var<Boolean>        rightToLeft   = bench.zoomTo(Workbench::rightToLeft,            Workbench::withRightToLeft);
        Var<Boolean>        compared      = bench.zoomTo(Workbench::comparedWithGridLayout, Workbench::withComparedWithGridLayout);

        Val<Layout> uniformLayout = bench.viewAs(Layout.class, b ->
                                        Layout.grid(b.mode(), b.collapseEmpty(), b.overflowGrowth(), b.rows(), b.columns(), b.gap(), b.gap())
                                    );
        Val<Layout> awtLayout     = bench.viewAs(Layout.class, b -> new AwtGridLayout(b.rows(), b.columns(), b.gap()));
        Val<String> uniformTitle  = bench.viewAsString( b ->
                                        "Layout.grid(" + b.mode().name() + ", " + b.collapseEmpty().name() + ", " +
                                        b.overflowGrowth().name() + ", " +
                                        b.rows() + ", " + b.columns() + ", " + b.gap() + ", " + b.gap() + ")"
                                    );
        Val<String> awtTitle      = awtLayout.viewAsString(Object::toString);

        return page(
            topGrid()
            .add(FULL_ROW,
                panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 14, 10)
                .withMinSize(0, 0)
                .withPrefSize(FIELDS_REFERENCE_WIDTH, 0)
                .withStyle( it -> it.backgroundColor(CARD).border(1, HAIRLINE).borderRadius(12) )
                .add(FIELD_SPAN, tilesField(tileCount))
                .add(FIELD_SPAN,
                    field("Mode",
                        comboBox(mode).withTooltip("How the grid is built from the numbers of rows and columns")
                    )
                )
                .add(FIELD_SPAN,
                    field("CollapseEmpty",
                        comboBox(collapseEmpty).withTooltip("Which rows and columns without a tile are left out")
                    )
                )
                .add(FIELD_SPAN,
                    field("OverflowGrowth",
                        comboBox(growth).withTooltip("How the grid grows when there are more tiles than cells")
                    )
                )
                .add(FIELD_SPAN, field("Gaps", slider(UI.Axis.HORIZONTAL, 0, 24, gap).withTicks(GAP_TICKS)))
                .add(FIELD_SPAN,
                    field("Rows",
                        spinner(new SpinnerNumberModel(3, 0, 12, 1)).withValue(rows)
                        .withTooltip("The declared number of rows, where 0 means as many as the tiles need")
                    )
                )
                .add(FIELD_SPAN,
                    field("Columns",
                        spinner(new SpinnerNumberModel(4, 0, 12, 1)).withValue(columns)
                        .withTooltip("The declared number of columns, where 0 means as many as the tiles need")
                    )
                )
                .add(FIELD_SPAN,
                    field("Layout",
                        box("fillx, wrap 1, ins 0, gap 0 2", "[grow, fill]")
                        .add("wmin 0", checkBox("Right to left", rightToLeft))
                        .add("wmin 0", checkBox("Compare with java.awt.GridLayout", compared))
                    )
                )
                .add(FIELD_SPAN,
                    field("Presets",
                        box("fillx, wrap 1, ins 0, gap 0 4", "[grow, fill]")
                        .add("wmin 0",
                            button("SwingTree defaults")
                            .withTooltip("WRAP_AFTER_COLUMNS and ROWS_AND_COLUMNS")
                            .onClick( it -> bench.update(Workbench::withSwingTreeDefaults) )
                        )
                        .add("wmin 0",
                            button("Like java.awt.GridLayout")
                            .withTooltip("SPREAD_OVER_ROWS and NONE")
                            .onClick( it -> bench.update(Workbench::withGridLayoutSettings) )
                        )
                        .add("wmin 0",
                            button("Overflowing grid")
                            .withTooltip("16 tiles in a declared grid of 1 row and 4 columns, which has only 4 cells")
                            .onClick( it -> bench.update(Workbench::withOverflowShowcase) )
                        )
                    )
                )
            )
            .add(WORKBENCH_EXPLANATION_SPAN,
                explanationCard(
                    "Configure every setting yourself",
                    prose(bench.viewAsString(Workbench::explanation)),
                    smallPrint("Rows and columns can't both be 0: setting one of them to 0 while the other one is 0 sets the other one to 1.")
                )
            )
            .add(WORKBENCH_CODE_SPAN, codeBlock(bench.viewAsString(Workbench::code))),
            stage("", "[grow, fill]")
            .add(GROW.and(PUSH).and("wmin 0, hmin 0, sgx grids"),
                gridFrame(uniformTitle, Val.of(true),
                    box()
                    .withLayout(uniformLayout)
                    .withStyle(rightToLeft, (rtl, it) -> it.orientation(rtl ? UI.ComponentOrientation.RIGHT_TO_LEFT : UI.ComponentOrientation.LEFT_TO_RIGHT))
                    .addAll(tiles, TileWorkshopView::tile)
                )
            )
            .add(GROW.and(PUSH).and("wmin 0, hmin 0, sgx grids"),
                gridFrame(awtTitle, compared,
                    box()
                    .withLayout(awtLayout)
                    .withStyle(rightToLeft, (rtl, it) -> it.orientation(rtl ? UI.ComponentOrientation.RIGHT_TO_LEFT : UI.ComponentOrientation.LEFT_TO_RIGHT))
                    .addAll(tiles, TileWorkshopView::tile)
                )
            )
        );
    }

    // ── Pages and cards ─────────────────────────────────────────────────────────

    private static UIForAnySwing<?, ?> scenarioPage(
        Var<Tuple<Tile>>    tiles,
        String              heading,
        String              explanation,
        String              tryThis,
        String              code,
        UIForAnySwing<?, ?> stage
    ) {
        Var<Integer> tileCount = tiles.zoomTo(Tuple::size, Tile::resized);
        return page(
            topGrid()
            .add(CONTROLS_SPAN,
                panel("fillx, wrap 1, ins 12 14 12 14", "[grow, fill]")
                .withMinSize(0, 0)
                .withStyle( it -> it.backgroundColor(CARD).border(1, HAIRLINE).borderRadius(12) )
                .add("wmin 0", tilesField(tileCount))
            )
            .add(EXPLANATION_SPAN, explanationCard(heading, prose(explanation), prose(tryThis, 11, ACCENT)))
            .add(CODE_SPAN, codeBlock(code)),
            stage
        );
    }

    /**
     *  A tab: the cards of its top grid above the stage with its tiles.
     *  <p>
     *  The stage has to get all the height the cards leave over, and a grid never stretches
     *  a row to the height of its container. So the page is a {@link BorderLayout}, which
     *  gives its centre whatever the north leaves. A {@code BorderLayout} sets the width of
     *  its north before it asks for its preferred height, which is the width for height
     *  question a wrapping grid needs. The grid in the north declares no reference width of
     *  its own, because the {@code BorderLayout} would take that preferred size literally;
     *  the top grid inside it declares one, which is safe for a grid inside a grid.
     */
    private static UIForAnySwing<?, ?> page( UIForAnySwing<?, ?> topGrid, UIForAnySwing<?, ?> stage ) {
        return
            panel()
            .withLayout(new BorderLayout())
            .withStyle( it -> it.backgroundColor(PAGE).padding(4, 4, 16, 4) )
            .add(BorderLayout.NORTH,
                panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 0, 0)
                .withMinSize(0, 0)
                .withStyle( it -> it.backgroundColor(PAGE) )
                .add(FULL_ROW, topGrid)
            )
            .add(BorderLayout.CENTER, stage);
    }

    private static UIForPanel<JPanel> topGrid() {
        return
            panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 12, 12)
            .withMinSize(0, 0)
            .withPrefSize(TOP_REFERENCE_WIDTH, 0)
            .withStyle( it -> it.backgroundColor(PAGE) );
    }

    /**
     *  The panel holding the grids of a tab, with a margin on both sides as wide as the
     *  gaps of the top grid, so that the grids line up with the cards above them.
     */
    private static UIForPanel<JPanel> stage( String columnConstraints, String rowConstraints ) {
        return stage("", columnConstraints, rowConstraints);
    }

    private static UIForPanel<JPanel> stage( String extraLayoutConstraints, String columnConstraints, String rowConstraints ) {
        return
            panel("fill, ins 0 12 0 12, gap 12, hidemode 3" + ( extraLayoutConstraints.isEmpty() ? "" : ", " + extraLayoutConstraints ), columnConstraints, rowConstraints)
            .withMinSize(0, 0)
            .withPrefHeight(320)
            .withStyle( it -> it.backgroundColor(PAGE) );
    }

    private static UIForPanel<JPanel> explanationCard( String heading, UIForAnySwing<?, ?> text, UIForAnySwing<?, ?> footnote ) {
        return
            panel("fillx, wrap 1, ins 12 14 12 14, gap 6", "[grow, fill]")
            .withMinSize(0, 0)
            .withStyle( it -> it.backgroundColor(CARD).border(1, HAIRLINE).borderRadius(12) )
            .add("wmin 0", heading(heading))
            .add("wmin 0", text)
            .add("wmin 0", footnote);
    }

    /** The number of tiles, with its title on top of a slider which has a button on each side. */
    private static UIForBox<JBox> tilesField( Var<Integer> tileCount ) {
        return
            box("fillx, wrap 3, ins 0, gap 4 2", "[][grow, fill][]")
            .withMinSize(0, 0)
            .add("span 2", fieldTitle("Tiles"))
            .add("right",
                label(tileCount.viewAsString( n -> n == 1 ? "1 tile" : n + " tiles" ))
                .withStyle( it -> it.componentFont( f -> f.size(11).color(SOFT_INK) ) )
            )
            .add(
                button("−")
                .withProperty("JButton.buttonType", "toolBarButton")
                .withTooltip("Remove the last tile")
                .onClick( it -> tileCount.update( n -> Math.max(0, n - 1) ) )
            )
            .add("wmin 0",
                slider(UI.Axis.HORIZONTAL, 0, MAX_TILES, tileCount)
                .withTicks(TILE_TICKS)
                .withTooltip("Drag to add or remove tiles")
                .withMinSize(0, 0)
            )
            .add(
                button("+")
                .withProperty("JButton.buttonType", "toolBarButton")
                .withTooltip("Add a tile")
                .onClick( it -> tileCount.update( n -> Math.min(MAX_TILES, n + 1) ) )
            );
    }

    /** A control of the workbench with its title on top of it. */
    private static UIForBox<JBox> field( String title, UIForAnySwing<?, ?> control ) {
        return
            box("fillx, wrap 1, ins 0, gap 0 2", "[grow, fill]")
            .withMinSize(0, 0)
            .add("wmin 0", fieldTitle(title))
            .add("wmin 0", control);
    }

    private static UIForAnySwing<?, ?> gridFrame( String caption, UIForAnySwing<?, ?> grid ) {
        return gridFrame(Val.of(caption), Val.of(true), grid);
    }

    private static UIForAnySwing<?, ?> gridFrame( Val<String> caption, Val<Boolean> visible, UIForAnySwing<?, ?> grid ) {
        return panel("fill, wrap 1, ins 10 12 12 12, gap 8", "[grow, fill]", "[][grow, fill]")
            .withMinSize(0, 0)
            .isVisibleIf(visible)
            .withStyle( it -> it
                .backgroundColor(STAGE)
                .border(1, HAIRLINE)
                .borderRadius(12)
            )
            .add("wmin 0",
                label(caption)
                .withStyle( it -> it.componentFont( f -> f.family("Monospaced").size(11).color(SOFT_INK) ) )
            )
            .add(GROW.and(PUSH).and("wmin 0, hmin 0"), grid);
    }

    private static UIForAnySwing<?, ?> tile( Tile tile ) {
        Color glaze = new Color(tile.glaze().rgb());
        Color edge  = glaze.darker();
        Color text  = tile.glaze().isLight() ? INK : Color.WHITE;
        return box("fill, wrap 1, ins 2, gap 0", "[grow, center]", "push[]0[]push")
            .withMinSize(0, 0)
            .withTooltip("Tile " + tile.number() + ", glazed in " + tile.glaze().title())
            .withStyle( it -> it
                .backgroundColor(glaze)
                .border(1, edge)
                .borderRadius(7)
            )
            .add("wmin 0",
                label(String.valueOf(tile.number()))
                .withStyle( it -> it.componentFont( f -> f.size(16).weight(2f).color(text) ) )
            )
            .add("wmin 0",
                label(tile.glaze().title())
                .withStyle( it -> it.componentFont( f -> f.size(9).color(text) ) )
            );
    }

    private static UIForAnySwing<?, ?> heading( String text ) {
        return label(text).withStyle( it -> it.componentFont( f -> f.size(15).weight(2f).color(INK) ) );
    }

    private static UIForAnySwing<?, ?> fieldTitle( String text ) {
        return label(text).withStyle( it -> it.componentFont( f -> f.size(11).weight(2f).color(SOFT_INK) ) );
    }

    // ── Text ────────────────────────────────────────────────────────────────────

    /**
     *  Puts the whole workshop into a scroll pane which never scrolls sideways, and which
     *  only scrolls up and down when the window is too short for it. Otherwise the content
     *  is stretched to the height of the window, so the grids get all the room there is,
     *  while a short or narrow window can still reach every card and every grid.
     */
    private static UIForAnySwing<?, ?> scrolling( UIForAnySwing<?, ?> page ) {
        return
            scrollPane( conf -> conf
                .fitWidth(true)
                .fitHeight(conf.viewport().getHeight() > conf.view().getPreferredSize().height)
            )
            .withHorizontalScrollBarPolicy(UI.Active.NEVER)
            .withVerticalScrollIncrement(24)
            .withStyle( it -> it.borderWidth(0).backgroundColor(PAGE) )
            .add(page);
    }

    private static UIForTextArea<UI.TextArea> smallPrint( String text ) {
        return prose(text, 10, SOFT_INK);
    }

    private static UIForTextArea<UI.TextArea> prose( String text ) {
        return prose(text, 11, SOFT_INK);
    }

    /**
     *  A read-only, line wrapping text area for a text which changes: it follows the given
     *  view through a plain {@link javax.swing.JTextArea#setText(String)} on the UI thread.
     *  <p>
     *  It is not bound through {@code textArea(Val<String>)} on purpose. That binding sets the
     *  text while the document listeners are detached, including the one through which the
     *  text area rebuilds the views of its wrapped lines. The text is correct afterwards, but
     *  a text with a different number of paragraphs is painted with the lines of the previous
     *  one, as a garbled mix of both.
     */
    private static UIForTextArea<UI.TextArea> prose( Viewable<String> text ) {
        return prose(text.get(), 11, SOFT_INK).onView(text, it -> it.get().setText(text.get()));
    }

    private static UIForTextArea<UI.TextArea> prose( String text, int fontSize, Color color ) {
        return
            UI.of(styledTextArea(text))
            .isEditableIf(false)
            .isFocusableIf(false)
            .peek( area -> {
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                area.addComponentListener(new RewrapOnResize());
            })
            .withMinSize(0, 0)
            .withStyle( it -> it
                .backgroundColor(UI.Color.TRANSPARENT)
                .borderWidth(0)
                .componentFont( f -> f.size(fontSize).color(color) )
            );
    }

    /**
     *  Lays a line wrapping text area out once more after its width changed, so that the
     *  grid around it gets to see how tall the text really is.
     *  <p>
     *  A responsive grid asks its cards for their preferred height before it gives them
     *  their new width, and only a nested grid can answer for a width it does not have yet.
     *  A wrapping text area answers with the height of the lines it had at its old width,
     *  so after a large resize, like maximising the window, its card would stay too short
     *  and cut the text off. Once the text area has its new width, it knows its real height,
     *  and one more layout pass hands that to the grid. The pass only happens when the width
     *  changed, so a text area which cannot get its preferred height never keeps asking.
     */
    private static final class RewrapOnResize extends java.awt.event.ComponentAdapter
    {
        private int lastWidth = -1;

        @Override
        public void componentResized( java.awt.event.ComponentEvent event ) {
            java.awt.Component area = event.getComponent();
            if ( area.getWidth() == lastWidth )
                return;
            lastWidth = area.getWidth();
            if ( area.getPreferredSize().height != area.getHeight() )
                ((javax.swing.JComponent) area).revalidate();
        }
    }

    /** @see #prose(Viewable) — a code block which changes is fed in the same way. */
    private static UIForTextArea<UI.TextArea> codeBlock( Viewable<String> code ) {
        return codeBlock(code.get()).onView(code, it -> it.get().setText(code.get()));
    }

    private static UI.TextArea styledTextArea( String text ) {
        UI.TextArea area = new UI.TextArea();
        area.setText(text);
        return area;
    }

    private static UIForTextArea<UI.TextArea> codeBlock( String code ) {
        return
            UI.of(styledTextArea(code))
            .isEditableIf(false)
            .isFocusableIf(false)
            .withMinSize(0, 0)
            .withStyle( it -> it
                .backgroundColor(CODE)
                .borderRadius(12)
                .padding(12, 14, 12, 14)
                .componentFont( f -> f.family("Monospaced").size(11).color(CODE_INK) )
            );
    }

    public static void main( String... args ) {
        FlatLightLaf.setup();
        Var<TileWorkshopViewModel> vm = Var.of(TileWorkshopViewModel.initial());
        UI.show("The Tile Workshop — SwingTree grid layouts", f -> new TileWorkshopView(vm));
        EventProcessor.DECOUPLED.join();
    }
}
