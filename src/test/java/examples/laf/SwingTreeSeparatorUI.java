package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSeparatorUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** The {@link JSeparator} UI delegate: one hairline as thick as the symbol set asks for. */
public final class SwingTreeSeparatorUI
        extends    BasicSeparatorUI
        implements SwingTreeStyledComponentUI<JSeparator>
{
    private final SwingTreeLookAndFeel.Theme _theme;

    SwingTreeSeparatorUI( SwingTreeLookAndFeel.Theme theme ) { _theme = theme; }

    public static ComponentUI createUI( JComponent c ) { return new SwingTreeSeparatorUI(SwingTreeLookAndFeel.installedTheme()); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        _theme.installStyleOn(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> {
            if ( _theme.symbols().drawsItsOwnChrome() )
                drawHairline(g2, (JSeparator) c);
            else
                super.paint(g2, c);
        });
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public Dimension getPreferredSize( JComponent c ) {
        if ( !_theme.symbols().drawsItsOwnChrome() )
            return super.getPreferredSize(c);
        int thickness = thickness();
        return ((JSeparator) c).getOrientation() == SwingConstants.VERTICAL
                ? new Dimension(thickness, 0)
                : new Dimension(0, thickness);
    }

    @Override
    public ComponentStyleDelegate<JSeparator> style( ComponentStyleDelegate<JSeparator> it ) throws Exception {
        return _theme.applyStyle(it);
    }

    private int thickness() {
        return Math.max(1, UI.scale(_theme.symbols().separatorThickness()));
    }

    /** Down or across the middle of whatever box a layout gave it, rather than along its near edge:
     *  a tool bar hands a separator a box several pixels wide, and a line drawn at the edge of that
     *  box sits against the control beside it instead of between the two. A margin takes part of
     *  that box away, so the middle it is centred in is the box the margin leaves. */
    private void drawHairline( Graphics2D g, JSeparator separator ) {
        int       thickness = thickness();
        Rectangle box       = LafUtilities.marginBoxOf(separator);
        g.setColor(_theme.palette().borderSoft());
        if ( separator.getOrientation() == SwingConstants.VERTICAL )
            g.fillRect(box.x + ( box.width - thickness ) / 2, box.y, thickness, box.height);
        else
            g.fillRect(box.x, box.y + ( box.height - thickness ) / 2, box.width, thickness);
    }
}
