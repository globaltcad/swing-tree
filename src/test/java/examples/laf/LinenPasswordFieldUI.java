package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import java.awt.Color;
import java.awt.Graphics;

/**
 *  The {@link JPasswordField} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Visually identical to {@link LinenTextFieldUI} — same rounded surface,
 *  same accent-coloured focus glow — but the underlying
 *  {@link BasicPasswordFieldUI} substitutes echo characters for the typed
 *  text, so passwords aren't accidentally rendered. The class is
 *  {@code final}; customise via stylesheet or inline {@code .withStyle(..)}.
 */
public final class LinenPasswordFieldUI
        extends    BasicPasswordFieldUI
        implements SwingTreeStyledComponentUI<JPasswordField>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenPasswordFieldUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override
    public void update(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LinenPaint.applyAaHints((java.awt.Graphics2D) g2);
            paintSafely(g2);
        });
    }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    public ComponentStyleDelegate<JPasswordField> style(ComponentStyleDelegate<JPasswordField> it) {
        JPasswordField f = it.component();
        boolean editable = f.isEnabled() && f.isEditable();
        boolean focused  = editable && f.isFocusOwner();

        Color   surface    = editable ? LinenPalette.SURFACE_FIELD : LinenPalette.SURFACE_DISABLED;
        Color   foreground = f.isEnabled() ? LinenPalette.TEXT : LinenPalette.TEXT_DISABLED;
        Color   border     = focused ? LinenPalette.ACCENT : LinenPalette.BORDER;
        double  borderW    = focused ? 2 : 1;
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