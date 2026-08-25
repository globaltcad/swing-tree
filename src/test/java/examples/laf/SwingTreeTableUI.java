package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
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
 *  The {@link JTable} UI delegate. Gridlines are off by default for a cleaner look - re-enable
 *  them per instance with {@link JTable#setShowGrid(boolean)} - and the band behind a selected
 *  row is filled here rather than by the cell renderer, for the reason spelled out in
 *  {@link SwingTreeListUI}.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 */
public final class SwingTreeTableUI
        extends    BasicTableUI
        implements SwingTreeStyledComponentUI<JTable>
{
    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
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
            // Swing's own row height is a fixed 16 pixels set by the table's constructor, which on
            // a scaled display is shorter than the text in it. A symbol set with no chrome still
            // leaves this look and feel owning the font, and a row the font does not fit in is
            // unreadable rather than merely plain.
            table.setRowHeight(rowHeightFor(table));
        }
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LafPaint.applyAaHints((Graphics2D) g2);
            paintSelectionBands((Graphics2D) g2, (JTable) c);
            super.paint(g2, c);
        });
    }

    /** @return a row tall enough for the table's own font, with a little air above and below. */
    private static int rowHeightFor( JTable table ) {
        java.awt.Font font = table.getFont();
        int size = font == null ? UI.scale(13) : Math.round(font.getSize2D());
        return Math.round(size * 1.9f);
    }

    /** Fills a band behind each selected row; see {@link SwingTreeListUI} for why the table
     *  paints it rather than the cell renderer. */
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
