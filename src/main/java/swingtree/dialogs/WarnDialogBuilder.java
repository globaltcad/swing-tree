package swingtree.dialogs;

import swingtree.UI;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Component;

/**
 *  An immutable builder class for creating simple warning dialogs
 *  based on the {@link JOptionPane} class, more specifically the
 *  {@link JOptionPane#showMessageDialog(Component, Object, String, int, Icon)}
 *  method.
 *  <p>
 *  This class is intended to be used as part of the {@link UI} API
 *  by calling the {@link UI#warn()} factory method.
 */
public final class WarnDialogBuilder
{
    private static final WarnDialogBuilder _NONE = new WarnDialogBuilder("", "", null, null);

    /**
     * @return A {@link WarnDialogBuilder} instance that is not configured in any way.
     */
    public static WarnDialogBuilder get() { return _NONE; }

    private final String   _title;
    private final String   _message;
    private final Icon _icon;
    private final Component _parent;


    private WarnDialogBuilder(
        String title,
        String message,
        Icon icon,
        Component parent
    ) {
        _title   = title;
        _message = message;
        _icon    = icon;
        _parent  = parent;
    }

    /**
     *  Set the title of the dialog.
     *  @param title The title of the dialog.
     *  @return A new {@link WarnDialogBuilder} instance with the specified title.
     */
    public WarnDialogBuilder title(String title ) {
        return new WarnDialogBuilder(title, _message, _icon, _parent);
    }

    /**
     *  Set the message of the dialog.
     *  @param message The message of the dialog.
     *  @return A new {@link WarnDialogBuilder} instance with the specified message.
     */
    public WarnDialogBuilder message(String message ) {
        return new WarnDialogBuilder(_title, message, _icon, _parent);
    }

    /**
     *  Set the icon of the dialog.
     *  @param icon The icon of the dialog.
     *  @return A new {@link WarnDialogBuilder} instance with the specified icon.
     */
    public WarnDialogBuilder icon(Icon icon ) {
        return new WarnDialogBuilder(_title, _message, icon, _parent);
    }

    /**
     *  Set the parent of the dialog.
     *  @param parent The parent of the dialog.
     *  @return A new {@link WarnDialogBuilder} instance with the specified parent.
     */
    public WarnDialogBuilder parent(Component parent ) {
        return new WarnDialogBuilder(_title, _message, _icon, parent);
    }

    /**
     *  Show the dialog with the specified configuration.
     */
    public void show() {
        UI.run(() -> {
            JOptionPane.showMessageDialog(
                _parent,
                _message,
                _title,
                JOptionPane.WARNING_MESSAGE,
                _icon
            );
        });
    }
}
