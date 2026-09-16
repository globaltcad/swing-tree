package examples.laf;

import swingtree.api.Painter;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

/**
 *  The {@link JScrollPane} UI delegate. The scroll bars are painted by
 *  {@link SwingTreeScrollBarUI}.
 */
public final class SwingTreeScrollPaneUI
        extends    BasicScrollPaneUI
        implements SwingTreeStyledComponentUI<JScrollPane>
{
    private final SwingTreeLookAndFeel.Theme _theme;

    SwingTreeScrollPaneUI( SwingTreeLookAndFeel.Theme theme ) { _theme = theme; }

    public static ComponentUI createUI( JComponent c ) { return new SwingTreeScrollPaneUI(SwingTreeLookAndFeel.installedTheme()); }

    /** The component this scroll pane scrolls, whose focus it repaints on, or null before it has one. */
    private @Nullable Component _trackedView = null;

    /** Follows the component the viewport shows, which an application usually adds after the
     *  scroll pane is built. */
    private final ContainerListener _viewTracker = new ContainerAdapter() {
        @Override public void componentAdded( ContainerEvent e )   { trackView(); }
        @Override public void componentRemoved( ContainerEvent e ) { trackView(); }
    };

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        _theme.installStyleOn(c);
        JViewport viewport = scrollpane.getViewport();
        if ( viewport != null )
            viewport.addContainerListener(_viewTracker);
        trackView();
        installTableBorder();
    }

    /**
     *  Gives a scroll pane around a table the border {@link JTable} gives it. The table does that
     *  itself when it is added and when its own delegate is installed, but a theme switch installs
     *  the table's delegate before this one, whose defaults then replace that border with the
     *  plain scroll pane border. A scroll pane a style rule governs has no Swing border to replace.
     */
    private void installTableBorder() {
        JViewport viewport = scrollpane.getViewport();
        if ( _theme.styles(scrollpane.getClass()) || viewport == null || !(viewport.getView() instanceof JTable) )
            return;
        if ( UIManager.getBorder("Table.scrollPaneBorder") != null )
            LookAndFeel.installBorder(scrollpane, "Table.scrollPaneBorder");
    }

    @Override
    public void uninstallUI( JComponent c ) {
        JViewport viewport = scrollpane.getViewport();
        if ( viewport != null )
            viewport.removeContainerListener(_viewTracker);
        if ( _trackedView != null )
            LafUtilities.uninstallFocusRepaint(scrollpane, _trackedView);
        _trackedView = null;
        super.uninstallUI(c);
    }

    /**
     *  Repaints the scroll pane whenever what it scrolls gains or loses the focus. A look and feel
     *  may draw a focus ring around the scroll pane of a focused text area, as Nimbus does, and a
     *  style is only gathered again when its component repaints.
     */
    private void trackView() {
        JViewport viewport = scrollpane.getViewport();
        Component view     = viewport == null ? null : viewport.getView();
        if ( view == _trackedView )
            return;
        if ( _trackedView != null )
            LafUtilities.uninstallFocusRepaint(scrollpane, _trackedView);
        _trackedView = view;
        if ( view != null )
            LafUtilities.repaintOnFocusChange(scrollpane, view);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, ((JScrollPane) c).getViewportBorder() == null ? Painter.none() : g2 -> super.paint(g2, c));
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JScrollPane> style( ComponentStyleDelegate<JScrollPane> it ) throws Exception {
        return _theme.applyStyle(it);
    }
}
