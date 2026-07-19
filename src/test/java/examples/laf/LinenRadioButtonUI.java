package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;
import java.awt.Color;
import java.awt.Graphics;

/**
 *  The {@link javax.swing.JRadioButton} UI delegate of the
 *  {@link LinenLookAndFeel}.
 *  <p>
 *  Mirrors {@link LinenCheckBoxUI} but uses {@link LinenIcons#radio()} as
 *  the glyph — an outlined circle with a smaller accent-coloured disc
 *  when selected. The surface is transparent so the parent's texture
 *  shows through unbroken.
 *  <p>
 *  The class is {@code final}; override per radio via
 *  {@code .withStyle(..)} or globally with a
 *  {@link swingtree.style.StyleSheet} entry on {@code type(JRadioButton.class)}.
 */
public final class LinenRadioButtonUI
        extends    BasicRadioButtonUI
        implements SwingTreeStyledComponentUI<AbstractButton>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenRadioButtonUI(); }

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
    // Swing UI delegates only ever paint on the Event Dispatch Thread, so the
    // inherited synchronization is not needed here.
    @SuppressWarnings("UnsynchronizedOverridesSynchronized")
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