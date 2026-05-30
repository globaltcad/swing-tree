package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuUI;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JMenu} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  A {@code JMenu} appears in two contexts:
 *  <ul>
 *      <li>As a <b>top-level entry</b> on a {@link javax.swing.JMenuBar}
 *          — drawn as a flat label with a {@link LinenPalette#ACCENT_SOFT}
 *          highlight when armed or its popup is open.</li>
 *      <li>As a <b>sub-menu entry</b> inside another popup — same as a
 *          regular {@link LinenMenuItemUI}, with the standard
 *          {@code Menu.arrowIcon} chevron on the right hinting that
 *          children expand from it.</li>
 *  </ul>
 *  Both cases share the styling rule in {@link LinenMenuStyles#applyItemStyle}.
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenMenuUI
        extends    BasicMenuUI
        implements SwingTreeStyledComponentUI<JMenu>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenMenuUI(); }

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
    public ComponentStyleDelegate<JMenu> style(ComponentStyleDelegate<JMenu> it) {
        return LinenMenuStyles.applyItemStyle(it, it.component());
    }
}