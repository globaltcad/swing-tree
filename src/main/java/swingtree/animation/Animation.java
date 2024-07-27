package swingtree.animation;

import sprouts.Var;

/**
 * An animation is a function which is called repeatedly during its lifetime,
 * which is determined by a {@link LifeTime} and a {@link RunCondition}.
 */
@FunctionalInterface
public interface Animation
{
    /**
     *  Creates an {@link Animation} that animates the item
     *  of the supplied {@link Var} property in a transformative way,
     *  instead of producing side effects on the property item itself.
     *
     * @param target The value that is animated.
     * @param animator A function taking in the current animation status and
     *                 a value to be transformed and returned based in the status.
     * @return The created {@link Animation}.
     */
    static <T> Animation of( Var<T> target, AnimationFor<T> animator )
    {
        return new Animation() {
            @Override
            public void run(AnimationState state) {
                T newValue = animator.run(state, target.get());
                target.set( newValue );
            }
            @Override
            public void finish(AnimationState state) {
                T newValue = animator.finish(state, target.get());
                target.set( newValue );
            }
        };
    }

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
