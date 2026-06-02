package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/**
 *  The {@link JSpinner} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  The spinner surface is styled like a small {@link LinenTextFieldUI}.
 *  The two stepper buttons are replaced with flat chevron buttons
 *  (▲ / ▼) drawn in {@link LinenPalette#TEXT_MUTED} that brighten to
 *  {@link LinenPalette#ACCENT} on hover. The text editor inside the
 *  spinner receives its own UI (typically {@link LinenTextFieldUI}) so
 *  it inherits the same focus glow.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenSpinnerUI
        extends    BasicSpinnerUI
        implements SwingTreeStyledComponentUI<JSpinner>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenSpinnerUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LinenPaint.applyAaHints((java.awt.Graphics2D) g2);
            super.paint(g2, c);
        });
    }

    @Override
    public void update(Graphics g, JComponent c) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    protected Component createNextButton() {
        Component b = new StepperButton(+1);
        installNextButtonListeners(b);
        return b;
    }

    @Override
    protected Component createPreviousButton() {
        Component b = new StepperButton(-1);
        installPreviousButtonListeners(b);
        return b;
    }

    @Override
    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    public ComponentStyleDelegate<JSpinner> style(ComponentStyleDelegate<JSpinner> it) {
        JSpinner s = it.component();
        boolean enabled = s.isEnabled();
        boolean focused = enabled && innerEditorHasFocus(s);

        Color  surface = enabled ? LinenPalette.SURFACE_FIELD : LinenPalette.SURFACE_DISABLED;
        Color  fg      = enabled ? LinenPalette.TEXT : LinenPalette.TEXT_DISABLED;
        Color  border  = focused ? LinenPalette.ACCENT : LinenPalette.BORDER;
        double bw      = focused ? 2 : 1;
        double margin  = focused ? 0 : 1;

        return it
                .margin(margin)
                .padding(3)
                .borderRadius(7)
                .borderWidth(bw)
                .borderColor(border)
                .backgroundColor(surface)
                .foregroundColor(fg);
    }

    /**
     *  A {@link JSpinner}'s editor is a wrapper {@link JComponent} (by default
     *  a {@link JSpinner.DefaultEditor}) that contains the actual input
     *  control — usually a {@link javax.swing.JFormattedTextField} — as a
     *  child. Focus therefore lives on the descendant, not on the wrapper,
     *  so {@code editor.isFocusOwner()} would never return {@code true}.
     *  This helper walks one level into the default editor to read the right
     *  focus flag for the spinner's outer border.
     */
    private static boolean innerEditorHasFocus(JSpinner s) {
        Component editor = s.getEditor();
        if (editor instanceof JSpinner.DefaultEditor)
            return ((JSpinner.DefaultEditor) editor).getTextField().isFocusOwner();
        return editor != null && editor.isFocusOwner();
    }

    /** Tiny chevron button — direction is {@code +1} (up) or {@code -1} (down). */
    private static final class StepperButton extends JButton {
        private final int direction;
        StepperButton(int direction) {
            this.direction = direction;
            setBorder(null);
            setContentAreaFilled(false);
            setFocusable(false);
            setOpaque(false);
            setRolloverEnabled(true);
        }
        @Override public Dimension getPreferredSize() {
            return new Dimension(UI.scale(18), UI.scale(11));
        }
        @Override public Insets getInsets() { return new Insets(0, 0, 0, 0); }
        @Override public boolean isFocusTraversable() { return false; }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                AbstractButton self = this;
                Color colour = self.getModel().isRollover() || self.getModel().isPressed()
                        ? LinenPalette.ACCENT
                        : LinenPalette.TEXT_MUTED;
                if (!self.isEnabled()) colour = LinenPalette.TEXT_DISABLED;
                g2.setColor(colour);
                float w = getWidth(), h = getHeight();
                float cx = w / 2f, cy = h / 2f;
                float a  = UI.scale(3f);
                Path2D.Float p = new Path2D.Float();
                if (direction > 0) {
                    p.moveTo(cx - a, cy + a * 0.5f);
                    p.lineTo(cx,     cy - a * 0.5f);
                    p.lineTo(cx + a, cy + a * 0.5f);
                } else {
                    p.moveTo(cx - a, cy - a * 0.5f);
                    p.lineTo(cx,     cy + a * 0.5f);
                    p.lineTo(cx + a, cy - a * 0.5f);
                }
                g2.setStroke(new BasicStroke(Math.max(1.2f, UI.scale(1.4f)),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(p);
            } finally {
                g2.dispose();
            }
        }
    }
}