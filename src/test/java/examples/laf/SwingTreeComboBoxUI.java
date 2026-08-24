package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

/**
 *  The {@link JComboBox} UI delegate. The outer surface mirrors a text field; the drop-down
 *  button is a flat, transparent button whose arrow the configured symbol set draws.
 *  <p>
 *  The popup window is rendered separately by Swing and inherits the popup-menu and list
 *  defaults; an application wanting more control over it can override the {@code ComboBox.list*}
 *  keys.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 */
public final class SwingTreeComboBoxUI
        extends    BasicComboBoxUI
        implements SwingTreeStyledComponentUI<JComboBox<?>>
{
    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeComboBoxUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
        // A non-editable combo owns focus itself and the inherited delegate repaints it, but an
        // editable combo's focus lives on its editor, so bridge that to a repaint of the whole.
        JComboBox<?> combo = (JComboBox<?>) c;
        if ( combo.getEditor() != null )
            LafFocus.repaintOnFocus(combo, combo.getEditor().getEditorComponent());
    }

    @Override
    public void uninstallUI( JComponent c ) {
        JComboBox<?> combo = (JComboBox<?>) c;
        if ( combo.getEditor() != null )
            LafFocus.uninstall(combo, combo.getEditor().getEditorComponent());
        super.uninstallUI(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LafPaint.applyAaHints((Graphics2D) g2);
            super.paint(g2, c);
        });
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    protected JButton createArrowButton() { return new ArrowButton(); }

    @Override
    public ComponentStyleDelegate<JComboBox<?>> style( ComponentStyleDelegate<JComboBox<?>> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }

    /**
     *  A flat, transparent button carrying nothing but the symbol set's drop-down arrow. It
     *  paints itself rather than going through the style engine: the surrounding combo box is
     *  already one styled surface, and a second one inside it would draw a box around the arrow.
     */
    private static final class ArrowButton extends JButton
    {
        ArrowButton() {
            setBorder(null);
            setContentAreaFilled(false);
            setFocusable(false);
            setOpaque(false);
            setRolloverEnabled(true);
        }

        @Override public Dimension getPreferredSize() {
            int side = UI.scale(SwingTreeLookAndFeel.symbols().comboArrowButtonSize());
            return new Dimension(side, side);
        }

        @Override public Insets getInsets() { return new Insets(0, 0, 0, 0); }


        @Override protected void paintComponent( Graphics g ) {
            ButtonModel model = getModel();
            Graphics2D  g2    = (Graphics2D) g.create();
            try {
                SwingTreeLookAndFeel.symbols().paintComboArrow(
                        g2, SwingTreeLookAndFeel.palette(), getWidth(), getHeight(),
                        isEnabled(), model.isRollover(), model.isPressed()
                );
            } finally {
                g2.dispose();
            }
        }
    }
}
