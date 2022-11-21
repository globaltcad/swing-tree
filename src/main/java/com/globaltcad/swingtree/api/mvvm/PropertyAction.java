package com.globaltcad.swingtree.api.mvvm;

@FunctionalInterface
public interface PropertyAction<T> {

    void act(ActionDelegate<T> delegate);

}
