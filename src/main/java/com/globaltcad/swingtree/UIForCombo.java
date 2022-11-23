package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.api.mvvm.Var;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 *  A swing tree builder node for {@link JComboBox} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 */
public class UIForCombo<E,C extends JComboBox<E>> extends UIForAbstractSwing<UIForCombo<E,C>, JComboBox<E>>
{
    protected UIForCombo( JComboBox<E> component ) {
        super(component);
        if ( component.getModel() instanceof AbstractComboModel )
            _bindComboModelToEditor((AbstractComboModel<E>) component.getModel());

    }

    private void _bindComboModelToEditor(AbstractComboModel<E> model ) {
        Component editor = getComponent().getEditor().getEditorComponent();
        if ( editor instanceof JTextField ) {
            JTextField field = (JTextField) editor;
            UI.of(field).onTextChange( it -> model.setFromEditor(field.getText()) );
        }
    }



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
    public UIForCombo<E,C> onSelection( UIAction<SimpleDelegate<JComboBox<E>, ActionEvent>> action ) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        JComboBox<E> combo = getComponent();
        _onSelection(e -> _doApp(()->action.accept(new SimpleDelegate<>(combo, e, ()->getSiblinghood()))) );
        return this;
    }

    private void _onSelection( Consumer<ActionEvent> consumer ) {
        getComponent().addActionListener(consumer::accept);
    }

    /**
     *  Use this to enable or disable editing for the wrapped UI component.
     *
     * @param isEditable The truth value determining if the UI component should be editable or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForCombo<E,C> isEditableIf( boolean isEditable ) {
        getComponent().setEditable(isEditable);
        return this;
    }

    /**
     *  Use this to enable or disable editing of the wrapped UI component
     *  through property binding dynamically.
     *
     * @param isEditable The boolean property determining if the UI component should be editable or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForCombo<E,C> isEditableIf( Var<Boolean> isEditable ) {
        isEditable.onShow(v -> _doUI(() -> isEditableIf(v)));
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

    public final UIForCombo<E,C> withSelectedItem( Var<E> var ) {
        LogUtil.nullArgCheck(var, "var", Var.class);
        var.onShow(v-> _doUI(()-> _setSelectedItem(v)));
        _onSelection(
            e -> _doApp((E)getComponent().getSelectedItem(), sel->var.act(sel))
        );
        return withSelectedItem(var.get());
    }

    public final UIForCombo<E,C> withSelectedItem( E item ) {
        _setSelectedItem(item);
        return this;
    }

    private void _setSelectedItem( E item ) {
        // Ok, so a combo box fires an event when the selection is changed programmatically.
        // This is a problem, because we don't want to trigger the action listener.
        // So we temporarily remove the action listener(s), and then add them back.
        // 1. Get the action listener(s)
        ActionListener[] listeners = getComponent().getActionListeners();
        // 2. Remove them
        for ( ActionListener listener : listeners )
            getComponent().removeActionListener(listener);
        // 3. Set the selected item
        getComponent().setSelectedItem(item);
        // 4. Add them back
        for ( ActionListener listener : listeners )
            getComponent().addActionListener(listener);
    }

}
