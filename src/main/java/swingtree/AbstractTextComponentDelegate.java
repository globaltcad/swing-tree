package swingtree;

import sprouts.Action;

import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.util.Objects;

/**
 *  A delegate object for text event listeners served by the numerous {@link java.awt.TextComponent} types.
 *  A concrete sub-type of this may be passed to one of the {@link sprouts.Action} lambdas
 *  registered by {@link UIForAnyTextComponent#onTextInsert(Action)}, {@link UIForAnyTextComponent#onTextRemove(Action)}
 *  and {@link UIForAnyTextComponent#onTextReplace(Action)}.
 *  <br>
 *  See {@link TextInsertDelegate}, {@link TextRemoveDelegate} and {@link TextReplaceDelegate} for concrete
 *  implementations of this abstract class.
 */
public abstract class AbstractTextComponentDelegate
{
    private final JTextComponent textComponent;
    private final DocumentFilter.FilterBypass filterBypass;
    private final int offset;
    private final int length;


    protected AbstractTextComponentDelegate(
        JTextComponent textComponent,
        DocumentFilter.FilterBypass filterBypass,
        int offset,
        int length
    ) {
        this.textComponent = Objects.requireNonNull(textComponent);
        this.filterBypass = filterBypass;
        this.offset = offset;
        this.length = length;
    }

    public JTextComponent getComponent() {
        // We make sure that only the Swing thread can access the component:
        if (UI.thisIsUIThread())
            return textComponent;
        else
            throw new IllegalStateException(
                    "Text component can only be accessed by the Swing thread."
            );
    }

    /**
     * @return The {@link DocumentFilter.FilterBypass} object that can be used to modify the document.
     */
    public DocumentFilter.FilterBypass getFilterBypass() {
        return filterBypass;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

}
