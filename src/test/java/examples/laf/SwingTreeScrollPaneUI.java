package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicScrollPaneUI;
import java.awt.Graphics;

/**
 *  The {@link JScrollPane} UI delegate: the frame around a scrollable region. What that frame
 *  looks like - a field, a card, a rail or nothing at all - follows the
 *  {@link SwingTreeLookAndFeel.Surface} the scroll pane was tagged with. All of the scrolling
 *  visuals live on {@link SwingTreeScrollBarUI}.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 */
public final class SwingTreeScrollPaneUI
        extends    BasicScrollPaneUI
        implements SwingTreeStyledComponentUI<JScrollPane>
{
    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeScrollPaneUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        JScrollPane pane = (JScrollPane) c;
        // The style engine draws the frame, so the component border should be cleared - but only
        // when it is a look-and-feel default (a UIResource). A border the application set
        // explicitly is preserved so it survives a look-and-feel swap, honouring Swing's contract.
        if ( pane.getBorder() instanceof UIResource && SwingTreeLookAndFeel.styles(c.getClass()) )
            pane.setBorder(null);
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        ComponentExtension.from(c).paintBackground(g, g2 -> super.paint(g2, c));
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
