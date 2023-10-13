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
import java.util.Optional;

/**
 * A swing tree builder node for {@link JList} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <L> The type of the {@link JList} instance which will be wrapped by this builder node.
 */
public class UIForList<E, L extends JList<E>> extends UIForAnySwing<UIForList<E, L>, L>
{

    /**
     * Extensions of the {@link  UIForAnySwing} always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    UIForList( L component ) { super(component); }

    /**
     *  Takes the provided list of entry objects and sets them as {@link JList} data.
     *
     * @param entries The list of entries to set as data.
     * @return This instance of the builder node.
     */
    public final UIForList<E, L> withEntries( List<E> entries ) {
        getComponent().setModel (
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
            );
        return this;
    }

    /**
     *  Takes the provided array of entry objects and sets them as {@link JList} data.
     *
     * @param entries The array of entries to set as data.
     * @return This instance of the builder node.
     */
    public final UIForList<E, L> withEntries( E... entries ) {
        getComponent().setListData(entries);
        return this;
    }

    public final UIForList<E, L> withEntries( Vals<E> entries ) {
        ValsListModel<E> model = new ValsListModel<>(entries);
        getComponent().setModel(model);
        _onShow( entries, v -> model.fire(v) );
        return this;
    }

    public final UIForList<E, L> withSelection( Var<E> selection ) {
        getComponent().addListSelectionListener( e -> {
            if ( !e.getValueIsAdjusting() )
                selection.set(From.VIEW,  getComponent().getSelectedValue() );
        });
        _onShow( selection, v -> getComponent().setSelectedValue( v, true ) );
        return this;
    }

    public final UIForList<E, L> withSelection( Val<E> selection ) {
        _onShow( selection, v -> getComponent().setSelectedValue( v, true ) );
        return this;
    }


    private static class ValsListModel<E> extends AbstractListModel<E> {

        private final Vals<E> _entries;

        public ValsListModel( Vals<E> entries ) {
            _entries = entries;
        }

        @Override public int getSize() { return _entries.size(); }
        @Override public E getElementAt( int i ) { return _entries.at( i ).orElseNull(); }

        public void fire(ValsDelegate<E> v) {
            switch ( v.changeType() ) {
                case ADD:    fireIntervalAdded( this, v.index(), v.index() ); break;
                case REMOVE: fireIntervalRemoved( this, v.index(), v.index() ); break;
                case SET:    fireContentsChanged( this, v.index(), v.index() ); break;
                default:
                    fireContentsChanged( this, 0, _entries.size() );
            }
        }
    }

    /**
     *  The {@link ListEntryRenderer} passed to this method is a functional interface
     *  receiving a {@link ListEntryDelegate} instance which is
     *  used to render each entry of the {@link JList} instance.
     *
     * @param renderer The renderer to use for each entry of the {@link JList} instance.
     * @return This instance of the builder node.
     */
    public final UIForList<E, L> withRenderer( ListEntryRenderer<E, L> renderer ) {
        getComponent().setCellRenderer((list, value, index, isSelected, cellHasFocus) -> renderer.render(new ListEntryDelegate<E, L>() {
            @Override public L list() { return (L) list; }
            @Override public Optional<E> entry() { return Optional.ofNullable(value); }
            @Override public int index() { return index; }
            @Override public boolean isSelected() { return isSelected; }
            @Override public boolean hasFocus() { return cellHasFocus; }
        }));
        return this;
    }

    /**
     * Adds an {@link Action} to the underlying {@link JList}
     * through an {@link javax.swing.event.ListSelectionListener},
     * which will be called when a list selection has been made.
     * {see JList#addListSelectionListener(ListSelectionListener)}.
     *
     * @param action The {@link Action} that will be notified.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForList<E, L> onSelection( Action<ComponentDelegate<JList<E>, ListSelectionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        L list = getComponent();
        list.addListSelectionListener(e -> _doApp(()->action.accept(new ComponentDelegate<>(list, e, ()->getSiblinghood()))) );
        return this;
    }

    /**
     * Receives a {@link Render.Builder} instance providing
     * a fluent API for configuring how the values of this {@link JList} instance
     * should be rendered.
     * <p>
     * @param renderBuilder The {@link Render.Builder} instance.
     * @return This very instance, which enables builder-style method chaining.
     * @param <V> The type of the values that will be rendered.
     */
    public final <V extends E> UIForList<E, L> withRenderer( Render.Builder<L,V> renderBuilder ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return withRenderer((ListCellRenderer<E>) renderBuilder.buildForList(getComponent()));
    }

    /**
     * Sets the Swing internal delegate that is used to paint each cell in the list.
     * <p>
     * @param renderer The {@link ListCellRenderer} that will be used to paint each cell in the list.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForList<E, L> withRenderer( ListCellRenderer<E> renderer ) {
        getComponent().setCellRenderer(renderer);
        return this;
    }

}
