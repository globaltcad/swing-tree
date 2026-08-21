package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.Color;
import java.awt.Graphics;

/**
 *  The {@link JTextField} UI delegate of the {@link LinenLookAndFeel}.
 *
 *  <h2>Visual behaviour</h2>
 *  <ul>
 *      <li><b>Resting</b> — a slightly cooler cream
 *          ({@link LinenPalette#SURFACE_FIELD}) with a 1&nbsp;dev-px taupe
 *          border, gently rounded corners and comfortable inner padding.</li>
 *      <li><b>Focused</b> — the border thickens to 2&nbsp;dev-px and switches
 *          to {@link LinenPalette#ACCENT}; a faint accent-tinted glow appears
 *          underneath, indicating the active field without being noisy.</li>
 *      <li><b>Disabled / non-editable</b> — text fades, surface dims to
 *          {@link LinenPalette#SURFACE_DISABLED}.</li>
 *  </ul>
 *
 *  <p>The non-zero {@code borderRadius} causes the SwingTree style engine
 *  to mark the component as non-opaque, which in turn suppresses Swing's
 *  default rectangular background fill — so the rounded shape and the
 *  focus glow are rendered without rectangular bleed-through.
 *
 *  <p>The class is {@code final}; tweak via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenTextFieldUI
        extends    BasicTextFieldUI
        implements SwingTreeStyledComponentUI<JTextField>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenTextFieldUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
        LinenFocus.repaintOnFocus(c, c); // text components don't self-repaint on focus
        LinenSelection.repaintOnSelectionChange((javax.swing.text.JTextComponent) c); // ...nor enough of themselves on a selection change
    }

    @Override
    public void uninstallUI(JComponent c) {
        LinenSelection.uninstall((javax.swing.text.JTextComponent) c);
        LinenFocus.uninstall(c, c);
        super.uninstallUI(c);
    }

    /**
     *  {@link javax.swing.plaf.basic.BasicTextUI#paint(Graphics, JComponent)}
     *  is {@code final}, so the engine is wired into {@link #update} instead —
     *  but the text-painting pass is still triggered by <b>calling</b> that
     *  final {@code paint(..)}, never {@code paintSafely(..)} directly.
     *  <p>
     *  The difference is not cosmetic. {@code BasicTextUI.paint(..)} takes the
     *  document's read lock around {@code paintSafely(..)} — that lock is the
     *  documented guarantee "that the model won't change from the view of this
     *  thread while it's rendering" — and it skips painting entirely while the
     *  root view has no child yet. Calling {@code paintSafely(..)} straight
     *  gives up both: a document mutated while the view hierarchy is being
     *  rendered makes the view ask for text that is no longer there, and Swing
     *  answers with {@code StateInvariantError: Can't render: p0,p1}. Measured
     *  on a wrapped text area with a writer on another thread: 192 such errors
     *  in 400 paints without the lock, none with it.
     */
    @Override
    public void update(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LinenPaint.applyAaHints((java.awt.Graphics2D) g2);
            super.paint(g2, c);
        });
    }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    public ComponentStyleDelegate<JTextField> style(ComponentStyleDelegate<JTextField> it) {
        JTextField f = it.component();

        boolean editable = f.isEnabled() && f.isEditable();
        boolean focused  = editable && f.isFocusOwner();

        Color   surface    = editable ? LinenPalette.SURFACE_FIELD : LinenPalette.SURFACE_DISABLED;
        Color   foreground = f.isEnabled() ? LinenPalette.TEXT : LinenPalette.TEXT_DISABLED;
        Color   border     = focused ? LinenPalette.ACCENT : LinenPalette.BORDER;
        double  borderW    = focused ? 2 : 1;
        // See LinenButtonUI: shrinking the margin by the same delta the
        // border grows keeps the overall footprint constant on focus.
        double  margin     = focused ? 0 : 1;

        it = it
                .margin(margin)
                .padding(5, 9, 5, 9)
                .borderRadius(7)
                .borderWidth(borderW)
                .borderColor(border)
                .backgroundColor(surface)
                .foregroundColor(foreground);

        if (focused) {
            // A faint accent-tinted glow that "breathes" under the field.
            it = it
                    .shadowColor(new Color(LinenPalette.ACCENT.getRed(),
                                           LinenPalette.ACCENT.getGreen(),
                                           LinenPalette.ACCENT.getBlue(), 70))
                    .shadowBlurRadius(6)
                    .shadowSpreadRadius(0)
                    .shadowOffset(0, 0)
                    .shadowIsInset(false);
        }
        return it;
    }
}