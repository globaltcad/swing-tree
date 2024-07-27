package swingtree.animation;

import org.jspecify.annotations.Nullable;
import sprouts.Var;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 *  This defines what is needed for animating an immutable
 *  value type {@code T} through a transformation function
 *  {@link AnimationFor} and a {@link LifeTime} determining
 *  when the animation should run.
 *  It is essentially just an immutable wrapper for {@code T},
 *  {@link AnimationFor} and a {@link LifeTime}.
 *  <p>
 *  An {@link Animatable} is designed to be provided by an immutable
 *  view model to animate its properties using the {@link swingtree.UI#animate(Var, Animatable)}
 *  or {@link swingtree.UI#animate(Var, Function)} methods. <br>
 *  Where the {@link Var} property is used as the mutable animation state
 *  and the animatable defines how the property item should be transformed
 *  repeatedly during the iterations of the entire animation lifetime.
 *
 * @param <T> The type of the value that is continuously transformed
 *           by the {@link AnimationFor}.
 */
public final class Animatable<T>
{
    private static final Animatable<?> _NONE = new Animatable<>(LifeTime.none(), null, (state, value) -> value);

    private final LifeTime        _lifeTime;
    private final @Nullable T     _initialValue;
    private final AnimationFor<T> _animation;

    /**
     *  Returns an {@link Animatable} instance that does nothing
     *  when run due to a {@link LifeTime} of {@link LifeTime#none()}.
     *  This is equivalent to a no-op and may also be used instead
     *  of {@code null} in cases where an {@link Animatable} is expected.
     *
     * @param <T> The type of the value that is animated.
     * @return An {@link Animatable} instance that does nothing.
     */
    public static <T> Animatable<T> none() {
        return (Animatable<T>) _NONE;
    }

    /**
     *  Returns an {@link Animatable} instance that merely holds the
     *  supplied value and does nothing when run due to a no-op
     *  transformation function and a {@link LifeTime} of {@link LifeTime#none()}. <br>
     *  You can think of this as an instantaneous animation to the
     *  specified value as the first and final state of the "animation".
     *
     * @param value The value that should be used for setting the new application/animator state.
     * @param <T> The type of the new animation/application state.
     * @return An {@link Animatable} instance that only holds a single animation state,
     *        which is the supplied value.
     * @throws NullPointerException If the supplied value is {@code null}.
     */
    public static <T> Animatable<T> of( T value ) {
        Objects.requireNonNull(value);
        return of( LifeTime.none(), value, (state, v) -> v );
    }

    /**
     *  Returns an {@link Animatable} instance that animates the supplied
     *  value through the provided {@link AnimationFor} function during
     *  the specified {@link LifeTime} starting with the supplied initial value
     *  as initial application/animation state.
     *
     * @param lifeTime The lifetime of the animation.
     * @param initialValue The initial value of the animation.
     * @param animation The transformation function that is called repeatedly
     *                  during the lifetime of the animation.
     * @param <T> The type of the value that is animated.
     * @return An {@link Animatable} instance that animates the supplied value.
     * @throws NullPointerException If any of the arguments is {@code null}.
     * @see #of(LifeTime, AnimationFor) for an {@link Animatable} without an initial value.
     */
    public static <T> Animatable<T> of(
        LifeTime        lifeTime,
        T               initialValue,
        AnimationFor<T> animation
    ) {
        Objects.requireNonNull(lifeTime);
        Objects.requireNonNull(animation);
        Objects.requireNonNull(initialValue);
        return new Animatable<>(lifeTime, initialValue, animation);
    }

    /**
     *  Returns an {@link Animatable} instance that is used to
     *  transform {@code T} values using the supplied {@link AnimationFor} function
     *  during the specified {@link LifeTime}.
     *
     * @param lifeTime The lifetime of the animation.
     * @param animation The transformation function that is called repeatedly
     *                  during the lifetime of the animation.
     * @param <T> The type of the value that is animated.
     * @return An {@link Animatable} instance that animates the supplied value.
     * @throws NullPointerException If any of the arguments is {@code null}.
     * @see #of(LifeTime, Object, AnimationFor) for an {@link Animatable} with an initial value.
     */
    public static <T> Animatable<T> of(
        LifeTime        lifeTime,
        AnimationFor<T> animation
    ) {
        Objects.requireNonNull(lifeTime);
        Objects.requireNonNull(animation);
        return new Animatable<>(lifeTime, null, animation);
    }

    private Animatable(
        LifeTime        lifeTime,
        @Nullable T     initialValue,
        AnimationFor<T> animation
    ) {
        _lifeTime     = lifeTime;
        _initialValue = initialValue;
        _animation    = animation;
    }

    /**
     *  Returns the {@link LifeTime} of the animation,
     *  which is used by {@link swingtree.UI#animate(Var, Animatable)}
     *  to determine for how long the animation should run.
     *
     * @return The {@link LifeTime} of the animation.
     */
    public LifeTime lifeTime() {
        return _lifeTime;
    }

    /**
     *  Returns the initial value of the animation
     *  or an empty {@link Optional} if no initial value is set.
     *  This is the value that is used as the initial state
     *  of the animation before the first transformation is applied.<br>
     *  If there is no initial value set, the result of the first
     *  transformation is used as the initial state of the animation.
     *
     * @return The initial value of the animation state.
     * @see #of(LifeTime, AnimationFor) for an {@link Animatable} without an initial value.
     * @see #of(LifeTime, Object, AnimationFor) for an {@link Animatable} with an initial value.
     */
    public Optional<T> initialState() {
        return Optional.ofNullable(_initialValue);
    }

    /**
     *  Returns the transformation function that is called repeatedly
     *  during the lifetime of the animation to transform the item
     *  of a {@link Var} property when passed to the
     *  {@link swingtree.UI#animate(Var, Animatable)} method.
     *
     * @return The transformation function of the animation.
     */
    public AnimationFor<T> animator() {
        return _animation;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                "lifeTime="     + _lifeTime     + ", " +
                "initialValue=" + _initialValue + ", " +
                "animation="    + _animation    +
            "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(_lifeTime, _initialValue, _animation);
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null || getClass() != obj.getClass() )
            return false;
        Animatable<?> other = (Animatable<?>) obj;
        return Objects.equals(_lifeTime,     other._lifeTime    ) &&
               Objects.equals(_initialValue, other._initialValue) &&
               Objects.equals(_animation,    other._animation   );
    }
}
