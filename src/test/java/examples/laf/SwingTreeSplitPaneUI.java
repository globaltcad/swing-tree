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
 *  The {@link JSplitPane} UI delegate. Everything visible is on the divider, whose centre line
 *  and grip the symbol set draws.
 */
public final class SwingTreeSplitPaneUI
        extends    BasicSplitPaneUI
        implements SwingTreeStyledComponentUI<JSplitPane>
{
    private final SwingTreeLookAndFeel.Theme _theme;

    SwingTreeSplitPaneUI( SwingTreeLookAndFeel.Theme theme ) { _theme = theme; }

    public static ComponentUI createUI( JComponent c ) { return new SwingTreeSplitPaneUI(SwingTreeLookAndFeel.installedTheme()); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        JSplitPane pane = (JSplitPane) c;
        // The style engine draws the frame. Only a border Swing itself installed is dropped, so
        // that one the application set survives.
        if ( _theme.symbols().drawsItsOwnChrome() ) {
            if ( pane.getBorder() instanceof UIResource )
                pane.setBorder(null);
            pane.setDividerSize(UI.scale(_theme.symbols().splitDividerThickness()));
            // BasicSplitPaneUI starts with continuousLayout off, which draws a marker line while
            // the divider is dragged and moves the split only when the mouse is released.
            pane.setContinuousLayout(true);
        }
        _theme.installStyleOn(c);
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
    public BasicSplitPaneDivider createDefaultDivider() { return new Divider(this, _theme); }

    @Override
    public ComponentStyleDelegate<JSplitPane> style( ComponentStyleDelegate<JSplitPane> it ) throws Exception {
        return _theme.applyStyle(it);
    }

    /** The strip between the two panes, decorated by the symbol set. */
    private static final class Divider extends BasicSplitPaneDivider
    {
        private final SwingTreeLookAndFeel.Theme _theme;

        Divider( BasicSplitPaneUI ui, SwingTreeLookAndFeel.Theme theme ) {
            super(ui);
            _theme = theme;
        }

        @Override
        public void paint( Graphics g ) {
            // BasicSplitPaneDivider.paint fills the divider background and lays out the child
            // components, so anything drawn before it would be filled over.
            super.paint(g);
            if ( !_theme.symbols().drawsItsOwnChrome() )
                return;
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                _theme.symbols().paintSplitGrip(
                        g2, _theme.palette(), getWidth(), getHeight(),
                        orientation == JSplitPane.HORIZONTAL_SPLIT,
                        splitPane != null && splitPane.isEnabled()
                );
            } finally {
                g2.dispose();
            }
        }
    }
}
