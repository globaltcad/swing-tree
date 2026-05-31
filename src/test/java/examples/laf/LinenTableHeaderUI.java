package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JTableHeader} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Header cells are flat {@link LinenPalette#SURFACE} rectangles with a
 *  small inset, bold-leaning {@link LinenPalette#TEXT_MUTED} text and a
 *  hairline {@link LinenPalette#BORDER_SOFT} divider underneath. A
 *  {@link LinenHeaderRenderer} is installed as the default renderer for
 *  every column so the look is consistent regardless of model.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenTableHeaderUI
        extends    BasicTableHeaderUI
        implements SwingTreeStyledComponentUI<JTableHeader>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenTableHeaderUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        JTableHeader h = (JTableHeader) c;
        TableCellRenderer linenRenderer = new LinenHeaderRenderer();
        // Follow Swing's UIResource contract: only replace renderers that
        // the application has *not* explicitly opted into. A renderer the
        // app installed directly is signalled by its NON-UIResource type;
        // we must leave those alone so they survive LAF swaps.
        if (isReplaceableLafDefault(h.getDefaultRenderer()))
            h.setDefaultRenderer(linenRenderer);
        if (h.getColumnModel() != null) {
            for (int i = 0; i < h.getColumnModel().getColumnCount(); i++) {
                TableColumn col = h.getColumnModel().getColumn(i);
                if (isReplaceableLafDefault(col.getHeaderRenderer()))
                    col.setHeaderRenderer(linenRenderer);
            }
        }
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    /**
     *  @return {@code true} if {@code current} is either absent or a
     *          {@link UIResource}, i.e. a LAF-installed default the next
     *          LAF is allowed to overwrite. An application-supplied
     *          renderer (a plain {@code TableCellRenderer} that does
     *          not implement {@code UIResource}) returns {@code false}
     *          and is preserved.
     */
    private static boolean isReplaceableLafDefault(TableCellRenderer current) {
        return current == null || current instanceof UIResource;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LinenPaint.applyAaHints((Graphics2D) g2);
            super.paint(g2, c);
        });
    }

    @Override
    public void update(Graphics g, JComponent c) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JTableHeader> style(ComponentStyleDelegate<JTableHeader> it) {
        return it
                .backgroundColor(LinenPalette.SURFACE)
                .foregroundColor(LinenPalette.TEXT_MUTED)
                .borderAt(UI.Edge.BOTTOM, 1, LinenPalette.BORDER_SOFT);
    }

    /**
     *  Default header cell renderer for Linen tables: a left-padded label
     *  in {@link LinenPalette#TEXT_MUTED}.
     *  <p>
     *  Implements {@link UIResource} so Swing recognises it as a
     *  LAF-installed default and lets the next LAF replace it cleanly
     *  on {@link javax.swing.UIManager#setLookAndFeel(javax.swing.LookAndFeel)}.
     *  Without the marker, an application that switches away from Linen
     *  would still see this renderer "stick" on every header.
     */
    private static final class LinenHeaderRenderer extends DefaultTableCellRenderer implements UIResource {
        LinenHeaderRenderer() {
            setHorizontalAlignment(SwingConstants.LEADING);
        }
        @Override
        public Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setForeground(LinenPalette.TEXT_MUTED);
            l.setBackground(new Color(0, 0, 0, 0));
            l.setOpaque(false);
            l.setBorder(new EmptyBorder(UI.scale(4), UI.scale(10), UI.scale(4), UI.scale(10)));
            return l;
        }
    }
}