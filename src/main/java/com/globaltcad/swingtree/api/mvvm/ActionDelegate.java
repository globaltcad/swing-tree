package com.globaltcad.swingtree.api.mvvm;

import java.util.List;

public interface ActionDelegate<T> {

    Var<T> current();

    Val<T> previous();

    default Val<T> previous(int steps) {
        return history().get(history().size() - steps - 1);
    }

    List<Val<T>> history();

}
