package com.globaltcad.swingtree;


import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.api.mvvm.Val;
import com.globaltcad.swingtree.api.mvvm.Var;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 *  A swing tree builder node for {@link JTextComponent} instances.
 */
public abstract class UIForAbstractTextComponent<I, C extends JTextComponent> extends UIForAbstractSwing<I, C>
{
    private UIAction<RemoveDelegate> remove;
    private UIAction<InsertDelegate> insert;
    private UIAction<ReplaceDelegate> replace;

    /**
     *  A custom document filter which is simply a lambda-rization wrapper which ought to make
     *  the implementation of custom callbacks more convenient, because the user does not have to implement
     *  all the methods provided by the {@link DocumentFilter}, but can simply pass a lambda for either one
     *  of them.
     */
    private final DocumentFilter filter = new DocumentFilter()
    {
        /**
         * See documentation in {@link DocumentFilter}!
         */
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            if ( remove != null ) remove.accept( new RemoveDelegate(getComponent(), fb, offset, length) );
            else fb.remove(offset, length);
        }
        /**
         * See documentation in {@link DocumentFilter}!
         */
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if ( insert != null ) insert.accept( new InsertDelegate(getComponent(), fb, offset, string.length(), string, attr) );
            else fb.insertString(offset, string, attr);
        }
        /**
         * See documentation in {@link DocumentFilter}!
         */
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if ( replace != null ) replace.accept(new ReplaceDelegate(getComponent(), fb, offset, length, text, attrs));
            else fb.replace(offset, length, text, attrs);
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
        return (I) this;
    }

    public final I withText( Val<String> val ) {
        val.onShow(v->doUI(()->getComponent().setText(v)));
        return withText( val.get() );
    }

    public final I withText( Var<String> var ) {
        var.onShow(v->doUI(()->getComponent().setText(v)));
        _onKeyTyped( (KeyEvent e) -> {
            String oldText = getComponent().getText();
            // We need to add the now typed character to the old text, because the key typed event
            // is fired before the text is actually inserted into the text component.
            String part1 = oldText.substring(0, getComponent().getCaretPosition());
            String part2 = oldText.substring(getComponent().getCaretPosition());
            String newText;
            if ( e.getKeyChar() == '\b' ) // backspace
                newText = part1 + part2; // The user has deleted a character, so we need to remove it from the text.
            else if ( e.getKeyChar() == '\u007f' ) // delete
                newText = part1 + ( part2.length() < 2 ? part2 : part2.substring(1) );
            else
                newText = part1 + e.getKeyChar() + part2;
            doApp(newText, t -> var.set(t).act() );
        });
        return withText( var.get() );
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
    public final I withTextOrientation(UI.HorizontalDirection direction) {
        LogUtil.nullArgCheck(direction, "direction", UI.HorizontalDirection.class);
        getComponent().setComponentOrientation(direction.forTextOrientation());
        return (I) this;
    }

    /**
     *  Use this to modify the components' modifiability.
     *
     * @param isEditable The flag determining if the underlying {@link JTextComponent} should be editable or not.
     * @return This very builder to allow for method chaining.
     */
    public final I isEditableIf(boolean isEditable) {
        getComponent().setEditable(isEditable);
        return (I) this;
    }


    /**
     *  Use this to register any change in the contents of the text component including both
     *  the displayed text and its attributes.
     *
     * @param action An action which will be executed when the text or its attributes in the underlying {@link JTextComponent} changes.
     * @return This very builder to allow for method chaining.
     */
    public final I onContentChange(Consumer<SimpleDelegate<JTextComponent, DocumentEvent>> action) {
        C component = getComponent();
        component.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  {doApp(()->action.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));}
            @Override public void removeUpdate(DocumentEvent e)  {doApp(()->action.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));}
            @Override public void changedUpdate(DocumentEvent e) {doApp(()->action.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));}
        });
        return (I) this;
    }

    /**
     *  Use this to register if the text in this text component changes.
     *  This does not include style attributes like font size.
     *
     * @param action An action which will be executed when the text string in the underlying {@link JTextComponent} changes.
     * @return This very builder to allow for method chaining.
     */
    public final I onTextChange(Consumer<SimpleDelegate<JTextComponent, DocumentEvent>> action) {
        C component = getComponent();
        component.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) {doApp(()->action.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));}
            @Override public void removeUpdate(DocumentEvent e) {doApp(()->action.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())));}
            @Override public void changedUpdate(DocumentEvent e) {}
        });
        return (I) this;
    }

    /**
     * @param action An action which will be executed in case the underlying
     *               component supports text filtering (The underlying document is an {@link AbstractDocument}).
     */
    private void ifFilterable(Runnable action) {
        if ( getComponent().getDocument() instanceof AbstractDocument ) {
            action.run();
            AbstractDocument doc = (AbstractDocument)getComponent().getDocument();
            doc.setDocumentFilter(filter);
        }
    }

    /**
     * @param action A {@link UIAction} lambda which will be called when parts (or all) of the text in
     *               the underlying text component gets removed.
     *
     * @return This very builder to allow for method chaining.
     */
    public final I onTextRemove(UIAction<RemoveDelegate> action) {
        ifFilterable( () -> this.remove = action );
        return (I) this;
    }

    /**
     * @param action A {@link UIAction} lambda which will be called when new text gets inserted
     *               into the underlying text component.
     *
     * @return This very builder to allow for method chaining.
     */
    public final I onTextInsert(UIAction<InsertDelegate> action) {
        ifFilterable( () -> this.insert = action );
        return (I) this;
    }

    /**
     * @param action A {@link UIAction} lambda which will be called when the text in
     *               the underlying text component gets replaced.
     *
     * @return This very builder to allow for method chaining.
     */
    public final I onTextReplace(UIAction<ReplaceDelegate> action) {
        ifFilterable( () -> this.replace = action );
        return (I) this;
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

        public JTextComponent getTextComponent() { return textComponent; }
        public DocumentFilter.FilterBypass getFilterBypass() { return filterBypass; }
        public int getOffset() { return offset; }
        public int getLength() { return length; }
    }

    public static final class RemoveDelegate extends AbstractDelegate
    {
        private RemoveDelegate(JTextComponent textComponent, DocumentFilter.FilterBypass filterBypass, int offset, int length) {
            super(textComponent, filterBypass, offset, length);
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
        public String text() { return text; }
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
        public AttributeSet getAttributeSet() { return attributeSet; }
    }

}

