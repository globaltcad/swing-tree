package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTreeUI;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *  The {@link JTree} UI delegate. The disclosure handles are the symbol set's, installed as the
 *  {@code Tree.expandedIcon} and {@code Tree.collapsedIcon} defaults, and the guide lines from a
 *  parent to its children are left undrawn.
 */
public final class SwingTreeTreeUI
        extends    BasicTreeUI
        implements SwingTreeStyledComponentUI<JTree>
{
    private final SwingTreeLookAndFeel.Theme _theme;
    private final int                        _leftChildIndent;
    private final int                        _rightChildIndent;

    SwingTreeTreeUI( SwingTreeLookAndFeel.Theme theme, int leftChildIndent, int rightChildIndent ) {
        _theme            = theme;
        _leftChildIndent  = leftChildIndent;
        _rightChildIndent = rightChildIndent;
    }

    public static ComponentUI createUI( JComponent c ) {
        return new SwingTreeTreeUI(
                    SwingTreeLookAndFeel.installedTheme(),
                    UIManager.getInt("Tree.leftChildIndent"),
                    UIManager.getInt("Tree.rightChildIndent")
                );
    }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        JTree tree = (JTree) c;
        // Installed rather than set, so that a symbol set drawing no chrome gives the handles back
        // and one the application set stands.
        LookAndFeel.installProperty(tree, "showsRootHandles", _theme.symbols().drawsItsOwnChrome());
        if ( _theme.symbols().drawsItsOwnChrome() ) {
            setExpandedIcon(GlyphIcons.treeExpanded(_theme));
            setCollapsedIcon(GlyphIcons.treeCollapsed(_theme));
        }
        LafUtilities.rescaleOnUiScaleChange(tree, () -> applyScaledMetrics(tree));
        _theme.installStyleOn(c);
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
        if ( _theme.symbols().drawsItsOwnChrome() )
            tree.setRowHeight(UI.scale(_theme.symbols().treeRowHeight()));
        else {
            // A row shorter than the font in it is unreadable rather than merely plain.
            java.awt.Font font = tree.getFont();
            int size = font == null ? UI.scale(13) : Math.round(font.getSize2D());
            tree.setRowHeight(Math.round(size * 1.75f));
        }
        setLeftChildIndent(UI.scale(_leftChildIndent));
        setRightChildIndent(UI.scale(_rightChildIndent));
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> {
            paintSelectionBands(g2, (JTree) c);
            super.paint(g2, c);
        });
    }

    /**
     *  Fills a band the width of the tree behind each selected row.
     *  <p>
     *  A tree cell renderer only ever fills the box its own label occupies, so selecting a deep
     *  node marks a short bar somewhere off to the right rather than the row. The tree knows which
     *  rows are selected and is painted once, so the band is filled here and the renderers, which
     *  are not opaque, are painted over it.
     */
    private void paintSelectionBands( Graphics2D g, JTree tree ) {
        if ( !_theme.symbols().drawsItsOwnChrome() )
            return; // Swing's own renderer is carrying the selection colour
        int[] selected = tree.getSelectionRows();
        if ( selected == null )
            return;
        g.setColor(_theme.palette().accentSoft());
        for ( int row : selected ) {
            Rectangle band = tree.getRowBounds(row);
            if ( band != null )
                g.fillRect(0, band.y, tree.getWidth(), band.height);
        }
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    /** No vertical guide line between siblings, unless the symbol set draws no chrome. */
    @Override
    protected void paintVerticalLine( Graphics g, JComponent c, int x, int top, int bottom ) {
        if ( !_theme.symbols().drawsItsOwnChrome() )
            super.paintVerticalLine(g, c, x, top, bottom);
    }

    /** No horizontal guide line into a child, unless the symbol set draws no chrome. */
    @Override
    protected void paintHorizontalLine( Graphics g, JComponent c, int y, int left, int right ) {
        if ( !_theme.symbols().drawsItsOwnChrome() )
            super.paintHorizontalLine(g, c, y, left, right);
    }

    @Override
    public ComponentStyleDelegate<JTree> style( ComponentStyleDelegate<JTree> it ) throws Exception {
        return _theme.applyStyle(it);
    }
}
