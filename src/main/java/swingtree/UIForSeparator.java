package swingtree;

import sprouts.Val;

import javax.swing.*;
import java.awt.*;

/**
 *  A SwingTree builder node designed for configuring {@link JSeparator} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 */
public final class UIForSeparator<S extends JSeparator> extends UIForAnySwing<UIForSeparator<S>, S>
{
    private final BuilderState<S> _state;

    /**
     * Instances of ths {@link UIForSeparator} always wrap
     * a single {@link JSeparator} for which they are responsible and expose helpful utility methods.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    UIForSeparator(S component) {
        _state = new BuilderState<>(component);
    }

    @Override
    protected BuilderState<S> _state() {
        return _state;
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
                    thisComponent.setOrientation( align.orElseThrow().forSeparator() );
                })
                ._this();
    }

    /**
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
            d.height = separatorLength;
        else if ( thisComponent.getOrientation() == JSeparator.HORIZONTAL )
            d.width = separatorLength;
        thisComponent.setPreferredSize(d);
    }

    /**
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
                    _setLength( thisComponent, separatorLength.orElseThrow() );
                })
                ._this();
    }
}
