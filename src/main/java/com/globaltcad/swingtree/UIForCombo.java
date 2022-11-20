package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *  A swing tree builder node for {@link JComboBox} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 */
public class UIForCombo<E,C extends JComboBox<E>> extends UIForAbstractSwing<UIForCombo<E,C>, JComboBox<E>>
{
    protected UIForCombo(JComboBox<E> component) { super(component); }

    /**
     * Adds an {@link UIAction} to the underlying {@link JComboBox}
     * through an {@link java.awt.event.ActionListener},
     * which will be called when a selection has been made. If the combo box is editable, then
     * an {@link ActionEvent} will be fired when editing has stopped.
     * For more information see {@link JComboBox#addActionListener(ActionListener)}.
     *
     * @param action The {@link UIAction} that will be notified.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForCombo<E,C> onSelection(UIAction<SimpleDelegate<JComboBox<E>, ActionEvent>> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        JComboBox<E> combo = getComponent();
        combo.addActionListener(e -> _doApp(()->action.accept(new SimpleDelegate<>(combo, e, ()->getSiblinghood()))) );
        return this;
    }

    /**
     *  Use this to enable or disable editing for the wrapped UI component.
     *
     * @param isEditable The truth value determining if the UI component should be editable or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForCombo<E,C> isEditableIf(boolean isEditable) {
        getComponent().setEditable(isEditable);
        return this;
    }

    public final <V extends E> UIForCombo<E,C> withRenderer( Render.Builder<C,V> renderBuilder ) {
        LogUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return withRenderer((ListCellRenderer<E>) renderBuilder.getForList());
    }

    public final UIForCombo<E,C> withRenderer( ListCellRenderer<E> renderer ) {
        getComponent().setRenderer(renderer);
        return this;
    }

}
