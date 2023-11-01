package swingtree;

import sprouts.Action;
import sprouts.From;
import sprouts.Var;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Consumer;

/**
 *  A SwingTree builder node designed for configuring {@link JComboBox} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 */
public final class UIForCombo<E,C extends JComboBox<E>> extends UIForAnySwing<UIForCombo<E,C>, JComboBox<E>>
{
    private final BuilderState<JComboBox<E>> _state;

    UIForCombo( BuilderState<JComboBox<E>> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<JComboBox<E>> _state() {
        return _state;
    }
    
    @Override
    protected UIForCombo<E,C> _with( BuilderState<JComboBox<E>> newState ) {
        return new UIForCombo<>(newState);
    }

    private void _bindComboModelToEditor( JComboBox<E> thisComponent, AbstractComboModel<E> model ) {
        Component editor = thisComponent.getEditor().getEditorComponent();
        if ( editor instanceof JTextField ) {
            JTextField field = (JTextField) editor;
            boolean[] comboIsOpen = {false};
            WeakReference<JComboBox<E>> weakCombo = new WeakReference<>(thisComponent);
            UI.of(field).onTextChange( it -> {
                JComboBox<E> strongCombo = weakCombo.get();
                if ( !comboIsOpen[0] && strongCombo != null && strongCombo.isEditable() )
                    model.setFromEditor(field.getText());
            });

            _onShow( model._getSelectedItemVar(), thisComponent, (c, v) ->
                model.doQuietly(()->{
                    c.getEditor().setItem(v);
                    model.fireListeners();
                })
            );

            // Adds a PopupMenu listener which will listen to notification
            // messages from the popup portion of the combo box.
            thisComponent.addPopupMenuListener(new PopupMenuListener() {
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
        return _with( thisComponent -> {
                    _onPopupOpen(thisComponent, e -> _doApp(()->action.accept(new ComponentDelegate<>( (C) thisComponent, e )) ) );
                })
                ._this();
    }

    private void _onPopupOpen( JComboBox<E> thisComponent, Consumer<PopupMenuEvent> consumer ) {
        thisComponent.addPopupMenuListener(new PopupMenuListener() {
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
        return _with( thisComponent -> {
                    _onPopupClose(thisComponent,
                        e -> _doApp(()->action.accept(new ComponentDelegate<>( (C) thisComponent, e )) )
                    );
                })
                ._this();
    }

    private void _onPopupClose( JComboBox<E> thisComponent, Consumer<PopupMenuEvent> consumer ) {
        thisComponent.addPopupMenuListener(new PopupMenuListener() {
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
        return _with( thisComponent -> {
                    _onPopupCancel(thisComponent,
                        e -> _doApp(()->action.accept(new ComponentDelegate<>( (C) thisComponent, e )) )
                    );
                })
                ._this();
    }

    private void _onPopupCancel( JComboBox<E> thisComponent, Consumer<PopupMenuEvent> consumer ) {
        thisComponent.addPopupMenuListener(new PopupMenuListener() {
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
        return _with( thisComponent -> {
                   _onSelection(thisComponent,
                       e -> _doApp(()->action.accept(new ComponentDelegate<>( thisComponent, e )))
                   );
               })
               ._this();
    }

    private void _onSelection( JComboBox<E> thisComponent, Consumer<ActionEvent> consumer ) {
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
        ActionListener[] listeners = thisComponent.getActionListeners();
        for (ActionListener listener : listeners)
            thisComponent.removeActionListener(listener);

        thisComponent.addActionListener(consumer::accept);

        for ( int i = listeners.length - 1; i >= 0; i-- ) // reverse order because swing does not give us the listeners in the order they were added!
            thisComponent.addActionListener(listeners[i]);
    }

    /**
     * Adds an {@link ActionListener} to the editor component of the underlying {@link JComboBox}
     * which will be called when a selection has been made. If the combo box is editable, then
     * an {@link ActionEvent} will be fired when editing has stopped.
     * For more information see {@link JComboBox#addActionListener(ActionListener)}.
     * <p>
     * @param action The {@link Action} that will be notified.
     * @return This very builder instance, which allows for method chaining.
     **/
    public UIForCombo<E,C> onEnter( Action<ComponentDelegate<C, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                   _onEnter(thisComponent, e -> _doApp(()->action.accept(new ComponentDelegate<>( (C) thisComponent, e ))) );
               })
               ._this();
    }

    private void _onEnter( JComboBox<E> thisComponent, Consumer<ActionEvent> consumer ) {
        Component editor = thisComponent.getEditor().getEditorComponent();
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
        return _with( thisComponent -> {
                   thisComponent.setEditable(isEditable);
               })
               ._this();
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
        NullUtil.nullPropertyCheck(isEditable, "isEditable", "Null is not a valid state for modelling 'isEditable''.");
        return _withOnShow( isEditable, (thisComponent, v) -> {
                    thisComponent.setEditable(v);
                })
                ._with( thisComponent -> {
                    thisComponent.setEditable(isEditable.get());
                })
                ._this();
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
        return _with( thisComponent -> {
                    thisComponent.setRenderer((ListCellRenderer<E>) renderBuilder.buildForCombo((C)thisComponent));
                })
                ._this();
    }

    /**
     * @param model The {@link ComboBoxModel} to be used for the combo box.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForCombo<E,C> withModel( ComboBoxModel<E> model ) {
        return _with( thisComponent -> {
                    if ( model instanceof AbstractComboModel )
                        _bindComboModelToEditor(thisComponent, (AbstractComboModel<E>) model );
                        thisComponent.setModel(model);
                })
                ._this();
    }

    /**
     * Sets the {@link ListCellRenderer} for the {@link JComboBox}, which renders the combo box items
     * by supplying a custom component for each item through the
     * {@link ListCellRenderer#getListCellRendererComponent(JList, Object, int, boolean, boolean)} method.
     * <p>
     * @param renderer The {@link ListCellRenderer} that will be used to paint each cell in the combo box.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForCombo<E,C> withRenderer( ListCellRenderer<E> renderer ) {
        return _with( thisComponent -> {
                    thisComponent.setRenderer(renderer);
                })
                ._this();
    }

    /**
     * @param item The item which should be set as the currently selected combo box item.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForCombo<E,C> withSelectedItem( E item ) {
        return _with( thisComponent -> {
                    _setSelectedItem(thisComponent, item);
               })
               ._this();
    }

    /**
     *  Use this to dynamically set the selected item of the combo box.
     *
     * @param item The item to be selected.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForCombo<E,C> withSelectedItem( Var<E> item ) {
        NullUtil.nullArgCheck(item, "item", Var.class);
        return _with( thisComponent -> {
                    ComboBoxModel<E> model = thisComponent.getModel();
                    if ( model instanceof AbstractComboModel )
                        withModel(((AbstractComboModel<E>)model).withVar(item));
                    else {
                        // The user has a custom model AND wants to bind to a property:
                        _onShow( item, thisComponent, (c,v) -> _setSelectedItem(c, v) );
                        _onSelection(thisComponent,
                            e -> _doApp( (E)thisComponent.getSelectedItem(), newItem -> item.set(From.VIEW, newItem)  )
                        );
                    }
                    _setSelectedItem(thisComponent, item.get());
                })
                ._this();
    }

    private void _setSelectedItem( JComboBox<E> thisComponent, E item ) {
        // Ok, so a combo box fires an event when the selection is changed programmatically.
        // This is a problem, because we don't want to trigger the action listener.
        // So we temporarily remove the action listener(s), and then add them back.
        // 1. Get the action listener(s)
        Component editor = thisComponent.getEditor().getEditorComponent();
        AbstractDocument abstractDocument = null;
        ActionListener[]   listeners    = thisComponent.getActionListeners();
        DocumentListener[] docListeners = {};
        if ( editor instanceof JTextField ) {
            JTextField field = (JTextField) editor;
            Document doc = field.getDocument();
            if ( doc instanceof AbstractDocument ) {
                abstractDocument = (AbstractDocument) doc;
                docListeners = ((AbstractDocument)doc).getDocumentListeners();
            }
        }

        // 2. Remove them
        for ( ActionListener listener : listeners )
            thisComponent.removeActionListener(listener);
        if ( abstractDocument != null ) {
            for (DocumentListener listener : docListeners) {
                abstractDocument.removeDocumentListener(listener);
            }
        }

        try {
            // 3. Set the selected item
            thisComponent.setSelectedItem(item);
            // 3.1 We make sure the editor also gets an update!
            thisComponent.getEditor().setItem(item);

        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }

        // 4. Add them back
        for ( ActionListener listener : listeners )
            thisComponent.addActionListener(listener);
        if ( abstractDocument != null ) {
            for (DocumentListener listener : docListeners) {
                abstractDocument.addDocumentListener(listener);
            }
        }
    }

}
