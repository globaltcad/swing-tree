package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicListUI;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *  The {@link JList} UI delegate. Entries are rendered by whatever cell renderer the list
 *  carries; the band behind a selected entry is filled here.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 */
public final class SwingTreeListUI
        extends    BasicListUI
        implements SwingTreeStyledComponentUI<JList<?>>
{
    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeListUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LafPaint.applyAaHints((Graphics2D) g2);
            paintSelectionBands((Graphics2D) g2, (JList<?>) c);
            super.paint(g2, c);
        });
    }

    /**
     *  Fills a band behind each selected entry.
     *  <p>
     *  Swing's own mechanism - a cell renderer that arrives wearing the list's selection colour -
     *  cannot work under a look and feel backed by the style engine: one renderer instance stands
     *  in for every row, while the engine keys a component's gathered style and its rendered
     *  layers on the component, so a per-row colour decided in the renderer's delegate lands on
     *  whichever row happens to be painted next. The list, on the other hand, knows exactly which
     *  rows are selected and is painted once, so the band belongs here. The renderers on top are
     *  not opaque, so the band shows through them.
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
