package swingtree;

import org.slf4j.Logger;
import sprouts.Action;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/**
 *  A delegate object for the {@link Action} lambda passed
 *  to the {@link UIForAnyTextComponent#onTextReplace(Action)} method,
 *  which is used to register a listener for text replacement events.
 *  <br>
 *  This delegate provides useful context information to the action listener,
 *  such as the text component itself, the {@link DocumentFilter.FilterBypass}
 *  object that can be used to replace text in the document, the offset at which
 *  the text is to be replaced and the length of the text to be replaced.
 */
public final class TextReplaceDelegate extends AbstractTextComponentDelegate
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TextReplaceDelegate.class);

    private final String text;
    private final AttributeSet attributeSet;


    TextReplaceDelegate(
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
     * @return The text to be inserted or an empty {@link String} indicating that no text is to be inserted.
     *         Null is never returned.
     */
    public String getText() {
        return text;
    }

    /**
     * @return The text to be removed.
     */
    public String getReplacementText() {
        try {
            return getComponent().getDocument().getText(getOffset(), getLength());
        } catch (BadLocationException e) {
            log.error(
                    "Failed to read the replacement text from the document " +
                    "at offset "+ getOffset() + " and using length " + getLength() + "!",
                    e
                );
        }
        return "";
    }

    /**
     * @return The attribute set of the text to be inserted.
     */
    public AttributeSet getAttributeSet() {
        return attributeSet;
    }
}
