package swingtree;


import sprouts.Action;
import sprouts.Val;
import sprouts.Var;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

/**
 *  A swing tree builder node for {@link JTextComponent} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 */
public abstract class UIForAbstractTextComponent<I, C extends JTextComponent> extends UIForAbstractSwing<I, C>
{
    private final java.util.List<Action<RemoveDelegate>>   removes = new ArrayList<>();
    private final java.util.List<Action<InsertDelegate>>   inserts = new ArrayList<>();
    private final java.util.List<Action<ReplaceDelegate>>  replaces = new ArrayList<>();

    /**
     *  A custom document filter which is simply a lambda-rization wrapper which ought to make
     *  the implementation of custom callbacks more convenient, because the user does not have to implement
     *  all the methods provided by the {@link DocumentFilter}, but can simply pass a lambda for either one
     *  of them.
     */
    private final DocumentFilter filter = new DocumentFilter()
    {
        private final C _component = getComponent();

        /**
         * See documentation in {@link DocumentFilter}!
         */
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            removes.forEach(action -> action.accept( new RemoveDelegate(_component, fb, offset, length) ) );
            if ( removes.isEmpty() ) fb.remove(offset, length);
        }
        /**
         * See documentation in {@link DocumentFilter}!
         */
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            inserts.forEach(action -> action.accept( new InsertDelegate(_component, fb, offset, string.length(), string, attr) ) );
            if ( inserts.isEmpty() ) fb.insertString(offset, string, attr);
        }
        /**
         * See documentation in {@link DocumentFilter}!
         */
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            replaces.forEach(action -> action.accept(new ReplaceDelegate(_component, fb, offset, length, text, attrs)) );
            if ( replaces.isEmpty() ) fb.replace(offset, length, text, attrs);
        }
    };

    protected UIForAbstractTextComponent(C component) { super(component); }

    /**
     * Sets the text of the wrapped <code>{@link TextComponent}</code>
     * to the specified text. If the text is <code>null</code>
     * or empty, has the effect of simply deleting the old text.
     * When text has been inserted, the resulting caret location
     * is determined by the implementation of the caret class.
     *
     * <p>
     * Note that text is not a bound property, so no {@link java.beans.PropertyChangeEvent}
     * is fired when it changes. To listen for changes to the text,
     * register action lambdas through {@link #onTextChange(Consumer)} or
     * use {@link DocumentListener} directly.
     * </p>
     *
     * @param text The new text to be set for the wrapped text component type.
     * @return This very builder to allow for method chaining.
     */
    public final I withText( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class, "Please use an empty string instead of null!");
        getComponent().setText(text);
        return _this();
    }

    /**
     * Binds the text of the wrapped <code>{@link TextComponent}</code> to
     * the specified {@link Val} property instance so that the text of the wrapped
     * text component is dynamically updated whenever the value of the property changes.
     * <p>
     *     Note that the text of the wrapped text component is only updated if the new value
     *     is different from the old value. This is to avoid infinite feedback loops.
     * <br>
     * @param val The property instance to bind the text of the wrapped text component to.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if the specified property is <code>null</code>.
     * @throws IllegalArgumentException if the specified property allows <code>null</code> values.
     */
    public final I withText( Val<String> val ) {
        NullUtil.nullArgCheck(val, "val", Val.class);
        _onShow(val, v -> getComponent().setText(v) );
        return withText( val.orElseThrow() );
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
        _onShow( text, newText -> {
            C c = getComponent();
            if ( !Objects.equals(c.getText(), newText) ) // avoid infinite recursion or some other Swing weirdness
                c.setText(newText);
        });
        _onTextChange( e -> {
            try {
                String newText = e.getDocument().getText(0, e.getDocument().getLength());
                _doApp(newText, t -> {
                    if ( UI.thisIsUIThread() )
                        UI.runLater( () -> text.act(t) ); // avoid attempt to mutate in notification
                        /*
                            We apply the text to the property in the next EDT cycle,
                            which is important to avoid mutating the property in a notification.
                            Because if a user decides to rebroadcast the text property in the 'onAct' callback,
                            then the text component will receive that new text while it is still in the middle of
                            document mutation, which is not allowed by Swing!
                        */
                    else
                        text.act(t);
                });
            } catch (BadLocationException ex) {
                throw new RuntimeException(ex);
            }
        });
        return withText( text.orElseThrow() );
    }

    /**
     *  Use this to set the font of the wrapped {@link JTextComponent}.
     * @param font The font of the text which should be displayed on the text component.
     * @return This builder instance, to allow for method chaining.
     * @throws IllegalArgumentException if {@code font} is {@code null}.
     */
    public final I withFont( Font font ) {
        NullUtil.nullArgCheck(font, "font", Font.class);
        this.getComponent().setFont( font );
        return _this();
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
        _onShow( font, v -> withFont(v) );
        return withFont( font.orElseThrow() );
    }

    /**
     * The provided {@link UI.HorizontalDirection} translates to {@link ComponentOrientation}
     * instances which are used to align the elements or text within the wrapped {@link JTextComponent}.
     * {@link LayoutManager} and {@link Component}
     * subclasses will use this property to
     * determine how to lay out and draw components.
     * <p>
     * Note: This method indirectly changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param direction The text orientation type which should be used.
     * @return This very builder to allow for method chaining.
     */
    public final I withTextOrientation( UI.HorizontalDirection direction ) {
        NullUtil.nullArgCheck(direction, "direction", UI.HorizontalDirection.class);
        getComponent().setComponentOrientation(direction.forTextOrientation());
        return _this();
    }

    /**
     * The provided {@link UI.HorizontalDirection} property translates to {@link ComponentOrientation}
     * instances which are used to align the elements or text within the wrapped {@link JTextComponent}.
     * {@link LayoutManager} and {@link Component}
     * subclasses will use this property to
     * determine how to lay out and draw components.
     * <p>
     * Note: This method indirectly changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param direction The text orientation type which should be used.
     * @return This very builder to allow for method chaining.
     */
    public final I withHorizontalTextOrientation( Val<UI.HorizontalDirection> direction ) {
        NullUtil.nullArgCheck( direction, "direction", Val.class );
        NullUtil.nullPropertyCheck(direction, "direction", "Null is not a valid value for the text orientation!");
        _onShow( direction, v -> {
            withTextOrientation(v);
            getComponent().validate();
        });
        return withTextOrientation(direction.orElseThrow());
    }

    /**
     *  Use this to modify the components' modifiability.
     *
     * @param isEditable The flag determining if the underlying {@link JTextComponent} should be editable or not.
     * @return This very builder to allow for method chaining.
     */
    public final I isEditableIf( boolean isEditable ) {
        getComponent().setEditable(isEditable);
        return _this();
    }


    /**
     *  Use this to register any change in the contents of the text component including both
     *  the displayed text and its attributes.
     *
     * @param action An action which will be executed when the text or its attributes in the underlying {@link JTextComponent} changes.
     * @return This very builder to allow for method chaining.
     */
    public final I onContentChange( Consumer<SimpleDelegate<JTextComponent, DocumentEvent>> action ) {
        C component = getComponent();
        component.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  {
                _doApp(()->action.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));}
            @Override public void removeUpdate(DocumentEvent e)  {
                _doApp(()->action.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));}
            @Override public void changedUpdate(DocumentEvent e) {
                _doApp(()->action.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));}
        });
        return _this();
    }

    /**
     *  Use this to register if the text in this text component changes.
     *  This does not include style attributes like font size.
     *
     * @param action An action which will be executed when the text string in the underlying {@link JTextComponent} changes.
     * @return This very builder to allow for method chaining.
     */
    public final I onTextChange( Consumer<SimpleDelegate<JTextComponent, DocumentEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Consumer.class);
        C component = getComponent();
        _onTextChange( e -> _doApp( () -> action.accept(new SimpleDelegate<>(component, e, () -> getSiblinghood() ))) );
        return _this();
    }

    protected final void _onTextChange( Consumer<DocumentEvent> action ) {
        getComponent().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.accept(e); }
            @Override public void removeUpdate(DocumentEvent e) { action.accept(e); }
            @Override public void changedUpdate(DocumentEvent e) {}
        });
    }

    /**
     * @param action An action which will be executed in case the underlying
     *               component supports text filtering (The underlying document is an {@link AbstractDocument}).
     */
    private void _ifFilterable( Runnable action ) {
        if ( getComponent().getDocument() instanceof AbstractDocument ) {
            action.run();
            AbstractDocument doc = (AbstractDocument)getComponent().getDocument();
            doc.setDocumentFilter(filter);
        }
    }

    /**
     * @param action A {@link Action} lambda which will be called when parts (or all) of the text in
     *               the underlying text component gets removed.
     *
     * @return This very builder to allow for method chaining.
     */
    public final I onTextRemove( Action<RemoveDelegate> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        _ifFilterable( () -> this.removes.add(action) );
        return _this();
    }

    /**
     * @param action A {@link Action} lambda which will be called when new text gets inserted
     *               into the underlying text component.
     *
     * @return This very builder to allow for method chaining.
     */
    public final I onTextInsert( Action<InsertDelegate> action ) {
        _ifFilterable( () -> this.inserts.add(action) );
        return _this();
    }

    /**
     * @param action A {@link Action} lambda which will be called when the text in
     *               the underlying text component gets replaced.
     *
     * @return This very builder to allow for method chaining.
     */
    public final I onTextReplace( Action<ReplaceDelegate> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        _ifFilterable( () -> this.replaces.add(action) );
        return _this();
    }


    public static abstract class AbstractDelegate
    {
        private final JTextComponent textComponent;
        private final DocumentFilter.FilterBypass filterBypass;
        private final int offset;
        private final int length;

        protected AbstractDelegate(JTextComponent textComponent, DocumentFilter.FilterBypass filterBypass, int offset, int length) {
            this.textComponent = textComponent;
            this.filterBypass = filterBypass;
            this.offset = offset;
            this.length = length;
        }

        public JTextComponent getComponent() {
            // We make sure that only the Swing thread can access the component:
            if ( UI.thisIsUIThread() ) return textComponent;
            else
                throw new IllegalStateException(
                        "Text component can only be accessed by the Swing thread."
                    );
        }
        public DocumentFilter.FilterBypass getFilterBypass() { return filterBypass; }
        public int getOffset() { return offset; }
        public int getLength() { return length; }

    }

    public static final class RemoveDelegate extends AbstractDelegate
    {
        private RemoveDelegate(JTextComponent textComponent, DocumentFilter.FilterBypass filterBypass, int offset, int length) {
            super(textComponent, filterBypass, offset, length);
        }

        public String getTextToBeRemoved() {
            try {
                return getComponent().getDocument().getText(getOffset(), getLength());
            } catch (BadLocationException e) {
                throw new IllegalStateException("Could not get text to be removed!", e);
            }
        }
    }

    public static final class InsertDelegate extends AbstractDelegate
    {
        private final String text;
        private final AttributeSet attributeSet;

        private InsertDelegate(JTextComponent textComponent, DocumentFilter.FilterBypass filterBypass, int offset, int length, String text, AttributeSet attributeSet) {
            super(textComponent, filterBypass, offset, length);
            this.text = text;
            this.attributeSet = attributeSet;
        }
        public String getTextToBeInserted() { return text; }
        public AttributeSet attributeSet() { return attributeSet; }
    }

    public static final class ReplaceDelegate extends AbstractDelegate
    {
        private final String text;
        private final AttributeSet attributeSet;

        private ReplaceDelegate(JTextComponent textComponent, DocumentFilter.FilterBypass filterBypass, int offset, int length, String text, AttributeSet attributeSet) {
            super(textComponent, filterBypass, offset, length);
            this.text = text;
            this.attributeSet = attributeSet;
        }
        public String getText() { return text; }
        public String getReplacementText() {
            try {
                return getComponent().getDocument().getText(getOffset(), getLength());
            } catch (BadLocationException e) {
                throw new IllegalStateException("Could not get text to be removed!", e);
            }
        }
        public AttributeSet getAttributeSet() { return attributeSet; }
    }

}

