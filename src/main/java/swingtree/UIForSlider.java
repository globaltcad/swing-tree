package swingtree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Action;
import sprouts.Val;
import sprouts.Var;
import swingtree.api.model.SliderTicks;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JSlider} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *  <p>
 *  A slider in SwingTree works with numbers of the type {@code N}, which is
 *  {@link Integer} unless you created the slider through one of the factory methods
 *  taking properties of another number type, like
 *  {@link UI#slider(UI.Axis, Number, Number, Var)} with a {@code Var<Double>}.
 *  The minimum, the maximum, the value and the tick marks of the slider are all
 *  expressed in that type, even though a plain {@link JSlider} only knows whole numbers.
 *  <p>
 *  On a slider for whole numbers, a minimum or maximum set directly through
 *  {@link JSlider#setMinimum(int)} or {@link JSlider#setMaximum(int)} becomes the range
 *  of the slider, and its tick marks and labels are laid out again for that range.
 *  On a slider for {@link Float} or {@link Double} numbers, the whole numbers of the
 *  {@link JSlider} are an internal detail, so such a call is undone right away.
 *
 * @param <S> The type of {@link JSlider} that this {@link UIForSlider} is configuring.
 * @param <N> The type of the numbers the slider works with.
 */
public final class UIForSlider<S extends JSlider, N extends Number> extends UIForAnySwing<UIForSlider<S, N>, S>
{
    private static final Logger log = LoggerFactory.getLogger(UIForSlider.class);

    private final BuilderState<S> _state;
    private final Class<N>        _numberType;

    UIForSlider( BuilderState<S> state, Class<N> numberType ) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(numberType);
        _state      = state;
        _numberType = numberType;
    }

    @Override
    protected BuilderState<S> _state() {
        return _state;
    }

    @Override
    protected UIForSlider<S, N> _newBuilderWithState(BuilderState<S> newState ) {
        return new UIForSlider<>(newState, _numberType);
    }

    private SliderState _sliderStateOf( S thisComponent ) {
        return SliderState.of(thisComponent, _numberType, _state().eventProcessor());
    }

    /**
     *  Sets the orientation of the slider.
     *  @param axis The orientation of the slider.
     *  @return This builder node.
     */
    public final UIForSlider<S, N> withOrientation( UI.Axis axis ) {
        NullUtil.nullArgCheck( axis, "axis", UI.Axis.class );
        return _with( thisComponent -> {
                   _setOrientation( thisComponent, axis );
               })
               ._this();
    }

    private void _setOrientation( S thisComponent, UI.Axis axis ) {
        thisComponent.setOrientation(axis.forSlider());
    }

    /**
     *  Dynamically sets the orientation of the slider.
     *  @param axis The orientation of the slider.
     *  @return This builder node.
     */
    public final UIForSlider<S, N> withOrientation( Val<UI.Axis> axis ) {
        NullUtil.nullArgCheck( axis, "axis", Val.class );
        NullUtil.nullPropertyCheck( axis, "axis", "Null is not a valid axis" );
        return _withOnShow( axis, (thisComponent,v) -> {
                    _setOrientation(thisComponent, v);
               })
                ._with( thisComponent -> {
                    _setOrientation(thisComponent, axis.orElseThrowUnchecked());
                })
               ._this();
    }

    /**
     *  Adds an {@link Action} which is called when the user changes the state of the slider,
     *  for example by moving its knob, or by pressing or releasing the mouse button on it.
     *  Changes which your application makes through the properties or values given to this
     *  builder do not call the action.
     *  <p>
     *  Every change calls each action once, in the order in which the actions were added.
     *  When the knob snaps to a tick mark (see {@link SliderTicks#withSnapToTicks(boolean)}),
     *  the actions are called after the knob has snapped, so they read the number at that tick mark.
     *
     * @param action The {@link Action} that will be called through the underlying change event.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    public final UIForSlider<S, N> onChange( Action<ComponentDelegate<JSlider, ChangeEvent>> action ) {
        NullUtil.nullArgCheck( action, "action", Action.class );
        return _with( thisComponent -> {
                    _sliderStateOf(thisComponent).onChange(
                        e -> _runInApp(()->{
                            try {
                                action.accept(new ComponentDelegate<>(thisComponent, e));
                            } catch (Exception ex) {
                                log.error(SwingTree.get().logMarker(), "Error while executing action on slider change!", ex);
                            }
                        })
                    );
                })
                ._this();
    }

    /**
     *  Sets the minimum value of the slider, which is the number at the start of its track.
     *
     * @param min The minimum value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code min} is {@code null}.
     */
    public final UIForSlider<S, N> withMin( N min ) {
        NullUtil.nullArgCheck( min, "min", Number.class );
        return _with( thisComponent -> {
                    _sliderStateOf(thisComponent).setMin(min);
                })
                ._this();
    }

    /**
     *  Binds the supplied {@link Val} property to the minimum value of the slider,
     *  so that when the value of the property changes, the minimum of the slider,
     *  and with it the position of the knob and of the tick marks, is updated accordingly.
     *  When the user moves the knob, the value written back by the slider is never
     *  smaller than the item of this property.
     *
     * @param min The property holding the minimum value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code min} is {@code null} or allows {@code null} items.
     */
    public final UIForSlider<S, N> withMin( Val<N> min ) {
        NullUtil.nullArgCheck( min, "min", Val.class );
        NullUtil.nullPropertyCheck( min, "min", "The minimum value of a slider must not be null!" );
        return _withOnShow( min, (thisComponent,v) -> {
                    _sliderStateOf(thisComponent).setMin(v);
                })
                ._with( thisComponent -> {
                    SliderState state = _sliderStateOf(thisComponent);
                    state.keepMinWithin(min);
                    state.setMin(min.orElseThrowUnchecked());
                })
                ._this();
    }

    /**
     *  Sets the maximum value of the slider, which is the number at the end of its track.
     *
     * @param max The maximum value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code max} is {@code null}.
     */
    public final UIForSlider<S, N> withMax( N max ) {
        NullUtil.nullArgCheck( max, "max", Number.class );
        return _with( thisComponent -> {
                    _sliderStateOf(thisComponent).setMax(max);
                })
                ._this();
    }

    /**
     *  Binds the supplied {@link Val} property to the maximum value of the slider,
     *  so that when the value of the property changes, the maximum of the slider,
     *  and with it the position of the knob and of the tick marks, is updated accordingly.
     *  When the user moves the knob, the value written back by the slider is never
     *  larger than the item of this property.
     *
     * @param max The property holding the maximum value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code max} is {@code null} or allows {@code null} items.
     */
    public final UIForSlider<S, N> withMax( Val<N> max ) {
        NullUtil.nullArgCheck( max, "max", Val.class );
        NullUtil.nullPropertyCheck( max, "max", "The maximum value of a slider must not be null!" );
        return _withOnShow( max, (thisComponent,v) -> {
                    _sliderStateOf(thisComponent).setMax(v);
                })
                ._with( thisComponent -> {
                    SliderState state = _sliderStateOf(thisComponent);
                    state.keepMaxWithin(max);
                    state.setMax(max.orElseThrowUnchecked());
                })
                ._this();
    }

    /**
     *  Sets the current value of the slider, which places the knob at that number.
     *  A number outside the range of the slider places the knob at the nearest end of the track.
     *
     * @param value The current value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForSlider<S, N> withValue( N value ) {
        NullUtil.nullArgCheck( value, "value", Number.class );
        return _with( thisComponent -> {
                    _sliderStateOf(thisComponent).setValue(value);
                })
                ._this();
    }

    /**
     *  Binds the supplied {@link Val} property to the value of the slider,
     *  which causes the knob of the slider to move when the value of the property changes.
     *  But note that the supplied property is a read only, so when the user moves
     *  the knob, the property will not be updated.
     *  Use {@link #withValue(Var)} if you want to bind a property bidirectionally.
     *  <p>
     *  While the user holds the knob with the mouse, changes of the property do not move it.
     *
     * @param value A property used to dynamically update the value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null} or allows {@code null} items.
     */
    public final UIForSlider<S, N> withValue( Val<N> value ) {
        NullUtil.nullArgCheck( value, "value", Val.class );
        NullUtil.nullPropertyCheck( value, "value", "The value of a slider must not be null!" );
        return _withOnShow( value, (thisComponent,v) -> {
                    _sliderStateOf(thisComponent).setValue(v);
                })
                ._with( thisComponent -> {
                    _sliderStateOf(thisComponent).setValue(value.orElseThrowUnchecked());
                })
                ._this();
    }

    /**
     *  Use this to bind the supplied {@link Var} property to the value of the slider.
     *  When the user moves the knob, the {@link Var} is updated,
     *  and when the item of the {@link Var} is changed as part of the application logic,
     *  the knob moves accordingly.
     *  <p>
     *  The number written into the property is of the property's own type, and it is the number
     *  at the position of the knob, or, if the knob snaps to tick marks
     *  (see {@link SliderTicks#withSnapToTicks(boolean)}), the number at the tick mark it snaps to.
     *  A click on the knob which does not move it writes nothing new, so the property keeps
     *  exactly the number your application set.
     *  <p>
     *  While the user holds the knob with the mouse, changes of the property do not move it,
     *  and when the user lets go, the number under the knob is written into the property.
     *
     * @param value A property holding the value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null} or allows {@code null} items.
     */
    public final UIForSlider<S, N> withValue( Var<N> value ) {
        NullUtil.nullArgCheck( value, "value", Var.class );
        NullUtil.nullPropertyCheck( value, "value", "The value of a slider must not be null!" );
        return _withOnShow( value, (thisComponent,v) -> {
                    _sliderStateOf(thisComponent).setValue(v);
                })
                ._with( thisComponent -> {
                    SliderState state = _sliderStateOf(thisComponent);
                    state.setValue(value.orElseThrowUnchecked());
                    state.writeUserChangesTo(value);
                })
                ._this();
    }

    /**
     *  Configures the tick marks and labels along the slider through the supplied
     *  {@link SliderTicks} value, which describes where the major and minor tick marks are,
     *  whether they are drawn, whether the knob snaps to them and which labels are shown:
     *  <pre>{@code
     *  UI.slider(UI.Axis.HORIZONTAL, 0, 100, volume)
     *  .withTicks(
     *      SliderTicks.of(Integer.class)
     *      .withMajorSpacing(25)
     *      .withMinorTicksBetween(4)
     *      .withLabelsAtMajorTicks( v -> v + "%" )
     *  )
     *  }</pre>
     *  The spacings and label positions are numbers of the slider's own type, so a slider
     *  for a {@code Double} property is configured with a {@code SliderTicks<Double>}.
     *  Whenever the minimum or maximum of the slider changes, the tick marks and labels
     *  are laid out again for the new range.
     *
     * @param ticks The tick marks and labels of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code ticks} is {@code null}.
     */
    public final UIForSlider<S, N> withTicks( SliderTicks<N> ticks ) {
        NullUtil.nullArgCheck( ticks, "ticks", SliderTicks.class );
        return _with( thisComponent -> {
                    _sliderStateOf(thisComponent).setTicks(ticks);
                })
                ._this();
    }

    /**
     *  Binds the tick marks and labels along the slider to the supplied property holding a
     *  {@link SliderTicks} value, so that the slider shows the tick marks and labels of the
     *  property's current item, and follows every change of it.
     *  <p>
     *  The property may live in your view model, or be derived from view model state right
     *  in the view, like this slider which shows its labels only while a flag says so:
     *  <pre>{@code
     *  SliderTicks<Integer> plain    = SliderTicks.of(Integer.class).withMajorSpacing(25);
     *  SliderTicks<Integer> labelled = plain.withLabelsAtMajorTicks();
     *
     *  UI.slider(UI.Axis.HORIZONTAL, 0, 100, volume)
     *  .withTicks( showLabels.viewAs(SliderTicks.classTyped(Integer.class), show -> show ? labelled : plain) )
     *  }</pre>
     *  Under the {@link swingtree.threading.EventProcessor#DECOUPLED} event processor the
     *  new {@link SliderTicks} reaches the slider as part of the property's change event,
     *  in the same order as every other change of the slider's properties.
     *
     * @param ticks A property holding the tick marks and labels of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code ticks} is {@code null} or allows {@code null} items.
     */
    public final UIForSlider<S, N> withTicks( Val<SliderTicks<N>> ticks ) {
        NullUtil.nullArgCheck( ticks, "ticks", Val.class );
        NullUtil.nullPropertyCheck( ticks, "ticks", "Use 'SliderTicks.of(..)' instead of null to describe a slider without tick marks!" );
        return _withOnShow( ticks, (thisComponent,v) -> {
                    _sliderStateOf(thisComponent).setTicks(v);
                })
                ._with( thisComponent -> {
                    _sliderStateOf(thisComponent).setTicks(ticks.orElseThrowUnchecked());
                })
                ._this();
    }
}
