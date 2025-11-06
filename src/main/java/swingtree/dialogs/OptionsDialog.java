package swingtree.dialogs;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.From;
import sprouts.Var;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.api.IconDeclaration;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 *  An immutable builder class for creating simple enum based option dialogs
 *  where the user can select one of the enum options.
 *  <p>
 *  This class is intended to be used as part of the {@link UI} API
 *  by calling the {@link UI#choice(String, Enum[])} or {@link UI#choice(String, Var)} factory methods.
 *  <p>
 *  Here a simple usage example:
 *  <pre>{@code
 *      // In your view model:
 *      public enum MyOptions { YES, NO, CANCEL }
 *      private final Var<MyOptions> selectedOption = Var.of(MyOptions.YES);
 *      // In your view:
 *      UI.choice("Select an option:", vm.selectedOption())
 *      .parent(this)
 *      .showAsQuestion( o -> switch(o) {
 *          case YES    -> "Yes, please!";
 *          case NO     -> "No, thank you!";
 *          case CANCEL -> "Cancel";
 *      });
 *  }</pre>
 *  In this example, the user will be presented with a dialog
 *  containing the message "Select an option:" and the enum options "YES", "NO" and "CANCEL"
 *  presented as "Yes, please!", "No, thank you!" and "Cancel" respectively.
 *  The dialog will know the available options from the {@link Var} instance "selectedOption".
 *  <p>
 *  Note that this API translates to the
 *  {@link JOptionPane#showOptionDialog(Component, Object, String, int, int, Icon, Object[], Object)} method.
 *
 * @param <E> The type of the {@link Enum} options that the dialog will present to the user.
 */
public final class OptionsDialog<E extends Enum<E>>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(OptionsDialog.class);

    private static final OptionsDialog<?> _NONE = new OptionsDialog<>(
                                                                -1,
                                                                "",
                                                                "",
                                                                null,
                                                                null,
                                                                null,
                                                                null,
                                                                null
                                                                );

    /**
     *  Creates a new {@link OptionsDialog} instance with the specified message and options.
     *
     * @param message The message of the dialog presenting various options to the user.
     * @param options The {@link Enum} options that the user can select from.
     * @return A new {@link OptionsDialog} instance with the specified message and options.
     * @param <E> The type of the {@link Enum} options.
     */
    public static <E extends Enum<E>> OptionsDialog<E> offering( String message, E... options ) {
        Objects.requireNonNull(options);
        for ( Enum<?> option : options )
            Objects.requireNonNull(option);
        
        return ((OptionsDialog<E>)_NONE).message(message).options(options);
    }


    /**
     *  Creates a new {@link OptionsDialog} instance with the specified message and enum property
     *  from which the options, default option and selected option will be derived.
     *
     * @param message The message of the dialog presenting various options to the user.
     * @param property The property to which the selected option will be assigned.
     * @return A new {@link OptionsDialog} instance with the specified message and options.
     * @param <E> The type of the {@link Enum} options.
     */
    public static <E extends Enum<E>> OptionsDialog<E> offering( String message, Var<E> property ) {
        Objects.requireNonNull(property);
        return ((OptionsDialog<E>)_NONE).message(message).property(property);
    }


    private final int                    _type;
    private final String                 _title;
    private final String                 _message;
    private final @Nullable E            _default;
    private final @Nullable E[]          _options;
    private final @Nullable Icon         _icon;
    private final @Nullable Component    _parent;
    private final @Nullable Var<E>       _property;


    private OptionsDialog(
        int                 type,
        String              title,
        String              message,
        @Nullable E         defaultOption,
        @Nullable E[]       options,
        @Nullable Icon      icon,
        @Nullable Component parent,
        @Nullable Var<E>    property
    ) {
        _type     = type;
        _title    = Objects.requireNonNull(title);
        _message  = Objects.requireNonNull(message);
        _default  = defaultOption;
        _options  = options;
        _icon     = icon;
        _parent   = parent;
        _property = property;
    }

    /**
     *  Creates an updated options dialog config with the specified title
     *  which will used as the window title of the dialog
     *  when it is shown to the user.
     *
     * @param title The title of the dialog.
     * @return A new {@link OptionsDialog} instance with the specified title.
     */
    public OptionsDialog<E> titled( String title ) {
        return new OptionsDialog<>(_type, title, _message, _default, _options, _icon, _parent, _property);
    }

    /**
     * @param message The message of the dialog.
     * @return A new {@link OptionsDialog} instance with the specified message.
     */
    private OptionsDialog<E> message( String message ) {
        return new OptionsDialog<>(_type, _title, message, _default, _options, _icon, _parent, _property);
    }

    /**
     *  Creates an updated options dialog config with the specified default option,
     *  which will be the option with the initial focus when the dialog is shown.
     *  If the user presses the enter key, this option will be selected automatically.
     *
     * @param defaultOption The default option of the dialog.
     *                      This option will be selected by default.
     * @return A new {@link OptionsDialog} instance with the specified default option.
     */
    public OptionsDialog<E> defaultOption( E defaultOption ) {
        Objects.requireNonNull(defaultOption);
        return new OptionsDialog<>(_type, _title, _message, defaultOption, _options, _icon, _parent, _property);
    }

    /**
     * @param options The options of the dialog.
     *                The user will be able to select one of these options.
     * @return A new {@link OptionsDialog} instance with the specified options.
     */
    private OptionsDialog<E> options( E... options ) {
        Objects.requireNonNull(options);
        for ( Enum<?> option : options )
            Objects.requireNonNull(option);
        return new OptionsDialog<>(_type, _title, _message, _default, options, _icon, _parent, _property);
    }

    /**
     *  Allows you to specify an icon declaration for an icon that will be displayed in the dialog window.
     *  An icon declaration is a constant that simply holds the location of the icon resource.
     *  This is the preferred way to specify an icon for the dialog.
     *
     * @param icon The icon declaration for an icon that will be displayed in the dialog window.
     * @return A new {@link OptionsDialog} instance with the specified icon.
     */
    public OptionsDialog<E> icon( IconDeclaration icon ) {
        Objects.requireNonNull(icon);
        return icon.find().map(this::icon).orElse(this);
    }

    /**
     *  Creates an updated options dialog config with the specified icon,
     *  which will be displayed in the dialog window.
     *  Consider using the {@link #icon(IconDeclaration)} method instead,
     *  as it is the preferred way to specify an icon for the dialog.
     *
     * @param icon The icon of the dialog.
     * @return A new {@link OptionsDialog} instance with the specified icon.
     */
    public OptionsDialog<E> icon( Icon icon ) {
        return new OptionsDialog<>(_type, _title, _message, _default, _options, icon, _parent, _property);
    }

    /**
     *  Creates an updated options dialog config with the specified icon path
     *  leading to the icon that will be displayed in the dialog window.
     *  The icon will be loaded using the {@link UI#findIcon(String)} method.
     *  But consider using the {@link #icon(IconDeclaration)} method instead of this,
     *  as it is the preferred way to specify an icon for the dialog.
     *
     * @param path The path to the icon of the dialog.
     * @return A new {@link OptionsDialog} instance with the specified icon.
     */
    public OptionsDialog<E> icon( String path ) {
        Objects.requireNonNull(path);
        return new OptionsDialog<>(_type, _title, _message, _default, _options, UI.findIcon(path).orElse(null), _parent, _property);
    }

    /**
     *  You may specify a reference to a parent component for the dialog,
     *  which will be used to center the dialog on the parent component.
     *  See {@link JOptionPane#showOptionDialog(Component, Object, String, int, int, Icon, Object[], Object)}
     *  for more information.
     *
     * @param parent The parent component of the dialog.
     * @return A new {@link OptionsDialog} instance with the specified parent component.
     */
    public OptionsDialog<E> parent( Component parent ) {
        Objects.requireNonNull(parent);
        return new OptionsDialog<>(_type, _title, _message, _default, _options, _icon, parent, _property);
    }
    
    /**
     * @param property The property to which the selected option will be assigned.
     * @return A new {@link OptionsDialog} instance with the specified property.
     */
    private OptionsDialog<E> property( Var<E> property ) {
        Objects.requireNonNull(property);
        E[] options       = _options;
        E   defaultOption = _default;
        if ( options == null ) 
            options = property.type().getEnumConstants();
        if ( defaultOption == null )
            defaultOption = property.orElseNull();
        
        return new OptionsDialog<>(_type, _title, _message, defaultOption, options, _icon, _parent, property);
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
     * @return A new {@link OptionsDialog} instance with the specified type.
     */
    private OptionsDialog<E> _type( int type ) {
        return new OptionsDialog<>(type, _title, _message, _default, _options, _icon, _parent, _property);
    }

    /**
     *  Shows the options dialog as a question dialog (see {@link JOptionPane#QUESTION_MESSAGE}) and returns the
     *  {@link Enum} answer that the user selected from the existing options.
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @return The {@link Enum} instance that the user selected in the dialog.
     */
    public Optional<E> showAsQuestion() {
        return showAsQuestion(Object::toString);
    }

    /**
     *  Shows the options dialog as a question dialog (see {@link JOptionPane#QUESTION_MESSAGE}) and returns the
     *  {@link Enum} answer that the user selected from the existing options.
     *  The presenter function is used to convert the enum options to strings that
     *  will be displayed in the dialog for the user to select from. <br>
     *  This is useful when your enum constant naming adheres to a specific naming convention,
     *  like capitalized snake case, and you want to present the options in a more user-centric format.
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @param presenter The presenter function that converts the enum options to strings to be displayed to the user.
     * @return The {@link Enum} instance that the user selected in the dialog.
     */
    public Optional<E> showAsQuestion( Function<E, String> presenter ) {
        return _type(JOptionPane.QUESTION_MESSAGE).show(presenter);
    }

    /**
     *  Shows the options dialog as an error dialog (see {@link JOptionPane#ERROR_MESSAGE}) and returns the
     *  {@link Enum} answer that the user selected from the existing options.
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @return The {@link Enum} instance that the user selected in the dialog.
     */
    public Optional<E> showAsError() {
        return showAsError(Object::toString);
    }

    /**
     *  Shows the options dialog as an error dialog (see {@link JOptionPane#ERROR_MESSAGE}) and returns the
     *  {@link Enum} answer that the user selected from the existing options.
     *  The presenter function is used to convert the enum options to strings that
     *  will be displayed in the dialog for the user to select from. <br>
     *  This is useful when your enum constant naming adheres to a specific naming convention,
     *  like capitalized snake case, and you want to present the options in a more user-centric format.
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @param presenter The presenter function that converts the enum options to strings to be displayed to the user.
     * @return The {@link Enum} instance that the user selected in the dialog.
     */
    public Optional<E> showAsError( Function<E, String> presenter ) {
        return _type(JOptionPane.ERROR_MESSAGE).show(presenter);
    }

    /**
     *  Shows the options dialog as a warning dialog (see {@link JOptionPane#WARNING_MESSAGE}) and returns the
     *  {@link Enum} answer that the user selected from the existing options.
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @return The {@link Enum} instance that the user selected in the dialog.
     */
    public Optional<E> showAsWarning() {
        return showAsWarning(Object::toString);
    }

    /**
     *  Shows the options dialog as a warning dialog (see {@link JOptionPane#WARNING_MESSAGE}) and returns the
     *  {@link Enum} answer that the user selected from the existing options.
     *  The presenter function is used to convert the enum options to strings that
     *  will be displayed in the dialog for the user to select from. <br>
     *  This is useful when your enum constant naming adheres to a specific naming convention,
     *  like capitalized snake case, and you want to present the options in a more user-centric format.
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @param presenter The presenter function that converts the enum options to strings to be displayed to the user.
     * @return The {@link Enum} instance that the user selected in the dialog.
     */
    public Optional<E> showAsWarning( Function<E, String> presenter ) {
        return _type(JOptionPane.WARNING_MESSAGE).show(presenter);
    }

    /**
     *  Shows the options dialog as an information dialog (see {@link JOptionPane#INFORMATION_MESSAGE}) and returns the
     *  {@link Enum} answer that the user selected from the existing options.
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @return The {@link Enum} instance that the user selected in the dialog.
     */
    public Optional<E> showAsInfo() {
        return showAsInfo(Object::toString);
    }

    /**
     *  Shows the options dialog as an information dialog (see {@link JOptionPane#INFORMATION_MESSAGE}) and returns the
     *  {@link Enum} answer that the user selected from the existing options.
     *  The presenter function is used to convert the enum options to strings that
     *  will be displayed in the dialog for the user to select from. <br>
     *  This is useful when your enum constant naming adheres to a specific naming convention,
     *  like capitalized snake case, and you want to present the options in a more user-centric format.
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @param presenter The presenter function that converts the enum options to strings to be displayed to the user.
     * @return The {@link Enum} instance that the user selected in the dialog.
     */
    public Optional<E> showAsInfo( Function<E, String> presenter ) {
        return _type(JOptionPane.INFORMATION_MESSAGE).show(presenter);
    }

    /**
     *  Shows the options dialog as a plain dialog (see {@link JOptionPane#PLAIN_MESSAGE}) and returns the
     *  {@link Enum} answer that the user selected from the existing options.
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @return The {@link Enum} instance that the user selected in the dialog.
     */
    public Optional<E> showAsPlain() {
        return showAsPlain(Object::toString);
    }

    /**
     *  Shows the options dialog as a plain dialog (see {@link JOptionPane#PLAIN_MESSAGE}) and returns the
     *  {@link Enum} answer that the user selected from the existing options.
     *  The presenter function is used to convert the enum options to strings that
     *  will be displayed in the dialog for the user to select from. <br>
     *  This is useful when your enum constant naming adheres to a specific naming convention,
     *  like capitalized snake case, and you want to present the options in a more user-centric format.
     *  Note that this method is blocking and will only return when the user has selected
     *  an option in the dialog.
     *
     * @param presenter A function that converts the enum options to strings that will be displayed in the dialog.
     * @return The {@link Enum} instance that the user selected in the dialog.
     */
    public Optional<E> showAsPlain( Function<E, String> presenter ) {
        return _type(JOptionPane.PLAIN_MESSAGE).show(presenter);
    }

    /**
     *  Calling this method causes the dialog to be shown to the user.
     *  The method is blocking and will only return when the user has selected an option
     *  or closed the dialog.
     *  If the dialog is closed, the method will return an empty {@link Optional},
     *  otherwise it will return the {@link Enum} that the user selected.
     *
     * @return The {@link Enum} that the user selected in the dialog wrapped in an {@link Optional}
     *         or an empty {@link Optional} if the user closed the dialog.
     */
    public Optional<E> show() {
        return this.show(Object::toString);
    }

    /**
     *  Calling this method causes the dialog to be shown to the user.
     *  The method is blocking and will only return when the user has selected an option
     *  or closed the dialog.
     *  If the dialog is closed, the method will return an empty {@link Optional},
     *  otherwise it will return the {@link Enum} that the user selected.
     *  The presenter function is used to convert the enum options to strings that
     *  will be displayed in the dialog for the user to select from. <br>
     *  This is useful when your enum constant naming adheres to a specific naming convention,
     *  like capitalized snake case, and you want to present the options in a more user-centric format.
     *
     * @param presenter The presenter function that converts the enum options to strings to be displayed to the user.
     * @return The {@link Enum} that the user selected in the dialog wrapped in an {@link Optional}
     *         or an empty {@link Optional} if the user closed the dialog.
     */
    public Optional<E> show( Function<E, String> presenter ) {
        E[] options = _options;
        if ( options == null ) {
            if ( _property != null )
                options = _property.type().getEnumConstants();
            else {
                log.warn(SwingTree.get().loggingMarker(), "No options were specified for dialog with title '{}' and message '{}'.", _title, _message);
            }
        }
        if ( options == null )
            options = (E[])new Enum<?>[0];

        String[] asStr = new String[options.length];
        for ( int i = 0; i < options.length; i++ ) {
            try {
                asStr[i] = presenter.apply(options[i]);
            } catch ( Exception e ) {
                log.warn(SwingTree.get().loggingMarker(), "An exception occurred while converting an enum option to a string!", e);
                asStr[i] = options[i].toString();
            }
        }

        E defaultOption = _default;

        if ( defaultOption == null ) {
            if ( _property != null && _property.isPresent() )
                defaultOption = _property.get();
            else if ( options.length > 0 )
                defaultOption = options[0];
        }

        String defaultOptionStr = "";
        if ( defaultOption != null ) {
            try {
                defaultOptionStr = presenter.apply(defaultOption);
            } catch ( Exception e ) {
                log.warn(SwingTree.get().loggingMarker(), "An exception occurred while converting the default option to a string!", e);
                defaultOptionStr = defaultOption.toString();
            }
        }

        int type = _type;
        if ( type < 0 ) {
            if ( _property != null || options.length != 0 )
                type = JOptionPane.QUESTION_MESSAGE;
            else {
                type = JOptionPane.PLAIN_MESSAGE;
            }
        }

        int selectedIdx = Context.summoner.showOptionDialog(
                                    _parent,                    // parent component, if this is not null then the dialog will be centered on it
                                    _message,                   // message to display
                                    _title,                     // title of the dialog
                                    JOptionPane.DEFAULT_OPTION, // type of the dialog
                                    type,                       // type of the dialog
                                    _icon,                      // icon to display
                                    asStr,                      // options to display
                                    defaultOptionStr               // default option
                                );

        if ( _property != null && selectedIdx >= 0 && options[selectedIdx] != null )
            _property.set( From.VIEW,  options[selectedIdx] );

        return Optional.ofNullable( selectedIdx >= 0 ? options[selectedIdx] : null );
    }
}


