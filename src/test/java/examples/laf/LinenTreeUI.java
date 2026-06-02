package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTreeUI;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JTree} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Linen replaces the basic LAF's plus/minus disclosure handles with the
 *  rotated {@linkplain LinenIcons#treeCollapsed() chevron} pair, makes
 *  rows {@value #ROW_HEIGHT}&nbsp;dev-px tall for breathing room and
 *  hides the parent-to-child guide lines for a calmer look. Selection
 *  colours come from the {@code Tree.selectionBackground} and
 *  {@code Tree.selectionForeground} defaults installed by
 *  {@link LinenLookAndFeel}.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenTreeUI
        extends    BasicTreeUI
        implements SwingTreeStyledComponentUI<JTree>
{
    /** Default row height in developer pixels. */
    static final int ROW_HEIGHT = 22;

    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenTreeUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        JTree t = (JTree) c;
        t.setRowHeight(UI.scale(ROW_HEIGHT));
        t.setShowsRootHandles(true);
        setExpandedIcon(LinenIcons.treeExpanded());
        setCollapsedIcon(LinenIcons.treeCollapsed());
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

    /** No vertical guide line between siblings. */
    @Override
    protected void paintVerticalLine(Graphics g, JComponent c, int x, int top, int bottom) { /* off */ }

    /** No horizontal guide line into a child. */
    @Override
    protected void paintHorizontalLine(Graphics g, JComponent c, int y, int left, int right) { /* off */ }

    @Override
    public ComponentStyleDelegate<JTree> style(ComponentStyleDelegate<JTree> it) {
        return it
                .backgroundColor(LinenPalette.SURFACE_FIELD)
                .foregroundColor(LinenPalette.TEXT);
    }
}