package swingtree.animation;

/**
 *  Implementations of this interface are used to define when an animation should stop
 *  running based on the current {@linkn AnimationState}.
 */
@FunctionalInterface
public interface StopCondition {

    /**
     *  Checks if the animation should stop running based on the current {@link AnimationState}.
     *
     * @param state The current {@link AnimationState}.
     * @return {@code true} if the animation should stop running, {@code false} otherwise.
     */
    boolean check( AnimationState state );

}
