package examples.laf;

import swingtree.api.Painter;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarUI;
import java.awt.Graphics;

/**
 *  The {@link JToolBar} UI delegate. An undocked tool bar is painted no differently, and the frame
 *  of the window it floats in belongs to whichever look and feel owns dialogs.
 */
public final class SwingTreeToolBarUI
        extends    BasicToolBarUI
        implements SwingTreeStyledComponentUI<JToolBar>
{
    private final SwingTreeLookAndFeel.Theme _theme;

    SwingTreeToolBarUI( SwingTreeLookAndFeel.Theme theme ) { _theme = theme; }

    public static ComponentUI createUI( JComponent c ) { return new SwingTreeToolBarUI(SwingTreeLookAndFeel.installedTheme()); }

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
    public ComponentStyleDelegate<JToolBar> style( ComponentStyleDelegate<JToolBar> it ) throws Exception {
        return _theme.applyStyle(it);
    }
}
