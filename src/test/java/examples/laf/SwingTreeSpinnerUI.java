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
    private final SwingTreeLookAndFeel.Theme _theme;

    SwingTreeSpinnerUI( SwingTreeLookAndFeel.Theme theme ) { _theme = theme; }

    public static ComponentUI createUI( JComponent c ) { return new SwingTreeSpinnerUI(SwingTreeLookAndFeel.installedTheme()); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        _theme.installStyleOn(c);
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
        // The focus repaint follows the field that can take focus, or a spinner given a new model
        // after it was built would stop showing focus.
        if ( oldEditor instanceof JSpinner.DefaultEditor )
            LafUtilities.uninstallFocusRepaint(spinner, ((JSpinner.DefaultEditor) oldEditor).getTextField());
        if ( newEditor instanceof JSpinner.DefaultEditor ) {
            LafUtilities.repaintOnFocusChange(spinner, ((JSpinner.DefaultEditor) newEditor).getTextField());
            restyleInPlace(((JSpinner.DefaultEditor) newEditor).getTextField());
        }
    }

    private void restyleInPlace( JComponent inner ) {
        if ( inner.getUI() instanceof SwingTreeStyledComponentUI )
            _theme.installStyleOn(inner);
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
        if ( !_theme.symbols().drawsItsOwnChrome() )
            return super.createNextButton();
        Component button = new StepperButton(_theme, true);
        installNextButtonListeners(button);
        return button;
    }

    @Override
    protected Component createPreviousButton() {
        if ( !_theme.symbols().drawsItsOwnChrome() )
            return super.createPreviousButton();
        Component button = new StepperButton(_theme, false);
        installPreviousButtonListeners(button);
        return button;
    }

    @Override
    public ComponentStyleDelegate<JSpinner> style( ComponentStyleDelegate<JSpinner> it ) throws Exception {
        return _theme.applyStyle(it);
    }

    /** One of the two stepper buttons. */
    private static final class StepperButton extends ActuatorButton
    {
        private final boolean _up;

        StepperButton( SwingTreeLookAndFeel.Theme theme, boolean up ) {
            super(theme);
            _up = up;
        }

        @Override public Dimension getPreferredSize() {
            Symbols symbols = theme().symbols();
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
