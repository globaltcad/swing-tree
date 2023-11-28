package swingtree;

import sprouts.Action;

import javax.swing.text.AttributeSet;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/**
 *  A delegate object for the {@link Action} lambda passed
 *  to the {@link UIForAnyTextComponent#onTextInsert(Action)} method,
 *  which is used to register a listener for text insertion events.
 *  <br>
 *  This delegate provides useful context information to the action listener,
 *  such as the text component itself, the {@link DocumentFilter.FilterBypass}
 *  object that can be used to insert text into the document, the offset at which
 *  the text is to be inserted and the length of the text to be inserted.
 */
public final class TextInsertDelegate extends AbstractTextComponentDelegate
{
    private final String text;
    private final AttributeSet attributeSet;


    TextInsertDelegate(
        JTextComponent textComponent,
        DocumentFilter.FilterBypass filterBypass,
        int offset,
        int length,
        String text,
        AttributeSet attributeSet
    ) {
        super(textComponent, filterBypass, offset, length);
        this.text = text;
        this.attributeSet = attributeSet;
    }

    /**
     * @return The text to be inserted.
     */
    public String getTextToBeInserted() {
        return text;
    }

    /**
     * @return The attribute set of the text to be inserted.
     */
    public AttributeSet attributeSet() {
        return attributeSet;
    }
}
