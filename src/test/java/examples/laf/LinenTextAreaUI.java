package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextAreaUI;
import java.awt.Color;
import java.awt.Graphics;

/**
 *  The {@link JTextArea} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Text areas are usually mounted inside a {@link javax.swing.JScrollPane};
 *  in that arrangement we want the area itself to render the field
 *  surface (cream + soft border) while the scroll pane carries the
 *  outer frame. Linen therefore styles the area the same way as a
 *  single-line text field: rounded background, focus accent, padding.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenTextAreaUI
        extends    BasicTextAreaUI
        implements SwingTreeStyledComponentUI<JTextArea>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenTextAreaUI(); }

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
     *  The text-painting pass goes through the {@code final}
     *  {@link javax.swing.plaf.basic.BasicTextUI#paint(Graphics, JComponent)}
     *  rather than through {@code paintSafely(..)} directly, because that is
     *  what takes the document's read lock while the view renders. See
     *  {@link LinenTextFieldUI#update(Graphics, JComponent)} for why it matters.
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
    public ComponentStyleDelegate<JTextArea> style(ComponentStyleDelegate<JTextArea> it) {
        JTextArea a = it.component();
        boolean editable = a.isEnabled() && a.isEditable();
        boolean focused  = editable && a.isFocusOwner();

        Color  surface    = editable ? LinenPalette.SURFACE_FIELD : LinenPalette.SURFACE_DISABLED;
        Color  foreground = a.isEnabled() ? LinenPalette.TEXT : LinenPalette.TEXT_DISABLED;
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