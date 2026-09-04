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
 *  The {@link JTree} UI delegate. The disclosure handles are the symbol set's, installed as the
 *  {@code Tree.expandedIcon} and {@code Tree.collapsedIcon} defaults, and the guide lines from a
 *  parent to its children are left undrawn.
 */
public final class SwingTreeTreeUI
        extends    BasicTreeUI
        implements SwingTreeStyledComponentUI<JTree>
{
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
     *  The two lengths a tree stores instead of deriving them on every paint: the height of a row,
     *  and how far a child is inset from its parent. Both are written again after every change of
     *  the UI scale factor, because a tree whose rows stay 22 pixels tall while its font doubles
     *  clips off the bottom of every label it has.
     */
    private void applyScaledMetrics( JTree tree ) {
        if ( SwingTreeLookAndFeel.drawsOwnChrome() )
            tree.setRowHeight(UI.scale(SwingTreeLookAndFeel.symbols().treeRowHeight()));
        else {
            // A row shorter than the font in it is unreadable rather than merely plain.
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

    /** No vertical guide line between siblings, unless the symbol set draws no chrome. */
    @Override
    protected void paintVerticalLine( Graphics g, JComponent c, int x, int top, int bottom ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            super.paintVerticalLine(g, c, x, top, bottom);
    }

    /** No horizontal guide line into a child, unless the symbol set draws no chrome. */
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
