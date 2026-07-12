package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Action;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *  A SwingTree builder node designed for configuring {@link JSlider} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <S> The type of {@link JSlider} that this {@link UIForSlider} is configuring.
 */
public final class UIForSlider<S extends JSlider> extends UIForAnySwing<UIForSlider<S>, S>
{
    private static final int PREFERRED_STEPS = 256;
    private static final Logger log = LoggerFactory.getLogger(UIForSlider.class);

    private final BuilderState<S> _state;

    UIForSlider( BuilderState<S> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<S> _state() {
        return _state;
    }
    
    @Override
    protected UIForSlider<S> _newBuilderWithState(BuilderState<S> newState ) {
        return new UIForSlider<>(newState);
    }

    /**
     *  Sets the orientation of the slider.
     *  @param align The orientation of the slider.
     *  @return This builder node.
     */
    public final UIForSlider<S> withOrientation( UI.Align align ) {
        NullUtil.nullArgCheck( align, "align", UI.Align.class );
        return _with( thisComponent -> {
                   _setOrientation( thisComponent, align );
               })
               ._this();
    }

    private void _setOrientation( S thisComponent, UI.Align align ) {
        _doWithoutListeners(thisComponent,
            () -> thisComponent.setOrientation(align.forSlider())
        );
    }

    /**
     *  Dynamically sets the orientation of the slider.
     *  @param align The orientation of the slider.
     *  @return This builder node.
     */
    public final UIForSlider<S> withOrientation( Val<UI.Align> align ) {
        NullUtil.nullArgCheck( align, "align", Val.class );
        NullUtil.nullPropertyCheck( align, "align", "Null is not a valid alignment" );
        return _withOnShow( align, (thisComponent,v) -> {
                    _setOrientation(thisComponent, align.orElseThrowUnchecked());
               })
                ._with( thisComponent -> {
                    _setOrientation(thisComponent, align.orElseThrowUnchecked());
                })
               ._this();
    }

    /**
     * Adds an {@link Action} to the underlying {@link JSlider}
     * through an {@link javax.swing.event.ChangeListener},
     * which will be called when the state of the slider changes.
     * For more information see {@link JSlider#addChangeListener(javax.swing.event.ChangeListener)}.
     *
     * @param action The {@link Action} that will be called through the underlying change event.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    public final UIForSlider<S> onChange( Action<ComponentDelegate<JSlider, ChangeEvent>> action ) {
        NullUtil.nullArgCheck( action, "action", Action.class );
        return _with( thisComponent -> {
                    _onChange(thisComponent,
                        e -> _runInApp(()->{
                            try {
                                action.accept(new ComponentDelegate<>(thisComponent, e));
                            } catch (Exception ex) {
                                log.error(SwingTree.get().logMarker(), "Error while executing action on slider change!", ex);
                            }
                        })
                    );
                })
                ._this();
    }

    private void _onChange( S thisComponent, Consumer<ChangeEvent> action ) {
        thisComponent.addChangeListener(action::accept);
    }

    /**
     * Sets the minimum value of the slider.
     * For more information see {@link JSlider#setMinimum(int)}.
     *
     * @param min The minimum value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSlider<S> withMin( int min ) {
        return _with( thisComponent -> {
                    _setMin( thisComponent, min );
                })
                ._this();
    }

    private void _setMin( S thisComponent, int min ) {
        _doWithoutListeners(thisComponent, ()->thisComponent.setMinimum( min ));
    }

    /**
     *  Binds the supplied {@link Val} property to the min value of the slider
     *  so that when the value of the property changes, the min value of the slider will be updated accordingly.
     *  For more information about the underlying value in the component, see {@link JSlider#setMinimum(int)}.
     *
     * @param min The min property used to dynamically update the min value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code min} is {@code null}.
     */
    public final UIForSlider<S> withMin( Val<Integer> min ) {
        NullUtil.nullArgCheck( min, "min", Val.class );
        return _withOnShow( min, (thisComponent,v) -> {
                    _setMin(thisComponent, v); // Note: the captured event value is used, never the property itself, which belongs to the application thread.
                })
                ._with( thisComponent -> {
                    _setMin(thisComponent, min.orElseThrowUnchecked());
                })
                ._this();
    }

    /**
     * Sets the maximum value of the slider.
     * For more information see {@link JSlider#setMaximum(int)} (int)}.
     *
     * @param max The maximum value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSlider<S> withMax( int max ) {
        return _with( thisComponent -> {
                    _setMax( thisComponent, max );
                })
                ._this();
    }

    private void _setMax( S thisComponent, int max ) {
        _doWithoutListeners(thisComponent, ()->thisComponent.setMaximum( max ));
    }

    /**
     *  Binds the supplied {@link Val} property to the max value of the slider.
     *  When the value of the property changes, the max value of the slider will be updated accordingly.
     *  For more information about the underlying value in the component, see {@link JSlider#setMaximum(int)}.
     *
     * @param max An integer property used to dynamically update the max value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code max} is {@code null}.
     */
    public final UIForSlider<S> withMax( Val<Integer> max ) {
        NullUtil.nullArgCheck( max, "max", Val.class );
        return _withOnShow( max, (thisComponent,v) -> {
                    _setMax(thisComponent, v); // Note: the captured event value is used, never the property itself, which belongs to the application thread.
                })
                ._with( thisComponent -> {
                    _setMax(thisComponent, max.orElseThrowUnchecked());
                })
                ._this();
    }

    /**
     * Sets the current value of the slider.
     * For more information see {@link JSlider#setValue(int)}.
     *
     * @param value The current value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSlider<S> withValue( int value ) {
        return _with( thisComponent -> {
                    _setValue( thisComponent, value );
                })
                ._this();
    }

    private void _setValue( S thisComponent, int value ) {
        _doWithoutListeners(thisComponent, ()->thisComponent.setValue( value ));
    }

    /**
     *  Binds the supplied {@link Val} property to the value of the slider,
     *  which causes the knob of the slider to move when the value of the property changes.
     *  But note that the supplied property is a read only, so when the user updates
     *  the value of the slider, the property will not be updated.
     *  Use {@link #withValue(Var)} if you want to bind a property bidirectionally.
     *  For more information about the underlying value in the component, see {@link JSlider#setValue(int)}.
     *
     * @param val An integer property used to dynamically update the value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForSlider<S> withValue( Val<Integer> val ) {
        NullUtil.nullArgCheck( val, "val", Val.class );
        return _withOnShow( val, (thisComponent,v) -> {
                    _setValue(thisComponent, v); // Note: the captured event value is used, never the property itself, which belongs to the application thread.
                })
                ._with( thisComponent -> {
                    _setValue(thisComponent, val.orElseThrowUnchecked());
                })
                ._this();
    }

    final <N extends Number> UIForSlider<S> _withBinding(
        Val<N> userMin, Val<N> userMax, Val<N> userCurrent, boolean biDirectional
    ) {
        Objects.requireNonNull(userMin);
        Objects.requireNonNull(userMax);
        Objects.requireNonNull(userCurrent);
        boolean allOfTheSameType = userMin.type() == userMax.type() &&
                                   userMax.type() == userCurrent.type();

        if ( !allOfTheSameType )
            throw new IllegalArgumentException("Min, max and current slider values must all be of the same type.");

        Class<N> userType = userMin.type();
        boolean isWholeNumber = userType == Integer.class || userType == Long.class || userType == Short.class || userType == Byte.class;
        if ( !isWholeNumber ) {
            // The scale factor depends on both userMin and userMax, so whenever either of them changes,
            // the slider's min, max and value all need to be recomputed together — otherwise the user
            // could drag the knob into a position whose inverse-scaled value falls outside [userMin, userMax].
            return _with( thisComponent -> {
                        // The UI thread must not read the properties directly, because they belong to
                        // the application thread. So the UI thread works with these copies of min, max
                        // and current value, which are refreshed through the property change events,
                        // replayed in their original order.
                        Number[] shown = { userMin.orElseThrowUnchecked(), userMax.orElseThrowUnchecked(), userCurrent.orElseThrowUnchecked() };
                        _onShow( userMin, thisComponent, (c, v) -> {
                            shown[0] = v;
                            _updateScaledSliderRange(c, shown[0], shown[1], shown[2]);
                        });
                        _onShow( userMax, thisComponent, (c, v) -> {
                            shown[1] = v;
                            _updateScaledSliderRange(c, shown[0], shown[1], shown[2]);
                        });
                        _onShow( userCurrent, thisComponent, (c, v) -> {
                            shown[2] = v;
                            _updateScaledSliderRange(c, shown[0], shown[1], shown[2]);
                        });
                        _updateScaledSliderRange(thisComponent, shown[0], shown[1], shown[2]);
                        if ( biDirectional ) {
                            Var<N> target = (Var<N>) userCurrent;
                            _onChange(thisComponent,
                                e -> _runInApp(thisComponent.getValue(), newSliderInt -> {
                                    N minVal = userMin.orElseThrowUnchecked();
                                    N maxVal = userMax.orElseThrowUnchecked();
                                    int minInt = _scale(Integer.class, minVal, minVal, maxVal, false);
                                    int maxInt = _scale(Integer.class, maxVal, minVal, maxVal, false);
                                    N targetItem;
                                    if ( newSliderInt <= minInt )
                                        targetItem = minVal;
                                    else if ( newSliderInt >= maxInt )
                                        targetItem = maxVal;
                                    else
                                        targetItem = _scale(userType, newSliderInt, minVal, maxVal, true);
                                    target.set(From.VIEW, targetItem);
                                })
                            );
                        }
                    })
                    ._this();
        }
        if ( userType != Integer.class )
            return _withBindingInternal(
                    userMin.viewAsInt( n -> _convertTo(Integer.class, n) ),
                    userMax.viewAsInt( n -> _convertTo(Integer.class, n) ),
                    userCurrent.viewAsInt( n -> _convertTo(Integer.class, n) ),
                    biDirectional ? (Var<N>) userCurrent : null,
                    n->_convertTo(userType, n)
                );
        else
            return _withBindingInternal(
                    userMin,
                    userMax,
                    userCurrent,
                    biDirectional ? (Var<N>) userCurrent : null,
                    n->_convertTo(userType, n)
                );
    }

    private void _updateScaledSliderRange(
        S thisComponent, Number minVal, Number maxVal, Number currentVal
    ) {
        int newMin = _scale(Integer.class, minVal, minVal, maxVal, false);
        int newMax = _scale(Integer.class, maxVal, minVal, maxVal, false);
        int newVal = _scale(Integer.class, currentVal, minVal, maxVal, false);
        int currentExtent = thisComponent.getModel().getExtent();
        _doWithoutListeners(thisComponent, () ->
            thisComponent.getModel().setRangeProperties(
                newVal, currentExtent, newMin, newMax, thisComponent.getValueIsAdjusting()
            )
        );
    }

    final <N extends Number, T extends Number> UIForSlider<S> _withBindingInternal(
        Val<N> min, Val<N> max, Val<N> current, @Nullable Var<T> target, Function<Integer,T> scaling
    ) {
        return _withOnShow( min, (thisComponent,v) -> {
                    _setMin(thisComponent, v.intValue()); // Note: the captured event value is used, never the property itself, which belongs to the application thread.
                })
                ._withOnShow( max, (thisComponent,v) -> {
                    _setMax(thisComponent, v.intValue());
                })
                ._withOnShow( current, (thisComponent,v) -> {
                    _setValue(thisComponent, v.intValue());
                })
                ._with( thisComponent -> {
                    _setMin(thisComponent, min.orElseThrowUnchecked().intValue());
                    _setMax(thisComponent, max.orElseThrowUnchecked().intValue());
                    _setValue(thisComponent, current.orElseThrowUnchecked().intValue());
                    if ( target != null ) {
                        _onChange(thisComponent,
                            e -> _runInApp(thisComponent.getValue(), newItem -> {
                                T targetItem = scaling.apply(newItem);
                                target.set(From.VIEW, targetItem);
                            })
                        );
                    }
                })
                ._this();
    }

    private <N extends Number> N _scale( Class<N> target, Number in, Number min, Number max, boolean inverse ) {
        double scale = _goodScaleFor(min, max);
        if ( inverse )
            scale = 1.0 / scale;
        double newValue = in.doubleValue() * scale;
        // No we convert the new value to the target type.
        return _convertTo(target, newValue);
    }

    private <N extends Number> N _convertTo( Class<N> target, Number in ) {
        if ( target == Integer.class )
            return target.cast(in.intValue());
        if ( target == Long.class )
            return target.cast(in.longValue());
        if ( target == Short.class )
            return target.cast(in.shortValue());
        if ( target == Byte.class )
            return target.cast(in.byteValue());
        if ( target == Float.class )
            return target.cast(in.floatValue());
        if ( target == Double.class )
            return target.cast(in.doubleValue());
        throw new IllegalArgumentException("Unsupported number type: " + target);
    }


    private <N extends Number> double _goodScaleFor( N min, N max ) {
        double minVal = min.doubleValue();
        double maxVal = max.doubleValue();
        double diff = maxVal - minVal;
        // The scale should ensure that we have at least PREFERRED_STEPS steps in integer values.
        return PREFERRED_STEPS / diff;
    }

    /**
     *  Use this to bind the supplied {@link Var} property to the value of the slider.
     *  When the value of the slider changes, the value of the {@link Var} will be updated
     *  and when the item of the {@link Var} is changed as part of the application logic,
     *  the value of the slider will be updated accordingly.
     *
     * @param var An integer property used to dynamically update the value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForSlider<S> withValue( Var<Integer> var ) {
        NullUtil.nullArgCheck( var, "var", Var.class );
        return _withOnShow( var, (thisComponent,v) -> {
                    _setValue(thisComponent, v);
                })
                ._with( thisComponent -> {
                    _onChange(thisComponent,
                        e -> _runInApp(thisComponent.getValue(), newItem -> var.set(From.VIEW, newItem) )
                    );
                    _setValue(thisComponent, var.orElseThrowUnchecked());
                })
                ._this();
    }

    /**
     * Sets the major tick spacing of the slider.
     * For more information see {@link JSlider#setMajorTickSpacing(int)}.
     *
     * @param spacing The major tick spacing of the slider.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSlider<S> withMajorTickSpacing( int spacing ) {
        return _with( thisComponent -> {
                    thisComponent.setMajorTickSpacing( spacing );
                })
                ._this();
    }

    /**
     * Sets the minor tick spacing of the slider.
     * For more information see {@link JSlider#setMinorTickSpacing(int)}.
     *
     * @param spacing The minor tick spacing of the slider.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSlider<S> withMinorTickSpacing( int spacing ) {
        return _with( thisComponent -> {
                    thisComponent.setMinorTickSpacing( spacing );
                })
                ._this();
    }

    /**
     * Dynamically sets the major tick spacing of the slider.
     * For more information see {@link JSlider#setMajorTickSpacing(int)}.
     * @param spacing The major tick spacing of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code spacing} is {@code null}.
     */
    public final UIForSlider<S> withMajorTickSpacing( Val<Integer> spacing ) {
        NullUtil.nullArgCheck( spacing, "spacing", Val.class );
        NullUtil.nullPropertyCheck( spacing, "spacing" );
        return _withOnShow( spacing, (thisComponent,v) -> {
                    thisComponent.setMajorTickSpacing(v);
                })
                ._with( thisComponent -> {
                    thisComponent.setMajorTickSpacing( spacing.orElseThrowUnchecked() );
                })
                ._this();
    }

    /**
     * Dynamically sets the minor tick spacing of the slider.
     * For more information see {@link JSlider#setMinorTickSpacing(int)}.
     * @param spacing The minor tick spacing of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code spacing} is {@code null}.
     */
    public final UIForSlider<S> withMinorTickSpacing( Val<Integer> spacing ) {
        NullUtil.nullArgCheck( spacing, "spacing", Val.class );
        NullUtil.nullPropertyCheck( spacing, "spacing" );
        return _withOnShow( spacing, (thisComponent,v) -> {
                    thisComponent.setMinorTickSpacing(v);
                })
                ._with( thisComponent -> {
                    thisComponent.setMinorTickSpacing( spacing.orElseThrowUnchecked() );
                })
                ._this();
    }


    private void _doWithoutListeners( S thisComponent, Runnable someTask ) {
        // We need to first remove the change listener, otherwise we might trigger unwanted events.
        ChangeListener[] listeners = thisComponent.getChangeListeners();
        for ( ChangeListener listener : listeners )
            thisComponent.removeChangeListener( listener );

        someTask.run();

        // Now we can add the listeners back.
        for ( ChangeListener listener : listeners )
            thisComponent.addChangeListener( listener );
    }

}