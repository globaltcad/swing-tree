package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Graphics;

/**
 *  The {@link AbstractButton} UI delegate of the {@link LinenLookAndFeel} —
 *  used for both {@link javax.swing.JButton} and
 *  {@link javax.swing.JToggleButton} since they share the same model.
 *
 *  <h2>Visual behaviour</h2>
 *  <ul>
 *      <li><b>Resting</b> — a soft cream surface with a faint top-to-bottom
 *          highlight gradient and a 1&nbsp;dev-px taupe border. A barely
 *          visible drop shadow grounds the button.</li>
 *      <li><b>Hovered</b> — the surface lifts to {@link LinenPalette#SURFACE_HOVER}
 *          and the drop shadow widens.</li>
 *      <li><b>Pressed / selected</b> — the surface deepens to
 *          {@link LinenPalette#SURFACE_PRESSED} and the shadow becomes inset,
 *          giving a tactile push-in feel.</li>
 *      <li><b>Focused</b> — the border thickens to 2&nbsp;dev-px and switches
 *          to {@link LinenPalette#ACCENT}. No external focus rectangle is
 *          painted; the border <i>is</i> the focus indicator.</li>
 *      <li><b>Disabled</b> — surface and text fade to the disabled colours,
 *          shadow disappears.</li>
 *  </ul>
 *
 *  <h2>Semantic variants</h2>
 *  Which <i>colours</i> the states above resolve to is decided by
 *  {@link LinenVariant}, read from the component's style group tags. A button
 *  tagged {@code .group(LinenVariant.PRIMARY)} is filled with the moss accent, a
 *  {@link LinenVariant#DANGER} one with faded brick, and a
 *  {@link LinenVariant#QUIET} one carries neither surface nor border until the
 *  pointer arrives. Everything else — radius, padding, the focus border that
 *  grows while the margin shrinks to absorb it — is shared across the variants.
 *  <p>
 *  This is deliberately a LAF-owned enum rather than something an application
 *  spells out in its own {@code StyleSheet}: a style sheet is resolved
 *  <em>before</em> the look-and-feel, so a LAF that sets a background
 *  unconditionally would overwrite the application's brand colour. See
 *  {@link LinenVariant} for the full explanation.
 *
 *  <p>All numeric inputs (radius, padding, shadow offsets, …) are in
 *  developer pixels and are scaled automatically by SwingTree for HiDPI.
 *
 *  <p>The class is {@code final}; customise via a stylesheet entry such as
 *  {@code add(type(JButton.class), it -> it.borderRadius(0))} or per-component
 *  with {@code .withStyle(..)}.
 */
public final class LinenButtonUI
        extends    BasicButtonUI
        implements SwingTreeStyledComponentUI<AbstractButton>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenButtonUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        AbstractButton b = (AbstractButton) c;
        // The style engine paints the fill — Swing's own opaque fill would
        // double-paint underneath rounded corners and the noise overlay.
        b.setContentAreaFilled(false);
        b.setBorderPainted(true);
        b.setRolloverEnabled(true);
        b.setFocusPainted(false);
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
    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    public ComponentStyleDelegate<AbstractButton> style(ComponentStyleDelegate<AbstractButton> it) {
        AbstractButton b = it.component();
        ButtonModel    m = b.getModel();

        boolean enabled  = b.isEnabled();
        boolean pressed  = enabled && m.isArmed() && m.isPressed();
        boolean selected = enabled && m.isSelected();
        boolean sunken   = pressed || selected;
        boolean rollover = enabled && m.isRollover() && !pressed;
        boolean focused  = enabled && b.isFocusOwner();

        LinenVariant variant = LinenVariant.of(b);

        Color   surface = variant.surface(enabled, sunken, rollover);
        Color   border  = variant.border(enabled, focused, rollover);
        double  borderW = focused ? 2 : 1;
        // Outer margin shrinks by the same delta the border grows so the
        // component's overall footprint stays constant on focus — no layout
        // shift in the surrounding tree.
        double  margin  = focused ? 0 : 1;

        it = it
                .margin(margin)
                .padding(6, 16, 6, 16)
                .borderRadius(9)
                .borderWidth(borderW)
                .borderColor(border)
                .backgroundColor(surface)
                .foregroundColor(variant.foreground(enabled));

        if (!enabled)
            return it;

        if (sunken) {
            // Pressed: subtle inset shadow for a tactile push-in feel.
            return it
                    .shadowColor(new Color(0, 0, 0, 55))
                    .shadowBlurRadius(4)
                    .shadowSpreadRadius(0)
                    .shadowOffset(0, 1)
                    .shadowIsInset(true);
        }
        if (!variant.isRaised()) {
            // A quiet control lies flat in whatever it sits on: no shadow to
            // lift it off the surface, and no gradient over a fill it may not
            // even have.
            return it;
        }
        // Resting + hovered share the same shape; hover deepens the shadow.
        int   blur   = rollover ? 8 : 3;
        int   alpha  = rollover ? 55 : 25;
        int   yOff   = rollover ? 2 : 1;
        Color top    = variant.surface(true, false, true);
        return it
                .shadowColor(new Color(0, 0, 0, alpha))
                .shadowBlurRadius(blur)
                .shadowSpreadRadius(-1)
                .shadowOffset(0, yOff)
                .shadowIsInset(false)
                .gradient(g -> g
                        .colors(top, surface)
                        .span(UI.Span.TOP_TO_BOTTOM)
                        .clipTo(UI.ComponentArea.BODY)
                );
    }
}