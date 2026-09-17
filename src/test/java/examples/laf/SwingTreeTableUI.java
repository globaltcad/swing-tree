package examples.laf;

import org.jspecify.annotations.Nullable;
import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTableUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *  The {@link JTable} UI delegate. Grid lines are off unless the table asks for them back with
 *  {@link JTable#setShowGrid(boolean)}.
 */
public final class SwingTreeTableUI
        extends    BasicTableUI
        implements SwingTreeStyledComponentUI<JTable>
{
    private final SwingTreeLookAndFeel.Theme _theme;
    private final @Nullable Color            _stripe;

    SwingTreeTableUI( SwingTreeLookAndFeel.Theme theme, @Nullable Color stripe ) {
        _theme  = theme;
        _stripe = stripe;
    }

    public static ComponentUI createUI( JComponent c ) {
        return new SwingTreeTableUI(SwingTreeLookAndFeel.installedTheme(), UIManager.getColor("Table.alternateRowColor"));
    }

    /** The grid lines and cell spacing the table had before this delegate removed them, given back
     *  when it is uninstalled; {@code null} where the symbol set draws no chrome and nothing was
     *  removed. Swing installs neither from its defaults, so no later look and feel would. */
    private @Nullable Grid _displacedGrid = null;

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        JTable table = (JTable) c;
        if ( _theme.symbols().drawsItsOwnChrome() ) {
            _displacedGrid = new Grid(table);
            table.setShowGrid(false);
            table.setIntercellSpacing(new Dimension(0, 0));
        }
        _theme.installStyleOn(c);
        // Installed rather than set, so that a row height the application chose stands. Without a
        // symbol set's own height, the table's constructor leaves a fixed 16 pixels, which is
        // shorter than the font this look and feel installs once the UI scale factor is above one.
        LookAndFeel.installProperty(table, "rowHeight", _theme.symbols().drawsItsOwnChrome()
                                                        ? UI.scale(_theme.symbols().tableRowHeight())
                                                        : rowHeightFor(table));
    }

    @Override
    public void uninstallUI( JComponent c ) {
        super.uninstallUI(c);
        if ( _displacedGrid != null )
            _displacedGrid.restoreOn((JTable) c);
        _displacedGrid = null;
    }

    private static final class Grid
    {
        private final boolean   _horizontalLines;
        private final boolean   _verticalLines;
        private final Dimension _intercellSpacing;

        Grid( JTable table ) {
            _horizontalLines  = table.getShowHorizontalLines();
            _verticalLines    = table.getShowVerticalLines();
            _intercellSpacing = table.getIntercellSpacing();
        }

        void restoreOn( JTable table ) {
            table.setShowHorizontalLines(_horizontalLines);
            table.setShowVerticalLines(_verticalLines);
            table.setIntercellSpacing(_intercellSpacing);
        }
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> {
            paintStripes(g2, (JTable) c);
            paintSelectionBands(g2, (JTable) c);
            super.paint(g2, c);
        });
    }

    /** @return a row tall enough for the table's own font, with a little air above and below. */
    private static int rowHeightFor( JTable table ) {
        java.awt.Font font = table.getFont();
        int size = font == null ? UI.scale(13) : Math.round(font.getSize2D());
        return Math.round(size * 1.9f);
    }

    /**
     *  Tints every second row, so that a wide row can be followed across the table.
     *  <p>
     *  Swing has no notion of this: {@code Table.alternateRowColor} is read by whichever renderer
     *  a look and feel installs, and a table with a renderer of its own therefore loses the
     *  stripes. Filling them under the renderers puts them back whatever renders the cells, for
     *  the reason {@link #paintSelectionBands} fills its bands there.
     */
    private void paintStripes( Graphics2D g, JTable table ) {
        if ( !_theme.symbols().drawsItsOwnChrome() )
            return;
        Color stripe = _stripe;
        if ( stripe == null || stripe.equals(table.getBackground()) )
            return;
        Rectangle clip = g.getClipBounds();
        g.setColor(stripe);
        for ( int row = 1; row < table.getRowCount(); row += 2 ) {
            Rectangle band = table.getCellRect(row, 0, true);
            if ( clip == null || (band.y + band.height >= clip.y && band.y <= clip.y + clip.height) )
                g.fillRect(0, band.y, table.getWidth(), band.height);
        }
    }

    /** Fills a band behind each selected row, for the reason {@link SwingTreeListUI} paints its
     *  own: one renderer instance cannot carry a colour that differs from row to row. */
    private void paintSelectionBands( Graphics2D g, JTable table ) {
        if ( !_theme.symbols().drawsItsOwnChrome() )
            return; // Swing's own renderer is carrying the selection colour
        int[] selected = table.getSelectedRows();
        if ( selected.length == 0 )
            return;
        g.setColor(_theme.palette().accentSoft());
        for ( int row : selected ) {
            Rectangle band = table.getCellRect(row, 0, true);
            g.fillRect(0, band.y, table.getWidth(), band.height);
        }
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JTable> style( ComponentStyleDelegate<JTable> it ) throws Exception {
        return _theme.applyStyle(it);
    }
}
