package swingtree.dialogs;

import org.jspecify.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.HeadlessException;

public interface OptionsDialogSummoner
{
    default int showOptionDialog(
        @Nullable Component parentComponent,
        Object message,
        String title,
        int optionType,
        int messageType,
        @Nullable Icon icon,
        Object[] options,
        @Nullable Object initialValue
    ) throws HeadlessException {
        return JOptionPane.showOptionDialog(parentComponent, message, title, optionType, messageType, icon, options, initialValue);
    }

    default void showMessageDialog(
        @Nullable Component parentComponent,
        Object message,
        String title,
        int messageType,
        @Nullable Icon icon
    ) throws HeadlessException {
        JOptionPane.showMessageDialog(parentComponent, message, title, messageType, icon);
    }
}
