package swingtree;

import sprouts.Val;

import javax.swing.*;
import java.awt.*;

/**
 *  A swing tree builder node for {@link JSeparator} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 */
public class UIForSeparator<S extends JSeparator> extends UIForAbstractSwing<UIForSeparator<S>, S>
{
    /**
     * Instances of ths {@link UIForSeparator} always wrap
     * a single {@link JSeparator} for which they are responsible and expose helpful utility methods.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    protected UIForSeparator(S component) { super(component); }

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
    public final UIForSeparator<S> with( UI.Align align ) {
        NullUtil.nullArgCheck( align, "align", Val.class );
        getComponent().setOrientation(align.forSeparator());
        return this;
    }

    /**
     * @param align The alignment property used to dynamically update the alignment of the separator.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     */
    public final UIForSeparator<S> withAlignment( Val<UI.Align> align ) {
        NullUtil.nullArgCheck( align, "align", Val.class );
        NullUtil.nullPropertyCheck( align, "align", "Null is not a valid alignment." );
        _onShow( align, v -> getComponent().setOrientation(v.forSeparator()) );
        return with(align.get());
    }

    /**
     * @param separatorLength The length of the separation line.
     * @return This very builder to allow for method chaining.
     */
    public final UIForSeparator<S> withLength( int separatorLength ) {
        S separator = getComponent();
        Dimension d = separator.getPreferredSize();
        if ( separator.getOrientation() == JSeparator.VERTICAL ) d.height = separatorLength;
        else if ( separator.getOrientation() == JSeparator.HORIZONTAL ) d.width = separatorLength;
        separator.setPreferredSize(d);
        return this;
    }

    /**
     * @param separatorLength The property dynamically determining the length of the separation line.
     * @return This very builder to allow for method chaining.
     */
    public UIForSeparator<S> withLength( Val<Integer> separatorLength ) {
        NullUtil.nullArgCheck( separatorLength, "separatorLength", Val.class );
        NullUtil.nullPropertyCheck( separatorLength, "separatorLength", "Null is not a valid separator length." );
        _onShow( separatorLength, this::withLength );
        return this;
    }
}
