package swingtree.api;

import swingtree.UI;

import javax.swing.*;

/**
 *  A supplier of a boolean telling the {@link javax.swing.JScrollPane} if its content
 *  should always fit one of its viewport dimensions.
 *  Implementations of this are designed to be passed as lambda expressions to the scroll pane configurator API
 *  at {@link UI#scrollPane(Configurator)} using
 *  {@link swingtree.ScrollableComponentDelegate#fitHeight(FitViewportSupplier)}
 *  or {@link swingtree.ScrollableComponentDelegate#fitHeight(FitViewportSupplier)}. <br>
 *  Internally this translates to implementations of the {@link Scrollable}s interface
 *  and its methods {@link Scrollable#getScrollableTracksViewportWidth()} and
 *  {@link Scrollable#getScrollableTracksViewportHeight()}
 */
@FunctionalInterface
public interface FitViewportSupplier {

    boolean shouldFit( JViewport viewport, JComponent viewedContent );

}
