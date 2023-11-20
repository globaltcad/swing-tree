package swingtree.dialogs;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.HeadlessException;

public interface OptionsDialogSummoner
{
    default int showOptionDialog(
            Component parentComponent,
            Object message, String title, int optionType, int messageType,
            Icon icon, Object[] options, Object initialValue
    ) throws HeadlessException {
        return JOptionPane.showOptionDialog(parentComponent, message, title, optionType, messageType, icon, options, initialValue);
    }

    default void showMessageDialog(
            Component parentComponent,
            Object message, String title, int messageType, Icon icon
    ) throws HeadlessException {
        JOptionPane.showMessageDialog(parentComponent, message, title, messageType, icon);
    }
}
