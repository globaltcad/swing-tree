package swingtree.style;

import swingtree.ComponentExtension;

import javax.swing.*;
import java.util.Objects;

/**
 *  A {@link StyleTrait} is a set of properties that will be used to
 *  target specific {@link JComponent}s matching said properties, so that
 *  you can apply custom {@link Style}a to them using the {@link StyleDelegate} API
 *  exposed by methods like {@link StyleSheet#add(StyleTrait, Styler)}
 *  (or on component builders directly through {@link swingtree.UIForAnySwing#withStyle(Styler)}). <br>
 *  Instances of this are supposed to be created and registered inside
 *  custom {@link StyleSheet} extensions which you can use to apply
 *  custom styles to your UIs. <br>
 *
 * @param <C> The type of {@link JComponent} this {@link StyleTrait} is for.
 */
public final class StyleTrait<C extends JComponent>
{
    private final String   _group;
    private final String   _id;
    private final String[] _toInherit;
    private final Class<C> _type;

    private StyleTrait( String name, String id, String[] inherits, Class<C> type ) {
        _group = Objects.requireNonNull(name);
        _id = Objects.requireNonNull(id);
        _toInherit = Objects.requireNonNull(inherits).clone();
        _type = Objects.requireNonNull(type);
        // And we check for duplicates and throw an exception if we find any.
        for ( int i = 0; i < _toInherit.length - 1; i++ )
            if ( _toInherit[i].equals(_toInherit[i+1]) )
                throw new IllegalArgumentException(
                        "Duplicate inheritance found in " + this + "!"
                );
    }

    StyleTrait() { this( "", "", new String[0], (Class<C>) JComponent.class); }

    public String group() { return _group; }

    public StyleTrait<C> group( String group ) { return new StyleTrait<>(group, _id, _toInherit, _type); }

    public <E extends Enum<E>> StyleTrait<C> group( E group ) { return group(group.name()); }

    public StyleTrait<C> id( String id ) { return new StyleTrait<>(_group, id, _toInherit, _type); }

    public <E extends Enum<E>> StyleTrait<C> id( E id ) { return id(id.name()); }

    public String id() { return _id; }

    public String[] inheritance() { return _toInherit; }

    public StyleTrait<C> inherits( String... superGroups ) { return new StyleTrait<>(_group, _id, superGroups, _type ); }

    public <E extends Enum<E>> StyleTrait<C> inherits( E... superGroups ) {
        String[] superGroupNames = new String[superGroups.length];
        for ( int i = 0; i < superGroups.length; i++ )
            superGroupNames[i] = superGroups[i].name();
        return inherits(superGroupNames);
    }

    public Class<?> type() { return _type; }

    public <T extends JComponent> StyleTrait<T> type( Class<T> type ) { return new StyleTrait<>(_group, _id, _toInherit, type ); }

    public boolean isApplicableTo( JComponent component ) {
        boolean typeIsCompatible = _type.isAssignableFrom(component.getClass());
        boolean idIsCompatible = _id.isEmpty() || _id.equals(component.getName());
        boolean belongsToApplicableGroup =
                ComponentExtension.from(component)
                        .getStyleGroups()
                        .stream()
                        .anyMatch( sg -> sg.equals(_group));

        boolean nameIsCompatible = _group.isEmpty() || belongsToApplicableGroup;
        return typeIsCompatible && idIsCompatible && nameIsCompatible;
    }

    public boolean thisInherits( StyleTrait<?> other ) {

        if ( !this.id().isEmpty() || !other.id().isEmpty() )
            return false;

        boolean thisIsExtensionOfOther = false;
        for ( String extension : this.inheritance() )
            if ( extension.equals(other.group()) ) {
                thisIsExtensionOfOther = true;
                break;
            }

        boolean thisIsSubclassOfOther = other.type().isAssignableFrom(this.type());

        if ( thisIsExtensionOfOther && !thisIsSubclassOfOther )
            throw new IllegalArgumentException(
                    this + " is an extension of " + other + " but is not a subclass of it."
                );

        return (thisIsExtensionOfOther || other.group().isEmpty()) && thisIsSubclassOfOther;
    }

    public boolean typeInherits(StyleTrait<?> other ) {
        if ( this.type().equals(other.type()) )
            return true;
        else if ( other.type().isAssignableFrom(this.type()) )
            return true;
        else
            return false;
    }

    @Override
    public String toString() {
        return "StyleTrait[" +
                    "group='" + _group + '\'' +
                    ", id='" + _id + '\'' +
                    ", inherits=" + java.util.Arrays.toString(_toInherit) +
                    ", type=" + _type +
                ']';
    }

    @Override
    public int hashCode() {
        // Note that the order of the inheritance list is not important.
        int sum = 0;
        for ( String inherit : _toInherit)
            sum += inherit.hashCode();
        return Objects.hash(_group, _id, sum, _type);
    }

    @Override
    public boolean equals( Object other ) {
        if ( !( other instanceof StyleTrait ) )
            return false;

        StyleTrait<?> that = (StyleTrait<?>) other;
        return this._group.equals(that._group) &&
                this._id.equals(that._id) &&
                this._type.equals(that._type) &&
                java.util.Arrays.equals(this._toInherit, that._toInherit);
    }

}
