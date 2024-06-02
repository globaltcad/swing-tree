package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.*;
import swingtree.api.ListEntryDelegate;
import swingtree.api.ListEntryRenderer;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 *  A SwingTree builder node designed for configuring {@link JList} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <L> The type of the {@link JList} instance which will be managed by this builder.
 */
public final class UIForList<E, L extends JList<E>> extends UIForAnySwing<UIForList<E, L>, L>
{
    private static final Logger log = LoggerFactory.getLogger(UIForList.class);
    private final BuilderState<L> _state;

    /**
     * Extensions of the {@link  UIForAnySwing} always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the underlying component is built.
     */
    UIForList( BuilderState<L> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<L> _state() {
        return _state;
    }
    
    @Override
    protected UIForList<E, L> _newBuilderWithState(BuilderState<L> newState ) {
        return new UIForList<>(newState);
    }

    /**
     *  Takes the provided list of entry objects and sets them as {@link JList} data.
     *
     * @param entries The list of entries to set as data.
     * @return This instance of the builder node.
     */
    public final UIForList<E, L> withEntries( List<E> entries ) {
        return _with( thisComponent ->
                thisComponent.setModel (
                        new AbstractListModel<E>() {

                            private List<E> _reference = new ArrayList<>(entries);

                            @Override
                            public int getSize() { _checkContentChange(); return entries.size(); }
                            @Override
                            public E getElementAt( int i ) { _checkContentChange(); return entries.get( i ); }

                            private void _checkContentChange() {
                                UI.runLater(()-> {
                                    if ( _reference.size() != entries.size() ) {
                                        fireContentsChanged( this, 0, entries.size() );
                                        _reference = new ArrayList<>(entries);
                                    }
                                    else
                                        for ( int i = 0; i < entries.size(); i++ )
                                            if ( !_reference.get( i ).equals( entries.get( i ) ) ) {
                                                fireContentsChanged( this, 0, entries.size() );
                                                _reference = new ArrayList<>(entries);
                                                break;
                                            }
                                });
                            }
                        }
                    )
                )
                ._this();
    }

    /**
     *  Takes the provided array of entry objects and sets them as {@link JList} data.
     *
     * @param entries The array of entries to set as data.
     * @return This instance of the builder node.
     */
    @SafeVarargs
    public final UIForList<E, L> withEntries( E... entries ) {
        return _with( thisComponent -> {
                    thisComponent.setListData( entries );
                })
                ._this();
    }

    /**
     *  Takes the provided observable property list of entries in the form of a {@link Vals}
     *  object and uses them as a basis for modelling the {@link JList} data.
     *  If the {@link Vals} object changes, the {@link JList} data will be updated accordingly,
     *  and vice versa.
     *
     * @param entries The {@link Vals} of entries to set as data model.
     * @return This instance of the builder node to allow for builder-style fluent method chaining.
     */
    public final UIForList<E, L> withEntries( Vals<E> entries ) {
        Objects.requireNonNull(entries, "entries");
        ValsListModel<E> model = new ValsListModel<>(entries);
        return _with( thisComponent -> {
                    thisComponent.setModel(model);
                })
                ._withOnShow( entries, (thisComponent, v) -> {
                    model.fire(v);
                })
                ._this();
    }

    /**
     *  Takes an observable property in the form of a {@link Var} object
     *  and uses it as a basis for modelling the {@link JList} selection.
     *  If the {@link Var} object changes, the {@link JList} selection will be updated accordingly,
     *  and vice versa.
     *  If you do not want this relationship to be bidirectional, use {@link #withSelection(Val)} instead.
     *
     * @param selection The {@link Var} of entries to set as selection model.
     * @return This instance of the builder node to allow for fluent method chaining.
     */
    @SuppressWarnings("NullAway")
    public final UIForList<E, L> withSelection( Var<E> selection ) {
        return _with( thisComponent -> {
                     thisComponent.addListSelectionListener( e -> {
                         if ( !e.getValueIsAdjusting() )
                             // Necessary because Java 8 does not check if index is out of bounds.
                             if (thisComponent.getMinSelectionIndex() >= thisComponent.getModel().getSize())
                                 selection.set( From.VIEW, null );
                             else
                                 selection.set( From.VIEW,  thisComponent.getSelectedValue() );
                     });
                })
                ._withOnShow( selection, (thisComponent,v) -> {
                    if (v == null)
                        // Necessary because Java 8 does not handle null properly.
                        thisComponent.clearSelection();
                    else
                        thisComponent.setSelectedValue(v, true);
                })
                ._with( thisComponent -> {
                    thisComponent.setSelectedValue( selection.orElseNull(), true );
                })
               ._this();
    }

    /**
     *  Takes an observable read-only property in the form of a {@link Val} object
     *  and uses it as a basis for modelling the {@link JList} selection.
     *  If the {@link Val} object changes, the {@link JList} selection will be updated accordingly.
     *  However, if the {@link JList} selection changes due to user interaction,
     *  the {@link Val} object will not be updated.
     *
     * @param selection The {@link Val} of entries to set as selection model.
     * @return This instance of the builder node to allow for fluent method chaining.
     */
    public final UIForList<E, L> withSelection( Val<E> selection ) {
        NullUtil.nullArgCheck(selection, "selection", Val.class);
        return _withOnShow( selection, (thisComponent,v) -> {
                    thisComponent.setSelectedValue( v, true );
               })
                ._with( thisComponent -> {
                    thisComponent.setSelectedValue( selection.orElseNull(), true );
                })
               ._this();
    }

    /**
     *  The {@link ListEntryRenderer} passed to this method is a functional interface
     *  receiving a {@link ListEntryDelegate} instance and returns
     *  a {@link javax.swing.JComponent}, which is
     *  used to render each entry of the {@link JList} instance. <br>
     *  A typical usage of this method would look like this:
     *  <pre>{@code
     *   listOf(vm.colors())
     *   .withRenderer( it -> new Component() {
     *     {@literal @}Override
     *     public void paint(Graphics g) {
     *       g.setColor(it.entry().orElse(Color.PINK));
     *       g.fillRect(0,0,getWidth(),getHeight());
     *     }
     *   })
     * }</pre>
     * <p>
     * In this example, a new {@link JList} is created for the observable property list
     * of colors, which is provided by the <code>vm.colors()</code> method.
     * The entries of said list are individually exposed specified renderer
     * lambda expression, which return a {@link javax.swing.JComponent} instance
     * that is used by the {@link JList} to render each entry.
     * In this case a colored rectangle is rendered for each entry.
     *
     * @param renderer The {@link ListEntryRenderer} that will be used to supply {@link javax.swing.JComponent}s
     *                 responsible for rendering each entry of the {@link JList} instance.
     * @return This instance of the builder node to allow for fluent method chaining.
     */
    public final UIForList<E, L> withRenderComponent( ListEntryRenderer<E, L> renderer ) {
        return _with( thisComponent -> {
                    thisComponent.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> renderer.render(new ListEntryDelegate<E, L>() {
                        @Override public L list() { return (L) list; }
                        @Override public Optional<E> entry() { return Optional.ofNullable(value); }
                        @Override public int index() { return index; }
                        @Override public boolean isSelected() { return isSelected; }
                        @Override public boolean hasFocus() { return cellHasFocus; }
                    }));
                })
                ._this();
    }

    /**
     * Adds an {@link Action} event handler to the underlying {@link JList}
     * through a {@link javax.swing.event.ListSelectionListener},
     * which will be called when a list selection has been made.
     * {see JList#addListSelectionListener(ListSelectionListener)}.
     *
     * @param action The {@link Action} that will be notified.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForList<E, L> onSelection( Action<ComponentDelegate<JList<E>, ListSelectionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    thisComponent.addListSelectionListener(
                        e -> _runInApp(()->action.accept(new ComponentDelegate<>( thisComponent, e)))
                    );
                })
                ._this();
    }

    /**
     * Receives a {@link Render.Builder} instance providing
     * a fluent builder API for configuring how the values of this {@link JList} instance
     * should be rendered. The {@link Render.Builder} instance may be
     * created through the {@link UI#renderListItem(Class)} method.
     * <p>
     * Take a look at the following example:
     * <pre>{@code
     *  UI.list(vm.wordList())
     *  .withRenderer(
     *    renderListItem(String.class)
     *    .asText(
     *      cell->"("+cell.value().get()+")"
     *    )
     *  )
     *  }</pre>
     *  In this example, a new {@link JList} is created with a list of words
     *  in the form of {@link String}s, which is provided by the <code>vm.wordList()</code> method.
     *  The entries of said list are individually prepared for rendering
     *  through the {@link Render.As#asText(Function)} method.
     *
     * @param renderBuilder The {@link Render.Builder} containing the fluently configured renderer.
     * @return This very instance, which enables builder-style method chaining.
     * @param <V> The type of the values that will be rendered.
     */
    public final <V extends E> UIForList<E, L> withRenderer( Render.Builder<L,V> renderBuilder ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return _withRenderer(renderBuilder);
    }

    private final <V extends E> UIForList<E, L> _withRenderer( Render.Builder<L,V> renderBuilder ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return _with( thisComponent -> {
                    thisComponent.setCellRenderer((ListCellRenderer<E>) renderBuilder.buildForList(thisComponent));
                })
                ._this();
    }

    /**
     * Sets the {@link ListCellRenderer} for the {@link JList}, which renders the list items
     * by supplying a custom component for each item through the
     * {@link ListCellRenderer#getListCellRendererComponent(JList, Object, int, boolean, boolean)} method.
     * <p>
     * @param renderer The {@link ListCellRenderer} that will be used to paint each cell in the list.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForList<E, L> withRenderer( ListCellRenderer<E> renderer ) {
        return _with( thisComponent -> {
                    thisComponent.setCellRenderer(renderer);
                })
                ._this();
    }

    /**
     *  Use this to build a list cell renderer for various item types without
     *  a meaningful common super-type (see {@link #withRenderer(Class, Function)}).
     *  You would typically want to use this method to render generic types where the only
     *  common type is {@link Object}, yet you want to render the item
     *  in a specific way depending on their actual type.
     *  This is done like so:
     *  <pre>{@code
     *  UI.list(new Object[]{":-)", 42L, '§'})
     *  .withRenderer( it -> it
     *      .when(String.class).asText( cell -> "String: "+cell.getValue() )
     *      .when(Character.class).asText( cell -> "Char: "+cell.getValue() )
     *      .when(Number.class).asText( cell -> "Number: "+cell.getValue() )
     *  );
     *  }</pre>
     *
     * @param renderBuilder A lambda function that configures the renderer for this combo box.
     * @return This combo box instance for further configuration.
     * @param <V> The type of the value that is being rendered in this combo box.
     */
    public final <V extends E> UIForList<E, L> withRenderer(
        Function<Render.Builder<L,V>,Render.Builder<L,V>> renderBuilder
    ) {
        Class<Object> commonType = Object.class;
        Objects.requireNonNull(commonType);
        Render.Builder render = Render.forCombo(commonType).when(commonType).asText(cell->cell.valueAsString().orElse(""));
        try {
            render = renderBuilder.apply(render);
        } catch (Exception e) {
            log.error("Error while building renderer.", e);
            return this;
        }
        Objects.requireNonNull(render);
        return _withRenderer(render);
    }

    /**
     *  Use this to build a list cell renderer for a specific item type.
     *  What you would typically want to do is customize the text that should be displayed
     *  for a specific item type. <br>
     *  This is done like so:
     *  <pre>{@code
     *  UI.list("A", "B", "C")
     *  .withRendererFor(String.class, it -> it
     *      .asText(cell -> cell.getValue().toLowerCase())
     *  );
     *  }</pre>
     *
     * @param itemType The type of the items which should be rendered using a custom renderer.* @return A render builder exposing an API that allows you to
     *          configure how he passed item type should be rendered.
     * @param renderBuilder A lambda function that takes a {@link Render.As} builder as argument and returns another
     *                      {@link Render.Builder} that configures how the passed item type should be rendered.
     * @return This builder instance for further configuration.
     * @param <T> The type of the items which should be rendered using a custom renderer.
     * @param <V> The type of the items which should be rendered using a custom renderer.
     * @deprecated Use {@link #withRenderer(Class, Function)} instead.
     */
    @Deprecated
    public final <T, V extends E> UIForList<E, L> withRendererFor(
        Class<T> itemType,
        Function<Render.As<JList<T>, T, T>,Render.Builder<L,V>> renderBuilder
    ) {
        Render.Builder<L,V> render;
        try {
            render = renderBuilder.apply(Render.forList(itemType).when(itemType));
        } catch (Exception e) {
            log.error("Error while building renderer.", e);
            return this;
        }
        return _withRenderer(render);
    }

    /**
     *  Use this to build a list cell renderer for a specific item type and its subtype.
     *  You would typically want to use this method to render generic types like {@link Object}
     *  where you want to render the item in a specific way depending on its actual type.
     *  This is done like so:
     *  <pre>{@code
     *  UI.list(new Number[]{1f, 42L, 4.20d})
     *  .withRenderer(Number.class, it -> it
     *      .when(Integer.class).asText( cell -> "Integer: "+cell.getValue() )
     *      .when(Long.class).asText( cell -> "Long: "+cell.getValue() )
     *      .when(Float.class).asText( cell -> "Float: "+cell.getValue() )
     *  );
     *  }</pre>
     *
     * @param commonType The common type of the items which should be rendered using a custom renderer.
     * @return A render builder exposing an API that allows you to configure how he passed item types should be rendered.
     * @param <T> The common super-type type of the items which should be rendered using a custom renderer.
     */
    public final <T extends E> UIForList<E,L> withRenderer(
        Class<T> commonType,
        Function<Render.Builder<L, T>,Render.Builder<L, T>> renderBuilder
    ) {
        Objects.requireNonNull(commonType);
        Render.Builder render = Render.forCombo(commonType).when(commonType).asText(cell->cell.valueAsString().orElse(""));
        try {
            render = renderBuilder.apply(render);
        } catch (Exception e) {
            log.error("Error while building renderer.", e);
            return this;
        }
        Objects.requireNonNull(render);
        return _withRenderer(render);
    }


    private static class ValsListModel<E> extends AbstractListModel<E>
    {
        private final Vals<E> _entries;

        public ValsListModel( Vals<E> entries ) {
            _entries = Objects.requireNonNull(entries, "entries");
        }

        @Override public int getSize() {
            return _entries.size();
        }
        @Override public @Nullable E getElementAt(int i ) {
            return _entries.at( i ).orElseNull();
        }

        public void fire( ValsDelegate<E> v ) {
            int index = v.index();
            if ( index < 0 ) {
                fireContentsChanged( this, 0, _entries.size() );
                return;
            }
            switch ( v.changeType() ) {
                case ADD:    fireIntervalAdded(   this, index, index + v.newValues().size() ); break;
                case REMOVE: fireIntervalRemoved( this, index, index + v.oldValues().size() ); break;
                case SET:    fireContentsChanged( this, index, index + v.newValues().size() ); break;
                default:
                    fireContentsChanged( this, 0, _entries.size() );
            }
        }
    }

}
