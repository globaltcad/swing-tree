package swingtree;

import sprouts.Val;

import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JProgressBar} instances.
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
    protected UIForProgressBar<P> _with( BuilderState<P> newState ) {
        return new UIForProgressBar<>(newState);
    }

    /**
     *  Sets the orientation of the slider.
     *  @param align The orientation of the slider.
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
     *  Dynamically sets the orientation of the slider.
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
                   thisComponent.setOrientation(align.orElseThrow().forProgressBar());
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
                    thisComponent.setMinimum( min.orElseThrow() );
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
                   thisComponent.setMaximum( max.orElseThrow() );
               })
               ._this();
    }

    /**
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
                   thisComponent.setValue( val.orElseThrow() );
               })
               ._this();
    }

    /**
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
                   _setProgress( thisComponent, progress.orElseThrow() );
               })
               ._this();
    }

}
