package com.globaltcad.swingtree.api;

@FunctionalInterface
public interface UIAction<D>
{
    void accept(D context);
}
