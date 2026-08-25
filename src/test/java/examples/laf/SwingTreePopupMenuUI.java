package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicPopupMenuUI;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JPopupMenu} UI delegate: the rounded, softly shadowed sheet the menu entries sit
 *  on, so that it visibly lifts off whatever lives underneath.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 */
public final class SwingTreePopupMenuUI
        extends    BasicPopupMenuUI
        implements SwingTreeStyledComponentUI<JPopupMenu>
{
    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreePopupMenuUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        JPopupMenu menu = (JPopupMenu) c;
        // The style engine paints the frame, so clear the border - but only when it is a
        // look-and-feel default (a UIResource); see SwingTreeScrollPaneUI for why.
        if ( menu.getBorder() instanceof UIResource && SwingTreeLookAndFeel.styles(c.getClass()) )
            menu.setBorder(null);
        SwingTreeLookAndFeel.installStyleOn(c);
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
    public ComponentStyleDelegate<JPopupMenu> style( ComponentStyleDelegate<JPopupMenu> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}
