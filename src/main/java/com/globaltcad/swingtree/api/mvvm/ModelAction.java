package com.globaltcad.swingtree.api.mvvm;

@FunctionalInterface
public interface ModelAction<T> {

    void act(ActionDelegate<T> delegate);

}
