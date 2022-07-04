package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;

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
        _component.addListSelectionListener(e -> action.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())) );
        return this;
    }


}
