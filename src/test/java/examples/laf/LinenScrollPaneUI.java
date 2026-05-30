package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;
import java.awt.Graphics;

/**
 *  The {@link JScrollPane} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Linen scroll panes are framed by a soft, rounded border in
 *  {@link LinenPalette#BORDER} and a small inner padding so the
 *  enclosed viewport isn't flush against the edge. All scrolling
 *  visuals live on the {@link LinenScrollBarUI} side.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenScrollPaneUI
        extends    BasicScrollPaneUI
        implements SwingTreeStyledComponentUI<JScrollPane>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenScrollPaneUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ((JScrollPane) c).setBorder(null); // we draw the frame via withStyle
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> super.paint(g2, c));
    }

    @Override
    public void update(Graphics g, JComponent c) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JScrollPane> style(ComponentStyleDelegate<JScrollPane> it) {
        return it
                .backgroundColor(LinenPalette.SURFACE_FIELD)
                .foregroundColor(LinenPalette.TEXT)
                .borderRadius(8)
                .borderWidth(1)
                .borderColor(LinenPalette.BORDER)
                .padding(2);
    }
}