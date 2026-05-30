package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuBarUI;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JMenuBar} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  A flat horizontal strip in {@link LinenPalette#SURFACE} sitting on
 *  top of a hairline {@link LinenPalette#BORDER_SOFT} separator —
 *  visually distinct from the window background without overwhelming
 *  it. Top-level menu items are styled by {@link LinenMenuUI}.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenMenuBarUI
        extends    BasicMenuBarUI
        implements SwingTreeStyledComponentUI<JMenuBar>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenMenuBarUI(); }

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
    public ComponentStyleDelegate<JMenuBar> style(ComponentStyleDelegate<JMenuBar> it) {
        return it
                .backgroundColor(LinenPalette.SURFACE)
                .foregroundColor(LinenPalette.TEXT)
                .padding(2, 4, 2, 4)
                .borderAt(UI.Edge.BOTTOM, 1, LinenPalette.BORDER_SOFT);
    }
}