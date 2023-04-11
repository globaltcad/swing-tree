package swingtree;


import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import sprouts.Action;
import sprouts.Val;
import sprouts.Vals;
import sprouts.Var;
import swingtree.api.Peeker;
import swingtree.api.UIVerifier;
import swingtree.api.mvvm.Viewable;
import swingtree.input.Keyboard;
import swingtree.layout.CompAttr;
import swingtree.layout.LayoutAttr;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;


/**
 *  A swing tree builder node for any kind {@link JComponent} instance.
 *  This is the most generic builder type and therefore abstract super-type for almost all other builders.
 *  This builder defines nested building for anything extending the {@link JComponent} class.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *  <br><br>
 *
 * @param <I> The concrete extension of the {@link AbstractNestedBuilder}.
 * @param <C> The type parameter for the component type wrapped by an instance of this class.
 */
public abstract class UIForAnySwing<I, C extends JComponent> extends AbstractNestedBuilder<I, C, JComponent>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UI.class);

    private final static String _TIMERS_KEY = "_swing-tree.timers";

    private boolean _idAlreadySet = false; // The id translates to the 'name' property of swing components.
    private boolean _migAlreadySet = false;

    /**
     *  Extensions of the {@link  UIForAnySwing} always wrap
     *  a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    public UIForAnySwing(C component ) { super(component); }

    /**
     *  This method exposes a concise way to set an identifier for the component
     *  wrapped by this {@link UI}!
     *  In essence this is simply a delegate for the {@link JComponent#setName(String)} method
     *  to make it more expressive and widely recognized what is meant
     *  ("id" is shorter and makes more sense than "name" which could be confused with "title").
     *
     * @param id The identifier for this {@link JComponent} which will
     *           simply translate to {@link JComponent#setName(String)}
     *
     * @return The JComponent type which will be wrapped by this builder node.
     */
    public final I id( String id ) {
        if ( _idAlreadySet )
            throw new IllegalArgumentException("The id has already been specified for this component!");
        getComponent().setName(id);
        _idAlreadySet = true;
        return _this();
    }

    /**
     *  Use this to make the wrapped UI component visible or invisible.
     *
     * @param isVisible The truth value determining if the UI component should be visible or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isVisibleIf( boolean isVisible ) {
        getComponent().setVisible( isVisible );
        return _this();
    }

    /**
     *  This is the inverse of {@link #isVisibleIf(boolean)}.
     *  Use this to make the wrapped UI component invisible or visible.
     * @param isVisible The truth value determining if the UI component should be visible or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isVisibleIfNot( boolean isVisible ) {
        getComponent().setVisible(!isVisible);
        return _this();
    }

    /**
     *  Use this to make the wrapped UI component dynamically visible or invisible. <br>
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param isVisible The truth value determining if the UI component should be visible or not wrapped in a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isVisibleIf( Val<Boolean> isVisible ) {
        NullUtil.nullArgCheck(isVisible, "isVisible", Val.class);
        NullUtil.nullPropertyCheck(isVisible, "isVisible", "Null is not allowed to model the visibility of a UI component!");
        _onShow( isVisible, v -> getComponent().setVisible(v) );
        return isVisibleIf( isVisible.orElseThrow() );
    }

    /**
     *  This is the inverse of {@link #isVisibleIf(Val)}.
     *  Use this to make the wrapped UI component dynamically invisible or visible. <br>
     * @param isVisible The truth value determining if the UI component should be visible or not wrapped in a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isVisibleIfNot( Val<Boolean> isVisible ) {
        NullUtil.nullArgCheck(isVisible, "isVisible", Val.class);
        NullUtil.nullPropertyCheck(isVisible, "isVisible", "Null is not allowed to model the visibility of a UI component! A boolean should only be true or false!");
        _onShow( isVisible, v -> getComponent().setVisible(!v) );
        return isVisibleIf( !isVisible.orElseThrow() );
    }

    /**
     *  Use this to make the wrapped UI component dynamically visible or invisible,
     *  based on the equality between the supplied enum value and enum property. <br>
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param enumValue The enum value which, if equal to the supplied enum property, makes the UI component visible.
     * @param enumProperty The enum property which, if equal to the supplied enum value, makes the UI component visible.
     * @param <E> The enum type.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <E extends Enum<E>> I isVisibleIf( E enumValue, Val<E> enumProperty ) {
        NullUtil.nullArgCheck(enumValue, "enumValue", Enum.class);
        NullUtil.nullArgCheck(enumProperty, "enumProperty", Val.class);
        NullUtil.nullPropertyCheck(enumProperty, "enumProperty", "Null is not allowed to model the visibility of a UI component!");
        _onShow( enumProperty, v -> getComponent().setVisible( v == enumValue ) );
        return isVisibleIf( enumValue == enumProperty.orElseThrow() );
    }

    /**
     *  This is the inverse of {@link #isVisibleIf(Enum, Val)}.
     *  Use this to make the wrapped UI component dynamically invisible or visible,
     *  based on the equality between the supplied enum value and enum property. <br>
     * @param enumValue The enum value which, if equal to the supplied enum property, makes the UI component invisible.
     * @param enumProperty The enum property which, if equal to the supplied enum value, makes the UI component invisible.
     * @param <E> The enum type for both the supplied enum value and enum property.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <E extends Enum<E>> I isVisibleIfNot( E enumValue, Val<E> enumProperty ) {
        NullUtil.nullArgCheck(enumValue, "enumValue", Enum.class);
        NullUtil.nullArgCheck(enumProperty, "enumProperty", Val.class);
        NullUtil.nullPropertyCheck(enumProperty, "enumProperty", "Null is not allowed to model the visibility of a UI component!");
        _onShow( enumProperty, v -> getComponent().setVisible( v != enumValue ) );
        return isVisibleIf( enumValue != enumProperty.orElseThrow() );
    }

    /**
     *  Use this to enable or disable the wrapped UI component.
     *
     * @param isEnabled The truth value determining if the UI component should be enabled or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isEnabledIf( boolean isEnabled ) {
        _setEnabled( isEnabled );
        return _this();
    }

    /**
     *  This is the inverse of {@link #isEnabledIf(boolean)}.
     *  Use this to disable or enable the wrapped UI component.
     *
     * @param isEnabled The truth value determining if the UI component should be enabled or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isEnabledIfNot( boolean isEnabled ) {
        _setEnabled( !isEnabled );
        return _this();
    }

    /**
     *  Use this to dynamically enable or disable the wrapped UI component.
     *
     * @param isEnabled The truth value determining if the UI component should be enabled or not wrapped in a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isEnabledIf( Val<Boolean> isEnabled ) {
        NullUtil.nullArgCheck(isEnabled, "isEnabled", Val.class);
        NullUtil.nullPropertyCheck(isEnabled, "isEnabled", "Null value for isEnabled is not allowed!");
        _onShow( isEnabled, v -> _setEnabled(v) );
        return isEnabledIf( isEnabled.orElseThrow() );
    }

    /**
     *  This is the inverse of {@link #isEnabledIf(Val)}.
     *  Use this to dynamically disable or enable the wrapped UI component.
     *
     * @param isEnabled The truth value determining if the UI component should be enabled or not wrapped in a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isEnabledIfNot( Val<Boolean> isEnabled ) {
        NullUtil.nullArgCheck(isEnabled, "isEnabled", Val.class);
        NullUtil.nullPropertyCheck(isEnabled, "isEnabled", "Null value for isEnabled is not allowed!");
        _onShow( isEnabled, v -> _setEnabled(!v) );
        return isEnabledIf( !isEnabled.orElseThrow() );
    }

    /**
     *  Use this to make the wrapped UI component dynamically enabled or disabled,
     *  based on the equality between the supplied enum value and enum property. <br>
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param enumValue The enum value which, if equal to the supplied enum property, makes the UI component enabled.
     * @param enumProperty The enum property which, if equal to the supplied enum value, makes the UI component enabled.
     * @param <E> The enum type for both the supplied enum value and enum property.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <E extends Enum<E>> I isEnabledIf( E enumValue, Val<E> enumProperty ) {
        NullUtil.nullArgCheck(enumValue, "enumValue", Enum.class);
        NullUtil.nullArgCheck(enumProperty, "enumProperty", Val.class);
        NullUtil.nullPropertyCheck(enumProperty, "enumProperty", "The enumProperty may not have null values!");
        _onShow( enumProperty, v -> _setEnabled( v == enumValue ) );
        return isEnabledIf( enumValue == enumProperty.orElseThrow() );
    }

    /**
     *  This is the inverse of {@link #isEnabledIf(Enum, Val)}.
     *  Use this to make the wrapped UI component dynamically disabled or enabled,
     *  based on the equality between the supplied enum value and enum property. <br>
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param enumValue The enum value which, if equal to the supplied enum property, makes the UI component disabled.
     * @param enumProperty The enum property which, if equal to the supplied enum value, makes the UI component disabled.
     * @param <E> The enum type for both the supplied enum value and enum property.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <E extends Enum<E>> I isEnabledIfNot( E enumValue, Val<E> enumProperty ) {
        NullUtil.nullArgCheck(enumValue, "enumValue", Enum.class);
        NullUtil.nullArgCheck(enumProperty, "enumProperty", Val.class);
        NullUtil.nullPropertyCheck(enumProperty, "enumProperty", "The enumProperty may not have null values!");
        _onShow( enumProperty, v -> _setEnabled( v != enumValue ) );
        return isEnabledIf( enumValue != enumProperty.orElseThrow() );
    }

    protected void _setEnabled( boolean isEnabled ) { getComponent().setEnabled( isEnabled ); }

    /**
     *  Use this to make the wrapped UI component focusable.
     *  @param isFocusable The truth value determining if the UI component should be focusable or not.
     *  @return This very instance, which enables builder-style method chaining.
     */
    public final I isFocusableIf( boolean isFocusable ) {
        getComponent().setFocusable( isFocusable );
        return _this();
    }

    /**
     *  Use this to dynamically make the wrapped UI component focusable.
     *  This is useful if you want to make a component focusable only if a certain condition is met.
     *  <br>
     *  <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     *  @param isFocusable The truth value determining if the UI component should be focusable or not wrapped in a {@link Val}.
     *  @return This very instance, which enables builder-style method chaining.
     */
    public final I isFocusableIf( Val<Boolean> isFocusable ) {
        NullUtil.nullArgCheck(isFocusable, "isFocusable", Val.class);
        NullUtil.nullPropertyCheck(isFocusable, "isFocusable", "Null value for isFocusable is not allowed!");
        _onShow( isFocusable, v -> isFocusableIf(v) );
        this.isFocusableIf( isFocusable.orElseThrow() );
        return _this();
    }

    /**
     *  Use this to make the wrapped UI component focusable if a certain condition is not met.
     *  @param notFocusable The truth value determining if the UI component should be focusable or not.
     *                     If {@code false}, the component will be focusable.
     *  @return This very instance, which enables builder-style method chaining.
     */
    public final I isFocusableIfNot( boolean notFocusable ) {
        getComponent().setFocusable( !notFocusable );
        return _this();
    }

    /**
     *  Use this to dynamically make the wrapped UI component focusable.
     *  This is useful if you want to make a component focusable only if a certain condition is met.
     *  <br>
     *  <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     *  @param isFocusable The truth value determining if the UI component should be focusable or not, wrapped in a {@link Val}.
     *  @return This very instance, which enables builder-style method chaining.
     *  @throws IllegalArgumentException if the supplied {@code isFocusable} is {@code null}.
     */
    public final I isFocusableIfNot( Val<Boolean> isFocusable ) {
        NullUtil.nullArgCheck(isFocusable, "isFocusable", Val.class);
        NullUtil.nullPropertyCheck(isFocusable, "isFocusable", "Null value for isFocusable is not allowed!");
        _onShow( isFocusable, v -> isFocusableIf(!v) );
        this.isFocusableIf( !isFocusable.orElseThrow() );
        return _this();
    }

    /**
     *  Use this to make the wrapped UI component dynamically focusable or non-focusable
     *  based on the equality between the supplied enum value and enum property. <br>
     *  <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     *  @param enumValue The enum value which, if equal to the supplied enum property, makes the UI component focusable.
     *  @param enumProperty The enum property which, if equal to the supplied enum value, makes the UI component focusable.
     *  @param <E> The enum type for both the supplied enum value and enum property.
     *  @return This very instance, which enables builder-style method chaining.
     *  @throws IllegalArgumentException if the supplied {@code enumValue} or {@code enumProperty} is {@code null}.
     */
    public final <E extends Enum<E>> I isFocusableIf( E enumValue, Val<E> enumProperty ) {
        NullUtil.nullArgCheck(enumValue, "enumValue", Enum.class);
        NullUtil.nullArgCheck(enumProperty, "enumProperty", Val.class);
        NullUtil.nullPropertyCheck(enumProperty, "enumProperty", "The enumProperty may not have null values!");
        _onShow( enumProperty, v -> isFocusableIf( v == enumValue ) );
        return isFocusableIf( enumValue == enumProperty.orElseThrow() );
    }

    /**
     *  This is the inverse of {@link #isFocusableIf(Enum, Val)}.
     *  Use this to make the wrapped UI component dynamically focusable or non-focusable
     *  based on the equality between the supplied enum value and enum property. <br>
     *  <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     *  @param enumValue The enum value which, if equal to the supplied enum property, makes the UI component non-focusable.
     *  @param enumProperty The enum property which, if equal to the supplied enum value, makes the UI component non-focusable.
     *  @param <E> The enum type for both the supplied enum value and enum property.
     *  @return This very instance, which enables builder-style method chaining.
     *  @throws IllegalArgumentException if the supplied {@code enumValue} or {@code enumProperty} is {@code null}.
     */
    public final <E extends Enum<E>> I isFocusableIfNot( E enumValue, Val<E> enumProperty ) {
        NullUtil.nullArgCheck(enumValue, "enumValue", Enum.class);
        NullUtil.nullArgCheck(enumProperty, "enumProperty", Val.class);
        NullUtil.nullPropertyCheck(enumProperty, "enumProperty", "The enumProperty may not have null values!");
        _onShow( enumProperty, v -> isFocusableIf( v != enumValue ) );
        return isFocusableIf( enumValue != enumProperty.orElseThrow() );
    }


    /**
     *  Use this to make the wrapped UI component opaque.
     *  This is the inverse of {@link #makeNonOpaque()}.
     *
     *  @return This very instance, which enables builder-style method chaining.
     */
    public final I makeOpaque() {
        getComponent().setOpaque( true );
        return _this();
    }

    /**
     *  Use this to make the wrapped UI component transparent.
     *  This is the inverse of {@link #makeOpaque()}.
     *
     *  @return This very instance, which enables builder-style method chaining.
     */
    public final I makeNonOpaque() {
        getComponent().setOpaque( false );
        return _this();
    }

    /**
     *  Use this to make the wrapped UI component opaque.
     *  @param isOpaque The truth value determining if the UI component should be opaque or not.
     *  @return This very instance, which enables builder-style method chaining.
     */
    public final I isOpaqueIf( boolean isOpaque ) {
        getComponent().setOpaque( isOpaque );
        return _this();
    }

    /**
     *  Use this to dynamically make the wrapped UI component opaque.
     *  This is useful if you want to make a component opaque only if a certain condition is met.
     *  <br>
     *  <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     *  @param isOpaque The truth value determining if the UI component should be opaque or not wrapped in a {@link Val}.
     *  @return This very instance, which enables builder-style method chaining.
     */
    public final I isOpaqueIf( Val<Boolean> isOpaque ) {
        NullUtil.nullArgCheck( isOpaque, "isOpaque", Val.class );
        NullUtil.nullPropertyCheck(isOpaque, "isOpaque", "Null value for isOpaque is not allowed! A boolean should only have the values true or false!");
        _onShow( isOpaque, v -> isOpaqueIf(v) );
        this.isOpaqueIf( isOpaque.orElseThrow() );
        return _this();
    }

    /**
     *  Use this to make the wrapped UI component opaque if the given condition is not met.
     *  @param notOpaque The truth value determining if the UI component should be opaque or not, where false means opaque.
     *  @return This very instance, which enables builder-style method chaining.
     */
    public final I isOpaqueIfNot( boolean notOpaque ) {
        getComponent().setOpaque( !notOpaque );
        return _this();
    }

    /**
     *  Use this to dynamically make the wrapped UI component opaque if the boolean item of the given {@link Val} is false.
     *  This is useful if you want to make a component opaque only if a certain condition is not met.
     *  <br>
     *  <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     *  @param notOpaque The truth value determining if the UI component should be opaque or not, wrapped in a {@link Val}, where false means opaque.
     *  @return This very instance, which enables builder-style method chaining.
     */
    public final I isOpaqueIfNot( Val<Boolean> notOpaque ) {
        NullUtil.nullArgCheck( notOpaque, "notOpaque", Val.class );
        NullUtil.nullPropertyCheck(notOpaque, "notOpaque", "Null value for isOpaque is not allowed! A boolean should only have the values true or false!");
        _onShow( notOpaque, v -> isOpaqueIf(!v) );
        this.isOpaqueIf( !notOpaque.orElseThrow() );
        return _this();
    }

    /**
     *  This allows you to register validation logic for the wrapped UI component.
     *  Although the delegate exposed to the {@link UIVerifier} lambda
     *  indirectly exposes you to the UIs state, you should not access the UI directly
     *  from within the lambda, but modify the properties inside your view model instead.
     *
     * @param verifier The validation logic provided by your view model.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isValidIf( UIVerifier<C> verifier ) {
        getComponent().setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify( JComponent input ) {
                return verifier.isValid(
                        new SimpleDelegate<>(
                                getComponent(),
                                new ComponentEvent(getComponent(), 0),
                                () -> getSiblinghood()
                            )
                        );
                /*
                    We expect the user to model the state of the UI components
                    using properties in the view model.
                 */
            }
        });
        return _this();
    }

    /**
     * Adds {@link String} key/value "client property" pairs to the wrapped component.
     * <p>
     * The arguments will be passed to {@link JComponent#putClientProperty(Object, Object)}
     * which accesses
     * a small per-instance hashtable. Callers can use get/putClientProperty
     * to annotate components that were created by another module.
     * For example, a
     * layout manager might store per child constraints this way. <br>
     * This is in essence a more convenient way than the alternative usage pattern involving
     * the {@link #peek(Peeker)} method to peek into the builder's component like so: <br>
     * <pre>{@code
     *     UI.button()
     *     .peek( button -> button.putClientProperty("key", "value") );
     * }</pre>
     *
     * @param key the new client property key which may be used for styles or layout managers.
     * @param value the new client property value.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withProperty( String key, String value ) {
        getComponent().putClientProperty(key, value);
        return _this();
    }

    /**
     *  Use this to attach a border to the wrapped component.
     *
     * @param border The {@link Border} which should be set for the wrapped component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBorder( Border border ) {
        Objects.requireNonNull(border, "Null value for border is not allowed! Use an empty border instead!");
        getComponent().setBorder( border );
        return _this();
    }

    /**
     *  Use this to dynamically attach a border to the wrapped component. <br>
     *  <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param border The {@link Border} which should be set for the wrapped component wrapped in a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBorder( Val<Border> border ) {
        NullUtil.nullArgCheck(border, "border", Val.class);
        NullUtil.nullPropertyCheck(border, "border", "Null value for border is not allowed! Use an empty border instead!");
        _onShow( border, v -> getComponent().setBorder(v) );
        return this.withBorder( border.orElseNull() );
    }


    /**
     *  Use this to define an empty {@link Border} with the provided insets.
     *
     * @param top The top inset.
     * @param left The left inset.
     * @param bottom The bottom inset.
     * @param right The right inset.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withEmptyBorder( int top, int left, int bottom, int right ) {
        getComponent().setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        return _this();
    }

    /**
     *  Use this to define a titled empty {@link Border} with the provided insets.
     *
     * @param title The title of the border.
     * @param top The top inset.
     * @param left The left inset.
     * @param bottom The bottom inset.
     * @param right The right inset.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withEmptyBorderTitled( String title, int top, int left, int bottom, int right ) {
        NullUtil.nullArgCheck( title, "title", String.class );
    	getComponent().setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(top, left, bottom, right), title));
    	return _this();
    }

    /**
     *  Use this to define a titled empty {@link Border} with the provided insets
     *  and where the title is bound to a {@link Val}.
     *
     * @param title The title of the border wrapped in a {@link Val},
     *              which will update the border title dynamically when changed.
     * @param top The top inset.
     * @param left The left inset.
     * @param bottom The bottom inset.
     * @param right The right inset.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withEmptyBorderTitled( Val<String> title, int top, int left, int bottom, int right ) {
        NullUtil.nullArgCheck( title, "title", Val.class );
        NullUtil.nullPropertyCheck( title, "title", "Null value for title is not allowed! Use an empty string instead!" );
        _onShow( title, v -> getComponent().setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(top, left, bottom, right), v)) );
        return this.withEmptyBorderTitled( title.orElseNull(), top, left, bottom, right );
    }

    /**
     *  Use this to define an empty {@link Border} with the provided insets.
     *
     * @param topBottom The top and bottom insets.
     * @param leftRight The left and right insets.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withEmptyBorder( int topBottom, int leftRight ) {
        return withEmptyBorder(topBottom, leftRight, topBottom, leftRight);
    }

    /**
     *  Use this to define a titled empty {@link Border} with the provided insets.
     *
     * @param title The title of the border.
     * @param topBottom The top and bottom insets.
     * @param leftRight The left and right insets.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withEmptyBorderTitled( String title, int topBottom, int leftRight ) {
        NullUtil.nullArgCheck( title, "title", String.class );
        return withEmptyBorderTitled( title, topBottom, leftRight, topBottom, leftRight );
    }

    /**
     *  Use this to define a titled empty {@link Border} with the provided insets
     *  and where the title is bound to a {@link Val}.
     *
     * @param title The title of the border wrapped in a {@link Val}. When the value changes, the border title will be updated.
     * @param topBottom The top and bottom insets.
     * @param leftRight The left and right insets.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withEmptyBorderTitled( Val<String> title, int topBottom, int leftRight ) {
        NullUtil.nullArgCheck( title, "title", Val.class );
        NullUtil.nullPropertyCheck(title, "title", "Null value for title is not allowed! Use an empty string instead!");
        _onShow( title, v -> getComponent().setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(topBottom, leftRight, topBottom, leftRight), v)) );
        return this.withEmptyBorderTitled( title.orElseNull(), topBottom, leftRight );
    }

    /**
     *  Use this to define an empty {@link Border} with the provided insets.
     *
     * @param all The insets for all sides.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withEmptyBorder( int all ) { return withEmptyBorder(all, all, all, all); }

    /**
     *  Creates an empty and un-titled {@link Border} with the provided insets
     *  property bound to all insets of said border.
     *  <p>
     *  An empty and un-titled {@link Border} is basically just a way to add some
     *  space around the component. It is not visible by default.
     *
     * @param all The insets for all sides.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withEmptyBorder( Val<Integer> all ) {
        NullUtil.nullArgCheck( all, "all", Val.class );
        NullUtil.nullPropertyCheck(all, "all", "Null value for all is not allowed! Use an empty border instead!");
        _onShow( all, v -> getComponent().setBorder(BorderFactory.createEmptyBorder(v, v, v, v)) );
        return this.withEmptyBorder( all.get() );
    }

            /**
			 *  Use this to define a titled empty {@link Border} with the provided insets.
			 *
			 * @param title The title of the border.
			 * @param all The insets for all sides.
			 * @return This very instance, which enables builder-style method chaining.
			 */
    public final I withEmptyBorderTitled( String title, int all ) {
        NullUtil.nullArgCheck( title, "title", String.class );
        return withEmptyBorderTitled(title, all, all, all, all);
    }

    /**
     *  Creates a titled empty border bound to a {@link String} property and the provided insets.
     * @param title The title of the border in the form of a {@link Val} property.
     * @param all The insets size for all sides.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withEmptyBorderTitled( Val<String> title, int all ) {
        NullUtil.nullArgCheck( title, "title", Val.class );
        NullUtil.nullPropertyCheck(title, "title", "Null value for title is not allowed! Use an empty string instead!");
        _onShow( title, v ->
            getComponent().setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(all, all, all, all), v)
            )
        );
        return this.withEmptyBorderTitled( title.orElseNull(), all );
    }

    /**
     *  Creates a titled empty border bound to a {@link String} property.
     * @param title The title of the border in the form of a {@link Val} property.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withEmptyBorderTitled( Val<String> title ) {
        return withEmptyBorderTitled(title, 5);
    }

    /**
	 *  Use this to define a line {@link Border} with the provided color and insets.
	 *
	 * @param color The color of the line border.
	 * @param thickness The thickness of the line border.
	 * @return This very instance, which enables builder-style method chaining.
	 */
    public final I withLineBorder( Color color, int thickness ) {
        NullUtil.nullArgCheck( color, "color", Color.class );
        getComponent().setBorder(BorderFactory.createLineBorder(color, thickness));
        return _this();
    }

    /**
     *  Creates a line border bound to a {@link Color} property.
     *  When the color changes, the border will be updated with the new color.
     * @param color The color of the border in the form of a {@link Val} property.
     * @param thickness The thickness of the line border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLineBorder( Val<Color> color, int thickness ) {
        NullUtil.nullArgCheck( color, "color", Val.class );
        NullUtil.nullPropertyCheck(color, "color", "Null value for color is not allowed! Use a transparent color or other default color instead!");
        _onShow( color, v -> getComponent().setBorder(BorderFactory.createLineBorder(v, thickness)) );
        return this.withLineBorder( color.get(), thickness );
    }

    /**
	 *  Use this to define a titled line {@link Border} with the provided color and insets.
	 *
	 * @param title The title of the border.
	 * @param color The color of the line border.
	 * @param thickness The thickness of the line border.
	 * @return This very instance, which enables builder-style method chaining.
	 */
    public final I withLineBorderTitled( String title, Color color, int thickness ) {
        NullUtil.nullArgCheck( title, "title", String.class );
        NullUtil.nullArgCheck( color, "color", Color.class );
        getComponent().setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(color, thickness), title));
        return _this();
    }

    /**
     * Creates a titled line border bound to a {@link String} property.
     * @param title The title property of the border which will update the border when the value changes.
     * @param color The color of the border.
     * @param thickness The thickness of the line border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLineBorderTitled( Val<String> title, Color color, int thickness ) {
        NullUtil.nullArgCheck( title, "title", Val.class );
        NullUtil.nullPropertyCheck(title, "title", "Null value for title is not allowed! Use an empty string instead!");
        _onShow( title, v ->
            getComponent().setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(color, thickness), v)
            )
        );
        return this.withLineBorderTitled( title.orElseNull(), color, thickness );
    }

    /**
     * Creates a titled line border bound to a {@link String} property
     * and a {@link Color} property.
     * When any of the properties change, the border will be updated with the new values.
     *
     * @param title The title property of the border which will update the border when the value changes.
     * @param color The color property of the border which will update the border when the value changes.
     * @param thickness The thickness of the line border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLineBorderTitled( Val<String> title, Val<Color> color, int thickness ) {
        NullUtil.nullArgCheck( title, "title", Val.class );
        NullUtil.nullPropertyCheck(title, "title", "Null value for title is not allowed! Use an empty string instead!");
        NullUtil.nullArgCheck( color, "color", Val.class );
        NullUtil.nullPropertyCheck(color, "color", "Null value for color is not allowed! Use a transparent color or other default color instead!");
        _onShow( title, v ->
            getComponent().setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(color.get(), thickness), v)
            )
        );
        _onShow( color, v ->
            getComponent().setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(v, thickness), title.get())
            )
        );
        return this.withLineBorderTitled( title.orElseNull(), color.get(), thickness );
    }

    /**
     *  Use this to define a line {@link Border} with the provided color and a default thickness of {@code 1}.
     *
     * @param color The color of the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLineBorder( Color color ) {
        NullUtil.nullArgCheck( color, "color", Color.class );
        return withLineBorder(color, 1);
    }

    /**
     *  Use this to define a titled line {@link Border} with the provided color and a default thickness of {@code 1}.
     *
     * @param title The title of the border.
     * @param color The color of the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLineBorderTitled( String title, Color color ) {
        NullUtil.nullArgCheck( title, "title", String.class );
        NullUtil.nullArgCheck( color, "color", Color.class );
        return withLineBorderTitled( title, color, 1 );
    }

    /**
     *  Use this to attach a rounded line {@link Border} with the provided
     *  color and insets to the {@link JComponent}.
     *
     * @param color The color of the border.
     * @param thickness The thickness of the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withRoundedLineBorder( Color color, int thickness ) {
        NullUtil.nullArgCheck( color, "color", Color.class );
        getComponent().setBorder(BorderFactory.createLineBorder(color, thickness, true));
        return _this();
    }

    /**
     *  Use this to attach a titled rounded line {@link Border} with the provided
     *  color and insets to the {@link JComponent}.
     *
     * @param title The title of the border.
     * @param color The color of the border.
     * @param thickness The thickness of the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withRoundedLineBorderTitled( String title, Color color, int thickness ) {
        NullUtil.nullArgCheck( title, "title", String.class );
        NullUtil.nullArgCheck( color, "color", Color.class );
        getComponent().setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(color, thickness, true), title));
        return _this();
    }

    /**
     *  Creates a titled rounded line {@link Border} with the provided
     *  color and insets for this {@link JComponent} and binds the border to the provided
     *  title property.
     *  When the title property changes, the border will be updated with the new value.
     *
     * @param title The title property of the border which will update the border when the value changes.
     * @param color The color of the border.
     * @param thickness The thickness of the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withRoundedLineBorderTitled( Val<String> title, Color color, int thickness ) {
        NullUtil.nullArgCheck( title, "title", Val.class );
        NullUtil.nullPropertyCheck(title, "title", "Null value for title is not allowed! Use an empty string instead!");
        _onShow( title, v ->
            getComponent().setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(color, thickness, true), v)
            )
        );
        return this.withRoundedLineBorderTitled( title.orElseNull(), color, thickness );
    }

    /**
     *  Creates a titled rounded line {@link Border} with the provided
     *  color and insets for this {@link JComponent} and binds the border to the provided
     *  title and color properties.
     *  When the title or color properties change, 
     *  then the border will be updated with the new values.
     *
     * @param title The title property of the border which will update the border when the value changes.
     * @param color The color property of the border which will update the border when the value changes.
     * @param thickness The thickness of the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withRoundedLineBorderTitled( Val<String> title, Val<Color> color, int thickness ) {
        NullUtil.nullArgCheck( title, "title", Val.class );
        NullUtil.nullPropertyCheck(title, "title", "Null value for title is not allowed! Use an empty string instead!");
        NullUtil.nullArgCheck( color, "color", Val.class );
        NullUtil.nullPropertyCheck(color, "color", "Null value for color is not allowed! Use a transparent color or other default color instead!");
        _onShow( title, v ->
            getComponent().setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(color.get(), thickness, true), v)
            )
        );
        _onShow( color, v ->
            getComponent().setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(v, thickness, true), title.get())
            )
        );
        return this.withRoundedLineBorderTitled( title.get(), color.get(), thickness );
    }

    /**
     *  Use this to attach a rounded line {@link Border} with the provided
     *  color and a default thickness of {@code 1} to the {@link JComponent}.
     *
     * @param color The color of the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withRoundedLineBorder( Color color ) {
        NullUtil.nullArgCheck( color, "color", Color.class );
        return withRoundedLineBorder(color, 1);
    }

    /**
     *  Use this to attach a titled rounded line {@link Border} with the provided
     *  color property and a custom thickness to the {@link JComponent}.
     *
     * @param color The color of the border.
     * @param thickness The thickness of the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withRoundedLineBorder( Val<Color> color, int thickness ) {
        NullUtil.nullArgCheck( color, "color", Val.class );
        NullUtil.nullPropertyCheck(color, "color", "Null value for color is not allowed! Use a transparent color or other default color instead!");
        _onShow( color, v ->
            getComponent().setBorder(BorderFactory.createLineBorder(v, thickness, true))
        );
        return this.withRoundedLineBorder( color.get(), thickness );
    }

    /**
     *  Use this to attach a titled rounded line {@link Border} with the provided
     *  title, color and a default thickness of {@code 1} to the {@link JComponent}.
     *
     * @param title The title of the border.
     * @param color The color of the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withRoundedLineBorderTitled( String title, Color color ) {
        NullUtil.nullArgCheck( title, "title", String.class );
        NullUtil.nullArgCheck( color, "color", Color.class );
        return withRoundedLineBorderTitled( title, color, 1 );
    }

    /**
     *  Use this to attach a titled rounded line {@link Border} with the provided
     *  title and color to the {@link JComponent}, as well as a default thickness of {@code 1}.
     *
     * @param title The title property of the border, which will update the border when the property changes.
     * @param color The color property of the border, which will update the border when the property changes.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withRoundedLineBorderTitled( Val<String> title, Val<Color> color ) {
        NullUtil.nullArgCheck( title, "title", String.class );
        NullUtil.nullArgCheck( color, "color", Color.class );
        NullUtil.nullPropertyCheck(title, "title", "Null value for title is not allowed! Use an empty String instead!");
        return withRoundedLineBorderTitled( title, color, 1 );
    }

    /**
     *  Use this to attach a rounded black line {@link Border} with
     *  a thickness of {@code 1} to the {@link JComponent}.
     *
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withRoundedLineBorder() { return withRoundedLineBorder(Color.BLACK, 1); }

    /**
     *  Use this to attach a titled rounded black line {@link Border} with
     *  a thickness of {@code 1} to the {@link JComponent}.
     *
     * @param title The title of the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withRoundedLineBorderTitled( String title ) {
        NullUtil.nullArgCheck( title, "title", String.class );
        return withRoundedLineBorderTitled( title, Color.BLACK, 1 );
    }

    /**
     *  Creates a titled rounded black line {@link Border} with
     *  a thickness of {@code 1} to the {@link JComponent} and binds it to the provided 
     *  title property.
     *  When the property changes, the border will be updated.
     *
     * @param title The title property of the border, which will update the border when the property changes.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withRoundedLineBorderTitled( Val<String> title ) {
        NullUtil.nullArgCheck( title, "title", String.class );
        NullUtil.nullPropertyCheck(title, "title", "Null value for title is not allowed! Use an empty String instead!");
        return withRoundedLineBorderTitled( title, Color.BLACK, 1 );
    }

    /**
     *  Use this to attach a {@link javax.swing.border.MatteBorder} with the provided
     *  color and insets to the {@link JComponent}.
     *
     * @param color The color of the border.
     * @param top The top inset.
     * @param left The left inset.
     * @param bottom The bottom inset.
     * @param right The right inset.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withMatteBorder( Color color, int top, int left, int bottom, int right ) {
        NullUtil.nullArgCheck( color, "color", Color.class );
        getComponent().setBorder(BorderFactory.createMatteBorder(top, left, bottom, right, color));
        return _this();
    }

    /**
     *  Use this to attach a titled {@link javax.swing.border.MatteBorder} with the provided
     *  color and insets to the {@link JComponent}.
     *
     * @param title The title of the border.
     * @param color The color of the border.
     * @param top The top inset.
     * @param left The left inset.
     * @param bottom The bottom inset.
     * @param right The right inset.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withMatteBorderTitled( String title, Color color, int top, int left, int bottom, int right ) {
        NullUtil.nullArgCheck( title, "title", String.class );
        NullUtil.nullArgCheck( color, "color", Color.class );
        getComponent().setBorder(BorderFactory.createTitledBorder(BorderFactory.createMatteBorder(top, left, bottom, right, color), title));
        return _this();
    }

    /**
     *  Use this to attach a {@link javax.swing.border.MatteBorder} with the provided
     *  color and insets to the {@link JComponent}.
     *
     * @param color The color of the border.
     * @param topBottom The top and bottom insets.
     * @param leftRight The left and right insets.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withMatteBorder( Color color, int topBottom, int leftRight ) {
        NullUtil.nullArgCheck( color, "color", Color.class );
        return withMatteBorder(color, topBottom, leftRight, topBottom, leftRight);
    }

    /**
     *  Use this to attach a titled {@link javax.swing.border.MatteBorder} with the provided
     *  color and insets to the {@link JComponent}.
     *
     * @param title The title of the border.
     * @param color The color of the border.
     * @param topBottom The top and bottom insets.
     * @param leftRight The left and right insets.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withMatteBorderTitled( String title, Color color, int topBottom, int leftRight ) {
        NullUtil.nullArgCheck( title, "title", String.class );
        NullUtil.nullArgCheck( color, "color", Color.class );
        return withMatteBorderTitled(title, color, topBottom, leftRight, topBottom, leftRight);
    }

    /**
     *  Use this to attach a {@link javax.swing.border.MatteBorder} with the provided
     *  color and insets to the {@link JComponent}.
     *
     * @param color The color of the border.
     * @param all The insets for all sides.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withMatteBorder( Color color, int all ) {
        NullUtil.nullArgCheck( color, "color", Color.class );
        return withMatteBorder(color, all, all, all, all);
    }

    /**
     *  Use this to attach a titled {@link javax.swing.border.MatteBorder} with the provided
     *  color and insets to the {@link JComponent}.
     *
     * @param title The title of the border.
     * @param color The color of the border.
     * @param all The insets for all sides.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withMatteBorderTitled( String title, Color color, int all ) {
        NullUtil.nullArgCheck( title, "title", String.class );
        NullUtil.nullArgCheck( color, "color", Color.class );
        return withMatteBorderTitled(title, color, all, all, all, all);
    }

    /**
     *  Use this to attach a {@link javax.swing.border.CompoundBorder} with the provided
     *  borders to the {@link JComponent}.
     *
     * @param first The first border.
     * @param second The second border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withCompoundBorder( Border first, Border second ) {
        NullUtil.nullArgCheck( first, "first", Border.class );
        NullUtil.nullArgCheck( second, "second", Border.class );
        getComponent().setBorder(BorderFactory.createCompoundBorder(first, second));
        return _this();
    }

    /**
     *  Use this to attach a titled {@link javax.swing.border.CompoundBorder} with the
     *  provided borders to the {@link JComponent}.
     *
     * @param title The title of the border.
     * @param first The first border.
     * @param second The second border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withCompoundBorderTitled( String title, Border first, Border second ) {
        getComponent().setBorder(BorderFactory.createTitledBorder(BorderFactory.createCompoundBorder(first, second), title));
        return _this();
    }

    /**
     *  Use this to attach a {@link javax.swing.border.TitledBorder} with the provided title.
     *
     * @param title The title of the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBorderTitled( String title ) {
        NullUtil.nullArgCheck(title, "title", String.class);
        getComponent().setBorder(BorderFactory.createTitledBorder(title));
        return _this();
    }

    /**
     *  Use this to attach a {@link javax.swing.border.TitledBorder} with the
     *  provided title property dynamically setting the title String.
     *
     * @param title The title property for the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBorderTitled( Val<String> title ) {
        NullUtil.nullArgCheck(title, "title", Val.class);
        getComponent().setBorder(BorderFactory.createTitledBorder(title.get()));
        _onShow( title, t -> {
            Border foundBorder = getComponent().getBorder();
            if ( foundBorder instanceof TitledBorder)
                ((TitledBorder)foundBorder).setTitle(t);
            else
                getComponent().setBorder(BorderFactory.createTitledBorder(t));
        });
        return _this();
    }

    /**
     *  Use this set the cursor type which should be displayed
     *  when hovering over the UI component wrapped by this builder.
     *  <br>
     *  Here an example of how to use this method:
     *  <pre>{@code
     *      UI.button("Click me!").with(UI.Cursor.HAND);
     *  }</pre>
     *
     * @param type The {@link UI.Cursor} type defined by a simple enum exposed by this API.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I with( UI.Cursor type ) {
        getComponent().setCursor( new java.awt.Cursor( type.type ) );
        return _this();
    }

    /**
     *  Use this to dynamically set the cursor type which should be displayed
     *  when hovering over the UI component wrapped by this builder. <br>
     *  <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param type The {@link UI.Cursor} type defined by a simple enum exposed by this API wrapped in a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withCursor( Val<UI.Cursor> type ) {
        NullUtil.nullArgCheck( type, "type", Val.class );
        NullUtil.nullPropertyCheck(type, "type", "Null is not allowed to model a cursor type.");
        _onShow( type, t -> getComponent().setCursor( new java.awt.Cursor( t.type ) ) );
        return with( type.orElseThrow() );
    }

    /**
     *  Use this to set the cursor type which should be displayed
     *  when hovering over the UI component wrapped by this builder
     *  based on boolean property determining if the provided cursor should be set ot not. <br>
     *  <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param condition The boolean property determining if the provided cursor should be set ot not.
     * @param type The {@link UI.Cursor} type defined by a simple enum exposed by this API wrapped in a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withCursorIf( Val<Boolean> condition, UI.Cursor type ) {
        NullUtil.nullArgCheck( condition, "condition", Val.class );
        NullUtil.nullArgCheck( type, "type", UI.Cursor.class );
        NullUtil.nullPropertyCheck(condition, "condition", "Null is not allowed to model the cursor selection state.");
        _onShow( condition, c -> getComponent().setCursor( new java.awt.Cursor( c ? type.type : UI.Cursor.DEFAULT.type ) ) );
        return with( condition.orElseThrow() ? type : UI.Cursor.DEFAULT );
    }

    /**
     *  Use this to dynamically set the cursor type which should be displayed
     *  when hovering over the UI component wrapped by this builder
     *  based on boolean property determining if the provided cursor should be set ot not. <br>
     *  <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param condition The boolean property determining if the provided cursor should be set ot not.
     * @param type The {@link UI.Cursor} type property defined by a simple enum exposed by this API.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withCursorIf( Val<Boolean> condition, Val<UI.Cursor> type ) {
        NullUtil.nullArgCheck( condition, "condition", Val.class );
        NullUtil.nullArgCheck( type, "type", Val.class );
        NullUtil.nullPropertyCheck(condition, "condition", "Null is not allowed to model the cursor selection state.");
        NullUtil.nullPropertyCheck(type, "type", "Null is not allowed to model a cursor type.");
        Cursor[] baseCursor = new Cursor[1];
        _onShow( condition, c -> type.fireSet() );
        _onShow( type, c ->{
            if (baseCursor[0] == null) baseCursor[0] = getComponent().getCursor();
            getComponent().setCursor( new java.awt.Cursor( condition.get() ? c.type : baseCursor[0].getType() ) );
        });
        return with( condition.orElseThrow() ? type.orElseThrow() : UI.Cursor.DEFAULT );
    }

    /**
     *  Use this to set the {@link LayoutManager} of the component wrapped by this builder. <br>
     *  This is in essence a more convenient way than the alternative usage pattern involving
     *  the {@link #peek(Peeker)} method to peek into the builder's component like so: <br>
     *  <pre>{@code
     *      UI.panel()
     *      .peek( panel -> panel.setLayout(new FavouriteLayoutManager()) );
     *  }</pre>
     *
     * @param layout The {@link LayoutManager} which should be supplied to the wrapped component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout( LayoutManager layout ) {
        if ( _migAlreadySet )
            throw new IllegalArgumentException("The mig layout has already been specified for this component!");
        getComponent().setLayout(layout);
        return _this();
    }

    /**
     *  Use this to set a {@link FlowLayout} for the component wrapped by this builder. <br>
     *  This is in essence a more convenient way than the alternative usage pattern involving
     *  the {@link #peek(Peeker)} method to peek into the builder's component like so: <br>
     *  <pre>{@code
     *      UI.panel()
     *      .peek( panel -> panel.setLayout(new FlowLayout()) );
     *  }</pre>
     *
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withFlowLayout() { return this.withLayout(new FlowLayout()); }

    /**
     *  Use this to set a {@link GridLayout} for the component wrapped by this builder. <br>
     *  This is in essence a more convenient way than the alternative usage pattern involving
     *  the {@link #peek(Peeker)} method to peek into the builder's component like so: <br>
     *  <pre>{@code
     *      UI.panel()
     *      .peek( panel -> panel.setLayout(new GridLayout()) );
     *  }</pre>
     *
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withGridLayout() { return this.withLayout(new GridLayout()); }

    /**
     *  Use this to set a new {@link GridBagLayout} for the component wrapped by this builder. <br>
     *  This is in essence a more convenient way than the alternative usage pattern involving
     *  the {@link #peek(Peeker)} method to peek into the builder's component like so: <br>
     *  <pre>{@code
     *      UI.panel()
     *      .peek( panel -> panel.setLayout(new GridBagLayout()) );
     *  }</pre>
     *  ...or specifying the layout manager like so: <br>
     *  <pre>{@code
     *    UI.panel().withLayout( new GridBagLayout() );
     *  }</pre>
     *
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withGridBagLayout() { return this.withLayout(new GridBagLayout()); }

    /**
     *  Use this to set a {@link GridLayout} for the component wrapped by this builder. <br>
     *  This is in essence a more convenient way than the alternative usage pattern involving
     *  the {@link #peek(Peeker)} method to peek into the builder's component like so: <br>
     *  <pre>{@code
     *      UI.panel()
     *      .peek( panel -> panel.setLayout(new GridLayout(rows, cols)) );
     *  }</pre>
     *
     * @param rows The number of rows in the grid.
     * @param cols The number of columns in the grid.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withGridLayout( int rows, int cols ) { return this.withLayout(new GridLayout(rows, cols)); }

    /**
     *  Passes the provided string to the layout manager of the wrapped component.
     *  By default, a {@link MigLayout} is used for the component wrapped by this UI builder.
     *
     * @param attr A string defining the layout (usually mig layout).
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout( String attr ) { return withLayout(attr, null); }

    /**
     *  Passes the provided string to the {@link MigLayout} manager of the wrapped component.
     *
     * @param attr Essentially an immutable string wrapper defining the mig layout.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout( LayoutAttr attr ) { return withLayout(attr.toString(), null); }

    /**
     *  This creates a {@link MigLayout} for the component wrapped by this UI builder.
     *
     * @param attr The constraints for the layout.
     * @param colConstrains The column layout for the {@link MigLayout} instance.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout( String attr, String colConstrains ) {
        return withLayout(attr, colConstrains, null);
    }

    /**
     * @param attr The constraints for the layout.
     * @param colConstrains The column layout for the {@link MigLayout} instance.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout( LayoutAttr attr, String colConstrains ) {
        return withLayout(attr.toString(), colConstrains, null);
    }

    /**
     *  This creates a {@link MigLayout} for the component wrapped by this UI builder.
     *
     * @param attr The constraints for the layout.
     * @param colConstrains The column layout for the {@link MigLayout} instance.
     * @param rowConstraints The row layout for the {@link MigLayout} instance.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout( LayoutAttr attr, String colConstrains, String rowConstraints ) {
        return withLayout(attr.toString(), colConstrains, rowConstraints);
    }

    /**
     *  This creates a {@link MigLayout} for the component wrapped by this UI builder.
     *
     * @param constraints The constraints for the layout.
     * @param colConstrains The column layout for the {@link MigLayout} instance.
     * @param rowConstraints The row layout for the {@link MigLayout} instance.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout( String constraints, String colConstrains, String rowConstraints ) {
        if (_migAlreadySet)
            throw new IllegalArgumentException("The mig layout has already been specified for this component!");

        // We make sure the default hidemode is 2 instead of 3 (which sucks because it takes up too much space)
        if ( constraints == null )
            constraints = "hidemode 2";
        else if ( !constraints.contains("hidemode") )
            constraints += ", hidemode 2";

        MigLayout migLayout = new MigLayout(constraints, colConstrains, rowConstraints);
        getComponent().setLayout(migLayout);
        _migAlreadySet = true;
        return _this();
    }

    /**
     *  Use this to set a helpful tool tip text for this UI component.
     *  The tool tip text will be displayed when the mouse hovers on the
     *  UI component for some time. <br>
     *  This is in essence a convenience method, which avoid having to expose the underlying component
     *  through the {@link #peek(Peeker)} method like so: <br>
     *  <pre>{@code
     *      UI.button("Click Me")
     *      .peek( button -> button.setToolTipText("Can be clicked!") );
     *  }</pre>
     *
     * @param tooltip The tool tip text which should be set for the UI component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withTooltip( String tooltip ) {
        NullUtil.nullArgCheck(tooltip, "tooltip", String.class, "Use the empty string to clear the tooltip text!");
        getComponent().setToolTipText(tooltip.isEmpty() ? null : tooltip);
        return _this();
    }

    /**
     *  Use this to bind to a {@link sprouts.Val}
     *  containing a tooltip string.
     *  This is a convenience method, which would
     *  be equivalent to:
     *  <pre>{@code
     *      UI.button("Click Me")
     *      .peek( button -> {
     *          tip.onSetItem(JButton::setToolTipText);
     *          button.setToolTipText(tip.get());
     *      });
     *  }</pre><br>
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param tip The tooltip which should be displayed when hovering over the tab header.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final I withTooltip( Val<String> tip ) {
        NullUtil.nullArgCheck(tip, "tip", Val.class);
        NullUtil.nullPropertyCheck(tip, "tip", "Please use an empty string instead of null!");
        _onShow( tip, v -> getComponent().setToolTipText(v.isEmpty() ? null : v) );
        return this.withTooltip( tip.orElseThrow() );
    }

    /**
     *  Use this to set the background color of the UI component
     *  wrapped by this builder.<br>
     *  This is in essence a convenience method, which avoid having to expose the underlying component
     *  through the {@link #peek(Peeker)} method like so: <br>
     *  <pre>{@code
     *      UI.label("Something")
     *      .peek( label -> label.setBackground(Color.CYAN) );
     *  }</pre>
     *
     * @param color The background color which should be set for the UI component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBackground( Color color ) {
        NullUtil.nullArgCheck(color, "color", Color.class);
        getComponent().setBackground(color);
        return _this();
    }

    /**
     *  Use this to bind to a {@link sprouts.Val}
     *  containing a background color.
     *  This is a convenience method, which would
     *  be equivalent to:
     *  <pre>{@code
     *      UI.button("Click Me")
     *      .peek( button -> {
     *          bg.onSetItem(JButton::setBackground);
     *          button.setBackground(bg.get());
     *      });
     *  }</pre><br>
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param bg The background color which should be set for the UI component wrapped by a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBackground( Val<Color> bg ) {
        NullUtil.nullArgCheck(bg, "bg", Val.class);
        NullUtil.nullPropertyCheck(bg, "bg", "Please use the default color of this component instead of null!");
        _onShow( bg, v -> getComponent().setBackground(v) );
        return this.withBackground( bg.orElseNull() );
    }

    /**
     *  Use this to bind to a background color
     *  which will be set dynamically based on a boolean property.
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param colorIfTrue The background color which should be set for the UI component.
     * @param condition The condition property which determines whether the background color should be set or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBackgroundIf( Val<Boolean> condition, Color colorIfTrue ) {
        NullUtil.nullArgCheck(condition, "condition", Val.class);
        NullUtil.nullArgCheck(colorIfTrue, "bg", Color.class);
        NullUtil.nullPropertyCheck(condition, "condition", "Null is not allowed to model the usage of the provided background color!");
        Var<Color> baseColor = Var.of( getComponent().getBackground() );
        Var<Color> color = Var.of( colorIfTrue );
        _onShow( condition, v -> _updateBackground( condition, color, baseColor ) );
        return this.withBackground( condition.orElse(false) ? colorIfTrue : getComponent().getBackground() );
    }

    /**
     *  Use this to dynamically bind to a background color
     *  which will be set dynamically based on a boolean property.
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param color The background color property which should be set for the UI component.
     * @param condition The condition property which determines whether the background color should be set or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBackgroundIf( Val<Boolean> condition, Val<Color> color ) {
        NullUtil.nullArgCheck(condition, "condition", Val.class);
        NullUtil.nullArgCheck(color, "color", Val.class);
        NullUtil.nullPropertyCheck(condition, "condition", "Null is not allowed to model the usage of the provided background color!");
        NullUtil.nullPropertyCheck(color, "color", "Null is not allowed to model the the provided background color! Please use the default color of this component instead.");
        Var<Color> baseColor = Var.of(getComponent().getBackground());
        _onShow( condition, setColor -> _updateBackground(condition, color, baseColor) );
        _onShow( color, v -> _updateBackground(condition, color, baseColor) );
        return this.withBackground(condition.orElse(false) ? color.orElseThrow() : getComponent().getBackground());
    }

    /**
     *  Use this to bind to 2 colors to the background of the component
     *  which sre set based on the value of a boolean property.
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param condition The condition property which determines whether the background color should be set or not.
     * @param colorIfTrue The background color which should be set for the UI component.
     * @param colorIfFalse The background color which should be set for the UI component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBackgroundIf( Val<Boolean> condition, Color colorIfTrue, Color colorIfFalse ) {
        return this.withBackgroundIf( condition, Var.of(colorIfTrue), Var.of(colorIfFalse) );
    }

    /**
     *  Use this to bind to 2 color properties to the background of the component
     *  which sre set based on the value of a boolean property.
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param condition The condition property which determines whether the background color should be set or not.
     * @param colorIfTrue The background color which should be set for the UI component.
     * @param colorIfFalse The background color which should be set for the UI component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBackgroundIf( Val<Boolean> condition, Val<Color> colorIfTrue, Val<Color> colorIfFalse ) {
        NullUtil.nullArgCheck(condition, "condition", Val.class);
        NullUtil.nullArgCheck(colorIfTrue, "colorIfTrue", Val.class);
        NullUtil.nullArgCheck(colorIfFalse, "colorIfFalse", Val.class);
        NullUtil.nullPropertyCheck(condition, "condition", "Null is not allowed to model the usage of the provided background color!");
        _onShow( condition, v -> _updateBackground( condition, colorIfTrue, Var.of(colorIfFalse.get()) ) );
        _onShow( colorIfTrue, v -> _updateBackground( condition, colorIfTrue, Var.of(colorIfFalse.get()) ) );
        _onShow( colorIfFalse, v -> _updateBackground( condition, colorIfTrue, Var.of(colorIfFalse.get()) ) );
        return this.withBackground( condition.get() ? colorIfTrue.orElseNull() : colorIfFalse.orElseNull() );
    }

    /**
     *  Set the color of this {@link JComponent}. (This is usually the font color for components displaying text) <br>
     *  This is in essence a convenience method, which avoid having to expose the underlying component
     *  through the {@link #peek(Peeker)} method like so: <br>
     *  <pre>{@code
     *      UI.label("Something")
     *      .peek( label -> label.setForeground(Color.GRAY) );
     *  }</pre>
     *
     * @param color The color of the foreground (usually text).
     * @return This very builder to allow for method chaining.
     */
    public final I withForeground( Color color ) {
        NullUtil.nullArgCheck(color, "color", Color.class);
        getComponent().setForeground(color);
        return _this();
    }

    /**
     *  Use this to bind to a {@link sprouts.Val}
     *  containing a foreground color.
     *  This is a convenience method, which would
     *  be equivalent to:
     *  <pre>{@code
     *      UI.button("Click Me")
     *      .peek( button -> {
     *          fg.onSetItem(JButton::setForeground);
     *          button.setForeground(fg.get());
     *      });
     *  }</pre><br>
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param fg The foreground color which should be set for the UI component wrapped by a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withForeground( Val<Color> fg ) {
        NullUtil.nullArgCheck(fg, "fg", Val.class);
        NullUtil.nullPropertyCheck(fg, "fg", "Please use the default color of this component instead of null!");
        _onShow( fg, v -> getComponent().setForeground(v) );
        return this.withForeground( fg.orElseNull() );
    }
    
    /**
     *  Use this to bind to a foreground color
     *  which will be set dynamically based on a boolean property.
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param fg The foreground color which should be set for the UI component.
     * @param condition The condition property which determines whether the foreground color should be set or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withForegroundIf( Val<Boolean> condition, Color fg ) {
        NullUtil.nullArgCheck(condition, "condition", Val.class);
        NullUtil.nullArgCheck(fg, "fg", Color.class);
        NullUtil.nullPropertyCheck(condition, "condition", "Null is not allowed to model the usage of the provided foreground color!");
        Var<Color> baseColor = Var.of(getComponent().getForeground());
        Var<Color> newColor = Var.of(fg);
        _onShow( condition, v -> _updateForeground( condition, newColor, baseColor ) );
        return this.withForeground( condition.orElse(false) ? fg : getComponent().getForeground() );
    }
    
    /**
     *  Use this to dynamically bind to a foreground color
     *  which will be set dynamically based on a boolean property.
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param color The foreground color property which should be set for the UI component.
     * @param condition The condition property which determines whether the foreground color should be set or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withForegroundIf( Val<Boolean> condition, Val<Color> color ) {
        NullUtil.nullArgCheck(condition, "condition", Val.class);
        NullUtil.nullArgCheck(color, "color", Val.class);
        NullUtil.nullPropertyCheck(condition, "condition", "Null is not allowed to model the usage of the provided foreground color!");
        NullUtil.nullPropertyCheck(color, "color", "Null is not allowed to model the the provided foreground color! Please use the default color of this component instead.");
        Var<Color> baseColor = Var.of(getComponent().getForeground());
        _onShow( condition, setColor -> _updateForeground(condition, color, baseColor) );
        _onShow( color, v -> _updateForeground(condition, color, baseColor) );
        return this.withForeground(condition.orElse(false) ? color.orElseThrow() : getComponent().getForeground());
    }

    /**
     *  Use this to dynamically bind to a foreground color
     *  which will be set dynamically based on a boolean property.
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param condition The condition property which determines whether the foreground color should be set or not.
     * @param colorIfTrue The foreground color which should be set for the UI component.
     * @param colorIfFalse The foreground color which should be set for the UI component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withForegroundIf( Val<Boolean> condition, Color colorIfTrue, Color colorIfFalse ) {
        NullUtil.nullArgCheck(condition, "condition", Val.class);
        NullUtil.nullArgCheck(colorIfTrue, "colorIfTrue", Color.class);
        NullUtil.nullArgCheck(colorIfFalse, "colorIfFalse", Color.class);
        NullUtil.nullPropertyCheck(condition, "condition", "Null is not allowed to model the usage of the provided foreground color!");
        _onShow( condition, v -> _updateForeground( condition, Var.of(colorIfTrue), Var.of(colorIfFalse) ) );
        return this.withForeground( condition.get() ? colorIfTrue : colorIfFalse );
    }

    /**
     *  Use this to dynamically bind to a foreground color
     *  which will be set dynamically based on a boolean property.
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param condition The condition property which determines whether the foreground color should be set or not.
     * @param colorIfTrue The foreground color property which should be set for the UI component.
     * @param colorIfFalse The foreground color property which should be set for the UI component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withForegroundIf( Val<Boolean> condition, Val<Color> colorIfTrue, Val<Color> colorIfFalse ) {
        NullUtil.nullArgCheck(condition, "condition", Val.class);
        NullUtil.nullArgCheck(colorIfTrue, "colorIfTrue", Val.class);
        NullUtil.nullArgCheck(colorIfFalse, "colorIfFalse", Val.class);
        NullUtil.nullPropertyCheck(condition, "condition", "Null is not allowed to model the usage of the provided foreground color!");
        NullUtil.nullPropertyCheck(colorIfTrue, "colorIfTrue", "Null is not allowed to model the the provided foreground color! Please use the default color of this component instead.");
        NullUtil.nullPropertyCheck(colorIfFalse, "colorIfFalse", "Null is not allowed to model the the provided foreground color! Please use the default color of this component instead.");
        _onShow( condition, v -> _updateForeground( condition, colorIfTrue, Var.of(colorIfFalse.get()) ) );
        _onShow( colorIfTrue, v -> _updateForeground( condition, colorIfTrue, Var.of(colorIfFalse.get()) ) );
        _onShow( colorIfFalse, v -> _updateForeground( condition, colorIfTrue, Var.of(colorIfFalse.get()) ) );
        return this.withForeground( condition.get() ? colorIfTrue.get() : colorIfFalse.get() );
    }

    private void _updateForeground(
        Val<Boolean> condition,
        Val<Color> color,
        Val<Color> baseColor
    ) {
        if ( condition.get() )
            getComponent().setForeground(color.get());
        else
            getComponent().setForeground(baseColor.get());
    }

    private void _updateBackground(
            Val<Boolean> condition,
            Val<Color> color,
            Val<Color> baseColor
    ) {
        if ( condition.get() )
            getComponent().setBackground(color.get());
        else
            getComponent().setBackground(baseColor.get());
    }

    /**
     *  Set the minimum {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component. <br>
     * @param size The minimum {@link Dimension} of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMinSize( Dimension size ) {
        NullUtil.nullArgCheck(size, "size", Dimension.class);
        getComponent().setMinimumSize(size);
        return _this();
    }

    /**
     *  Bind to a {@link Val} object to
     *  dynamically set the maximum {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} (Dimension)} on the underlying component. <br>
     *  This is a convenience method, which would
     *  be equivalent to:
     *  <pre>{@code
     *    UI.button("Click Me")
     *    .peek( button -> {
     *      size.onSetItem(JButton::setMinimumSize);
     *      button.setMinimumSize(size.get());
     *    });
     *  }</pre>
     *
     * @param size The minimum {@link Dimension} of the component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withMinSize( Val<Dimension> size ) {
        NullUtil.nullArgCheck(size, "size", Val.class);
        NullUtil.nullPropertyCheck(size, "size", "Null is not allowed to model the minimum size of this component!");
        _onShow( size, v -> {
            C comp = getComponent();
            comp.setMinimumSize(v);
            comp.revalidate();
        });
        return this.withMinSize( size.orElseThrow() );
    }

    /**
     *  Set the minimum width and heigh ({@link Dimension}) of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component. <br>
     * @param width The minimum width of the component.
     * @param height The minimum height of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMinSize( int width, int height ) {
        getComponent().setMinimumSize(new Dimension(width, height));
        return _this();
    }

    /**
     *  Bind to a {@link Val} object to
     *  dynamically set the minimum {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component. <br>
     * @param width The minimum width of the component.
     * @param height The minimum height of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMinSize( Val<Integer> width, Val<Integer> height ) {
        NullUtil.nullArgCheck(width, "width", Val.class);
        NullUtil.nullArgCheck(height, "height", Val.class);
        NullUtil.nullPropertyCheck(width, "width", "Null is not allowed to model the minimum width of this component!");
        NullUtil.nullPropertyCheck(height, "height", "Null is not allowed to model the minimum height of this component!");
        _onShow( width, w -> {
            C comp = getComponent();
            comp.setMinimumSize(new Dimension(w, getComponent().getMinimumSize().height));
            comp.revalidate();
        });
        _onShow( height, h -> {
            C comp = getComponent();
            comp.setMinimumSize(new Dimension(getComponent().getMinimumSize().width, h));
            comp.revalidate();
        });
        return this.withMinSize( width.orElseThrow(), height.orElseThrow() );
    }

    /**
     *  Use this to only set the minimum width of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component for you. <br>
     * @param width The minimum width which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMinWidth( int width ) {
        getComponent().setMinimumSize(new Dimension(width, getComponent().getMinimumSize().height));
        return _this();
    }

    /**
     *  Use this to dynamically set only the minimum width of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component for you. <br>
     * @param width The minimum width which should be set for the underlying component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withMinWidth( Val<Integer> width ) {
        NullUtil.nullArgCheck(width, "width", Val.class);
        NullUtil.nullPropertyCheck(width, "width", "Null is not allowed to model the minimum width of this component!");
        _onShow( width, w -> {
            C comp = getComponent();
            comp.setMinimumSize(new Dimension(w, getComponent().getMinimumSize().height));
            comp.revalidate(); // Swing is not smart enough to do this automatically
        });
        return this.withMinWidth( width.orElseThrow() );
    }


    /**
     *  Use this to only set the minimum height of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component for you. <br>
     * @param height The minimum height which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMinHeight( int height ) {
        getComponent().setMinimumSize(new Dimension(getComponent().getMinimumSize().width, height));
        return _this();
    }

    /**
     *  Use this to dynamically set only the minimum height of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component for you. <br>
     * @param height The minimum height which should be set for the underlying component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withMinHeight( Val<Integer> height ) {
        NullUtil.nullArgCheck(height, "height", Val.class);
        NullUtil.nullPropertyCheck(height, "height", "Null is not allowed to model the minimum height of this component!");
        _onShow( height, h -> {
            C comp = getComponent();
            comp.setMinimumSize( new Dimension(getComponent().getMinimumSize().width, h));
            comp.revalidate(); // The revalidate is necessary to make the change visible.
        });
        return this.withMinHeight( height.orElseThrow() );
    }

    /**
     *  Set the maximum {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     * @param size The maximum {@link Dimension} of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMaxSize( Dimension size ) {
        NullUtil.nullArgCheck(size, "size", Dimension.class);
        getComponent().setMaximumSize(size);
        return _this();
    }

    /**
     *  Bind to a {@link Val} object to
     *  dynamically set the maximum {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     * @param size The maximum {@link Dimension} of the component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withMaxSize( Val<Dimension> size ) {
        NullUtil.nullArgCheck(size, "size", Val.class);
        NullUtil.nullPropertyCheck(size, "size", "Null is not allowed to model the maximum size of this component!");
        _onShow( size, v -> {
            C comp = getComponent();
            comp.setMaximumSize(v);
            comp.revalidate(); // For some reason this is needed to make the change visible.
        });
        return this.withMaxSize( size.orElseThrow() );
    }

    /**
     *  Set the maximum width and height ({@link Dimension}) of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     * @param width The maximum width of the component.
     * @param height The maximum height of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMaxSize( int width, int height ) {
        getComponent().setMaximumSize(new Dimension(width, height));
        return _this();
    }

    /**
     *  Bind to a {@link Val} object to
     *  dynamically set the maximum {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     * @param width The maximum width of the component.
     * @param height The maximum height of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMaxSize( Val<Integer> width, Val<Integer> height ) {
        NullUtil.nullArgCheck(width, "width", Val.class);
        NullUtil.nullArgCheck(height, "height", Val.class);
        NullUtil.nullPropertyCheck(width, "width", "Null is not allowed to model the maximum width of this component!");
        NullUtil.nullPropertyCheck(height, "height", "Null is not allowed to model the maximum height of this component!");
        _onShow( width, w -> {
            C comp = getComponent();
            comp.setMaximumSize(new Dimension(w, getComponent().getMaximumSize().height));
            comp.revalidate(); // Swing is not smart enough to do this automatically :(
        });
        _onShow( height, h -> {
            C comp = getComponent();
            comp.setMaximumSize(new Dimension(getComponent().getMaximumSize().width, h));
            comp.revalidate(); // Still not smart enough to do this automatically :(
        });
        return this.withMaxSize( width.orElseThrow(), height.orElseThrow() );
    }

    /**
     *  Use this to only set the maximum width of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component for you. <br>
     * @param width The maximum width which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMaxWidth( int width ) {
        getComponent().setMaximumSize(new Dimension(width, getComponent().getMaximumSize().height));
        return _this();
    }

    /**
     *  Use this to dynamically set only the maximum width of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component for you. <br>
     * @param width The maximum width which should be set for the underlying component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withMaxWidth( Val<Integer> width ) {
        NullUtil.nullArgCheck(width, "width", Val.class);
        NullUtil.nullPropertyCheck(width, "width", "Null is not allowed to model the maximum width of this component!");
        _onShow( width, w -> {
            C comp = getComponent();
            comp.setMaximumSize(new Dimension(w, getComponent().getMaximumSize().height));
            comp.revalidate(); // When the size changes, the layout manager needs to be informed.
        });
        return this.withMaxWidth( width.orElseThrow() );
    }

    /**
     *  Use this to only set the maximum height of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component for you. <br>
     * @param height The maximum height which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMaxHeight( int height ) {
        getComponent().setMaximumSize(new Dimension(getComponent().getMaximumSize().width, height));
        return _this();
    }

    /**
     *  Use this to dynamically set only the maximum height of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component for you. <br>
     * @param height The maximum height which should be set for the underlying component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withMaxHeight( Val<Integer> height ) {
        NullUtil.nullArgCheck(height, "height", Val.class);
        NullUtil.nullPropertyCheck(height, "height", "Null is not allowed to model the maximum height of this component!");
        _onShow( height, h -> {
            C comp = getComponent();
            comp.setMaximumSize(new Dimension(getComponent().getMaximumSize().width, h));
            comp.revalidate(); // The revalidate is necessary to make the change visible, this makes sure the layout is recalculated.
        });
        return this.withMaxHeight( height.orElseThrow() );
    }

    /**
     *  Set the preferred {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     * @param size The preferred {@link Dimension} of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefSize( Dimension size ) {
        NullUtil.nullArgCheck(size, "size", Dimension.class);
        getComponent().setPreferredSize(size);
        return _this();
    }

    /**
     *  Bind to a {@link Val} object to
     *  dynamically set the preferred {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     * @param size The preferred {@link Dimension} of the component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefSize( Val<Dimension> size ) {
        NullUtil.nullArgCheck(size, "size", Val.class);
        NullUtil.nullPropertyCheck(size, "size", "Null is not allowed to model the preferred size of this component!");
        _onShow( size, v -> {
            C comp = getComponent();
            comp.setPreferredSize(v);
            comp.revalidate();
        });
        return this.withPrefSize( size.orElseNull() );
    }

    /**
     *  Set the preferred width and height ({@link Dimension}) of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     * @param width The preferred width of the component.
     * @param height The preferred height of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefSize( int width, int height ) {
        getComponent().setPreferredSize(new Dimension(width, height));
        return _this();
    }

    /**
     *  Bind to a {@link Val} object to
     *  dynamically set the preferred {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     * @param width The preferred width of the component.
     * @param height The preferred height of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefSize( Val<Integer> width, Val<Integer> height ) {
        NullUtil.nullArgCheck(width, "width", Val.class);
        NullUtil.nullArgCheck(height, "height", Val.class);
        NullUtil.nullPropertyCheck(width, "width", "Null is not allowed to model the preferred width of this component!");
        NullUtil.nullPropertyCheck(height, "height", "Null is not allowed to model the preferred height of this component!");
        _onShow( width, w -> {
            C comp = getComponent();
            comp.setPreferredSize(new Dimension(w, getComponent().getPreferredSize().height));
            comp.revalidate(); // We need to revalidate the component to make sure the layout manager is aware of the new size.
        });
        _onShow( height, h -> {
            C comp = getComponent();
            comp.setPreferredSize(new Dimension(getComponent().getPreferredSize().width, h));
            comp.revalidate(); // We need to revalidate the component to make sure the layout manager is aware of the new size.
        });
        return this.withPrefSize( width.orElseThrow(), height.orElseThrow() );
    }

    /**
     *  Use this to only set the preferred width of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component for you. <br>
     * @param width The preferred width which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefWidth( int width ) {
        getComponent().setPreferredSize(new Dimension(width, getComponent().getPreferredSize().height));
        return _this();
    }

    /**
     *  Use this to dynamically set only the preferred width of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component for you. <br>
     * @param width The preferred width which should be set for the underlying component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefWidth( Val<Integer> width ) {
        NullUtil.nullArgCheck(width, "width", Val.class);
        NullUtil.nullPropertyCheck(width, "width", "Null is not allowed to model the preferred width of this component!");
        _onShow( width, w -> {
            C comp = getComponent();
            comp.setPreferredSize(new Dimension(w, getComponent().getPreferredSize().height));
            comp.revalidate(); // We need to revalidate the component to make sure the new preferred size is applied.
        });
        return this.withPrefWidth( width.orElseThrow() );
    }

    /**
     *  Use this to only set the preferred height of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component for you. <br>
     * @param height The preferred height which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefHeight( int height ) {
        getComponent().setPreferredSize(new Dimension(getComponent().getPreferredSize().width, height));
        return _this();
    }

    /**
     *  Use this to dynamically set only the preferred height of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component for you. <br>
     * @param height The preferred height which should be set for the underlying component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefHeight(Val<Integer> height ) {
        NullUtil.nullArgCheck(height, "height", Val.class);
        NullUtil.nullPropertyCheck(height, "height", "Null is not allowed to model the preferred height of this component!");
        _onShow( height, h -> {
            C comp = getComponent();
            comp.setPreferredSize(new Dimension(getComponent().getPreferredSize().width, h));
            comp.revalidate(); // We need to revalidate the component to make sure the new preferred size is applied.
        });
        return this.withPrefHeight( height.orElseThrow() );
    }

    /**
     *  Set the current {@link Dimension})/size (width and height) of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param size The current {@link Dimension} of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withSize( Dimension size ) {
        NullUtil.nullArgCheck(size, "size", Dimension.class);
        getComponent().setSize(size);
        return _this();
    }

    /**
     *  Bind to a {@link Val} object to
     *  dynamically set the current {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param size The current {@link Dimension} of the component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withSize( Val<Dimension> size ) {
        NullUtil.nullArgCheck(size, "size", Val.class);
        NullUtil.nullPropertyCheck(size, "size", "Null is not allowed to model the size of this component!");
        _onShow( size, v -> {
            C comp = getComponent();
            comp.setSize(v);
            comp.revalidate(); // We need to revalidate the component to make sure the new size is applied.
        });
        return this.withSize( size.orElseNull() );
    }

    /**
     *  Set the current width of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param width The current width of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withWidth( int width ) {
        getComponent().setSize(new Dimension(width, getComponent().getSize().height));
        return _this();
    }

    /**
     *  Bind to a {@link Val} object to
     *  dynamically set the current width of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param width The current width of the component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withWidth( Val<Integer> width ) {
        NullUtil.nullArgCheck(width, "width", Val.class);
        NullUtil.nullPropertyCheck(width, "width", "Null is not allowed to model the width of this component!");
        _onShow( width, w -> {
            C comp = getComponent();
            comp.setSize(new Dimension(w, getComponent().getSize().height));
            comp.revalidate(); // We need to revalidate the component to make sure the new size is applied.
        });
        return this.withWidth( width.orElseThrow() );
    }

    /**
     *  Set the current height of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param height The current height of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withHeight( int height ) {
        getComponent().setSize(new Dimension(getComponent().getSize().width, height));
        return _this();
    }

    /**
     *  Bind to a {@link Val} object to
     *  dynamically set the current height of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param height The current height of the component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withHeight( Val<Integer> height ) {
        NullUtil.nullArgCheck(height, "height", Val.class);
        NullUtil.nullPropertyCheck(height, "height", "Null is not allowed to model the height of this component!");
        _onShow( height, h -> {
            C comp = getComponent();
            comp.setSize(new Dimension(getComponent().getSize().width, h));
            comp.revalidate(); // We need to revalidate the component to make sure the new size is applied.
        });
        return this.withHeight( height.orElseThrow() );
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse click events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the wrapped
     *  button component.
     *  <br><br>
     *
     * @param onClick The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseClick( Action<SimpleDelegate<C, MouseEvent>> onClick ) {
        NullUtil.nullArgCheck(onClick, "onClick", Action.class);
        C component = getComponent();
        component.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { 
                _doApp(() -> onClick.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse release events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the wrapped
     *  button component.
     *  <br><br>
     *
     * @param onRelease The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseRelease( Action<SimpleDelegate<C, MouseEvent>> onRelease ) {
        NullUtil.nullArgCheck(onRelease, "onRelease", Action.class);
        C component = getComponent();
        component.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                _doApp(() -> onRelease.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse press events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the wrapped
     *  button component.
     *  <br><br>
     *
     * @param onPress The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMousePress( Action<SimpleDelegate<C, MouseEvent>> onPress ) {
        NullUtil.nullArgCheck(onPress, "onPress", Action.class);
        C component = getComponent();
        component.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                _doApp(() -> onPress.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse enter events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the wrapped
     *  button component.
     *  <br><br>
     *
     * @param onEnter The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseEnter( Action<SimpleDelegate<C, MouseEvent>> onEnter ) {
        NullUtil.nullArgCheck(onEnter, "onEnter", Action.class);
        C component = getComponent();
        component.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                _doApp(() -> onEnter.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse exit events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the wrapped
     *  button component.
     *  <br><br>
     *
     * @param onExit The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseExit( Action<SimpleDelegate<C, MouseEvent>> onExit ) {
        NullUtil.nullArgCheck(onExit, "onExit", Action.class);
        C component = getComponent();
        component.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                _doApp(() -> onExit.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse drag events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the wrapped
     *  button component.
     *  <br><br>
     *
     * @param onDrag The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseDrag( Action<SimpleDelegate<C, MouseEvent>> onDrag ) {
        NullUtil.nullArgCheck(onDrag, "onDrag", Action.class);
        C component = getComponent();
        component.addMouseListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                _doApp(() -> onDrag.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        component.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                _doApp(() -> onDrag.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse move events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the wrapped
     *  button component.
     *  <br><br>
     *
     * @param onMove The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseMove( Action<SimpleDelegate<C, MouseEvent>> onMove ) {
        NullUtil.nullArgCheck(onMove, "onMove", Action.class);
        C component = getComponent();
        component.addMouseListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                _doApp(() -> onMove.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        component.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                _doApp(() -> onMove.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse wheel events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the wrapped
     *  button component.
     *  <br><br>
     *
     * @param onWheel The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseWheelMove( Action<SimpleDelegate<C, MouseWheelEvent>> onWheel ) {
        NullUtil.nullArgCheck(onWheel, "onWheel", Action.class);
        C component = getComponent();
        component.addMouseWheelListener(new MouseWheelListener() {
            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                _doApp(() -> onWheel.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     *  Use this to register and catch mouse wheel up movement events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the wrapped
     *  button component.
     *  <br><br>
     * @param onWheelUp The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseWheelUp( Action<SimpleDelegate<C, MouseWheelEvent>> onWheelUp ) {
        NullUtil.nullArgCheck(onWheelUp, "onWheelUp", Action.class);
        C component = getComponent();
        component.addMouseWheelListener(new MouseWheelListener() {
            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                if( e.getWheelRotation() < 0 ) {
                    _doApp(() -> onWheelUp.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
                }
            }
        });
        return _this();
    }

    /**
     *  Use this to register and catch mouse wheel down movement events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the wrapped
     *  button component.
     *  <br><br>
     * @param onWheelDown The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseWheelDown( Action<SimpleDelegate<C, MouseWheelEvent>> onWheelDown ) {
        NullUtil.nullArgCheck(onWheelDown, "onWheelDown", Action.class);
        C component = getComponent();
        component.addMouseWheelListener( e -> {
                if ( e.getWheelRotation() > 0 )
                    _doApp(() -> onWheelDown.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            });
        return _this();
    }

    /**
     *  The provided lambda will be invoked when the component's size changes.
     *  This will internally translate to a {@link ComponentListener} implementation.
     *
     * @param onResize The resize action which will be called when the underlying component changes size.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onResize( Action<SimpleDelegate<C, ComponentEvent>> onResize ) {
        NullUtil.nullArgCheck(onResize, "onResize", Action.class);
        C component = getComponent();
        component.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                _doApp(()->onResize.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     *  The provided lambda will be invoked when the component was moved.
     *  This will internally translate to a {@link ComponentListener} implementation.
     *
     * @param onMoved The action lambda which will be executed once the component was moved / its position canged.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMoved( Action<SimpleDelegate<C, ComponentEvent>> onMoved ) {
        NullUtil.nullArgCheck(onMoved, "onMoved", Action.class);
        C component = getComponent();
        component.addComponentListener(new ComponentAdapter() {
            @Override public void componentMoved(ComponentEvent e) {
                _doApp(()->onMoved.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     *  Adds the supplied {@link Action} wrapped in a {@link ComponentListener}
     *  to the component, to receive those component events where the wrapped component becomes visible.
     *
     * @param onShown The {@link Action} which gets invoked when the component has been made visible.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onShown( Action<SimpleDelegate<C, ComponentEvent>> onShown ) {
        NullUtil.nullArgCheck(onShown, "onShown", Action.class);
        C component = getComponent();
        component.addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) {
                _doApp(()->onShown.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     *  Adds the supplied {@link Action} wrapped in a {@link ComponentListener}
     *  to the component, to receive those component events where the wrapped component becomes invisible.
     *
     * @param onHidden The {@link Action} which gets invoked when the component has been made invisible.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onHidden( Action<SimpleDelegate<C, ComponentEvent>> onHidden ) {
        NullUtil.nullArgCheck(onHidden, "onHidden", Action.class);
        C component = getComponent();
        component.addComponentListener(new ComponentAdapter() {
            @Override public void componentHidden(ComponentEvent e) {
                _doApp(()->onHidden.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     * Adds the supplied {@link Action} wrapped in a {@link FocusListener}
     * to the component, to receive those focus events where the wrapped component gains input focus.
     *
     * @param onFocus The {@link Action} which should be executed once the input focus was gained on the wrapped component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onFocusGained( Action<SimpleDelegate<C, ComponentEvent>> onFocus ) {
        NullUtil.nullArgCheck(onFocus, "onFocus", Action.class);
        C component = getComponent();
        component.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                _doApp(()->onFocus.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     * Adds the supplied {@link Action} wrapped in a focus listener
     * to receive those focus events where the wrapped component loses input focus.
     *
     * @param onFocus The {@link Action} which should be executed once the input focus was lost on the wrapped component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onFocusLost( Action<SimpleDelegate<C, ComponentEvent>> onFocus ) {
        NullUtil.nullArgCheck(onFocus, "onFocus", Action.class);
        C component = getComponent();
        component.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                _doApp(()->onFocus.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     * Adds the supplied {@link Action} wrapped in a {@link KeyListener}
     * to the component, to receive key events triggered when the wrapped component receives keyboard input.
     * <br><br>
     * @param onKeyPressed The {@link Action} which will be executed once the wrapped component received a key press.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onKeyPressed( Action<SimpleDelegate<C, KeyEvent>> onKeyPressed ) {
        NullUtil.nullArgCheck(onKeyPressed, "onKeyPressed", Action.class);
        C component = getComponent();
        component.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                _doApp(()->onKeyPressed.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     * Adds the supplied {@link Action} wrapped in a {@link KeyListener} to the component,
     * to receive key events triggered when the wrapped component receives a particular
     * keyboard input matching the provided {@link swingtree.input.Keyboard.Key}.
     * <br><br>
     * @param key The {@link swingtree.input.Keyboard.Key} which should be matched to the key event.
     * @param onKeyPressed The {@link Action} which will be executed once the wrapped component received the targeted key press.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onPressed( Keyboard.Key key, Action<SimpleDelegate<C, KeyEvent>> onKeyPressed ) {
        NullUtil.nullArgCheck(key, "key", Keyboard.Key.class);
        NullUtil.nullArgCheck(onKeyPressed, "onKeyPressed", Action.class);
        C component = getComponent();
        component.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed( KeyEvent e ) {
                if ( e.getKeyCode() == key.code )
                    _doApp(()->onKeyPressed.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

                             /**
     * Adds the supplied {@link Action} wrapped in a {@link KeyListener}
     * to the component, to receive key events triggered when the wrapped component receives keyboard input.
     * <br><br>
     * @param onKeyReleased The {@link Action} which will be executed once the wrapped component received a key release.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyPressed(Action)
     */
    public final I onKeyReleased( Action<SimpleDelegate<C, KeyEvent>> onKeyReleased ) {
        NullUtil.nullArgCheck(onKeyReleased, "onKeyReleased", Action.class);
        C component = getComponent();
        component.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                _doApp(()->onKeyReleased.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood()))); }
        });
        return _this();
    }

    /**
     * Adds the supplied {@link Action} wrapped in a {@link KeyListener} to the component,
     * to receive key events triggered when the wrapped component receives a particular
     * keyboard input matching the provided {@link swingtree.input.Keyboard.Key}.
     * <br><br>
     * @param key The {@link swingtree.input.Keyboard.Key} which should be matched to the key event.
     * @param onKeyReleased The {@link Action} which will be executed once the wrapped component received the targeted key release.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyPressed(Action)
     * @see #onKeyReleased(Action)
     */
    public final I onReleased( Keyboard.Key key, Action<SimpleDelegate<C, KeyEvent>> onKeyReleased ) {
        NullUtil.nullArgCheck(key, "key", Keyboard.Key.class);
        NullUtil.nullArgCheck(onKeyReleased, "onKeyReleased", Action.class);
        C component = getComponent();
        component.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased( KeyEvent e ) {
                if ( e.getKeyCode() == key.code )
                    _doApp(()->onKeyReleased.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
            }
        });
        return _this();
    }

    /**
     * Adds the supplied {@link Action} wrapped in a {@link KeyListener}
     * to the component, to receive key events triggered when the wrapped component receives keyboard input.
     * <br><br>
     * @param onKeyTyped The {@link Action} which will be executed once the wrapped component received a key typed.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyPressed(Action)
     * @see #onKeyReleased(Action)
     */
    public final I onKeyTyped( Action<SimpleDelegate<C, KeyEvent>> onKeyTyped ) {
        NullUtil.nullArgCheck(onKeyTyped, "onKeyTyped", Action.class);
        C component = getComponent();
        _onKeyTyped( e ->
            _doApp(()->onKeyTyped.accept(new SimpleDelegate<>(component, e, this::getSiblinghood)))
        );
        return _this();
    }

    /**
     * Adds the supplied {@link Action} wrapped in a {@link KeyListener} to the component,
     * to receive key events triggered when the wrapped component receives a particular
     * keyboard input matching the provided {@link swingtree.input.Keyboard.Key}.
     * <br><br>
     * @param key The {@link swingtree.input.Keyboard.Key} which should be matched to the key event.
     * @param onKeyTyped The {@link Action} which will be executed once the wrapped component received the targeted key typed.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyPressed(Action)
     * @see #onKeyReleased(Action)
     * @see #onKeyTyped(Action)
     */
    public final I onTyped( Keyboard.Key key, Action<SimpleDelegate<C, KeyEvent>> onKeyTyped ) {
        NullUtil.nullArgCheck(key, "key", Keyboard.Key.class);
        NullUtil.nullArgCheck(onKeyTyped, "onKeyTyped", Action.class);
        C component = getComponent();
        _onKeyTyped( e -> {
            if ( e.getKeyCode() == key.code )
                _doApp(()->onKeyTyped.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));
        });
        return _this();
    }

    protected void _onKeyTyped( Consumer<KeyEvent> action ) {
        getComponent().addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) {
                action.accept(e);
            }
        });
    }

    /**
     *  Use this to register periodic update actions which should be called
     *  based on the provided {@code delay}! <br>
     *  The following example produces a label which will display the current date.
     *  <pre>{@code
     *      UI.label("")
     *          .doUpdates( 100, it -> it.getComponent().setText(new Date().toString()) )
     *  }</pre>
     *
     * @param delay The delay in milliseconds between calling the provided {@link Action}.
     * @param onUpdate The {@link Action} which should be called periodically.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I doUpdates( int delay, Action<SimpleDelegate<C, ActionEvent>> onUpdate ) {
        NullUtil.nullArgCheck(onUpdate, "onUpdate", Action.class);
        Timer timer = new Timer(delay, e -> onUpdate.accept(new SimpleDelegate<>(getComponent(), e, this::getSiblinghood)));
        {
            java.util.List<Timer> timers = (java.util.List<Timer>) getComponent().getClientProperty(_TIMERS_KEY);
            if ( timers == null ) {
                timers = new ArrayList<>();
                getComponent().putClientProperty(_TIMERS_KEY, timers);
            }
            timers.add(timer);
        }
        timer.start();
        return _this();
    }

    @Override
    protected void _add( JComponent component, Object conf ) {
        NullUtil.nullArgCheck(component, "component", JComponent.class);
        if ( conf == null )
            getComponent().add(component);
        else
            getComponent().add(component, conf);
    }

    /**
     * @param builder A builder for another {@link JComponent} instance which ought to be added to the wrapped component type.
     * @param <T> The type of the {@link JComponent} which is wrapped by the provided builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <T extends JComponent> I add( UIForAnySwing<?, T> builder ) {
        this.add(new AbstractNestedBuilder[]{builder});
        return _this();
    }

    /**
     *  Use this to nest builder nodes into this builder to effectively plug the wrapped {@link JComponent}s
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument is expected to contain layout information for the layout manager of the wrapped {@link JComponent},
     *  through the {@link JComponent#add(Component, Object)} method.
     *  By default, the {@link MigLayout} is used.
     *  <br><br>
     *
     * @param attr The additional mig-layout information which should be passed to the UI tree.
     * @param builder A builder for another {@link JComponent} instance which ought to be added to the wrapped component type.
     * @param <T> The type of the {@link JComponent} which is wrapped by the provided builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <T extends JComponent> I add( String attr, UIForAnySwing<?, T> builder ) {
        return this.add(attr, new UIForAnySwing[]{builder});
    }

    /**
     *  Use this to nest builder nodes into this builder to effectively plug the wrapped {@link JComponent}s
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument will be passed to the layout manager of the wrapped {@link JComponent},
     *  through the {@link JComponent#add(Component, Object)} method.
     *  By default, the {@link MigLayout} is used.
     *  <br><br>
     *
     * @param attr The mig-layout attribute.
     * @param builder A builder for another {@link JComponent} instance which ought to be added to the wrapped component type.
     * @param <T> The type of the {@link JComponent} which is wrapped by the provided builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <T extends JComponent> I add( CompAttr attr, UIForAnySwing<?, T> builder ) {
        return this.add(attr.toString(), new UIForAnySwing[]{builder});
    }

    /**
     *  Use this to nest builder types into this builder to effectively plug the wrapped {@link JComponent}s 
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument represents layout attributes/constraints which will
     *  be passed to the {@link LayoutManager} of the underlying {@link JComponent}.
     *  through the {@link JComponent#add(Component, Object)} method.
     *  <br><br>
     *
     * @param attr The additional mig-layout information which should be passed to the UI tree.
     * @param builders An array of builders for a corresponding number of {@link JComponent} 
     *                  type which ought to be added to the wrapped component type of this builder.
     * @param <B> The builder type parameter, a subtype of {@link UIForAnySwing}.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    public final <B extends UIForAnySwing<?, ?>> I add(String attr, B... builders ) {
        LayoutManager layout = getComponent().getLayout();
        if ( _isBorderLayout(attr) && !(layout instanceof BorderLayout) ) {
            if ( layout instanceof MigLayout )
                log.warn("Layout ambiguity detected! Border layout constraint cannot be added to 'MigLayout'.");
            getComponent().setLayout(new BorderLayout()); // The UI Maker tries to fill in the blanks!
        }
        for ( UIForAnySwing<?, ?> b : builders ) _doAdd(b, attr);
        return _this();
    }

    /**
     *  Use this to nest builder types into this builder to effectively plug the wrapped {@link JComponent}s
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument will be passed to the {@link LayoutManager}
     *  of the underlying {@link JComponent} to serve as layout constraints.
     *  through the {@link JComponent#add(Component, Object)} method.
     *  <br><br>
     *
     * @param attr The first mig-layout information which should be passed to the UI tree.
     * @param builders An array of builders for a corresponding number of {@link JComponent}
     *                  type which ought to be added to the wrapped component type of this builder.
     * @param <B> The builder type parameter, a subtype of {@link UIForAnySwing}.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    public final <B extends UIForAnySwing<?, ?>> I add(CompAttr attr, B... builders ) {
        return this.add(attr.toString(), builders);
    }

    /**
     *  Use this to nest {@link JComponent} types into this builder to effectively plug the provided {@link JComponent}s
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument represents layout attributes/constraints which will
     *  be applied to the subsequently provided {@link JComponent} types.
     *  <br><br>
     *
     * @param attr The additional layout information which should be passed to the UI tree.
     * @param components A {@link JComponent}s array which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    public final <E extends JComponent> I add( String attr, E... components ) {
        NullUtil.nullArgCheck(attr, "conf", Object.class);
        NullUtil.nullArgCheck(components, "components", Object[].class);
        for( E component : components ) {
            NullUtil.nullArgCheck(component, "component", JComponent.class);
            this.add(attr, UI.of(component));
        }
        return _this();
    }

    /**
     *  Use this to nest {@link JComponent} types into this builder to effectively plug the provided {@link JComponent}s
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first 2 arguments will be joined by a comma and passed to the {@link LayoutManager}
     *  of the underlying {@link JComponent} to serve as layout constraints.
     *  <br><br>
     *
     * @param attr The first layout information which should be passed to the UI tree.
     * @param components A {@link JComponent}s array which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    public final <E extends JComponent> I add( CompAttr attr, E... components ) {
        return this.add(attr.toString(), components);
    }

    /**
     *  This allows you to bind to a {@link swingtree.api.mvvm.Viewable}
     *  implementation and automatically update the view when the view model changes.
     *
     * @param viewable A {@link sprouts.Val} property which holds a {@link Viewable} instance
     *        that will be used to generate the view.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I add( Val<? extends Viewable> viewable ) {
        NullUtil.nullArgCheck(viewable, "viewable", Val.class);
        _addViewableProp(viewable, null);
        return _this();
    }

    /**
     *  This allows you to bind to a property list of {@link Viewable}s
     *  to automatically update the view when your view models change.
     *
     * @param viewables A {@link sprouts.Vals} list of {@link Viewable}s
     *                  wrapped in a {@link sprouts.Val} properties.
     *                  The Viewables will be used to generate the view.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I add( Vals<? extends Viewable> viewables ) {
        NullUtil.nullArgCheck(viewables, "viewables", Vals.class);
        _addViewableProps( viewables, null );
        return _this();
    }

    /**
     *  This allows you to bind to a {@link Viewable}
     *  implementation and automatically update the view when the view model changes.
     *
     * @param attr The layout information which should be passed to the UI tree.
     * @param viewable A {@link sprouts.Val} property which holds a {@link Viewable} instance
     *        that will be used to generate the view.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I add( String attr, Val<? extends Viewable> viewable ) {
        NullUtil.nullArgCheck(attr, "attr", Object.class);
        NullUtil.nullArgCheck(viewable, "viewable", Val.class);
        _addViewableProp(viewable, attr);
        return _this();
    }

    /**
     *  This allows you to bind to a property list of {@link Viewable}s
     *  to automatically update the view when your view models change.
     *
     * @param attr The layout information which should be passed to the UI tree.
     * @param viewables A {@link sprouts.Vals} list of {@link Viewable}s
     *                  wrapped in a {@link sprouts.Val} properties.
     *                  The Viewables will be used to generate the view.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I add( String attr, Vals<? extends Viewable> viewables ) {
        NullUtil.nullArgCheck(attr, "attr", Object.class);
        NullUtil.nullArgCheck(viewables, "viewables", Vals.class);
        _addViewableProps( viewables, attr );
        return _this();
    }

    /**
     *  This allows you to bind to a {@link Viewable}
     *  implementation and automatically update the view when the view model changes.
     *
     * @param attr The layout information which should be passed to the UI tree.
     * @param viewable A {@link sprouts.Val} property which holds a {@link Viewable} instance
     *        that will be used to generate the view.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I add( CompAttr attr, Val<? extends Viewable> viewable ) {
        return this.add(attr.toString(), viewable);
    }

    /**
     *  This allows you to bind to a property list of {@link Viewable}s
     *  to automatically update the view when your view models change.
     *
     * @param attr The layout information which should be passed to the UI tree.
     * @param viewables A {@link sprouts.Vals} list of {@link Viewable}s
     *                  wrapped in a {@link sprouts.Val} properties.
     *                  The Viewables will be used to generate the view.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I add( CompAttr attr, Vals<? extends Viewable> viewables ) {
        return this.add(attr.toString(), viewables);
    }

    protected void _addViewableProps( Vals<? extends Viewable> viewables, String attr ) {
        _onShow( viewables, delegate -> {
            // we simply redo all the components.
            switch ( delegate.changeType() ) {
                case SET: _updateComponentAt(delegate.index(), delegate.newValue().get(), attr); break;
                case ADD:
                    if ( delegate.index() < 0 && delegate.newValue().isEmpty() ) {
                        // This is basically a add all operation, so we clear the components first.
                        _clearComponents();
                        // and then we add all the components.
                        for ( int i = 0; i < delegate.vals().size(); i++ )
                            _addComponentAt( i, delegate.vals().at(i).get(), attr );
                    }
                    else
                        _addComponentAt(delegate.index(), delegate.newValue().get(), attr);
                    break;
                case REMOVE: _removeComponentAt(delegate.index()); break;
                case CLEAR: _clearComponents(); break;
                case NONE: break;
                default: throw new IllegalStateException("Unknown type: "+delegate.changeType());
            }
        });
        viewables.forEach( v -> add(v.createView(JComponent.class)) );
    }

    private void _addViewableProp( Val<? extends Viewable> viewable, String attr ) {
        // First we remember the index of the component which will be provided by the viewable dynamically.
        final int index = _childCount();
        // Then we add the component provided by the viewable to the list of children.
        if ( attr == null ) {
            if ( viewable.isPresent() )
                this.add(viewable.get().createView(JComponent.class));
            else
                this.add(new JPanel()); // We add a dummy component to the list of children.
        } else {
            if ( viewable.isPresent() )
                this.add(attr, viewable.get().createView(JComponent.class));
            else
                this.add(attr, new JPanel()); // We add a dummy component to the list of children.
        }
        // Finally we add a listener to the viewable which will update the component when the viewable changes.
        _onShow( viewable, v -> _updateComponentAt(index, v, attr) );
    }

    private void _updateComponentAt( int index, Viewable v, String attr ) {
        component().ifPresent( c -> {
            JComponent newComponent = v == null ? new JPanel() : UI.use(_eventProcessor, () -> v.createView(JComponent.class) );
            // We remove the old component.
            c.remove(c.getComponent(index));
            // We add the new component.
            if ( attr == null )
                c.add(newComponent, index);
            else
                c.add(newComponent, attr, index);
            // We update the layout.
            c.revalidate();
            c.repaint();
        });
    }

    private void _addComponentAt( int index, Viewable v, String attr ) {
        component().ifPresent( c -> {
            // We add the new component.
            if ( attr == null )
                c.add(UI.use(_eventProcessor, () -> v.createView(JComponent.class)), index);
            else
                c.add(UI.use(_eventProcessor, () -> v.createView(JComponent.class)), attr, index);
            // We update the layout.
            c.revalidate();
            c.repaint();
        });
    }

    private void _removeComponentAt( int index ) {
        component().ifPresent( c -> {
            // We remove the old component.
            c.remove(c.getComponent(index));
            // We update the layout.
            c.revalidate();
            c.repaint();
        });
    }

    private void _clearComponents() {
        component().ifPresent( c -> {
            // We remove all components.
            c.removeAll();
            // We update the layout.
            c.revalidate();
            c.repaint();
        });
    }

    private static boolean _isBorderLayout( Object o ) {
        return BorderLayout.CENTER.equals(o) ||
                BorderLayout.PAGE_START.equals(o) ||
                BorderLayout.PAGE_END.equals(o) ||
                BorderLayout.LINE_END.equals(o) ||
                BorderLayout.LINE_START.equals(o) ||
                BorderLayout.EAST.equals(o)  ||
                BorderLayout.WEST.equals(o)  ||
                BorderLayout.NORTH.equals(o) ||
                BorderLayout.SOUTH.equals(o);
    }
}
