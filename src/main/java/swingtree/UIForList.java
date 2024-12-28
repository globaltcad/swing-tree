package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Action;
import sprouts.*;
import swingtree.api.Configurator;
import swingtree.api.ListEntryDelegate;
import swingtree.api.ListEntryRenderer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    public final UIForList<E, L> withSelection( Var<E> selection ) {
        return _with( thisComponent -> {
                     thisComponent.addListSelectionListener( e -> {
                         if ( !e.getValueIsAdjusting() )
                             // Necessary because Java 8 does not check if index is out of bounds.
                             if (thisComponent.getMinSelectionIndex() >= thisComponent.getModel().getSize())
                                 selection.set( From.VIEW, NullUtil.fakeNonNull(null) );
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
     *   .withRenderComponent( it -> new Component() {
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
     * <p>
     * <b>Note that the preferred way to build a list cell renderer for various item types
     * is to use the {@link #withCell(Configurator)} method, which provides a more rich and fluent builder API.</b>
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
                        e -> _runInApp(()->{
                            try {
                                action.accept(new ComponentDelegate<>(thisComponent, e));
                            } catch (Exception ex) {
                                log.error("Error occurred while processing list selection event.", ex);
                            }
                        })
                    );
                })
                ._this();
    }

    private final <V extends E> UIForList<E, L> _withRenderer( CellBuilder<L,V> cellBuilder ) {
        NullUtil.nullArgCheck(cellBuilder, "renderBuilder", CellBuilder.class);
        return _with( thisComponent -> {
                    cellBuilder.buildForList(thisComponent);
                })
                ._this();
    }

    /**
     * Sets the {@link ListCellRenderer} for the {@link JList}, which renders the list items
     * by supplying a custom component for each item through the
     * {@link ListCellRenderer#getListCellRendererComponent(JList, Object, int, boolean, boolean)} method.
     * <p>
     * Also see {@link #withCells(Configurator)} method, which is the preferred way to build
     * a list cell renderer for various item types using a more rich and fluent builder API.
     *
     * @param renderer The {@link ListCellRenderer} that will be used to paint each cell in the list.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForList<E, L> withCellRenderer( ListCellRenderer<E> renderer ) {
        return _with( thisComponent -> {
                    thisComponent.setCellRenderer(renderer);
                })
                ._this();
    }

    /**
     *  Use this to build a list cell renderer for various item types
     *  by defining a renderer for each type or using {@link Object} as a common type
     *  using the fluent builder API exposed to the {@link Configurator}
     *  lambda function passed to this method. <br>
     *  This method is typically used when the list holds inhomogeneous item
     *  types which you want to be handled differently in the list view.
     *  <pre>{@code
     *  UI.list(new Object[]{":-)", 42L, '§'})
     *  .withCells( it -> it
     *      .when(String.class).asText( cell -> "String: "+cell.getValue() )
     *      .when(Character.class).asText( cell -> "Char: "+cell.getValue() )
     *      .when(Number.class).asText( cell -> "Number: "+cell.getValue() )
     *  );
     *  }</pre>
     *  Note that a similar API is also available for the {@link javax.swing.JComboBox}
     *  and {@link javax.swing.JTable} components, see {@link UIForCombo#withCells(Configurator)},
     *  {@link UIForTable#withCells(Configurator)} and {@link UIForTable#withCellsForColumn(int, Configurator)}
     *  for more information.
     *  <p>
     *  <b>
     *      Also see {@link #withCell(Configurator)} method, which constitutes the preferred way
     *      to build a list cell renderer as it is simpler, more concise and less error-prone.
     *  </b>
     *
     * @param renderBuilder A lambda function that configures the renderer for this combo box.
     * @return This combo box instance for further configuration.
     * @param <V> The type of the value that is being rendered in this combo box.
     */
    public final <V extends E> UIForList<E, L> withCells(
        Configurator<CellBuilder<L,V>> renderBuilder
    ) {
        Class<Object> commonType = Object.class;
        Objects.requireNonNull(commonType);
        CellBuilder render = CellBuilder.forList(commonType);
        try {
            render = renderBuilder.configure(render);
        } catch (Exception e) {
            log.error("Error while building renderer.", e);
            return this;
        }
        Objects.requireNonNull(render);
        return _withRenderer(render);
    }

    /**
     *  Allows for the configuration of a cell view for the items of the {@link JList} instance.
     *  The {@link Configurator} lambda function passed to this method receives a {@link CellConf}
     *  exposing the current item value and the current selection state of the cell.
     *  You may update return an updated cell with a desired view component
     *  through methods like {@link CellConf#view(Component)} or {@link CellConf#updateView(Configurator)}.
     *  <p>
     *  Here code snippet demonstrating how this method may be used
     *  as part of a UI declaration:
     *  <pre>{@code
     *      UI.list(new Month[]{Month.JANUARY, Month.FEBRUARY, Month.MARCH})
     *      .withCell( cell -> cell
     *          .updateView( comp -> comp
     *              .orGet(JLabel::new) // initialize a new JLabel if not already present
     *              .updateIf(JLabel.class, tf -> {
     *                  tf.setText(cell.valueAsString().orElse(""));
     *                  tf.setBackground(cell.isSelected() ? Color.YELLOW : Color.WHITE);
     *                  return tf;
     *              })
     *          )
     *      )
     *  }</pre>
     *  In this example, a new {@link JList} is created for an array of objects.
     *  The {@link Configurator} lambda function passed to the {@link #withCell(Configurator)} method
     *  configures the cell view for each item in the list.
     *
     * @param cellConfigurator The {@link Configurator} lambda function that configures the cell view.
     * @return This instance of the builder node to allow for fluent method chaining.
     * @param <V> The type of the value that is being rendered in this combo box.
     */
    public final <V extends E> UIForList<E, L> withCell(
            Configurator<CellConf<L, V>> cellConfigurator
    ) {
        return withCells( it -> it.when((Class)Object.class).as(cellConfigurator) );
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
            int index = v.index().orElse(-1);
            if ( index < 0 ) {
                fireContentsChanged( this, 0, _entries.size() );
                return;
            }
            switch ( v.change() ) {
                case ADD:    fireIntervalAdded(   this, index, index + v.newValues().size() ); break;
                case REMOVE: fireIntervalRemoved( this, index, index + v.oldValues().size() ); break;
                case SET:    fireContentsChanged( this, index, index + v.newValues().size() ); break;
                default:
                    fireContentsChanged( this, 0, _entries.size() );
            }
        }
    }

}
