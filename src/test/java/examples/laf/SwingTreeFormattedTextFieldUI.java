package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicFormattedTextFieldUI;
import javax.swing.text.JTextComponent;
import java.awt.Graphics;

/**
 *  The {@link JFormattedTextField} UI delegate. Visually identical to
 *  {@link SwingTreeTextFieldUI}, but the inherited delegate routes input through the
 *  field's formatter so values can be validated and committed or reverted.
 *  <p>
 *  Painting takes the document read lock the way {@link SwingTreeTextFieldUI} describes.
 */
public final class SwingTreeFormattedTextFieldUI
        extends    BasicFormattedTextFieldUI
        implements SwingTreeStyledComponentUI<JFormattedTextField>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeFormattedTextFieldUI(); }

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
    public ComponentStyleDelegate<JFormattedTextField> style( ComponentStyleDelegate<JFormattedTextField> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}
