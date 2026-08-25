package examples.laf;

import swingtree.api.Styler;

import javax.swing.JComponent;
import java.util.Objects;

/**
 *  One entry of a look-and-feel style table: a {@link Styler} together with the component type
 *  it governs. A rule applies to that type and to every subtype of it, which is what lets a
 *  preset write one rule for {@code AbstractButton} and a second, more specific one for
 *  {@code JCheckBox}.
 *
 *  @see SwingTreeLookAndFeel.Conf#stylerFor(Class)
 */
final class StyleRule
{
    private final Class<? extends JComponent> _type;
    private final Styler<?>                   _styler;

    /** Ties the two type parameters together, so a button rule cannot be registered against a text field. */
    static <C extends JComponent> StyleRule of( Class<C> type, Styler<C> styler ) {
        return new StyleRule(type, styler);
    }

    StyleRule( Class<? extends JComponent> type, Styler<?> styler ) {
        _type   = Objects.requireNonNull(type);
        _styler = Objects.requireNonNull(styler);
    }

    Class<? extends JComponent> type() { return _type; }

    Styler<?> styler() { return _styler; }

    /** Whether this rule governs the given runtime class. */
    boolean appliesTo( Class<?> componentType ) { return _type.isAssignableFrom(componentType); }

    @Override
    public String toString() { return getClass().getSimpleName() + "[" + _type.getSimpleName() + "]"; }
}
