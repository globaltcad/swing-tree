package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

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
 *
 */
public final class Tab
{
    private final JComponent _contents;
    private final String _title;
    private final Icon _icon;
    private final String _tip;
    private final UIAction<SimpleDelegate<JTabbedPane, ChangeEvent>> _onSelected;

    Tab(
        JComponent contents,
        String title,
        Icon icon,
        String tip,
        UIAction<SimpleDelegate<JTabbedPane, ChangeEvent>> onSelected
    ) {
        LogUtil.nullArgCheck(title,"title",String.class);
        _contents = contents;
        _title = title;
        _icon = icon;
        _tip = tip;
        _onSelected = onSelected;
    }

    public final Tab withIcon(Icon icon) {
        LogUtil.nullArgCheck(icon,"icon",Icon.class);
        if ( _contents != null ) throw new IllegalArgumentException("Tab object may not be called anymore after the contents were specified!");
        if ( _icon != null ) throw new IllegalArgumentException("Icon already specified!");
        return new Tab(_contents, _title, icon, _tip, _onSelected);
    }

    public final Tab withTip(String tip) {
        LogUtil.nullArgCheck(tip,"tip",String.class);
        if ( _contents != null ) throw new IllegalArgumentException("Tab object may not be called anymore after the contents were specified!");
        if ( _tip != null ) throw new IllegalArgumentException("Tip already specified!");
        return new Tab(_contents, _title, _icon, tip, _onSelected);
    }

    public final Tab add(UIForAbstractSwing<?,?> contents) {
        if ( _contents != null ) throw new IllegalArgumentException("Contents already specified!");
        return new Tab(contents.getComponent(), _title, _icon, _tip, _onSelected);
    }

    public final Tab add(JComponent contents) {
        if ( _contents != null ) throw new IllegalArgumentException("Contents already specified!");
        return new Tab(contents, _title, _icon, _tip, _onSelected);
    }

    public final Tab onSelection(UIAction<SimpleDelegate<JTabbedPane, ChangeEvent>> onSelected) {
        if ( _onSelected != null ) throw new IllegalArgumentException("Selection event already specified!");
        return new Tab(_contents, _title, _icon, _tip, onSelected);
    }

    public final JComponent contents() { return _contents; }

    public final String title() { return _title; }

    public final Icon icon() { return _icon; }

    public final String tip() { return _tip; }

    public final UIAction<SimpleDelegate<JTabbedPane, ChangeEvent>> onSelection() {
        return _onSelected;
    }
}
