package swingtree.style;

import javax.swing.*;

public class StyleDelegate<C extends JComponent>
{
    private final C _component;
    private final Style _style;

    public StyleDelegate( C component, Style style ) {
        _component = component;
        _style = style;
    }

    public C component() { return _component; }

    public Style style() { return _style; }
}
