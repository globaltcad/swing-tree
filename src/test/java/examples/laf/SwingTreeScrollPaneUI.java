package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;
import java.awt.Graphics;

/**
 *  The {@link JScrollPane} UI delegate: the frame around a scrollable region. What that frame
 *  looks like - a field, a card, a rail or nothing at all - follows the
 *  {@link SwingTreeLookAndFeel.Surface} the scroll pane was tagged with. All of the scrolling
 *  visuals live on {@link SwingTreeScrollBarUI}.
 */
public final class SwingTreeScrollPaneUI
        extends    BasicScrollPaneUI
        implements SwingTreeStyledComponentUI<JScrollPane>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeScrollPaneUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> super.paint(g2, c));
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JScrollPane> style( ComponentStyleDelegate<JScrollPane> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}
