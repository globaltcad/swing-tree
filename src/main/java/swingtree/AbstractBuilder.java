package swingtree;

import sprouts.Action;
import sprouts.Val;
import sprouts.Vals;
import sprouts.ValsDelegate;
import swingtree.api.Peeker;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.function.Consumer;

/**
 *  This is the root builder type for all other builder subtypes.
 *  It is a generic builder which may wrap anything to allow for method chaining based building!
 *
 * @param <I> The concrete implementation type of this builder.
 * @param <C> The component type parameter.
 */
abstract class AbstractBuilder<I, C extends Component>
{
    /**
     *  The component wrapped by this builder node.
     */
    private final WeakReference<C> _component;
    private C _componentStrongRef; // A strong reference to the component (This is only used to prevent the component from being garbage collected)

    /**
     * The thread mode determines how events are dispatched to the component.
     * And also which type of thread can access the component.
     */
    protected final EventProcessor _eventProcessor = UI.SETTINGS().getEventProcessor();

    /**
     *  The type class of the component wrapped by this builder node.
     *  See documentation for method "build" for more information.
     */
    protected final Class<C> _type;

    /**
     *  Instances of the {@link AbstractBuilder} as well as its sub types always wrap
     *  a single component for which they are responsible.
     *
     * @param component The component type which will be wrapped by this builder node.
     */
    public AbstractBuilder( C component ) {
        _type = (Class<C>) component.getClass();
        _component = new WeakReference<>(component);
        _componentStrongRef = component;
    }

    /**
     * @param action An action which should be executed by the UI thread (EDT).
     */
    protected final void _doUI( Runnable action ) { _eventProcessor.registerUIEvent( action ); }

    /**
     * @param action An action which should be executed by the application thread.
     */
    protected final void _doApp( Runnable action ) { _eventProcessor.registerAppEvent(action); }

    /**
     * @param value A value which should be captured.
     * @param action A consumer lambda receiving the provided value and
     *               is then executed by the application thread.
     * @param <T> The type of the value.
     */
    protected final <T> void _doApp( T value, Consumer<T> action ) { _doApp(()->action.accept(value)); }

    /**
     * @param val A property which should be captured.
     * @param displayAction A consumer lambda receiving the provided value and
     *                      is then executed by the UI thread.
     * @param <T> The type of the value.
     */
    protected final <T> void _onShow(Val<T> val, Consumer<T> displayAction ) {
        val.onSet(new Action<Val<T>>() {
            @Override
            public void accept( Val<T> val ) {
                T v = val.orElseNull(); // IMPORTANT! We first capture the value and then execute the action in the app thread.
                _doUI(() ->
                    /*
                        We make sure that the action is only executed if the component
                        is not disposed. This is important because the action may
                        access the component, and we don't want to get a NPE.
                     */
                        component().ifPresent(c -> {
                            displayAction.accept(v); // Here the captured value is used. This is extremely important!
                        /*
                             Since this is happening in another thread we are using the captured property item/value.
                             The property might have changed in the meantime, but we don't care about that,
                             we want things to happen in the order they were triggered.
                         */
                        })
                );
            }
            @Override public boolean canBeRemoved() { return !component().isPresent(); }
        });
    }

    /**
     * @param vals A property list which should be captured.
     * @param displayAction A consumer lambda receiving the action delegate and
     *                      is then executed by the UI thread.
     * @param <T> The type of the value.
     */
    protected final <T> void _onShow(Vals<T> vals, Consumer<ValsDelegate<T>> displayAction ) {
        vals.onChange(new Action<ValsDelegate<T>>() {
            @Override
            public void accept(ValsDelegate<T> delegate) {
                _doUI(() ->
                        component().ifPresent(c -> {
                            displayAction.accept(delegate);
                        /*
                            We make sure that the action is only executed if the component
                            is not disposed. This is important because the action may
                            access the component, and we don't want to get a NPE.
                         */
                        })
                );
            }
            @Override public boolean canBeRemoved() { return !component().isPresent(); }
        });
    }

    protected final I _this() { return (I) this; }

    protected final void _detachStrongRef() { _componentStrongRef = null; }

    /**
     *  The component wrapped by this builder node.
     */
    public final C getComponent() {
        if ( _eventProcessor != EventProcessor.COUPLED && !UI.thisIsUIThread() )
            throw new IllegalStateException(
                    "This UI is configured to be decoupled from the application thread, " +
                    "which means that it can only be modified from the EDT. " +
                    "Please use 'UI.run(()->...)' method to execute your modifications on the EDT."
                );
        return _component.get();
    }

    /**
     *  The optional component wrapped by this builder node.
     */
    public final OptionalUI<C> component() { return OptionalUI.ofNullable(_component.get()); }

    /**
     *  The type class of the component wrapped by this builder node.
     *  See documentation for method "build" for more information.
     * @return The type class of the component wrapped by this builder node.
     */
    public final Class<C> getType() { return _type; }

    /**
     *  Use this if you wish to access the component wrapped by this builder directly.
     *  This is useful for more fine-grained control, like for example calling
     *  methods like "setName", "setTitle", and so on... <br>
     *  This method accepts a lambda to which the component wrapped by this builder will be supplied.
     *  The lambda can then call said methods or perform other tasks which
     *  might be relevant to the component while also not
     *  breaking the benefits of nesting and method chaining provided by this class...
     *  <br>
     *  The below example shows how this method allows for more fine-grained control over the wrapped component:
     *  <pre>{@code
     *      UI.panel()
     *      peek( panel -> panel.setDebugGraphicsOptions(true) );
     *  }</pre>
     *  <br><br>
     *
     * @param action A Consumer lambda which simply returned the wrapped JComponent type for interacting it.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I peek( Peeker<C> action ) {
        action.accept(getComponent());
        return _this();
    }

    /**
     *  Use this to build or not build UI, based on a boolean condition value and a consumer
     *  lambda which continues the building process if the previous boolean is true.
     *  This builder will simply be supplied to the provided consumer lambda.
     *  Inside the second lambda, one can then continue building the UI while also not
     *  breaking the benefits of nesting and method chaining provided by this class...
     *  <p>
     *  This isin essence a more advanced version of {@link #apply(Consumer)}.
     *  <br>
     *
     * @param condition The truth value which determines if the second consumer lambda will be executed.
     * @param building A Consumer lambda which simply consumes this builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I applyIf( boolean condition, Consumer<I> building ) {
        NullUtil.nullArgCheck(building, "building", Consumer.class);
        I builder = _this();
        if ( condition ) building.accept(builder);
        return builder;
    }


    /**
     *  Use this to build or not build UI, based on a provided {@link Optional} representing a consumer
     *  lambda which continues the building process if is is present within the optional.
     *  This builder will simply be supplied to the provided consumer lambda.
     *  Inside the second lambda, one can then continue building the UI while also not
     *  breaking the benefits of nesting and method chaining provided by this class...
     *  <p>
     *  This is in essence a more advanced version of {@link #applyIf(boolean, Consumer)}
     *  and {@link #apply(Consumer)}.
     *  <br>
     *
     * @param building An optional consumer lambda which simply consumes this builder node.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I applyIfPresent( Optional<Consumer<I>> building ) {
        NullUtil.nullArgCheck(building, "building", Optional.class);
        I builder = _this();
        building.ifPresent( buildingLambda -> buildingLambda.accept(builder) );
        return builder;
    }

    /**
     *  Use this to continue building UI inside a provided lambda.
     *  This is especially useful for when you need to build UI based on loops.
     *  This builder instance will simply be supplied to the provided consumer lambda.
     *  Inside this lambda, you can then continue building the UI while also not
     *  breaking the benefits of nesting and method chaining provided by this class...
     *  <br><br>
     *
     * @param building A Consumer lambda which simply consumes this builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I apply( Consumer<I> building ) {
        NullUtil.nullArgCheck(building, "building", Consumer.class);
        return applyIf(true, building);
    }

    /**
     *  This method completes the building process for the wrapped
     *  {@link javax.swing.JComponent} type by returning it.
     *  However, it also expects the user to pass the class of the {@link javax.swing.JComponent}
     *  wrapped by this builder! This is not out of necessity but for better
     *  readability when using the builder in more extensive ways where
     *  the beginning and end of the method chaining and nesting of the builder does
     *  not fit on one screen. <br>
     *  In that case the expression "{@code .getResulting(JMenu.class)}" helps
     *  to identify which type of {@link javax.swing.JComponent} is currently being build on a given
     *  nesting layer... <br><br>
     *
     * @param type The type class of the component which this builder wraps.
     * @param <T> The type parameter of the component which this builder wraps.
     * @return The result of the building process, namely: a type of JComponent.
     */
    public <T extends C> T get( Class<T> type ) {
        assert type == _type || type.isAssignableFrom(_type);
        return (T) _component.get();
    }
}
