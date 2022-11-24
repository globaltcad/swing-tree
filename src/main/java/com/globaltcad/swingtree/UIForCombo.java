package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.api.mvvm.Var;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
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
    }

    private void _bindComboModelToEditor( AbstractComboModel<E> model ) {
        Component editor = getComponent().getEditor().getEditorComponent();
        if ( editor instanceof JTextField ) {
            JTextField field = (JTextField) editor;
            JComboBox<E> comboBox = getComponent();
            boolean[] comboIsOpen = {false};
            UI.of(field).onTextChange( it -> {
                if ( !comboIsOpen[0] && comboBox.isEditable() )
                    model.setFromEditor(field.getText());
            });

            model.onSelectedItemShow( v -> {
                if ( comboBox.isEditable() )
                    comboBox.getEditor().setItem(v);
            });

            // Adds a PopupMenu listener which will listen to notification
            // messages from the popup portion of the combo box.
            comboBox.addPopupMenuListener(new PopupMenuListener() {
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    // This method is called before the popup menu becomes visible.
                    comboIsOpen[0] = true;
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    // This method is called before the popup menu becomes invisible
                    comboIsOpen[0] = false;
                }

                public void popupMenuCanceled(PopupMenuEvent e) {
                    // This method is called when the popup menu is canceled
                    comboIsOpen[0] = false;
                }
            });

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

    public final UIForCombo<E,C> withModel(ComboBoxModel<E> model) {
        if ( model instanceof AbstractComboModel )
            _bindComboModelToEditor((AbstractComboModel<E>) model);
        getComponent().setModel(model);
        return this;
    }

    public final UIForCombo<E,C> withRenderer( ListCellRenderer<E> renderer ) {
        getComponent().setRenderer(renderer);
        return this;
    }

    public final UIForCombo<E,C> withSelectedItem( Var<E> var ) {
        LogUtil.nullArgCheck(var, "var", Var.class);
        ComboBoxModel<E> model = getComponent().getModel();
        if ( model instanceof AbstractComboModel )
            withModel(((AbstractComboModel<E>)model).withVar(var));
        else {
            // The user has a custom model AND wants to bind to a property:
            var.onShow(v-> _doUI(()-> _setSelectedItem(v)));
            _onSelection(
                e -> _doApp((E)getComponent().getSelectedItem(), sel->var.act(sel))
            );
        }
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
        // 3.1 We make sure the editor also gets an update!
        getComponent().getEditor().setItem(item);

        // 4. Add them back
        for ( ActionListener listener : listeners )
            getComponent().addActionListener(listener);
    }

}
