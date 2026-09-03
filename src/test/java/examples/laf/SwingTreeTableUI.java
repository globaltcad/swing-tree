package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTableUI;
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
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeTableUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        JTable table = (JTable) c;
        if ( SwingTreeLookAndFeel.drawsOwnChrome() ) {
            table.setShowGrid(false);
            table.setIntercellSpacing(new Dimension(0, 0));
            table.setRowHeight(UI.scale(SwingTreeLookAndFeel.symbols().tableRowHeight()));
        } else {
            // The table's constructor sets a fixed 16 pixels, which is shorter than the font
            // this look and feel installs once the UI scale factor is above one.
            table.setRowHeight(rowHeightFor(table));
        }
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> {
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

    /** Fills a band behind each selected row, for the reason {@link SwingTreeListUI} paints its
     *  own: one renderer instance cannot carry a colour that differs from row to row. */
    private static void paintSelectionBands( Graphics2D g, JTable table ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            return; // Swing's own renderer is carrying the selection colour
        int[] selected = table.getSelectedRows();
        if ( selected.length == 0 )
            return;
        g.setColor(SwingTreeLookAndFeel.palette().accentSoft());
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
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}
