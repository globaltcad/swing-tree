package swingtree.dialogs;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.From;
import sprouts.Var;
import swingtree.UI;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.Objects;
import java.util.Optional;

/**
 *  An immutable builder class for creating simple enum based option dialogs
 *  where the user can select one of the enum options.
 *  <p>
 *  This class is intended to be used as part of the {@link UI} API
 *  by calling the {@link UI#choice(String, Enum[])} or {@link UI#choice(String, Var)} factory methods.
 *  <p>
 *  Note that this API translates to the
 *  {@link JOptionPane#showOptionDialog(Component, Object, String, int, int, Icon, Object[], Object)} method.
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
     * @param icon The icon of the dialog.
     * @return A new {@link OptionsDialog} instance with the specified icon.
     */
    public OptionsDialog<E> icon( Icon icon ) {
        return new OptionsDialog<>(_type, _title, _message, _default, _options, icon, _parent, _property);
    }

    /**
     * @param path The path to the icon of the dialog.
     * @return A new {@link OptionsDialog} instance with the specified icon.
     */
    public OptionsDialog<E> icon( String path ) {
        Objects.requireNonNull(path);
        return new OptionsDialog<>(_type, _title, _message, _default, _options, UI.findIcon(path).orElse(null), _parent, _property);
    }

    /**
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
        return _type(JOptionPane.QUESTION_MESSAGE).show();
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
        return _type(JOptionPane.ERROR_MESSAGE).show();
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
        return _type(JOptionPane.WARNING_MESSAGE).show();
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
        return _type(JOptionPane.INFORMATION_MESSAGE).show();
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
        return _type(JOptionPane.PLAIN_MESSAGE).show();
    }

    /**
     * @return The {@link Enum} that the user selected in the dialog wrapped in an {@link Optional}
     *        or an empty {@link Optional} if the user closed the dialog.
     */
    public Optional<E> show() {
        E[] options = _options;
        if ( options == null ) {
            if ( _property != null )
                options = _property.type().getEnumConstants();
            else {
                log.warn("No options were specified for dialog with title '{}' and message '{}'.", _title, _message);
            }
        }
        if ( options == null )
            options = (E[])new Enum<?>[0];

        String[] asStr = new String[options.length];
        for ( int i = 0; i < options.length; i++ )
            asStr[i] = options[i].toString();

        String defaultOption = _default != null ? _default.toString() : null;

        if ( defaultOption == null ) {
            if ( _property != null && _property.isPresent() )
                defaultOption = _property.get().toString();
            else if ( options.length > 0 )
                defaultOption = options[0].toString();
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
                                    defaultOption               // default option
                                );

        if ( _property != null && selectedIdx >= 0 && options[selectedIdx] != null )
            _property.set( From.VIEW,  options[selectedIdx] );

        return Optional.ofNullable( selectedIdx >= 0 ? options[selectedIdx] : null );
    }
}


