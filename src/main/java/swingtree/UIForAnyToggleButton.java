package swingtree;

import sprouts.From;
import sprouts.Var;

import javax.swing.JToggleButton;

/**
 *  An abstract precursor for swing tree builder nodes for {@link JToggleButton} instances.
 *  Extend this class to create a builder node for a custom {@link JToggleButton} subtype.
 *
 * @param <I> The type of this builder node.
 * @param <B> The type of the {@link JToggleButton} subtype which will be managed by this builder.
 */
public abstract class UIForAnyToggleButton<I, B extends JToggleButton> extends UIForAnyButton<I, B>
{
    /**
     *  Use this to dynamically bind the selection flag of the button to a {@link Var}
     *  property which will determine the selection state of the button based on the
     *  equality of the property value and the provided reference value.
     *  So if the first {@code state} argument is equal to the value of the {@code selection} property,
     *  the button will be selected, otherwise it will be deselected.<br>
     *  A typical use case is to bind a button to an enum property, like so: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Step { ONE, TWO, THREE }
     *      Var<Step> step = Var.of(Step.ONE);
     *
     *      // In your view:
     *      UI.radioButton("Two").isSelectedIf(Step.TWO, vm.getStep());
     *  }</pre>
     *  As you can see, the radio button will be selected if the enum property is equal to the supplied enum value
     *  and deselected otherwise. <br>
     *  <br>
     * <i>Hint: Use {@code myProperty.fireChange(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param state The reference value which this {@link JToggleButton} should represent.
     * @param selection The {@link sprouts.Var} instance which will be used
     *                  to dynamically model the selection state of the wrapped {@link JToggleButton} type
     *                  based on the equality of the {@code state} argument and the value of the property.
     * @param <E> The type of the property value.
     * @return The current builder type, to allow for further method chaining.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final <E> I isSelectedIf( E state, Var<E> selection ) {
        NullUtil.nullArgCheck(state, "state", Enum.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullPropertyCheck(selection, "selection", "Null is not a valid selection state.");
        return _withOnShow( selection, (button,s) -> {
                   _setSelectedSilently(button, state.equals(s));
               })
               ._with( button -> {
                   _setSelectedSilently(button, state.equals(selection.get()));
                   // When the user clicks the button, we update the selection property!
                   // But only if the button is selected, otherwise we'll ignore the click.
                   // And we also trigger "set" events for the button, so that other buttons
                   // can be updated to reflect the new selection state.
                   _onChange(button, event -> {
                       if ( button.isSelected() )
                           selection.set(From.VIEW,  state ).fireChange(From.VIEW_MODEL);
                   });
               })
               ._this();
    }

}