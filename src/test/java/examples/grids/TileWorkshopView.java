package examples.grids;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.UIForTextArea;
import swingtree.api.Layout;
import swingtree.api.model.SliderTicks;
import swingtree.layout.UniformGridLayout.CollapseEmpty;
import swingtree.layout.UniformGridLayout.Mode;
import swingtree.threading.EventProcessor;

import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
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
 *      <li><b>Workbench</b> — every setting chosen by hand, bound through a {@code Val<Layout>},
 *          optionally next to a {@link GridLayout} with the same counts.</li>
 *  </ol>
 *  The numbers on the tiles are the order in which they were added, which makes the
 *  order in which a grid fills its cells visible. Run {@link #main(String...)} to open it.
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

    private static final SliderTicks<Integer> TILE_TICKS =
            SliderTicks.of(Integer.class).withMajorSpacing(4).withMinorTicksBetween(3).withLabelsAtMajorTicks();

    private static final SliderTicks<Integer> GAP_TICKS =
            SliderTicks.of(Integer.class).withMajorSpacing(6).withMinorTicksBetween(5).withLabelsAtMajorTicks();

    private static final String INTRODUCTION =
            "Every tab of this workshop lays out numbered tiles with a UniformGridLayout, the layout manager " +
            "behind withGridLayout(..) and Layout.grid(..). Drag the slider in the tool bar of a tab to add or " +
            "remove tiles, read what the tab is about, and check that its grid behaves exactly as described. " +
            "The tiles are numbered in the order they were added, so you can follow how a grid fills its cells: " +
            "row by row, starting at the top. In the last tab, Workbench, you choose every setting yourself.";

    public TileWorkshopView( Var<TileWorkshopViewModel> vm ) {
        UI.of(this).withLayout("fill, ins 0")
        .withPrefSize(1180, 940)
        .withStyle( it -> it.backgroundColor(PAGE) )
        .add(GROW.and("wmin 0, hmin 0"), scrolling(
            box("fill, wrap 1, ins 18 22 18 22, gap 14", "[grow, fill]", "[][grow, fill]")
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

    private static UIForAnySwing<?, ?> header() {
        return box("fillx, wrap 1, ins 0, gap 2", "[grow, fill]")
            .add("wmin 0",
                label("The Tile Workshop")
                .withStyle( it -> it.componentFont( f -> f.size(28).weight(2f).color(INK) ) )
            )
            .add("wmin 0",
                label("An interactive guide to the grid layout of SwingTree")
                .withStyle( it -> it.componentFont( f -> f.size(15).color(ACCENT) ) )
            )
            .add("wmin 0, gaptop 8", prose(INTRODUCTION));
    }

    // ── The scenario tabs ───────────────────────────────────────────────────────

    private static UIForAnySwing<?, ?> declaredColumnsPage( Var<Tuple<Tile>> tiles ) {
        return page(
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
            "UI.panel().withLayout(new GridLayout(2, 5, 6, 6))",
            panel("fill, ins 0, gap 12", "[grow, fill][grow, fill]", "[grow, fill]")
            .withStyle( it -> it.backgroundColor(PAGE) )
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
        return page(
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
            "    UniformGridLayout.Mode.WRAP_AFTER_COLUMNS,\n" +
            "    UniformGridLayout.CollapseEmpty.ROWS_AND_COLUMNS,\n" +
            "    3, 4, 6, 6\n" +
            ")",
            gridFrame("withGridLayout(3, 4, 6, 6)",
                box().withGridLayout(3, 4, 6, 6).addAll(tiles, TileWorkshopView::tile)
            )
        );
    }

    private static UIForAnySwing<?, ?> collapseChoicesPage( Var<Tuple<Tile>> tiles ) {
        return page(
            tiles,
            "Choose which empty rows and columns are left out",
            "CollapseEmpty decides what happens to the rows and columns no tile occupies. The four grids " +
            "below are all declared with 2 rows and 4 columns, and differ only in that setting. With 3 tiles, " +
            "NONE keeps the empty column and the empty row, COLUMNS leaves out the empty column, ROWS leaves " +
            "out the empty row, and ROWS_AND_COLUMNS, the default, leaves out both. From 5 tiles on, the " +
            "tiles reach every row and every column, so all four grids look alike.",
            "Try it: compare 1, 3 and 4 tiles. The cells of NONE keep their size until the grid is full.",
            "UI.panel().withGridLayout(\n" +
            "    UniformGridLayout.Mode.WRAP_AFTER_COLUMNS,\n" +
            "    UniformGridLayout.CollapseEmpty.NONE,   // or COLUMNS, ROWS, ROWS_AND_COLUMNS\n" +
            "    2, 4, 6, 6\n" +
            ")",
            panel("fill, ins 0, gap 12, wrap 2", "[grow, fill][grow, fill]", "[grow, fill][grow, fill]")
            .withStyle( it -> it.backgroundColor(PAGE) )
            .apply( stage -> {
                for ( CollapseEmpty collapseEmpty : CollapseEmpty.values() )
                    stage.add(GROW.and("wmin 0, hmin 0"),
                        gridFrame("CollapseEmpty." + collapseEmpty.name(),
                            box()
                            .withGridLayout(Mode.WRAP_AFTER_COLUMNS, collapseEmpty, 2, 4, 6, 6)
                            .addAll(tiles, TileWorkshopView::tile)
                        )
                    );
            })
        );
    }

    private static UIForAnySwing<?, ?> likeGridLayoutPage( Var<Tuple<Tile>> tiles ) {
        return page(
            tiles,
            "Keep the arrangement of java.awt.GridLayout",
            "If a screen of yours relies on how java.awt.GridLayout arranges its components, choose the mode " +
            "SPREAD_OVER_ROWS and collapse nothing. The grid then spreads its tiles over the declared rows and " +
            "ignores the declared columns, exactly like the JDK does, and keeps the rows its tiles do not reach. " +
            "Both grids below are declared with 3 rows and 5 columns, and they look identical for every number " +
            "of tiles. The JDK grid does not follow the UI scale factor, so this app scales its gaps by hand.",
            "Try it: slide through all numbers of tiles. With 2 tiles, both grids keep an empty third row, and with 13 tiles, both use 5 columns.",
            "UI.panel().withGridLayout(\n" +
            "    UniformGridLayout.Mode.SPREAD_OVER_ROWS,\n" +
            "    UniformGridLayout.CollapseEmpty.NONE,\n" +
            "    3, 5, 6, 6\n" +
            ")\n" +
            "\n" +
            "// lays out its children like\n" +
            "UI.panel().withLayout(new GridLayout(3, 5, 6, 6))",
            panel("fill, ins 0, gap 12", "[grow, fill][grow, fill]", "[grow, fill]")
            .withStyle( it -> it.backgroundColor(PAGE) )
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
        return page(
            tiles,
            "A count of 0 means as many as the tiles need",
            "Leave one of the two counts open with a 0. withGridLayout(0, 3) starts a new row after every " +
            "3 tiles and has as many rows as the tiles need. withGridLayout(2, 0) spreads the tiles over 2 " +
            "rows, in as many columns as they need, so 5 tiles get 3 columns. With a single tile it only has " +
            "1 row, because the empty second row is left out. Only one of the two counts may be 0.",
            "Try it: add tiles, and watch the left grid grow downwards and the right grid grow sideways.",
            "UI.panel().withGridLayout(0, 3, 6, 6)   // 3 columns, as many rows as needed\n" +
            "UI.panel().withGridLayout(2, 0, 6, 6)   // 2 rows, as many columns as needed",
            panel("fill, ins 0, gap 12", "[grow, fill][grow, fill]", "[grow, fill]")
            .withStyle( it -> it.backgroundColor(PAGE) )
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
        Var<Integer>        rows          = bench.zoomTo(Workbench::rows,                   Workbench::withDeclaredRows);
        Var<Integer>        columns       = bench.zoomTo(Workbench::columns,                Workbench::withDeclaredColumns);
        Var<Integer>        gap           = bench.zoomTo(Workbench::gap,                    Workbench::withGap);
        Var<Boolean>        rightToLeft   = bench.zoomTo(Workbench::rightToLeft,            Workbench::withRightToLeft);
        Var<Boolean>        compared      = bench.zoomTo(Workbench::comparedWithGridLayout, Workbench::withComparedWithGridLayout);

        Val<Layout> uniformLayout = bench.viewAs(Layout.class, b ->
                                        Layout.grid(b.mode(), b.collapseEmpty(), b.rows(), b.columns(), b.gap(), b.gap())
                                    );
        Val<Layout> awtLayout     = bench.viewAs(Layout.class, b -> new AwtGridLayout(b.rows(), b.columns(), b.gap()));
        Val<String> uniformTitle  = bench.viewAsString( b ->
                                        "Layout.grid(" + b.mode().name() + ", " + b.collapseEmpty().name() + ", " +
                                        b.rows() + ", " + b.columns() + ", " + b.gap() + ", " + b.gap() + ")"
                                    );
        Val<String> awtTitle      = awtLayout.viewAsString(Object::toString);

        return
            panel("fill, wrap 1, ins 16, gap 12", "[grow, fill]", "[][][][grow, fill]")
            .withStyle( it -> it.backgroundColor(PAGE) )
            .add("wmin 0", tileToolBar(tileCount))
            .add("wmin 0",
                toolBar()
                .peek( bar -> bar.setFloatable(false) )
                .withStyle( it -> it
                    .backgroundColor(CARD)
                    .border(1, HAIRLINE)
                    .borderRadius(10)
                    .padding(6, 12, 6, 12)
                )
                .add(toolLabel("Mode"))
                .add(comboBox(mode).withMaxWidth(240).withTooltip("How the grid is built from the numbers of rows and columns"))
                .add(toolLabel("CollapseEmpty"))
                .add(comboBox(collapseEmpty).withMaxWidth(240).withTooltip("Which rows and columns without a tile are left out"))
                .add(toolLabel("Rows"))
                .add(
                    spinner(new SpinnerNumberModel(3, 0, 12, 1)).withValue(rows).withMaxWidth(90)
                    .withTooltip("The declared number of rows, where 0 means as many as the tiles need")
                )
                .add(toolLabel("Columns"))
                .add(
                    spinner(new SpinnerNumberModel(4, 0, 12, 1)).withValue(columns).withMaxWidth(90)
                    .withTooltip("The declared number of columns, where 0 means as many as the tiles need")
                )
            )
            .add("wmin 0",
                panel("fill, ins 14 16 14 16, gap 18 6", "[grow 50, fill][grow 50, fill]", "[][top]")
                .withStyle( it -> it.backgroundColor(CARD).border(1, HAIRLINE).borderRadius(12) )
                .add("span 2, wmin 0, wrap", heading("Configure every setting yourself"))
                .add("wmin 0",
                    box("fillx, wrap 1, ins 0, gap 8", "[grow, fill]")
                    .add("wmin 0", prose(bench.viewAsString(Workbench::explanation)))
                    .add("wmin 0, gaptop 4",
                        box("fillx, ins 0, gap 10 6", "[][grow, fill]")
                        .add(fieldLabel("Gaps"))
                        .add("wmin 0, wrap", slider(UI.Axis.HORIZONTAL, 0, 24, gap).withTicks(GAP_TICKS))
                        .add(fieldLabel("Layout"))
                        .add("wmin 0, wrap",
                            box("fillx, ins 0, gap 6", "[grow, fill][grow, fill]")
                            .add("wmin 0", checkBox("Right to left", rightToLeft))
                            .add("wmin 0", checkBox("Compare with java.awt.GridLayout", compared))
                        )
                        .add(fieldLabel("Presets"))
                        .add("wmin 0",
                            box("fillx, ins 0, gap 6", "[grow, fill][grow, fill]")
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
                        )
                    )
                    .add("wmin 0", smallPrint("Rows and columns can't both be 0: setting one of them to 0 while the other one is 0 sets the other one to 1."))
                )
                .add("wmin 0", codeBlock(bench.viewAsString(Workbench::code)))
            )
            .add(GROW.and(PUSH).and("wmin 0, hmin 260"),
                panel("fill, ins 0, gap 12, hidemode 3")
                .withStyle( it -> it.backgroundColor(PAGE) )
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

    // ── Building blocks ─────────────────────────────────────────────────────────

    private static UIForAnySwing<?, ?> page(
        Var<Tuple<Tile>>    tiles,
        String              heading,
        String              explanation,
        String              tryThis,
        String              code,
        UIForAnySwing<?, ?> stage
    ) {
        Var<Integer> tileCount = tiles.zoomTo(Tuple::size, Tile::resized);
        return
            panel("fill, wrap 1, ins 16, gap 12", "[grow, fill]", "[][][grow, fill]")
            .withStyle( it -> it.backgroundColor(PAGE) )
            .add("wmin 0", tileToolBar(tileCount))
            .add("wmin 0",
                panel("fill, ins 14 16 14 16, gap 18 6", "[grow 55, fill][grow 45, fill]", "[][top]")
                .withStyle( it -> it.backgroundColor(CARD).border(1, HAIRLINE).borderRadius(12) )
                .add("span 2, wmin 0, wrap", heading(heading))
                .add("wmin 0",
                    box("fillx, wrap 1, ins 0, gap 8", "[grow, fill]")
                    .add("wmin 0", prose(explanation))
                    .add("wmin 0", prose(tryThis, 13, ACCENT))
                )
                .add("wmin 0", codeBlock(code))
            )
            .add(GROW.and(PUSH).and("wmin 0, hmin 260"), stage);
    }

    /**
     *  Puts the whole workshop into a scroll pane which never scrolls sideways, and which
     *  only scrolls up and down when the window is too short for it. Otherwise the content
     *  is stretched to the height of the window, so the grids get all the room there is,
     *  while a short or narrow window can still reach every tool bar and every grid.
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

    private static UIForAnySwing<?, ?> tileToolBar( Var<Integer> tileCount ) {
        return
            toolBar()
            .peek( bar -> bar.setFloatable(false) )
            .withStyle( it -> it
                .backgroundColor(CARD)
                .border(1, HAIRLINE)
                .borderRadius(10)
                .padding(4, 12, 4, 12)
            )
            .add(
                label("Tiles")
                .withStyle( it -> it.padding(0, 0, 0, 10).componentFont( f -> f.size(14).weight(2f).color(INK) ) )
            )
            .add(
                button("−")
                .withTooltip("Remove the last tile")
                .onClick( it -> tileCount.update( n -> Math.max(0, n - 1) ) )
            )
            .add(
                slider(UI.Axis.HORIZONTAL, 0, MAX_TILES, tileCount)
                .withTicks(TILE_TICKS)
                .withTooltip("Drag to add or remove tiles")
                .withMinSize(0, 0)
            )
            .add(
                button("+")
                .withTooltip("Add a tile")
                .onClick( it -> tileCount.update( n -> Math.min(MAX_TILES, n + 1) ) )
            )
            .add(
                label(tileCount.viewAsString( n -> n == 1 ? "1 tile" : n + " tiles" ))
                .withPrefWidth(70)
                .withStyle( it -> it.padding(0, 10, 0, 0).componentFont( f -> f.size(14).color(SOFT_INK) ) )
            );
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
                .withStyle( it -> it.componentFont( f -> f.family("Monospaced").size(12).color(SOFT_INK) ) )
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
                .borderRadius(9)
            )
            .add("wmin 0",
                label(String.valueOf(tile.number()))
                .withStyle( it -> it.componentFont( f -> f.size(22).weight(2f).color(text) ) )
            )
            .add("wmin 0",
                label(tile.glaze().title())
                .withStyle( it -> it.componentFont( f -> f.size(10).color(text) ) )
            );
    }

    private static UIForAnySwing<?, ?> heading( String text ) {
        return label(text).withStyle( it -> it.componentFont( f -> f.size(18).weight(2f).color(INK) ) );
    }

    private static UIForAnySwing<?, ?> toolLabel( String text ) {
        return label(text).withStyle( it -> it.padding(0, 12, 0, 6).componentFont( f -> f.size(13).color(SOFT_INK) ) );
    }

    private static UIForAnySwing<?, ?> fieldLabel( String text ) {
        return label(text).withStyle( it -> it.componentFont( f -> f.size(13).color(SOFT_INK) ) );
    }

    private static UIForTextArea<UI.TextArea> smallPrint( String text ) {
        return prose(text, 11, SOFT_INK);
    }

    private static UIForTextArea<UI.TextArea> prose( String text ) {
        return prose(text, 13, SOFT_INK);
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
        return prose(text.get(), 13, SOFT_INK).onView(text, it -> it.get().setText(text.get()));
    }

    private static UIForTextArea<UI.TextArea> prose( String text, int fontSize, Color color ) {
        return
            UI.of(styledTextArea(text))
            .isEditableIf(false)
            .isFocusableIf(false)
            .peek( area -> { area.setLineWrap(true); area.setWrapStyleWord(true); } )
            .withMinSize(0, 0)
            .withStyle( it -> it
                .backgroundColor(UI.Color.TRANSPARENT)
                .borderWidth(0)
                .componentFont( f -> f.size(fontSize).color(color) )
            );
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
                .borderRadius(8)
                .padding(10, 14, 10, 14)
                .componentFont( f -> f.family("Monospaced").size(12).color(CODE_INK) )
            );
    }

    public static void main( String... args ) {
        FlatLightLaf.setup();
        Var<TileWorkshopViewModel> vm = Var.of(TileWorkshopViewModel.initial());
        UI.show("The Tile Workshop — SwingTree grid layouts", f -> new TileWorkshopView(vm));
        EventProcessor.DECOUPLED.join();
    }
}
