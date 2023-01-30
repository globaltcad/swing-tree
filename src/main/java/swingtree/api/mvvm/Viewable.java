package swingtree.api.mvvm;

/**
 *  A provider of a view which usually is also the view model of the view
 *  it provides.
 */
public interface Viewable
{
    /**
     * @param viewType the type of the view to provide, in case of Swing usually {@link javax.swing.JComponent}.
     * @return the view of the given type.
     * @param <V> the type of the view to provide.
     */
    <V> V createView( Class<V> viewType );
}
