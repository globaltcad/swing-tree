package swingtree.dialogs;


import swingtree.UI;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Component;

/**
 *  An immutable builder class for creating simple error info dialogs
 *  based on the {@link JOptionPane} class, more specifically the
 *  {@link JOptionPane#showMessageDialog(Component, Object, String, int, Icon)}
 *  method.
 *  <p>
 *  This class is intended to be used as part of the {@link UI} API
 *  by calling the {@link UI#error()} factory method.
 */
public final class ErrorDialogBuilder
{
    private static final ErrorDialogBuilder _NONE = new ErrorDialogBuilder("", "", null, null);

    /**
     * @return A {@link ErrorDialogBuilder} instance that is not configured in any way.
     */
    public static ErrorDialogBuilder get() { return _NONE; }

    private final String   _title;
    private final String   _message;
    private final Icon     _icon;
    private final Component _parent;


    private ErrorDialogBuilder(
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
     *  @return A new {@link ErrorDialogBuilder} instance with the specified title.
     */
    public ErrorDialogBuilder title( String title ) {
        return new ErrorDialogBuilder(title, _message, _icon, _parent);
    }

    /**
     *  Set the message of the dialog.
     *  @param message The message of the dialog.
     *  @return A new {@link ErrorDialogBuilder} instance with the specified message.
     */
    public ErrorDialogBuilder message( String message ) {
        return new ErrorDialogBuilder(_title, message, _icon, _parent);
    }

    /**
     *  Set the icon of the dialog.
     *  @param icon The icon of the dialog.
     *  @return A new {@link ErrorDialogBuilder} instance with the specified icon.
     */
    public ErrorDialogBuilder icon( Icon icon ) {
        return new ErrorDialogBuilder(_title, _message, icon, _parent);
    }

    /**
     *  Set the icon of the dialog.
     *  @param path The path to the icon of the dialog.
     *  @return A new {@link ErrorDialogBuilder} instance with the specified icon.
     */
    public ErrorDialogBuilder icon( String path ) {
        return new ErrorDialogBuilder(_title, _message, UI.findIcon(path).orElse(null), _parent);
    }

    /**
     *  Set the parent component of the dialog.
     *  @param parent The parent component of the dialog.
     *  @return A new {@link ErrorDialogBuilder} instance with the specified parent component.
     */
    public ErrorDialogBuilder parent( Component parent ) {
        return new ErrorDialogBuilder(_title, _message, _icon, parent);
    }

    /**
     *  Show the error dialog.
     */
    public void show() {
        UI.run(() -> {
            JOptionPane.showMessageDialog(_parent, _message, _title, JOptionPane.ERROR_MESSAGE, _icon);
        });
    }
}
