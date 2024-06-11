package swingtree.dialogs;

import swingtree.UI;

import javax.swing.JOptionPane;

/**
 *  An enum representing the possible answers to a confirmation dialog.
 *  <p>
 *  This enum is intended to be used as part of the {@link ConfirmDialog} API
 *  which can be accessed by calling the {@link UI#confirmation(String)} factory method.
 */
public enum ConfirmAnswer
{
    YES,
    NO,
    CANCEL,
    CLOSE;

    /**
     *  A convenience method equivalent to {@code myEnum == ConfirmAnswer.YES}.
     *
     * @return {@code true} if the user selected the "yes" option, {@code false} otherwise.
     */
    public boolean isYes() { return this == YES; }

    /**
     *  A convenience method equivalent to {@code myEnum == ConfirmAnswer.NO}.
     *
     * @return {@code true} if the user selected the "no" option, {@code false} otherwise.
     */
    public boolean isNo() { return this == NO; }

    /**
     *  A convenience method equivalent to {@code myEnum == ConfirmAnswer.CANCEL}.
     *
     * @return {@code true} if the user selected the "cancel" option, {@code false} otherwise.
     */
    public boolean isCancel() { return this == CANCEL; }

    /**
     *  A convenience method equivalent to {@code myEnum == ConfirmAnswer.CLOSE}.
     *
     * @return {@code true} if the user selected the "close" option, {@code false} otherwise.
     */
    public boolean isClose() { return this == CLOSE; }

    /**
     *  A convenience method to check if the user
     *  selected the "cancel" option, or closed the dialog.
     *  This is equivalent to {@code myEnum == ConfirmAnswer.CANCEL || myEnum == ConfirmAnswer.CLOSE}.
     *
     * @return {@code true} if the user selected the "cancel" option, or they closed the dialog, {@code false} otherwise.
     */
    public boolean isCancelOrClose() { return isCancel() || isClose(); }

    /**
     *  Converts a {@link JOptionPane} constant to a {@link ConfirmAnswer} enum.
     *
     * @return The {@link JOptionPane} constant corresponding to this answer.
     */
    static ConfirmAnswer from( int option ) {
        switch ( option ) {
            case JOptionPane.YES_OPTION:    return YES;
            case JOptionPane.NO_OPTION:     return NO;
            case JOptionPane.CLOSED_OPTION: return CLOSE;
            default:
                return CANCEL;
        }
    }
}
