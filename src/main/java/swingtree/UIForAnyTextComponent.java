package swingtree;


import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Action;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;
import swingtree.style.ComponentExtension;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.Font;
import java.awt.TextComponent;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

/**
 *  A SwingTree builder designed for configuring various kinds of {@link JTextComponent} instances
 *  in a fluent and declarative way. It also allows for the binding of text properties to the text component
 *  so that the text of the text component is dynamically updated whenever the value of the property changes
 *  and conversely, the value of the property is dynamically updated whenever the text of the text component changes.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 *  @param <C> The type parameter for the text component type which is being built by this builder.
 *  @param <I> The type parameter for the instance type of this concrete class itself.
 */
public abstract class UIForAnyTextComponent<I, C extends JTextComponent> extends UIForAnySwing<I, C>
{
    private static final Logger log = LoggerFactory.getLogger(UIForAnyTextComponent.class);

    /**
     * Sets the text of the wrapped <code>{@link TextComponent}</code>
     * to the specified text. If the text is <code>null</code>
     * an exception is thrown. Please use an empty string instead of null!
     * When text has been inserted, the resulting caret location
     * is determined by the implementation of the caret class.
     *
     * <p>
     * Note that text is not a bound property, so no {@link java.beans.PropertyChangeEvent}
     * is fired when it changes. To listen for changes to the text,
     * register action lambdas through {@link #onTextChange(Action)} or
     * use {@link DocumentListener} directly.
     * </p>
     *
     * @param text The new text to be set for the wrapped text component type.
     * @return This very builder to allow for method chaining.
     */
    public final I withText( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class, "Please use an empty string instead of null!");
        return _with( thisComponent -> {
                    _setTextSilently( thisComponent, text );
                })
                ._this();
    }

    /**
     * Binds the text of the wrapped <code>{@link TextComponent}</code> to
     * the specified {@link Val} property instance so that the text of the wrapped
     * text component is dynamically updated whenever the value of the property changes.
     * <p>
     *     Note that the text of the wrapped text component is only updated if the new value
     *     is different from the old value. This is to avoid infinite feedback loops.
     * <br>
     * @param text The property instance to bind the text of the wrapped text component to.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if the specified property is <code>null</code>.
     * @throws IllegalArgumentException if the specified property allows <code>null</code> values.
     */
    public final I withText( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return _withOnShow( text, (c, t) -> {
                    _setTextSilently( c, t );
                })
                ._with( thisComponent -> {
                    _setTextSilently( thisComponent, text.orElseThrowUnchecked() );
                })
                ._this();
    }

    /**
     *  Binds the text of the wrapped <code>{@link TextComponent}</code> to
     *  the specified {@link Val} property instance so that the text of the wrapped
     *  text component is dynamically updated whenever the value of the property changes.
     *  <p>
     *  This method is the same as {@link #withText(Val)} except that the {@link Var}
     *  property is used instead of the {@link Val} property which allows for the
     *  text of the wrapped text component to be changed by the user.
     *
     * @param text The property instance to bind the text of the wrapped text component to.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if the specified property is <code>null</code>.
     * @throws IllegalArgumentException if the specified property allows <code>null</code> values.
     */
    public final I withText( Var<String> text ) {
        NullUtil.nullPropertyCheck(text, "text", "Use an empty string instead of null!");
        return _withOnShow( text, (c, t) -> {
                    if ( !Objects.equals(c.getText(), t) )  // avoid infinite recursion or some other Swing weirdness
                        _setTextSilently( c, t );
                })
                ._with( thisComponent -> {
                    _onTextChange(thisComponent, e -> {
                        try {
                            String newText = e.getDocument().getText(0, e.getDocument().getLength());
                            _runInApp(newText, t -> {
                                if ( UI.thisIsUIThread() )
                                    UI.runLater( () -> {
                                        try {
                                            text.set(From.VIEW, e.getDocument().getText(0, e.getDocument().getLength()));
                                        } catch (BadLocationException ex) {
                                            throw new RuntimeException(ex);
                                        }
                                    }); // avoid attempt to mutate in notification
                                    /*
                                        We apply the text to the property in the next EDT cycle,
                                        which is important to avoid mutating the property in a notification.
                                        Because if a user decides to rebroadcast the text property in the 'onAct' callback,
                                        then the text component will receive that new text while it is still in the middle of
                                        document mutation, which is not allowed by Swing!
                                        (java.lang.IllegalStateException: Attempt to mutate in notification
                                            at javax.swing.text.AbstractDocument.writeLock(AbstractDocument.java:1338))
                                    */
                                else
                                    text.set(From.VIEW, t);
                            });
                        } catch (BadLocationException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    _setTextSilently( thisComponent, text.orElseThrowUnchecked() );
                })
                ._this();
    }

    protected final void _setTextSilently( C thisComponent, String text ) {
        Document doc = thisComponent.getDocument();
        if (doc instanceof AbstractDocument) {
            AbstractDocument abstractDoc = (AbstractDocument) doc;
            // We remove all document listeners to avoid infinite recursion
            // and other Swing weirdness.
            DocumentListener[] listeners = abstractDoc.getListeners(DocumentListener.class);
            for ( DocumentListener listener : listeners )
                abstractDoc.removeDocumentListener(listener);

            thisComponent.setText(text);

            for ( DocumentListener listener : listeners )
                abstractDoc.addDocumentListener(listener);

            thisComponent.repaint(); // otherwise the text is not updated until the next repaint
        }
        else
            thisComponent.setText(text);
    }

    /**
     *  Use this to set the font of the wrapped {@link JTextComponent}.
     * @param font The font of the text which should be displayed on the text component.
     * @return This builder instance, to allow for method chaining.
     * @throws IllegalArgumentException if {@code font} is {@code null}.
     */
    public final I withFont( Font font ) {
        NullUtil.nullArgCheck(font, "font", Font.class);
        return _with( thisComponent -> {
                    if ( _isUndefinedFont(font) )
                        thisComponent.setFont(null);
                    else
                        thisComponent.setFont(font);
                })
                ._this();
    }

    /**
     *  Use this to dynamically set the font of the wrapped {@link JTextComponent}
     *  through the provided view model property.
     *  When the font wrapped by the provided property changes,
     *  then so does the font of this text component.
     *
     * @param font The font property of the text which should be displayed on the text component.
     * @return This builder instance, to allow for method chaining.
     * @throws IllegalArgumentException if {@code font} is {@code null}.
     * @throws IllegalArgumentException if {@code font} is a property which can wrap {@code null}.
     */
    public final I withFont( Val<Font> font ) {
        NullUtil.nullArgCheck(font, "font", Val.class);
        NullUtil.nullPropertyCheck(font, "font", "Use the default font of this component instead of null!");
        return _withOnShow( font, (c,v) -> {
                    if ( _isUndefinedFont(v) )
                        c.setFont(null);
                    else
                        c.setFont(v);
                })
                ._with( thisComponent -> {
                    Font newFont = font.orElseThrowUnchecked();
                    if ( _isUndefinedFont(newFont) )
                        thisComponent.setFont( null );
                    else
                        thisComponent.setFont( newFont );
                })
                ._this();

    }

    /**
     *  Use this to modify the components' modifiability.
     *
     * @param isEditable The flag determining if the underlying {@link JTextComponent} should be editable or not.
     * @return This very builder to allow for method chaining.
     */
    public final I isEditableIf( boolean isEditable ) {
        return _with( thisComponent -> {
                    thisComponent.setEditable(isEditable);
                })
                ._this();
    }


    /**
     *  Use this to register any change in the contents of the text component including both
     *  the displayed text and its attributes.
     *
     * @param action An action which will be executed when the text or its attributes in the underlying {@link JTextComponent} changes.
     * @return This very builder to allow for method chaining.
     */
    public final I onContentChange( Action<ComponentDelegate<JTextComponent, DocumentEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    thisComponent.getDocument().addDocumentListener(new DocumentListener() {
                        @Override public void insertUpdate(DocumentEvent e)  {
                            _runInApp(()->{
                                try {
                                    action.accept(new ComponentDelegate<>(thisComponent, e));
                                } catch (Exception ex) {
                                    log.error("Error while executing action on text component content change!", ex);
                                }
                            });
                        }
                        @Override public void removeUpdate(DocumentEvent e)  {
                            _runInApp(()->{
                                try {
                                    action.accept(new ComponentDelegate<>(thisComponent, e));
                                } catch (Exception ex) {
                                    log.error("Error while executing action on text component content change!", ex);
                                }
                            });
                        }
                        @Override public void changedUpdate(DocumentEvent e) {
                            _runInApp(()->{
                                try {
                                    action.accept(new ComponentDelegate<>(thisComponent, e));
                                } catch (Exception ex) {
                                    log.error("Error while executing action on text component content change!", ex);
                                }
                            });
                        }
                    });
                })
                ._this();
    }

    /**
     *  Use this to register if the text in this text component changes.
     *  This does not include style attributes like font size.
     *
     * @param action An action which will be executed when the text string in the underlying {@link JTextComponent} changes.
     * @return This very builder to allow for method chaining.
     */
    public final I onTextChange( Action<ComponentDelegate<JTextComponent, DocumentEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    _onTextChange(thisComponent, e -> _runInApp( () -> {
                        try {
                            action.accept(new ComponentDelegate<>(thisComponent, e));
                        } catch (Exception ex) {
                            log.error("Error while executing action on text change!", ex);
                        }
                    }));
                })
                ._this();
    }

    protected final void _onTextChange( C thisComponent, Consumer<DocumentEvent> action ) {
        thisComponent.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.accept(e); }
            @Override public void removeUpdate(DocumentEvent e) { action.accept(e); }
            @Override public void changedUpdate(DocumentEvent e) {}
        });
    }

    /**
     * @param thisComponent The component which is wrapped by this builder.
     * @param action        An action which will be executed in case the underlying
     *                      component supports text filtering (The underlying document is an {@link AbstractDocument}).
     */
    private void _ifFilterable( C thisComponent, Runnable action ) {
        if ( thisComponent.getDocument() instanceof AbstractDocument ) {
            ExtraState state = ExtraState.of( thisComponent );
            action.run();
            AbstractDocument doc = (AbstractDocument) thisComponent.getDocument();
            doc.setDocumentFilter(new DocumentFilter() {
                /**
                 * See documentation in {@link DocumentFilter}!
                 */
                @Override
                public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                    state.removes.forEach(action -> {
                        try {
                            action.accept(new TextRemoveDelegate(thisComponent, fb, offset, length));
                        } catch (Exception e) {
                            log.error("Error while executing action on text remove!", e);
                        }
                    });
                    if ( state.removes.isEmpty() ) fb.remove(offset, length);
                }
                /**
                 * See documentation in {@link DocumentFilter}!
                 */
                @Override
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                    state.inserts.forEach(action -> {
                        try {
                            action.accept(new TextInsertDelegate(thisComponent, fb, offset, string.length(), string, attr));
                        } catch (Exception e) {
                            log.error("Error while executing action on text insert!", e);
                        }
                    });
                    if ( state.inserts.isEmpty() ) fb.insertString(offset, string, attr);
                }
                /**
                 * See documentation in {@link DocumentFilter}!
                 */
                @Override
                public void replace(FilterBypass fb, int offset, int length, @Nullable String text, AttributeSet attrs) throws BadLocationException {
                    state.replaces.forEach(action -> {
                        try {
                            action.accept(new TextReplaceDelegate(thisComponent, fb, offset, length, text, attrs));
                        } catch (Exception e) {
                            log.error("Error while executing action on text replace!", e);
                        }
                    });
                    if ( state.replaces.isEmpty() ) fb.replace(offset, length, text, attrs);
                }
            });
        }
    }

    /**
     *  Allows you to register a user action listener which will be called
     *  whenever parts (or all) of the text in the underlying text component gets removed.
     *  This event is based on the {@link DocumentFilter#remove(DocumentFilter.FilterBypass, int, int)}
     *  method of the underlying {@link AbstractDocument}.
     *
     * @param action A {@link Action} lambda which will be called when parts (or all) of the text in
     *               the underlying text component gets removed.
     *
     * @return This very builder to allow for method chaining.
     */
    public final I onTextRemove( Action<TextRemoveDelegate> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    ExtraState state = ExtraState.of( thisComponent );
                    _ifFilterable(thisComponent, () -> state.removes.add(action));
               })
               ._this();
    }

    /**
     *  Allows you to register a user action listener which will be called
     *  whenever new text gets inserted into the underlying text component.
     *  This event is based on the
     *  {@link DocumentFilter#insertString(DocumentFilter.FilterBypass, int, String, AttributeSet)}
     *  method of the underlying {@link AbstractDocument}.
     *  Use the {@link TextInsertDelegate} that is supplied to your action to
     *  get access to the underlying text component and the text insertion details.
     *
     * @param action A {@link Action} lambda which will be called when new text gets inserted
     *               into the underlying text component.
     *
     * @return This very builder to allow for method chaining.
     */
    public final I onTextInsert( Action<TextInsertDelegate> action ) {
        return _with( thisComponent -> {
                    ExtraState state = ExtraState.of( thisComponent );
                    _ifFilterable(thisComponent, () -> state.inserts.add(action));
               })
               ._this();
    }

    /**
     *  This method allows you to register a user action which will be called
     *  whenever the text in the underlying text component gets replaced.
     *  This event is based on the
     *  {@link DocumentFilter#replace(DocumentFilter.FilterBypass, int, int, String, AttributeSet)}
     *  method of the underlying {@link AbstractDocument}.
     *  Use the {@link TextReplaceDelegate} that is supplied to your action to
     *  get access to the underlying text component and the text replacement details.
     *
     * @param action A {@link Action} lambda which will be called when the text in
     *               the underlying text component gets replaced.
     *
     * @return This very builder to allow for method chaining.
     */
    public final I onTextReplace( Action<TextReplaceDelegate> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    ExtraState state = ExtraState.of( thisComponent );
                    _ifFilterable(thisComponent, () -> state.replaces.add(action));
               })
               ._this();
    }


    private static class ExtraState
    {
        static ExtraState of( JTextComponent textComponent ) {
            return ComponentExtension.from(textComponent)
                                     .getOrSet(ExtraState.class, ExtraState::new);
        }

        final java.util.List<Action<TextRemoveDelegate>>   removes = new ArrayList<>();
        final java.util.List<Action<TextInsertDelegate>>   inserts = new ArrayList<>();
        final java.util.List<Action<TextReplaceDelegate>>  replaces = new ArrayList<>();
    }

}

