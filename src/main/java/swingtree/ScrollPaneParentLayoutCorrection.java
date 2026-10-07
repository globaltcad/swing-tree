package swingtree;

import org.jspecify.annotations.Nullable;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.Optional;

/**
 *  This class exists to make a {@link JScrollPane} grow and shrink together with its
 *  content, the same way any other container in a layout does.
 *  <p>
 *  A {@link JScrollPane} is a "validate root", which means that a {@link Component#revalidate()}
 *  of anything inside of it lays out the scroll pane, but never the parent of the scroll pane.
 *  So when the content of a scroll pane grows, Swing marks every ancestor of the scroll pane
 *  as invalid, but it only lays out the scroll pane itself. The scroll pane keeps its old size
 *  even if there is room for a bigger one, and then suddenly jumps to its new size much later,
 *  the next time something else in the window is revalidated.
 *  <p>
 *  This class corrects that by listening for new sizes of the view inside the {@link JViewport}
 *  of the scroll pane. Swing delivers a new size as a resize event after the layout which caused
 *  it has finished, which is when this class calls {@link Component#revalidate()} on the parent
 *  of the scroll pane, so that the parent lays out the scroll pane at its new preferred size.
 *  It keeps listening when the view or the viewport is replaced, and it does nothing for a
 *  scroll pane which is not a validate root, because Swing already lays out its parent.
 *  <p>
 *  The view is also resized whenever the parent gives the scroll pane a new size,
 *  for example on every step of resizing the window. Then the parent has just been laid out,
 *  and laying it out again would double the cost of resizing the window. So this class only
 *  revalidates the parent when the preferred size of the scroll pane changed since the last time,
 *  because only a new preferred size can give the scroll pane a different place in its parent.
 *  <p>
 *  The SwingTree native {@link UI.ScrollPane} installs this class on itself.
 */
final class ScrollPaneParentLayoutCorrection
{
    private final JScrollPane _ownerScrollPane;
    private @Nullable Dimension _preferredSizeAtLastRevalidation;
    private final ComponentAdapter _revalidateParentWhenTheViewIsResized = new ComponentAdapter() {
        @Override public void componentResized( ComponentEvent e ) {
            if ( !_ownerScrollPane.isValidateRoot() )
                return;
            Dimension preferredSize = _ownerScrollPane.getPreferredSize();
            if ( preferredSize.equals(_preferredSizeAtLastRevalidation) )
                return;
            _preferredSizeAtLastRevalidation = preferredSize;
            Optional.ofNullable(_ownerScrollPane.getParent()).ifPresent(Component::revalidate);
        }
    };
    private final ContainerAdapter _followTheViewWhenItIsReplaced = new ContainerAdapter() {
        @Override public void componentAdded( ContainerEvent e ) { _followTheSizeOf(e.getChild()); }
        @Override public void componentRemoved( ContainerEvent e ) { e.getChild().removeComponentListener(_revalidateParentWhenTheViewIsResized); }
    };

    static void installOn( JScrollPane scrollPane ) {
        new ScrollPaneParentLayoutCorrection(scrollPane);
    }

    private ScrollPaneParentLayoutCorrection( JScrollPane scrollPane ) {
        _ownerScrollPane = scrollPane;
        scrollPane.addPropertyChangeListener("viewport", e -> {
            if ( e.getOldValue() instanceof JViewport )
                _stopFollowingTheViewOf((JViewport) e.getOldValue());
            if ( e.getNewValue() instanceof JViewport )
                _followTheViewOf((JViewport) e.getNewValue());
        });
        if ( scrollPane.getViewport() != null )
            _followTheViewOf(scrollPane.getViewport());
    }

    private void _followTheViewOf( JViewport viewport ) {
        viewport.addContainerListener(_followTheViewWhenItIsReplaced);
        if ( viewport.getView() != null )
            _followTheSizeOf(viewport.getView());
    }

    private void _stopFollowingTheViewOf( JViewport viewport ) {
        viewport.removeContainerListener(_followTheViewWhenItIsReplaced);
        if ( viewport.getView() != null )
            viewport.getView().removeComponentListener(_revalidateParentWhenTheViewIsResized);
    }

    private void _followTheSizeOf( Component view ) {
        view.removeComponentListener(_revalidateParentWhenTheViewIsResized);
        view.addComponentListener(_revalidateParentWhenTheViewIsResized);
    }
}
