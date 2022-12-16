package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.api.mvvm.Var;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
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
            boolean[] comboIsOpen = {false};
            WeakReference<JComboBox<E>> weakCombo = new WeakReference<>(getComponent());
            UI.of(field).onTextChange( it -> {
                JComboBox<E> strongCombo = weakCombo.get();
                if ( !comboIsOpen[0] && strongCombo != null && strongCombo.isEditable() )
                    model.setFromEditor(field.getText());
            });

            _onShow( model._getSelectedItemVar(), v ->
                component().ifPresent( combo -> {
                    if ( combo.isEditable() )
                        combo.getEditor().setItem(v);
                })
            );

            // Adds a PopupMenu listener which will listen to notification
            // messages from the popup portion of the combo box.
            getComponent().addPopupMenuListener(new PopupMenuListener() {
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
     *  Registers a listener to be notified when the combo box is opened,
     *  meaning its popup menu is shown after the user clicks on the combo box.
     *
     * @param action the action to be executed when the combo box is opened.
     * @return this
     */
    public UIForCombo<E,C> onOpen( UIAction<SimpleDelegate<C, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        _onPopupOpen( e -> _doApp(()->action.accept(new SimpleDelegate<>( (C) getComponent(), e, this::getSiblinghood )) ) );
        return this;
    }

    private void _onPopupOpen( Consumer<PopupMenuEvent> consumer ) {
        getComponent().addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // This method is called before the popup menu becomes visible.
                consumer.accept(e);
            }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuCanceled(PopupMenuEvent e) {/* Not relevant here */}
        });
    }

    /**
     *  Registers a listener to be notified when the combo box is closed,
     *  meaning its popup menu is hidden after the user clicks on the combo box.
     *
     * @param action the action to be executed when the combo box is closed.
     * @return this
     */
    public UIForCombo<E,C> onClose( UIAction<SimpleDelegate<C, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        _onPopupClose( e -> _doApp(()->action.accept(new SimpleDelegate<>( (C) getComponent(), e, this::getSiblinghood )) ) );
        return this;
    }

    private void _onPopupClose( Consumer<PopupMenuEvent> consumer ) {
        getComponent().addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                consumer.accept(e); // This method is called before the popup menu becomes invisible
            }
            public void popupMenuCanceled(PopupMenuEvent e) {/* Not relevant here */}
        });
    }

    /**
     *  Registers a listener to be notified when the combo box is canceled,
     *  meaning its popup menu is hidden which
     *  typically happens when the user clicks outside the combo box.
     *
     * @param action the action to be executed when the combo box is canceled.
     * @return this
     */
    public UIForCombo<E,C> onCancel( UIAction<SimpleDelegate<C, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        _onPopupCancel( e -> _doApp(()->action.accept(new SimpleDelegate<>( (C) getComponent(), e, this::getSiblinghood )) ) );
        return this;
    }

    private void _onPopupCancel( Consumer<PopupMenuEvent> consumer ) {
        getComponent().addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuCanceled(PopupMenuEvent e) {
                consumer.accept(e); // This method is called when the popup menu is canceled
            }
        });
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
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    public UIForCombo<E,C> onSelection( UIAction<SimpleDelegate<JComboBox<E>, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        JComboBox<E> combo = getComponent();
        _onSelection(e -> _doApp(()->action.accept(new SimpleDelegate<>( combo, e, this::getSiblinghood ))) );
        return this;
    }

    private void _onSelection( Consumer<ActionEvent> consumer ) {
        getComponent().addActionListener(consumer::accept);
    }

    /**
     * Adds an {@link ActionListener} to the editor component of the underlying {@link JComboBox}
     * which will be called when a selection has been made. If the combo box is editable, then
     * an {@link ActionEvent} will be fired when editing has stopped.
     * For more information see {@link JComboBox#addActionListener(ActionListener)}.
     * <p>
     * @param action The {@link UIAction} that will be notified.
     **/
    public UIForCombo<E,C> onEnter( UIAction<SimpleDelegate<C, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        C combo = (C) getComponent();
        _onEnter(e -> _doApp(()->action.accept(new SimpleDelegate<>( combo, e, this::getSiblinghood ))) );
        return this;
    }

    private void _onEnter( Consumer<ActionEvent> consumer ) {
        Component editor = getComponent().getEditor().getEditorComponent();
        if ( editor instanceof JTextField ) {
            JTextField field = (JTextField) editor;
            UI.of(field).onEnter( it -> consumer.accept(it.getEvent()) );
        }
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
     * @throws IllegalArgumentException if {@code isEditable} is {@code null}.
     */
    public UIForCombo<E,C> isEditableIf( Var<Boolean> isEditable ) {
        _onShow( isEditable, this::isEditableIf );
        return this;
    }

    public final <V extends E> UIForCombo<E,C> withRenderer( Render.Builder<C,V> renderBuilder ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return withRenderer((ListCellRenderer<E>) renderBuilder.getForCombo());
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
        NullUtil.nullArgCheck(var, "var", Var.class);
        ComboBoxModel<E> model = getComponent().getModel();
        if ( model instanceof AbstractComboModel )
            withModel(((AbstractComboModel<E>)model).withVar(var));
        else {
            // The user has a custom model AND wants to bind to a property:
            _onShow( var, this::_setSelectedItem );
            _onSelection(
                e -> _doApp( (E)getComponent().getSelectedItem(), var::act )
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
