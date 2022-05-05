package com.globaltcad.swingtree;

import javax.swing.*;

public class Tab
{
    private final JComponent _contents;
    private final String _title;
    private final Icon _icon;
    private final String _tip;

    public Tab of(JComponent component, String title) {
        return new Tab(component, title, null, null);
    }

    private Tab(JComponent contents, String title, Icon icon, String tip) {
        LogUtil.nullArgCheck(contents, "contents", JComponent.class);
        LogUtil.nullArgCheck(title, "name", String.class);
        _contents = contents;
        _title = title;
        _icon = icon;
        _tip = tip;
    }

    public final Tab withIcon(Icon icon) {
        if ( _icon != null ) throw new IllegalArgumentException("Icon already specified!");
        return new Tab(_contents, _title, icon, _tip);
    }

    public final Tab withTip(String tip) {
        if ( _tip != null ) throw new IllegalArgumentException("Tip already specified!");
        return new Tab(_contents, _title, _icon, tip);
    }

    public JComponent contents() { return _contents; }

    public String title() { return _title; }

    public Icon icon() { return _icon; }

    public String tip() { return _tip; }
}
