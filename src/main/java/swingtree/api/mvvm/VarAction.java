package swingtree.api.mvvm;

/**
 *  A {@link VarAction} can be registered on a {@link Var} to be executed
 *  when the value of the property is changed by the UI to which it is bound.
 *  The {@link VarAction} is passed an {@link ValDelegate} which
 *  provides read-only access to the current and previous value of the property as well
 *  as a list of historic values.
 *
 * @param <T> The type of the value wrapped by a given property...
 */
@FunctionalInterface
public interface VarAction<T> {

    /**
     *    This method is called when the value of the property is changed by the UI.
     *
     * @param delegate The {@link ValDelegate} providing access to the current
     *                 and previous value of the property as well as a list of historic values.
     */
    void act(ValDelegate<T> delegate);

}
