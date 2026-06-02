package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonMenuItemUI;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JRadioButtonMenuItem} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Same row layout as {@link LinenMenuItemUI}, with the shared
 *  {@link LinenIcons#radio() radio glyph} on the left to indicate
 *  membership in an exclusive group.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenRadioButtonMenuItemUI
        extends    BasicRadioButtonMenuItemUI
        implements SwingTreeStyledComponentUI<JRadioButtonMenuItem>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenRadioButtonMenuItemUI(); }

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
    public ComponentStyleDelegate<JRadioButtonMenuItem> style(ComponentStyleDelegate<JRadioButtonMenuItem> it) {
        return LinenMenuStyles.applyItemStyle(it, it.component());
    }
}