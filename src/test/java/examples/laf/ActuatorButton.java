package examples.laf;

import swingtree.style.ComponentStyleDelegate;

import javax.swing.JButton;
import javax.swing.border.Border;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

/**
 *  The small button a control is worked by: a combo box's drop-down, a spinner's two steppers, a
 *  scroll bar's two ends. Swing puts each of them inside a control the application never declared,
 *  and each is drawn by the {@link Symbols} set rather than by the style engine, because the
 *  control around it is already a styled surface and a second one would draw a box around the
 *  arrow.
 *  <p>
 *  <b>Refusing a border is what makes that hold after a theme change as well as before one.</b> An
 *  actuator is a {@link JButton}, so {@link SwingTreeButtonUI} is its UI delegate and installs the
 *  style engine's border while the delegate is being installed. Clearing that border in a
 *  constructor is enough only the first time: switching preset calls
 *  {@link javax.swing.SwingUtilities#updateComponentTreeUI}, which installs a fresh delegate on
 *  every component that already exists, and there is no constructor left to run afterwards. The
 *  actuator would then wear a whole control's outline - a raised, carved box around a drop-down
 *  arrow - from the first switch onwards, while a freshly started application looked right.
 */
abstract class ActuatorButton extends JButton
{
    ActuatorButton() {
        super.setBorder(null);
        setContentAreaFilled(false);
        setFocusable(false);
        setOpaque(false);
        setRolloverEnabled(true);
    }

    /** Ignores {@code border} and stays borderless, for the reason given on the class. */
    @Override
    public final void setBorder( Border border ) { super.setBorder(null); }

    /** @return no insets at all: the symbol set is handed the whole button and fills it. */
    @Override
    public Insets getInsets() { return new Insets(0, 0, 0, 0); }

    /**
     *  Hands the whole button to {@link #paintActuator} on a scratch context. Nothing calls up to
     *  {@link javax.swing.plaf.ComponentUI#update}, so the style engine never paints an actuator
     *  even if a {@link ComponentStyleDelegate} was gathered for one.
     */
    @Override
    protected final void paintComponent( Graphics g ) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            paintActuator(g2, SwingTreeLookAndFeel.symbols(), SwingTreeLookAndFeel.palette());
        } finally {
            g2.dispose();
        }
    }

    /**
     *  Draws this actuator.
     *
     * @param g a scratch context covering the whole button, which the implementation may configure
     *          freely and does not have to dispose of
     * @param symbols the symbol set in force
     * @param palette the palette in force
     */
    abstract void paintActuator( Graphics2D g, Symbols symbols, SwingTreeLookAndFeel.Palette palette );
}
