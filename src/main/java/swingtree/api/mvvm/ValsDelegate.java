package swingtree.api.mvvm;

/**
 *  A context object passed to the various types of change listeners registered
 *  in property lists such as {@link Vals} and {@link Vars}.
 *  It provides helpful context information about the change, such as the
 *  previous and current value of the property, the index of the property
 *  in the list, and type of change.
 *
 * @param <T> The type of the value wrapped by the delegated property...
 */
public interface ValsDelegate<T>
{
    /**
     *  @return The mutation type of the change which may be one of the following:
     *          <ul>
     *              <li>{@link Change#ADD}</li>
     *              <li>{@link Change#REMOVE}</li>
     *              <li>{@link Change#SET}</li>
     *              <li>{@link Change#SORT}</li>
     *              <li>{@link Change#CLEAR}</li>
     *              <li>{@link Change#DISTINCT}</li>
     *              <li>{@link Change#NONE}</li>
     *          </ul>
     */
    Change changeType();

    /**
     *  @return The index at which a list mutation took place or -1
     *          if the change does not involve a particular index, like a list clear or a list sort.
     */
    int index();

    /**
     * @return The previous value of the property or an empty property if the change does not involve a previous value.
     */
    Val<T> oldValue();

    /**
     * @return The current value of the property or an empty property if the change does not involve a current value.
     */
    Val<T> newValue();

}
