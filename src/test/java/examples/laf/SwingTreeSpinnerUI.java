package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
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

/**
 *  The {@link JSpinner} UI delegate. The spinner's surface is styled like a small text field;
 *  its two stepper buttons are flat and carry nothing but the symbol set's arrows. The editor
 *  inside receives its own delegate, so it picks up the same focus treatment.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 */
public final class SwingTreeSpinnerUI
        extends    BasicSpinnerUI
        implements SwingTreeStyledComponentUI<JSpinner>
{
    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeSpinnerUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
        // The spinner's outer border tracks its inner editor's focus, but focus never lands on
        // the spinner itself, so bridge the editor's focus to a repaint of the whole spinner.
        JSpinner  spinner = (JSpinner) c;
        Component editor  = spinner.getEditor();
        if ( editor instanceof JSpinner.DefaultEditor )
            LafFocus.repaintOnFocus(spinner, ((JSpinner.DefaultEditor) editor).getTextField());
    }

    @Override
    public void uninstallUI( JComponent c ) {
        JSpinner  spinner = (JSpinner) c;
        Component editor  = spinner.getEditor();
        if ( editor instanceof JSpinner.DefaultEditor )
            LafFocus.uninstall(spinner, ((JSpinner.DefaultEditor) editor).getTextField());
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
    protected Component createNextButton() {
        Component button = new StepperButton(true);
        installNextButtonListeners(button);
        return button;
    }

    @Override
    protected Component createPreviousButton() {
        Component button = new StepperButton(false);
        installPreviousButtonListeners(button);
        return button;
    }

    @Override
    public ComponentStyleDelegate<JSpinner> style( ComponentStyleDelegate<JSpinner> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }

    /** One of the two stepper buttons. It paints itself rather than going through the style
     *  engine, for the same reason a combo box's drop-down button does: the spinner around it is
     *  already one styled surface, and a second one inside it would draw a box around the arrow. */
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
