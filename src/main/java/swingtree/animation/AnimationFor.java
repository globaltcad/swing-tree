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
 *
 * @param <T> The type of the value that is animated
 *           through repeated transformations.
 */
@FunctionalInterface
public interface AnimationFor<T>
{
    T run( AnimationStatus status, T value );

    default T finish(AnimationStatus state, T value ) {
        return value;
    }
}
