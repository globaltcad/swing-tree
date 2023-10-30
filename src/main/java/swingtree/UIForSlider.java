package swingtree;

import sprouts.Action;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Objects;
import java.util.function.Consumer;

/**
 *  A SwingTree builder node designed for configuring {@link JSlider} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 */
public final class UIForSlider<S extends JSlider> extends UIForAnySwing<UIForSlider<S>, S>
{
    private final BuilderState<S> _state;

    UIForSlider( BuilderState<S> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<S> _state() {
        return _state;
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
                    _setOrientation(thisComponent, align.orElseThrow());
               })
                ._with( thisComponent -> {
                    _setOrientation(thisComponent, align.orElseThrow());
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
                        e -> _doApp(()->action.accept(new ComponentDelegate<>(thisComponent, e)))
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
     * @param min The min property used to dynamically update the min value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code min} is {@code null}.
     */
    public final UIForSlider<S> withMin( Val<Integer> min ) {
        NullUtil.nullArgCheck( min, "min", Val.class );
        return _withOnShow( min, (thisComponent,v) -> {
                    _setMin(thisComponent, min.orElseThrow());
                })
                ._with( thisComponent -> {
                    _setMin(thisComponent, min.orElseThrow());
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
     * @param max An integer property used to dynamically update the max value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code max} is {@code null}.
     */
    public final UIForSlider<S> withMax( Val<Integer> max ) {
        NullUtil.nullArgCheck( max, "max", Val.class );
        return _withOnShow( max, (thisComponent,v) -> {
                    _setMax(thisComponent, max.orElseThrow());
                })
                ._with( thisComponent -> {
                    _setMax(thisComponent, max.orElseThrow());
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
     * @param val An integer property used to dynamically update the value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForSlider<S> withValue( Val<Integer> val ) {
        NullUtil.nullArgCheck( val, "val", Val.class );
        return _withOnShow( val, (thisComponent,v) -> {
                    _setValue(thisComponent, val.orElseThrow());
                })
                ._with( thisComponent -> {
                    _setValue(thisComponent, val.orElseThrow());
                })
                ._this();
    }

    /**
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
                        e -> _doApp(thisComponent.getValue(), newItem -> var.set(From.VIEW, newItem) )
                    );
                    _setValue(thisComponent, var.orElseThrow());
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
                    thisComponent.setMajorTickSpacing( spacing.orElseThrow() );
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
                    thisComponent.setMinorTickSpacing( spacing.orElseThrow() );
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