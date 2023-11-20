package swingtree.dialogs;

import org.slf4j.Logger;
import sprouts.From;
import sprouts.Var;
import swingtree.UI;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.Objects;

public final class SelectEnumDialogBuilder<E extends Enum<E>>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SelectEnumDialogBuilder.class);

    private static final SelectEnumDialogBuilder<?> _QUESTION = _type(JOptionPane.QUESTION_MESSAGE).title("Confirm");
    private static final SelectEnumDialogBuilder<?> _INFO     = _type(JOptionPane.INFORMATION_MESSAGE).title("Info");
    private static final SelectEnumDialogBuilder<?> _WARN     = _type(JOptionPane.WARNING_MESSAGE).title("Warning");
    private static final SelectEnumDialogBuilder<?> _ERROR    = _type(JOptionPane.ERROR_MESSAGE).title("Error");
    private static final SelectEnumDialogBuilder<?> _PLAIN    = _type(JOptionPane.PLAIN_MESSAGE).title("Message");

    /**
     * @return A {@link SelectEnumDialogBuilder} instance that is dedicated for configuring a specific message.
     */
    private static <E extends Enum<E>> SelectEnumDialogBuilder<E> _type( int type ) {
        return new SelectEnumDialogBuilder<E>(type, "", "", null, null, null, null, null);
    }

    public static <E extends Enum<E>> SelectEnumDialogBuilder<E> question( E... options ) { 
        Objects.requireNonNull(options);
        for ( Enum<?> option : options )
            Objects.requireNonNull(option);
        
        return ((SelectEnumDialogBuilder<E>) _QUESTION).options(options);
    }

    public static <E extends Enum<E>> SelectEnumDialogBuilder<E> question( Var<E> property ) { 
        Objects.requireNonNull(property);
        return ((SelectEnumDialogBuilder<E>) _QUESTION).property(property);
    }

    public static <E extends Enum<E>> SelectEnumDialogBuilder<E> info( E... options )     {
        Objects.requireNonNull(options);
        for ( Enum<?> option : options )
            Objects.requireNonNull(option);
        
        return ((SelectEnumDialogBuilder<E>) _INFO).options(options);
    }
    
    public static <E extends Enum<E>> SelectEnumDialogBuilder<E> info( Var<E> property )     {
        Objects.requireNonNull(property);
        return ((SelectEnumDialogBuilder<E>) _INFO).property(property);
    }

    public static <E extends Enum<E>> SelectEnumDialogBuilder<E> warning( E... options )     {
        Objects.requireNonNull(options);
        for ( Enum<?> option : options )
            Objects.requireNonNull(option);
        
        return ((SelectEnumDialogBuilder<E>) _WARN).options(options);
    }
    
    public static <E extends Enum<E>> SelectEnumDialogBuilder<E> warning( Var<E> property )     {
        Objects.requireNonNull(property);
        return ((SelectEnumDialogBuilder<E>) _WARN).property(property);
    }

    public static <E extends Enum<E>> SelectEnumDialogBuilder<E> error( E... options )    {
        Objects.requireNonNull(options);
        for ( Enum<?> option : options )
            Objects.requireNonNull(option);
        
        return ((SelectEnumDialogBuilder<E>) _ERROR).options(options);
    }
    
    public static <E extends Enum<E>> SelectEnumDialogBuilder<E> error( Var<E> property )    {
        Objects.requireNonNull(property);
        return ((SelectEnumDialogBuilder<E>)_ERROR).property(property);
    }

    public static <E extends Enum<E>> SelectEnumDialogBuilder<E> plain( E... options )    {
        Objects.requireNonNull(options);
        for ( Enum<?> option : options )
            Objects.requireNonNull(option);
        
        return ((SelectEnumDialogBuilder<E>) _PLAIN).options(options);
    }

    public static <E extends Enum<E>> SelectEnumDialogBuilder<E> plain( Var<E> property )    {
        Objects.requireNonNull(property);
        return ((SelectEnumDialogBuilder<E>) _PLAIN).property(property);
    }


    private final int          _type;
    private final String       _title;
    private final String       _message;
    private final E      _default;
    private final E[]    _options;
    private final Icon         _icon;
    private final Component    _parent;
    private final Var<E> _property;


    private SelectEnumDialogBuilder(
            int          type,
            String       title,
            String       message,
            E      defaultOption,
            E[]    options,
            Icon         icon,
            Component    parent,
            Var<E> property
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
     * @return A new {@link SelectEnumDialogBuilder} instance with the specified title.
     */
    public SelectEnumDialogBuilder<E> title( String title ) {
        return new SelectEnumDialogBuilder<>(_type, title, _message, _default, _options, _icon, _parent, _property);
    }

    /**
     * @param message The message of the dialog.
     * @return A new {@link SelectEnumDialogBuilder} instance with the specified message.
     */
    public SelectEnumDialogBuilder<E> message( String message ) {
        return new SelectEnumDialogBuilder<>(_type, _title, message, _default, _options, _icon, _parent, _property);
    }

    /**
     * @param defaultOption The default option of the dialog.
     *                      This option will be selected by default.
     * @return A new {@link SelectEnumDialogBuilder} instance with the specified default option.
     */
    public SelectEnumDialogBuilder<E> defaultOption( E defaultOption ) {
        Objects.requireNonNull(defaultOption);
        return new SelectEnumDialogBuilder<>(_type, _title, _message, defaultOption, _options, _icon, _parent, _property);
    }

    /**
     * @param options The options of the dialog.
     *                The user will be able to select one of these options.
     * @return A new {@link SelectEnumDialogBuilder} instance with the specified options.
     */
    private SelectEnumDialogBuilder<E> options( E... options ) {
        Objects.requireNonNull(options);
        for ( Enum<?> option : options )
            Objects.requireNonNull(option);
        return new SelectEnumDialogBuilder<>(_type, _title, _message, _default, options, _icon, _parent, _property);
    }

    /**
     * @param icon The icon of the dialog.
     * @return A new {@link SelectEnumDialogBuilder} instance with the specified icon.
     */
    public SelectEnumDialogBuilder<E> icon( Icon icon ) {
        return new SelectEnumDialogBuilder<>(_type, _title, _message, _default, _options, icon, _parent, _property);
    }

    /**
     * @param path The path to the icon of the dialog.
     * @return A new {@link SelectEnumDialogBuilder} instance with the specified icon.
     */
    public SelectEnumDialogBuilder<E> icon( String path ) {
        Objects.requireNonNull(path);
        return new SelectEnumDialogBuilder<>(_type, _title, _message, _default, _options, UI.findIcon(path).orElse(null), _parent, _property);
    }

    /**
     * @param parent The parent component of the dialog.
     * @return A new {@link SelectEnumDialogBuilder} instance with the specified parent component.
     */
    public SelectEnumDialogBuilder<E> parent( Component parent ) {
        Objects.requireNonNull(parent);
        return new SelectEnumDialogBuilder<>(_type, _title, _message, _default, _options, _icon, parent, _property);
    }
    
    /**
     * @param property The property to which the selected option will be assigned.
     * @return A new {@link SelectEnumDialogBuilder} instance with the specified property.
     */
    private SelectEnumDialogBuilder<E> property( Var<E> property ) {
        Objects.requireNonNull(property);
        E[] options       = _options;
        E   defaultOption = _default;
        if ( options == null ) 
            options = property.type().getEnumConstants();
        if ( defaultOption == null )
            defaultOption = property.orElseNull();
        
        return new SelectEnumDialogBuilder<>(_type, _title, _message, defaultOption, options, _icon, _parent, property);
    }

    /**
     * @return The {@link Enum} that the user selected in the dialog.
     */
    public E show() {
        E[] options       = _options;
        if ( options == null ) {
            if ( _property != null )
                options = _property.type().getEnumConstants();
            else {
                log.warn("No options were specified for dialog with title '{}' and message '{}'.", _title, _message);
            }
        }

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

        int selectedIdx = Context.summoner.showOptionDialog(
                                    _parent,                    // parent component, if this is not null then the dialog will be centered on it
                                    _message,                   // message to display
                                    _title,                     // title of the dialog
                                    JOptionPane.DEFAULT_OPTION, // type of the dialog
                                    _type,                      // type of the dialog
                                    _icon,                      // icon to display
                                    asStr,                      // options to display
                                    defaultOption               // default option
                                );

        if ( _property != null && selectedIdx >= 0 && options[selectedIdx] != null )
            _property.set(From.VIEW,  options[selectedIdx] );

        return selectedIdx >= 0 ? options[selectedIdx] : _default;
    }
}


