package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;
import java.awt.Color;
import java.awt.Graphics;

/**
 *  The {@link JToolTip} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Renders a small, rounded callout with a deep-taupe background,
 *  light text and a soft drop shadow — distinct enough from the
 *  application's panels to read as a floating popover. The class
 *  is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenToolTipUI
        extends    BasicToolTipUI
        implements SwingTreeStyledComponentUI<JToolTip>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenToolTipUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LinenPaint.applyAaHints((java.awt.Graphics2D) g2);
            super.paint(g2, c);
        });
    }

    @Override
    public void update(Graphics g, JComponent c) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JToolTip> style(ComponentStyleDelegate<JToolTip> it) {
        return it
                .padding(4, 10, 4, 10)
                .borderRadius(6)
                .borderWidth(0)
                .backgroundColor(LinenPalette.ACCENT)
                .foregroundColor(new Color(0xFA, 0xF6, 0xEC))
                .shadowColor(new Color(0, 0, 0, 70))
                .shadowBlurRadius(8)
                .shadowSpreadRadius(-2)
                .shadowOffset(0, 3)
                .shadowIsInset(false);
    }
}