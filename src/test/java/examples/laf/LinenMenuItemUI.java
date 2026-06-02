package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuItemUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JMenuItem} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Menu items have a transparent surface that picks up the parent
 *  {@link LinenPopupMenuUI}'s cream, and use a soft
 *  {@link LinenPalette#ACCENT_SOFT} highlight with the dark
 *  {@link LinenPalette#TEXT} foreground when the item is armed (the
 *  pointer is over it or it is being kept open via the keyboard).
 *  Accelerator text is drawn by {@link BasicMenuItemUI} in
 *  {@link LinenPalette#TEXT_MUTED}, matching the calm Linen palette.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenMenuItemUI
        extends    BasicMenuItemUI
        implements SwingTreeStyledComponentUI<JMenuItem>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenMenuItemUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LinenPaint.applyAaHints((Graphics2D) g2);
            super.paint(g2, c);
        });
    }

    @Override
    public void update(Graphics g, JComponent c) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    public ComponentStyleDelegate<JMenuItem> style(ComponentStyleDelegate<JMenuItem> it) {
        return LinenMenuStyles.applyItemStyle(it, it.component());
    }
}

/**
 *  Shared styling primitives for all the menu-item flavours
 *  ({@link LinenMenuItemUI}, {@link LinenMenuUI},
 *  {@link LinenCheckBoxMenuItemUI}, {@link LinenRadioButtonMenuItemUI}).
 *  Package-private; the only reason it's a separate type instead of
 *  inline code is so the same "armed → accent-soft pill" rule can't
 *  drift between the four call sites.
 */
final class LinenMenuStyles
{
    private LinenMenuStyles() {}

    /**
     *  Returns a {@link ComponentStyleDelegate} configured with Linen's
     *  shared menu-item rules — padding, rounded selection pill and
     *  state-appropriate colours.
     */
    static <T extends AbstractButton> ComponentStyleDelegate<T> applyItemStyle(
            ComponentStyleDelegate<T> it, T item)
    {
        ButtonModel m       = item.getModel();
        boolean     enabled = item.isEnabled();
        boolean     armed   = enabled && (m.isArmed() || m.isSelected());

        Color surface = armed ? LinenPalette.ACCENT_SOFT : new Color(0, 0, 0, 0);
        Color text    = enabled ? LinenPalette.TEXT : LinenPalette.TEXT_DISABLED;

        return it
                .padding(4, 8, 4, 8)
                .borderRadius(6)
                .borderWidth(0)
                .backgroundColor(surface)
                .foregroundColor(text);
    }
}