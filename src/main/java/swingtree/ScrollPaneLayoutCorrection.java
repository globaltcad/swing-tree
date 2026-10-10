package swingtree;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *  This class corrects two gaps in how Swing lays out a {@link JScrollPane} and its surroundings.
 *  <p>
 *  <b>The parent of the scroll pane:</b>
 *  This class makes a {@link JScrollPane} grow and shrink together with its
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
 *  <b>The content of the scroll pane:</b>
 *  A component styled with {@code TextConf.autoPreferredHeight(true)} prefers the height of its
 *  wrapped text at its width, so its preferred height is known only after a layout gave it its width.
 *  SwingTree's components therefore measure their text in their {@code doLayout()}, while their
 *  parent is still being laid out. A {@link Component#revalidate()} called at that moment has no effect,
 *  because Swing marks the parent as laid out when its layout finishes, and Swing paints right after
 *  the layout. Without a correction, the component would be painted once at the height which belongs
 *  to its old width, and laid out at its new height only in a later task on Swing's event thread.
 *  <p>
 *  This class corrects that in {@link #validateTree(Runnable)}, which {@link UI.ScrollPane} calls from
 *  its {@link java.awt.Container#validateTree()}. Swing calls that method both when it lays out the
 *  scroll pane itself and when it lays out a container around it, and in both cases before it paints.
 *  While the scroll pane is being laid out, this class keeps a list in the client property
 *  {@link #COMPONENTS_WITH_NEW_TEXT_HEIGHT} of the scroll pane, to which a component inside it adds itself
 *  when its preferred height changed (see {@code ComponentBackend.measureTextHeightAfterLayoutOf}).
 *  After the layout, this class invalidates the listed components and lays out the scroll pane again,
 *  at most 3 times, so that a height which keeps changing the width (through a scroll bar)
 *  cannot repeat the layout forever.
 *  <p>
 *  The SwingTree native {@link UI.ScrollPane} installs this class on itself.
 */
final class ScrollPaneLayoutCorrection
{
    /** The key of the client property; {@code ComponentBackend} uses the same string to find the list. */
    static final String COMPONENTS_WITH_NEW_TEXT_HEIGHT = "swingtree.componentsWithNewTextHeight";

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

    static ScrollPaneLayoutCorrection installOn( JScrollPane scrollPane ) {
        return new ScrollPaneLayoutCorrection(scrollPane);
    }

    private ScrollPaneLayoutCorrection( JScrollPane scrollPane ) {
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

    void validateTree( Runnable validateTree ) {
        List<JComponent> changed = new ArrayList<>();
        _ownerScrollPane.putClientProperty(COMPONENTS_WITH_NEW_TEXT_HEIGHT, changed);
        try {
            validateTree.run();
            for ( int pass = 0; pass < 3 && !changed.isEmpty(); pass++ ) {
                List<JComponent> again = new ArrayList<>(changed);
                changed.clear();
                again.forEach(Component::invalidate);
                validateTree.run();
            }
        } finally {
            _ownerScrollPane.putClientProperty(COMPONENTS_WITH_NEW_TEXT_HEIGHT, null);
        }
    }
}
