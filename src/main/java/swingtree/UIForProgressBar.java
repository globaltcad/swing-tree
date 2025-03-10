package swingtree;

import sprouts.Val;

import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JProgressBar} instances.
 *
 * @param <P> The type of {@link JProgressBar} that this {@link UIForProgressBar} is configuring.
 */
public final class UIForProgressBar<P extends JProgressBar> extends UIForAnySwing<UIForProgressBar<P>, P>
{
    private final BuilderState<P> _state;

    /**
     * Extensions of the {@link  UIForAnySwing} always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the component is built.
     */
    UIForProgressBar( BuilderState<P> state ) {
        _state = state;
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }
    
    @Override
    protected UIForProgressBar<P> _newBuilderWithState(BuilderState<P> newState ) {
        return new UIForProgressBar<>(newState);
    }

    /**
     *  Sets a fixed orientation for the slider using the {@link UI.Align} enum.
     *
     *  @param align The orientation constant of the slider.
     *  @return This builder node.
     */
    public final UIForProgressBar<P> withOrientation( UI.Align align ) {
        NullUtil.nullArgCheck( align, "align", UI.Align.class );
        return _with( thisComponent -> {
                    thisComponent.setOrientation(align.forProgressBar());
                })
                ._this();
    }

    /**
     *  Models the orientation of the slider using a {@link Val} property
     *  which allows for dynamic updates to the orientation of the slider.
     *  So when the value of the property changes, the orientation of the slider will be updated accordingly.
     *
     *  @param align The orientation of the slider.
     *  @return This builder node.
     */
    public final UIForProgressBar<P> withOrientation( Val<UI.Align> align ) {
        NullUtil.nullArgCheck( align, "align", Val.class );
        NullUtil.nullPropertyCheck( align, "align", "Null is not a valid alignment" );
        return _withOnShow( align, (thisComponent,v) -> {
                   thisComponent.setOrientation(v.forProgressBar());
               })
               ._with( thisComponent -> {
                   thisComponent.setOrientation(align.orElseThrowUnchecked().forProgressBar());
               })
               ._this();
    }

    /**
     * Sets the minimum value of the slider.
     * For more information see {@link JProgressBar#setMinimum(int)}.
     *
     * @param min The minimum value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForProgressBar<P> withMin( int min ) {
        return _with( thisComponent -> {
                    thisComponent.setMinimum( min );
               })
               ._this();
    }

    /**
     *  Models the minimum value of the slider using a {@link Val} property
     *  which allows for dynamic updates to the min value of the slider.
     *  So when the value of the property changes, the min value of the slider will be updated accordingly.
     *
     * @param min The min property used to dynamically update the min value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code min} is {@code null}.
     */
    public final UIForProgressBar<P> withMin( Val<Integer> min ) {
        NullUtil.nullArgCheck( min, "min", Val.class );
        NullUtil.nullPropertyCheck(min, "min", "Null is not a valid min value for the value of a progress bar.");
        return _withOnShow( min, (c,v) -> {
                    c.setMinimum( v );
               })
               ._with( thisComponent -> {
                    thisComponent.setMinimum( min.orElseThrowUnchecked() );
               })
               ._this();
    }

    /**
     * Sets the maximum value of the progress bar.
     * For more information see {@link JProgressBar#setMaximum(int)} (int)}.
     *
     * @param max The maximum value of the progress bar.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForProgressBar<P> withMax( int max ) {
        return _with( thisComponent -> {
                    thisComponent.setMaximum( max );
               })
               ._this();
    }

    /**
     *  Models the maximum value of the progress bar using a {@link Val} property
     *  so that when the value of the property changes, the max value of the progress bar will be updated accordingly.
     *  For more information see {@link JProgressBar#setMaximum(int)}.
     *
     * @param max An integer property used to dynamically update the max value of the progress bar.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code max} is {@code null}.
     */
    public final UIForProgressBar<P> withMax( Val<Integer> max ) {
        NullUtil.nullArgCheck( max, "max", Val.class );
        NullUtil.nullPropertyCheck(max, "max", "Null is not a valid max value for the value of a progress bar.");
        return _withOnShow( max, (thisComponent,v) -> {
                    thisComponent.setMaximum( v );
               })
               ._with( thisComponent -> {
                   thisComponent.setMaximum( max.orElseThrowUnchecked() );
               })
               ._this();
    }

    /**
     *  Sets the value of the progress bar
     *  through the {@link JProgressBar#setValue(int)} method
     *  of the underlying {@link JProgressBar} type.
     *
     * @param value The value to set for this {@link JProgressBar}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForProgressBar<P> withValue( int value ) {
        return _with( thisComponent -> {
                    thisComponent.setValue( value );
               })
               ._this();
    }

    /**
     *  Use this to specify the current progress value in terms of a double value between 0 and 1,
     *  where 0 represents 0% progress and 1 represents 100% progress.
     *  Note that the actual value of the progress bar will be calculated based on the min and max values
     *  currently specified for the progress bar.
     *
     * @param progress The progress value, a number between 0 and 1, to set for this {@link JProgressBar}.
     *              Note that this will be converted to an integer value between the min and max values.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForProgressBar<P> withProgress( double progress ) {
        if ( progress < 0 || progress > 1 )
            throw new IllegalArgumentException( "Progress value must be between 0 and 1" );
        return _with( thisComponent -> {
                    _setProgress( thisComponent, progress );
               })
               ._this();
    }

    private void _setProgress( P thisComponent, double progress ) {
        int min = thisComponent.getMinimum();
        int max = thisComponent.getMaximum();
        int range = max - min;
        int value = (int) ( min + range * progress );
        thisComponent.setValue( value );
    }

    /**
     *  Allows you to model the progress of the progress bar in terms of an integer based property
     *  which will update the progress bar value whenever it is changed, typically
     *  in your view model or controller.
     *
     * @param val An integer property used to dynamically update the value of the progress bar.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForProgressBar<P> withValue( Val<Integer> val ) {
        NullUtil.nullArgCheck( val, "val", Val.class );
        NullUtil.nullPropertyCheck(val, "value", "Null is not a valid value for the progress property of a progress bar.");
        return _withOnShow( val, (thisComponent,v) -> {
                    thisComponent.setValue( v );
               })
               ._with( thisComponent -> {
                   thisComponent.setValue( val.orElseThrowUnchecked() );
               })
               ._this();
    }

    /**
     *  Allows you to model the progress of the progress bar in terms of a double value between 0 and 1
     *  using a {@link Val} property.
     *  When the value of the property changes, the progress bar will be updated accordingly. <br>
     *  The progress value will be converted to an integer value between the min and max values.
     *
     * @param progress A double property used to dynamically update the value of the progress bar.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForProgressBar<P> withProgress( Val<Double> progress ) {
        NullUtil.nullArgCheck( progress, "progress", Val.class );
        NullUtil.nullPropertyCheck(progress, "progress", "Null is not a valid progress for the progress property of a progress bar.");
        return _withOnShow( progress, (thisComponent,v) -> {
                    _setProgress( thisComponent, v );
               })
               ._with( thisComponent -> {
                   _setProgress( thisComponent, progress.orElseThrowUnchecked() );
               })
               ._this();
    }

}
