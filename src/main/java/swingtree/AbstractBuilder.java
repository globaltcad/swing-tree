package swingtree;

import sprouts.*;
import sprouts.Action;
import swingtree.api.Peeker;
import swingtree.style.ComponentExtension;
import swingtree.threading.EventProcessor;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *  This is the root builder type for all other builder subtypes.
 *  It is a generic builder which may wrap anything to allow for method chaining based building!
 *
 * @param <I> The concrete implementation type of this builder, "I" stands for "Implementation".
 * @param <C> The component type parameter.
 */
abstract class AbstractBuilder<I, C extends Component>
{
    /**
     *  The component wrapped by this builder node.
     */
    private final MaybeWeakReference<C> _component;

    /**
     *  A strong reference to the component (This is only used to prevent the component from being garbage collected)
     * @param <T> The type of the component.
     */
    private static class MaybeWeakReference<T extends Component> extends WeakReference<T> {
        private T _strongRef; // This is only used to prevent the component from being garbage collected
        public MaybeWeakReference( T referent ) { super(referent); _strongRef = referent; }
        public void detachStrongRef() { _strongRef = null; }
    }

    /**
     * The thread mode determines how events are dispatched to the component.
     * And also which type of thread can access the component.
     */
    protected final EventProcessor _eventProcessor = SwingTree.get().getEventProcessor();

    /**
     *  The type class of the component wrapped by this builder node.
     */
    protected final Class<C> _type;

    /**
     *  Instances of the {@link AbstractBuilder} as well as its subtypes always wrap
     *  a single component for which they are responsible.
     *
     * @param component The component type which will be wrapped by this builder node.
     */
    public AbstractBuilder( C component ) {
        _type = (Class<C>) component.getClass();
        _component = new MaybeWeakReference<>(component);
        if ( component instanceof JComponent )
            ComponentExtension.makeSureComponentHasExtension( (JComponent) component );
    }

    /**
     * @param action An action which should be executed by the UI thread (EDT).
     */
    protected final void _doUI( Runnable action ) { _eventProcessor.registerUIEvent( action ); }

    /**
     * @param action An action which should be executed by the application thread,
     *               which is determined by implementations of the {@link EventProcessor},
     *               also see {@link UI#use(EventProcessor, Supplier)}.
     */
    protected final void _doApp( Runnable action ) { _eventProcessor.registerAppEvent(action); }

    /**
     * @param value A value which should be captured and then passed to the provided action
     *              on the current application thread (see {@link EventProcessor} and {@link UI#use(EventProcessor, Supplier)}).
     * @param action A consumer lambda which is executed by the application thread
     *               and receives the provided value.
     * @param <T> The type of the value.
     */
    protected final <T> void _doApp( T value, Consumer<T> action ) { _doApp(()->action.accept(value)); }

    /**
     *  Use this to register a state change listener for the provided property
     *  which will be executed by the UI thread (see {@link EventProcessor}).
     *
     * @param val A property whose state changes should be listened to on the UI thread.
     * @param displayAction A consumer lambda receiving the provided value and
     *                      is then executed by the UI thread.
     * @param <T> The type of the item wrapped by the provided property.
     */
    protected final <T> void _onShow( Val<T> val, Consumer<T> displayAction ) {
        val.onChange(From.ALL, new Action<Val<T>>() {
            @Override
            public void accept( Val<T> val ) {
                T v = val.orElseNull(); // IMPORTANT! We first capture the value and then execute the action in the app thread.
                _doUI(() ->
                    /*
                        We make sure that the action is only executed if the component
                        is not disposed. This is important because the action may
                        access the component, and we don't want to get a NPE.
                     */
                        component().ifPresent( c -> {
                            try {
                                displayAction.accept(v); // Here the captured value is used. This is extremely important!
                                /*
                                     Since this is happening in another thread we are using the captured property item/value.
                                     The property might have changed in the meantime, but we don't care about that,
                                     we want things to happen in the order they were triggered.
                                 */
                            } catch ( Exception e ) {
                                throw new RuntimeException(
                                    "Failed to apply state of property '" + val + "' to component '" + c + "'.",
                                    e
                                );
                            }
                        })
                );
            }
            @Override public boolean canBeRemoved() { return !component().isPresent(); }
        });
    }

    /**
     *  Use this to register a state change listener for the provided property list
     *  which will be executed by the UI thread (see {@link EventProcessor}).
     *
     * @param vals A property list whose state changes should be listened to on the UI thread.
     * @param displayAction A consumer lambda receiving the action delegate and
     *                      is then executed by the UI thread.
     * @param <T> The type of the items wrapped by the provided property list.
     */
    protected final <T> void _onShow( Vals<T> vals, Consumer<ValsDelegate<T>> displayAction ) {
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

    /**
     * @return The builder instance itself based on the type parameter {@code <I>}.
     */
    protected final I _this() { return (I) this; }

    /**
     *  This method sets the strong reference to the component to null,
     *  which allows the component to be garbage collected.
     *  This is important to avoid memory leaks, as a component is typically
     *  part of a tree of components, and if one component is not garbage collected,
     *  then the whole tree is not garbage collected.
     */
    protected final void _detachStrongRef() { _component.detachStrongRef(); }

    /**
     *  The component wrapped by this builder node.
     *
     *  @throws IllegalStateException if this method is called from a thread other than the EDT
     *                                and this UI is configured to be decoupled from the application thread.
     *                                See {@link UI#use(EventProcessor, Supplier)}.
     *  @return The component wrapped by this builder node.
     */
    public final C getComponent() {
        boolean isCoupled       = _eventProcessor == EventProcessor.COUPLED;
        boolean isCoupledStrict = _eventProcessor == EventProcessor.COUPLED_STRICT;

        if ( !isCoupled && !isCoupledStrict && !UI.thisIsUIThread() )
            throw new IllegalStateException(
                    "This UI is configured to be decoupled from the application thread, " +
                    "which means that it can only be modified from the EDT. " +
                    "Please use 'UI.run(()->...)' method to execute your modifications on the EDT."
                );
        return _component.get();
    }

    /**
     *  The optional component wrapped by this builder node.
     *
     * @return An {@link OptionalUI} wrapping a component or null.
     *         This optional will throw an exception if the
     *         application has an application thread (see {@link UI#use(EventProcessor, Supplier)})
     *         and this method is called from a thread other than the EDT.
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
     *  This builder instance will simply be supplied to the provided consumer lambda.
     *  Inside the second lambda, one can then continue building the UI while also not
     *  breaking the benefits of nesting and method chaining provided by this builder...
     *  <p>
     *  This is in essence a more advanced version of {@link #apply(Consumer)}.
     *  <br>
     *  Here a simple usage example:
     *  <pre>{@code
     *      UI.panel()
     *      .applyIf( userIsLoggedIn, it -> it
     *          .add( UI.label("Welcome back!") )
     *          .add( UI.button("Logout")).onClick( () -> logout() )
     *          .add( UI.button("Settings")).onClick( () -> showSettings() )
     *      )
     *      .applyIf( !userIsLoggedIn, it -> it
     *          .add( UI.label("Please login to continue.") )
     *          .add( UI.button("Login")).onClick( () -> login() );
     *      );
     *  }</pre>
     *  Here we use theis method to build a panel
     *  with different content depending on whether the user is logged in or not.
     *  <br><br>
     *
     * @param condition The truth value which determines if the second consumer lambda is executed or not.
     * @param building A {@link Consumer} lambda which simply consumes this builder instance.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I applyIf( boolean condition, Consumer<I> building ) {
        NullUtil.nullArgCheck(building, "building", Consumer.class);
        I builder = _this();
        if ( condition ) building.accept(builder);
        return builder;
    }


    /**
     *  Allows you to build declarative UI conditionally,
     *  meaning that the UI is only built if the provided {@link Optional} value is present.
     *  <p>
     *  Consider the following example:
     *  <pre>{@code
     * // In your view model:
     * public Optional<MySubModel> getM() {
     *   return Optional.ofNullable(this.model);
     * }
     *
     * // In your view:
     * UI.panel()
     * .add(UI.label("Maybe Sub Model:"))
     * .applyIfPresent(vm.getM().map(m->ui->ui
     *   .add(UI.label("Hello Sub Model!"))
     *   .add(UI.label("A:")
     *   .add(UI.textField(m.getA()))
     *   .add(UI.label("B:"))
     *   .add(UI.textField(m.getB()))
     *   // ...
     * ))
     * .add(UI.label("Some other stuff..."));
     * }</pre>
     *
     * The {@code applyIfPresent} method takes an {@code Optional<Consumer<I>>} as parameter,
     * where {@code I} is the type of the UI builder.
     * This allows you to map the optional value to a consumer which is only executed if the value is present.
     * If the optional value is present, the consumer is executed with the
     * current UI builder as a parameter, which allows you to continue building the UI as usual.
     * <br>
     * The {@code m->ui->ui} may look a bit confusing at first, but it is simply a lambda expression
     * which takes the optional value and returns a consumer ({@code ui->ui...}) which takes the UI builder
     * as a parameter.
     * <br>
     * This is in essence a more advanced {@code Optional} centric version of {@link #applyIf(boolean, Consumer)}
     * and {@link #apply(Consumer)}.
     * <br>
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
     *  Use this to continue building UI inside a provided lambda
     *  if you need to introduce some imperative code in between
     *  the building process. <br>
     *  This is especially useful for when you need to build UI based on loops.
     *  The current builder instance will simply be supplied to the provided {@link Consumer} lambda.
     *  Inside the supplied lambda, you can then continue building the UI while also not
     *  breaking the benefits of nesting and method chaining, effectively preserving
     *  the declarative nature of the builder.
     *  <br><br>
     *  Here is a simple example of how this method can be used to build a panel
     *  with a variable amount of images displayed in a grid:
     *  <pre>{@code
     *      UI.panel("wrap 3")
     *      .apply( it -> {
     *          for ( String path : imagePaths )
     *              it.add( UI.label(UI.icon(path)) );
     *      });
     *  }</pre>
     *  <br><br>
     *  Here is another example of how this method can be used to build a panel
     *  with a variable amount of buttons displayed in a grid:
     *  <pre>{@code
     *    UI.panel("wrap 4")
     *    .apply( it -> {
     *      for ( int i = 0; i < numOfButtons; i++ )
     *          it.add( UI.button("Button " + i)
     *          .onClick( () -> {...} );
     *    });
     *  }</pre>
     *  <br><br>
     *
     * @param building A Consumer lambda which simply consumes this builder instance.
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
     *  In that case the expression "{@code .get(JMenu.class)}" helps
     *  to identify which type of {@link javax.swing.JComponent} is currently being built on a given
     *  nesting layer... <br><br>
     *  Here is a simple example of how this method can be used to build a menu bar:
     *  <pre>{@code
     *      UI.panel()
     *      .add(
     *          UI.menuBar()
     *          .add( UI.menu("File") )
     *          .add( UI.menuItem("Open") )
     *          .add( UI.menuItem("Save") )
     *          // ...
     *          .add( UI.menuItem("Exit") )
     *          .get(JMenuBar.class)
     *      )
     *      .add( UI.button("Click me!") )
     *      .get(JPanel.class);
     *  }</pre>
     *  As you can see, the expression "{@code .get(JMenuBar.class)}" as well as the expression
     *  "{@code .get(JPanel.class)}" at the end of the builder chain help to identify
     *  which type of {@link javax.swing.JComponent} is currently being built on a given nesting layer.
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
