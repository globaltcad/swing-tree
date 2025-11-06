package swingtree.dialogs;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.api.IconDeclaration;

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
 *  by calling the {@link UI#confirmation(String)} factory method.
 */
public final class ConfirmDialog
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ConfirmDialog.class);

    /**
     *  Creates a new {@link ConfirmDialog} instance with the specified question.
     *  @param question The question to ask the user.
     *  @return A new {@link ConfirmDialog} instance with the specified question.
     */
    public static ConfirmDialog asking(String question ) {
        Objects.requireNonNull(question);
        return new ConfirmDialog(
                    -1,
                    "",
                    question,
                    "Yes",
                    "No",
                    "Cancel",
                    ConfirmAnswer.YES,
                    null,
                    null
                );
    }

    private final int                  _type;
    private final String               _title;
    private final String               _message;
    private final String               _yesOption;
    private final String               _noOption;
    private final String               _cancelOption;
    private final ConfirmAnswer        _defaultOption;
    private final @Nullable Icon       _icon;
    private final @Nullable Component  _parent;


    private ConfirmDialog(
        int                 type,
        String              title,
        String              message,
        String              yesOption,
        String              noOption,
        String              cancelOption,
        ConfirmAnswer       defaultOption,
        @Nullable Icon      icon,
        @Nullable Component parent
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
     *  This method allows you to specify the title of the dialog,
     *  which is the text that will be displayed in the title bar of the dialog window.
     *  If you don't specify a title, a default title may be used based on the dialog type,
     *  so a title does not need to be specified here for the dialog to be shown.
     *
     * @param title The title of the dialog.
     * @return A new {@link ConfirmDialog} instance with the specified title.
     */
    public ConfirmDialog titled( String title ) {
        return new ConfirmDialog(_type, title, _message, _yesOption, _noOption, _cancelOption, _defaultOption, _icon, _parent);
    }

    /**
     *  This method allows you to specify some text that will be used to represent the {@link ConfirmAnswer#YES}
     *  option in the dialog. <br>
     *  So when the user clicks on that option, the dialog will return {@link ConfirmAnswer#YES}.
     *
     * @param yesOption The text of the "yes" option.
     * @return A new {@link ConfirmDialog} instance with the specified "yes" option text.
     */
    public ConfirmDialog yesOption( String yesOption ) {
        return new ConfirmDialog(_type, _title, _message, yesOption, _noOption, _cancelOption, _defaultOption, _icon, _parent);
    }

    /**
     *  This method allows you to specify some text that will be used to represent the {@link ConfirmAnswer#NO}
     *  option in the dialog. <br>
     *  So when the user clicks on that option, the dialog will return {@link ConfirmAnswer#NO}.
     *
     * @param noOption The text of the "no" option.
     * @return A new {@link ConfirmDialog} instance with the specified "no" option text.
     */
    public ConfirmDialog noOption( String noOption ) {
        return new ConfirmDialog(_type, _title, _message, _yesOption, noOption, _cancelOption, _defaultOption, _icon, _parent);
    }

    /**
     *  This method allows you to specify some text that will be used to represent the {@link ConfirmAnswer#CANCEL}
     *  option in the dialog. <br>
     *  So when the user clicks on that option, the dialog will return {@link ConfirmAnswer#CANCEL}.
     *
     * @param cancelOption The text of the "cancel" option.
     * @return A new {@link ConfirmDialog} instance with the specified "cancel" option text.
     */
    public ConfirmDialog cancelOption( String cancelOption ) {
        return new ConfirmDialog(_type, _title, _message, _yesOption, _noOption, cancelOption, _defaultOption, _icon, _parent);
    }

    /**
     *  Use this to specify the default option for the dialog, which is the option
     *  which will have the initial focus when the dialog is shown. <br>
     *  So when the user presses the "Enter" key, the dialog will return the option
     *  that was set as the default option.
     *
     * @param defaultOption The text of the default option.
     * @return A new {@link ConfirmDialog} instance with the specified default option text.
     */
    public ConfirmDialog defaultOption( ConfirmAnswer defaultOption ) {
        return new ConfirmDialog(_type, _title, _message, _yesOption, _noOption, _cancelOption, defaultOption, _icon, _parent);
    }

    /**
     *  Use this to specify the icon for the confirm dialog through an {@link IconDeclaration},
     *  which is a constant that represents the icon resource with a preferred size.
     *  The icon will be loaded and cached automatically for you when using the declaration based approach.
     *
     * @param icon The icon declaration of the dialog, which may contain the path to the icon resource.
     * @return A new {@link ConfirmDialog} instance with the specified icon declaration.
     */
    public ConfirmDialog icon( IconDeclaration icon ) {
        Objects.requireNonNull(icon);
        return icon.find().map(this::icon).orElse(this);
    }

    /**
     *  Defines the icon for the dialog, whose appearance and position may vary depending on the
     *  look and feel of the current system.
     *  Consider using the {@link #icon(IconDeclaration)} method over this one
     *  as it reduces the risk of icon loading issues.
     *
     * @param icon The icon of the dialog.
     * @return A new {@link ConfirmDialog} instance with the specified icon.
     */
    public ConfirmDialog icon( Icon icon ) {
        return new ConfirmDialog(_type, _title, _message, _yesOption, _noOption, _cancelOption, _defaultOption, icon, _parent);
    }

    /**
     *  Use this to display a custom icon in the dialog by
     *  providing the path to the icon resource.
     *  Consider using the {@link #icon(IconDeclaration)} method over this one
     *  as you may also specify the preferred size of the icon through the declaration.
     *
     * @param path The path to the icon of the dialog.
     * @return A new {@link ConfirmDialog} instance with the specified icon.
     */
    public ConfirmDialog icon( String path ) {
        Objects.requireNonNull(path);
        return new ConfirmDialog(_type, _title, _message, _yesOption, _noOption, _cancelOption, _defaultOption, UI.findIcon(path).orElse(null), _parent);
    }

    /**
     *  Allows you to specify the parent component of the dialog, which is the component
     *  that the dialog will be centered on top of. <br>
     *  This is useful when you want to make the dialog modal to a specific component,
     *  but a parent component is not required to show the dialog.
     *
     * @param parent The parent component of the dialog.
     * @return A new {@link ConfirmDialog} instance with the specified parent component.
     */
    public ConfirmDialog parent( Component parent ) {
        return new ConfirmDialog(_type, _title, _message, _yesOption, _noOption, _cancelOption, _defaultOption, _icon, parent);
    }

    /**
     * @param type The type of the dialog, which may be one of the following:
     *             <ul>
     *                  <li>{@link JOptionPane#ERROR_MESSAGE}</li>
     *                  <li>{@link JOptionPane#INFORMATION_MESSAGE}</li>
     *                  <li>{@link JOptionPane#WARNING_MESSAGE}</li>
     *                  <li>{@link JOptionPane#PLAIN_MESSAGE}</li>
     *                  <li>{@link JOptionPane#QUESTION_MESSAGE}</li>
     *             </ul>
     * @return A new {@link ConfirmDialog} instance with the specified type.
     */
    private ConfirmDialog _type( int type ) {
        return new ConfirmDialog(type, _title, _message, _yesOption, _noOption, _cancelOption, _defaultOption, _icon, _parent);
    }

    /**
     *  Shows the confirmation dialog as a question dialog (see {@link JOptionPane#QUESTION_MESSAGE}) and returns the
     *  {@link ConfirmAnswer} that the user selected in the dialog. <br>
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @return The {@link ConfirmAnswer} that the user selected in the dialog.
     */
    public ConfirmAnswer showAsQuestion() {
        return _type(JOptionPane.QUESTION_MESSAGE).show();
    }

    /**
     *  Shows the confirmation dialog as an error dialog (see {@link JOptionPane#ERROR_MESSAGE}) and returns the
     *  {@link ConfirmAnswer} that the user selected in the dialog. <br>
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @return The {@link ConfirmAnswer} that the user selected in the dialog.
     */
    public ConfirmAnswer showAsError() {
        return _type(JOptionPane.ERROR_MESSAGE).show();
    }

    /**
     *  Shows the confirmation dialog as an info dialog (see {@link JOptionPane#INFORMATION_MESSAGE}) and returns the
     *  {@link ConfirmAnswer} that the user selected in the dialog. <br>
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @return The {@link ConfirmAnswer} that the user selected in the dialog.
     */
    public ConfirmAnswer showAsInfo() {
        return _type(JOptionPane.INFORMATION_MESSAGE).show();
    }

    /**
     *  Shows the confirmation dialog as a warning dialog (see {@link JOptionPane#WARNING_MESSAGE}) and returns the
     *  {@link ConfirmAnswer} that the user selected in the dialog. <br>
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @return The {@link ConfirmAnswer} that the user selected in the dialog.
     */
    public ConfirmAnswer showAsWarning() {
        return _type(JOptionPane.WARNING_MESSAGE).show();
    }

    /**
     *  Shows the confirmation dialog as a plain dialog (see {@link JOptionPane#PLAIN_MESSAGE}) and returns the
     *  {@link ConfirmAnswer} that the user selected in the dialog. <br>
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @return The {@link ConfirmAnswer} that the user selected in the dialog.
     */
    public ConfirmAnswer showPlain() {
        return _type(JOptionPane.PLAIN_MESSAGE).show();
    }

    /**
     *  Use this to summon the dialog with the current settings and wait
     *  for the user to select an option. <br>
     *  The answer will be returned as a {@link ConfirmAnswer} enum.
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
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

                int type = _type;
                if ( type == -1 ) {
                    if ( optionsType == JOptionPane.YES_NO_OPTION || optionsType == JOptionPane.YES_NO_CANCEL_OPTION )
                        type = JOptionPane.QUESTION_MESSAGE;
                    else
                        type = JOptionPane.PLAIN_MESSAGE;
                }

                String title = _title.trim();
                if ( title.isEmpty() ) {
                    if ( type == JOptionPane.QUESTION_MESSAGE )
                        title = "Confirm";
                    if ( type == JOptionPane.ERROR_MESSAGE )
                        title = "Error";
                    if ( type == JOptionPane.INFORMATION_MESSAGE )
                        title = "Info";
                    if ( type == JOptionPane.WARNING_MESSAGE )
                        title = "Warning";
                    if ( type == JOptionPane.PLAIN_MESSAGE )
                        title = "Message";
                }

                String defaultOption = "";
                switch ( _defaultOption ) {
                    case YES:    defaultOption = yes;    break;
                    case NO:     defaultOption = no;     break;
                    case CANCEL: defaultOption = cancel; break;
                    default: break;
                }

                return ConfirmAnswer.from(Context.summoner.showOptionDialog(
                            _parent, _message, title, optionsType,
                            type, _icon, options.toArray(), defaultOption
                        ));
            });
        } catch (Exception e) {
            log.error(SwingTree.get().loggingMarker(), "Failed to show confirm dialog, returning 'CANCEL' as dialog result!", e);
            return ConfirmAnswer.CANCEL;
        }
    }
}
