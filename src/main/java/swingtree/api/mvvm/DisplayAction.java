package swingtree.api.mvvm;

@FunctionalInterface
public interface DisplayAction<T> {

    void display( ActionDelegate<T> delegate );

    default boolean canBeRemoved() { return false; }

}