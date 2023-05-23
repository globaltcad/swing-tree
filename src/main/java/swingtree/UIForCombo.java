package swingtree;

import sprouts.Action;
import sprouts.Var;

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
public class UIForCombo<E,C extends JComboBox<E>> extends UIForAnySwing<UIForCombo<E,C>, JComboBox<E>>
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
                    model.doQuietly(()->{
                        combo.getEditor().setItem(v);
                        model.fireListeners();
                    });
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
    public UIForCombo<E,C> onOpen( Action<ComponentDelegate<C, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        _onPopupOpen( e -> _doApp(()->action.accept(new ComponentDelegate<>( (C) getComponent(), e, this::getSiblinghood )) ) );
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
    public UIForCombo<E,C> onClose( Action<ComponentDelegate<C, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        _onPopupClose( e -> _doApp(()->action.accept(new ComponentDelegate<>( (C) getComponent(), e, this::getSiblinghood )) ) );
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
    public UIForCombo<E,C> onCancel( Action<ComponentDelegate<C, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        _onPopupCancel( e -> _doApp(()->action.accept(new ComponentDelegate<>( (C) getComponent(), e, this::getSiblinghood )) ) );
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
     * Adds an {@link Action} to the underlying {@link JComboBox}
     * through an {@link java.awt.event.ActionListener},
     * which will be called when a selection has been made. If the combo box is editable, then
     * an {@link ActionEvent} will be fired when editing has stopped.
     * For more information see {@link JComboBox#addActionListener(ActionListener)}.
     *
     * @param action The {@link Action} that will be notified.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    public UIForCombo<E,C> onSelection( Action<ComponentDelegate<JComboBox<E>, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        JComboBox<E> combo = getComponent();
        _onSelection(e -> _doApp(()->action.accept(new ComponentDelegate<>( combo, e, this::getSiblinghood ))) );
        return this;
    }

    private void _onSelection( Consumer<ActionEvent> consumer ) {
        /*
            When an action event is fired, Swing will go through all the listeners
            from the most recently added to the first added. This means that if we simply add
            a listener through the "addActionListener" method, we will be the last to be notified.
            This is problematic because it is built on the assumption that the last listener
            added is more interested in the event than the first listener added.
            This however is an unintuitive assumption, meaning a user would expect
            the first listener added to be the most interested in the event
            simply because it was added first.
            This is especially true in the context of declarative UI design.
        */
        ActionListener[] listeners = getComponent().getActionListeners();
        for (ActionListener listener : listeners)
            getComponent().removeActionListener(listener);

        getComponent().addActionListener(consumer::accept);

        for ( int i = listeners.length - 1; i >= 0; i-- ) // reverse order because swing does not give us the listeners in the order they were added!
            getComponent().addActionListener(listeners[i]);
    }

    /**
     * Adds an {@link ActionListener} to the editor component of the underlying {@link JComboBox}
     * which will be called when a selection has been made. If the combo box is editable, then
     * an {@link ActionEvent} will be fired when editing has stopped.
     * For more information see {@link JComboBox#addActionListener(ActionListener)}.
     * <p>
     * @param action The {@link Action} that will be notified.
     **/
    public UIForCombo<E,C> onEnter( Action<ComponentDelegate<C, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        C combo = (C) getComponent();
        _onEnter(e -> _doApp(()->action.accept(new ComponentDelegate<>( combo, e, this::getSiblinghood ))) );
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

    /**
     *  Pass a {@link Render.Builder} to this method to customize the rendering of the combo box.
     *
     * @param renderBuilder The {@link Render.Builder} to be used for customizing the rendering of the combo box.
     * @return This very instance, which enables builder-style method chaining.
     * @param <V> The type of the value to be rendered.
     */
    public final <V extends E> UIForCombo<E,C> withRenderer( Render.Builder<C,V> renderBuilder ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return withRenderer((ListCellRenderer<E>) renderBuilder.getForCombo());
    }

    /**
     * @param model The {@link ComboBoxModel} to be used for the combo box.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForCombo<E,C> withModel( ComboBoxModel<E> model ) {
        if ( model instanceof AbstractComboModel )
            _bindComboModelToEditor((AbstractComboModel<E>) model);
        getComponent().setModel(model);
        return this;
    }

    public final UIForCombo<E,C> withRenderer( ListCellRenderer<E> renderer ) {
        getComponent().setRenderer(renderer);
        return this;
    }

    /**
     *  Use this to dynamically set the selected item of the combo box.
     *
     * @param item The item to be selected.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForCombo<E,C> withSelectedItem( Var<E> item ) {
        NullUtil.nullArgCheck(item, "item", Var.class);
        ComboBoxModel<E> model = getComponent().getModel();
        if ( model instanceof AbstractComboModel )
            withModel(((AbstractComboModel<E>)model).withVar(item));
        else {
            // The user has a custom model AND wants to bind to a property:
            _onShow( item, this::_setSelectedItem );
            _onSelection(
                e -> _doApp( (E)getComponent().getSelectedItem(), item::act )
            );
        }
        return withSelectedItem(item.get());
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
