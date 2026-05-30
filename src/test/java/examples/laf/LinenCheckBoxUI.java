package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import java.awt.Color;
import java.awt.Graphics;

/**
 *  The {@link javax.swing.JCheckBox} UI delegate of the
 *  {@link LinenLookAndFeel}.
 *  <p>
 *  Unlike a {@link javax.swing.JButton}, a check box reserves all of its
 *  visual identity for the <i>glyph</i> next to its label — the surrounding
 *  surface is transparent so the parent's cream texture shows through.
 *  The glyph itself is rendered by {@link LinenIcons#checkBox()}, which
 *  reads the button model state and paints a rounded square with a
 *  thick accent-coloured tick when selected.
 *  <p>
 *  The class is {@code final}; override per check box via
 *  {@code .withStyle(..)} or globally with a
 *  {@link swingtree.style.StyleSheet} entry on {@code type(JCheckBox.class)}.
 */
public final class LinenCheckBoxUI
        extends    BasicCheckBoxUI
        implements SwingTreeStyledComponentUI<AbstractButton>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenCheckBoxUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        AbstractButton b = (AbstractButton) c;
        b.setContentAreaFilled(false);
        b.setRolloverEnabled(true);
        b.setFocusPainted(false);
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
    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    public ComponentStyleDelegate<AbstractButton> style(ComponentStyleDelegate<AbstractButton> it) {
        AbstractButton b = it.component();
        Color fg = b.isEnabled() ? LinenPalette.TEXT : LinenPalette.TEXT_DISABLED;
        return it
                .backgroundColor(new Color(0, 0, 0, 0))
                .foregroundColor(fg)
                .padding(2, 4, 2, 4);
    }
}