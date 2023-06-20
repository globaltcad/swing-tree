package swingtree.animation;

/**
 * An animation is a function which is called repeatedly during its lifetime,
 * which is determined by a {@link LifeTime} and a {@link StopCondition}.
 */
@FunctionalInterface
public interface Animation
{
    /**
     *  This method is called repeatedly during the lifetime of the animation.
     *  The {@link AnimationState} contains information about the current state of the animation.
     *
     * @param state The current state of the animation.
     */
    void run( AnimationState state );

    /**
     *  This method is called after the animation has finished.
     *  The default implementation does nothing.
     *  Use this to clean up the state of your components
     *  any used resources after the animation has finished.
     *
     * @param state The current state of the animation.
     */
    default void finish( AnimationState state ) {
        /* Override this method to perform cleanup after the animation has finished */
    }
}
