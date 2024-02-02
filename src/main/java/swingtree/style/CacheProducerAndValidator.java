package swingtree.style;

interface CacheProducerAndValidator<T> {
    T produce(RenderConf currentState, ComponentAreas context);

    boolean leadsToSameValue(RenderConf oldState, RenderConf newState, ComponentAreas context);
}
