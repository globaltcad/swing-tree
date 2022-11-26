package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.ListEntryDelegate;
import com.globaltcad.swingtree.api.ListEntryRenderer;
import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.util.List;

/**
 * A swing tree builder node for {@link JList} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <L> The type of the {@link JList} instance which will be wrapped by this builder node.
 */
public class UIForList<E, L extends JList<E>> extends UIForAbstractSwing<UIForList<E, L>, L>
{

    /**
     * Extensions of the {@link  UIForAbstractSwing} always wrap
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
                    public int getSize() { return entries.size(); }
                    public E getElementAt( int i ) { return entries.get( i ); }
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

    public final UIForList<E, L> withRenderer( ListEntryRenderer<E, L> renderer ) {
        getComponent().setCellRenderer((list, value, index, isSelected, cellHasFocus) -> renderer.render(new ListEntryDelegate<E, L>() {
            @Override public L list() { return (L) list; }
            @Override public E entry() { return value; }
            @Override public int index() { return index; }
            @Override public boolean isSelected() { return isSelected; }
            @Override public boolean hasFocus() { return cellHasFocus; }
        }));
        return this;
    }

    /**
     * Adds an {@link UIAction} to the underlying {@link JList}
     * through an {@link javax.swing.event.ListSelectionListener},
     * which will be called when a list selection has been made.
     * {@see JList#addListSelectionListener(ListSelectionListener)}.
     *
     * @param action The {@link UIAction} that will be notified.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForList<E, L> onSelection( UIAction<SimpleDelegate<JList<E>, ListSelectionEvent>> action ) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        L list = getComponent();
        list.addListSelectionListener(e -> _doApp(()->action.accept(new SimpleDelegate<>(list, e, ()->getSiblinghood()))) );
        return this;
    }

    public final <V extends E> UIForList<E, L> withRenderer( Render.Builder<L,V> renderBuilder ) {
        LogUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return withRenderer((ListCellRenderer<E>) renderBuilder.getForList());
    }

    public final UIForList<E, L> withRenderer( ListCellRenderer<E> renderer ) {
        getComponent().setCellRenderer(renderer);
        return this;
    }

}
