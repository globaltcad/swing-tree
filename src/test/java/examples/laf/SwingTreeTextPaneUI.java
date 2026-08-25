package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextPaneUI;
import javax.swing.text.JTextComponent;
import java.awt.Graphics;

/**
 *  The {@link JTextPane} UI delegate. A text pane is the styled-document cousin of a text
 *  area and is painted identically, letting the styled document control the colour and font
 *  of individual runs of text.
 *  <p>
 *  Painting takes the document read lock the way {@link SwingTreeTextFieldUI} describes.
 */
public final class SwingTreeTextPaneUI
        extends    BasicTextPaneUI
        implements SwingTreeStyledComponentUI<JTextPane>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeTextPaneUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
        // A text component does not repaint itself when it gains or loses focus, and repaints
        // only a narrow damage rectangle when its selection changes - neither is enough for a
        // style that is re-gathered as part of the component's own paint cycle.
        LafUtilities.repaintOnFocusChange(c, c);
        LafUtilities.repaintOnSelectionChange((JTextComponent) c);
    }

    @Override
    public void uninstallUI( JComponent c ) {
        LafUtilities.uninstallSelectionRepaint((JTextComponent) c);
        LafUtilities.uninstallFocusRepaint(c, c);
        super.uninstallUI(c);
    }

    @Override
    public void update( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> super.paint(g2, c));
    }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JTextPane> style( ComponentStyleDelegate<JTextPane> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}
