package examples.laf;

import swingtree.UI;
import swingtree.api.Painter;
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
        if ( editor instanceof JSpinner.DefaultEditor ) {
            LafUtilities.repaintOnFocusChange(spinner, ((JSpinner.DefaultEditor) editor).getTextField());
            restyleInPlace(((JSpinner.DefaultEditor) editor).getTextField());
        }
    }

    /**
     *  A spinner's text field is styled when its own delegate is installed, which is before the
     *  spinner puts it inside itself, so a rule asking whether it sits inside a spinner gets the
     *  wrong answer until the field first paints. A layout that measures the spinner before that
     *  paint then gives it the height of a free standing text field, and keeps it.
     */
    @Override
    protected void replaceEditor( JComponent oldEditor, JComponent newEditor ) {
        super.replaceEditor(oldEditor, newEditor);
        if ( newEditor instanceof JSpinner.DefaultEditor )
            restyleInPlace(((JSpinner.DefaultEditor) newEditor).getTextField());
    }

    private static void restyleInPlace( JComponent inner ) {
        if ( inner.getUI() instanceof SwingTreeStyledComponentUI )
            SwingTreeLookAndFeel.installStyleOn(inner);
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
        LafUtilities.paintStyled(g, c, Painter.none());
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

    /** One of the two stepper buttons. */
    private static final class StepperButton extends ActuatorButton
    {
        private final boolean _up;

        StepperButton( boolean up ) { _up = up; }

        @Override public Dimension getPreferredSize() {
            Symbols symbols = SwingTreeLookAndFeel.symbols();
            return new Dimension(UI.scale(symbols.spinnerButtonWidth()),
                                 UI.scale(symbols.spinnerButtonHeight()));
        }

        @Override
        void paintActuator( Graphics2D g, Symbols symbols, SwingTreeLookAndFeel.Palette palette ) {
            ButtonModel model = getModel();
            symbols.paintSpinnerArrow(
                    g, palette, getWidth(), getHeight(), _up,
                    isEnabled(), model.isRollover(), model.isPressed()
            );
        }
    }
}
