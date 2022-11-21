package com.globaltcad.swingtree.api.mvvm;

import java.util.List;
import java.util.Optional;

public interface ActionDelegate<T> {

    Var<T> current();
    
    default Var<T> getCurrent() { return current(); }

    Val<T> previous();
    
    default Val<T> getPrevious() { return previous(); }

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
    
    default Optional<Val<T>> getPrevious(int steps) { return previous(steps); }
    
    default Optional<Val<T>> get( int steps ) { return previous(-steps); }

    List<Val<T>> history();

}
