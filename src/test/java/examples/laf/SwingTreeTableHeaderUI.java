package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Graphics;

/**
 *  The {@link JTableHeader} UI delegate. It installs a default cell renderer, so that a heading is
 *  a padded label in the palette's muted text colour whatever the table's model says.
 */
public final class SwingTreeTableHeaderUI
        extends    BasicTableHeaderUI
        implements SwingTreeStyledComponentUI<JTableHeader>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeTableHeaderUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        JTableHeader header = (JTableHeader) c;
        // A per-column header renderer is left alone. A column with none of its own already falls
        // back to the header default, and one installed per column would outlive this look and
        // feel: other look and feels replace the header default but never clear per-column ones.
        if ( SwingTreeLookAndFeel.drawsOwnChrome() && isReplaceableLafDefault(header.getDefaultRenderer()) )
            header.setDefaultRenderer(new HeaderRenderer());
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    /** @return {@code true} for a renderer the next look and feel is allowed to overwrite, which
     *          means one that is absent or marked as a {@link UIResource}. */
    private static boolean isReplaceableLafDefault( TableCellRenderer current ) {
        return current == null || current instanceof UIResource;
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> super.paint(g2, c));
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JTableHeader> style( ComponentStyleDelegate<JTableHeader> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }

    /**
     *  The default header cell renderer: a padded label in the palette's muted text colour. It is
     *  a {@link UIResource} so that the next look and feel replaces it instead of keeping it.
     */
    private static final class HeaderRenderer extends DefaultTableCellRenderer implements UIResource
    {
        HeaderRenderer() { setHorizontalAlignment(SwingConstants.LEADING); }

        @Override
        public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
        ) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setForeground(SwingTreeLookAndFeel.palette().textMuted());
            label.setBackground(SwingTreeLookAndFeel.Palette.TRANSPARENT);
            label.setOpaque(false);
            label.setBorder(new EmptyBorder(UI.scale(4), UI.scale(10), UI.scale(4), UI.scale(10)));
            return label;
        }
    }
}
