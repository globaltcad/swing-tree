package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextPaneUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JTextPane} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  {@code JTextPane} is the styled-document cousin of {@link javax.swing.JTextArea};
 *  Linen treats them visually identically (cream surface, rounded corners,
 *  growing accent border on focus), letting the styled document itself
 *  control colour and font of individual runs of text. The class is
 *  {@code final}; customise via stylesheet or inline {@code .withStyle(..)}.
 */
public final class LinenTextPaneUI
        extends    BasicTextPaneUI
        implements SwingTreeStyledComponentUI<JTextPane>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenTextPaneUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
        LinenFocus.repaintOnFocus(c, c); // text components don't self-repaint on focus
    }

    @Override
    public void uninstallUI(JComponent c) {
        LinenFocus.uninstall(c, c);
        super.uninstallUI(c);
    }

    @Override
    public void update(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LinenPaint.applyAaHints((Graphics2D) g2);
            paintSafely(g2);
        });
    }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    public ComponentStyleDelegate<JTextPane> style(ComponentStyleDelegate<JTextPane> it) {
        JTextPane p = it.component();
        boolean editable = p.isEnabled() && p.isEditable();
        boolean focused  = editable && p.isFocusOwner();

        Color  surface    = editable ? LinenPalette.SURFACE_FIELD : LinenPalette.SURFACE_DISABLED;
        Color  foreground = p.isEnabled() ? LinenPalette.TEXT : LinenPalette.TEXT_DISABLED;
        Color  border     = focused ? LinenPalette.ACCENT : LinenPalette.BORDER;
        double borderW    = focused ? 2 : 1;
        double margin     = focused ? 0 : 1;

        return it
                .margin(margin)
                .padding(6, 9, 6, 9)
                .borderRadius(7)
                .borderWidth(borderW)
                .borderColor(border)
                .backgroundColor(surface)
                .foregroundColor(foreground);
    }
}