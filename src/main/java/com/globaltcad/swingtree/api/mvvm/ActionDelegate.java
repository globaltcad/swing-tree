package com.globaltcad.swingtree.api.mvvm;

import java.util.List;
import java.util.Optional;

/**
 *  Implementations of this are passed to {@link PropertyAction} lambdas
 *  exposing certain context information to the lambda, like the current
 *  value of the property, the previous one as well as access to a limit
 *  list of historic values.
 *
 * @param <T> The type of the value wrapped by a given property...
 */
public interface ActionDelegate<T> {

    /**
     * @return The current property.
     */
    Val<T> current();

    /**
     * @return The current property.
     */
    default Val<T> getCurrent() { return current(); }

    /**
     * @return The previous property.
     */
    Val<T> previous();

    /**
     * @return The previous property.
     */
    default Val<T> getPrevious() { return previous(); }

    /**
     * @param index The index of the historic value to retrieve (increment to go further back in time).
     * @return An {@link Optional} of the historic property.
     */
    default Optional<Val<T>> previous(int index) {
        if ( index == 0 ) return Optional.of(current());
        if ( index <  0 ) throw new IllegalArgumentException("The number of steps must be positive");
        index--;
        // The index is accessing the history in reverse order
        // because the last element is the latest historic value, so the previous one!
        List<Val<T>> history = history();
        if ( index >= history.size() ) return Optional.empty();
        return Optional.of(history().get(index));
    }

    /**
     * @param index The index of the historic value to retrieve (increment to go further back in time).
     * @return An {@link Optional} of the historic property.
     */
    default Optional<Val<T>> getPrevious(int index) { return previous(index); }

    /**
     * @param time The index of the historic value to retrieve (decrement to go further back in time).
     * @return An {@link Optional} of the historic property.
     */
    default Optional<Val<T>> get( int time ) { return previous(-time); }

    /**
     * @return The list of historic values.
     */
    List<Val<T>> history();

}
