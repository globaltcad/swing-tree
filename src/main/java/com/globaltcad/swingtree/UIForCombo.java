package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *  A swing tree builder node for {@link JComboBox} instances.
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
        _component.addActionListener(e -> doApp(()->action.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood()))) );
        return this;
    }

    /**
     *  Use this to enable or disable editing for the wrapped UI component.
     *
     * @param isEditable The truth value determining if the UI component should be editable or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForCombo<E,C> isEditableIf(boolean isEditable) {
        _component.setEditable(isEditable);
        return this;
    }

    public final <V extends E> UIForCombo<E,C> withRenderer( Render.Builder<C,V> renderBuilder ) {
        LogUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return withRenderer((ListCellRenderer<E>) renderBuilder.getForList());
    }

    public final UIForCombo<E,C> withRenderer( ListCellRenderer<E> renderer ) {
        _component.setRenderer(renderer);
        return this;
    }

}
