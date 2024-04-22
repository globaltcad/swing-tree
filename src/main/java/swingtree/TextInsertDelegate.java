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
        JTextComponent              textComponent,
        DocumentFilter.FilterBypass filterBypass,
        int                         offset,
        int                         length,
        String                      text,
        AttributeSet                attributeSet
    ) {
        super(textComponent, filterBypass, offset, length);
        this.text         = ( text == null ? "" : text );
        this.attributeSet = attributeSet;
    }

    /**
     *  Exposes the text that is being inserted.
     *  See {@link DocumentFilter#insertString(DocumentFilter.FilterBypass, int, String, AttributeSet)}
     *  for more information.
     *
     * @return The text to be inserted or an empty {@link String} if no text is to be inserted.
     *         Null is never returned.
     */
    public String getTextToBeInserted() {
        return text;
    }

    /**
     *  Exposes the attribute set of the text to be inserted.
     *  An attribute is a key-value pair that can be used to store
     *  arbitrary data about the text to be inserted.
     *  See {@link DocumentFilter#insertString(DocumentFilter.FilterBypass, int, String, AttributeSet)}
     *  for more information about the underlying source of this attribute set.
     * @return The attribute set of the text to be inserted.
     */
    public final AttributeSet attributeSet() {
        return attributeSet;
    }
}
