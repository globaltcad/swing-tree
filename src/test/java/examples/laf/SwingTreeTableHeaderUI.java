package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;

/**
 *  The {@link JTableHeader} UI delegate. It installs a default cell renderer, so that a heading is
 *  a padded label in the palette's muted text colour whatever the table's model says.
 */
public final class SwingTreeTableHeaderUI
        extends    BasicTableHeaderUI
        implements SwingTreeStyledComponentUI<JTableHeader>
{
    /** The key Nimbus files the room around a heading's text under, which a look and feel that
     *  keeps different room puts into its defaults. */
    private static final String CONTENT_MARGINS = "TableHeader:\"TableHeader.renderer\".contentMargins";
    private static final Insets DEFAULT_MARGINS = new Insets(4, 10, 4, 10);

    private final SwingTreeLookAndFeel.Theme _theme;
    private final Insets                     _headingMargins;

    SwingTreeTableHeaderUI( SwingTreeLookAndFeel.Theme theme, Insets headingMargins ) {
        _theme          = theme;
        _headingMargins = headingMargins;
    }

    public static ComponentUI createUI( JComponent c ) {
        Insets headingMargins = UIManager.getInsets(CONTENT_MARGINS);
        return new SwingTreeTableHeaderUI(
                    SwingTreeLookAndFeel.installedTheme(),
                    headingMargins == null ? DEFAULT_MARGINS : headingMargins
                );
    }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        JTableHeader header = (JTableHeader) c;
        // A per-column header renderer is left alone. A column with none of its own already falls
        // back to the header default, and one installed per column would outlive this look and
        // feel: other look and feels replace the header default but never clear per-column ones.
        if ( _theme.symbols().drawsItsOwnChrome() && isReplaceableLafDefault(header.getDefaultRenderer()) )
            header.setDefaultRenderer(new HeaderRenderer(_theme, _headingMargins));
        _theme.installStyleOn(c);
    }

    /** @return {@code true} for a renderer the next look and feel is allowed to overwrite, which
     *          means one that is absent or marked as a {@link UIResource}. */
    private static boolean isReplaceableLafDefault( TableCellRenderer current ) {
        return current == null || current instanceof UIResource;
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> {
            super.paint(g2, c);
            paintColumnDividers(g2, (JTableHeader) c);
        });
    }

    /**
     *  Rules one heading off from the next, when the symbol set asks for it.
     *  <p>
     *  A table is free to be drawn without grid lines and still want its headings separated, so the
     *  lines are drawn here from the column model rather than left to {@link javax.swing.JTable}'s
     *  own vertical grid.
     */
    private void paintColumnDividers( Graphics2D g, JTableHeader header ) {
        Paint line = _theme.symbols().tableHeaderDivider(_theme.palette(), header.getHeight());
        if ( line == null )
            return;
        TableColumnModel columns = header.getColumnModel();
        int thickness = Math.max(1, UI.scale(1));
        int x = 0;
        g.setPaint(line);
        for ( int column = 0; column < columns.getColumnCount() - 1; column++ ) {
            x += columns.getColumn(column).getWidth();
            g.fillRect(x - thickness, 0, thickness, header.getHeight());
        }
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JTableHeader> style( ComponentStyleDelegate<JTableHeader> it ) throws Exception {
        return _theme.applyStyle(it);
    }

    /**
     *  The default header cell renderer: a padded label in whatever ink the header itself wears,
     *  which is the one the style rule for {@link JTableHeader} put there. It is a
     *  {@link UIResource} so that the next look and feel replaces it instead of keeping it.
     */
    private static final class HeaderRenderer extends DefaultTableCellRenderer implements UIResource
    {
        private final SwingTreeLookAndFeel.Theme _theme;
        private final Insets                     _margins;

        HeaderRenderer( SwingTreeLookAndFeel.Theme theme, Insets margins ) {
            _theme   = theme;
            _margins = margins;
            setHorizontalAlignment(SwingConstants.LEADING);
        }

        @Override
        public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
        ) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            JTableHeader header = table == null ? null : table.getTableHeader();
            label.setForeground(header == null ? _theme.palette().textMuted() : header.getForeground());
            label.setBackground(SwingTreeLookAndFeel.Palette.TRANSPARENT);
            label.setOpaque(false);
            Insets margins = _margins;
            label.setBorder(new EmptyBorder(UI.scale(margins.top), UI.scale(margins.left), UI.scale(margins.bottom), UI.scale(margins.right)));
            return label;
        }
    }
}
