package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

/** The {@link JSpinner} UI delegate. Its two stepper buttons carry the symbol set's arrows. */
public final class SwingTreeSpinnerUI
        extends    BasicSpinnerUI
        implements SwingTreeStyledComponentUI<JSpinner>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeSpinnerUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
        // Focus lands on the text field inside the spinner, never on the spinner itself, and the
        // spinner's own border is what has to change when it does.
        JSpinner  spinner = (JSpinner) c;
        Component editor  = spinner.getEditor();
        if ( editor instanceof JSpinner.DefaultEditor )
            LafUtilities.repaintOnFocusChange(spinner, ((JSpinner.DefaultEditor) editor).getTextField());
    }

    @Override
    public void uninstallUI( JComponent c ) {
        JSpinner  spinner = (JSpinner) c;
        Component editor  = spinner.getEditor();
        if ( editor instanceof JSpinner.DefaultEditor )
            LafUtilities.uninstallFocusRepaint(spinner, ((JSpinner.DefaultEditor) editor).getTextField());
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

    @Override
    protected Component createNextButton() {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            return super.createNextButton();
        Component button = new StepperButton(true);
        installNextButtonListeners(button);
        return button;
    }

    @Override
    protected Component createPreviousButton() {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            return super.createPreviousButton();
        Component button = new StepperButton(false);
        installPreviousButtonListeners(button);
        return button;
    }

    @Override
    public ComponentStyleDelegate<JSpinner> style( ComponentStyleDelegate<JSpinner> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }

    /** One of the two stepper buttons. It paints itself instead of going through the style
     *  engine, because the spinner around it is already a styled surface and a second one would
     *  draw a box around the arrow. */
    private static final class StepperButton extends JButton
    {
        private final boolean _up;

        StepperButton( boolean up ) {
            _up = up;
            setBorder(null);
            setContentAreaFilled(false);
            setFocusable(false);
            setOpaque(false);
            setRolloverEnabled(true);
        }

        @Override public Dimension getPreferredSize() {
            Symbols symbols = SwingTreeLookAndFeel.symbols();
            return new Dimension(UI.scale(symbols.spinnerButtonWidth()),
                                 UI.scale(symbols.spinnerButtonHeight()));
        }

        @Override public Insets getInsets() { return new Insets(0, 0, 0, 0); }


        @Override protected void paintComponent( Graphics g ) {
            ButtonModel model = getModel();
            Graphics2D  g2    = (Graphics2D) g.create();
            try {
                SwingTreeLookAndFeel.symbols().paintSpinnerArrow(
                        g2, SwingTreeLookAndFeel.palette(), getWidth(), getHeight(), _up,
                        isEnabled(), model.isRollover(), model.isPressed()
                );
            } finally {
                g2.dispose();
            }
        }
    }
}
