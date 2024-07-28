package swingtree.animation;

/**
 *  Implementations of this interface are used to define when an animation should
 *  continue running and when it should stop based on the current {@link AnimationStatus}
 *  of an {@link Animation}.
 */
@FunctionalInterface
interface RunCondition {

    /**
     *  Checks if the animation should continue running or if it should stop
     *  by examining the given {@link AnimationStatus}.
     *
     * @param status The current {@link AnimationStatus}, i.e. the current progress of the animation.
     * @return {@code true} if the animation should continue running, {@code false} otherwise.
     */
    boolean shouldContinue( AnimationStatus status );

}
