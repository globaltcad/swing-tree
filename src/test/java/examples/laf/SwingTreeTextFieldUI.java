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
 *  The {@link JTextField} UI delegate: a rounded field whose border grows into the accent
 *  colour on focus, under a faint accent-tinted glow.
 *  <p>
 *  Painting goes through the {@code final}
 *  {@link javax.swing.plaf.basic.BasicTextUI#paint(Graphics, JComponent)} and not through
 *  {@code paintSafely(..)}, because only the former takes the document's read lock. Without it a
 *  document mutated mid-render makes the view ask for text that is no longer there: measured 192
 *  {@code StateInvariantError: Can't render: p0,p1} in 400 paints, none with the lock.
 */
public final class SwingTreeTextFieldUI
        extends    BasicTextFieldUI
        implements SwingTreeStyledComponentUI<JTextField>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeTextFieldUI(); }

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
    public ComponentStyleDelegate<JTextField> style( ComponentStyleDelegate<JTextField> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}
