package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSeparatorUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JSeparator} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Renders a single hairline in {@link LinenPalette#BORDER_SOFT} across
 *  the full length of the separator — horizontal or vertical depending
 *  on {@link JSeparator#getOrientation()}. The stroke is one component
 *  pixel wide, multiplied by the current
 *  {@linkplain UI#scale() UI scale factor} so the line stays crisp on
 *  HiDPI displays.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenSeparatorUI
        extends    BasicSeparatorUI
        implements SwingTreeStyledComponentUI<JSeparator>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenSeparatorUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> drawHairline((Graphics2D) g2, (JSeparator) c));
    }

    @Override
    public void update(Graphics g, JComponent c) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        int thickness = Math.max(1, UI.scale(1));
        return ((JSeparator) c).getOrientation() == SwingConstants.VERTICAL
                ? new Dimension(thickness, 0)
                : new Dimension(0, thickness);
    }

    @Override
    public ComponentStyleDelegate<JSeparator> style(ComponentStyleDelegate<JSeparator> it) {
        return it.backgroundColor(LinenPalette.BORDER_SOFT)
                 .foregroundColor(LinenPalette.BORDER_SOFT);
    }

    private static void drawHairline(Graphics2D g, JSeparator c) {
        int thickness = Math.max(1, UI.scale(1));
        g.setColor(LinenPalette.BORDER_SOFT);
        if (c.getOrientation() == SwingConstants.VERTICAL)
            g.fillRect(0, 0, thickness, c.getHeight());
        else
            g.fillRect(0, 0, c.getWidth(), thickness);
    }
}