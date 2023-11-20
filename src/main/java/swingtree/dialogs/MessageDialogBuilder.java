package swingtree.dialogs;

import swingtree.UI;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Component;

/**
 *  An immutable builder class for creating simple message dialogs (errors, warnings, infos...)
 *  based on the {@link JOptionPane} class, more specifically the
 *  {@link JOptionPane#showMessageDialog(Component, Object, String, int, Icon)}
 *  method.
 *  <p>
 *  This class is intended to be used as part of the {@link UI} API
 *  by calling the {@link UI#warn()}, {@link UI#error()}, {@link UI#info()} factory methods.
 */
public final class MessageDialogBuilder
{
    private static final MessageDialogBuilder _WARNING = get(JOptionPane.WARNING_MESSAGE).title("Warning");
    private static final MessageDialogBuilder _INFO    = get(JOptionPane.INFORMATION_MESSAGE).title("Info");
    private static final MessageDialogBuilder _ERROR   = get(JOptionPane.ERROR_MESSAGE).title("Error");

    /**
     * @param type The type of the dialog (error, warning, info... see {@link JOptionPane#showMessageDialog(Component, Object, String, int, Icon)}).
     * @return A {@link MessageDialogBuilder} instance that is not configured in any way.
     */
    public static MessageDialogBuilder get( int type ) { return new MessageDialogBuilder(type, "", "", null, null); }

    public static MessageDialogBuilder warning() { return _WARNING; }

    public static MessageDialogBuilder info() { return _INFO; }

    public static MessageDialogBuilder error() { return _ERROR; }

    private final int       _type;
    private final String    _title;
    private final String    _message;
    private final Icon      _icon;
    private final Component _parent;


    private MessageDialogBuilder(
        int type,
        String title,
        String message,
        Icon icon,
        Component parent
    ) {
        _type    = type;
        _title   = title;
        _message = message;
        _icon    = icon;
        _parent  = parent;
    }

    /**
     *  Set the title of the dialog.
     *  @param title The title of the dialog.
     *  @return A new {@link MessageDialogBuilder} instance with the specified title.
     */
    public MessageDialogBuilder title( String title ) {
        return new MessageDialogBuilder(_type, title, _message, _icon, _parent);
    }

    /**
     *  Set the message of the dialog.
     *  @param message The message of the dialog.
     *  @return A new {@link MessageDialogBuilder} instance with the specified message.
     */
    public MessageDialogBuilder message(String message ) {
        return new MessageDialogBuilder(_type, _title, message, _icon, _parent);
    }

    /**
     *  Set the icon of the dialog.
     *  @param icon The icon of the dialog.
     *  @return A new {@link MessageDialogBuilder} instance with the specified icon.
     */
    public MessageDialogBuilder icon(Icon icon ) {
        return new MessageDialogBuilder(_type, _title, _message, icon, _parent);
    }

    /**
     *  Set the parent of the dialog.
     *  @param parent The parent of the dialog.
     *  @return A new {@link MessageDialogBuilder} instance with the specified parent.
     */
    public MessageDialogBuilder parent(Component parent ) {
        return new MessageDialogBuilder(_type, _title, _message, _icon, parent);
    }

    /**
     *  Show the dialog with the specified configuration.
     */
    public void show() {
        UI.run(() -> {
            Context.summoner.showMessageDialog(
                _parent,  // parent component, if this is not null then the dialog will be centered on it
                _message, // message to display
                _title,   // title of the dialog displayed in the title bar of the dialog window
                _type,    // type of the dialog (error, warning, info...)
                _icon     // icon to display in the dialog
            );
        });
    }
}
