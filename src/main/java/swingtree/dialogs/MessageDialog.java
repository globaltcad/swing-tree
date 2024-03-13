package swingtree.dialogs;

import org.jspecify.annotations.Nullable;
import swingtree.UI;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.Objects;

/**
 *  An immutable builder class for creating simple message dialogs (errors, warnings, infos...)
 *  based on the {@link JOptionPane} class, more specifically the
 *  {@link JOptionPane#showMessageDialog(Component, Object, String, int, Icon)}
 *  method.
 *  <p>
 *  This class is intended to be used as part of the {@link UI} API
 *  by calling the {@link UI#message(String)} factory method.
 */
public final class MessageDialog
{
    public static MessageDialog saying(String text ) {
        Objects.requireNonNull(text);
        return new MessageDialog(
                    -1,
                    "",
                    text,
                    null,
                    null
                );
    }

    private final int                 _type;
    private final String              _title;
    private final String              _message;
    private final @Nullable Icon      _icon;
    private final @Nullable Component _parent;


    private MessageDialog(
        int                 type,
        String              title,
        String              message,
        @Nullable Icon      icon,
        @Nullable Component parent
    ) {
        _type    = type;
        _title   = title;
        _message = message;
        _icon    = icon;
        _parent  = parent;
    }

    /**
     *  Set the type of the dialog.
     *  @param type The type of the dialog.
     *  @return A new {@link MessageDialog} instance with the specified type.
     */
    public MessageDialog type(int type ) {
        return new MessageDialog(type, _title, _message, _icon, _parent);
    }

    /**
     *  Set the title of the dialog.
     *  @param title The title of the dialog.
     *  @return A new {@link MessageDialog} instance with the specified title.
     */
    public MessageDialog titled(String title ) {
        return new MessageDialog(_type, title, _message, _icon, _parent);
    }

    /**
     *  Set the icon of the dialog.
     *  @param icon The icon of the dialog.
     *  @return A new {@link MessageDialog} instance with the specified icon.
     */
    public MessageDialog icon(Icon icon ) {
        return new MessageDialog(_type, _title, _message, icon, _parent);
    }

    /**
     *  Set the parent of the dialog.
     *  @param parent The parent of the dialog.
     *  @return A new {@link MessageDialog} instance with the specified parent.
     */
    public MessageDialog parent( Component parent ) {
        return new MessageDialog(_type, _title, _message, _icon, parent);
    }

    /**
     *  Show the dialog with the specified configuration
     *  as an error dialog.
     */
    public void showAsError() {
        type(JOptionPane.ERROR_MESSAGE).show();
    }

    /**
     *  Show the dialog with the specified configuration
     *  as a warning dialog.
     */
    public void showAsWarning() {
        type(JOptionPane.WARNING_MESSAGE).show();
    }

    /**
     *  Show the dialog with the specified configuration
     *  as an info dialog.
     */
    public void showAsInfo() {
        type(JOptionPane.INFORMATION_MESSAGE).show();
    }

    /**
     *  Show the dialog with the specified configuration.
     */
    public void show() {
        UI.run(() -> {
            int type = _type;
            if ( type == -1 )
                type = JOptionPane.INFORMATION_MESSAGE;

            Context.summoner.showMessageDialog(
                _parent,  // parent component, if this is not null then the dialog will be centered on it
                _message, // message to display
                _title,   // title of the dialog displayed in the title bar of the dialog window
                type,     // type of the dialog (error, warning, info...)
                _icon     // icon to display in the dialog
            );
        });
    }
}
