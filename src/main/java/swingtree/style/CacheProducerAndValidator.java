package swingtree.style;

interface CacheProducerAndValidator<T> {
    T produce(StructureConf currentState, ComponentAreas context);

    boolean leadsToSameValue(StructureConf oldState, StructureConf newState, ComponentAreas context);
}
