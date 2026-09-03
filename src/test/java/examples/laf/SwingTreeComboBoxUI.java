package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
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
import java.awt.Rectangle;

/**
 *  The {@link JComboBox} UI delegate. The drop-down list is a popup of Swing's own making and
 *  takes the popup menu and list defaults; the {@code ComboBox.list*} keys are what an
 *  application overrides to change it.
 */
public final class SwingTreeComboBoxUI
        extends    BasicComboBoxUI
        implements SwingTreeStyledComponentUI<JComboBox<?>>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeComboBoxUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
        // Focus on an editable combo box lands on its editor, and BasicComboBoxUI repaints the
        // combo box only for focus that lands on the combo box itself.
        JComboBox<?> combo = (JComboBox<?>) c;
        if ( combo.getEditor() != null )
            LafUtilities.repaintOnFocusChange(combo, combo.getEditor().getEditorComponent());
    }

    @Override
    public void uninstallUI( JComponent c ) {
        JComboBox<?> combo = (JComboBox<?>) c;
        if ( combo.getEditor() != null )
            LafUtilities.uninstallFocusRepaint(combo, combo.getEditor().getEditorComponent());
        super.uninstallUI(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> super.paint(g2, c));
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    /**
     *  Swing fills the strip showing the current value from the {@code ComboBox.background}
     *  default rather than from the component, which puts a square opaque rectangle over the
     *  rounded, possibly translucent surface a style rule has just painted. So it is only filled
     *  when no rule paints that surface.
     */
    @Override
    public void paintCurrentValueBackground( Graphics g, Rectangle bounds, boolean hasFocus ) {
        if ( !SwingTreeLookAndFeel.styles(comboBox.getClass()) )
            super.paintCurrentValueBackground(g, bounds, hasFocus);
    }

    @Override
    protected JButton createArrowButton() {
        return SwingTreeLookAndFeel.drawsOwnChrome() ? new ArrowButton() : super.createArrowButton();
    }

    @Override
    public ComponentStyleDelegate<JComboBox<?>> style( ComponentStyleDelegate<JComboBox<?>> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }

    /**
     *  A flat transparent button carrying the symbol set's drop-down arrow. It paints itself
     *  instead of going through the style engine, because the combo box around it is already a
     *  styled surface and a second one would draw a box around the arrow.
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
