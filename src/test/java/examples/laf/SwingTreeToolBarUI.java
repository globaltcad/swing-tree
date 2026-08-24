package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarUI;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JToolBar} UI delegate: a flat strip carrying a row of commands. Floating mode -
 *  when the user undocks the bar - keeps the same painting; the floating window's own frame is
 *  drawn by whatever look and feel owns the dialog. The handle the bar is dragged by is painted
 *  by the configured symbol set, on the tool bar's content layer.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 */
public final class SwingTreeToolBarUI
        extends    BasicToolBarUI
        implements SwingTreeStyledComponentUI<JToolBar>
{
    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeToolBarUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
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

    @Override
    public ComponentStyleDelegate<JToolBar> style( ComponentStyleDelegate<JToolBar> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}
