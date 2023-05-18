package swingtree.style;

import javax.swing.*;
import java.util.function.Function;

/**
 *  A {@link StyleDelegate} is a delegate for a {@link JComponent} and its {@link Style} configuration
 *  used to apply further specify the style of said {@link JComponent}.
 *  Instances of this will be exposed to you via the {@link swingtree.UIForAnySwing#withStyle(Function)}
 *  method, where you can specify a lambda that takes a {@link StyleDelegate} and returns a
 *  transformed {@link Style} object, as well as inside of {@link StyleSheet} extensions
 *  where you can declare similar styling lambdas for {@link StyleTrait}s, which are
 *  styling rules... <br>
 *
 * @param <C> The type of {@link JComponent} this {@link StyleDelegate} is for.
 */
public final class StyleDelegate<C extends JComponent>
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
