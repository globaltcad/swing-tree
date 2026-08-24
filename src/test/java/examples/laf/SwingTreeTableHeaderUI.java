package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
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
import java.awt.Graphics2D;

/**
 *  The {@link JTableHeader} UI delegate. A header cell renderer is installed as the header's
 *  default so that the heading row reads as a quiet caption regardless of the table's model.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 */
public final class SwingTreeTableHeaderUI
        extends    BasicTableHeaderUI
        implements SwingTreeStyledComponentUI<JTableHeader>
{
    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeTableHeaderUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        JTableHeader header = (JTableHeader) c;
        // Follow Swing's UIResource contract: only replace the header's default renderer when it
        // is a look-and-feel default (absent or a UIResource); a renderer the application
        // installed directly is a NON-UIResource and must survive look-and-feel swaps. Per-column
        // header renderers are deliberately left alone: a column with a null header renderer
        // already falls back to this default, and installing ours per column would make it stick
        // after switching away, because other look and feels replace the header's default
        // renderer but do not clear per-column ones.
        if ( isReplaceableLafDefault(header.getDefaultRenderer()) )
            header.setDefaultRenderer(new HeaderRenderer());
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    /**
     * @param current the renderer the header carries right now
     * @return {@code true} if it is either absent or a {@link UIResource}, i.e. a look-and-feel
     *         default the next look and feel is allowed to overwrite
     */
    private static boolean isReplaceableLafDefault( TableCellRenderer current ) {
        return current == null || current instanceof UIResource;
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LafPaint.applyAaHints((Graphics2D) g2);
            super.paint(g2, c);
        });
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
     *  The default header cell renderer: a padded label in the palette's muted text colour.
     *  <p>
     *  Implements {@link UIResource} so Swing recognises it as a look-and-feel default and lets
     *  the next look and feel replace it cleanly. Without the marker, an application that
     *  switches away would still see this renderer on every header.
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
