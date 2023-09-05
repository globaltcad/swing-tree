package swingtree;

import sprouts.Var;

import javax.swing.*;

/**
 *  An abstract precursor for swing tree builder nodes for {@link JToggleButton} instances.
 *  Extend this class to create a builder node for a custom {@link JToggleButton} subtype.
 *
 * @param <I> The type of this builder node.
 * @param <B> The type of the {@link JToggleButton} subtype which will be wrapped by this builder node.
 */
public abstract class UIForAnyToggleButton<I, B extends JToggleButton> extends UIForAnyButton<I, B>
{
    protected UIForAnyToggleButton(B component ) { super(component); }

    /**
     *  Use this to dynamically bind to an enum based {@link sprouts.Var}
     *  instance which will be used to dynamically model the selection state of the
     *  wrapped {@link JToggleButton} type by checking
     *  weather the property matches the provided enum or not.
     *
     * @param state The reference {@link Enum} which this {@link JToggleButton} should represent.
     * @param selection The {@link sprouts.Var} instance which will be used
     *                  to dynamically model the selection state of the wrapped {@link JToggleButton} type.
     * @param <E> The type of the enum.
     * @return This builder node.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final <E extends Enum<E>> I isSelectedIf( E state, Var<E> selection ) {
        NullUtil.nullArgCheck(state, "state", Enum.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullPropertyCheck(selection, "selection", "Null is not a valid selection state.");
        this.isSelectedIf( state.equals(selection.get()) );
        _onShow( selection, s -> isSelectedIf( state.equals(selection.get()) ) );
        B component = getComponent();
        String currentText = component.getText();
        if ( currentText == null || currentText.isEmpty() )
            component.setText( state.toString() );

        // When the user clicks the button, we update the selection property!
        // But only if the button is selected, otherwise we'll ignore the click.
        // And we also trigger "set" events for the button, so that other buttons
        // can be updated to reflect the new selection state.
        _onChange( event -> {
            if ( component.isSelected() )
                selection.act( state ).fireSet();
        });

        return (I) this;
    }
}