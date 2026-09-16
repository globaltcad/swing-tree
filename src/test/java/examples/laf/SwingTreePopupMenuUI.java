package examples.laf;

import swingtree.api.Painter;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;
import java.awt.Graphics;

/** The {@link JPopupMenu} UI delegate. */
public final class SwingTreePopupMenuUI
        extends    BasicPopupMenuUI
        implements SwingTreeStyledComponentUI<JPopupMenu>
{
    private final SwingTreeLookAndFeel.Theme _theme;

    SwingTreePopupMenuUI( SwingTreeLookAndFeel.Theme theme ) { _theme = theme; }

    public static ComponentUI createUI( JComponent c ) { return new SwingTreePopupMenuUI(SwingTreeLookAndFeel.installedTheme()); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        _theme.installStyleOn(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, Painter.none());
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JPopupMenu> style( ComponentStyleDelegate<JPopupMenu> it ) throws Exception {
        return _theme.applyStyle(it);
    }
}
