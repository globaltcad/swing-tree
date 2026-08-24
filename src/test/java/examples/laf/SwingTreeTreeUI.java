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
 *  The {@link JTree} UI delegate. The disclosure handles come from the configured symbol set
 *  through the {@code Tree.expandedIcon} and {@code Tree.collapsedIcon} defaults, rows are given
 *  breathing room, and the parent-to-child guide lines are suppressed for a calmer look.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 */
public final class SwingTreeTreeUI
        extends    BasicTreeUI
        implements SwingTreeStyledComponentUI<JTree>
{
    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeTreeUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        JTree tree = (JTree) c;
        tree.setRowHeight(UI.scale(SwingTreeLookAndFeel.symbols().treeRowHeight()));
        tree.setShowsRootHandles(true);
        setExpandedIcon(GlyphIcons.treeExpanded());
        setCollapsedIcon(GlyphIcons.treeCollapsed());
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
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

    /** No vertical guide line between siblings. */
    @Override
    protected void paintVerticalLine( Graphics g, JComponent c, int x, int top, int bottom ) { /* off */ }

    /** No horizontal guide line into a child. */
    @Override
    protected void paintHorizontalLine( Graphics g, JComponent c, int y, int left, int right ) { /* off */ }

    @Override
    public ComponentStyleDelegate<JTree> style( ComponentStyleDelegate<JTree> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}
