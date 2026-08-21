package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;
import java.awt.Color;
import java.awt.Graphics;

/**
 *  The {@link JPanel} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  By default a Linen panel is drawn in the warm cream
 *  {@link LinenPalette#BACKGROUND} colour with a barely visible
 *  <i>stochastic</i> noise overlay painted in
 *  {@link LinenPalette#TEXTURE_LIGHT} / {@link LinenPalette#TEXTURE_DARK}.
 *  The two texture colours are within a few RGB steps of the base background,
 *  so the result reads as a subtle grain — like sun-bleached linen fabric —
 *  rather than as visual noise.
 *  <p>
 *  A real window needs more than one kind of surface, so a panel tagged with a
 *  {@link LinenSurface} is painted as that instead: {@link LinenSurface#CARD} as
 *  a raised, rounded, hairlined sheet with no grain, {@link LinenSurface#RAIL}
 *  as a flat strip of the same colour, and {@link LinenSurface#TRANSPARENT} as
 *  nothing at all. Only the fill and the grain are decided here — padding,
 *  spacing and per-edge borders stay free for the application to set.
 *  <p>
 *  Both the colour and the noise are pure SwingTree style entries, so an
 *  application can suppress the texture entirely from a
 *  {@link swingtree.style.StyleSheet}:
 *  <pre>{@code
 *    add(type(JPanel.class), it -> it.noise(n -> n.colors())); // no colours = no noise
 *  }</pre>
 *  ...or replace it with a different noise function, gradient, or colour.
 *  This class is {@code final}: extension happens through the cascade,
 *  not through subclassing.
 */
public final class LinenPanelUI
        extends    BasicPanelUI
        implements SwingTreeStyledComponentUI<JPanel>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenPanelUI(); }

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
    public ComponentStyleDelegate<JPanel> style(ComponentStyleDelegate<JPanel> it) {
        it = it.foregroundColor(LinenPalette.TEXT);
        switch ( LinenSurface.of(it.component()) ) {
            case CARD:
                return it
                        .backgroundColor(LinenPalette.SURFACE)
                        .borderRadius(14)
                        .border(1, LinenPalette.BORDER_SOFT)
                        .shadowColor(new Color(0x3D, 0x35, 0x2A, 28))
                        .shadowBlurRadius(14)
                        .shadowSpreadRadius(-2)
                        .shadowOffset(0, 3);
            case RAIL:
                return it.backgroundColor(LinenPalette.SURFACE);
            case TRANSPARENT:
                return it.backgroundColor(LinenPalette.TRANSPARENT);
            default:
                return it
                        .backgroundColor(LinenPalette.BACKGROUND)
                        .noise(n -> n
                                .function(UI.NoiseType.STOCHASTIC)
                                .colors(LinenPalette.TEXTURE_LIGHT, LinenPalette.TEXTURE_DARK)
                                .scale(0.6)
                                .clipTo(UI.ComponentArea.BODY)
                        );
        }
    }
}