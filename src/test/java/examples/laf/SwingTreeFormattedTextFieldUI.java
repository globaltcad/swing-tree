package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicFormattedTextFieldUI;
import javax.swing.text.JTextComponent;
import java.awt.Graphics;

/** The {@link JFormattedTextField} UI delegate. */
public final class SwingTreeFormattedTextFieldUI
        extends    BasicFormattedTextFieldUI
        implements SwingTreeStyledComponentUI<JFormattedTextField>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeFormattedTextFieldUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
        // Swing repaints neither on a focus change nor across a whole new selection, and the
        // style is re-gathered while the component paints, so both need a repaint of their own.
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
        // BasicTextUI.paint(..) takes the document read lock, paintSafely(..) does not.
        LafUtilities.paintStyled(g, c, g2 -> super.paint(g2, c));
    }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JFormattedTextField> style( ComponentStyleDelegate<JFormattedTextField> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}
