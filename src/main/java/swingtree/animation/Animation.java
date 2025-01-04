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
     * @param <T> The type of the value that is animated.
     *            This is typically a value object that models the state of the animation
     *            in terms of the properties that change over time.
     */
    static <T> Animation of( Var<T> target, AnimationTransformation<T> animator )
    {
        return new Animation() {
            @Override
            public void run( AnimationStatus status ) {
                T newValue = target.get();
                try {
                    newValue = animator.run(status, newValue);
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                } finally {
                    target.set( newValue );
                }
            }
            @Override
            public void finish( AnimationStatus status ) {
                T newValue = target.get();
                try {
                    newValue = animator.finish(status, newValue);
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                } finally {
                    target.set( newValue );
                }
            }
        };
    }

    /**
     *  This method is called repeatedly during the lifetime of the animation.
     *  The {@link AnimationStatus} contains information about the current state of the animation. <br>
     *  Note that this method deliberately requires the handling of checked exceptions
     *  at its invocation sites because there may be any number of implementations
     *  hiding behind this interface and so it is unwise to assume that
     *  all of them will be able to execute gracefully without throwing exceptions.
     *
     * @param status The current state of the animation.
     * @throws Exception If during the execution of this method an error is raised.
     *                   <b>Due to this being a generic interface, the likelihood of
     *                   exceptions being thrown is high and so it is recommended
     *                   to handle them at the invocation site.</b>
     */
    void run( AnimationStatus status ) throws Exception;

    /**
     *  This method is called after the animation has finished.
     *  The default implementation does nothing.
     *  Use this to clean up the state of your components
     *  any used resources after the animation has finished. <br>
     *  Note that this method deliberately requires the handling of checked exceptions
     *  at its invocation sites because there may be any number of implementations
     *  hiding behind this interface and so it is unwise to assume that
     *  all of them will be able to execute gracefully without throwing exceptions.
     *
     * @param status The current state of the animation.
     * @throws Exception If during the execution of this method an error is raised.
     *                   <b>Due to this being a generic interface, the likelihood of
     *                   exceptions being thrown is high and so it is recommended
     *                   to handle them at the invocation site.</b>
     */
    default void finish( AnimationStatus status ) throws Exception {
        /* Override this method to perform cleanup after the animation has finished */
    }
}
