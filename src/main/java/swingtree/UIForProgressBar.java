package swingtree;

import sprouts.Val;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JProgressBar} instances.
 */
public class UIForProgressBar<P extends JProgressBar> extends UIForAnySwing<UIForProgressBar<P>, P>
{
    /**
     * Extensions of the {@link  UIForAnySwing} always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    public UIForProgressBar( P component ) {
        super(component);
    }

    /**
     *  Sets the orientation of the slider.
     *  @param align The orientation of the slider.
     *  @return This builder node.
     */
    public final UIForProgressBar<P> with( UI.Align align ) {
        NullUtil.nullArgCheck( align, "align", UI.Align.class );
        getComponent().setOrientation(align.forProgressBar());
        return this;
    }

    /**
     *  Dynamically sets the orientation of the slider.
     *  @param align The orientation of the slider.
     *  @return This builder node.
     */
    public final UIForProgressBar<P> withAlignment( Val<UI.Align> align ) {
        NullUtil.nullArgCheck( align, "align", Val.class );
        NullUtil.nullPropertyCheck( align, "align", "Null is not a valid alignment" );
        _onShow( align, v -> with(align.orElseThrow()) );
        return with(align.orElseThrow());
    }

    /**
     * Sets the minimum value of the slider.
     * For more information see {@link JProgressBar#setMinimum(int)}.
     *
     * @param min The minimum value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForProgressBar<P> withMin( int min ) {
        getComponent().setMinimum( min );
        return this;
    }

    /**
     * @param min The min property used to dynamically update the min value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code min} is {@code null}.
     */
    public final UIForProgressBar<P> withMin( Val<Integer> min ) {
        NullUtil.nullArgCheck( min, "min", Val.class );
        NullUtil.nullPropertyCheck(min, "min", "Null is not a valid min value for the value of a progress bar.");
        _onShow( min, this::withMin);
        return this;
    }

    /**
     * Sets the maximum value of the progress bar.
     * For more information see {@link JProgressBar#setMaximum(int)} (int)}.
     *
     * @param max The maximum value of the progress bar.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForProgressBar<P> withMax( int max ) {
        getComponent().setMaximum( max );
        return this;
    }

    /**
     * @param max An integer property used to dynamically update the max value of the progress bar.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code max} is {@code null}.
     */
    public final UIForProgressBar<P> withMax( Val<Integer> max ) {
        NullUtil.nullArgCheck( max, "max", Val.class );
        NullUtil.nullPropertyCheck(max, "max", "Null is not a valid max value for the value of a progress bar.");
        _onShow( max, this::withMax);
        return this;
    }

    /**
     * @param value The value to set for this {@link JProgressBar}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForProgressBar<P> withValue( int value ) {
        getComponent().setValue(value);
        return this;
    }

    /**
     * @param progress The progress value, a number between 0 and 1, to set for this {@link JProgressBar}.
     *              Note that this will be converted to an integer value between the min and max values.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForProgressBar<P> withProgress( double progress ) {
        if ( progress < 0 || progress > 1 )
            throw new IllegalArgumentException( "Progress value must be between 0 and 1" );
        int min = getComponent().getMinimum();
        int max = getComponent().getMaximum();
        int range = max - min;
        int value = (int) (min + range * progress);
        getComponent().setValue(value);
        return this;
    }

    /**
     * @param val An integer property used to dynamically update the value of the progress bar.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForProgressBar<P> withValue( Val<Integer> val ) {
        NullUtil.nullArgCheck( val, "val", Val.class );
        NullUtil.nullPropertyCheck(val, "value", "Null is not a valid value for the progress property of a progress bar.");
        _onShow( val, this::withValue );
        return withValue(val.orElseThrow());
    }

    /**
     * @param progress A double property used to dynamically update the value of the progress bar.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForProgressBar<P> withProgress( Val<Double> progress ) {
        NullUtil.nullArgCheck( progress, "progress", Val.class );
        NullUtil.nullPropertyCheck(progress, "progress", "Null is not a valid progress for the progress property of a progress bar.");
        _onShow( progress, this::withProgress );
        return withProgress(progress.orElseThrow());
    }

}
