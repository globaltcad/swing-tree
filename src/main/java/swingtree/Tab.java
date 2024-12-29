package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.*;
import swingtree.api.IconDeclaration;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Objects;
import java.util.Optional;

/**
 *  An immutable data carrier exposing everything needed to configure a tab of a {@link JTabbedPane}.
 *  One can create instances of this through the {@link UI#tab(String)} factory method
 *  and then add them to instances of a {@link UIForTabbedPane} builder like so: <br>
 *  <pre>{@code
 *      UI.tabbedPane()
 *      .add(UI.tab("one").add(UI.panel().add(..)))
 *      .add(UI.tab("two").withTip("I give info!").add(UI.label("read me")))
 *      .add(UI.tab("three").withIcon(someIcon).add(UI.button("click me")))
 *  }</pre>
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 */
public final class Tab
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Tab.class);

    @Nullable private final JComponent   _contents;
    @Nullable private final JComponent   _headerComponent;
    @Nullable private final Val<String>  _title;
    @Nullable private final Val<Boolean> _isSelected;
    @Nullable private final Val<Boolean> _isEnabled;
    @Nullable private final Val<Icon>    _icon;
    @Nullable private final Val<String>  _tip;
    @Nullable private final Action<ComponentDelegate<JTabbedPane, ChangeEvent>> _onSelected;
    @Nullable private final Action<ComponentDelegate<JTabbedPane, MouseEvent>>  _onMouseClick;


    Tab(
        @Nullable JComponent   contents,
        @Nullable JComponent   headerComponent,
        @Nullable Val<String>  title,
        @Nullable Val<Boolean> isSelected,
        @Nullable Val<Boolean> isEnabled,
        @Nullable Val<Icon>    icon,
        @Nullable Val<String>  tip,
        @Nullable Action<ComponentDelegate<JTabbedPane, ChangeEvent>> onSelected,
        @Nullable Action<ComponentDelegate<JTabbedPane, MouseEvent>>  onMouseClick
    ) {
        if ( headerComponent == null )
            NullUtil.nullArgCheck(title,"title",String.class);
        if ( title == null )
            NullUtil.nullArgCheck(headerComponent,"headerComponent",JComponent.class);

        _contents        = contents;
        _headerComponent = headerComponent;
        _title           = title;
        _isSelected      = isSelected;
        _isEnabled       = isEnabled;
        _icon            = icon;
        _tip             = tip;
        _onSelected      = onSelected;
        _onMouseClick    = onMouseClick;
    }

    /**
     *  Use this to make the tab selected by default.
     * @param isSelected The selected state of the tab.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab isSelectedIf( boolean isSelected ) {
        if ( _isSelected != null )
            log.warn("Selection flag already specified!", new Throwable());

        return new Tab(_contents, _headerComponent, _title, Var.of(isSelected), _isEnabled, _icon, _tip, _onSelected, _onMouseClick);
    }

    /**
     *  Binds the boolean property passed to this method to the selected state of the tab,
     *  which means that when the state of the property changes, the selected state of the tab will change accordingly.
     *  Conversely, when the tab is selected, the property will be set to true, otherwise it will be set to false.
     *
     * @param isSelected The selected state property of the tab.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab isSelectedIf( Var<Boolean> isSelected ) {
        NullUtil.nullArgCheck(isSelected,"isSelected",Val.class);
        if ( _isSelected != null )
            log.warn("Selection flag already specified!", new Throwable());

        return new Tab(_contents, _headerComponent, _title, isSelected, _isEnabled, _icon, _tip, _onSelected, _onMouseClick);
    }

    /**
     *  Binds the boolean property passed to this method to the selected state of the tab,
     *  which means that when the state of the property changes, the selected state of the tab will change accordingly.
     *  Note that this is not a two-way binding, so when the user changes the selection state of the tab,
     *  the property will not be updated.
     *
     * @param isSelected The selected state property of the tab.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab isSelectedIf( Val<Boolean> isSelected ) {
        NullUtil.nullArgCheck(isSelected,"isSelected",Val.class);
        if ( _isSelected != null )
            log.warn("Selection flag already specified!", new Throwable());

        return new Tab(_contents, _headerComponent, _title, isSelected, _isEnabled, _icon, _tip, _onSelected, _onMouseClick);
    }

    /**
     *  Binds the boolean selection state of the tab to a specific enum value
     *  of a corresponding enum property.
     *  When the enum property is set to the provided enum value, the tab will be selected.
     *
     * @param state The state of the tab.
     * @param selectedState The selected state property of the tab.
     * @param <E> The type of the state.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final <E extends Enum<E>> Tab isSelectedIf( E state, Var<E> selectedState ) {
        NullUtil.nullArgCheck(state,"state",Enum.class);
        NullUtil.nullArgCheck(selectedState,"selectedState",Var.class);
        if ( _isSelected != null )
            log.warn("Selection flag already specified!", new Throwable());

        Var<Boolean> isSelectedModel = Var.of(state == selectedState.get());
        Viewable.cast(selectedState).onChange(From.ALL, it -> {
            isSelectedModel.set(it.channel(), state == it.currentValue().orElseThrowUnchecked());
        });
        Viewable.cast(isSelectedModel).onChange(From.ALL,  it -> {
            if ( it.currentValue().orElseThrowUnchecked() )
                selectedState.set(it.channel(), state);
        });
        return new Tab(_contents, _headerComponent, _title, isSelectedModel, _isEnabled, _icon, _tip, _onSelected, _onMouseClick);
    }

    /**
     *  A tab may be enabled or disabled, which you can specify with this method.
     *
     * @param isEnabled The enabled state of the tab.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab isEnabledIf( boolean isEnabled ) {
        if ( _isEnabled != null )
            log.warn("Enabled flag already specified!", new Throwable());

        return new Tab(_contents, _headerComponent, _title, _isSelected, Val.of(isEnabled), _icon, _tip, _onSelected, _onMouseClick);
    }

    /**
     *   Binds the boolean property passed to this method to the enabled state of the tab,
     *   which means that when the state of the property changes, the enabled state of the tab will change accordingly.
     * @param isEnabled The enabled state property of the tab.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab isEnabledIf( Val<Boolean> isEnabled ) {
        NullUtil.nullArgCheck(isEnabled,"isEnabled",Val.class);
        if ( _isEnabled != null )
            log.warn("Enabled flag already specified!", new Throwable());
        return new Tab(_contents, _headerComponent, _title, _isSelected, isEnabled, _icon, _tip, _onSelected, _onMouseClick);
    }

    /**
     *  Binds the boolean enabled state of the tab to a specific enum value
     *  and a corresponding enum property.
     *  When the enum property is set to the provided enum value, the tab will be selected.
     *
     * @param state The state of the tab.
     * @param enabledState The enabled state property of the tab.
     * @param <E> The type of the state.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final <E extends Enum<E>> Tab isEnabledIf( E state, Val<E> enabledState ) {
        NullUtil.nullArgCheck(state,"state",Enum.class);
        NullUtil.nullArgCheck(enabledState,"enabledState",Val.class);
        if ( _isEnabled != null )
            log.warn("Enabled flag already specified!", new Throwable());
        Val<Boolean> isEnabled = enabledState.viewAs(Boolean.class, it -> it == state );
        return new Tab(_contents, _headerComponent, _title, _isSelected, isEnabled, _icon, _tip, _onSelected, _onMouseClick);
    }

    /**
     *  A tab header may have an icon displayed in it, which you can specify with this method.
     *
     * @param icon The icon which should be displayed in the tab header.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab withIcon( Icon icon ) {
        NullUtil.nullArgCheck(icon,"icon",Icon.class);
        if ( _icon != null )
            log.warn("Icon already specified!", new Throwable());
        return new Tab(_contents, _headerComponent, _title, _isSelected, _isEnabled, Val.of(icon), _tip, _onSelected, _onMouseClick);
    }

    /**
     *  Determines the icon to be displayed in the tab header based on a {@link IconDeclaration},
     *  which is essentially just a path to the icon which should be displayed in the tab header.
     *  If the icon resource is not found, then no icon will be displayed.
     *
     * @param icon The icon declaration, essentially just a path to the icon which should be displayed in the tab header.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab withIcon( IconDeclaration icon ) {
        Objects.requireNonNull(icon);
        return icon.find().map(this::withIcon).orElse(this);
    }

    /**
     *  Allows you to dynamically model the icon displayed on the tab through a property bound
     *  to this tab.
     *  <p>
     *  Note that you may not use the {@link Icon} or {@link ImageIcon} classes directly
     *  as a value for your property,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is the fact that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed, and so they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful in case of unit tests for you view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param iconDeclaration The icon property which should be displayed in the tab header.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab withIcon( Val<IconDeclaration> iconDeclaration ) {
        NullUtil.nullArgCheck(iconDeclaration,"icon",Val.class);
        if ( _icon != null )
            log.warn("Icon already specified!", new Throwable());
        Val<Icon> asIcon = iconDeclaration.viewAs( Icon.class, it -> it.find().orElse(null) );
        return new Tab(_contents, _headerComponent, _title, _isSelected, _isEnabled, asIcon, _tip, _onSelected, _onMouseClick);
    }

    /**
     *  Allows you to define the tooltip which should be displayed when hovering over the tab header.
     *
     * @param tip The tooltip which should be displayed when hovering over the tab header.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab withTip( String tip ) {
        NullUtil.nullArgCheck(tip,"tip",String.class);
        if ( _tip != null )
            log.warn("Tip already specified!", new Throwable());
        return new Tab(_contents, _headerComponent, _title, _isSelected, _isEnabled, _icon, Val.of(tip), _onSelected, _onMouseClick);
    }

    /**
     *  Allows you to bind a string property to the tooltip of the tab.
     *  When the item of the property changes, the tooltip will be updated accordingly.
     *  You can see the tooltip when hovering over the tab header.
     *
     * @param tip The tooltip property which should be displayed when hovering over the tab header.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab withTip( Val<String> tip ) {
        NullUtil.nullArgCheck(tip,"tip",String.class);
        if ( _tip != null )
            log.warn("Tip already specified!", new Throwable());
        return new Tab(_contents, _headerComponent, _title, _isSelected, _isEnabled, _icon, tip, _onSelected, _onMouseClick);
    }

    public final Tab withHeader( JComponent headerComponent ) {
        NullUtil.nullArgCheck(headerComponent,"headerComponent",JComponent.class);
        if ( _headerComponent != null )
            log.warn("Header component already specified!", new Throwable());
        return new Tab(_contents, headerComponent, _title, _isSelected, _isEnabled, _icon, _tip, _onSelected, _onMouseClick);
    }

    /**
     *  Use this to add custom components to the tab header like buttons,
     *  or labels with icons.
     *
     * @param headerComponent The component which should be displayed in the tab header.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab withHeader( UIForAnySwing<?,?> headerComponent ) {
        NullUtil.nullArgCheck(headerComponent,"headerComponent", UIForAnySwing.class);
        return this.withHeader( headerComponent.getComponent() );
    }

    /**
     *  Use this to add the contents UI to the tab.
     *
     * @param contents The contents which should be displayed in the tab.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab add( UIForAnySwing<?,?> contents ) {
        if ( _contents != null )
            log.warn("Content component already specified!", new Throwable());
        return new Tab(contents.getComponent(), _headerComponent, _title, _isSelected, _isEnabled, _icon, _tip, _onSelected, _onMouseClick);
    }

    /**
     *  Use this to add the contents UI to the tab.
     *
     * @param contents The contents which should be displayed in the tab.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab add( JComponent contents ) {
        if ( _contents != null )
            log.warn("Content component already specified!", new Throwable());
        return new Tab(contents, _headerComponent, _title, _isSelected, _isEnabled, _icon, _tip, _onSelected, _onMouseClick);
    }

    /**
     *  Use this to register and catch generic {@link ChangeEvent} based selection events for this tab
     *  and perform some action when the tab is selected.
     *
     * @param onSelected The action to be executed when the tab is selected.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab onSelection( Action<ComponentDelegate<JTabbedPane, ChangeEvent>> onSelected ) {
        if ( _onSelected != null )
            onSelected = _onSelected.andThen(onSelected);
        return new Tab(_contents, _headerComponent, _title, _isSelected, _isEnabled, _icon, _tip, onSelected, _onMouseClick);
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse click events for this tab.
     *  This method adds the provided consumer lambda to the {@link JTabbedPane} that this tab is added to.
     *
     * @param onClick The lambda instance which will be passed to the {@link JTabbedPane} as {@link MouseListener}.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab onMouseClick( Action<ComponentDelegate<JTabbedPane, MouseEvent>> onClick ) {
        if ( _onMouseClick != null )
            onClick = _onMouseClick.andThen(onClick);
        return new Tab(_contents, _headerComponent, _title, _isSelected, _isEnabled, _icon, _tip, _onSelected, onClick);
    }

    final Optional<JComponent> contents() { return Optional.ofNullable(_contents); }

    final Optional<Val<String>> title() { return Optional.ofNullable(_title); }

    final Optional<Val<Boolean>> isSelected() { return Optional.ofNullable(_isSelected); }

    final Optional<Val<Boolean>> isEnabled() { return Optional.ofNullable(_isEnabled); }

    final Optional<Val<Icon>> icon() { return Optional.ofNullable(_icon); }

    final Optional<Val<String>> tip() { return Optional.ofNullable(_tip); }

    final Optional<JComponent> headerContents() { return Optional.ofNullable(_headerComponent); }

    final Optional<Action<ComponentDelegate<JTabbedPane, ChangeEvent>>> onSelection() { return Optional.ofNullable(_onSelected); }

    final Optional<Action<ComponentDelegate<JTabbedPane, MouseEvent>>> onMouseClick() { return Optional.ofNullable(_onMouseClick); }
}
