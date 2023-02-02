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
        getComponent().setText(text);
        return _this();
    }

    public final I withText( Val<String> val ) {
        _onShow(val, v -> getComponent().setText(v) );
        return withText( val.orElseThrow() );
    }

    public final I withText( Var<String> text ) {
        NullUtil.nullPropertyCheck(text, "text", "Use an empty string instead of null!");
        _onShow( text, newText -> {
            C c = getComponent();
            if ( _eventProcessor == EventProcessor.COUPLED )
                UI.runLater(() -> {
                    if ( !Objects.equals(c.getText(), newText) )
                        c.setText(newText);
                    /*
                        Okay, so this looks really strange, I know, but this is important to prevent a tricky bug!
                        The bug is that if this code here is triggered by the key type event below, then the
                        text component will not yet be updated with the new text, because the text of the component
                        will only be updated after the key type event has been processed.
                        So if a property in a user view model rebroadcasts the newly typed text generated
                        by the '_onKeyTyped' below, then user action (key typed, deletion, etc.) will be
                        added to the 'newTex' string we have here in this lambda, which is not what we want!
                    */
                });
            else
                if ( !Objects.equals(c.getText(), newText) )
                    c.setText(newText);
        });
        _onKeyTyped( (KeyEvent e) -> {
            String oldText = getComponent().getText();
            // We need to add the now typed character to the old text, because the key typed event
            // is fired before the text is actually inserted into the text component.
            String part1 = oldText.substring(0, getComponent().getCaretPosition());
            String part2 = oldText.substring(getComponent().getCaretPosition());
            String newText;
            if ( e.getKeyChar() == '\b' ) // backspace
                newText = part1 + part2; // The user has deleted a character(s), they will already be gone, we just need to set the text.
            else if ( e.getKeyChar() == '\u007f' ) // delete
                newText = part1 + ( part2.length() < 2 ? part2 : part2.substring(1) );
            else
                newText = part1 + e.getKeyChar() + part2;
            _doApp(newText, text::act);
        });
        return withText( text.orElseThrow() );
    }

    public final I withFont( Font font ) {
        NullUtil.nullArgCheck(font, "font", Font.class);
        this.getComponent().setFont( font );
        return _this();
    }

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
        C component = getComponent();
        component.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) {
                _doApp(()->action.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));}
            @Override public void removeUpdate(DocumentEvent e) {
                _doApp(()->action.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));}
            @Override public void changedUpdate(DocumentEvent e) {}
        });
        return _this();
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

