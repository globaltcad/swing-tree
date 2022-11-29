package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Optional;

/**
 *  An immutable data carrier exposing everything needed to configure a tab of a {@link JTabbedPane}.
 *  One can create instances of this through the {@link UI#tab(String)} factory method
 *  and then add them to instances of a {@link UIForTabbedPane} builder like so: <br>
 *  <pre>{@code
 *      UI.tabbedPane()
 *      .add(UI.tab("one").add(UI.panel().add(..)))
 *      .add(UI.tab("two").withTip("I give info!").add(UI.label("read me")))
 *      .add(UI.tab("three").with(someIcon).add(UI.button("click me")))
 *  }</pre>
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 */
public final class Tab
{
    private final JComponent _contents;
    private final JComponent _headerComponent;
    private final String _title;
    private final Icon _icon;
    private final String _tip;
    private final UIAction<SimpleDelegate<JTabbedPane, ChangeEvent>> _onSelected;

    private final UIAction<SimpleDelegate<JTabbedPane, MouseEvent>> _onMouseClick;

    Tab(
        JComponent contents,
        JComponent headerComponent,
        String title,
        Icon icon,
        String tip,
        UIAction<SimpleDelegate<JTabbedPane, ChangeEvent>> onSelected,
        UIAction<SimpleDelegate<JTabbedPane, MouseEvent>> onMouseClick
    ) {
        if ( headerComponent == null )
            NullUtil.nullArgCheck(title,"title",String.class);
        if ( title == null )
            NullUtil.nullArgCheck(headerComponent,"headerComponent",JComponent.class);
        _contents = contents;
        _headerComponent = headerComponent;
        _title = title;
        _icon = icon;
        _tip = tip;
        _onSelected = onSelected;
        _onMouseClick = onMouseClick;
    }

    /**
     * @param icon The icon which should be displayed in the tab header.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab withIcon(Icon icon) {
        NullUtil.nullArgCheck(icon,"icon",Icon.class);
        if ( _contents != null ) throw new IllegalArgumentException("Tab object may not be called anymore after the contents were specified!");
        if ( _icon != null ) throw new IllegalArgumentException("Icon already specified!");
        return new Tab(_contents, _headerComponent, _title, icon, _tip, _onSelected, _onMouseClick);
    }

    /**
     * @param tip The tooltip which should be displayed when hovering over the tab header.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab withTip(String tip) {
        NullUtil.nullArgCheck(tip,"tip",String.class);
        if ( _contents != null ) throw new IllegalArgumentException("Tab object may not be called anymore after the contents were specified!");
        if ( _tip != null ) throw new IllegalArgumentException("Tip already specified!");
        return new Tab(_contents, _headerComponent, _title, _icon, tip, _onSelected, _onMouseClick);
    }

    public final Tab withHeader( JComponent headerComponent ) {
        NullUtil.nullArgCheck(headerComponent,"headerComponent",JComponent.class);
        if ( _contents != null ) throw new IllegalArgumentException("Tab object may not be called anymore after the contents were specified!");
        if ( _headerComponent != null ) throw new IllegalArgumentException("Header component already specified!");
        return new Tab(_contents, headerComponent, _title, _icon, _tip, _onSelected, _onMouseClick);
    }

    public final Tab withHeader( UIForAbstractSwing<?,?> headerComponent ) {
        NullUtil.nullArgCheck(headerComponent,"headerComponent",UIForAbstractSwing.class);
        return this.withHeader( headerComponent.getComponent() );
    }

    public final Tab add(UIForAbstractSwing<?,?> contents) {
        if ( _contents != null ) throw new IllegalArgumentException("Contents already specified!");
        return new Tab(contents.getComponent(), _headerComponent, _title, _icon, _tip, _onSelected, _onMouseClick);
    }

    public final Tab add(JComponent contents) {
        if ( _contents != null ) throw new IllegalArgumentException("Contents already specified!");
        return new Tab(contents, _headerComponent, _title, _icon, _tip, _onSelected, _onMouseClick);
    }

    /**
     * @param onSelected The action to be executed when the tab is selected.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab onSelection( UIAction<SimpleDelegate<JTabbedPane, ChangeEvent>> onSelected ) {
        if ( _onSelected != null ) throw new IllegalArgumentException("Selection event already specified!");
        return new Tab(_contents, _headerComponent, _title, _icon, _tip, onSelected, _onMouseClick);
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse click events for this tab.
     *  This method adds the provided consumer lambda to the {@link JTabbedPane} that this tab is added to.
     *
     * @param onClick The lambda instance which will be passed to the {@link JTabbedPane} as {@link MouseListener}.
     * @return A new {@link Tab} instance with the provided argument, which enables builder-style method chaining.
     */
    public final Tab onMouseClick( UIAction<SimpleDelegate<JTabbedPane, MouseEvent>> onClick ) {
        if ( _onMouseClick != null ) throw new IllegalArgumentException("Mouse click event already specified!");
        return new Tab(_contents, _headerComponent, _title, _icon, _tip, _onSelected, onClick);
    }

    final Optional<JComponent> contents() { return Optional.ofNullable(_contents); }

    final Optional<String> title() { return Optional.ofNullable(_title); }

    final Optional<Icon> icon() { return Optional.ofNullable(_icon); }

    final Optional<String> tip() { return Optional.ofNullable(_tip); }

    final Optional<JComponent> headerContents() { return Optional.ofNullable(_headerComponent); }

    final Optional<UIAction<SimpleDelegate<JTabbedPane, ChangeEvent>>> onSelection() {
        return Optional.ofNullable(_onSelected);
    }

    final Optional<UIAction<SimpleDelegate<JTabbedPane, MouseEvent>>> onMouseClick() { return Optional.ofNullable(_onMouseClick); }
}
