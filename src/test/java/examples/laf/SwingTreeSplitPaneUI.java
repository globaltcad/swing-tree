package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JSplitPane} UI delegate. The split pane itself carries no chrome; everything
 *  visible lives on the divider, whose centre line and grip the configured symbol set draws.
 */
public final class SwingTreeSplitPaneUI
        extends    BasicSplitPaneUI
        implements SwingTreeStyledComponentUI<JSplitPane>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeSplitPaneUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        JSplitPane pane = (JSplitPane) c;
        // The style engine draws the frame, so clear the border - but only when it is a
        // look-and-feel default (a UIResource); see SwingTreeScrollPaneUI for why.
        if ( SwingTreeLookAndFeel.drawsOwnChrome() ) {
            if ( pane.getBorder() instanceof UIResource )
                pane.setBorder(null);
            pane.setDividerSize(UI.scale(SwingTreeLookAndFeel.symbols().splitDividerThickness()));
            // BasicSplitPaneUI defaults continuousLayout to false, i.e. dragging the divider draws
            // only a placeholder marker line and the split repositions on mouse-release. Switch it
            // on so the panes resize live while the user drags.
            pane.setContinuousLayout(true);
        }
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
    public BasicSplitPaneDivider createDefaultDivider() { return new Divider(this); }

    @Override
    public ComponentStyleDelegate<JSplitPane> style( ComponentStyleDelegate<JSplitPane> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }

    /** The strip between the two panes, decorated by the symbol set. */
    private static final class Divider extends BasicSplitPaneDivider
    {
        Divider( BasicSplitPaneUI ui ) { super(ui); }

        @Override
        public void paint( Graphics g ) {
            // Paint the superclass first - BasicSplitPaneDivider.paint fills the divider
            // background when the divider is opaque and lays out any child components (the
            // drag-arrow buttons some look and feels add). Decorations drawn before that would be
            // erased by the background fill on the way back up.
            super.paint(g);
            if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
                return;
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                SwingTreeLookAndFeel.symbols().paintSplitGrip(
                        g2, SwingTreeLookAndFeel.palette(), getWidth(), getHeight(),
                        orientation == JSplitPane.HORIZONTAL_SPLIT,
                        splitPane != null && splitPane.isEnabled()
                );
            } finally {
                g2.dispose();
            }
        }
    }
}
