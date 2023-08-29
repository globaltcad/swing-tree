package swingtree.dialogs;

import swingtree.UI;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.Objects;

/**
 *  An immutable builder class for creating simple confirmation dialogs
 *  based on the {@link JOptionPane} class, more specifically the
 *  {@link JOptionPane#showOptionDialog(Component, Object, String, int, int, Icon, Object[], Object)}
 *  method.
 *  <p>
 *  This class is intended to be used as part of the {@link UI} API
 *  by calling the {@link UI#confirm()} factory method.
 */
public final class ConfirmDialogBuilder
{

    private static final ConfirmDialogBuilder _NONE = new ConfirmDialogBuilder("", "", "Yes", "No", "Yes", null, null);

    /**
     * @return A {@link ConfirmDialogBuilder} instance that is not configured in any way.
     */
    public static ConfirmDialogBuilder get() { return _NONE; }

    private final String   _title;
    private final String   _message;
    private final String   _yesOption;
    private final String   _noOption;
    private final String   _defaultOption;
    private final Icon     _icon;
    private final Component _parent;


    private ConfirmDialogBuilder(
        String title,
        String message,
        String yesOption,
        String noOption,
        String defaultOption,
        Icon icon,
        Component parent
    ) {
        _title         = Objects.requireNonNull(title);
        _message       = Objects.requireNonNull(message);
        _yesOption     = Objects.requireNonNull(yesOption);
        _noOption      = Objects.requireNonNull(noOption);
        _defaultOption = Objects.requireNonNull(defaultOption);
        _icon          = icon;
        _parent        = parent;
    }

    /**
     * @param title The title of the dialog.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified title.
     */
    public ConfirmDialogBuilder title(String title ) {
        return new ConfirmDialogBuilder(title, _message, _yesOption, _noOption, _defaultOption, _icon, _parent);
    }

    /**
     * @param message The message of the dialog.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified message.
     */
    public ConfirmDialogBuilder message(String message ) {
        return new ConfirmDialogBuilder(_title, message, _yesOption, _noOption, _defaultOption, _icon, _parent);
    }

    /**
     * @param yesOption The text of the "yes" option.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified "yes" option text.
     */
    public ConfirmDialogBuilder yesOption(String yesOption ) {
        return new ConfirmDialogBuilder(_title, _message, yesOption, _noOption, _defaultOption, _icon, _parent);
    }

    /**
     * @param noOption The text of the "no" option.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified "no" option text.
     */
    public ConfirmDialogBuilder noOption(String noOption ) {
        return new ConfirmDialogBuilder(_title, _message, _yesOption, noOption, _defaultOption, _icon, _parent);
    }

    /**
     * @param defaultOption The text of the default option.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified default option text.
     */
    public ConfirmDialogBuilder defaultOption(String defaultOption ) {
        return new ConfirmDialogBuilder(_title, _message, _yesOption, _noOption, defaultOption, _icon, _parent);
    }

    /**
     * @param icon The icon of the dialog.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified icon.
     */
    public ConfirmDialogBuilder icon(Icon icon ) {
        return new ConfirmDialogBuilder(_title, _message, _yesOption, _noOption, _defaultOption, icon, _parent);
    }

    /**
     * @param path The path to the icon of the dialog.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified icon.
     */
    public ConfirmDialogBuilder icon(String path ) {
        return new ConfirmDialogBuilder(_title, _message, _yesOption, _noOption, _defaultOption, UI.findIcon(path).orElse(null), _parent);
    }

    /**
     * @param parent The parent component of the dialog.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified parent component.
     */
    public ConfirmDialogBuilder parent(Component parent ) {
        return new ConfirmDialogBuilder(_title, _message, _yesOption, _noOption, _defaultOption, _icon, parent);
    }

    /**
     * @return The {@link ConfirmAnswer} that the user selected in the dialog.
     */
    public ConfirmAnswer show() {
        try {
            return UI.runAndGet(() -> {
                Object[] options = new Object[]{_yesOption, _noOption};

                int result = JOptionPane.showOptionDialog(
                                _parent, _message, _title, JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE, _icon, options, _defaultOption
                            );

                if ( result == JOptionPane.YES_OPTION ) return ConfirmAnswer.YES;
                if ( result == JOptionPane.NO_OPTION  ) return ConfirmAnswer.NO;
                return ConfirmAnswer.CANCEL;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return ConfirmAnswer.CANCEL;
        }
    }

}
