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
     *  by examining the given {@link AnimationStatus}. <br>
     *  Note that this method deliberately requires the handling of checked exceptions
     *  at its invocation sites because there may be any number of implementations
     *  hiding behind this interface and so it is unwise to assume that
     *  all of them will be able to execute gracefully without throwing exceptions.
     *
     * @param status The current {@link AnimationStatus}, i.e. the current progress of the animation.
     * @return {@code true} if the animation should continue running, {@code false} otherwise.
     */
    boolean shouldContinue( AnimationStatus status ) throws Exception;

}
