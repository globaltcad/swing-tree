package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicListUI;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** The {@link JList} UI delegate. */
public final class SwingTreeListUI
        extends    BasicListUI
        implements SwingTreeStyledComponentUI<JList<?>>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeListUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> {
            paintSelectionBands(g2, (JList<?>) c);
            super.paint(g2, c);
        });
    }

    /**
     *  Fills a band behind each selected entry.
     *  <p>
     *  Swing has a cell renderer arrive wearing the list's selection colour, which the style
     *  engine cannot reproduce: one renderer instance stands in for every entry, and the engine
     *  keys a gathered style on the component, so a colour chosen for one entry lands on whichever
     *  entry is painted next. The list knows which entries are selected and is painted once, so
     *  the band is filled here and the renderers, which are not opaque, are painted over it.
     */
    private static void paintSelectionBands( Graphics2D g, JList<?> list ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            return; // Swing's own renderer is carrying the selection colour
        int[] selected = list.getSelectedIndices();
        if ( selected.length == 0 )
            return;
        g.setColor(SwingTreeLookAndFeel.palette().accentSoft());
        for ( int index : selected ) {
            Rectangle band = list.getCellBounds(index, index);
            if ( band != null )
                g.fillRect(0, band.y, list.getWidth(), band.height);
        }
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JList<?>> style( ComponentStyleDelegate<JList<?>> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}
