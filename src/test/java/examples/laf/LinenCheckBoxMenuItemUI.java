package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JCheckBoxMenuItem} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Same painting rule as {@link LinenMenuItemUI} — flat row with an
 *  {@link LinenPalette#ACCENT_SOFT} highlight when armed — augmented by
 *  the shared {@link LinenIcons#checkBox() check glyph} on the left.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenCheckBoxMenuItemUI
        extends    BasicCheckBoxMenuItemUI
        implements SwingTreeStyledComponentUI<JCheckBoxMenuItem>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenCheckBoxMenuItemUI(); }

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
    public ComponentStyleDelegate<JCheckBoxMenuItem> style(ComponentStyleDelegate<JCheckBoxMenuItem> it) {
        return LinenMenuStyles.applyItemStyle(it, it.component());
    }
}