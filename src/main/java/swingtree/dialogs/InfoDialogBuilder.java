package swingtree.dialogs;

import swingtree.UI;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Component;

/**
 *  An immutable builder class for creating simple info dialogs
 *  based on the {@link JOptionPane} class, more specifically the
 *  {@link JOptionPane#showMessageDialog(Component, Object, String, int, Icon)}
 *  method.
 *  <p>
 *  This class is intended to be used as part of the {@link UI} API
 *  by calling the {@link UI#info()} factory method.
 */
public final class InfoDialogBuilder
{
    private static final InfoDialogBuilder _NONE = new InfoDialogBuilder("", "", null, null);

    /**
     * @return A {@link InfoDialogBuilder} instance that is not configured in any way.
     */
    public static InfoDialogBuilder get() { return _NONE; }

    private final String   _title;
    private final String   _message;
    private final Icon     _icon;
    private final Component _parent;


    private InfoDialogBuilder(
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
     *  @return A new {@link InfoDialogBuilder} instance with the specified title.
     */
    public InfoDialogBuilder title(String title ) {
        return new InfoDialogBuilder(title, _message, _icon, _parent);
    }

    /**
     *  Set the message of the dialog.
     *  @param message The message of the dialog.
     *  @return A new {@link InfoDialogBuilder} instance with the specified message.
     */
    public InfoDialogBuilder message(String message ) {
        return new InfoDialogBuilder(_title, message, _icon, _parent);
    }

    /**
     *  Set the icon of the dialog.
     *  @param icon The icon of the dialog.
     *  @return A new {@link InfoDialogBuilder} instance with the specified icon.
     */
    public InfoDialogBuilder icon(Icon icon ) {
        return new InfoDialogBuilder(_title, _message, icon, _parent);
    }

    /**
     *  Set the icon of the dialog.
     *  @param path The path of the icon of the dialog.
     *  @return A new {@link InfoDialogBuilder} instance with the specified icon.
     */
    public InfoDialogBuilder icon(String path ) {
        return icon(UI.findIcon(path).orElse(null));
    }

    /**
     *  Set the parent of the dialog.
     *  @param parent The parent of the dialog.
     *  @return A new {@link InfoDialogBuilder} instance with the specified parent.
     */
    public InfoDialogBuilder parent(Component parent ) {
        return new InfoDialogBuilder(_title, _message, _icon, parent);
    }

    /**
     *  Build the dialog and display it.
     */
    public void show() {
        UI.run(() -> {
            JOptionPane.showMessageDialog(_parent, _message, _title, JOptionPane.INFORMATION_MESSAGE, _icon);
        });
    }
}
