package swingtree.style;

import swingtree.ComponentExtension;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

public final class StyleTrait
{
    private final String _group;
    private final String _id;
    private final String[] _toInherit;
    private final Class<?> _type;

    private StyleTrait( String name, String id, String[] inherits, Class<?> type ) {
        _group = Objects.requireNonNull(name);
        _id = Objects.requireNonNull(id);
        _toInherit = Objects.requireNonNull(inherits).clone();
        _type = Objects.requireNonNull(type);
        // Now we sort the "inherits" groups:
        java.util.Arrays.sort(_toInherit);
        // And we check for duplicates and throw an exception if we find any.
        for ( int i = 0; i < _toInherit.length - 1; i++ )
            if ( _toInherit[i].equals(_toInherit[i+1]) )
                throw new IllegalArgumentException(
                        "Duplicate inheritance found in " + this + "!"
                );
    }

    StyleTrait() { this( "", "", new String[0], Object.class ); }

    public String group() { return _group; }

    public StyleTrait group( String group ) { return new StyleTrait(group, _id, _toInherit, _type); }

    public StyleTrait id( String id ) { return new StyleTrait(_group, id, _toInherit, _type); }

    public String id() { return _id; }

    public String[] inheritance() { return _toInherit; }

    public StyleTrait inherits( String... superGroups ) { return new StyleTrait(_group, _id, superGroups, _type ); }

    public Class<?> type() { return _type; }

    public StyleTrait type(Class<?> type ) { return new StyleTrait(_group, _id, _toInherit, type ); }

    public boolean isApplicableTo( JComponent component, List<String> inheritedNames ) {
        boolean typeIsCompatible = _type.isAssignableFrom(component.getClass());
        boolean idIsCompatible = _id.isEmpty() || _id.equals(component.getName());
        boolean belongsToApplicableGroup =
                ComponentExtension.from(component)
                        .getStyleGroups()
                        .stream()
                        .anyMatch( sg -> {
                            boolean isCompatible = sg.equals(_group);
                            if ( !isCompatible )
                                for ( String extension : _toInherit )
                                    if ( sg.equals(extension) ) {
                                        isCompatible = true;
                                        break;
                                    }
                            return isCompatible;
                        });
        boolean nameIsCompatible = _group.isEmpty() || belongsToApplicableGroup;
        return typeIsCompatible && idIsCompatible && nameIsCompatible;
    }

    public boolean thisInherits( StyleTrait other ) {

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

        return thisIsExtensionOfOther && thisIsSubclassOfOther;
    }

    @Override
    public String toString() {
        return "StyleTrait[" +
                    "name='" + _group + '\'' +
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

        StyleTrait that = (StyleTrait) other;
        return this._group.equals(that._group) &&
                this._id.equals(that._id) &&
                this._type.equals(that._type) &&
                java.util.Arrays.equals(this._toInherit, that._toInherit);
    }

}
