package examples.grids;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.Tuple;
import swingtree.layout.UniformGridLayout.CollapseEmpty;
import swingtree.layout.UniformGridLayout.Mode;
import swingtree.layout.UniformGridLayout.OverflowGrowth;

/**
 *  The state of the last tab of the {@link TileWorkshopView}, in which every setting of
 *  the grid can be chosen by hand: its mode, which empty rows and columns it leaves out,
 *  how it grows when it holds more tiles than cells, the declared numbers of rows and
 *  columns, the gaps and the component orientation.
 *  <p>
 *  Besides the withers, it offers the texts the tab shows about the current configuration,
 *  so that they can be read and checked without a user interface.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode @ToString
public final class Workbench
{
    private final Tuple<Tile>    tiles;
    private final Mode           mode;
    private final CollapseEmpty  collapseEmpty;
    private final OverflowGrowth overflowGrowth;
    private final int            rows;
    private final int            columns;
    private final int            gap;
    private final boolean        rightToLeft;
    private final boolean        comparedWithGridLayout;

    public static Workbench initial() {
        return new Workbench(
                    Tile.firstTiles(7),
                    Mode.WRAP_AFTER_COLUMNS,
                    CollapseEmpty.ROWS_AND_COLUMNS,
                    OverflowGrowth.ADD_ROWS,
                    3, 4, 6,
                    false,
                    false
                );
    }

    /**
     *  Whether the declared grid has fewer cells than there are tiles, so that it has to grow.
     *  A grid has a cell count to exceed in either mode, as long as it declares both counts;
     *  a count of 0 grows that axis as far as the tiles need and never overflows.
     */
    public boolean overflows() {
        return rows > 0 && columns > 0 && tiles.size() > rows * columns;
    }

    public int tileCount() { return tiles.size(); }

    public Workbench withTileCount( int count ) { return withTiles(Tile.resized(tiles, count)); }

    /**
     *  Sets the declared number of rows. A grid may not leave both counts open, so when
     *  the rows become 0 while the columns are 0 as well, the columns step up to 1.
     */
    public Workbench withDeclaredRows( int newRows ) {
        int newColumns = ( newRows == 0 && columns == 0 ) ? 1 : columns;
        return withRows(newRows).withColumns(newColumns);
    }

    /**
     *  Sets the declared number of columns. A grid may not leave both counts open, so when
     *  the columns become 0 while the rows are 0 as well, the rows step up to 1.
     */
    public Workbench withDeclaredColumns( int newColumns ) {
        int newRows = ( newColumns == 0 && rows == 0 ) ? 1 : rows;
        return withColumns(newColumns).withRows(newRows);
    }

    /** The settings a grid of {@code withGridLayout(rows, cols)} has by default. */
    public Workbench withSwingTreeDefaults() {
        return withMode(Mode.WRAP_AFTER_COLUMNS)
               .withCollapseEmpty(CollapseEmpty.ROWS_AND_COLUMNS)
               .withOverflowGrowth(OverflowGrowth.ADD_ROWS);
    }

    /**
     *  A grid of 1 row and 4 columns holding 16 tiles, which is 4 times its 4 cells, so that
     *  {@link OverflowGrowth#ADD_ROWS_AND_COLUMNS} can meet the declared ratio exactly and lays
     *  the tiles out in 2 rows of 8 columns. The other two growth settings give 4 rows of 4
     *  columns and a single row of 16 columns, so switching between them shows what each one does.
     */
    public Workbench withOverflowShowcase() {
        return withMode(Mode.WRAP_AFTER_COLUMNS)
               .withCollapseEmpty(CollapseEmpty.NONE)
               .withRows(1).withColumns(4)
               .withTileCount(16);
    }

    /**
     *  The settings which lay out components exactly like a {@code java.awt.GridLayout}: it spreads
     *  its components over its rows, keeps every empty row, and makes room by widening itself, which
     *  is what {@link OverflowGrowth#ADD_COLUMNS} does.
     */
    public Workbench withGridLayoutSettings() {
        return withMode(Mode.SPREAD_OVER_ROWS)
               .withCollapseEmpty(CollapseEmpty.NONE)
               .withOverflowGrowth(OverflowGrowth.ADD_COLUMNS);
    }

    private boolean collapsesRows() {
        return collapseEmpty == CollapseEmpty.ROWS || collapseEmpty == CollapseEmpty.ROWS_AND_COLUMNS;
    }

    private boolean collapsesColumns() {
        return collapseEmpty == CollapseEmpty.COLUMNS || collapseEmpty == CollapseEmpty.ROWS_AND_COLUMNS;
    }

    /** Describes in plain words what the current settings do with the tiles. */
    public String explanation() {
        StringBuilder text = new StringBuilder();
        if ( mode == Mode.WRAP_AFTER_COLUMNS ) {
            if ( columns > 0 ) {
                text.append("WRAP_AFTER_COLUMNS starts a new row after every ").append(tiles(columns)).append(".");
                if ( rows > 0 && collapsesRows() )
                    text.append(" The ").append(rows).append(" declared rows make no difference, because empty rows are left out.");
                else if ( rows > 0 )
                    text.append(" The grid keeps at least ").append(rows).append(rows == 1 ? " row." : " rows.");
            } else {
                text.append("With 0 columns, WRAP_AFTER_COLUMNS spreads the tiles over ").append(rows)
                    .append(rows == 1 ? " row" : " rows").append(", in as many columns as they need.");
            }
        } else {
            if ( rows > 0 ) {
                text.append("SPREAD_OVER_ROWS spreads the tiles over ").append(rows).append(rows == 1 ? " row" : " rows")
                    .append(", in as many columns as they need");
                text.append( columns > 0
                                ? ". The " + columns + " declared columns do not shape it, but they still count its cells."
                                : "." );
                if ( collapsesColumns() )
                    text.append(" Spreading never leaves a column empty, so collapsing columns changes nothing in this mode.");
            } else {
                text.append("With 0 rows, SPREAD_OVER_ROWS starts a new row after every ").append(tiles(columns)).append(".");
            }
        }
        text.append("\n\n");
        switch ( collapseEmpty ) {
            case NONE:
                text.append("CollapseEmpty.NONE keeps the rows and columns no tile occupies. They stay empty, but take up their space.");
                break;
            case COLUMNS:
                text.append("CollapseEmpty.COLUMNS leaves out the columns no tile occupies, and keeps the empty rows.");
                break;
            case ROWS:
                text.append("CollapseEmpty.ROWS leaves out the rows no tile occupies, and keeps the empty columns.");
                break;
            default:
                text.append("CollapseEmpty.ROWS_AND_COLUMNS leaves out every row and column no tile occupies.");
        }
        if ( rightToLeft )
            text.append(" Every row is filled from right to left.");
        text.append("\n\n").append(growthExplanation());
        return text.toString();
    }

    /** Describes in plain words what the growth setting does with the tiles right now. */
    private String growthExplanation() {
        if ( !overflows() ) {
            StringBuilder text = new StringBuilder();
            text.append("OverflowGrowth.").append(overflowGrowth.name()).append(" changes nothing right now, because ");
            if ( rows == 0 || columns == 0 )
                text.append("a count of 0 already grows as far as the tiles need, so the grid never holds more tiles than cells.");
            else
                text.append("the ").append(cells(rows * columns)).append(" of the declared grid still hold all ").append(tiles(tiles.size())).append(".");
            return text.toString();
        }
        StringBuilder text = new StringBuilder();
        text.append("There are more tiles than the ").append(cells(rows * columns))
            .append(" of the declared grid, so the mode has nothing left to say and the growth policy decides. ");
        switch ( overflowGrowth ) {
            case ADD_COLUMNS:
                text.append("OverflowGrowth.ADD_COLUMNS keeps the ").append(rows)
                    .append(rows == 1 ? " declared row" : " declared rows").append(" and adds columns to the right.");
                break;
            case ADD_ROWS_AND_COLUMNS:
                text.append("OverflowGrowth.ADD_ROWS_AND_COLUMNS adds a row or a column at a time, each time the one which keeps the ratio of rows to columns closest to the declared ")
                    .append(rows).append(" : ").append(columns)
                    .append(". The grid keeps its shape, and because the panel does not change size, so does every tile.");
                break;
            default:
                text.append("OverflowGrowth.ADD_ROWS keeps the ").append(columns)
                    .append(columns == 1 ? " declared column" : " declared columns")
                    .append(" and adds rows below, so a tile keeps its cell however many tiles follow it.");
        }
        return text.toString();
    }

    /** The SwingTree code which binds a panel to a grid with the current settings. */
    public String code() {
        return
            "Var<Layout> layout = Var.of(Layout.class,\n" +
            "    Layout.grid(\n" +
            "        Mode." + mode.name() + ",\n" +
            "        CollapseEmpty." + collapseEmpty.name() + ",\n" +
            "        OverflowGrowth." + overflowGrowth.name() + ",\n" +
            "        " + rows + ", " + columns + ",  // rows, columns\n" +
            "        " + gap + ", " + gap + "   // gaps\n" +
            "    )\n" +
            ");\n" +
            "UI.panel(layout)\n" +
            ( rightToLeft ? ".withStyle(it -> it.orientation(\n    UI.ComponentOrientation.RIGHT_TO_LEFT\n))\n" : "" ) +
            ".add(...);  // " + tiles(tiles.size()) + "\n" +
            "// all three enums are nested in UniformGridLayout";
    }

    private static String tiles( int count ) {
        return count == 1 ? "1 tile" : count + " tiles";
    }

    private static String cells( int count ) {
        return count == 1 ? "1 cell" : count + " cells";
    }
}
