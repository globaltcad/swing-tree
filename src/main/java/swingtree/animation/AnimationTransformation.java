package swingtree.animation;

/**
 *  Defines an animation in terms of functional transformations
 *  taking in an {@link AnimationStatus} together with a value
 *  and returning an updated value. <br>
 *  The value is expected to be immutable and may be everything
 *  from a simple {@link java.lang.Number} to a complex view model
 *  holding your application state and business logic. <br>
 *  <p>
 *  Implementations of this are designed to be used as
 *  part of the {@link Animatable} class holding the initial
 *  animation state and the {@link LifeTime} determining
 *  when the animation should run.<br>
 *  So you may use this interface to define an {@link Animatable}
 *  like this: <br>
 *  <pre>{@code
 *      return Animatable.of(
 *          LifeTime.of(0.45, TimeUnit.SECONDS), this,
 *          (status, model) -> model.withFontSize((int) (24 + status.pulse() * 16))
 *      );
 *  }
 *  </pre>
 *  An {@link Animatable} may then be passed to
 *  {@link swingtree.UI#animate(sprouts.Var, Animatable)} together
 *  with a {@link sprouts.Var} property to repeatedly transform
 *  the property item during the iterations of the entire animation lifetime.
 *
 * @param <T> The type of the value that is animated
 *           through repeated transformations.
 */
@FunctionalInterface
public interface AnimationTransformation<T>
{
    /**
     *  This takes in the current {@link AnimationStatus} and an immutable
     *  value of type {@code T} and returns a new updated value of type {@code T}.
     *  This may typically be an immutable view model whose fields are bound
     *  to the UI components of your application through property lenses.
     *
     * @param status The current status of the animation.
     * @param value The current value of the animated item.
     * @return The updated value of the animated item based on the current status,
     *          typically the {@link AnimationStatus#progress()} number.
     */
    T run( AnimationStatus status, T value ) throws Exception;

    /**
     *  This method is called after the animation has finished.
     *  The default implementation does nothing but returns the
     *  supplied value. <br>
     *  Implement this to create a cleaned up final version
     *  of the item after the animation has finished. <br>
     *  Note that this method deliberately requires the handling of checked exceptions
     *  at its invocation sites because there may be any number of implementations
     *  hiding behind this interface and so it is unwise to assume that
     *  all of them will be able to execute gracefully without throwing exceptions.
     *
     * @param status The current progress status of the animation, which
     *               is used to determine the intermediate values of
     *               the animated item. You may typically want to use
     *               the {@link AnimationStatus#progress()} number to
     *               interpolate fields of your view model.
     * @param value The final value of the animated item, which is
     *              expected to be an immutable object.
     * @return The final value of the animated item after the animation has finished.
     */
    default T finish( AnimationStatus status, T value ) throws Exception {
        return value;
    }
}
