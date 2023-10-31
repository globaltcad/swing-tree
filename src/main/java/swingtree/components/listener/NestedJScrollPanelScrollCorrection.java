package swingtree.components.listener;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.ref.WeakReference;

/**
 *  This {@link MouseWheelListener} exists to make {@link JScrollPane} scroll
 *  behavior the same as one is used to from a browser,
 *  where the mouse over a scrollable control will scroll
 *  that control until the control bottoms out, and then continues
 *  to scroll the parent {@link JScrollPane}, usually the
 *  {@link JScrollPane} for the whole page.
 *  <p>
 *  This class will do just that by listening to mouse wheel events
 *  on the {@link JScrollPane} and dispatching them to the parent
 *  {@link JScrollPane} when the scrollable control has bottomed out.
 *  <p>
 *  By default, the {@link JScrollPane} consumes all mouse wheel events
 *  and does not dispatch them to the parent {@link JScrollPane}.
 *  Which means that in case of 2 or more nested {@link JScrollPane}s,
 *  where the mouse cursor touches a inner JScrollPane, 
 *  then the scrolling events will only be dispatched to the inner
 *  {@link JScrollPane} and not to the parent {@link JScrollPane}.
 *  That means that scrolling the "parent" JScrollPane stops.
 *  <p>
 *  You may use this class to fix that behavior for your custom 
 *  {@link JScrollPane} objects.
 *  However, in case of the SwingTree native {@link swingtree.UI.ScrollPane}
 *  you don't need to do anything, because it already uses this class
 *  to fix the scrolling behavior.
 */
public class NestedJScrollPanelScrollCorrection implements MouseWheelListener
{
    private final JScrollPane _ownerScrollPane;
    
    private WeakReference<JScrollPane> _parentScrollPane;
    private int _previousValue = 0;


    public NestedJScrollPanelScrollCorrection( JScrollPane owner ) {
        _ownerScrollPane = owner;
    }


    private JScrollPane getParentScrollPane()
    {
        JScrollPane parentScrollPane = _parentScrollPane.get();

        if ( parentScrollPane == null ) {
            Component parent = _ownerScrollPane.getParent();
            while ( !(parent instanceof JScrollPane) && parent != null )
                parent = parent.getParent();

            parentScrollPane = (JScrollPane) parent;
            _parentScrollPane = new WeakReference<>(parentScrollPane);
        }
        return parentScrollPane;
    }

    @Override
    public void mouseWheelMoved( MouseWheelEvent e )
    {
        JScrollBar  bar    = _ownerScrollPane.getVerticalScrollBar();
        JScrollPane parent = getParentScrollPane();

        if ( parent != null )
        {
            /*
               Only dispatch if we have reached top/bottom on previous scroll
            */
            if ( e.getWheelRotation() < 0 ) {
                if ( bar.getValue() == 0 && _previousValue == 0 ) 
                    parent.dispatchEvent(_cloneEvent(e));
            } 
            else 
                if ( bar.getValue() == getMax() && _previousValue == getMax() )
                    parent.dispatchEvent(_cloneEvent(e));
            
            _previousValue = bar.getValue();
        }
        /*
           If parent scroll pane doesn't exist, remove this as a listener.
           We have to defer this till now (vs doing it in constructor)
           because in the constructor this item has no parent yet.
        */
        else
            _ownerScrollPane.removeMouseWheelListener(this);
    }

    private int getMax() {
        JScrollBar bar = _ownerScrollPane.getVerticalScrollBar();
        return bar.getMaximum() - bar.getVisibleAmount();
    }

    private MouseWheelEvent _cloneEvent( MouseWheelEvent e ) {
        return new MouseWheelEvent(
                getParentScrollPane(), e.getID(), e
                .getWhen(), e.getModifiers(), 1, 1, e
                .getClickCount(), false, e.getScrollType(), e
                .getScrollAmount(), e.getWheelRotation()
            );
    }
}
