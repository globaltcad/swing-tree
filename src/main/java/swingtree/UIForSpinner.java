package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Action;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.text.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Objects;
import java.util.function.Consumer;

/**
 *  A SwingTree builder node designed for configuring {@link JSpinner} instances.
 */
public final class UIForSpinner<S extends JSpinner> extends UIForAnySwing<UIForSpinner<S>, S>
{
    private static final Logger log = LoggerFactory.getLogger(UIForSpinner.class);
    private final BuilderState<S> _state;

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the component is built.
     */
    UIForSpinner( BuilderState<S> state ) {
        Objects.requireNonNull(state);
        _state = state.withMutator(this::_initialize);
    }

    private void _initialize( S thisComponent )
    {
        if ( thisComponent.getModel() != null && thisComponent.getModel().getClass() != SpinnerNumberModel.class )
            return;
        /*
            So it turns out that the default SpinnerNumberModel implementation
            is not very good. It does not support floating point step sizes...
            So we have to replace it with our own implementation, where the incrementation logic
            is a bit more flexible.
         */
        SpinnerNumberModel model = (SpinnerNumberModel) thisComponent.getModel();
        thisComponent.setModel(
            new SpinnerNumberModel(
                model.getNumber(),
                model.getMinimum(),
                model.getMaximum(),
                model.getStepSize()
            ) {
                @Override public void setValue(Object value) {
                    super.setValue(value);
                    updateEditorFormatter();
                }
                @Override public @Nullable Object getNextValue() { return incrValue(+1); }
                @Override public @Nullable Object getPreviousValue() { return incrValue(-1); }
                private @Nullable Number incrValue(int dir)
                {
                    Number newValue;
                    Number value = this.getNumber();
                    Number stepSize = this.getStepSize();
                    Comparable<Number> maximum = (Comparable<Number>) this.getMaximum();
                    Comparable<Number> minimum = (Comparable<Number>) this.getMinimum();
                    boolean valueIsRational = (value instanceof Float) || (value instanceof Double);
                    boolean stepIsRational = (stepSize instanceof Float) || (stepSize instanceof Double);
                    if ( valueIsRational || stepIsRational ) {
                        double v = value.doubleValue() + (stepSize.doubleValue() * (double)dir);
                        if ( value instanceof Double || stepSize instanceof Double )
                            newValue = v;
                        else
                            newValue = (float) v;
                    }
                    else {
                        long v = value.longValue() + (stepSize.longValue() * (long)dir);

                        if      ( value instanceof Long    ) newValue = v;
                        else if ( value instanceof Integer ) newValue = (int) v;
                        else if ( value instanceof Short   ) newValue = (short) v;
                        else
                            newValue = (byte) v;
                    }
                    if ( (maximum != null) && (maximum.compareTo(newValue) < 0) ) return null;
                    if ( (minimum != null) && (minimum.compareTo(newValue) > 0) ) return null;
                    else
                        return newValue;
                }
                private void updateEditorFormatter() {
                    JComponent editor = thisComponent.getEditor();
                    if (editor instanceof JSpinner.DefaultEditor) {
                        ((JSpinner.DefaultEditor)editor).getTextField().setFormatterFactory(
                            new JFormattedTextField.AbstractFormatterFactory() {
                                @Override public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                                    return getDefaultFormatterFactory(getNumber().getClass()).getFormatter(tf);
                                }
                            }
                        );
                    }
                }
            }
        );
    }

    @Override
    protected BuilderState<S> _state() {
        return _state;
    }
    
    @Override
    protected UIForSpinner<S> _newBuilderWithState(BuilderState<S> newState ) {
        return new UIForSpinner<>(newState);
    }

    private JFormattedTextField.AbstractFormatterFactory getDefaultFormatterFactory(Object type) {
        if (type instanceof DateFormat) {
            return new DefaultFormatterFactory(new DateFormatter
                    ((DateFormat)type));
        }
        if (type instanceof NumberFormat) {
            return new DefaultFormatterFactory(new NumberFormatter(
                    (NumberFormat)type));
        }
        if (type instanceof Format) {
            return new DefaultFormatterFactory(new InternationalFormatter(
                    (Format)type));
        }
        if (type instanceof Date) {
            return new DefaultFormatterFactory(new DateFormatter());
        }
        if (type instanceof Number) {
            JFormattedTextField.AbstractFormatter displayFormatter = new NumberFormatter();
            ((NumberFormatter)displayFormatter).setValueClass(type.getClass());
            JFormattedTextField.AbstractFormatter editFormatter = new NumberFormatter(
                    new DecimalFormat("#.#"));
            ((NumberFormatter)editFormatter).setValueClass(type.getClass());

            return new DefaultFormatterFactory(displayFormatter,
                    displayFormatter,editFormatter);
        }
        return new DefaultFormatterFactory(new DefaultFormatter());
    }

    /**
     * Adds an {@link Action} to the underlying {@link JSpinner}
     * through an {@link javax.swing.event.ChangeListener},
     * Use this to register an action to be performed when the spinner's value changes.
     *
     * @param action The action to be performed.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSpinner<S> onChange( Action<ComponentDelegate<JSpinner, ChangeEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent ->
                    _onChange(thisComponent,
                        e -> _runInApp(()->{
                            try {
                                action.accept(new ComponentDelegate<>(thisComponent, e));
                            } catch (Exception ex) {
                                log.error("Error while executing action on spinner change!", ex);
                            }
                        })
                    )
                )
                ._this();
    }

    private void _onChange( S thisComponent, Consumer<ChangeEvent> consumer ) {
        thisComponent.addChangeListener(consumer::accept);
    }

    /**
     * Sets the value of the spinner.
     *
     * @param value The value to set.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForSpinner<S> withValue( Object value ) {
        NullUtil.nullArgCheck(value, "value", Object.class);
        return _with( thisComponent -> {
                    thisComponent.setValue(value);
                })
                ._this();
    }

    /**
     * Sets the value of the spinner and also binds to said value.
     *
     * @param value The {@link sprouts.Val} wrapper whose value should be set.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForSpinner<S> withValue( Val<?> value ) {
        NullUtil.nullArgCheck(value, "value", Val.class);
        NullUtil.nullPropertyCheck(value, "value", "Null is not a valid spinner state!");
        return _withOnShow( value, (thisComponent,it) -> {
                    thisComponent.setValue(it);
                })
                ._with( thisComponent -> {
                    thisComponent.setValue(value.get());
                })
                ._this();
    }

    /**
     * Sets the value of the spinner and also binds to the provided property.
     *
     * @param value The {@link sprouts.Var} wrapper whose value should be set.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForSpinner<S> withValue( Var<?> value ) {
        NullUtil.nullArgCheck(value, "value", Var.class);
        NullUtil.nullPropertyCheck(value, "value", "Null is not a valid spinner state!");
        return _withOnShow( value, (thisComponent,v) -> {
                    thisComponent.setValue(v);
                })
                ._with( thisComponent -> {
                    _onChange(thisComponent, e -> {
                        // Get access the current component while still in the EDT. (getComponent() is only allowed in the EDT)
                        final Object current = thisComponent.getValue();
                        // Now let's do the actual work in the application thread:
                        _runInApp(() -> {
                            Object interpreted = current;
                            if (current != null && Number.class.isAssignableFrom(value.type())) {
                                if (Number.class.isAssignableFrom(current.getClass())) {
                                    Number n = (Number) current;
                                    if      (value.type() == Integer.class) interpreted = n.intValue();
                                    else if (value.type() == Long.class   ) interpreted = n.longValue();
                                    else if (value.type() == Float.class  ) interpreted = n.floatValue();
                                    else if (value.type() == Double.class ) interpreted = n.doubleValue();
                                    else if (value.type() == Short.class  ) interpreted = n.shortValue();
                                    else if (value.type() == Byte.class   ) interpreted = n.byteValue();
                                }
                                if (current.getClass() == String.class) {
                                    if      (value.type() == Integer.class) interpreted = Integer.parseInt((String) current);
                                    else if (value.type() == Long.class   ) interpreted = Long.parseLong((String) current);
                                    else if (value.type() == Float.class  ) interpreted = Float.parseFloat((String) current);
                                    else if (value.type() == Double.class ) interpreted = Double.parseDouble((String) current);
                                    else if (value.type() == Short.class  ) interpreted = Short.parseShort((String) current);
                                    else if (value.type() == Byte.class   ) interpreted = Byte.parseByte((String) current);
                                }
                            }
                            ((Var<Object>) value).set(From.VIEW,  interpreted );
                        });
                    });
                    thisComponent.setValue( value.get() );
                })
                ._this();
    }

    /**
     * Sets the numeric step size of the value of the spinner.
     * This expects your spinner to be based on the {@link SpinnerNumberModel}.
     *
     * @param n The {@link Number} which should be set as step size.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSpinner<S> withStepSize( Number n ) {
        NullUtil.nullArgCheck(n, "n", Number.class);
        return _with( thisComponent -> {
            SpinnerModel model = thisComponent.getModel();
            if ( !(model instanceof SpinnerNumberModel) )
                throw new IllegalArgumentException(
                        "This JSpinner can not have a numeric step size as it is not " +
                        "based on the SpinnerNumberModel!"
                    );
            SpinnerNumberModel numberModel = (SpinnerNumberModel) model;
            numberModel.setStepSize(n);
        })
        ._this();
    }

    /**
     * Sets the numeric step size of the value of the spinner and also binds to said value.
     * This expects your spinner to be based on the {@link SpinnerNumberModel}.
     *
     * @param val The {@link sprouts.Val} wrapper whose step size should be set.
     * @return This very instance, which enables builder-style method chaining.
     * @param <N> The type of the number.
     * @throws IllegalArgumentException if {@code val} is {@code null}.
     */
    public final <N extends Number> UIForSpinner<S> withStepSize( Val<N> val ) {
        NullUtil.nullArgCheck(val, "val", Val.class);
        NullUtil.nullPropertyCheck(val, "val", "Null is not a valid spinner step size!");
        return _with( thisComponent -> {
                    SpinnerModel model = thisComponent.getModel();
                    if ( !(model instanceof SpinnerNumberModel) )
                        throw new IllegalArgumentException(
                                "This JSpinner can not have a numeric step size as it is not based on the SpinnerNumberModel!"
                            );
                    SpinnerNumberModel numberModel = (SpinnerNumberModel) model;
                    _onShow( val, thisComponent, (c,v) -> numberModel.setStepSize(v) );
                    numberModel.setStepSize(val.get());
                })
                ._this();
    }
}
