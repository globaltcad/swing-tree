package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTreeUI;
import java.awt.Graphics;

/**
 *  The {@link JTree} UI delegate. The disclosure handles come from the configured symbol set
 *  through the {@code Tree.expandedIcon} and {@code Tree.collapsedIcon} defaults, rows are given
 *  breathing room, and the parent-to-child guide lines are suppressed for a calmer look.
 */
public final class SwingTreeTreeUI
        extends    BasicTreeUI
        implements SwingTreeStyledComponentUI<JTree>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeTreeUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        JTree tree = (JTree) c;
        if ( SwingTreeLookAndFeel.drawsOwnChrome() ) {
            tree.setShowsRootHandles(true);
            setExpandedIcon(GlyphIcons.treeExpanded());
            setCollapsedIcon(GlyphIcons.treeCollapsed());
        }
        LafUtilities.rescaleOnUiScaleChange(tree, () -> applyScaledMetrics(tree));
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
    public void uninstallUI( JComponent c ) {
        LafUtilities.uninstallUiScaleRescale(c);
        super.uninstallUI(c);
    }

    /**
     *  The two lengths a tree keeps rather than re-derives: how tall a row is, and how far a
     *  child is inset from its parent. Both are written again after every scale change, because
     *  a tree whose rows stay 22 pixels tall while its text doubles clips every label it has.
     */
    private void applyScaledMetrics( JTree tree ) {
        if ( SwingTreeLookAndFeel.drawsOwnChrome() )
            tree.setRowHeight(UI.scale(SwingTreeLookAndFeel.symbols().treeRowHeight()));
        else {
            // See SwingTreeTableUI: a row shorter than the font in it is unreadable, not plain.
            java.awt.Font font = tree.getFont();
            int size = font == null ? UI.scale(13) : Math.round(font.getSize2D());
            tree.setRowHeight(Math.round(size * 1.75f));
        }
        setLeftChildIndent(UI.scale(UIManager.getInt("Tree.leftChildIndent")));
        setRightChildIndent(UI.scale(UIManager.getInt("Tree.rightChildIndent")));
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> super.paint(g2, c));
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    /** No vertical guide line between siblings, unless the symbol set has no opinion at all. */
    @Override
    protected void paintVerticalLine( Graphics g, JComponent c, int x, int top, int bottom ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            super.paintVerticalLine(g, c, x, top, bottom);
    }

    /** No horizontal guide line into a child, same caveat. */
    @Override
    protected void paintHorizontalLine( Graphics g, JComponent c, int y, int left, int right ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            super.paintHorizontalLine(g, c, y, left, right);
    }

    @Override
    public ComponentStyleDelegate<JTree> style( ComponentStyleDelegate<JTree> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}
