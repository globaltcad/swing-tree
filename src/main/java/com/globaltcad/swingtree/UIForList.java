package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.ListEntryDelegate;
import com.globaltcad.swingtree.api.ListEntryRenderer;
import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.util.List;

public class UIForList<E, L extends JList<E>> extends UIForAbstractSwing<UIForList<E, L>, L>
{

    /**
     * Extensions of the {@link  UIForAbstractSwing} always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    UIForList(L component) { super(component); }

    /**
     *  Takes the provided list of entry objects and sets them as {@link JList} data.
     *
     * @param entries The list of entries to set as data.
     * @return This instance of the builder node.
     */
    public final UIForList<E, L> withEntries(List<E> entries) {
        getComponent().setListData((E[]) entries.toArray(new Object[0]));
        return this;
    }

    /**
     *  Takes the provided array of entry objects and sets them as {@link JList} data.
     *
     * @param entries The array of entries to set as data.
     * @return This instance of the builder node.
     */
    public final UIForList<E, L> withEntries(E... entries) {
        getComponent().setListData(entries.clone());
        return this;
    }

    public final UIForList<E, L> withRenderer(ListEntryRenderer<E, L> renderer) {
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
    public final UIForList<E, L> onSelection(UIAction<SimpleDelegate<JList<E>, ListSelectionEvent>> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        L list = getComponent();
        list.addListSelectionListener(e -> doApp(()->action.accept(new SimpleDelegate<>(list, e, ()->getSiblinghood()))) );
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
