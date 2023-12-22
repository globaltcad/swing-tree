package swingtree.style;

interface CacheProducerAndValidator<T> {
    T produce(ComponentConf currentState, ComponentAreas context);

    boolean leadsToSameValue(ComponentConf oldState, ComponentConf newState, ComponentAreas context);
}
