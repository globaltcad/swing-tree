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
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JTable} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Rows are rendered by Swing's
 *  {@link javax.swing.table.DefaultTableCellRenderer}, which reads its
 *  selection colours from the {@code Table.selectionBackground} /
 *  {@code Table.selectionForeground} defaults seeded by
 *  {@link LinenLookAndFeel}. Gridlines are kept off by default for a
 *  cleaner look — re-enable per-instance with
 *  {@link JTable#setShowGrid(boolean) setShowGrid(true)}.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenTableUI
        extends    BasicTableUI
        implements SwingTreeStyledComponentUI<JTable>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenTableUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        JTable t = (JTable) c;
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setRowHeight(UI.scale(24));
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LinenPaint.applyAaHints((Graphics2D) g2);
            paintSelectionBands((Graphics2D) g2, (JTable) c);
            super.paint(g2, c);
        });
    }

    /**
     *  Fills a band behind each selected row.
     *  <p>
     *  Swing's own mechanism — a cell renderer that arrives wearing the table's
     *  selection colour — cannot work under a SwingTree-backed look-and-feel:
     *  one renderer instance stands in for every cell, while the style engine
     *  keys a component's gathered style and its rendered layers on the
     *  component, so a per-cell colour decided in {@link LinenLabelUI} lands on
     *  whichever cell happens to be painted next. The table, on the other hand,
     *  knows exactly which rows are selected and is painted once, so the band
     *  belongs here. The renderers on top are not opaque, so the band shows
     *  through them.
     */
    private static void paintSelectionBands(Graphics2D g, JTable table) {
        int[] selected = table.getSelectedRows();
        if (selected.length == 0)
            return;
        g.setColor(LinenPalette.ACCENT_SOFT);
        for (int row : selected) {
            Rectangle band = table.getCellRect(row, 0, true);
            g.fillRect(0, band.y, table.getWidth(), band.height);
        }
    }

    @Override
    public void update(Graphics g, JComponent c) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JTable> style(ComponentStyleDelegate<JTable> it) {
        return it
                .backgroundColor(LinenPalette.SURFACE_FIELD)
                .foregroundColor(LinenPalette.TEXT);
    }
}