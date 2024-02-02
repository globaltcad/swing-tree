package swingtree.style;

interface CacheProducerAndValidator<T> {
    T produce(StructureConf currentState, ComponentAreas context);
}
