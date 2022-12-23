package swingtree.api.mvvm;

public interface ValsDelegate<T>
{
    int index();

    Mutation type();

    Val<T> newValue();

    Val<T> oldValue();
}
