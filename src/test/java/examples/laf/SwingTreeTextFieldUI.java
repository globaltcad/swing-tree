package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.JTextComponent;
import java.awt.Graphics;

/**
 *  The {@link JTextField} UI delegate.
 *  <p>
 *  Every text delegate here paints through the {@code final}
 *  {@link javax.swing.plaf.basic.BasicTextUI#paint(Graphics, JComponent)} rather than through
 *  {@code paintSafely(..)}, because only {@code paint(..)} takes the document's read lock. Without
 *  that lock a document edited while the view renders makes the view ask for text that is no
 *  longer there: 192 {@code StateInvariantError: Can't render: p0,p1} in 400 paints, none with it.
 */
public final class SwingTreeTextFieldUI
        extends    BasicTextFieldUI
        implements SwingTreeStyledComponentUI<JTextField>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeTextFieldUI(); }

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
        LafUtilities.paintStyled(g, c, g2 -> super.paint(g2, c));
    }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JTextField> style( ComponentStyleDelegate<JTextField> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}
