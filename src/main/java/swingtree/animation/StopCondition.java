package swingtree.animation;

@FunctionalInterface
public interface StopCondition {

    boolean check( AnimationState state );

}
