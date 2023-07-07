package swingtree.api.mvvm;

/**
 *  A provider of a view which is expected to be the view model of the view
 *  it provides.
 */
@Deprecated
public interface Viewable
{
    /**
     *  Provides a view of the given type for this view model.
     *  <p>
     *  Here a simple example demonstrating how a typical implementation of this
     *  method should look like in a pure swing application: <br>
     *  <pre>{@code
     *      //Override
     *      public <V> V createView(Class<V> viewType) {
     * 			return viewType.cast(new ViewOfMyViewModel(this));
     *      }
     * }</pre>
     * In an application with multiple view technologies, this method should
     * return the view of the given type, if it is supported by the view model. <br>
     * Here an example for a view model which supports both Swing and JavaFX:   <br>
     * <pre>{@code
     *    //Override
     *    public <V> V createView(Class<V> viewType) {
     *          if ( viewType == JComponent.class )
     *              return viewType.cast(new SwingViewOfMyViewModel(this));
     *          else if ( viewType == Node.class )
     *              return viewType.cast(new JavaFXViewOfMyViewModel(this));
     *          else
     *              throw new IllegalArgumentException("Unsupported view type: " + viewType);
     *    }
     * }</pre>
     *
     * @param viewType the type of the view to provide, in case of Swing usually {@link javax.swing.JComponent}.
     * @return the view of the given type.
     * @param <V> the type of the view to provide.
     */
    <V> V createView( Class<V> viewType );
}
