package com.globaltcad.swingtree;

import javax.swing.*;

public class Tab
{
    private final JComponent _contents;
    private final String _title;
    private final Icon _icon;
    private final String _tip;

    Tab(JComponent contents, String title, Icon icon, String tip) {
        LogUtil.nullArgCheck(title,"title",String.class);
        _contents = contents;
        _title = title;
        _icon = icon;
        _tip = tip;
    }

    public final Tab withIcon(Icon icon) {
        LogUtil.nullArgCheck(icon,"icon",Icon.class);
        if ( _contents != null ) throw new IllegalArgumentException("Tab object may not be called anymore after the contents were specified!");
        if ( _icon != null ) throw new IllegalArgumentException("Icon already specified!");
        return new Tab(_contents, _title, icon, _tip);
    }

    public final Tab withTip(String tip) {
        LogUtil.nullArgCheck(tip,"tip",String.class);
        if ( _contents != null ) throw new IllegalArgumentException("Tab object may not be called anymore after the contents were specified!");
        if ( _tip != null ) throw new IllegalArgumentException("Tip already specified!");
        return new Tab(_contents, _title, _icon, tip);
    }

    public final Tab add(UIForAbstractSwing<?,?> contents) {
        if ( _contents != null ) throw new IllegalArgumentException("Contents already specified!");
        return new Tab(contents.getComponent(), _title, _icon, _tip);
    }

    public final Tab add(JComponent contents) {
        if ( _contents != null ) throw new IllegalArgumentException("Contents already specified!");
        return new Tab(contents, _title, _icon, _tip);
    }

    public JComponent contents() { return _contents; }

    public String title() { return _title; }

    public Icon icon() { return _icon; }

    public String tip() { return _tip; }
}
