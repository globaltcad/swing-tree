package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicListUI;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JList} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  List rows are rendered by Swing's standard
 *  {@link javax.swing.DefaultListCellRenderer}, which reads its colours
 *  from the {@code List.selectionBackground} / {@code List.selectionForeground}
 *  UI defaults seeded by {@link LinenLookAndFeel}. The list itself
 *  receives a cream surface and a subtle outer frame so that an
 *  un-scroll-paned list still reads as a distinct container.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenListUI
        extends    BasicListUI
        implements SwingTreeStyledComponentUI<JList<?>>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenListUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
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
    public ComponentStyleDelegate<JList<?>> style(ComponentStyleDelegate<JList<?>> it) {
        return it
                .backgroundColor(LinenPalette.SURFACE_FIELD)
                .foregroundColor(LinenPalette.TEXT);
    }
}