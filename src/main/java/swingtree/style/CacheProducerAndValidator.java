package swingtree.style;

interface CacheProducerAndValidator<T> {
    T produce(BoxModelConf currentState, ComponentAreas context);
}
