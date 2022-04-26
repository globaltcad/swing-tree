package com.globaltcad.swingtree;


import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.util.function.Consumer;

public class UIForTextComponent extends UIForSwing<UIForTextComponent, JTextComponent>
{
    public interface Remove {void remove(JTextComponent textComp, DocumentFilter.FilterBypass fb, int offset, int length);}
    public interface Insert {void insert( JTextComponent textComp, DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr);}
    public interface Replace{void replace(JTextComponent textComp, DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs);}

    private Remove remove;
    private Insert insert;
    private Replace replace;

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
            if ( remove != null ) remove.remove( component, fb, offset, length );
            else fb.remove(offset, length);
        }

        /**
         * See documentation in {@link DocumentFilter}!
         */
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if ( insert != null ) insert.insert( component, fb, offset, string, attr );
            else fb.insertString(offset, string, attr);
        }

        /**
         * See documentation in {@link DocumentFilter}!
         */
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if ( replace != null ) replace.replace( component, fb, offset, length, text, attrs );
            else fb.replace(offset, length, text, attrs);
        }

    };

    protected UIForTextComponent(JTextComponent component) { super(component); }

    /**
     * @param action An action which will be executed when the text in the underlying {@link JTextComponent} changes.
     * @return This very builder to allow for method chaining.
     */
    public UIForTextComponent onTextChange(Consumer<EventContext<JTextComponent, DocumentEvent>> action) {
        this.component.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) {action.accept(new EventContext<>(component, e));}
            @Override public void removeUpdate(DocumentEvent e) {action.accept(new EventContext<>(component, e));}
            @Override public void changedUpdate(DocumentEvent e) {action.accept(new EventContext<>(component, e));}
        });
        return this;
    }

    /**
     * @param action An action which will be executed in case the underlying
     *               component supports text filtering (The underlying document is an {@link AbstractDocument}).
     */
    private void ifFilterable(Runnable action) {
        if ( this.component.getDocument() instanceof AbstractDocument ) {
            action.run();
            AbstractDocument doc = (AbstractDocument)this.component.getDocument();
            doc.setDocumentFilter(filter);
        }
    }

    /**
     * @param action A {@link Remove} lambda which will be called when parts (or all) of the text in
     *               the underlying text component gets removed.
     *
     * @return This very builder to allow for method chaining.
     */
    public UIForTextComponent onTextRemove(Remove action) {
        ifFilterable( () -> this.remove = action );
        return this;
    }

    /**
     * @param action A {@link Insert} lambda which will be called when new text gets inserted
     *               into the underlying text component.
     *
     * @return This very builder to allow for method chaining.
     */
    public UIForTextComponent onTextInsert(Insert action) {
        ifFilterable( () -> this.insert = action );
        return this;
    }

    /**
     * @param action A {@link Replace} lambda which will be called when the text in
     *               the underlying text component gets replaced.
     *
     * @return This very builder to allow for method chaining.
     */
    public UIForTextComponent onTextReplace(Replace action) {
        ifFilterable( () -> this.replace = action );
        return this;
    }
}

