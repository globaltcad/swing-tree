package swingtree.api.mvvm;

@FunctionalInterface
public interface Action<D> {

    void accept( D delegate );

    default boolean canBeRemoved() { return false; }

}
