package swingtree;

import sprouts.Action;
import sprouts.Val;
import sprouts.Var;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.function.Consumer;

/**
 *  A swing tree builder node for {@link JSlider} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 */
public class UIForSlider<S extends JSlider> extends UIForAnySwing<UIForSlider<S>, S>
{
    protected UIForSlider( S component ) { super(component); }

    /**
     *  Sets the orientation of the slider.
     *  @param align The orientation of the slider.
     *  @return This builder node.
     */
    public final UIForSlider<S> with( UI.Align align ) {
        NullUtil.nullArgCheck( align, "align", UI.Align.class );
        _doWithoutListeners(()->getComponent().setOrientation(align.forSlider()));
        return this;
    }

    /**
     *  Dynamically sets the orientation of the slider.
     *  @param align The orientation of the slider.
     *  @return This builder node.
     */
    public final UIForSlider<S> withAlignment( Val<UI.Align> align ) {
        NullUtil.nullArgCheck( align, "align", Val.class );
        NullUtil.nullPropertyCheck( align, "align", "Null is not a valid alignment" );
        _onShow( align, v -> with(align.orElseThrow()) );
        return this;
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
        S slider = getComponent();
        _onChange( e -> _doApp(()->action.accept(new ComponentDelegate<>(slider, e, this::getSiblinghood))) );
        return this;
    }

    private void _onChange( Consumer<ChangeEvent> action ) {
        getComponent().addChangeListener(action::accept);
    }

    /**
     * Sets the minimum value of the slider.
     * For more information see {@link JSlider#setMinimum(int)}.
     *
     * @param min The minimum value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSlider<S> withMin( int min ) {
        _doWithoutListeners(()->getComponent().setMinimum( min ));
        return this;
    }

    /**
     * @param min The min property used to dynamically update the min value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code min} is {@code null}.
     */
    public final UIForSlider<S> withMin( Val<Integer> min ) {
        NullUtil.nullArgCheck( min, "min", Val.class );
        _onShow( min, this::withMin);
        return this;
    }

    /**
     * Sets the maximum value of the slider.
     * For more information see {@link JSlider#setMaximum(int)} (int)}.
     *
     * @param max The maximum value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSlider<S> withMax( int max ) {
        _doWithoutListeners(()->getComponent().setMaximum( max ));
        return this;
    }

    /**
     * @param max An integer property used to dynamically update the max value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code max} is {@code null}.
     */
    public final UIForSlider<S> withMax( Val<Integer> max ) {
        NullUtil.nullArgCheck( max, "max", Val.class );
        _onShow( max, this::withMax);
        return this;
    }

    /**
     * Sets the current value of the slider.
     * For more information see {@link JSlider#setValue(int)}.
     *
     * @param value The current value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSlider<S> withValue( int value ) {
        _doWithoutListeners(()->getComponent().setValue( value ));
        return this;
    }

    /**
     * @param val An integer property used to dynamically update the value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForSlider<S> withValue( Val<Integer> val ) {
        NullUtil.nullArgCheck( val, "val", Val.class );
        _onShow( val, this::withValue );
        getComponent().setValue( val.orElseThrow() );
        return this;
    }

    /**
     * @param var An integer property used to dynamically update the value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForSlider<S> withValue( Var<Integer> var ) {
        NullUtil.nullArgCheck( var, "var", Var.class );
        _onChange( e -> _doApp(getComponent().getValue(), var::act) );
        return this.withValue( (Val<Integer>) var );
    }

    /**
     * Sets the major tick spacing of the slider.
     * For more information see {@link JSlider#setMajorTickSpacing(int)}.
     *
     * @param spacing The major tick spacing of the slider.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSlider<S> withMajorTickSpacing( int spacing ) {
        getComponent().setMajorTickSpacing( spacing );
        return this;
    }

    /**
     * Sets the minor tick spacing of the slider.
     * For more information see {@link JSlider#setMinorTickSpacing(int)}.
     *
     * @param spacing The minor tick spacing of the slider.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSlider<S> withMinorTickSpacing( int spacing ) {
        getComponent().setMinorTickSpacing( spacing );
        return this;
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
        _onShow( spacing, v -> getComponent().setMajorTickSpacing(v) );
        return this;
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
        _onShow( spacing, v -> getComponent().setMinorTickSpacing(v) );
        return this;
    }


    private void _doWithoutListeners(Runnable someTask) {
        // We need to first remove the change listener, otherwise we might trigger unwanted events.
        ChangeListener[] listeners = getComponent().getChangeListeners();
        for ( ChangeListener listener : listeners )
            getComponent().removeChangeListener( listener );

        someTask.run();

        // Now we can add the listeners back.
        for ( ChangeListener listener : listeners )
            getComponent().addChangeListener( listener );
    }

}