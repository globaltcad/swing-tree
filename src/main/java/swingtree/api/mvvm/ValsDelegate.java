package swingtree.api.mvvm;

import java.util.Optional;

public interface ValsDelegate<T>
{
    int index();

    Mutation type();

    Optional<Val<T>> value();
}
