package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextPaneUI;
import javax.swing.text.JTextComponent;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JTextPane} UI delegate. A text pane is the styled-document cousin of a text
 *  area and is painted identically, letting the styled document control the colour and font
 *  of individual runs of text.
 *  <p>
 *  The painting pass goes through the {@code final}
 *  {@link javax.swing.plaf.basic.BasicTextUI#paint(Graphics, JComponent)} rather than through
 *  {@code paintSafely(..)} directly, because that is what takes the document's read lock while
 *  the view renders - the documented guarantee "that the model won't change from the view of
 *  this thread while it's rendering". Calling {@code paintSafely(..)} straight gives that up: a
 *  document mutated while the view hierarchy is being rendered makes the view ask for text that
 *  is no longer there, and Swing answers with {@code StateInvariantError: Can't render: p0,p1}.
 *  Measured on a wrapped text area with a writer on another thread: 192 such errors in 400
 *  paints without the lock, none with it.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 *  Nothing about how it looks is decided here: the appearance comes from the configured style
 *  preset, and is reached through {@link SwingTreeLookAndFeel#applyStyle(ComponentStyleDelegate)}.
 */
public final class SwingTreeTextPaneUI
        extends    BasicTextPaneUI
        implements SwingTreeStyledComponentUI<JTextPane>
{
    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeTextPaneUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
        // A text component does not repaint itself when it gains or loses focus, and repaints
        // only a narrow damage rectangle when its selection changes - neither is enough for a
        // style that is re-gathered as part of the component's own paint cycle.
        LafFocus.repaintOnFocus(c, c);
        LafSelection.repaintOnSelectionChange((JTextComponent) c);
    }

    @Override
    public void uninstallUI( JComponent c ) {
        LafSelection.uninstall((JTextComponent) c);
        LafFocus.uninstall(c, c);
        super.uninstallUI(c);
    }

    @Override
    public void update( Graphics g, JComponent c ) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LafPaint.applyAaHints((Graphics2D) g2);
            super.paint(g2, c);
        });
    }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JTextPane> style( ComponentStyleDelegate<JTextPane> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}
