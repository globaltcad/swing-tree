package swingtree;

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

                            public int getSize() { _checkContentChange(); return entries.size(); }
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
    public final UIForList<E, L> withRenderer( ListEntryRenderer<E, L> renderer ) {
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


    private static class ValsListModel<E> extends AbstractListModel<E>
    {
        private final Vals<E> _entries;

        public ValsListModel( Vals<E> entries ) {
            _entries = Objects.requireNonNull(entries, "entries");
        }

        @Override public int getSize() { return _entries.size(); }
        @Override public E getElementAt( int i ) { return _entries.at( i ).orElseNull(); }

        public void fire( ValsDelegate<E> v ) {
            if ( v.index() < 0 ) {
                fireContentsChanged( this, 0, _entries.size() );
                return;
            }
            switch ( v.changeType() ) {
                case ADD:    fireIntervalAdded( this, v.index(), v.index() ); break;
                case REMOVE: fireIntervalRemoved( this, v.index(), v.index() ); break;
                case SET:    fireContentsChanged( this, v.index(), v.index() ); break;
                default:
                    fireContentsChanged( this, 0, _entries.size() );
            }
        }
    }

}
