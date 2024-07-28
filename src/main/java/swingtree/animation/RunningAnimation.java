package swingtree.animation;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Component;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 *  Runs an {@link Animation} on a {@link Component} according to a {@link LifeSpan} and a {@link RunCondition}.
 */
final class RunningAnimation
{
    private final @Nullable WeakReference<Component> _compRef;
    private final LifeSpan      _lifeSpan;
    private final Stride        _stride;
    private final RunCondition  _condition;
    private final Animation     _animation;
    private final AtomicLong    _currentRepeat = new AtomicLong(0);


    RunningAnimation(
        @Nullable Component component, // may be null if the animation is not associated with a specific component
        LifeSpan            lifeSpan,
        Stride              stride,
        RunCondition        condition,
        Animation           animation
    ) {
        _compRef   = component == null ? null : new WeakReference<>(component);
        _lifeSpan  = Objects.requireNonNull(lifeSpan);
        _stride    = Objects.requireNonNull(stride);
        _condition = Objects.requireNonNull(condition);
        _animation = Objects.requireNonNull(animation);
    }

    public LifeSpan lifeSpan() {
        return _lifeSpan;
    }

    public Optional<JComponent> component() {
        if ( _compRef == null ) return Optional.empty();
        Component _component = this._compRef.get();
        return Optional.ofNullable( _component instanceof JComponent ? (JComponent) _component : null );
    }

    public @Nullable WeakReference<Component> compRef() {
        return _compRef;
    }

    public Stride stride() {
        return _stride;
    }

    public RunCondition condition() {
        return _condition;
    }

    public Animation animation() {
        return _animation;
    }

    public long currentRepeat() {
        return _currentRepeat.get();
    }

    public void setCurrentRepeat( long repeat ) {
        _currentRepeat.set(repeat);
    }

}
