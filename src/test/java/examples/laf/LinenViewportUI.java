package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicViewportUI;
import java.awt.Graphics;

/**
 *  The {@link JViewport} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  A viewport is a near-invisible window onto a scroll pane's child; it
 *  exists mostly to clip and translate. Linen gives it the cream field
 *  surface so that the area behind a viewport (visible when the child
 *  doesn't fill it) matches the framing of the enclosing
 *  {@link LinenScrollPaneUI}.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenViewportUI
        extends    BasicViewportUI
        implements SwingTreeStyledComponentUI<JViewport>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenViewportUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
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
    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    public ComponentStyleDelegate<JViewport> style(ComponentStyleDelegate<JViewport> it) {
        it = it.foregroundColor(LinenPalette.TEXT);
        switch ( LinenSurface.of(it.component()) ) {
            case TRANSPARENT: return it.backgroundColor(LinenPalette.TRANSPARENT);
            case CARD:
            case RAIL:        return it.backgroundColor(LinenPalette.SURFACE);
            default:          return it.backgroundColor(LinenPalette.SURFACE_FIELD);
        }
    }
}