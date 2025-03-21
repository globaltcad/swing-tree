package swingtree;

import sprouts.Val;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JSeparator} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <S> The type of {@link JSeparator} that this {@link UIForSeparator} is configuring.
 */
public final class UIForSeparator<S extends JSeparator> extends UIForAnySwing<UIForSeparator<S>, S>
{
    private final BuilderState<S> _state;

    /**
     * Instances of ths {@link UIForSeparator} always wrap
     * a single {@link JSeparator} for which they are responsible and expose helpful utility methods.
     *
     * @param state The state of the builder.
     */
    UIForSeparator( BuilderState<S> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<S> _state() {
        return _state;
    }
    
    @Override
    protected UIForSeparator<S> _newBuilderWithState(BuilderState<S> newState ) {
        return new UIForSeparator<>(newState);
    }

    /**
     * Sets the orientation of the separator which can be either
     * {@link SwingConstants#HORIZONTAL} or {@link SwingConstants#VERTICAL}.
     * This method is a convenience method for {@link JSeparator#setOrientation(int)}
     * which receives the {@link UI.Align} enum instead of an integer.
     *
     * @param align The orientation of the separator.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     */
    public final UIForSeparator<S> withOrientation( UI.Align align ) {
        NullUtil.nullArgCheck( align, "align", Val.class );
        return _with( thisComponent -> {
                    thisComponent.setOrientation( align.forSeparator() );
                })
                ._this();
    }

    /**
     *  Binds the supplied alignment property to the orientation of the separator,
     *  so that whenever the property changes, the orientation of the separator will be updated accordingly.
     *
     * @param align The alignment property used to dynamically update the alignment of the separator.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     */
    public final UIForSeparator<S> withOrientation( Val<UI.Align> align ) {
        NullUtil.nullArgCheck( align, "align", Val.class );
        NullUtil.nullPropertyCheck( align, "align", "Null is not a valid alignment." );
        return _withOnShow( align, (c,v) -> {
                    c.setOrientation( v.forSeparator() );
                })
                ._with( thisComponent -> {
                    thisComponent.setOrientation( align.orElseThrowUnchecked().forSeparator() );
                })
                ._this();
    }

    /**
     *  Sets the length of the separation line either horizontally or vertically
     *  depending on the orientation of the separator.
     *  This method is an alignment aware convenience method for
     *  {@link JSeparator#setPreferredSize(Dimension)}.
     *
     * @param separatorLength The length of the separation line.
     * @return This very builder to allow for method chaining.
     */
    public final UIForSeparator<S> withLength( int separatorLength ) {
        return _with( thisComponent -> {
                    _setLength( thisComponent, separatorLength );
                })
                ._this();
    }

    private void _setLength( S thisComponent, int separatorLength ) {
        Dimension d = thisComponent.getPreferredSize();
        if ( thisComponent.getOrientation() == JSeparator.VERTICAL )
            d.height = UI.scale(separatorLength);
        else if ( thisComponent.getOrientation() == JSeparator.HORIZONTAL )
            d.width = UI.scale(separatorLength);
        thisComponent.setPreferredSize(d);
    }

    /**
     *  Binds the provided integer property to the length of the separation line.,
     *  which means that whenever the property changes, the length of the separation line will be updated accordingly.
     *
     * @param separatorLength The property dynamically determining the length of the separation line.
     * @return This very builder to allow for method chaining.
     */
    public UIForSeparator<S> withLength( Val<Integer> separatorLength ) {
        NullUtil.nullArgCheck( separatorLength, "separatorLength", Val.class );
        NullUtil.nullPropertyCheck( separatorLength, "separatorLength", "Null is not a valid separator length." );
        return _withOnShow( separatorLength, (thisComponent,it) -> {
                    _setLength( thisComponent, it );
                })
                ._with( thisComponent -> {
                    _setLength( thisComponent, separatorLength.orElseThrowUnchecked() );
                })
                ._this();
    }
}
