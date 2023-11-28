package swingtree;

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
    TextRemoveDelegate(
        JTextComponent textComponent,
        DocumentFilter.FilterBypass filterBypass,
        int offset,
        int length
    ) {
        super(textComponent, filterBypass, offset, length);
    }

    /**
     * @return The text to be removed.
     */
    public String getTextToBeRemoved() {
        try {
            return getComponent().getDocument().getText(getOffset(), getLength());
        } catch (BadLocationException e) {
            throw new IllegalStateException("Could not get text to be removed!", e);
        }
    }
}
