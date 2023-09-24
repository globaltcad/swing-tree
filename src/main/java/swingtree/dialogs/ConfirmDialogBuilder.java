package swingtree.dialogs;

import org.slf4j.Logger;
import swingtree.UI;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
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
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ConfirmDialogBuilder.class);

    private static final ConfirmDialogBuilder _QUESTION = type(JOptionPane.QUESTION_MESSAGE).title("Confirm");
    private static final ConfirmDialogBuilder _INFO     = type(JOptionPane.INFORMATION_MESSAGE).title("Info");
    private static final ConfirmDialogBuilder _WARN     = type(JOptionPane.WARNING_MESSAGE).title("Warning");
    private static final ConfirmDialogBuilder _ERROR    = type(JOptionPane.ERROR_MESSAGE).title("Error");
    private static final ConfirmDialogBuilder _PLAIN    = type(JOptionPane.PLAIN_MESSAGE).title("Message");

    /**
     * @return A {@link ConfirmDialogBuilder} instance that is dedicated for configuring a specific message.
     */
    private static ConfirmDialogBuilder type( int type ) { 
        return new ConfirmDialogBuilder(type, "", "", "Yes", "No", "Cancel", "Yes", null, null);
    }
    
    public static ConfirmDialogBuilder question() { return _QUESTION; }
    
    public static ConfirmDialogBuilder info()     { return _INFO;     }
    
    public static ConfirmDialogBuilder warning()     { return _WARN;     }
    
    public static ConfirmDialogBuilder error()    { return _ERROR;    }
    
    public static ConfirmDialogBuilder plain()    { return _PLAIN;    }


    private final int      _type;
    private final String   _title;
    private final String   _message;
    private final String   _yesOption;
    private final String   _noOption;
    private final String   _cancelOption;
    private final String   _defaultOption;
    private final Icon     _icon;
    private final Component _parent;


    private ConfirmDialogBuilder(
        int type,
        String title,
        String message,
        String yesOption,
        String noOption,
        String cancelOption,
        String defaultOption,
        Icon icon,
        Component parent
    ) {
        _type          = type;
        _title         = Objects.requireNonNull(title);
        _message       = Objects.requireNonNull(message);
        _yesOption     = Objects.requireNonNull(yesOption);
        _noOption      = Objects.requireNonNull(noOption);
        _cancelOption  = Objects.requireNonNull(cancelOption);
        _defaultOption = Objects.requireNonNull(defaultOption);
        _icon          = icon;
        _parent        = parent;
    }

    /**
     * @param title The title of the dialog.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified title.
     */
    public ConfirmDialogBuilder title( String title ) {
        return new ConfirmDialogBuilder(_type, title, _message, _yesOption, _noOption, _cancelOption, _defaultOption, _icon, _parent);
    }

    /**
     * @param message The message of the dialog.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified message.
     */
    public ConfirmDialogBuilder message( String message ) {
        return new ConfirmDialogBuilder(_type, _title, message, _yesOption, _noOption, _cancelOption, _defaultOption, _icon, _parent);
    }

    /**
     * @param yesOption The text of the "yes" option.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified "yes" option text.
     */
    public ConfirmDialogBuilder yesOption( String yesOption ) {
        return new ConfirmDialogBuilder(_type, _title, _message, yesOption, _noOption, _cancelOption, _defaultOption, _icon, _parent);
    }

    /**
     * @param noOption The text of the "no" option.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified "no" option text.
     */
    public ConfirmDialogBuilder noOption( String noOption ) {
        return new ConfirmDialogBuilder(_type, _title, _message, _yesOption, noOption, _cancelOption, _defaultOption, _icon, _parent);
    }

    /**
     * @param cancelOption The text of the "cancel" option.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified "cancel" option text.
     */
    public ConfirmDialogBuilder cancelOption( String cancelOption ) {
        return new ConfirmDialogBuilder(_type, _title, _message, _yesOption, _noOption, cancelOption, _defaultOption, _icon, _parent);
    }

    /**
     * @param defaultOption The text of the default option.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified default option text.
     */
    public ConfirmDialogBuilder defaultOption( String defaultOption ) {
        return new ConfirmDialogBuilder(_type, _title, _message, _yesOption, _noOption, _cancelOption, defaultOption, _icon, _parent);
    }

    /**
     * @param icon The icon of the dialog.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified icon.
     */
    public ConfirmDialogBuilder icon( Icon icon ) {
        return new ConfirmDialogBuilder(_type, _title, _message, _yesOption, _noOption, _cancelOption, _defaultOption, icon, _parent);
    }

    /**
     * @param path The path to the icon of the dialog.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified icon.
     */
    public ConfirmDialogBuilder icon( String path ) {
        return new ConfirmDialogBuilder(_type, _title, _message, _yesOption, _noOption, _cancelOption, _defaultOption, UI.findIcon(path).orElse(null), _parent);
    }

    /**
     * @param parent The parent component of the dialog.
     * @return A new {@link ConfirmDialogBuilder} instance with the specified parent component.
     */
    public ConfirmDialogBuilder parent( Component parent ) {
        return new ConfirmDialogBuilder(_type, _title, _message, _yesOption, _noOption, _cancelOption, _defaultOption, _icon, parent);
    }

    /**
     * @return The {@link ConfirmAnswer} that the user selected in the dialog.
     */
    public ConfirmAnswer show() {
        try {
            return UI.runAndGet(() -> {
                String yes    = _yesOption.trim();
                String no     = _noOption.trim();
                String cancel = _cancelOption.trim();

                List<Object> options = new ArrayList<>();
                if ( !yes.isEmpty()    )
                    options.add(yes);
                if ( !yes.isEmpty() && !no.isEmpty() )
                    options.add(no);
                if ( !cancel.isEmpty() && !options.isEmpty() )
                    options.add(cancel);

                int optionsType = JOptionPane.DEFAULT_OPTION;
                if ( !yes.isEmpty() && no.isEmpty() && cancel.isEmpty() )
                    optionsType = JOptionPane.OK_OPTION;
                if ( !yes.isEmpty() && !no.isEmpty() && cancel.isEmpty() )
                    optionsType = JOptionPane.YES_NO_OPTION;
                if ( !yes.isEmpty() && !no.isEmpty() && !cancel.isEmpty() )
                    optionsType = JOptionPane.YES_NO_CANCEL_OPTION;
                if ( !yes.isEmpty() && no.isEmpty() && !cancel.isEmpty() )
                    optionsType = JOptionPane.OK_CANCEL_OPTION;

                return ConfirmAnswer.from(JOptionPane.showOptionDialog(
                            _parent, _message, _title, optionsType,
                            _type, _icon, options.toArray(), _defaultOption
                        ));
            });
        } catch (Exception e) {
            log.error("Failed to show confirm dialog, returning 'CANCEL' as dialog result!", e);
            return ConfirmAnswer.CANCEL;
        }
    }
}
