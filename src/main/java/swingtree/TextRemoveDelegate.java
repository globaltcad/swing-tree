package swingtree;

import org.slf4j.Logger;
import sprouts.Action;

import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/**
 *  A delegate object for the {@link Action} lambda passed
 *  to the {@link UIForAnyTextComponent#onTextRemove(Action)}  method,
 *  which is used to register a listener for text removal events.
 *  <br>
 *  This delegate provides useful context information to the action listener,
 *  such as the text component itself, the {@link DocumentFilter.FilterBypass}
 *  object that can be used to remove text from the document, the offset at which
 *  the text is to be removed and the length of the text to be removed.
 */
public final class TextRemoveDelegate extends AbstractTextComponentDelegate
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TextRemoveDelegate.class);


    TextRemoveDelegate(
        JTextComponent              textComponent,
        DocumentFilter.FilterBypass filterBypass,
        int                         offset,
        int                         length
    ) {
        super(textComponent, filterBypass, offset, length);
    }

    /**
     *  This method exposes the actual text that is being removed from the document
     *  as part of the current text removal event.
     *  If nothing is removed from the document, an empty {@link String} is returned.
     *
     * @return The text to be removed or an empty {@link String} if no text is to be removed.
     *         Null is never returned.
     *
     */
    public String getTextToBeRemoved() {
        try {
            return getComponent().getDocument().getText(getOffset(), getLength());
        } catch (BadLocationException e) {
            log.error(
                    "Failed to read the text to be removed from the document " +
                    "at offset "+ getOffset() + " and using length " + getLength() + "!",
                    e
                );
            return "";
        }
    }
}
