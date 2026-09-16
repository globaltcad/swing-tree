package examples.grids;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;
import swingtree.api.Layout;

import javax.swing.JComponent;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.util.Objects;

/**
 *  A {@link Layout} which installs a plain {@link GridLayout} of the JDK, so that the
 *  {@link TileWorkshopView} can show it next to a {@code UniformGridLayout} with the same
 *  numbers of rows and columns.
 *  <p>
 *  {@code Layout.grid(..)} installs a {@code UniformGridLayout}, so the JDK layout needs a
 *  {@link Layout} of its own. A {@code GridLayout} does not follow the UI scale factor, so
 *  this one scales the gaps by hand, which keeps both grids comparable on a high resolution
 *  screen. Installing it is idempotent: SwingTree installs a bound layout on every style
 *  pass, and an unchanged {@link GridLayout} is left as it is.
 */
@Immutable
final class AwtGridLayout implements Layout
{
    private final int rows;
    private final int columns;
    private final int gap;

    AwtGridLayout( int rows, int columns, int gap ) {
        this.rows    = rows;
        this.columns = columns;
        this.gap     = gap;
    }

    @Override
    public void installFor( JComponent component ) {
        int scaledGap = UI.scale(gap);
        LayoutManager current = component.getLayout();
        if ( current instanceof GridLayout ) {
            GridLayout grid = (GridLayout) current;
            if ( grid.getRows() == rows && grid.getColumns() == columns && grid.getHgap() == scaledGap && grid.getVgap() == scaledGap )
                return;
        }
        component.setLayout(new GridLayout(rows, columns, scaledGap, scaledGap));
        component.revalidate();
    }

    @Override
    public boolean equals( Object other ) {
        if ( this == other ) return true;
        if ( !(other instanceof AwtGridLayout) ) return false;
        AwtGridLayout that = (AwtGridLayout) other;
        return rows == that.rows && columns == that.columns && gap == that.gap;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rows, columns, gap);
    }

    @Override
    public String toString() {
        return "new java.awt.GridLayout(" + rows + ", " + columns + ", " + gap + ", " + gap + ")";
    }
}
