package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JPopupMenu} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Renders the popup as a rounded {@link LinenPalette#SURFACE_FIELD}
 *  panel with a soft drop shadow so it visibly lifts off whatever lives
 *  underneath. The individual menu items inside are painted by
 *  {@link LinenMenuItemUI} / {@link LinenCheckBoxMenuItemUI} /
 *  {@link LinenRadioButtonMenuItemUI}.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenPopupMenuUI
        extends    BasicPopupMenuUI
        implements SwingTreeStyledComponentUI<JPopupMenu>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenPopupMenuUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ((JPopupMenu) c).setBorder(null); // SwingTree paints the frame
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
    public ComponentStyleDelegate<JPopupMenu> style(ComponentStyleDelegate<JPopupMenu> it) {
        return it
                .backgroundColor(LinenPalette.SURFACE_FIELD)
                .foregroundColor(LinenPalette.TEXT)
                .padding(4)
                .borderRadius(8)
                .borderWidth(1)
                .borderColor(LinenPalette.BORDER)
                .shadowColor(new Color(0, 0, 0, 70))
                .shadowBlurRadius(10)
                .shadowSpreadRadius(-2)
                .shadowOffset(0, 3)
                .shadowIsInset(false);
    }
}