package swingtree;


import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.ConstraintParser;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Action;
import sprouts.Event;
import sprouts.Observable;
import sprouts.Observer;
import sprouts.*;
import sprouts.impl.TupleDiff;
import sprouts.impl.TupleDiffOwner;
import swingtree.animation.AnimationDispatcher;
import swingtree.animation.AnimationStatus;
import swingtree.animation.LifeTime;
import swingtree.api.*;
import swingtree.api.mvvm.ViewSupplier;
import swingtree.components.JBox;
import swingtree.input.Keyboard;
import swingtree.layout.AddConstraint;
import swingtree.layout.LayoutConstraint;
import swingtree.layout.ResponsiveGridFlowLayout;
import swingtree.layout.Size;
import swingtree.style.ComponentExtension;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 *  A generic SwingTree builder node designed as a basis for configuring any kind of {@link JComponent} instance.
 *  This is the most generic builder type and therefore abstract super-type for almost all other builders.
 *  This builder defines nested building for anything extending the {@link JComponent} class.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *  <br><br>
 *
 * @param <I> The concrete extension of the {@link UIForAnything}.
 * @param <C> The type parameter for the component type wrapped by an instance of this class.
 */
public abstract class UIForAnySwing<I, C extends JComponent> extends UIForAnything<I, C, JComponent>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UIForAnySwing.class);

    private final static String _TIMERS_KEY = "_swing-tree.timers";


    @SuppressWarnings("ReferenceEquality")
    protected final boolean _isUndefinedFont( Font font ) {
        return font == UI.Font.UNDEFINED;
    }

    @SuppressWarnings("ReferenceEquality")
    protected final boolean _isUndefinedColor( Color color ) {
        return color == UI.Color.UNDEFINED;
    }

    private void _bindRepaintOn( JComponent thisComponent, Observable event ) {
        event.subscribe( () -> _runInUI( thisComponent::repaint ) );
    }

    private void _bindRepaintOn( JComponent thisComponent, Event event ) {
        Observable.cast(event).subscribe(
            Observer.ofWeak(thisComponent, innerComponent -> _runInUI(innerComponent::repaint) )
        );
    }

    private void _bindRepaintOn( JComponent thisComponent, Val<?> event ) {
        Viewable.cast(event).subscribe(
            Observer.ofWeak(thisComponent, innerComponent -> _runInUI(innerComponent::repaint) )
        );
    }

    /**
     *  Use this to bind an {@link Observable} (usually from a sprouts.Event)
     *  to the {@link JComponent#repaint()} method of the component represented by this builder.
     *  This means that the component will be repainted whenever
     *  the source of the observable is fired or changed.
     *
     * @param observable The observable to which the repaint method of the component will be bound.
     * @return This declarative builder instance, which enables builder-style method chaining.
     */
    public final I withRepaintOn( Observable observable ) {
        return _with( thisComponent -> _bindRepaintOn(thisComponent, observable) )._this();
    }

    /**
     *  This method exposes a concise way to bind multiple {@link Observable}s (usually sprouts.Event instances)
     *  to the {@link JComponent#repaint()} method of the component represented by this builder.
     *  This means that the component will be repainted whenever the source of any one of the
     *  observables is fired or changed.
     *
     * @param first The first observable to which the repaint method of the component will be bound.
     * @param second The second observable to which the repaint method of the component will be bound.
     * @param rest The rest of the observables to which the repaint method of the component will be bound.
     * @return This declarative builder instance, which enables builder-style method chaining.
     */
    public final I withRepaintOn( Observable first, Observable second, Observable... rest ) {
        return _with( c -> {
                    _bindRepaintOn(c, first);
                    _bindRepaintOn(c, second);
                    for ( Observable o : rest ) {
                        _bindRepaintOn(c, o);
                    }
                })._this();
    }

    /**
     *  Allows you to bind an {@link Event} to the {@link JComponent#repaint()} method of
     *  the component represented by this builder. <br>
     *  This means that the component will be repainted whenever the event is fired
     *  through the {@link Event#fire()} method.
     *
     * @param event The event to which the repaint method of the component will be bound.
     * @return This declarative builder instance, which enables builder-style method chaining.
     */
    public final I withRepaintOn( Event event ) {
        return _with( thisComponent -> _bindRepaintOn(thisComponent, event) )._this();
    }

    /**
     *  This method exposes a concise way to bind multiple {@link Event}s to the
     *  {@link JComponent#repaint()} method of the component represented by this builder.
     *  This means that the component will be repainted whenever any one of the events is fired
     *  through the {@link Event#fire()} method.
     *
     * @param first The first event to which the repaint method of the component will be bound.
     * @param second The second event to which the repaint method of the component will be bound.
     * @param rest The rest of the events to which the repaint method of the component will be bound.
     * @return This declarative builder instance, which enables builder-style method chaining.
     */
    public final I withRepaintOn( Event first, Event second, Event... rest ) {
        return _with( thisComponent -> {
                    _bindRepaintOn(thisComponent, first);
                    _bindRepaintOn(thisComponent, second);
                    for ( Event e : rest ) {
                        _bindRepaintOn(thisComponent, e);
                    }
                })
                ._this();
    }

    /**
     *  Allows you to bind a {@link Val} to the {@link JComponent#repaint()} method
     *  of the component represented by this builder. <br>
     *  This means that the component will be repainted whenever the value of the {@link Val}
     *  changes. If the {@link Val} is a mutable {@link Var} property,
     *  then this event is usually triggered through the {@link Var#set(Object)} method.<br>
     *  <p>
     *      A typical use case is to use {@link Var} properties in the
     *      {@link Styler} of the style API exposed by {@link UIForAnySwing#withStyle(Styler)},
     *      and then also pass these properties to the this {@code withRepaintOn}
     *      method to ensure that the style gets re-evaluated and then repainted.
     *  </p>
     *
     * @param event The {@link Val} to which the repaint method of the component will be bound.
     * @return This declarative builder instance, which enables builder-style method chaining.
     */
    public final I withRepaintOn( Val<?> event ) {
        return _with( thisComponent -> _bindRepaintOn(thisComponent, event) )._this();
    }

    /**
     *  Use this method to bind multiple {@link Val}s to the
     *  {@link JComponent#repaint()} method of the component represented by this builder.
     *  This means that the component will be repainted whenever the value of any one of the
     *  {@link Val}s changes. If the {@link Val} is a mutable {@link Var} property,
     *  then this event is usually triggered through the {@link Var#set(Object)} method.<br>
     *  <p>
     *      A typical use case is to use {@link Var} properties in the
     *      {@link Styler} of the style API exposed by {@link UIForAnySwing#withStyle(Styler)},
     *      and then also pass these properties to the this {@code withRepaintOn}
     *      method to ensure that the style gets re-evaluated and then repainted.
     *  </p>
     *
     * @param first The first {@link Val} to which the repaint method of the component will be bound.
     * @param second The second {@link Val} to which the repaint method of the component will be bound.
     * @param rest The rest of the {@link Val}s to which the repaint method of the component will be bound.
     * @return This declarative builder instance, which enables builder-style method chaining.
     */
    public final I withRepaintOn( Val<?> first, Val<?> second, Val<?>... rest ) {
        return _with( c -> {
                    _bindRepaintOn(c, first);
                    _bindRepaintOn(c, second);
                    for ( Val<?> o : rest ) {
                        _bindRepaintOn(c, o);
                    }
                })._this();
    }

    /**
     *  This method exposes a concise way to set an identifier for the component
     *  represented by this builder chain.
     *  In essence this is simply a delegate for the {@link JComponent#setName(String)} method
     *  to make it more expressive and widely recognized what is meant
     *  ("id" is shorter and makes more sense than "name" which could be confused with "title").
     *
     * @param id The identifier for this {@link JComponent} which will
     *           simply translate to {@link JComponent#setName(String)}
     *
     * @return The JComponent type which will be managed by this builder.
     */
    public final I id( String id ) {
        return _with( c -> ComponentExtension.from(c).setId(id) )._this();
    }

    /**
     *  This method exposes a concise way to set an enum based identifier for the component
     *  represented by this builder chain.
     *  In essence this is simply a delegate for the {@link JComponent#setName(String)} method
     *  to make it more expressive and widely recognized what is meant
     *  ("id" is shorter and makes more sense than "name" which could be confused with "title").
     *  <p>
     *  The enum identifier will be translated to a string using {@link Enum#name()}.
     *
     * @param id The enum identifier for this {@link JComponent} which will
     *           simply translate to {@link JComponent#setName(String)}
     *
     * @return The JComponent type which will be managed by this builder.
     * @param <E> The enum type.
     */
    public final <E extends Enum<E>> I id( E id ) {
        Objects.requireNonNull(id);
        return _with( c -> ComponentExtension.from(c).setId(id) )._this();
    }

    /**
     *  This method is part of the SwingTree style API, and it allows you to
     *  add this component to a style group.
     *  This is conceptually similar to CSS classes, with the difference that
     *  style groups can inherit from each other inside {@link swingtree.style.StyleSheet}s. <br>
     *  Here an example of how to define styles for a style group:
     *  <pre><code>
     *  new StyleSheet() {
     *      {@literal @}Override
     *      protected void build() {
     *          add(group("A").inherits("B", "C"), it -&gt; it
     *              .backgroundColor(Color.RED)
     *          );
     *          add(group("B"), it -&gt; it
     *              .borderWidth(12)
     *          );
     *          add(group("C"), it -&gt; it
     *              .borderWidth(16)
     *              .borderColor(Color.YELLOW)
     *          );
     *      }
     *    }
     *  </code></pre>
     *  <br>
     *  The style sheet in the above example code can be applied to a component like so:
     *  <pre>{@code
     *      UI.use(new MyStyleSheet(), ()->
     *          UI.button("Click me").group("A")
     *          .onClick(it -> {...})
     *      );
     *  }</pre><br>
     *  <b>It is advised to use the {@link #group(Enum[])} method
     *  instead of this method, as the usage of enums for modelling
     *  group tags offers much better compile time type safety!</b>
     *
     * @param groupTags The names of the style groups to which this component should be added.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I group( String... groupTags ) {
        return _with( c -> ComponentExtension.from(c).setStyleGroups(groupTags) )._this();
    }

    /**
     *  This method is part of the SwingTree style API, and it allows you to
     *  add this component to an enum based style group.
     *  This is conceptually similar to CSS classes, with the difference that
     *  style groups can inherit from each other inside {@link swingtree.style.StyleSheet}s. <br>
     *  Here an example of how to define styles for a style group:
     *  <pre><code>
     *  new StyleSheet() {
     *          {@literal @}Override
     *          protected void build() {
     *              add(group(MyGroups.A).inherits("B", "C"), it -&gt; it
     *                  .backgroundColor(Color.RED)
     *              );
     *              add(group(MyGroups.B), it -&gt; it
     *                  .borderWidth(12)
     *              );
     *              add(group(MyGroups.C), it -&gt; it
     *                  .borderWidth(16)
     *                  .borderColor(Color.YELLOW)
     *              );
     *          }
     *      }
     *  </code></pre>
     *  <br>
     *  The style sheet in the above example code can be applied to a component like so:
     *  <pre>{@code
     *      UI.use(new MyStyleSheet(), ()->
     *          UI.button("Click me").group(MyGroup.A)
     *          .onClick(it -> {...})
     *      );
     *  }</pre>
     *
     * @param groupTags The enum based style group to which this component should be added.
     * @return This very instance, which enables builder-style method chaining.
     * @param <E> The enum type.
     */
    @SafeVarargs
    public final <E extends Enum<E>> I group( E... groupTags ) {
        return _with( c -> ComponentExtension.from(c).setStyleGroups(groupTags) )._this();
    }

    /**
     *  Make the underlying {@link JComponent} type visible or invisible
     *  depending on the supplied boolean value.
     *
     * @param isVisible The truth value determining if the component should be visible or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isVisibleIf( boolean isVisible ) {
        return _with( c -> c.setVisible(isVisible) )._this();
    }

    /**
     *  This is the inverse of {@link #isVisibleIf(boolean)}, and it is
     *  used to make the underlying {@link JComponent} type visible or invisible.
     *  <p>
     *  If the supplied boolean value is {@code true}, the component will be invisible. <br>
     *  If the supplied boolean value is {@code false}, the component will be visible.
     *
     * @param isVisible The truth value determining if the UI component should be visible or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isVisibleIfNot( boolean isVisible ) {
        return _with( c -> c.setVisible(!isVisible) )._this();
    }

    /**
     *  Make the underlying {@link JComponent} type dynamically visible or invisible
     *  through the supplied {@link Val} property, which is automatically bound
     *  to the {@link JComponent#setVisible(boolean)} method of the underlying {@link JComponent} type.
     *  <p>
     *  This means that when the supplied {@link Val} property changes its value,
     *  then visibility of the underlying {@link JComponent} type will be updated accordingly.
     *  <p>
     * <i>
     *     Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to
     *           send the property value to this view component.
     * </i>
     *
     * @param isVisible The truth value determining if the UI component should be visible or not wrapped in a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isVisibleIf( Val<Boolean> isVisible ) {
        NullUtil.nullArgCheck(isVisible, "isVisible", Val.class);
        NullUtil.nullPropertyCheck(isVisible, "isVisible", "Null is not allowed to model the visibility of a UI component!");
        return _withOnShow( isVisible, (c, v) -> {
                    c.setVisible(v);
                })
                ._with( c -> {
                    c.setVisible( isVisible.orElseThrowUnchecked() );
                })
                ._this();
    }

    /**
     *  This is the inverse of {@link #isVisibleIf(Val)}, and it is
     *  used to make the underlying {@link JComponent} type dynamically visible or invisible.
     *  <p>
     *  This means that when the supplied {@link Val} property changes its value,
     *  then visibility of the underlying {@link JComponent} type will be updated accordingly.
     *  <p>
     *  If the supplied {@link Val} property is {@code true}, the component will be invisible. <br>
     *  If the supplied {@link Val} property is {@code false}, the component will be visible.
     *  <p>
     *  <i>
     *      Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to
     *            send the property value to this view component.
     *  </i>
     * @param isVisible The truth value determining if the UI component should be visible or not wrapped in a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isVisibleIfNot( Val<Boolean> isVisible ) {
        NullUtil.nullArgCheck(isVisible, "isVisible", Val.class);
        NullUtil.nullPropertyCheck(isVisible, "isVisible", "Null is not allowed to model the visibility of a UI component! A boolean should only be true or false!");
        return _withOnShow( isVisible, (c, v) -> {
                    c.setVisible(!v);
                })
                ._with( c -> {
                    c.setVisible( !isVisible.orElseThrowUnchecked() );
                })
                ._this();
    }

    /**
     *  Make the underlying {@link JComponent} type dynamically visible or invisible
     *  based on the equality between the supplied enum value and enum property. <br>
     *  <p>
     *  This means that when the supplied {@link Val} property changes its value,
     *  and the new value is equal to the supplied enum value,
     *  then the underlying {@link JComponent} type will be visible,
     *  otherwise it will be invisible.
     * <i>
     *     Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your
     *           view model to send the property value to this view component.
     * </i>
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
        return _withOnShow( enumProperty, (c,v) -> {
                    c.setVisible( v == enumValue );
                })
                ._with( c -> {
                    c.setVisible( enumValue == enumProperty.orElseThrowUnchecked() );
                })
                ._this();
    }

    /**
     *  This is the inverse of {@link #isVisibleIf(Enum, Val)}, and it is
     *  used to make the underlying {@link JComponent} type dynamically visible or invisible.
     *  <p>
     *  This means that when the supplied {@link Val} property changes its value,
     *  and the new value is equal to the supplied enum value,
     *  then the underlying {@link JComponent} type will be invisible,
     *  otherwise it will be visible.
     * <i>
     *     Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your
     *           view model to send the property value to this view component.
     * </i>
     * @param enumValue The enum value which, if equal to the supplied enum property, makes the UI component invisible.
     * @param enumProperty The enum property which, if equal to the supplied enum value, makes the UI component invisible.
     * @param <E> The enum type for both the supplied enum value and enum property.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <E extends Enum<E>> I isVisibleIfNot( E enumValue, Val<E> enumProperty ) {
        NullUtil.nullArgCheck(enumValue, "enumValue", Enum.class);
        NullUtil.nullArgCheck(enumProperty, "enumProperty", Val.class);
        NullUtil.nullPropertyCheck(enumProperty, "enumProperty", "Null is not allowed to model the visibility of a UI component!");
        return _withOnShow( enumProperty, (c,v) -> {
                    c.setVisible( v != enumValue );
                })
                ._with( c -> {
                    c.setVisible( enumValue != enumProperty.orElseThrowUnchecked() );
                })
                ._this();
    }

    /**
     *  Use this to enable or disable the wrapped UI component.
     *
     * @param isEnabled The truth value determining if the UI component should be enabled or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isEnabledIf( boolean isEnabled ) {
        return _with( c -> _setEnabled(c, isEnabled) )._this();
    }

    /**
     *  This is the inverse of {@link #isEnabledIf(boolean)}.
     *  Use this to disable or enable the wrapped UI component.
     *
     * @param isEnabled The truth value determining if the UI component should be enabled or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I isEnabledIfNot( boolean isEnabled ) {
        return _with( c -> _setEnabled(c, !isEnabled) )._this();
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
        return _withOnShow( isEnabled, (c,v) -> {
                    c.setEnabled(v);
                })
                ._with( c -> {
                    _setEnabled(c,  isEnabled.orElseThrowUnchecked() );
                })
                ._this();
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
        return _withOnShow( isEnabled, (c,v) -> {
                    _setEnabled(c, !v);
                })
                ._with( c -> {
                    _setEnabled(c,  !isEnabled.orElseThrowUnchecked() );
                })
                ._this();
    }

    /**
     *  Use this to make the wrapped UI component dynamically enabled or disabled,
     *  based on the equality between the supplied enum value and enum property. <br>
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
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
        return _withOnShow( enumProperty, (c,v) -> {
                    _setEnabled( c,  v == enumValue );
                })
                ._with( c -> {
                    _setEnabled(c,  enumValue == enumProperty.orElseThrowUnchecked() );
                })
                ._this();
    }

    /**
     *  This is the inverse of {@link #isEnabledIf(Enum, Val)}.
     *  Use this to make the wrapped UI component dynamically disabled or enabled,
     *  based on the equality between the supplied enum value and enum property. <br>
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
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
        return _withOnShow( enumProperty, (c,v) -> {
                    _setEnabled( c,  v != enumValue );
                })
                ._with( c -> {
                    _setEnabled(c,  enumValue != enumProperty.orElseThrowUnchecked() );
                })
                ._this();
    }

    protected void _setEnabled( C c, boolean isEnabled ) { 
        c.setEnabled( isEnabled ); 
    }

    /**
     *  Use this to make the wrapped UI component grab the input focus.
     *  @return This very instance, which enables builder-style method chaining.
     */
    public final I makeFocused() {
        return _with( c -> {
                    UI.runLater(() -> {
                        c.grabFocus();
                        // We do this later because in this point in time the UI is probably not
                        // yet fully built (swing-tree is using the builder-pattern).
                    });
                })
                ._this();
    }

    /**
     *  Use this to make the wrapped UI component focusable.
     *  @param isFocusable The truth value determining if the UI component should be focusable or not.
     *  @return This very instance, which enables builder-style method chaining.
     */
    public final I isFocusableIf( boolean isFocusable ) {
        return _with( c -> c.setFocusable(isFocusable) )._this();
    }

    /**
     *  Use this to dynamically make the wrapped UI component focusable.
     *  This is useful if you want to make a component focusable only if a certain condition is met.
     *  <br>
     *  <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     *  @param isFocusable The truth value determining if the UI component should be focusable or not wrapped in a {@link Val}.
     *  @return This very instance, which enables builder-style method chaining.
     */
    public final I isFocusableIf( Val<Boolean> isFocusable ) {
        NullUtil.nullArgCheck(isFocusable, "isFocusable", Val.class);
        NullUtil.nullPropertyCheck(isFocusable, "isFocusable", "Null value for isFocusable is not allowed!");
        return _withOnShow( isFocusable, (c,v) -> {
                    c.setFocusable(v);
                })
                ._with( c -> {
                    c.setFocusable( isFocusable.orElseThrowUnchecked() );
                })
                ._this();
    }

    /**
     *  Use this to make the wrapped UI component focusable if a certain condition is not met.
     *  @param notFocusable The truth value determining if the UI component should be focusable or not.
     *                     If {@code false}, the component will be focusable.
     *  @return This very instance, which enables builder-style method chaining.
     */
    public final I isFocusableIfNot( boolean notFocusable ) {
        return _with( c -> c.setFocusable( !notFocusable ) )._this();
    }

    /**
     *  Use this to dynamically make the wrapped UI component focusable.
     *  This is useful if you want to make a component focusable only if a certain condition is met.
     *  <br>
     *  <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     *  @param isFocusable The truth value determining if the UI component should be focusable or not, wrapped in a {@link Val}.
     *  @return This very instance, which enables builder-style method chaining.
     *  @throws IllegalArgumentException if the supplied {@code isFocusable} is {@code null}.
     */
    public final I isFocusableIfNot( Val<Boolean> isFocusable ) {
        NullUtil.nullArgCheck(isFocusable, "isFocusable", Val.class);
        NullUtil.nullPropertyCheck(isFocusable, "isFocusable", "Null value for isFocusable is not allowed!");
        return _withOnShow( isFocusable, (c,v) -> {
                    c.setFocusable( !v );
                })
                ._with( c -> {
                    c.setFocusable( !isFocusable.orElseThrowUnchecked() );
                })
                ._this();
    }

    /**
     *  Use this to make the wrapped UI component dynamically focusable or non-focusable
     *  based on the equality between the supplied enum value and enum property. <br>
     *  <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
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
        return _withOnShow( enumProperty, (c,v) -> {
                    c.setFocusable( v == enumValue );
                })
                ._with( c -> {
                    c.setFocusable( enumValue == enumProperty.orElseThrowUnchecked() );
                })
                ._this();
    }

    /**
     *  This is the inverse of {@link #isFocusableIf(Enum, Val)}.
     *  Use this to make the wrapped UI component dynamically focusable or non-focusable
     *  based on the equality between the supplied enum value and enum property. <br>
     *  <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
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
        return _withOnShow( enumProperty, (c,v) -> {
                    c.setFocusable( v != enumValue );
                })
                ._with( c -> {
                    c.setFocusable( enumValue != enumProperty.orElseThrowUnchecked() );
                })
                ._this();
    }


    /**
     *  Use this to make the wrapped UI component opaque.
     *  This is the inverse of {@link #makeNonOpaque()}.
     *
     *  @return This very instance, which enables builder-style method chaining.
     *  @deprecated SwingTree considers the opaqueness a property which emerges from the
     *              style configuration of the component. Therefore, it is not recommended
     *              to set the opaqueness directly. Instead, use the {@link #withBackground(Color)}
     *              method to set the style of the component so that it becomes opaque.
     */
    @Deprecated
    public final I makeOpaque() {
        return _with( c -> c.setOpaque( true ) )._this();
    }

    /**
     *  Use this to make the wrapped UI component transparent.
     *  This is the inverse of {@link #makeOpaque()}.
     *
     *  @return This very instance, which enables builder-style method chaining.
     *  @deprecated Use {@link #withBackground(Color)} instead, by passing
     *             it the {@link UI.Color#TRANSPARENT} constant.<br>
     *             Alternatively, you may use the {@link #peek(Peeker)}
     *             method to peek into the builder's component
     *             and set the flag directly.
     */
    @Deprecated
    public final I makeNonOpaque() {
        return _with( c -> c.setOpaque( false ) )._this();
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
        return _with( c -> {
                    c.setInputVerifier(new InputVerifier() {
                        @Override
                        public boolean verify( JComponent input ) {
                            return verifier.isValid(
                                    new ComponentDelegate<>(
                                            c,
                                            new ComponentEvent(c, 0)
                                    )
                            );
                        /*
                            We expect the user to model the state of the UI components
                            using properties in the view model.
                         */
                        }
                    });
                })
                ._this();
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
        return _with( c -> c.putClientProperty(key, value) )._this();
    }

    /**
     *  Use this to attach a border to the wrapped component.
     *
     * @param border The {@link Border} which should be set for the wrapped component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBorder( Border border ) {
        Objects.requireNonNull(border, "Null value for border is not allowed! Use an empty border instead!");
        return _with( c -> c.setBorder(border) )._this();
    }

    /**
     *  Use this to dynamically attach a border to the wrapped component. <br>
     *  <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param border The {@link Border} which should be set for the wrapped component wrapped in a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     * @deprecated Because changing the {@link Border} of a component dynamically after
     *             the component was initialized through the declarative SwingTree API,
     *             causes issues with the style engine of a component.
     *             (Which itself is based on using a custom border for style rendering)
     */
    @Deprecated
    public final I withBorder( Val<Border> border ) {
        NullUtil.nullArgCheck(border, "border", Val.class);
        NullUtil.nullPropertyCheck(border, "border", "Null value for border is not allowed! Use an empty border instead!");
        return _withOnShow( border, (c,v) -> {
                    c.setBorder(v);
                })
                ._with( c -> {
                    c.setBorder( border.orElseThrowUnchecked() );
                })
                ._this();
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
        return _with( c -> c.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right)) )._this();
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
        return _with( c -> c.setBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createEmptyBorder(top, left, bottom, right),
                            title
                        )
                    )
                )
                ._this();
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
        return _withOnShow( title, (c,v) -> {
                    c.setBorder(
                            BorderFactory.createTitledBorder(
                                BorderFactory.createEmptyBorder(top, left, bottom, right),
                                v
                            )
                        );
                })
                ._with( c -> {
                    c.setBorder(
                            BorderFactory.createTitledBorder(
                                    BorderFactory.createEmptyBorder(top, left, bottom, right),
                                    title.orElseThrowUnchecked()
                            )
                    );
                })
                ._this();
    }

    /**
     *  Use this to define an empty {@link Border} with the provided insets.
     *
     * @param topBottom The top and bottom insets.
     * @param leftRight The left and right insets.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withEmptyBorder( int topBottom, int leftRight ) {
        return withEmptyBorder( topBottom, leftRight, topBottom, leftRight );
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
        return _withOnShow( title, (c,v) -> {
                    c.setBorder(
                            BorderFactory.createTitledBorder(
                                BorderFactory.createEmptyBorder(topBottom, leftRight, topBottom, leftRight),
                                v
                            )
                        );
                })
                ._with( c -> {
                    c.setBorder(
                            BorderFactory.createTitledBorder(
                                    BorderFactory.createEmptyBorder(topBottom, leftRight, topBottom, leftRight),
                                    title.orElseThrowUnchecked()
                            )
                    );
                })
                ._this();
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
        return _withOnShow( all, (c,v) -> {
                    c.setBorder(BorderFactory.createEmptyBorder(v, v, v, v));
                })
                ._with( c -> {
                    c.setBorder(BorderFactory.createEmptyBorder(all.orElseThrowUnchecked(), all.orElseThrowUnchecked(), all.orElseThrowUnchecked(), all.orElseThrowUnchecked()));
                })
                ._this();
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
        return _withOnShow( title, (c,v) -> {
                    c.setBorder(
                            BorderFactory.createTitledBorder(
                                BorderFactory.createEmptyBorder(all, all, all, all),
                                v
                            )
                        );
                })
                ._with( c -> {
                    c.setBorder(
                            BorderFactory.createTitledBorder(
                                    BorderFactory.createEmptyBorder(all, all, all, all),
                                    title.orElseThrowUnchecked()
                            )
                    );
                })
                ._this();
    }

    /**
     *  Use this to define an empty {@link Border} with a title
     *  and a default insets size of 5.
     *
     * @param title The title of the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withEmptyBorderTitled( String title ) {
        NullUtil.nullArgCheck( title, "title", String.class );
        return withEmptyBorderTitled(title, 5);
    }

    /**
     *  Creates a titled empty border bound to a {@link String} property
     *  and a default insets size of 5.
     *
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
        return _with( c -> c.setBorder(BorderFactory.createLineBorder(color, thickness)) )._this();
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
        return _withOnShow( color, (c,v) -> {
                    c.setBorder(BorderFactory.createLineBorder(v, thickness));
                })
                ._with( c -> {
                    c.setBorder(BorderFactory.createLineBorder(color.orElseThrowUnchecked(), thickness));
                })
                ._this();
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
        return _with( c -> c.setBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(color, thickness),
                            title
                        )
                    )
                )
                ._this();
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
        return _withOnShow( title, (c,v) -> {
                    c.setBorder(
                            BorderFactory.createTitledBorder(
                                BorderFactory.createLineBorder(color, thickness),
                                v
                            )
                        );
                })
                ._with( c -> {
                    c.setBorder(
                            BorderFactory.createTitledBorder(
                                    BorderFactory.createLineBorder(color, thickness),
                                    title.orElseThrowUnchecked()
                            )
                    );
                })
                ._this();
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
        return _withOnShow( title, (c,v) -> {
                    c.setBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(color.orElseThrowUnchecked(), thickness),
                            v
                        )
                    );
                })
                ._withOnShow( color, (c,v) -> {
                    c.setBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(v, thickness),
                            title.orElseThrowUnchecked()
                        )
                    );
                })
                ._with( c -> {
                    c.setBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(color.orElseThrowUnchecked(), thickness),
                            title.orElseThrowUnchecked()
                        )
                    );
                })
                ._this();
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
        return _with( c -> c.setBorder(BorderFactory.createLineBorder(color, thickness, true)) )._this();
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
        return _with( c -> c.setBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(color, thickness, true),
                            title
                        )
                    )
                )
                ._this();
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
        return _withOnShow( title, (c,v) -> {
                    c.setBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(color, thickness, true),
                            v
                        )
                    );
                })
                ._with( c -> {
                    c.setBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(color, thickness, true),
                            title.orElseThrowUnchecked()
                        )
                    );
                })
                ._this();
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
        return _withOnShow( title, (c,v) -> {
                    c.setBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(color.orElseThrowUnchecked(), thickness, true),
                            v
                        )
                    );
                })
                ._withOnShow( color, (c,v) -> {
                    c.setBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(v, thickness, true),
                            title.orElseThrowUnchecked()
                        )
                    );
                })
                ._with( c -> {
                    c.setBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(color.orElseThrowUnchecked(), thickness, true),
                            title.orElseThrowUnchecked()
                        )
                    );
                })
                ._this();
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
        return _withOnShow( color, (c,v) -> {
                    c.setBorder(BorderFactory.createLineBorder(v, thickness, true));
                })
                ._with( c -> {
                    c.setBorder(BorderFactory.createLineBorder(color.orElseThrowUnchecked(), thickness, true));
                })
                ._this();
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
        return withRoundedLineBorderTitled( title, java.awt.Color.BLACK, 1 );
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
        return _with( c -> c.setBorder(BorderFactory.createMatteBorder(top, left, bottom, right, color)) )._this();
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
        return _with( c -> c.setBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createMatteBorder(top, left, bottom, right, color),
                            title
                        )
                    )
                )
                ._this();
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
        return _with( c -> c.setBorder(BorderFactory.createCompoundBorder(first, second)) )._this();
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
        return _with( c -> c.setBorder(
                        BorderFactory.createTitledBorder(
                            BorderFactory.createCompoundBorder(first, second),
                            title
                        )
                    )
                )
                ._this();
    }

    /**
     *  Use this to attach a {@link javax.swing.border.TitledBorder} with the provided title.
     *
     * @param title The title of the border.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBorderTitled( String title ) {
        NullUtil.nullArgCheck(title, "title", String.class, "Please use an empty String instead of null!");
        return _with( c -> c.setBorder(BorderFactory.createTitledBorder(title)) )._this();
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
        return _withOnShow( title, (c,t) -> {
                    Border foundBorder = c.getBorder();
                    if ( foundBorder instanceof TitledBorder )
                        ((TitledBorder)foundBorder).setTitle(t);
                    else
                        c.setBorder(BorderFactory.createTitledBorder(t));
                })
                ._with( c -> {
                    c.setBorder(BorderFactory.createTitledBorder(title.orElseThrowUnchecked()));
                })
                ._this();
    }

    /**
     *  Use this set the cursor type which should be displayed
     *  when hovering over the UI component wrapped by this builder.
     *  <br>
     *  Here an example of how to use this method:
     *  <pre>{@code
     *      UI.button("Click me!").withCursor(UI.Cursor.HAND);
     *  }</pre>
     *
     * @param type The {@link UI.Cursor} type defined by a simple enum exposed by this API.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withCursor( UI.Cursor type ) {
        NullUtil.nullArgCheck( type, "type", UI.Cursor.class );
        return _with( c -> c.setCursor( new java.awt.Cursor( type.type ) ) )._this();
    }

    /**
     *  Use this to dynamically set the cursor type which should be displayed
     *  when hovering over the UI component wrapped by this builder. <br>
     *  <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param type The {@link UI.Cursor} type defined by a simple enum exposed by this API wrapped in a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withCursor( Val<UI.Cursor> type ) {
        NullUtil.nullArgCheck( type, "type", Val.class );
        NullUtil.nullPropertyCheck(type, "type", "Null is not allowed to model a cursor type.");
        return _withOnShow( type, (c,t) -> {
                    c.setCursor( new java.awt.Cursor( t.type ) );
                })
                ._with( c -> {
                    c.setCursor( new java.awt.Cursor( type.orElseThrowUnchecked().type ) );
                })
                ._this();
    }

    /**
     *  Use this to set the cursor type which should be displayed
     *  when hovering over the UI component wrapped by this builder
     *  based on boolean property determining if the provided cursor should be set ot not. <br>
     *  <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param condition The boolean property determining if the provided cursor should be set ot not.
     * @param type The {@link UI.Cursor} type defined by a simple enum exposed by this API wrapped in a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withCursorIf( Val<Boolean> condition, UI.Cursor type ) {
        NullUtil.nullArgCheck( condition, "condition", Val.class );
        NullUtil.nullArgCheck( type, "type", UI.Cursor.class );
        NullUtil.nullPropertyCheck(condition, "condition", "Null is not allowed to model the cursor selection state.");
        return _withOnShow( condition, (c,v) -> {
                    c.setCursor( new java.awt.Cursor( v ? type.type : UI.Cursor.DEFAULT.type ) );
                })
                ._with( c -> {
                    c.setCursor( new java.awt.Cursor( condition.orElseThrowUnchecked() ? type.type : UI.Cursor.DEFAULT.type ) );
                })
                ._this();
    }

    /**
     *  Use this to dynamically set the cursor type which should be displayed
     *  when hovering over the UI component wrapped by this builder
     *  based on boolean property determining if the provided cursor should be set ot not. <br>
     *  <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
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
        return _with( thisComponent -> {
                    Cursor[] baseCursor = new Cursor[1];
                    _onShow( condition, thisComponent, (c,v) -> type.fireChange(From.VIEW_MODEL) );
                    _onShow( type, thisComponent, (c,v) -> {
                        if ( baseCursor[0] == null ) baseCursor[0] = c.getCursor();
                        c.setCursor( new java.awt.Cursor( condition.orElseThrowUnchecked() ? v.type : baseCursor[0].getType() ) );
                    });
                })
                ._with( c -> {
                    c.setCursor( new java.awt.Cursor( condition.orElseThrowUnchecked() ? type.orElseThrowUnchecked().type : UI.Cursor.DEFAULT.type ) );
                })
                ._this();
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
        return _with( c -> c.setLayout(layout) )._this();
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
    public final I withFlowLayout() { return this.withLayout(new ResponsiveGridFlowLayout()); }

    /**
     *  Use this to set a {@link FlowLayout} for the component wrapped by this builder. <br>
     *  This is in essence a more convenient way than the alternative usage pattern involving
     *  the {@link #peek(Peeker)} method to peek into the builder's component like so: <br>
     *  <pre>{@code
     *      UI.panel()
     *      .peek( panel -> panel.setLayout(new FlowLayout(alignment.forFlowLayout())) );
     *  }</pre>
     *
     * @param alignment The alignment of the layout.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withFlowLayout( UI.HorizontalAlignment alignment ) {
        NullUtil.nullArgCheck( alignment, "alignment", UI.HorizontalAlignment.class );
        return this.withLayout(new ResponsiveGridFlowLayout(alignment));
    }

    /**
     *  Use this to set a {@link FlowLayout} for the component wrapped by this builder. <br>
     *  This is in essence a more convenient way than the alternative usage pattern involving
     *  the {@link #peek(Peeker)} method to peek into the builder's component like so: <br>
     *  <pre>{@code
     *      UI.panel()
     *      .peek( panel -> panel.setLayout(new FlowLayout(alignment.forFlowLayout(), hgap, vgap)) );
     *  }</pre>
     *
     * @param alignment The alignment of the layout.
     * @param hgap The horizontal gap between components.
     * @param vgap The vertical gap between components.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withFlowLayout( UI.HorizontalAlignment alignment, int hgap, int vgap ) {
        NullUtil.nullArgCheck( alignment, "alignment", UI.HorizontalAlignment.class );
        return this.withLayout(new ResponsiveGridFlowLayout(alignment, hgap, vgap));
    }

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
     *  Use this to set a {@link GridLayout} for the component wrapped by this builder. <br>
     *  This is in essence a more convenient way than the alternative usage pattern involving
     *  the {@link #peek(Peeker)} method to peek into the builder's component like so: <br>
     *  <pre>{@code
     *      UI.panel()
     *      .peek( panel -> panel.setLayout(new GridLayout(rows, cols, hgap, vgap)) );
     *  }</pre>
     *
     * @param rows The number of rows in the grid.
     * @param cols The number of columns in the grid.
     * @param hgap The horizontal gap between cells.
     * @param vgap The vertical gap between cells.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withGridLayout( int rows, int cols, int hgap, int vgap ) {
        return this.withLayout(new GridLayout(rows, cols, hgap, vgap));
    }

    /**
     *  Use this to set a {@link BoxLayout} for the component wrapped by this builder. <br>
     *  This is in essence a more convenient way than the alternative usage pattern involving
     *  the {@link #peek(Peeker)} method to peek into the builder's component like so: <br>
     *  <pre>{@code
     *      UI.panel()
     *      .peek( panel -> panel.setLayout(new BoxLayout(panel, axis.forBoxLayout())) );
     *  }</pre>
     *
     * @param axis The axis for the box layout.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException If the provided axis is {@code null}.
     * @see UI.Axis
     * @see BoxLayout
     */
    public final I withBoxLayout( UI.Axis axis ) {
        NullUtil.nullArgCheck( axis, "axis", UI.Axis.class );
        return _with( c -> c.setLayout(new BoxLayout(c, axis.forBoxLayout())) )._this();
    }

    /**
     *  This creates a {@link MigLayout} for the component wrapped by this UI builder,
     *  based on the provided layout-constraints in the form of a simple string
     *  which is parsed by the {@link ConstraintParser} class into {@link LC} and {@link AC} instances.
     *  (also see {@link #withLayout(LC, AC, AC)}) <br> <br>
     *  A typical usage pattern would be like so: <br>
     *  <pre>{@code
     *    UI.of(new MyCustomPanel())
     *    .withLayout("fill wrap 2");
     *    .add( UI.button("Name:") )
     *    .add( UI.textArea() )
     *    .add(...)
     *    ...
     *  }</pre>
     *  In this example a new {@link MigLayout} is created which
     *  will wrap the components in the layout grid after 2 columns
     *  and fill the entire available space of the parent container.
     *  <br>
     *  Note that if not explicitly specified, the default {@code hidemode} will be set to 2, which means that
     *  when a component is hidden, it will not take up any space and the gaps around it will
     *  be collapsed. <br>
     *  Here an overview of the available hidemode values:
     *  <ul>
     *      <li><b>0:</b><br>
     *         Invisible components will be handled exactly as if they were visible.
     *      </li>
     *      <li><b>1:</b><br>
     *          The size of the component (if invisible) will be set to 0, 0.
     *      </li>
     *      <li><b>2 (SwingTree default):</b><br>
     *          The size of the component (if invisible) will be set to 0, 0 and the gaps
     *          will also be set to 0 around it.
     *      </li>
     *      <li><b>3:</b><br>
     *          Invisible components will not participate in the layout at all and it will
     *          for instance not take up a grid cell.
     *      </li>
     * </ul>
     *
     * @param attr The constraints concerning the entire layout.
     *             Passing {@code null} will result in an exception, use an empty string instead.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException If any of the arguments are {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public final I withLayout( String attr ) {
        NullUtil.nullArgCheck( attr, "attr", String.class );
        return withLayout(attr, "");
    }

    /**
     *  Creates a new {@link MigLayout} for the component wrapped by this UI builder,
     *  based on the provided layout constraints in the form of a {@link LC} instance,
     *  which is a builder for the layout constraints.
     *
     * @param attr A string defining the constraints concerning the entire layout.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException If any of the arguments are {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public final I withLayout( LC attr ) {
        NullUtil.nullArgCheck( attr, "attr", LC.class );
        return withLayout(attr, (AC) null, (AC) null);
    }

    /**
     *  Creates a new {@link MigLayout} for the component wrapped by this UI builder,
     *  based on the provided layout constraints in the form of a {@link LayoutConstraint} instance,
     *  which is an immutable string wrapper for the layout constraints.
     *  Instances of this are usually obtained from the {@link UI} namespace like
     *  {@link UI#FILL} or {@link UI#FILL_X}...
     *
     * @param attr Essentially an immutable string wrapper defining the mig layout.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException If any of the arguments are {@code null}.
     */
    public final I withLayout( LayoutConstraint attr ) {
        NullUtil.nullArgCheck( attr, "attr", LayoutConstraint.class );
        return withLayout(attr.toString(), "");
    }

    /**
     *  This creates a {@link MigLayout} for the component wrapped by this UI builder
     *  based on the provided layout constraints in the form of a string.
     *
     * @param attr A string defining constraints for the entire layout.
     * @param colConstrains The layout constraints for the columns int the {@link MigLayout} instance.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException If any of the arguments are {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public final I withLayout( String attr, String colConstrains ) {
        NullUtil.nullArgCheck(attr, "attr", String.class, "Please use an empty String instead of null!");
        NullUtil.nullArgCheck(colConstrains, "colConstrains", String.class, "Please use an empty String instead of null!");
        return withLayout(attr, colConstrains, "");
    }

    /**
     * This creates a {@link MigLayout} for the component wrapped by this UI builder
     * based on the provided layout constraints in the form of a {@link LC} instance
     * and column constraints in the form of a {@link AC} instance.
     *
     * @param attr The constraints for the layout, a {@link LC} instance.
     * @param colConstrains The column layout for the {@link MigLayout} instance as a {@link AC} instance.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException If any of the arguments are {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public final I withLayout( LC attr, AC colConstrains ) {
        return withLayout(attr, colConstrains, null);
    }

    /**
     * This creates a {@link MigLayout} for the component wrapped by this UI builder
     * based on the provided layout constraints in the form of a {@link LC} instance
     * and column constraints in the form of a simple string.
     *
     * @param attr The constraints for the layout, a {@link LC} instance.
     * @param colConstrains The column layout for the {@link MigLayout} instance as a simple string.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException If any of the arguments are {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public final I withLayout( LC attr, String colConstrains ) {
        AC parsedColConstrains = colConstrains == null ? null : ConstraintParser.parseColumnConstraints(colConstrains);
        return withLayout(attr, parsedColConstrains, null);
    }

    /**
     * This creates a {@link MigLayout} for the component wrapped by this UI builder
     * based on the provided layout constraints in the form of a {@link LC} instance
     * and column and row constraints in the form of a simple string.
     *
     * @param attr The constraints for the layout, a {@link LC} instance.
     * @param colConstrains The column layout for the {@link MigLayout} instance as a simple string.
     * @param rowConstraints The row layout for the {@link MigLayout} instance as a simple string.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException If any of the arguments are {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public final I withLayout( LC attr, String colConstrains, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LC.class);
        NullUtil.nullArgCheck(colConstrains, "colConstrains", String.class, "Please use an empty String instead of null!");
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class, "Please use an empty String instead of null!");
        AC parsedColConstrains = colConstrains.isEmpty() ? null : ConstraintParser.parseColumnConstraints(colConstrains);
        AC parsedRowConstrains = rowConstraints.isEmpty() ? null : ConstraintParser.parseRowConstraints(rowConstraints);
        return withLayout(attr, parsedColConstrains, parsedRowConstrains);
    }

    /**
     *  Takes the supplied layout constraints and column constraints
     *  uses them to construct a new {@link MigLayout} for the component wrapped by this UI builder.
     *
     * @param attr The constraints for the layout.
     * @param colConstrains The column layout for the {@link MigLayout} instance.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException If any of the arguments are {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public final I withLayout( LayoutConstraint attr, String colConstrains ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        NullUtil.nullArgCheck(colConstrains, "colConstrains", String.class, "Please use an empty String instead of null!");
        return withLayout(attr.toString(), colConstrains, "");
    }

    /**
     *  This creates a {@link MigLayout} for the component wrapped by this UI builder.
     *
     * @param attr The constraints for the layout in the form of a {@link LayoutConstraint} instance.
     * @param colConstrains The column layout for the {@link MigLayout} instance.
     * @param rowConstraints The row layout for the {@link MigLayout} instance.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout( LayoutConstraint attr, String colConstrains, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        NullUtil.nullArgCheck(colConstrains, "colConstrains", String.class, "Please use an empty String instead of null!");
        return withLayout(attr.toString(), colConstrains, rowConstraints);
    }

    /**
     *  This creates a {@link MigLayout} for the component wrapped by this UI builder,
     *  based on the provided layout-, column- and row-constraints in the form of simple strings,
     *  which are parsed by the {@link ConstraintParser} class into {@link LC} and {@link AC} instances.
     *  (also see {@link #withLayout(LC, AC, AC)}) <br> <br>
     *  A typical usage pattern would be like so: <br>
     *  <pre>{@code
     *    UI.of(new MyCustomPanel())
     *    .withLayout("wrap 2", "[]6[]", "[]8[]");
     *    .add( UI.label("Name:") )
     *    .add( UI.textField() )
     *    .add(...)
     *    ...
     *  }</pre>
     *  In this example a new {@link MigLayout} is created which
     *  will wrap the components in the layout grid after 2 columns,
     *  where the 2 columns are separated by a 6 pixel gap and the rows
     *  are separated by an 8 pixel gap. <br>
     *  <br>
     *  Note that if not explicitly specified, the default {@code hidemode} will be set to 2, which means that
     *  when a component is hidden, it will not take up any space and the gaps around it will
     *  be collapsed. <br>
     *  Here an overview of the available hidemode values:
     *  <ul>
     *      <li><b>0:</b><br>
     *         Invisible components will be handled exactly as if they were visible.
     *      </li>
     *      <li><b>1:</b><br>
     *          The size of the component (if invisible) will be set to 0, 0.
     *      </li>
     *      <li><b>2 (SwingTree default):</b><br>
     *          The size of the component (if invisible) will be set to 0, 0 and the gaps
     *          will also be set to 0 around it.
     *      </li>
     *      <li><b>3:</b><br>
     *          Invisible components will not participate in the layout at all and it will
     *          for instance not take up a grid cell.
     *      </li>
     * </ul>
     *
     * @param constraints The constraints concerning the entire layout.
     *                    Passing {@code null} will result in an exception, use an empty string instead.
     * @param colConstrains The column layout for the {@link MigLayout} instance,
     *                      which concern the columns in the layout grid.
     *                      Passing {@code null} will result in an exception, use an empty string instead.
     * @param rowConstraints The row layout for the {@link MigLayout} instance,
     *                       which concern the rows in the layout grid.
     *                       Passing {@code null} will result in an exception, use an empty string instead.
     * @return This very instance, which enables builder-style method chaining.
     *
     * @throws IllegalArgumentException If any of the arguments are {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public final I withLayout( String constraints, String colConstrains, String rowConstraints ) {
        NullUtil.nullArgCheck(constraints, "constraints", String.class, "Please use an empty String instead of null!");
        NullUtil.nullArgCheck(colConstrains, "colConstrains", String.class, "Please use an empty String instead of null!");
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class, "Please use an empty String instead of null!");

        // We make sure the default hidemode is 2 instead of 3 (which sucks because it takes up too much space)
        if ( constraints.isEmpty() )
            constraints = "hidemode 2";
        else if ( !constraints.contains("hidemode") )
            constraints += ", hidemode 2";

        constraints    = ( constraints.isEmpty() ? null : constraints );
        colConstrains  = ( colConstrains.isEmpty() ? null : colConstrains );
        rowConstraints = ( rowConstraints.isEmpty() ? null : rowConstraints );

        MigLayout migLayout = new MigLayout(constraints, colConstrains, rowConstraints);
        return _with( c -> c.setLayout(migLayout) )._this();
    }

    /**
     *  This creates a {@link MigLayout} for the component wrapped by this UI builder.
     *
     * @param attr The constraints for the layout.
     * @param colConstrains The column layout for the {@link MigLayout} instance.
     * @param rowConstraints The row layout for the {@link MigLayout} instance.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout( @Nullable LC attr, @Nullable AC colConstrains, @Nullable AC rowConstraints ) {
        // We make sure the default hidemode is 2 instead of 3 (which sucks because it takes up too much space)
        if ( attr == null )
            attr = new LC().hideMode(2);
        else if ( attr.getHideMode() == 0 )
            attr = attr.hideMode(2);

        MigLayout migLayout = new MigLayout(attr, colConstrains, rowConstraints);
        return _with( c -> c.setLayout(migLayout) )._this();
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
        return _with( c -> c.setToolTipText(tooltip.isEmpty() ? null : tooltip) )._this();
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
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param tip The tooltip which should be displayed when hovering over the tab header.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final I withTooltip( Val<String> tip ) {
        NullUtil.nullArgCheck(tip, "tip", Val.class);
        NullUtil.nullPropertyCheck(tip, "tip", "Please use an empty string instead of null!");
        return _withOnShow( tip, (c,v) -> {
                    c.setToolTipText( v.isEmpty() ? null : v );
                })
                ._with( c -> {
                    String tipString = tip.orElse("");
                    c.setToolTipText( tipString.isEmpty() ? null : tipString );
                })
                ._this();
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
        return _with( c -> _setBackground(c, color) )
                ._this();
    }

    /**
     *  Use this to set the background color of the UI component
     *  of this declarative builder using a color String.
     *  The supplied {@link String} is parsed to a {@link UI.Color}
     *  through the {@link UI#color(String)} method.<br>
     *  This is in essence a convenience method, which avoid having to expose the underlying component
     *  through the {@link #peek(Peeker)} method like so: <br>
     *  <pre>{@code
     *      UI.label("Something")
     *      .peek( label -> label.setBackground(UI.color("cyan")) );
     *  }</pre>
     *
     * @param color A color string which should be parsed to a {@link UI.Color} instance
     *              and then set as the background color of the UI component.
     * @return A new reference to this type of builder, to allow for fluent method chaining.
     */
    public final I withBackgroundColor( String color ) {
        NullUtil.nullArgCheck(color, "color", String.class);
        return this.withBackground(UI.color(color));
    }

    @SuppressWarnings("ReferenceEquality")
    protected void _setBackground( JComponent thisComponent, Color color ) {
        color = _isUndefinedColor(color) ? null : color;
        thisComponent.setBackground( color );
        color = thisComponent.getBackground();
        // ^ If the provided color is null the component may inherit the color from its parent!
        if ( color == UI.Color.TRANSPARENT ) {
            thisComponent.setOpaque(false);
        }
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
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param bg The background color which should be set for the UI component wrapped by a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBackground( Val<Color> bg ) {
        NullUtil.nullArgCheck(bg, "bg", Val.class);
        NullUtil.nullPropertyCheck(bg, "bg", "Please use the default color of this component instead of null!");
        return _withOnShow( bg, (c,v) -> {
                    c.setBackground( _isUndefinedColor(v) ? null : v );
                })
                ._with( c -> {
                    c.setBackground( _isUndefinedColor(bg.get()) ? null : bg.get() );
                })
                ._this();
    }

    /**
     *  Use this to bind to a background color
     *  which will be set dynamically based on a boolean property.
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param colorIfTrue The background color which should be set for the UI component.
     * @param condition The condition property which determines whether the background color should be set or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBackgroundIf( Val<Boolean> condition, Color colorIfTrue ) {
        NullUtil.nullArgCheck(condition, "condition", Val.class);
        NullUtil.nullArgCheck(colorIfTrue, "bg", Color.class);
        NullUtil.nullPropertyCheck(condition, "condition", "Null is not allowed to model the usage of the provided background color!");
        return _with( thisComponent -> {
                    Var<Color> baseColor = Var.of( thisComponent.getBackground() );
                    Var<Color> color = Var.of( colorIfTrue );
                    _onShow( condition, thisComponent, (c,v) -> _updateBackground( c, condition, color, baseColor ) );
                })
                ._with( c -> {
                    Color newColor =  condition.get() ? colorIfTrue : c.getBackground();
                    c.setBackground( _isUndefinedColor(newColor) ? null : newColor );
                })
                ._this();
    }

    /**
     *  Use this to dynamically bind to a background color
     *  which will be set dynamically based on a boolean property.
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
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
        return _with( thisComponent -> {
                    Var<Color> baseColor = Var.of( thisComponent.getBackground() );
                    _onShow( condition, thisComponent, (c,v) -> _updateBackground( c, condition, color, baseColor ) );
                    _onShow( color,     thisComponent, (c,v) -> _updateBackground( c, condition, color, baseColor ) );
                })
                ._with( c -> {
                    Color newColor = condition.get() ? color.get() : c.getBackground();
                    c.setBackground( _isUndefinedColor(newColor) ? null : newColor );
                })
                ._this();
    }

    /**
     *  Use this to bind to 2 colors to the background of the component
     *  which sre set based on the value of a boolean property.
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
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
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
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
        return _withOnShow( condition, (c,v) -> {
                   _updateBackground( c, condition, colorIfTrue, Var.of(colorIfFalse.get()) );
               })
               ._withOnShow( colorIfTrue, (c,v) -> {
                   _updateBackground( c, condition, colorIfTrue, Var.of(colorIfFalse.get()) );
               })
               ._withOnShow( colorIfFalse, (c,v) -> {
                   _updateBackground( c, condition, colorIfTrue, Var.of(colorIfFalse.get()) );
               })
               ._with( c -> {
                   Color newColor = condition.get() ? colorIfTrue.get() : colorIfFalse.get();
                   c.setBackground( _isUndefinedColor(newColor) ? null : newColor );
               })
               ._this();
    }

    /**
     *    Allows you to configure how the component wrapped by this builder
     *    looks and behaves, by passing a {@link Styler} lambda to this method
     *    which receiving a {@link swingtree.style.ComponentStyleDelegate} and returns
     *    an updated version with the desired style rules applied.
     *    <p>
     *    Here a typical example of how to style a button
     *    using the style API:
     *    <pre>{@code
     *        UI.button("Click Me!")
     *        .withStyle( it -> it
     *            .borderColor(Color.CYAN)
     *            .borderWidthAt(Edge.BOTTOM, 3)
     *            .borderRadius(10)
     *        )
     *    }</pre>
     *    <p>
     *    Here the {@code it} variable is the {@link swingtree.style.ComponentStyleDelegate} which
     *    exposes an extensive API for configuring how a particular component
     *    looks and behaves.
     *    <p>
     *    If you want to define style rules for an entire GUI or a part of it,
     *    take a look at the {@link swingtree.style.StyleSheet} class,
     *    which exposes an API for defining style rules similar to CSS
     *    but based on declarative source code instead of a text file.
     *
     * @param styler A {@link Styler} lambda can define a set of style rules for the component wrapped by this builder
     *               by receiving a {@link swingtree.style.ComponentStyleDelegate} and returning
     *               an updated version with the desired style rules applied.
     *
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withStyle( Styler<C> styler ) {
        NullUtil.nullArgCheck(styler, "styler", Styler.class);
        return _with( c -> {
                    ComponentExtension.from(c).addStyler( styler );
                })
                ._this();
    }

    /**
     *    Here an example demonstrating how a transitional style can be applied
     *    to make a border which can transition between 2 colors based on a boolean property:
     *    <pre>{@code
     *      UI.button("Click Me!")
     *      .withTransitionalStyle(vm.isError(), LifeTime.of(1, TimeUnit.SECONDS), (state, it) -> it
     *          .backgroundColor(Color.CYAN)
     *          .border(3, new Color((int)(state.progress() * 255), 0, 0))
     *      )
     *    }</pre>
     *
     *
     * @param transitionToggle The boolean {@link Val} property which determines the state to which the style should transition.
     *                         When the value of this property is {@code true}, the style will transition to a {@link AnimationStatus#progress()}
     *                         of {@code 1.0} over the provided {@link LifeTime}.
     *                         And when the value of this property is {@code false}, the style will transition to a {@link AnimationStatus#progress()}
     *                         of {@code 0.0} over the provided {@link LifeTime}.
     *
     * @param transitionLifeTime The {@link LifeTime} of the transition animation.
     *                           It defines for ow long the {@link AnimationStatus#progress()} will transition from {@code 0} to {@code 1} or vice versa.
     *
     * @param styler An {@link AnimatedStyler} lambda can define a set of style rules for the component wrapped by this builder
     *               by receiving an {@link AnimationStatus} and a {@link swingtree.style.ComponentStyleDelegate} and returning
     *               an updated version with the desired style rules applied.
     *               The {@link AnimatedStyler} may apply the style properties according to the {@link AnimationStatus}
     *               and its {@link AnimationStatus#progress()} method (or other methods) to create a smooth
     *               transition between the 2 states.
     *
     * @return This builder instance, which enables fluent method chaining.
     * @see #withTransitoryStyle(Event, LifeTime, AnimatedStyler)
     */
    public final I withTransitionalStyle(
        Val<Boolean>      transitionToggle,
        LifeTime          transitionLifeTime,
        AnimatedStyler<C> styler
    ) {
        NullUtil.nullArgCheck(transitionToggle, "transitionToggle", Val.class);
        NullUtil.nullArgCheck(transitionLifeTime, "transitionLifeTime", LifeTime.class);
        NullUtil.nullArgCheck(styler, "styler", AnimatedStyler.class);
        return _with( c -> {
                    FlipFlopStyler<C> flipFlopStyler = new FlipFlopStyler<>(transitionToggle.get(), c, transitionLifeTime, styler);
                    ComponentExtension.from(c).addStyler(flipFlopStyler::style);
                    _onShow( transitionToggle, c, (comp, v) -> flipFlopStyler.set(v) );
                })
                ._this();
    }

    /**
     *    Allows you to configure a style which will be applied to the component temporarily
     *    when the provided {@link Event} is fired. The style will be applied for the provided
     *    {@link LifeTime} and then removed again.
     *    Here an example demonstrating how an event based style animation which temporarily
     *    defines a custom background and border color on a label:
     *    <pre>{@code
     *      UI.label("I have a highlight animation!")
     *      .withTransitoryStyle(vm.highlightEvent(), LifeTime.of(0.5, TimeUnit.SECONDS), (state, it) -> it
     *          .backgroundColor(new Color(0, 0, 0, (int)(state.progress() * 255)))
     *          .borderColor(new Color(255, 255, 255, (int)(state.progress() * 255)))
     *      )
     *    }</pre>
     *
     * @param styleEvent The {@link Event} which should trigger the style animation.
     * @param styleLifeTime The {@link LifeTime} of the style animation.
     * @param styler An {@link AnimatedStyler} lambda can define a set of style rules for the component wrapped by this builder
     *               by receiving an {@link AnimationStatus} and a {@link swingtree.style.ComponentStyleDelegate} and returning
     *               an updated version with the desired style rules applied.
     *               The {@link AnimatedStyler} may apply the style properties according to the {@link AnimationStatus}
     *               and its {@link AnimationStatus#progress()} method (or other methods) to create a smooth
     *               transition between the 2 states.
     *
     * @return This builder instance, which enables fluent method chaining.
     * @see #withTransitionalStyle(Val, LifeTime, AnimatedStyler)
     */
    public final I withTransitoryStyle(
        Event             styleEvent,
        LifeTime          styleLifeTime,
        AnimatedStyler<C> styler
    ){
        NullUtil.nullArgCheck(styleEvent, "styleEvent", Event.class);
        NullUtil.nullArgCheck(styleLifeTime, "styleLifeTime", LifeTime.class);
        NullUtil.nullArgCheck(styler, "styler", AnimatedStyler.class);
        return _with( thisComponent -> {
                    Observable.cast(styleEvent).subscribe(Observer.ofWeak(thisComponent, (innerComponent)->{
                        AnimationDispatcher.animateFor(styleLifeTime, thisComponent).go(status ->
                            ComponentExtension.from(thisComponent)
                                .addAnimatedStyler(status, conf -> styler.style(status, conf))
                        );
                    }));
                })
                ._this();
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
        return _with( c -> c.setForeground( _isUndefinedColor(color) ? null : color ) )._this();
    }

    /**
     *  Allows you to define the {@link JComponent#getForeground()} color of
     *  the underlying {@link JComponent} using a color string.
     *  The supplied {@link String} is parsed to a {@link UI.Color}
     *  through the {@link UI#color(String)} method for you. <br>
     *  This is in essence a convenience method, which avoid having to expose the underlying component
     *  through the {@link #peek(Peeker)} method like so: <br>
     *  <pre>{@code
     *      UI.label("Something")
     *      .peek( label -> label.setForeground(UI.color("oak")) );
     *  }</pre>
     *
     * @param color A color string which should be parsed to a {@link UI.Color} instance
     *              and then set as the foreground color of the UI component.
     * @return A new reference to this type of builder, to allow for fluent method chaining.
     */
    public final I withForegroundColor( String color ) {
        NullUtil.nullArgCheck(color, "color", String.class);
        return this.withForeground(UI.color(color));
    }

    /**
     *  Use this to bind to a {@link sprouts.Val}
     *  containing a foreground color.
     *  This is a convenience method, which works
     *  similar to:
     *  <pre>{@code
     *      UI.button("Click Me")
     *      .peek( button -> {
     *          fg.onChange(From.VIEW_MODEL,  v -> button.setForeground(v.get()) );
     *          button.setForeground(fg.get());
     *      });
     *  }</pre><br>
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param fg The foreground color which should be set for the UI component wrapped by a {@link Val}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withForeground( Val<Color> fg ) {
        NullUtil.nullArgCheck(fg, "fg", Val.class);
        NullUtil.nullPropertyCheck(fg, "fg", "Please use the default color of this component instead of null!");
        return _withOnShow( fg, (c,v) -> {
                    c.setForeground( _isUndefinedColor(v) ? null : v );
                })
                ._with( c -> {
                    Color newColor = fg.get();
                    if ( _isUndefinedColor(newColor))
                        newColor = null;
                    c.setForeground( newColor );
                })
                ._this();
    }

    /**
     *  Use this to bind to a foreground color
     *  which will be set dynamically based on a boolean property.
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param fg The foreground color which should be set for the UI component.
     * @param condition The condition property which determines whether the foreground color should be set or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withForegroundIf( Val<Boolean> condition, Color fg ) {
        NullUtil.nullArgCheck(condition, "condition", Val.class);
        NullUtil.nullArgCheck(fg, "fg", Color.class);
        NullUtil.nullPropertyCheck(condition, "condition", "Null is not allowed to model the usage of the provided foreground color!");
        return _with( thisComponent -> {
                    Var<Color> baseColor = Var.of( thisComponent.getForeground() );
                    Var<Color> newColor = Var.of( fg );
                    _onShow( condition, thisComponent, (c,v) -> _updateForeground( c, condition, newColor, baseColor ) );
                })
                ._with( c -> {
                    Color newColor = condition.get() ? fg : c.getForeground();
                    if ( _isUndefinedColor(newColor))
                        newColor = null;
                    c.setForeground( newColor );
                })
                ._this();
    }

    /**
     *  Use this to dynamically bind to a foreground color
     *  which will be set dynamically based on a boolean property.
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
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
        return _with( thisComponent -> {
                    Var<Color> baseColor = Var.of( thisComponent.getForeground() );
                    _onShow( condition, thisComponent, (c,v) -> _updateForeground( c, condition, color, baseColor ) );
                    _onShow( color,     thisComponent, (c,v) -> _updateForeground( c, condition, color, baseColor ) );
                })
                ._with( c -> {
                    Color newColor = condition.get() ? color.get() : c.getForeground();
                    if ( _isUndefinedColor(newColor))
                        newColor = null;
                    c.setForeground( newColor );
                })
                ._this();
    }

    /**
     *  Use this to dynamically bind to a foreground color
     *  which will be set dynamically based on a boolean property.
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
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
        return _withOnShow( condition, (c,v) -> {
                    _updateForeground( c, condition, Var.of(colorIfTrue), Var.of(colorIfFalse) );
                })
                ._with( c -> {
                    Color newColor = condition.get() ? colorIfTrue : colorIfFalse;
                    if ( _isUndefinedColor(newColor) )
                        newColor = null;
                    c.setForeground( newColor );
                })
                ._this();
    }

    /**
     *  Use this to dynamically bind to a foreground color
     *  which will be set dynamically based on a boolean property.
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
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
        return _withOnShow( condition, (c,v) -> {
                    _updateForeground( c, condition, colorIfTrue, Var.of(colorIfFalse.get()) );
                })
                ._withOnShow( colorIfTrue, (c,v) -> {
                    _updateForeground( c, condition, colorIfTrue, Var.of(colorIfFalse.get()) );
                })
                ._withOnShow( colorIfFalse, (c,v) -> {
                    _updateForeground( c, condition, colorIfTrue, Var.of(colorIfFalse.get()) );
                })
                ._with( c -> {
                    Color newColor = condition.get() ? colorIfTrue.get() : colorIfFalse.get();
                    if ( _isUndefinedColor(newColor))
                        newColor = null;
                    c.setForeground( newColor );
                })
                ._this();
    }

    private void _updateForeground(
        C component,
        Val<Boolean> condition,
        Val<Color>   color,
        Val<Color>   baseColor
    ) {
        Color newColor = condition.is(true) ? color.get() : baseColor.get();
        if ( _isUndefinedColor(newColor))
            newColor = null;

        component.setForeground(newColor);
    }

    private void _updateBackground(
            C component,
            Val<Boolean> condition,
            Val<Color> color,
            Val<Color> baseColor
    ) {
        Color newColor =  condition.is(true) ? color.get() : baseColor.get();
        if ( _isUndefinedColor(newColor) )
            newColor = null;

        component.setBackground(newColor);
    }

    /**
     *  Set the minimum {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component. <br>
     * @param size The minimum {@link Dimension} of the component.
     * @return This very builder to allow for method chaining.
     * @deprecated Due to the inherent pitfalls that come along with the {@link Dimension} being mutable!<br>
     *             Use {@link #withMinSize(Size)} instead.
     */
    @Deprecated
    public final I withMinSize( Dimension size ) {
        NullUtil.nullArgCheck(size, "size", Dimension.class);
        return _with( c -> c.setMinimumSize(UI.scale(size)) )._this();
    }

    /**
     *  Set the minimum {@link Size} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component. <br>
     * @param size The minimum {@link Size} of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMinSize( Size size ) {
        NullUtil.nullArgCheck(size, "size", Dimension.class);
        return _with( c -> c.setMinimumSize(UI.scale(size.toDimension())) )._this();
    }

    /**
     *  Bind to a {@link Val} object to dynamically set the maximum {@link Size} of this {@link JComponent}. <br>
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
     * @param size The minimum {@link Size} of the component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withMinSize( Val<Size> size ) {
        NullUtil.nullArgCheck(size, "size", Val.class);
        NullUtil.nullPropertyCheck(size, "size", "Null is not allowed to model the minimum size of this component!");
        return _withOnShow( size, (c,v) -> {
                    c.setMinimumSize(UI.scale(v.toDimension()));
                    _revalidate(c);
                })
                ._with( c -> {
                    c.setMinimumSize( UI.scale(size.get().toDimension()) );
                })
                ._this();
    }

    /**
     *  Set the minimum width and heigh ({@link Dimension}) of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component. <br>
     * @param width The minimum width of the component.
     * @param height The minimum height of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMinSize( int width, int height ) {
        return _with( c -> c.setMinimumSize(new Dimension(UI.scale(width), UI.scale(height))) )._this();
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
        return _withOnShow( width, (c,w) -> {
                    c.setMinimumSize(new Dimension(UI.scale(w), c.getMinimumSize().height));
                    _revalidate(c);
                })
                ._withOnShow( height, (c,h) -> {
                    c.setMinimumSize(new Dimension(c.getMinimumSize().width, UI.scale(h)));
                    _revalidate(c);
                })
                ._with( c -> {
                    c.setMinimumSize( new Dimension(UI.scale(width.get()), UI.scale(height.get())) );
                })
                ._this();
    }

    /**
     *  Use this to only set the minimum width of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component for you. <br>
     * @param width The minimum width which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMinWidth( int width ) {
        return _with( c -> {
                    _setMinWidth(c, width);
               })
               ._this();
    }

    protected final void _setMinWidth( C component, int width ) {
        int currentHeight = component.getMinimumSize().height;
        if ( !component.isMinimumSizeSet() && UI.currentLookAndFeel().isOneOf(UI.LookAndFeel.METAL, UI.LookAndFeel.NIMBUS) )
            currentHeight = UI.scale(currentHeight);
        component.setMinimumSize(new Dimension(UI.scale(width), currentHeight));
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
        return _withOnShow( width, (c,w) -> {
                    c.setMinimumSize(new Dimension(UI.scale(w), c.getMinimumSize().height));
                    _revalidate(c); // Swing is not smart enough to do this automatically
                })
                ._with( c -> {
                    _setMinWidth(c, width.get());
                })
                ._this();
    }


    /**
     *  Use this to only set the minimum height of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component for you. <br>
     * @param height The minimum height which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMinHeight( int height ) {
        return _with( c -> {
                    _setMinHeight(c, height);
                })
                ._this();
    }

    protected final void _setMinHeight( C component, int height ) {
        int currentWidth = component.getMinimumSize().width;
        if ( !component.isMinimumSizeSet() && UI.currentLookAndFeel().isOneOf(UI.LookAndFeel.METAL, UI.LookAndFeel.NIMBUS) )
            currentWidth = UI.scale(currentWidth);
        component.setMinimumSize(new Dimension(currentWidth, UI.scale(height)));
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
        return _withOnShow( height, (c,h) -> {
                    c.setMinimumSize(new Dimension(c.getMinimumSize().width, UI.scale(h)));
                    _revalidate(c); // Swing is not smart enough to do this automatically
                })
                ._with( c -> {
                    _setMinHeight(c, height.get());
                })
                ._this();
    }

    /**
     *  Set the maximum {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     * @param size The maximum {@link Dimension} of the component.
     * @return This very builder to allow for method chaining.
     * @deprecated Due to the inherent pitfalls that come along with the {@link Dimension} being mutable!<br>
     *             Use {@link #withMaxSize(Size)} instead.
     */
    @Deprecated
    public final I withMaxSize( Dimension size ) {
        NullUtil.nullArgCheck(size, "size", Dimension.class);
        return _with( c -> c.setMaximumSize(UI.scale(size)) )._this();
    }

    /**
     *  Set the maximum {@link Size} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     * @param size The maximum {@link Size} of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMaxSize( Size size ) {
        NullUtil.nullArgCheck(size, "size", Size.class);
        return _with( c -> c.setMaximumSize(UI.scale(size.toDimension())) )._this();
    }

    /**
     *  Bind to a {@link Val} object to
     *  dynamically set the maximum {@link Size} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     * @param size The maximum {@link Size} of the component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withMaxSize( Val<Size> size ) {
        NullUtil.nullArgCheck(size, "size", Val.class);
        NullUtil.nullPropertyCheck(size, "size", "Null is not allowed to model the maximum size of this component!");
        return _withOnShow( size, (c,v) -> {
                    c.setMaximumSize(UI.scale(v.toDimension()));
                    _revalidate(c); // For some reason this is needed to make the change visible.
                })
                ._with( c -> {
                    c.setMaximumSize( UI.scale(size.get().toDimension()) );
                })
                ._this();
    }

    /**
     *  Set the maximum width and height ({@link Dimension}) of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     * @param width The maximum width of the component.
     * @param height The maximum height of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMaxSize( int width, int height ) {
        return _with( c -> c.setMaximumSize(new Dimension(UI.scale(width), UI.scale(height))) )._this();
    }

    /**
     *  Bind to a {@link Val} object to dynamically set the maximum size of this {@link JComponent}. <br>
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
        return _withOnShow( width, (c,w) -> {
                    c.setMaximumSize(new Dimension(UI.scale(w), c.getMaximumSize().height));
                    _revalidate(c); // Raw Swing is not smart enough to do this automatically :(
                })
                ._withOnShow( height, (c,h) -> {
                    c.setMaximumSize(new Dimension(c.getMaximumSize().width, UI.scale(h)));
                    _revalidate(c); // Still not smart enough to do this automatically :(
                })
                ._with( c -> {
                    c.setMaximumSize( new Dimension(UI.scale(width.get()), UI.scale(height.get())) );
                })
                ._this();
    }

    /**
     *  Use this to only set the maximum width of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component for you. <br>
     * @param width The maximum width which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMaxWidth( int width ) {
        return _with( c -> {
                    _setMaxWidth(c, width);
                })
                ._this();
    }

    private void _setMaxWidth( C component, int width ) {
        int currentHeight = component.getMaximumSize().height;
        if ( !component.isMaximumSizeSet() && UI.currentLookAndFeel().isOneOf(UI.LookAndFeel.METAL, UI.LookAndFeel.NIMBUS) )
            currentHeight = UI.scale(currentHeight);
        component.setMaximumSize(new Dimension(UI.scale(width), currentHeight));
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
        return _withOnShow( width, (c,w) -> {
                    c.setMaximumSize(new Dimension(UI.scale(w), c.getMaximumSize().height));
                    _revalidate(c); // When the size changes, the layout manager needs to be informed.
                })
                ._with( c -> {
                    _setMaxWidth(c, width.get());
                })
                ._this();
    }

    /**
     *  Use this to only set the maximum height of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component for you. <br>
     * @param height The maximum height which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public final I withMaxHeight( int height ) {
        return _with( c -> {
                    _setMaxHeight(c, height);
                })
                ._this();
    }

    private void _setMaxHeight( C component, int height ) {
        int currentWidth = component.getMaximumSize().width;
        if ( !component.isMaximumSizeSet() && UI.currentLookAndFeel().isOneOf(UI.LookAndFeel.METAL, UI.LookAndFeel.NIMBUS) )
            currentWidth = UI.scale(currentWidth);
        component.setMaximumSize(new Dimension(currentWidth, UI.scale(height)));
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
        return _withOnShow( height, (c,h) -> {
                    c.setMaximumSize(new Dimension(c.getMaximumSize().width, UI.scale(h)));
                    _revalidate(c); // The revalidate is necessary to make the change visible, this makes sure the layout is recalculated.
                })
                ._with( c -> {
                    _setMaxHeight(c, height.get());
                })
                ._this();
    }

    /**
     *  Set the preferred {@link Dimension} of this {@link JComponent}, which consists
     *  of a width and a height used as a suggestion to the {@link LayoutManager} of the
     *  parent container. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *
     * @param size The preferred {@link Dimension} of the component.
     * @return This very builder to allow for method chaining.
     * @deprecated Due to the inherent pitfalls that come along with the {@link Dimension} being mutable!<br>
     *             Please use {@link #withPrefSize(Size)} instead.
     */
    @Deprecated
    public final I withPrefSize( Dimension size ) {
        NullUtil.nullArgCheck(size, "size", Dimension.class);
        return _with( c -> c.setPreferredSize(UI.scale(size)) )._this();
    }

    /**
     *  Set the preferred {@link Size} of this {@link JComponent}, which consists
     *  of a width and a height used as a suggestion to the {@link LayoutManager} of the
     *  parent container. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     * @param size The preferred {@link Size} of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefSize( Size size ) {
        NullUtil.nullArgCheck(size, "size", Size.class);
        return _with( c -> c.setPreferredSize(UI.scale(size.toDimension())) )._this();
    }

    /**
     *  Bind to a {@link Val} object to dynamically set the preferred {@link Size} of this {@link JComponent},
     *  which consists of a width and a height used as a suggestion
     *  to the {@link LayoutManager} of the parent container. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *
     * @param size A property holding the preferred {@link Size} of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefSize( Val<Size> size ) {
        NullUtil.nullArgCheck(size, "size", Val.class);
        NullUtil.nullPropertyCheck(size, "size", "Null is not allowed to model the preferred size of this component!");
        return _withOnShow( size, (c,v) -> {
                    c.setPreferredSize(UI.scale(v.toDimension()));
                    _revalidate(c);
                })
                ._with( c -> {
                    c.setPreferredSize( UI.scale(size.get().toDimension()) );
                })
                ._this();
    }

    /**
     *  Set the preferred width and height ({@link Dimension}) of this {@link JComponent},
     *  which consists of a width and a height used as a suggestion
     *  to the {@link LayoutManager} of the parent container. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     * @param width The preferred width of the component.
     * @param height The preferred height of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefSize( int width, int height ) {
        return _with( c -> c.setPreferredSize(new Dimension(UI.scale(width), UI.scale(height))) )._this();
    }

    /**
     *  Bind to a {@link Val} object to dynamically set the preferred {@link Dimension} of this {@link JComponent},
     *  which consists of a width and a height used as a suggestion
     *  to the {@link LayoutManager} of the parent container. <br>
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
        return _withOnShow( width, (c,w) -> {
                    c.setPreferredSize(new Dimension(UI.scale(w), c.getPreferredSize().height));
                    _revalidate(c); // We need to revalidate the component to make sure the layout manager is aware of the new size.
                })
                ._withOnShow( height, (c,h) -> {
                    c.setPreferredSize(new Dimension(c.getPreferredSize().width, UI.scale(h)));
                    _revalidate(c); // We need to revalidate the component to make sure the layout manager is aware of the new size.
                })
                ._with( c -> {
                    c.setPreferredSize( new Dimension(UI.scale(width.get()), UI.scale(height.get())) );
                })
                ._this();
    }

    /**
     *  Use this to only set the preferred width of this {@link JComponent},
     *  which serves as a suggestion to the {@link LayoutManager} of the parent container. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component for you. <br>
     * @param width The preferred width which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefWidth( int width ) {
        return _with( c -> {
                    _setPrefWidth(c, width);
                })
                ._this();
    }

    protected final void _setPrefWidth( C component, int width ) {
        int currentHeight = component.getPreferredSize().height;
        if ( !component.isPreferredSizeSet() && UI.currentLookAndFeel().isOneOf(UI.LookAndFeel.METAL, UI.LookAndFeel.NIMBUS) )
            currentHeight = UI.scale(currentHeight);
        component.setPreferredSize(new Dimension(UI.scale(width), currentHeight));
    }

    /**
     *  Use this to dynamically set only the preferred width of this {@link JComponent},
     *  which serves as a suggestion to the {@link LayoutManager} of the parent container. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component for you. <br>
     * @param width The preferred width which should be set for the underlying component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefWidth( Val<Integer> width ) {
        NullUtil.nullArgCheck(width, "width", Val.class);
        NullUtil.nullPropertyCheck(width, "width", "Null is not allowed to model the preferred width of this component!");
        return _withOnShow( width, (c,w) -> {
                    c.setPreferredSize(new Dimension(UI.scale(w), c.getPreferredSize().height));
                    _revalidate(c); // We need to revalidate the component to make sure the new preferred size is applied.
                })
                ._with( c -> {
                    _setPrefWidth(c, width.get());
                })
                ._this();
    }

    /**
     *  Use this to only set the preferred height of this {@link JComponent},
     *  which serves as a suggestion to the {@link LayoutManager} of the parent container. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component for you. <br>
     * @param height The preferred height which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefHeight( int height ) {
        return _with( c -> {
                    _setPrefHeight(c, height);
                })
                ._this();
    }

    private void _setPrefHeight( C component, int height ) {
        int currentWidth = component.getPreferredSize().width;
        if ( !component.isPreferredSizeSet() && UI.currentLookAndFeel().isOneOf(UI.LookAndFeel.METAL, UI.LookAndFeel.NIMBUS) )
            currentWidth = UI.scale(currentWidth);
        component.setPreferredSize(new Dimension(currentWidth, UI.scale(height)));
    }

    /**
     *  Use this to dynamically set only the preferred height of this {@link JComponent},
     *  which serves as a suggestion to the {@link LayoutManager} of the parent container. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component for you. <br>
     * @param height The preferred height which should be set for the underlying component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withPrefHeight( Val<Integer> height ) {
        NullUtil.nullArgCheck(height, "height", Val.class);
        NullUtil.nullPropertyCheck(height, "height", "Null is not allowed to model the preferred height of this component!");
        return _withOnShow( height, (c,h) -> {
                    c.setPreferredSize(new Dimension(c.getPreferredSize().width, UI.scale(h)));
                    _revalidate(c); // We need to revalidate the component to make sure the new preferred size is applied.
                })
                ._with( c -> {
                    _setPrefHeight(c, height.get());
                })
                ._this();
    }

    /**
     *  Set the current {@link Dimension})/size (width and height) of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param size The current {@link Dimension} of the component.
     * @return This very builder to allow for method chaining.
     * @deprecated Due to the inherent pitfalls that come along with the {@link Dimension} being mutable!<br>
     *             Please use {@link #withSize(Size)} instead.
     */
    @Deprecated
    public final I withSize( Dimension size ) {
        NullUtil.nullArgCheck(size, "size", Dimension.class);
        return _with( c -> c.setSize(UI.scale(size)) )._this();
    }

    /**
     *  Sets the current {@link Size}) (width and height) of this {@link JComponent}. <br>
     *  Note however that calling this method, may not necessarily have a visual effect
     *  on the component, as the layout manager may override the size of the component. <br>
     *  So in case of the component being part of a layout (which is the case most of the time),
     *  you may want to use the {@link #withPrefSize(Size)} method instead. <br>
     *  This calls {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     *
     * @param size The current {@link Size} of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withSize( Size size ) {
        NullUtil.nullArgCheck(size, "size", Dimension.class);
        return _with( c -> c.setSize(UI.scale(size.toDimension())) )._this();
    }

    /**
     *  Bind to a {@link Val} object to dynamically set the current {@link Dimension} of this {@link JComponent}
     *  using a {@link Size} object. <br>
     *  Note however that calling this method, may not necessarily have a visual effect
     *  on the component, as the layout manager may override the size of the component. <br>
     *  So in case of the component being part of a layout (which is the case most of the time),
     *  you may want to use the {@link #withPrefSize(Val)} method instead. <br>
     *  The {@link Size} is automatically translated to a call to
     *  {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     *
     * @param size The current {@link Size} of the component wrapped by a {@link Val} property.
     * @return This very builder to allow for method chaining.
     */
    public final I withSize( Val<Size> size ) {
        NullUtil.nullArgCheck(size, "size", Val.class);
        NullUtil.nullPropertyCheck(size, "size", "Null is not allowed to model the size of this component!");
        return _withOnShow( size, (c,v) -> {
                    c.setSize(UI.scale(v.toDimension()));
                    _revalidate(c); // We need to revalidate the component to make sure the new size is applied.
                })
                ._with( c -> {
                    c.setSize( UI.scale(size.get().toDimension()) );
                })
                ._this();
    }

    /**
     *  Allows you to directly set the width and height of the current component
     *  directly instead of through the layout manager. <br>
     *  Note however that calling this method, may not necessarily have a visual effect
     *  on the component, as the layout manager may override the size of the component. <br>
     *  So in case of the component being part of a layout (which is the case most of the time),
     *  you may want to use the {@link #withPrefSize(int, int)} method instead. <br>
     *  Also note that this method translates to invoking
     *  {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     *
     * @param width The width of the component.
     * @param height The height of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withSize( int width, int height ) {
        return this.withSize( new Dimension(width, height) );
    }

    /**
     *  Set the current width of this {@link JComponent}. <br>
     *  Note however that calling this method, may not necessarily have a visual effect
     *  on the component, as the layout manager may override the set width of the component. <br>
     *  So in case of the component being part of a layout (which is the case most of the time),
     *  you may want to use the {@link #withPrefWidth(int)} method instead. <br>
     *  This calls {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param width The current width of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withWidth( int width ) {
        return _with( c -> {
                    c.setSize(new Dimension(UI.scale(width), c.getSize().height));
                })
                ._this();
    }

    /**
     *  Bind to a {@link Val} object to dynamically set the current width of this {@link JComponent}. <br>
     *  Note however that calling this method, may not necessarily have a visual effect
     *  on the component, as the layout manager may override the width of the component. <br>
     *  So in case of the component being part of a layout (which is the case most of the time),
     *  you may want to use the {@link #withPrefWidth(Val)} method instead. <br>
     *  This calls {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param width The current width of the component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withWidth( Val<Integer> width ) {
        NullUtil.nullArgCheck(width, "width", Val.class);
        NullUtil.nullPropertyCheck(width, "width", "Null is not allowed to model the width of this component!");
        return _withOnShow( width, (c,w) -> {
                    c.setSize(new Dimension(UI.scale(w), c.getSize().height));
                    _revalidate(c); // We need to revalidate the component to make sure the new size is applied.
                })
                ._with( c -> {
                    c.setSize(new Dimension(UI.scale(width.get()), c.getSize().height));
                })
                ._this();
    }

    /**
     *  Set the current height of this {@link JComponent}. <br>
     *  Note however that calling this method, may not necessarily have a visual effect
     *  on the component, as the layout manager may override the height of the component. <br>
     *  So in case of the component being part of a layout (which is the case most of the time),
     *  you may want to use the {@link #withPrefHeight(int)} method instead. <br>
     *  This calls {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param height The current height of the component.
     * @return This very builder to allow for method chaining.
     */
    public final I withHeight( int height ) {
        return _with( c -> {
                    c.setSize(new Dimension(c.getSize().width, UI.scale(height)));
                })
                ._this();
    }

    /**
     *  Bind to a {@link Val} object to dynamically set the current height of this {@link JComponent}. <br>
     *  Note however that calling this method, may not necessarily have a visual effect
     *  on the component, as the layout manager may override the height of the component. <br>
     *  So in case of the component being part of a layout (which is the case most of the time),
     *  you may want to use the {@link #withPrefHeight(Val)} method instead. <br>
     *  This calls {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param height The current height of the component wrapped by a {@link Val}.
     * @return This very builder to allow for method chaining.
     */
    public final I withHeight( Val<Integer> height ) {
        NullUtil.nullArgCheck(height, "height", Val.class);
        NullUtil.nullPropertyCheck(height, "height", "Null is not allowed to model the height of this component!");
        return _withOnShow( height, (c,h) -> {
                    c.setSize(new Dimension(c.getSize().width, UI.scale(h)));
                    _revalidate(c); // We need to revalidate the component to make sure the new size is applied.
                })
                ._with( c -> {
                    c.setSize(new Dimension(c.getSize().width, UI.scale(height.get())));
                })
                ._this();
    }

    private static void _revalidate( Component comp ) {
        comp.revalidate();
        if ( comp instanceof JScrollPane )
            Optional.ofNullable(comp.getParent())
                    .ifPresent(Component::revalidate); // For some reason, JScrollPane does not revalidate its parent when its preferred size changes.
    }

    /**
     *   Allows you to define a common width and height for the minimum, maximum, and preferred size of this component.
     *   This is a convenience method, which is equivalent to:
     *   <pre>{@code
     *      .withMinSize(width, height)
     *      .withMaxSize(width, height)
     *      .withPrefSize(width, height)
     *   }</pre>
     *   This method call translates to calling {@link JComponent#setMinimumSize(Dimension)},
     *   {@link JComponent#setMaximumSize(Dimension)}, and {@link JComponent#setPreferredSize(Dimension)}
     *   on the underlying component.
     *
     * @param width The min-, max- and preferred with of the component.
     * @param height The min-, max- and preferred height of the component.
     * @return A declarative builder instance to allow for further method chaining.
     */
    public final I withSizeExactly( int width, int height ) {
        UIForAnySwing<I, C> self = this;
        self = UIForAnySwing.class.cast(self.withMinSize(width, height));
        self = UIForAnySwing.class.cast(self.withMaxSize(width, height));
        self = UIForAnySwing.class.cast(self.withPrefSize(width, height));
        return self._this();
    }

    /**
     *  Allows you to define a common width and height for the minimum, maximum, and preferred size of this component
     *  in the form of the supplied {@link Size} object.
     *  This is in essence a convenience method equivalent to:
     *  <pre>{@code
     *      .withMinSize(size)
     *      .withMaxSize(size)
     *      .withPrefSize(size)
     *  }</pre>
     *  Underneath, this method call translates to calling {@link JComponent#setMinimumSize(Dimension)},
     *  {@link JComponent#setMaximumSize(Dimension)}, and {@link JComponent#setPreferredSize(Dimension)}
     *  on the underlying component.
     *
     * @param size The min-, max- and preferred {@link Size} of the component.
     * @return A declarative builder instance to allow for further method chaining.
     */
    public final I withSizeExactly( Size size ) {
        Objects.requireNonNull(size, "size");
        if ( size.equals(Size.unknown()) )
            return _this();

        if ( !size.hasPositiveHeight() ) // Width only!
            return withWidthExactly(size.width().map(Number::intValue).orElse(0));
         else if ( !size.hasPositiveWidth() ) // Height only!
            return withHeightExactly(size.height().map(Number::intValue).orElse(0));
        else
            return withSizeExactly(
                    size.width().map(Number::intValue).orElse(0),
                    size.height().map(Number::intValue).orElse(0)
                );
    }

    /**
     *  Bind to a {@link Size} object to dynamically update the common
     *  width and height for the minimum, maximum, and preferred size of this component.
     *  This is in essence a convenience method, which is equivalent to calling:
     *  <pre>{@code
     *      .withMinSize(width, height)
     *      .withMaxSize(width, height)
     *      .withPrefSize(width, height)
     *  }</pre>
     *  This method translates to calling {@link JComponent#setMinimumSize(Dimension)},
     *  {@link JComponent#setMaximumSize(Dimension)}, and {@link JComponent#setPreferredSize(Dimension)}
     *  on the underlying component.
     *
     * @param size A property wrapping the min-, max- and preferred {@link Size} of the component.
     *             When the property item changes, the size of the component will be updated accordingly.
     * @return A declarative builder instance to allow for further method chaining.
     */
    public final I withSizeExactly( Val<Size> size ) {
        NullUtil.nullArgCheck(size, "size", Val.class);
        NullUtil.nullPropertyCheck(size, "size", "Null is not allowed to model the size of this component!");
        return _withOnShow( size, (c,v) -> {
                    c.setMinimumSize(UI.scale(v.toDimension()));
                    c.setMaximumSize(UI.scale(v.toDimension()));
                    c.setPreferredSize(UI.scale(v.toDimension()));
                    _revalidate(c);
                })
                ._with( c -> {
                    c.setMinimumSize(UI.scale(size.get().toDimension()));
                    c.setMaximumSize(UI.scale(size.get().toDimension()));
                    c.setPreferredSize(UI.scale(size.get().toDimension()));
                })
                ._this();
    }

    /**
     *  Allows you to bind to two {@link Val} properties to dynamically update the common width and height
     *  for the minimum, maximum, and preferred size of this component.
     *  This is in essence a convenience method, which is equivalent to:
     *  <pre>{@code
     *      .withMinSize(width, height)
     *      .withMaxSize(width, height)
     *      .withPrefSize(width, height)
     *  }</pre>
     *  This method will translate the property updates to calling {@link JComponent#setMinimumSize(Dimension)},
     *  {@link JComponent#setMaximumSize(Dimension)}, and {@link JComponent#setPreferredSize(Dimension)}
     *  on the underlying component.
     *
     * @param width The min-, max- and preferred width of the component in the form of an integer property.
     *              When the property item changes, the width of the component will be updated accordingly.
     * @param height The min-, max- and preferred height of the component in the form of an integer property.
     *               When the property item changes, the height of the component will be updated accordingly.
     * @return A declarative builder instance to allow for further method chaining.
     * @throws NullPointerException If any of the provided properties is {@code null}.
     */
    public final I withSizeExactly( Val<Integer> width, Val<Integer> height ) {
        Objects.requireNonNull(width, "width");
        Objects.requireNonNull(height, "height");
        NullUtil.nullPropertyCheck(width, "width", "Null is not allowed to model the width of this component!");
        NullUtil.nullPropertyCheck(height, "height", "Null is not allowed to model the height of this component!");
        UIForAnySwing<I, C> self = this;
        self = UIForAnySwing.class.cast(self.withMinSize(width, height));
        self = UIForAnySwing.class.cast(self.withMaxSize(width, height));
        self = UIForAnySwing.class.cast(self.withPrefSize(width, height));
        return self._this();
    }

    /**
     *  Use this to set the min-, max- and preferred width of this {@link JComponent} to the same value. <br>
     *  This is a convenience method, which is equivalent to calling:
     *  <pre>{@code
     *      .withMinWidth(width)
     *      .withMaxWidth(width)
     *      .withPrefWidth(width)
     *  }</pre>
     *  An invocation to this method translates to {@link JComponent#setMinimumSize(Dimension)},
     *  {@link JComponent#setMaximumSize(Dimension)}, and {@link JComponent#setPreferredSize(Dimension)}
     *  on the underlying component.
     *
     * @param width The min-, max- and preferred width of the component.
     * @return A declarative builder instance to allow for further method chaining.
     */
    public final I withWidthExactly( int width ) {
        UIForAnySwing<I, C> self = this;
        self = UIForAnySwing.class.cast(self.withMinWidth(width));
        self = UIForAnySwing.class.cast(self.withMaxWidth(width));
        self = UIForAnySwing.class.cast(self.withPrefWidth(width));
        return self._this();
    }

    /**
     *  Use this to bind to a {@link Val} property to dynamically update the min-, max- and preferred width
     *  of this {@link JComponent} to the same value. <br>
     *  So whenever the item of the property changes, all these widths will
     *  be updated automatically for you.
     *  This is a convenience method, which is equivalent to calling {@link #withMinWidth(Val)},
     *  {@link #withMaxWidth(Val)} and {@link #withPrefWidth(Val)} on the underlying component.
     *  <pre>{@code
     *      .withMinWidth(width)
     *      .withMaxWidth(width)
     *      .withPrefWidth(width)
     *  }</pre>
     *  Eventually, all of this ultimately translates to {@link JComponent#setMinimumSize(Dimension)},
     *  {@link JComponent#setMaximumSize(Dimension)}, and {@link JComponent#setPreferredSize(Dimension)}
     *  on the underlying component.
     *
     * @param width The min-, max- and preferred width of the component wrapped by a {@link Val}.
     *              When the property item changes, the width of the component will be updated accordingly.
     * @return A declarative builder instance to allow for further method chaining.
     */
    public final I withWidthExactly( Val<Integer> width ) {
        Objects.requireNonNull(width, "width");
        NullUtil.nullPropertyCheck(width, "width", "Null is not allowed to model the width of this component!");
        UIForAnySwing<I, C> self = this;
        self = UIForAnySwing.class.cast(self.withMinWidth(width));
        self = UIForAnySwing.class.cast(self.withMaxWidth(width));
        self = UIForAnySwing.class.cast(self.withPrefWidth(width));
        return self._this();
    }

    /**
     *  Use this to set the min-, max- and preferred height of this {@link JComponent} to the same value. <br>
     *  This is a convenience method, which is equivalent to calling:
     *  <pre>{@code
     *      .withMinHeight(height)
     *      .withMaxHeight(height)
     *      .withPrefHeight(height)
     *  }</pre>
     *  An invocation to this method translates to {@link JComponent#setMinimumSize(Dimension)},
     *  {@link JComponent#setMaximumSize(Dimension)}, and {@link JComponent#setPreferredSize(Dimension)}
     *  on the underlying component.
     *
     * @param height The min-, max- and preferred height of the component.
     * @return A declarative builder instance to allow for further method chaining.
     */
    public final I withHeightExactly( int height ) {
        UIForAnySwing<I, C> self = this;
        self = UIForAnySwing.class.cast(self.withMinHeight(height));
        self = UIForAnySwing.class.cast(self.withMaxHeight(height));
        self = UIForAnySwing.class.cast(self.withPrefHeight(height));
        return self._this();
    }

    /**
     *  Use this to bind to a {@link Val} property to dynamically update the min-, max- and preferred height
     *  of this {@link JComponent} to the same value. <br>
     *  So whenever the item of the property changes, all these heights will
     *  be updated automatically for you.
     *  This is a convenience method, which is equivalent to calling {@link #withMinHeight(Val)},
     *  {@link #withMaxHeight(Val)} and {@link #withPrefHeight(Val)} on the underlying component.
     *  <pre>{@code
     *      .withMinHeight(height)
     *      .withMaxHeight(height)
     *      .withPrefHeight(height)
     *  }</pre>
     *  Eventually, all of this ultimately translates to {@link JComponent#setMinimumSize(Dimension)},
     *  {@link JComponent#setMaximumSize(Dimension)}, and {@link JComponent#setPreferredSize(Dimension)}
     *  on the underlying component.
     *
     * @param height The min-, max- and preferred height of the component wrapped by a {@link Val}.
     *               When the property item changes, the height of the component will be updated accordingly.
     * @return A declarative builder instance to allow for further method chaining.
     */
    public final I withHeightExactly( Val<Integer> height ) {
        Objects.requireNonNull(height, "height");
        NullUtil.nullPropertyCheck(height, "height", "Null is not allowed to model the height of this component!");
        UIForAnySwing<I, C> self = this;
        self = UIForAnySwing.class.cast(self.withMinHeight(height));
        self = UIForAnySwing.class.cast(self.withMaxHeight(height));
        self = UIForAnySwing.class.cast(self.withPrefHeight(height));
        return self._this();
    }

    /**
     *  Calls the provided action event handler when the mouse gets pressed and then released.
     *  This delegates to a {@link MouseListener} based mouse click event listener registered in the UI component.
     *  <br><br>
     *  Note that a click is defined as the combination of the <b>mouse being pressed
     *  and then released on the same position as it was pressed.</b>
     *  If the mouse moves between the press and the release events, then the
     *  event is considered a drag event instead of a mouse click! (see {@link #onMouseDrag(Action)})
     *
     * @param onClick The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseClick( Action<ComponentMouseEventDelegate<C>> onClick ) {
        NullUtil.nullArgCheck(onClick, "onClick", Action.class);
        return _with( c -> {
                    c.addMouseListener(new MouseAdapter() {
                        @Override public void mouseClicked(MouseEvent e) {
                            _runInApp(() -> {
                                try {
                                    onClick.accept(new ComponentMouseEventDelegate<>(c, e));
                                } catch ( Exception ex ) {
                                    log.error("Error in mouse click event action handler!", ex);
                                }
                            });
                        }
                    });
                })
                ._this();
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse release events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the component.
     *  <br><br>
     *
     * @param onRelease The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseRelease( Action<ComponentMouseEventDelegate<C>> onRelease ) {
        NullUtil.nullArgCheck(onRelease, "onRelease", Action.class);
        return _with( thisComponent -> {
                    thisComponent.addMouseListener(new MouseAdapter() {
                        @Override public void mouseReleased(MouseEvent e) {
                            _runInApp(() -> {
                                try {
                                    onRelease.accept(new ComponentMouseEventDelegate<>( thisComponent, e ));
                                } catch ( Exception ex ) {
                                    log.error("Error in mouse release event action handler!", ex);
                                }
                            });
                        }
                    });
                })
                ._this();
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse press events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the component.
     *  <br><br>
     *
     * @param onPress The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMousePress( Action<ComponentMouseEventDelegate<C>> onPress ) {
        NullUtil.nullArgCheck(onPress, "onPress", Action.class);
        return _with( thisComponent -> {
                    thisComponent.addMouseListener(new MouseAdapter() {
                        @Override public void mousePressed(MouseEvent e) {
                            _runInApp(() -> {
                                try {
                                    onPress.accept(new ComponentMouseEventDelegate<>(thisComponent, e));
                                } catch ( Exception ex ) {
                                    log.error("Error in mouse press event action handler!", ex);
                                }
                            });
                        }
                    });
                })
                ._this();
    }

    /**
     *  Use this to register and catch mouse enter events on
     *  the {@link UI.ComponentArea#BODY} of this UI component, <br>
     *  which consists of the full component boundaries except for the surrounding
     *  margins and corner rounding areas. <br>
     *  <p>
     *  If you want to catch mouse enter events on a different area of the component, use
     *  {@link #onMouseEnter(UI.ComponentArea, Action)} instead.
     *  Internally, this method adds the supplied {@link Action} lambda to a custom event dispatcher
     *  which ensures that the mouse enter event is only triggered when the mouse enters the
     *  boundaries of the specified area, irrespective of the existence of child components.
     *  <br><br>
     *
     * @param onEnter The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseEnter( Action<ComponentMouseEventDelegate<C>> onEnter ) {
        NullUtil.nullArgCheck(onEnter, "onEnter", Action.class);
        return onMouseEnter(UI.ComponentArea.BODY, onEnter);
    }

    /**
     *  Use this to register and catch mouse enter events on a specific area of this UI component,
     *  defined by the first argument, a {@link UI.ComponentArea} enum value, and the second argument,
     *  a lambda instance which will be invoked when the mouse enters the specified area.
     *  Internally, the provided {@link Action} lambda is handled by a custom event dispatcher
     *  which ensures that the mouse enter event is only triggered when the mouse enters the
     *  boundaries of the specified area, irrespective of the existence of child components.
     *  <br><br>
     *  Note that this mouse enter event is different from the native Swing mouse enter event,
     *  which also considers child components with mouse listeners as enter/exit boundaries.<br>
     *  If you want to rely on this the Swing behavior, use {@link #onMouseEnterGreedy(Action)} instead.<br>
     *  <b>
     *      We do however recommended to rely on this method, to avoid bugs due to the unexpected side effect
     *      of enter events being fired at the boundary to child components.
     *  </b>
     *
     * @param area The specific area of the component where the mouse enter event should be caught.
     * @param onEnter The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseEnter( UI.ComponentArea area, Action<ComponentMouseEventDelegate<C>> onEnter ) {
        NullUtil.nullArgCheck(onEnter, "onEnter", Action.class);
        return _with( thisComponent -> {
                    WeakReference<@Nullable C> source = new WeakReference<>(thisComponent);
                    MouseListener listener = new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) {
                            @Nullable C localComponent = source.get();
                            if ( localComponent != null )
                                _runInApp(() -> {
                                    try {
                                        onEnter.accept(new ComponentMouseEventDelegate<>(localComponent, e));
                                    } catch ( Exception ex ) {
                                        log.error("Error in mouse enter event action handler!", ex);
                                    }
                                });
                        }
                    };
                    EnterExitComponentBoundsEventDispatcher.addMouseEnterListener(area, thisComponent, listener);
                })
                ._this();
    }

    /**
     *  Use this to register and catch simple {@link MouseListener} based mouse enter events on this UI component.
     *  This method adds the supplied {@link Action} lambda in a {@link MouseListener} instance to the component.
     *  <br><br>
     *  This method is greedy in the sense that in case of the parent of this component
     *  also having a mouse listener, then a mouse cursor transition on top of this component
     *  will be considered an exit from the parent component and an enter into this component. <br>
     *  If you want to catch mouse enter events strictly in terms of the cursor being inside
     *  the component boundaries, or one of its areas, use {@link #onMouseEnter(Action)}, or
     *  {@link #onMouseEnter(UI.ComponentArea, Action)} instead.<br>
     *  <p><b>
     *      To avoid bugs due to the unexpected side effect of enter events being fired at the surface
     *      boundaries to child components, we recommend to use {@link #onMouseEnter(Action)} instead of this method!<br>
     *      Also note that this method is different from {@link #onMouseEnter(Action)}, in that it reports enter events at
     *      the boundaries of {@link swingtree.UI.ComponentArea#ALL} instead of {@link swingtree.UI.ComponentArea#BODY}.
     *  </b>
     *
     * @param onEnter The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseEnterGreedy( Action<ComponentMouseEventDelegate<C>> onEnter ) {
        NullUtil.nullArgCheck(onEnter, "onEnter", Action.class);
        return _with( thisComponent -> {
                    thisComponent.addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) {
                            _runInApp(() -> {
                                try {
                                    onEnter.accept(new ComponentMouseEventDelegate<>(thisComponent, e));
                                } catch ( Exception ex ) {
                                    log.error("Error in greedy mouse enter event action handler!", ex);
                                }
                            });
                        }
                    });
                })
                ._this();
    }

    /**
     *  Use this to register and catch mouse exit events on
     *  the {@link UI.ComponentArea#BODY} of this UI component, <br>
     *  which consists of the full component boundaries except for the surrounding
     *  margins and corner rounding areas. <br>
     *  <p>
     *  If you want to catch mouse exit events on a different area than the body of the component, use
     *  {@link #onMouseExit(UI.ComponentArea, Action)} instead.
     *  Internally, this method adds the supplied {@link Action} lambda to a custom event dispatcher
     *  which ensures that the mouse exit event is only triggered when the mouse exits the
     *  boundaries of the specified area, irrespective of the existence of child components.
     *  <br><br>
     *  Note that this mouse enter event is different from the native Swing mouse enter event,
     *  which also considers child components with mouse listeners as enter/exit event boundaries.<br>
     *  If you want to rely on this the Swing behavior, use {@link #onMouseExitGreedy(Action)} instead.<br>
     *  <b>
     *      We do however recommended to rely on this method, to avoid bugs due to the unexpected side effect
     *      of exit events being fired at the boundary to child components.
     *  </b>
     *
     * @param onExit The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseExit( Action<ComponentMouseEventDelegate<C>> onExit ) {
        NullUtil.nullArgCheck(onExit, "onExit", Action.class);
        return onMouseExit(UI.ComponentArea.BODY, onExit);
    }

    /**
     *  Use this to register and catch mouse exit events on a specific area of this UI component,
     *  by supplying a {@link UI.ComponentArea} enum value to define the area, and a {@link Action}
     *  lambda which will be invoked when the mouse exits the specified area, so that you can
     *  react to the event accordingly.<br>
     *  <p>
     *  Internally, the provided {@link Action} lambda is handled by a custom event dispatcher
     *  which ensures that the mouse exit event is only triggered when the mouse exits the
     *  boundaries of the specified area, irrespective of the existence of child components.
     *  <br><br>
     *  Note that this mouse exit event is different from the native Swing mouse exit event,
     *  which also considers child components with mouse listeners as enter/exit boundaries.
     *  If you want to rely on the native Swing behavior, use {@link #onMouseExitGreedy(Action)}
     *  instead of this method.
     *
     * @param area The specific area of the component where the mouse exit event should be caught.
     * @param onExit The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseExit( UI.ComponentArea area, Action<ComponentMouseEventDelegate<C>> onExit ) {
        NullUtil.nullArgCheck(onExit, "onExit", Action.class);
        return _with( thisComponent -> {
                    WeakReference<@Nullable C> source = new WeakReference<>(thisComponent);
                    MouseListener listener = new MouseAdapter() {
                        @Override public void mouseExited(MouseEvent e) {
                            @Nullable C localComponent = source.get();
                            if ( localComponent != null )
                                _runInApp(() -> {
                                    try {
                                        onExit.accept(new ComponentMouseEventDelegate<>(localComponent, e));
                                    } catch ( Exception ex ) {
                                        log.error("Error in mouse exit event action handler!", ex);
                                    }
                                });
                        }
                    };
                    EnterExitComponentBoundsEventDispatcher.addMouseExitListener(area, thisComponent, listener);
                })
                ._this();
    }

    /**
     *  Use this to register and catch simple {@link MouseListener} based mouse exit events on this UI component.
     *  This method adds the supplied {@link Action} lambda in a {@link MouseListener} instance to the component.
     *  <br><br>
     *  The method is considered greedy in the sense that in case of the parent of this component
     *  also having a mouse listener, then a mouse cursor transition over to be on top of this component
     *  will be considered an exit from the parent components and an enter into this component. <br>
     *  If you want to catch mouse exit events strictly in terms of the cursor being inside
     *  the component boundaries, or one of its areas, use {@link #onMouseExit(Action)}, or
     *  {@link #onMouseExit(UI.ComponentArea, Action)} instead.<br>
     *  <p><b>
     *      To avoid bugs due to the unexpected side effect of exit events being fired at the surface
     *      boundaries to child components, we recommend to use {@link #onMouseExit(Action)} instead of this method!<br>
     *      Also note that this method is different from {@link #onMouseExit(Action)}, in that it reports exit events at
     *      the boundaries of {@link swingtree.UI.ComponentArea#ALL} instead of {@link swingtree.UI.ComponentArea#BODY}.
     *  </b>
     *
     * @param onExit The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseExitGreedy( Action<ComponentMouseEventDelegate<C>> onExit ) {
        NullUtil.nullArgCheck(onExit, "onExit", Action.class);
        return _with( thisComponent -> {
                    thisComponent.addMouseListener(new MouseAdapter() {
                        @Override public void mouseExited(MouseEvent e) {
                            _runInApp(() -> {
                                try {
                                onExit.accept(new ComponentMouseEventDelegate<>( thisComponent, e ));
                                } catch ( Exception ex ) {
                                    log.error("Error in greedy mouse exit event action handler!", ex);
                                }
                            });
                        }
                    });
                })
                ._this();
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse drag events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the component.
     *  <br><br>
     *  The {@link ComponentDragEventDelegate} received by the {@link Action} lambda
     *  exposes both component and drag event
     *  context information, including a list of all the {@link MouseEvent}s involved
     *  in one continuous dragging motion (see {@link ComponentDragEventDelegate#dragEvents()} for more information).
     *
     * @param onDrag The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseDrag( Action<ComponentDragEventDelegate<C>> onDrag ) {
        NullUtil.nullArgCheck(onDrag, "onDrag", Action.class);
        return _with( thisComponent -> {
                   java.util.List<MouseEvent> dragEventHistory = new ArrayList<>();
                   MouseAdapter listener = new MouseAdapter() {
                       @Override public void mousePressed(MouseEvent e) {
                           dragEventHistory.clear();
                           dragEventHistory.add(e);
                       }
                       @Override public void mouseReleased(MouseEvent e) {
                           dragEventHistory.clear();
                       }
                       @Override public void mouseDragged(MouseEvent e) {
                           dragEventHistory.add(e);
                           _runInApp(() -> {
                               try {
                                   onDrag.accept(new ComponentDragEventDelegate<>(thisComponent, e, dragEventHistory));
                               } catch ( Exception ex ) {
                                   log.error("Error in mouse drag event action handler!", ex);
                               }
                           });
                       }
                   };
                   thisComponent.addMouseListener(listener);
                   thisComponent.addMouseMotionListener(listener);
               })
               ._this();
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse move events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the component.
     *  <br><br>
     *
     * @param onMove The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseMove( Action<ComponentMouseEventDelegate<C>> onMove ) {
        NullUtil.nullArgCheck(onMove, "onMove", Action.class);
        return _with( thisComponent -> {
                   thisComponent.addMouseListener(new MouseAdapter() {
                       @Override public void mouseMoved(MouseEvent e) {
                           _runInApp(() -> {
                               try {
                                   onMove.accept(new ComponentMouseEventDelegate<>(thisComponent, e));
                               } catch ( Exception ex ) {
                                   log.error("Error in mouse move event action handler!", ex);
                               }
                           });
                       }
                   });
                   thisComponent.addMouseMotionListener(new MouseMotionAdapter() {
                       @Override public void mouseMoved(MouseEvent e) {
                           _runInApp(() -> {
                               try {
                                   onMove.accept(new ComponentMouseEventDelegate<>(thisComponent, e));
                               } catch ( Exception ex ) {
                                   log.error("Error in mouse move event action handler!", ex);
                               }
                           });
                       }
                   });
               })
               ._this();
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse wheel events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the component.
     *  <br><br>
     *
     * @param onWheel The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseWheelMove( Action<ComponentDelegate<C, MouseWheelEvent>> onWheel ) {
        NullUtil.nullArgCheck(onWheel, "onWheel", Action.class);
        return _with( thisComponent -> {
                   thisComponent.addMouseWheelListener( e -> {
                       _runInApp(() -> {
                           try {
                               onWheel.accept(new ComponentDelegate<>(thisComponent, e));
                           } catch ( Exception ex ) {
                               log.error("Error in mouse wheel event action handler!", ex);
                           }
                       });
                   });
               })
               ._this();
    }

    /**
     *  Use this to register and catch mouse wheel up movement events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the component.
     *  <br><br>
     * @param onWheelUp The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseWheelUp( Action<ComponentDelegate<C, MouseWheelEvent>> onWheelUp ) {
        NullUtil.nullArgCheck(onWheelUp, "onWheelUp", Action.class);
        return _with( thisComponent -> {
                   thisComponent.addMouseWheelListener( e -> {
                       if ( e.getWheelRotation() < 0 )
                           _runInApp(() -> {
                               try {
                                   onWheelUp.accept(new ComponentDelegate<>(thisComponent, e ));
                               } catch ( Exception ex ) {
                                   log.error("Error in mouse wheel up event action handler!", ex);
                               }
                           });
                   });
               })
               ._this();
    }

    /**
     *  Use this to register and catch mouse wheel down movement events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the component.
     *  <br><br>
     * @param onWheelDown The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseWheelDown( Action<ComponentDelegate<C, MouseWheelEvent>> onWheelDown ) {
        NullUtil.nullArgCheck(onWheelDown, "onWheelDown", Action.class);
        return _with( thisComponent -> {
                   thisComponent.addMouseWheelListener( e -> {
                       if ( e.getWheelRotation() > 0 )
                               _runInApp(() -> {
                                   try {
                                       onWheelDown.accept(new ComponentDelegate<>(thisComponent, e));
                                   } catch ( Exception ex ) {
                                       log.error("Error in mouse wheel down event action handler!", ex);
                                   }
                               });
                   });
               })
               ._this();
    }

    /**
     *  The provided lambda will be invoked when the component's size changes.
     *  This will internally translate to a {@link ComponentListener} implementation.
     *  Passing null to this method will cause an exception to be thrown.
     *
     * @param onResize The resize action which will be called when the underlying component changes size.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onResize( Action<ComponentDelegate<C, ComponentEvent>> onResize ) {
        NullUtil.nullArgCheck(onResize, "onResize", Action.class);
        return _with( thisComponent -> {
                   thisComponent.addComponentListener(new ComponentAdapter() {
                       @Override public void componentResized(ComponentEvent e) {
                           _runInApp(()->{
                               try {
                                   onResize.accept(new ComponentDelegate<>(thisComponent, e));
                               } catch ( Exception ex ) {
                                   log.error("Error in resize event action handler!", ex);
                               }
                           });
                       }
                   });
               })
               ._this();
    }

    /**
     *  The provided lambda will be invoked when the component was moved.
     *  This will internally translate to a {@link ComponentListener} implementation.
     *
     * @param onMoved The action lambda which will be executed once the component was moved / its position canged.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMoved( Action<ComponentDelegate<C, ComponentEvent>> onMoved ) {
        NullUtil.nullArgCheck(onMoved, "onMoved", Action.class);
        return _with( thisComponent -> {
                   thisComponent.addComponentListener(new ComponentAdapter() {
                       @Override public void componentMoved(ComponentEvent e) {
                           _runInApp(()->{
                               try {
                                   onMoved.accept(new ComponentDelegate<>(thisComponent, e));
                               } catch ( Exception ex ) {
                                   log.error("Error in move event action handler!", ex);
                               }
                           });
                       }
                   });
               })
               ._this();
    }

    /**
     *  Adds the supplied {@link Action} wrapped in a {@link AncestorListener}
     *  to the component, to receive calls when the wrapped component becomes visible
     *  on the screen. <br>
     *  <p>
     *  Note that this does not correlate 1:1 with the {@link Component#isVisible()} flag,
     *  because a component may also be invisible when it is not part of the component hierarchy
     *  with a visible root component (window) or one of its ancestors (parent components)
     *  is not visible.
     *
     * @param onShown The {@link Action} which gets invoked when the component has been made visible
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onShown( Action<ComponentDelegate<C, AncestorEvent>> onShown ) {
        NullUtil.nullArgCheck(onShown, "onShown", Action.class);
        return _with( thisComponent -> {
                   _prependAncestorListenerTo(thisComponent, new AncestorListener() {
                       @Override public void ancestorAdded(AncestorEvent event) {
                           _runInApp(()->{
                               try {
                                   onShown.accept(new ComponentDelegate<>(thisComponent, event));
                               } catch ( Exception ex ) {
                                   log.error("Error in show event action handler!", ex);
                               }
                           });
                       }
                       @Override public void ancestorRemoved(AncestorEvent event) {}
                       @Override public void ancestorMoved(AncestorEvent event) {}
                   });
               })
               ._this();
    }

    /**
     *  Adds the supplied {@link Action} wrapped in a {@link AncestorListener}
     *  to the component, to receive calls when the wrapped component becomes invisible
     *  on the users screen. <br>
     *  <p>
     *  Note that this does not correlate 1:1 with the {@link Component#isVisible()} flag,
     *  because a component may also be invisible when it is not part of the component hierarchy
     *  with a visible root component (window) or one of its ancestors (parent components)
     *  is not visible.
     *
     * @param onHidden The {@link Action} which gets invoked when the component has been made invisible.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onHidden( Action<ComponentDelegate<C, AncestorEvent>> onHidden ) {
        NullUtil.nullArgCheck(onHidden, "onHidden", Action.class);
        return _with( thisComponent -> {
                    _prependAncestorListenerTo(thisComponent, new AncestorListener() {
                          @Override public void ancestorAdded(AncestorEvent event) {}
                          @Override public void ancestorRemoved(AncestorEvent event) {
                            _runInApp(()->{
                                 try {
                                      onHidden.accept(new ComponentDelegate<>(thisComponent, event));
                                 } catch ( Exception ex ) {
                                      log.error("Error in hide event action handler!", ex);
                                 }
                            });
                          }
                          @Override public void ancestorMoved(AncestorEvent event) {}
                     });
               })
               ._this();
    }

    /**
     *  Adds {@link AncestorListener} to the component so that it will
     *  be called after the previously added ancestor listener was called.<br>
     *  The default behavior of Swing is to call the listeners from the most recently added to the oldest.
     *  This method will prepend the listener to the list of listeners, so that it will be called first.
     * @param component The component to which the listener should be added.
     * @param listener The listener to be added.
     */
    private static void _prependAncestorListenerTo( JComponent component, AncestorListener listener ) {
        java.util.List<AncestorListener> listeners = new ArrayList<>(Arrays.asList(component.getAncestorListeners()));
        for ( AncestorListener l : listeners )
            component.removeAncestorListener(l);

        listeners.add(listener);
        Collections.reverse(listeners);// revert the order of the listeners

        for ( AncestorListener l : listeners )
            component.addAncestorListener(l);
    }

    /**
     * Adds the supplied {@link Action} wrapped in a {@link FocusListener}
     * to the component, to receive those focus events where the wrapped component gains input focus.
     *
     * @param onFocus The {@link Action} which should be executed once the input focus was gained on the wrapped component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onFocusGain( Action<ComponentDelegate<C, ComponentEvent>> onFocus ) {
        NullUtil.nullArgCheck(onFocus, "onFocus", Action.class);
        return _with( thisComponent -> {
                   thisComponent.addFocusListener(new FocusAdapter() {
                       @Override public void focusGained(FocusEvent e) {
                           _runInApp(()->{
                               try {
                                   onFocus.accept(new ComponentDelegate<>(thisComponent, e));
                               } catch ( Exception ex ) {
                                   log.error("Error in focus gain event action handler!", ex);
                               }
                           });
                       }
                   });
               })
               ._this();
    }

    /**
     * Adds the supplied {@link Action} wrapped in a focus listener
     * to receive those focus events where the wrapped component loses input focus.
     *
     * @param onFocus The {@link Action} which should be executed once the input focus was lost on the wrapped component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onFocusLoss( Action<ComponentDelegate<C, ComponentEvent>> onFocus ) {
        NullUtil.nullArgCheck(onFocus, "onFocus", Action.class);
        return _with( thisComponent -> {
                   thisComponent.addFocusListener(new FocusAdapter() {
                       @Override public void focusLost(FocusEvent e) {
                           _runInApp(()->{
                               try {
                                   onFocus.accept(new ComponentDelegate<>(thisComponent, e));
                               } catch ( Exception ex ) {
                                   log.error("Error in focus loss event action handler!", ex);
                               }
                           });
                       }
                   });
               })
               ._this();
    }

    /**
     * Adds the supplied {@link Action} wrapped in a {@link KeyListener}
     * to the component, to receive key events triggered when the wrapped component receives keyboard input.
     * <br><br>
     * @param onKeyPressed The {@link Action} which will be executed once the wrapped component received a key press.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onKeyPress( Action<ComponentDelegate<C, KeyEvent>> onKeyPressed ) {
        NullUtil.nullArgCheck(onKeyPressed, "onKeyPressed", Action.class);
        return _with( thisComponent -> {
                   thisComponent.addKeyListener(new KeyAdapter() {
                       @Override public void keyPressed(KeyEvent e) {
                           _runInApp(()->{
                               try {
                                   onKeyPressed.accept(new ComponentDelegate<>(thisComponent, e));
                               } catch ( Exception ex ) {
                                   log.error("Error in key press event action handler!", ex);
                               }
                           });
                       }
                   });
               })
               ._this();
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
    public final I onPressed( Keyboard.Key key, Action<ComponentDelegate<C, KeyEvent>> onKeyPressed ) {
        NullUtil.nullArgCheck(key, "key", Keyboard.Key.class);
        NullUtil.nullArgCheck(onKeyPressed, "onKeyPressed", Action.class);
        return _with( thisComponent -> {
                   thisComponent.addKeyListener(new KeyAdapter() {
                       @Override public void keyPressed( KeyEvent e ) {
                           if ( e.getKeyCode() == key.code )
                               _runInApp(()->{
                                   try {
                                       onKeyPressed.accept(new ComponentDelegate<>(thisComponent, e));
                                   } catch ( Exception ex ) {
                                       log.error("Error in key press event action handler!", ex);
                                   }
                               });
                       }
                   });
               })
               ._this();
    }

                             /**
     * Adds the supplied {@link Action} wrapped in a {@link KeyListener}
     * to the component, to receive key events triggered when the wrapped component receives keyboard input.
     * <br><br>
     * @param onKeyReleased The {@link Action} which will be executed once the wrapped component received a key release.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyPress(Action)
     */
    public final I onKeyRelease( Action<ComponentDelegate<C, KeyEvent>> onKeyReleased ) {
        NullUtil.nullArgCheck(onKeyReleased, "onKeyReleased", Action.class);
        return _with( thisComponent -> {
                   thisComponent.addKeyListener(new KeyAdapter() {
                       @Override public void keyReleased(KeyEvent e) {
                           _runInApp(()->{
                               try {
                                   onKeyReleased.accept(new ComponentDelegate<>(thisComponent, e));
                               } catch ( Exception ex ) {
                                   log.error("Error in key release event action handler!", ex);
                               }
                           });
                       }
                   });
               })
               ._this();
    }

    /**
     * Adds the supplied {@link Action} wrapped in a {@link KeyListener} to the component,
     * to receive key events triggered when the built component receives a particular
     * keyboard input matching the provided {@link swingtree.input.Keyboard.Key}.
     * <br><br>
     * @param key The {@link swingtree.input.Keyboard.Key} which should be matched to the key event.
     * @param onKeyReleased The {@link Action} which will be executed once the wrapped component received the targeted key release.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyPress(Action)
     * @see #onKeyRelease(Action)
     */
    public final I onRelease( Keyboard.Key key, Action<ComponentDelegate<C, KeyEvent>> onKeyReleased ) {
        NullUtil.nullArgCheck(key, "key", Keyboard.Key.class);
        NullUtil.nullArgCheck(onKeyReleased, "onKeyReleased", Action.class);
        return _with( thisComponent -> {
                   thisComponent.addKeyListener(new KeyAdapter() {
                       @Override public void keyReleased( KeyEvent e ) {
                           if ( e.getKeyCode() == key.code )
                               _runInApp(()->{
                                   try {
                                       onKeyReleased.accept(new ComponentDelegate<>(thisComponent, e));
                                   } catch ( Exception ex ) {
                                       log.error("Error in key release event action handler!", ex);
                                   }
                               });
                       }
                   });
               })
               ._this();
    }

    /**
     * Adds the supplied {@link Action} wrapped in a {@link KeyListener}
     * to the component, to receive key events triggered when the wrapped component receives keyboard input.
     * <br><br>
     * @param onKeyTyped The {@link Action} which will be executed once the wrapped component received a key typed.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyPress(Action)
     * @see #onKeyRelease(Action)
     */
    public final I onKeyTyped( Action<ComponentDelegate<C, KeyEvent>> onKeyTyped ) {
        NullUtil.nullArgCheck(onKeyTyped, "onKeyTyped", Action.class);
        return _with( thisComponent -> {
                   _onKeyTyped(thisComponent, (e, kl) -> {
                       _runInApp(() -> {
                           try {
                               onKeyTyped.accept(new ComponentDelegate<>(thisComponent, e));
                           } catch ( Exception ex ) {
                               log.error("Error in key typed event action handler!", ex);
                           }
                       });
                   });
               })
               ._this();
    }

    /**
     * Adds the supplied {@link Action} wrapped in a {@link KeyListener} to the component,
     * to receive key events triggered when the wrapped component receives a particular
     * keyboard input matching the provided {@link swingtree.input.Keyboard.Key}.
     * <br><br>
     * @param key The {@link swingtree.input.Keyboard.Key} which should be matched to the key event.
     * @param onKeyTyped The {@link Action} which will be executed once the wrapped component received the targeted key typed.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyPress(Action)
     * @see #onKeyRelease(Action)
     * @see #onKeyTyped(Action)
     */
    public final I onTyped( Keyboard.Key key, Action<ComponentDelegate<C, KeyEvent>> onKeyTyped ) {
        NullUtil.nullArgCheck(key, "key", Keyboard.Key.class);
        NullUtil.nullArgCheck(onKeyTyped, "onKeyTyped", Action.class);
        return _with( thisComponent -> {
                   _onKeyTyped(thisComponent, (e, kl) -> {
                       if ( e.getKeyCode() == key.code )
                           _runInApp(()->{
                               try {
                                   onKeyTyped.accept(new ComponentDelegate<>(thisComponent, e));
                               } catch ( Exception ex ) {
                                   log.error("Error in key typed event action handler!", ex);
                               }
                           });
                   });
               })
               ._this();
    }

    private void _onKeyTyped(C component, BiConsumer<KeyEvent, KeyAdapter> action ) {
        component.addKeyListener(new KeyAdapter() {
            private @Nullable KeyEvent lastEvent;

            @Override
            public void keyPressed(KeyEvent e) {
                lastEvent = e;
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if ( lastEvent != null && lastEvent.getKeyCode() == e.getKeyCode() ) {
                    action.accept(lastEvent, this);
                    lastEvent = null;
                }
            }
        });
    }

    /**
     *  Adds the supplied {@link Action} wrapped in a {@link KeyListener} to the component,
     *  to receive key events triggered when the wrapped component receives a particular
     *  keyboard input matching the provided character. <br>
     *  This method is a logical extension of the {@link #onTyped(Keyboard.Key, Action)} method,
     *  with the difference that it listens for any character instead of a specific key code.
     *  This also works with special characters which are typed using the combination
     *  of multiple keys (e.g. shift + number keys).
     *  <br><br>
     * @param character The character to listen for.
     * @param onKeyTyped The action to execute when the character is typed.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyTyped(Action)
     * @see #onTyped(Keyboard.Key, Action)
     */
    public final I onTyped( char character, Action<ComponentDelegate<C, KeyEvent>> onKeyTyped ) {
        NullUtil.nullArgCheck(onKeyTyped, "onKeyTyped", Action.class);
        return _with( thisComponent -> {
                   _onCharTyped(thisComponent, (e, kl) -> {
                       if ( e.getKeyChar() == character )
                           _runInApp(()->{
                               try {
                                   onKeyTyped.accept(new ComponentDelegate<>(thisComponent, e));
                               } catch ( Exception ex ) {
                                   log.error("Error in key typed event action handler!", ex);
                               }
                           });
                   });
               })
               ._this();
    }

    /**
     *  Adds the supplied {@link Action} wrapped in a {@link KeyListener} to the component,
     *  to receive key events triggered when the wrapped component receives a particular
     *  keyboard input matching the provided character. <br>
     *  This method is a logical extension of the {@link #onTyped(Keyboard.Key, Action)} method,
     *  with the difference that it listens for any character instead of a specific key code.
     *  This also works with special characters which are typed using the combination
     *  of multiple keys (e.g. shift + number keys).
     *  <br><br>
     * @param onKeyTyped The action to execute when the character is typed.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyTyped(Action)
     * @see #onKeyTyped(Action)
     * @see #onTyped(Keyboard.Key, Action)
     */
    public final I onCharTyped( Action<ComponentDelegate<C, KeyEvent>> onKeyTyped ) {
        NullUtil.nullArgCheck(onKeyTyped, "onKeyTyped", Action.class);
        return _with( thisComponent -> {
                   _onCharTyped(thisComponent, (e, kl) -> {
                       _runInApp(()->{
                           try {
                               onKeyTyped.accept(new ComponentDelegate<>(thisComponent, e));
                           } catch ( Exception ex ) {
                               log.error("Error in key typed event action handler!", ex);
                           }
                       });
                   });
               })
               ._this();
    }

    private void _onCharTyped( C component, BiConsumer<KeyEvent, KeyAdapter> action ) {
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                action.accept(e, this);
            }
        });
    }

    /**
     *  Exposes a functional {@link Configurator} API for turning the component into a drag source site
     *  with the given configuration. This method is a convenience method which internally
     *  creates and configures an AWT native {@link DragSource} instance for the component.
     *  <br><br>
     *  Here an example:
     *  <pre>{@code
     *      UI.label("Drag me!")
     *      .withDragAway( conf -> conf
     *          .enabled(true)
     *          .opacity(0.25)
     *          .payload("IT WORKS")
     *          .onDragMove( it -> {...})
     *      )
     *  }</pre>
     *  The example above creates a draggable label which will be dragged with an opacity of 0.25
     *  and carries the payload "IT WORKS" when dragged. The drag move event handler
     *  is attached to the drag source site and will be called when the label is dragged.<br>
     *  <p>
     *  If you want to create a drop site instead of a drag source site, use {@link #withDropSite(Configurator)}.
     *
     * @param configurator The {@link Configurator} which allows you to configure the drag source site
     *                     by receiving a {@link DragAwayComponentConf} instance.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withDragAway( Configurator<DragAwayComponentConf<C>> configurator ) {
        NullUtil.nullArgCheck(configurator, "configurator", Configurator.class);
        return _with( thisComponent -> {
                    ComponentExtension.from(thisComponent).addDragAwayConf(mousePosition -> {
                        DragAwayComponentConf<C> conf = DragAwayComponentConf.of(thisComponent, mousePosition);
                        try {
                            return configurator.configure(conf);
                        } catch (Exception e) {
                            log.error("Failed to configure drag away!", e);
                        }
                        return conf;
                    });
               })
               ._this();
    }

    /**
     *  Exposes a functional {@link Configurator} API for turning the component into a drag drop
     *  receiver site with the given configuration.
     *  This method is a convenience method which internally
     *  creates and configures an AWT native {@link DropTarget} instance for the component.
     *  <br><br>
     *  Here an example:
     *  <pre>{@code
     *      UI.label("Drop here!")
     *      .withDropSite( conf -> conf
     *          .onDragOver( it -> {
     *              it.animateFor(1, TimeUnit.SECONDS, status -> {
     *                  double r = 30 * status.fadeIn() * it.getScale();
     *                  double x = it.getEvent().getLocation().x - r / 2.0;
     *                  double y = it.getEvent().getLocation().y - r / 2.0;
     *                  it.paint(status, g -> {
     *                      g.setColor(new Color(0f, 1f, 1f, (float) status.fadeOut()));
     *                      g.fillOval((int) x, (int) y, (int) r, (int) r);
     *                  });
     *              });
     *          })
     *          .onDrop(it -> {
     *              it.animateFor(2, TimeUnit.SECONDS, status -> {
     *                  double r = 480 * status.fadeIn() * it.getScale();
     *                  double x = it.getEvent().getLocation().x - r / 2.0;
     *                  double y = it.getEvent().getLocation().y - r / 2.0;
     *                  it.paint(status, g -> {
     *                      g.setColor(UI.color(0.5+status.fadeOut()/2, status.fadeOut(), status.fadeIn(), status.fadeOut()));
     *                      g.fillOval((int) x, (int) y, (int) r, (int) r);
     *                  });
     *              });
     *          })
     *      )
     *  }</pre>
     *  The example above creates a drop site label which will animate an expanding circle when a drag is over it
     *  and another circle when a drop is performed on it. The drag over event handler
     *  is attached to the drop site and will be called when the label is dragged over it.
     *  <p>
     *  If you want to create a drag source site instead of a drop site, use {@link #withDragAway(Configurator)}.
     *
     * @param configurator The {@link Configurator} which allows you to configure the drop site
     *                     by receiving a {@link DragDropComponentConf} instance.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withDropSite( Configurator<DragDropComponentConf<C>> configurator ) {
        NullUtil.nullArgCheck(configurator, "configurator", Configurator.class);
        return _with( thisComponent -> {
                    _runInApp(() -> {
                        try {
                            DragDropComponentConf<C> conf = configurator.configure(DragDropComponentConf.of(thisComponent));
                            DropTarget target = conf.toNewDropTarget();
                            thisComponent.setDropTarget(target);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to configure drop site!", e);
                        }
                    });
               })
               ._this();
    }

    /**
     *  Allows you to cause an effect inside your UI when an observable event is fired.
     *  The provided {@link Action} event handler will be called
     *  on the UI thread when the {@link Observable} event is fired, irrespective of
     *  what thread the {@link Observable} event is fired on.
     *  However, it is expected that the {@link Observable} event is fired on the application thread
     *  and <b>the concrete implementation of the {@link Observable} is intended to
     *  be part of your view model</b>.
     *  <br><br>
     *  Here an example:
     *  <pre>{@code
     *  UI.label("I have a color animation!")
     *  .on(viewModel.someEvent(), it ->
     *    it.animateFor(3, TimeUnit.SECONDS, status -> {
     *      double r = status.progress();
     *      double g = 1 - status.progress();
     *      double b = status.pulse();
     *      it.setBackgroundColor(r, g, b);
     *    })
     *  )
     *  }</pre>
     *
     * @param observableEvent The {@link Observable} event to which the {@link Action} should be attached
     *                        and called on the UI thread when the event is fired in the view model.
     * @param action The {@link Action} which is invoked by the UI thread after the {@link Observable} event was fired
     *               by the business logic of the view model.
     * @return This very instance, which enables builder-style method chaining.
     * @param <E> The type of the {@link Observable} event.
     */
    public final <E extends Observable> I onView( E observableEvent, Action<ComponentDelegate<C, E>> action ) {
        NullUtil.nullArgCheck(observableEvent, "observableEvent", Observable.class);
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                   observableEvent.subscribe(() -> {
                       _runInUI(() -> {
                           try {
                               action.accept(new ComponentDelegate<>(thisComponent, observableEvent));
                           } catch ( Exception ex ) {
                               log.error("Error in view event action handler!", ex);
                           }
                       });
                   });
               })
               ._this();
    }

    /**
     *  Use this to attach a component {@link Action} event handler to a functionally supplied
     *  {@link Observable} event in order to implement a custom user event system.
     *  The supplied {@link Action} is executed on the application thread when the {@link Observable} event is fired and
     *  irrespective of the thread that {@link Observable} fired the event. <br>
     *  The {@link Action} is expected to perform an effect on the view model or the application state,
     *  <b>but not on the UI directly</b>. <br>
     *  (see {@link #onView(Observable, Action)} if you want your view model to affect the UI through an observable event)
     *  <br><br>
     *  Consider the following example:
     *  <pre>{@code
     *      UI.label("")
     *      .on(CustomEventSystem.touchGesture(), it -> ..some App update.. )
     *  }</pre>
     *  In this example we use an imaginary {@code CustomEventSystem} to register a touch gesture event handler
     *  which will be called on the application thread when the touch gesture event is fired.
     *  Although neither Swing nor SwingTree have a touch gesture event system, this example illustrates
     *  how one could easily integrate a custom event system into SwingTree UIs.
     *  <br><br>
     *  <b>
     *      Note that the provided {@link Observable} event is NOT expected to be part of the view model,
     *      but rather part of a custom event system that captures user input or other input
     *      which is not directly related to the business logic of the view model.
     *  </b>
     *
     * @param observableEvent The {@link Observable} event to which the {@link Action} should be attached.
     * @param action The {@link Action} which is invoked by the application thread after the {@link Observable} event was fired.
     * @return This very instance, which enables builder-style method chaining.
     * @param <E> The type of the {@link Observable} event.
     * @see #onView(Observable, Action) for a similar method which is intended to be used with view model events.
     */
    public final <E extends Observable> I on( E observableEvent, Action<ComponentDelegate<C, E>> action ) {
        NullUtil.nullArgCheck(observableEvent, "observableEvent", Observable.class);
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                   observableEvent.subscribe(() -> {
                       _runInApp(() -> {
                           try {
                               action.accept(new ComponentDelegate<>(thisComponent, observableEvent));
                           } catch ( Exception ex ) {
                               log.error("Error in custom event action handler!", ex);
                           }
                       });
                   });
               })
               ._this();
    }

    /**
     *  This is a logical extension of the {@link #on(Observable, Action)} method.
     *  Use this to attach a component {@link Action} event handler to a functionally supplied
     *  {@link Observable} event.
     *  The {@link Action} will be called on the application thread when the {@link Observable} event
     *  is fired, irrespective of the thread that fired the {@link Observable} event.
     *  The {@link Action} is expected to perform an effect on the view model or the application state,
     *  <b>but not on the UI directly</b>. <br>
     *  (see {@link #onView(Observable, Action)} if you want your view model to affect the UI through an observable event)
     *  <br><br>
     *  Consider the following example:
     *  <pre>{@code
     *      UI.label("")
     *      .on(c -> CustomEventSystem.touchGesture(c), it -> ..some App update.. )
     *  }</pre>
     *  Which may also be written as:
     *  <pre>{@code
     *    UI.label("")
     *    .on(CustomEventSystem::touchGesture, it -> ..some App update.. )
     * }</pre>
     *  In this example we use an imaginary {@code CustomEventSystem} to register a component specific
     *  touch gesture event handler which will be called on the application thread when the touch gesture event is fired.
     *  Although neither Swing nor SwingTree have a touch gesture event system, this example illustrates
     *  how one could easily integrate a custom event system into SwingTree UIs.
     *  <br><br>
     *  <b>
     *      Note that the {@link Observable} event supplied by the function
     *      is NOT expected to be part of the view model,
     *      but rather be part of a custom event system that captures user input or other input
     *      which is not directly related to the business logic of the view model.
     *  </b>
     *
     * @param eventSource The {@link Observable} event to which the {@link Action} should be attached.
     * @param action The {@link Action} which is invoked by the application thread after the {@link Observable} event was fired.
     * @return This very instance, which enables builder-style method chaining.
     * @param <E> The type of the {@link Observable} event.
     * @see #onView(Observable, Action) for a similar method which is intended to be used with view model events.
     */
    public final <E extends Observable> I on( Function<C, E> eventSource, Action<ComponentDelegate<C, E>> action ) {
        NullUtil.nullArgCheck(eventSource, "eventSource", Function.class);
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                   E observableEvent = eventSource.apply(thisComponent);
                   observableEvent.subscribe(() -> {
                       _runInApp(() -> {
                           try {
                               action.accept(new ComponentDelegate<>(thisComponent, observableEvent));
                           } catch ( Exception ex ) {
                               log.error("Error in custom event action handler!", ex);
                           }
                       });
                   });
               })
               ._this();
    }

    /**
     *  Use this to register periodic update actions which should be called
     *  based on the provided {@code delay}! <br>
     *  The following example produces a label which will display the current date.
     *  <pre>{@code
     *      UI.label("")
     *      .doUpdates( 100, it -> it.getComponent().setText(new Date().toString()) )
     *  }</pre>
     *
     * @param delay The delay in milliseconds between calling the provided {@link Action}.
     * @param onUpdate The {@link Action} which should be called periodically.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I doUpdates( int delay, Action<ComponentDelegate<C, ActionEvent>> onUpdate ) {
        NullUtil.nullArgCheck(onUpdate, "onUpdate", Action.class);
        return _with( thisComponent -> {
                   Timer timer = new Timer(delay, e -> {
                       try {
                           onUpdate.accept(new ComponentDelegate<>(thisComponent, e));
                       } catch ( Exception ex ) {
                           log.error("Error in update action handler!", ex);
                       }
                   });
                   {
                       java.util.List<Timer> timers = (java.util.List<Timer>) thisComponent.getClientProperty(_TIMERS_KEY);
                       if ( timers == null ) {
                           timers = new ArrayList<>();
                           thisComponent.putClientProperty(_TIMERS_KEY, timers);
                       }
                       timers.add(timer);
                   }
                   timer.start();
               })
               ._this();
    }

    @Override
    protected void _addComponentTo(
        C                       thisComponent,
        JComponent              addedComponent,
        @Nullable AddConstraint constraints
    ) {
        NullUtil.nullArgCheck(addedComponent, "component", JComponent.class);
        if ( constraints == null )
            thisComponent.add( addedComponent );
        else
            thisComponent.add( addedComponent, constraints.toConstraintForLayoutManager() );
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
    public final <T extends JComponent> I add(AddConstraint attr, UIForAnySwing<?, T> builder ) {
        return this.add(attr, new UIForAnySwing[]{builder});
    }

    /**
     *  Use this to nest builder types into this builder to effectively plug the wrapped {@link JComponent}s 
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument represents layout attributes/constraints which will
     *  be passed to the {@link LayoutManager} of the underlying {@link JComponent}.
     *  through the {@link JComponent#add(Component, Object)} method.
     *  <br><br>
     *  This may look like this:
     *  <pre>{@code
     *    UI.panel()
     *    .add("wrap", UI.label("A"), UI.label("B"))
     *    .add("grow", UI.label("C"), UI.label("D"))
     *  }</pre>
     *  Note that the first argument, "wrap" and "grow" in this case, are
     *  used as layout constraints for all the {@link JComponent}s which are added
     *  in the subsequent arguments of a single call to this method.
     *
     *
     * @param attr The additional mig-layout information which should be passed to the UI tree.
     * @param builders An array of builders for a corresponding number of {@link JComponent} 
     *                  type which ought to be added to the wrapped component type of this builder.
     * @param <B> The builder type parameter, a subtype of {@link UIForAnySwing}.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    public final <B extends UIForAnySwing<?, ?>> I add( String attr, B... builders ) {
        return _with( thisComponent -> {
                   _addBuildersTo(thisComponent, ()->attr, builders);
               })
               ._this();
    }

    private void _addBuildersTo(C thisComponent, AddConstraint attr, UIForAnySwing<?, ?>... builders ) {
        LayoutManager layout = thisComponent.getLayout();
        Object constraints = attr.toConstraintForLayoutManager();
        if ( _isBorderLayout(constraints) && !(layout instanceof BorderLayout) ) {
            if ( layout instanceof MigLayout )
                log.warn("Layout ambiguity detected! Border layout constraint cannot be added to 'MigLayout'.");
            thisComponent.setLayout(new BorderLayout()); // The UI Maker tries to fill in the blanks!
        }
        for ( UIForAnySwing<?, ?> b : builders )
            _addBuilderTo(thisComponent, b, attr);
    }

    /**
     *  Use this to nest builder types into this builder to effectively plug the wrapped {@link JComponent}s
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument will be passed to the {@link LayoutManager}
     *  of the underlying {@link JComponent} to serve as layout constraints
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
    public final <B extends UIForAnySwing<?, ?>> I add(AddConstraint attr, B... builders ) {
        Objects.requireNonNull(attr, "attr");
        Objects.requireNonNull(builders, "builders");
        return _with( thisComponent -> {
                    _addBuildersTo(thisComponent, attr, builders);
                })
                ._this();
    }

    /**
     *  Use this to nest builder types into this builder to effectively plug the {@link JComponent}s
     *  wrapped by the provided builders
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument represents placement constraints for the provided components which will
     *  be passed to the {@link MigLayout} of the underlying {@link JComponent}
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
    public final <B extends UIForAnySwing<?, ?>> I add( CC attr, B... builders ) {
        Objects.requireNonNull(attr, "attr");
        Objects.requireNonNull(builders, "builders");
        return _with( thisComponent -> {
                   LayoutManager layout = thisComponent.getLayout();
                   if ( !(layout instanceof MigLayout) )
                       log.warn("Layout ambiguity detected! Mig layout constraint cannot be added to '{}'.", layout.getClass().getSimpleName());

                   for ( UIForAnySwing<?, ?> b : builders )
                       _addBuilderTo(thisComponent, b, ()->attr);
               })
               ._this();
    }

    /**
     *  Use this to nest {@link JComponent} types into this builder to effectively plug the provided {@link JComponent}s
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument represents layout attributes/constraints which will
     *  be applied to the subsequently provided {@link JComponent} types.
     *  <br><br>
     *  This may look like this:
     *  <pre>{@code
     *    UI.panel()
     *    .add("wrap", new JLabel("A"), new JLabel("B"))
     *    .add("grow", new JLabel("C"), new JLabel("D"))
     *  }</pre>
     *  Note that the first argument, "wrap" and "grow" in this case, are
     *  used as layout constraints for all the {@link JComponent}s which are added
     *  in the subsequent arguments of a single call to this method.
     *
     * @param attr The additional layout information which should be passed to the UI tree.
     * @param components A {@link JComponent}s array which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     * @param <E> The type of the {@link JComponent} which is wrapped by the provided builder.
     */
    @SafeVarargs
    public final <E extends JComponent> I add( String attr, E... components ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        NullUtil.nullArgCheck(components, "components", JComponent[].class);
        return _with( thisComponent -> {
                   _addComponentsTo( thisComponent, attr, components );
               })
               ._this();
    }

    @SafeVarargs
    private final <E extends JComponent> void _addComponentsTo( C thisComponent, String attr, E... components ) {
        for ( E component : components ) {
            NullUtil.nullArgCheck(component, "component", JComponent.class);
            _addBuildersTo( thisComponent, ()->attr, new UIForSwing[]{UI.of(component)} );
        }
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
     * @param <E> The type of the {@link JComponent} which is wrapped by the provided builder.
     */
    @SafeVarargs
    public final <E extends JComponent> I add( AddConstraint attr, E... components ) {
        return _with( thisComponent -> {
                    _addComponentsTo( thisComponent, attr, components );
                })
                ._this();
    }

    @SafeVarargs
    private final <E extends JComponent> void _addComponentsTo( C thisComponent, AddConstraint attr, E... components ) {
        for ( E component : components ) {
            NullUtil.nullArgCheck(component, "component", JComponent.class);
            _addBuildersTo( thisComponent, attr, new UIForSwing[]{UI.of(component)} );
        }
    }

    /**
     *  This allows you to dynamically generate a view for the item of a property (usually a property
     *  holding a sub-view model) and automatically regenerate the view when the property changes.
     *  The {@link ViewSupplier} lambda passed to this method will receive the value of the property
     *  and is then expected to return a {@link JComponent} instance which will be added to the
     *  wrapped {@link JComponent} type of this builder.
     *
     * @param viewable A {@link sprouts.Val} property holding null or any other type of value,
     *                 preferably a view model instance.
     * @param viewSupplier A {@link ViewSupplier} instance which will be used to generate the view for the value held by the property.
     * @return This very instance, which enables builder-style method chaining.
     * @param <M> The type of the value held by the {@link Val} property.
     */
    public final <M> I add( Val<M> viewable, ViewSupplier<M> viewSupplier ) {
        NullUtil.nullArgCheck(viewable, "viewable", Val.class);
        return _with( thisComponent -> {
                   _addViewablePropTo(thisComponent, viewable, null, viewSupplier);
               })
               ._this();
    }

    /**
     *  This allows you to dynamically generate a view for the item of a property (usually a property
     *  holding a sub-view model) and automatically regenerate the view when the property changes.
     *  The {@link ViewSupplier} lambda passed to this method will receive the value of the property
     *  and is then expected to return a {@link JComponent} instance which will be added to the
     *  wrapped {@link JComponent} type of this builder.
     *
     * @param attr The layout information which should be used as layout constraints for the generated view.
     * @param viewable A {@link sprouts.Val} property holding null or any other type of value,
     *                 preferably a view model instance.
     * @param viewSupplier A {@link ViewSupplier} instance which will be used to generate the view for the value held by the property.
     * @return This very instance, which enables builder-style method chaining.
     * @param <M> The type of the value held by the {@link Val} property.
     */
    public final <M> I add( String attr, Val<M> viewable, ViewSupplier<M> viewSupplier ) {
        NullUtil.nullArgCheck(attr, "attr", Object.class);
        NullUtil.nullArgCheck(viewable, "viewable", Val.class);
        return _with( thisComponent -> {
            _addViewablePropTo(thisComponent, viewable, ()->attr, viewSupplier);
        })
                ._this();
    }

    /**
     *  This allows you to dynamically generate a view for the item of a property (usually a property
     *  holding a sub-view model) and automatically regenerate the view when the property changes.
     *  The {@link ViewSupplier} lambda passed to this method will receive the value of the property
     *  and is then expected to return a {@link JComponent} instance which will be added to the
     *  wrapped {@link JComponent} type of this builder.
     *
     * @param attr The layout information which should be used as layout constraints for the generated view.
     * @param viewable A {@link sprouts.Val} property holding null or any other type of value,
     *                 preferably a view model instance.
     * @param viewSupplier A {@link ViewSupplier} instance which will be used to generate the view for the value held by the property.
     * @return This very instance, which enables builder-style method chaining.
     * @param <M> The type of the value held by the {@link Val} property.
     */
    public final <M> I add( AddConstraint attr, Val<M> viewable, ViewSupplier<M> viewSupplier ) {
        return  _with( thisComponent -> {
            _addViewablePropTo(thisComponent, viewable, attr, viewSupplier);
        })
                ._this();
    }

    /**
     *  This allows you to dynamically generate views for the items in a {@link Vals} property list
     *  and automatically regenerate the view when any of the items change.
     *  The type of item can be anything, but it is usually a view model instance.
     *  The {@link ViewSupplier} lambda passed to this method will receive the value of the property
     *  and is then expected to return a {@link JComponent} instance which will be added to the
     *  wrapped {@link JComponent} type of this builder.
     *
     * @param viewables A {@link sprouts.Vals} list of items of any type but preferably view model instances.
     * @param viewSupplier A {@link ViewSupplier} instance which will be used to generate the view for each item in the list.
     *               The views will be added to the component wrapped by this builder instance.
     * @return This very instance, which enables builder-style method chaining.
     * @param <M> The type of the items in the {@link Vals} list.
     */
    public final <M> I addAll( Vals<M> viewables, ViewSupplier<M> viewSupplier ) {
        NullUtil.nullArgCheck(viewables, "viewables", Vals.class);
        Objects.requireNonNull(viewSupplier, "viewSupplier");
        return _with( thisComponent -> {
                    _addViewableProps( viewables, null, viewSupplier, thisComponent );
                })
                ._this();
    }

    /**
     *  This allows you to dynamically generate views for the items in a {@link Vals} property list
     *  and automatically regenerate the view when any of the items change.
     *  The type of item can be anything, but it is usually a view model instance.
     *  The {@link ViewSupplier} lambda passed to this method will receive the value of the property
     *  and is then expected to return a {@link JComponent} instance which will be added to the
     *  wrapped {@link JComponent} type of this builder.
     *
     * @param attr The layout information which should be used as layout constraints for the generated views.
     * @param viewables A {@link sprouts.Vals} list of items of any type but preferably view model instances.
     * @param viewSupplier A {@link ViewSupplier} instance which will be used to generate the view for each item in the list.
     *               The views will be added to the component wrapped by this builder instance.
     * @return This very instance, which enables builder-style method chaining.
     * @param <M> The type of the items in the {@link Vals} list.
     */
    public final <M> I addAll( String attr, Vals<M> viewables, ViewSupplier<M> viewSupplier ) {
        NullUtil.nullArgCheck(attr, "attr", Object.class);
        NullUtil.nullArgCheck(viewables, "viewables", Vals.class);
        return _with( thisComponent -> {
                    _addViewableProps( viewables, ()->attr, viewSupplier, thisComponent );
                })
                ._this();
    }

    /**
     *  This allows you to dynamically generate views for the items in a {@link Vals} property list
     *  and automatically regenerate the view when any of the items change.
     *  The type of item can be anything, but it is usually a view model instance.
     *  The {@link ViewSupplier} lambda passed to this method will receive the value of the property
     *  and is then expected to return a {@link JComponent} instance which will be added to the
     *  wrapped {@link JComponent} type of this builder.
     *
     * @param attr The layout information which should be used as layout constraints for the generated views.
     * @param viewables A {@link sprouts.Vals} list of items of any type but preferably view model instances.
     * @param viewSupplier A {@link ViewSupplier} instance which will be used to generate the view for each item in the list.
     *               The views will be added to the component wrapped by this builder instance.
     * @return This very instance, which enables builder-style method chaining.
     * @param <M> The type of the items in the {@link Vals} list.
     */
    public final <M> I addAll( AddConstraint attr, Vals<M> viewables, ViewSupplier<M> viewSupplier ) {
        return _with( thisComponent -> {
                    _addViewableProps( viewables, attr, viewSupplier, thisComponent );
                })
                ._this();
    }

    public final <M> I addAll( Val<Tuple<M>> viewables, ViewSupplier<M> viewSupplier ) {
        NullUtil.nullArgCheck(viewables, "viewables", Vals.class);
        Objects.requireNonNull(viewSupplier, "viewSupplier");
        return _with( thisComponent -> {
                    _addViewableProps( viewables, null, viewSupplier, thisComponent );
                })
                ._this();
    }

    public final <M> I addAll( String attr, Val<Tuple<M>> viewables, ViewSupplier<M> viewSupplier ) {
        NullUtil.nullArgCheck(attr, "attr", Object.class);
        NullUtil.nullArgCheck(viewables, "viewables", Vals.class);
        return _with( thisComponent -> {
                    _addViewableProps( viewables, ()->attr, viewSupplier, thisComponent );
                })
                ._this();
    }

    public final <M> I addAll( AddConstraint attr, Val<Tuple<M>> viewables, ViewSupplier<M> viewSupplier ) {
        return _with( thisComponent -> {
                    _addViewableProps( viewables, attr, viewSupplier, thisComponent );
                })
                ._this();
    }

    protected <M> void _addViewableProps( Vals<M> models, @Nullable AddConstraint attr, ViewSupplier<M> viewSupplier, C thisComponent ) {
        _onShow( models, thisComponent, (innerComponent, delegate) -> {
            // we simply redo all the components.
            Vals<M> newValues = delegate.newValues();
            Vals<M> oldValues = delegate.oldValues();

            switch ( delegate.changeType() ) {
                case SET:
                    for ( int i = 0; i < newValues.size(); i++ ) {
                        int position = i + delegate.index();
                        _updateComponentAt(position, newValues.at(i).get(), viewSupplier, attr, innerComponent);
                    }
                    break;
                case ADD:
                    if ( delegate.index() < 0 && newValues.any(Val::isEmpty) ) {
                        // This is basically a add all operation, so we clear the components first.
                        _clearComponentsOf(innerComponent);
                        // and then we add all the components.
                        for ( int i = 0; i < delegate.currentValues().size(); i++ )
                            _addComponentAt( i, delegate.currentValues().at(i).orElseNull(), viewSupplier, attr, innerComponent );
                    }
                    else {
                        for ( int i = 0; i < newValues.size(); i++ ) {
                            int position = i + delegate.index();
                            _addComponentAt(position, newValues.at(i).orElseNull(), viewSupplier, attr, innerComponent);
                        }
                    }
                    break;
                case REMOVE:
                    for ( int i = oldValues.size() - 1; i >= 0; i-- ) {
                        int position = i + delegate.index();
                        _removeComponentAt(position, innerComponent);
                    }
                    break;
                case CLEAR: _clearComponentsOf(innerComponent); break;
                case NONE: break;
                default:
                    log.error("Unknown change type: {}", delegate.changeType(), new Throwable());
                    // We do a simple rebuild:
                    Vals<M> currentValues = delegate.currentValues();
                    _clearComponentsOf(innerComponent);
                    for ( int i = 0; i < currentValues.size(); i++ )
                        _addComponentAt( i, currentValues.at(i).orElseNull(), viewSupplier, attr, innerComponent );
            }
            if ( innerComponent.getComponentCount() != delegate.currentValues().size() )
                log.warn(
                        "Broken binding to view model list detected! \n" +
                        "UI sub-component count '"+innerComponent.getComponentCount()+"' " +
                        "does not match viewable models list of size '"+delegate.currentValues().size()+"'. \n" +
                        "A possible cause for this is that components " +
                        "were " + ( innerComponent.getComponentCount() > delegate.currentValues().size() ? "added" : "removed" ) + " " +
                        "to this '" + innerComponent + "' \ndirectly, instead of through the property list binding. \n" +
                        "However, this could also be a bug in the UI framework.",
                        new Throwable()
                    );
        });
        models.forEach( v -> {
            UIForAnySwing<?, ?> view = null;
            try {
                view = viewSupplier.createViewFor(v);
            } catch ( Exception e ) {
                log.error("Error while creating view for '"+v+"'.", e);
            }
            if ( view == null )
                view = UI.box(); // We add a dummy component to the list of children.

            if ( attr == null )
                _addBuildersTo( thisComponent, view );
            else
                _addBuildersTo( thisComponent, attr, view );
        });
    }


    protected <M> void _addViewableProps( Val<Tuple<M>> models, @Nullable AddConstraint attr, ViewSupplier<M> viewSupplier, C thisComponent ) {
        AtomicReference<@Nullable TupleDiff> lastDiffRef = new AtomicReference<>(null);
        if (models.get() instanceof TupleDiffOwner)
            lastDiffRef.set(((TupleDiffOwner)models.get()).differenceFromPrevious().orElse(null));
        _onShow( models, thisComponent, (c, tupleOfModels) -> {
            TupleDiff diff = null;
            TupleDiff lastDiff = lastDiffRef.get();
            if (tupleOfModels instanceof TupleDiffOwner)
                diff = ((TupleDiffOwner)tupleOfModels).differenceFromPrevious().orElse(null);
            lastDiffRef.set(diff);

            if ( diff == null || ( lastDiff == null || !diff.isDirectSuccessorOf(lastDiff) ) ) {
                _clearComponentsOf(c);
                _addAllFromTuple(tupleOfModels, attr, viewSupplier, c);
            } else {
                int index = diff.index().orElse(-1);
                int count = diff.size();
                if ( index < 0 ) {
                    // We do a simple re-build
                    _clearComponentsOf(c);
                    _addAllFromTuple(tupleOfModels, attr, viewSupplier, c);
                } else {
                    switch (diff.change()) {
                        case SET:
                            for (int i = index; i < (index + count); i++)
                                _updateComponentAt(i, tupleOfModels.get(i), viewSupplier, attr, c);
                            break;
                        case ADD:
                            for (int i = index; i < (index + count); i++)
                                _addComponentAt(i, tupleOfModels.get(i), viewSupplier, attr, c);
                            break;
                        case REMOVE:
                            for (int i = (index + count - 1); i >= index; i--)
                                _removeComponentAt(i, c);
                            break;
                        case RETAIN: // Only keep the elements in the range.
                            // Remove trailing components:
                            for (int i = (c.getComponentCount() - 1); i >= (index + count); i--)
                                _removeComponentAt(i, c);
                            // Remove leading components:
                            for (int i = (index - 1); i >= 0; i--)
                                _removeComponentAt(i, c);
                            break;
                        case CLEAR:
                            _clearComponentsOf(c);
                            break;
                        case NONE:
                            break;
                        default:
                            log.error("Unknown change type: {}", diff.change(), new Throwable());
                            // We do a simple rebuild:
                            _clearComponentsOf(c);
                            for (int i = 0; i < tupleOfModels.size(); i++)
                                _addComponentAt(i, tupleOfModels.get(i), viewSupplier, attr, c);
                    }
                }
            }
        });
        Tuple<M> tupleOfModels = models.get();
        _addAllFromTuple(tupleOfModels, attr, viewSupplier, thisComponent);
    }

    private <M> void _addAllFromTuple( Tuple<M> tupleOfModels, @Nullable AddConstraint attr, ViewSupplier<M> viewSupplier, C thisComponent ) {
        for ( int i = 0; i < tupleOfModels.size(); i++ ) {
            UIForAnySwing<?, ?> view = null;
            try {
                view = viewSupplier.createViewFor(tupleOfModels.get(i));
            } catch ( Exception e ) {
                log.error("Error while creating view for '"+tupleOfModels.get(i)+"'.", e);
            }
            if ( view == null )
                view = UI.box(); // We add a dummy component to the list of children.

            if ( attr == null )
                _addBuildersTo( thisComponent, view );
            else
                _addBuildersTo( thisComponent, attr, view );
        }
    }

    private <M> void _addViewablePropTo(
        C thisComponent, Val<M> viewable, @Nullable AddConstraint attr, ViewSupplier<M> viewSupplier
    ) {
        // First we remember the index of the component which will be provided by the viewable dynamically.
        final int index = thisComponent.getComponentCount();
        // Then we add the component provided by the viewable to the list of children.
        if ( attr == null ) {
            if ( viewable.isPresent() ) {
                UIForAnySwing<?, ?> view = null;
                try {
                    view = viewSupplier.createViewFor(viewable.get());
                } catch ( Exception e ) {
                    log.error("Error while creating view for '{}'.", viewable.orElseNull(), e);
                }
                if ( view == null )
                    view = UI.box(); // We add a dummy component to the list of children.

                _addBuildersTo(thisComponent, view);
            } else
                _addComponentsTo(thisComponent, new JPanel()); // We add a dummy component to the list of children.
        } else {
            if ( viewable.isPresent() ) {
                UIForAnySwing<?, ?> view = null;
                try {
                    view = viewSupplier.createViewFor(viewable.get());
                } catch ( Exception e ) {
                    log.error("Error while creating view for '{}'.", viewable.orElseNull(), e);
                }
                if ( view == null )
                    view = UI.box(); // We add a dummy component to the list of children.
                _addBuildersTo(thisComponent, attr, view);
            } else
                _addComponentsTo(thisComponent, attr, new JPanel()); // We add a dummy component to the list of children.
        }
        // Finally we add a listener to the viewable which will update the component when the viewable changes.
        _onShow( viewable, thisComponent, (c,v) -> _updateComponentAt(index, v, viewSupplier, attr, c) );
    }

    private <M> void _updateComponentAt(
        int index, @Nullable M v, ViewSupplier<M> viewSupplier, @Nullable AddConstraint attr, C c
    ) {
        JComponent newComponent;
        if ( v == null ) {
            newComponent = new JBox();
        } else {
            UIForAnySwing<?, ?> view = null;
            try {
                view = viewSupplier.createViewFor(v);
            } catch ( Exception e ) {
                log.error("Error while creating view for '"+v+"'.", e);
            }
            if ( view == null )
                view = UI.box(); // We add a dummy component to the list of children.

            newComponent = view.get((Class)view.getType());
        }
        // We remove the old component.
        c.remove(c.getComponent(index));
        // We add the new component.
        if ( attr == null )
            c.add(newComponent, index);
        else
            c.add(newComponent, attr.toConstraintForLayoutManager(), index);
        // We update the layout.
        c.revalidate();
        c.repaint();
    }

    private <M> void _addComponentAt(
        int index, @Nullable M v, ViewSupplier<M> viewSupplier, @Nullable AddConstraint attr, C thisComponent
    ) {
        JComponent newComponent;
        if ( v == null ) {
            newComponent = new JBox();
        } else {
            UIForAnySwing<?, ?> view = null;
            try {
                view = viewSupplier.createViewFor(v);
            } catch ( Exception e ) {
                log.error("Error while creating view for '"+v+"'.", e);
            }
            if ( view == null )
                view = UI.box(); // We add a dummy component to the list of children.

            newComponent = view.get((Class)view.getType());
        }
        // We add the new component.
        if ( attr == null )
            thisComponent.add(newComponent, index);
        else
            thisComponent.add(newComponent, attr.toConstraintForLayoutManager(), index);
        // We update the layout.
        thisComponent.revalidate();
        thisComponent.repaint();
    }

    private void _removeComponentAt( int index, C thisComponent )
    {
        if ( index < 0 ) {
            log.error(
                "Cannot remove sub-component of '"+thisComponent+"' \n" +
                "at index '"+index+"' because the index is negative.",
                new Throwable()
            );
        } else {
            int numberOfExistingComponents = thisComponent.getComponentCount();
            if (index >= numberOfExistingComponents) {
                log.error(
                    "Cannot remove sub-component of '" + thisComponent + "' \n" +
                    "at index '" + index + "' because there it currently only has '" + numberOfExistingComponents + "' " +
                    "sub-components instead of at least '" + (index + 1) + "' sub-components.",
                    new Throwable()
                );
            } else {
                // We get the component at the specified index.
                Component component = thisComponent.getComponent(index);
                if ( component == null ) {
                    log.error(
                        "Cannot remove sub-component of '" + thisComponent + "' \n" +
                        "at index '" + index + "' because there is no component at that index.",
                        new Throwable()
                    );
                } else {
                    // We remove the component.
                    thisComponent.remove(component);
                    // We update the layout.
                    thisComponent.revalidate();
                    thisComponent.repaint();
                }
            }
        }
    }

    private void _clearComponentsOf( C thisComponent ) {
        // We remove all components.
        thisComponent.removeAll();
        // We update the layout.
        thisComponent.revalidate();
        thisComponent.repaint();
    }

    private static boolean _isBorderLayout( Object o ) {
        return BorderLayout.CENTER.equals(o)     ||
               BorderLayout.PAGE_START.equals(o) ||
               BorderLayout.PAGE_END.equals(o)   ||
               BorderLayout.LINE_END.equals(o)   ||
               BorderLayout.LINE_START.equals(o) ||
               BorderLayout.EAST.equals(o)       ||
               BorderLayout.WEST.equals(o)       ||
               BorderLayout.NORTH.equals(o)      ||
               BorderLayout.SOUTH.equals(o);
    }
}
