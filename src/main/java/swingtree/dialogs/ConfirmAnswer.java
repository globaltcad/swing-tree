package swingtree.dialogs;

import swingtree.UI;

/**
 *  An enum representing the possible answers to a confirmation dialog.
 *  <p>
 *  This enum is intended to be used as part of the {@link ConfirmDialogBuilder} API
 *  which can be accessed by calling the {@link UI#confirm()} factory method.
 */
public enum ConfirmAnswer
{
    YES,
    NO,
    CANCEL;

    /**
     * @return {@code true} if the user selected the "yes" option, {@code false} otherwise.
     */
    public boolean isYes() { return this == YES; }

    /**
     * @return {@code true} if the user selected the "no" option, {@code false} otherwise.
     */
    public boolean isNo() { return this == NO; }

    /**
     * @return {@code true} if the user selected the "cancel" option, {@code false} otherwise.
     */
    public boolean isCancel() { return this == CANCEL; }
}
