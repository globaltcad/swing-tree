package swingtree.style;

import swingtree.api.Styler;

import javax.swing.*;
import java.util.Objects;

/**
 *  A {@link StyleTrait} contains a set of properties that will be used to
 *  target specific {@link JComponent}s matching said properties, so that
 *  you can associate custom {@link Styler} lambdas to them
 *  which are using the {@link ComponentStyleDelegate} API
 *  to configure the style of the component. <br>
 *  See {@link StyleSheet#add(StyleTrait, Styler)} for more information. <br>
 *  Instances of this are supposed to be created and registered inside
 *  custom {@link StyleSheet} extensions, more specifically a {@link swingtree.style.StyleSheet#configure()}
 *  implementation in which you can register your {@link StyleTrait}s and
 *  {@link Styler}s using the {@link StyleSheet#add(StyleTrait, Styler)} method.
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
        _group     = Objects.requireNonNull(name);
        _id        = Objects.requireNonNull(id);
        _toInherit = Objects.requireNonNull(inherits).clone();
        _type      = Objects.requireNonNull(type);
        // And we check for duplicates and throw an exception if we find any.
        for ( int i = 0; i < _toInherit.length - 1; i++ )
            if ( _toInherit[i].equals(_toInherit[i+1]) )
                throw new IllegalArgumentException(
                        "Duplicate inheritance found in " + this + "!"
                );
    }

    StyleTrait() { this( "", "", new String[0], (Class<C>) JComponent.class); }


    String group() { return _group; }

    String id() { return _id; }

    String[] inheritance() { return _toInherit; }

    Class<?> type() { return _type; }


    /**
     *  Creates a new {@link StyleTrait} with the same properties as this one,
     *  but with the given group name. <br>
     *  <b>
     *      Note that this method defines the group in terms of a {@link String}
     *      which can be problematic with respect to compile-time safety. <br>
     *      Please consider using {@link #group(Enum)} instead.
     *  </b>
     *
     * @param group The new group name.
     * @return A new {@link StyleTrait} with the same properties as this one,
     *         but with the given group name.
     */
    public StyleTrait<C> group( String group ) { return new StyleTrait<>(group, _id, _toInherit, _type); }

    /**
     *  Creates a new {@link StyleTrait} with the same properties as this one,
     *  but with the given group in terms of an {@link Enum}. <br>
     *
     * @param group The new group in terms of an {@link Enum}.
     * @param <E> The type of the {@link Enum} to use as the group enum
     * @return A new {@link StyleTrait} with the same properties as this one,
     *         but with the given group name.
     */
    public <E extends Enum<E>> StyleTrait<C> group( E group ) {
        Objects.requireNonNull(group);
        return group(StyleUtility.toString(group));
    }

    /**
     *  Creates a new {@link StyleTrait} with the same properties as this one,
     *  but with the given id. <br>
     *  <b>
     *      Note that this method defines the id in terms of a {@link String}
     *      which can be problematic with respect to compile-time safety. <br>
     *      Please consider using {@link #id(Enum)} instead.
     *  </b>
     *
     * @param id The new id.
     * @return A new {@link StyleTrait} with the same properties as this one,
     *         but with the given id.
     */
    public StyleTrait<C> id( String id ) { return new StyleTrait<>(_group, id, _toInherit, _type); }

    /**
     *  Creates a new {@link StyleTrait} with the same properties as this one,
     *  but with the given id in terms of an {@link Enum}. <br>
     *
     * @param id The new id in terms of an {@link Enum}.
     * @param <E> The type of the {@link Enum} to use as the id enum
     * @return A new {@link StyleTrait} with the same properties as this one,
     *         but with the given id.
     */
    public <E extends Enum<E>> StyleTrait<C> id( E id ) {
        Objects.requireNonNull(id);
        return id(StyleUtility.toString(id));
    }

    /**
     *  Creates a new {@link StyleTrait} with the same properties as this one,
     *  but with an array of groups to inherit from. <br>
     *  <b>
     *      Note that this method defines the groups in terms of {@link String}s
     *      which can be problematic with respect to compile-time safety. <br>
     *      Please consider using {@link #inherits(Enum[])} instead.
     *  </b>
     *
     * @param superGroups The new groups to inherit from.
     * @return A new {@link StyleTrait} with the same properties as this one,
     *         but with the given groups to inherit from.
     */
    public StyleTrait<C> inherits( String... superGroups ) { return new StyleTrait<>(_group, _id, superGroups, _type ); }

    /**
     *  Creates a new {@link StyleTrait} with the same properties as this one,
     *  but with an array of groups to inherit from in terms of {@link Enum}s. <br>
     *
     * @param superGroups The new groups to inherit from in terms of {@link Enum}s.
     * @param <E> The type of the {@link Enum}s to use as the super group enums
     * @return A new {@link StyleTrait} with the same properties as this one,
     *         but with the given groups to inherit from.
     */
    @SafeVarargs
    public final <E extends Enum<E>> StyleTrait<C> inherits( E... superGroups ) {
        String[] superGroupNames = new String[superGroups.length];
        for ( int i = 0; i < superGroups.length; i++ ) {
            E superGroup = Objects.requireNonNull(superGroups[i]);
            superGroupNames[i] = StyleUtility.toString(superGroup);
        }
        return inherits(superGroupNames);
    }

    /**
     *  Creates a new {@link StyleTrait} with the same properties as this one,
     *  but with the given component type to which a style should be applied. <br>
     *
     * @param type The new type.
     * @param <T> The type of the {@link JComponent} to use as the type
     * @return A new {@link StyleTrait} with the same properties as this one,
     *         but with the given type.
     */
    public <T extends C> StyleTrait<T> type( Class<T> type ) {
        return new StyleTrait<>(_group, _id, _toInherit, type );
    }

    boolean isApplicableTo( JComponent component ) {
        Objects.requireNonNull(component);
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

    boolean thisInherits( StyleTrait<?> other ) {

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

    @Override
    public String toString() {
        String inherits = java.util.Arrays.toString(_toInherit);
        return "StyleTrait[" +
                    "group='"   + _group   + "', " +
                    "id='"      + _id      + "', " +
                    "inherits=" + inherits + ", "  +
                    "type="     + _type    +
                ']';
    }

    @Override
    public int hashCode() {
        // Note that the order of the inheritance list is not important.
        int sum = 0;
        for ( String inherit : _toInherit)
            sum += inherit.hashCode();

        return Objects.hash( _group, _id, sum, _type );
    }

    @Override
    public boolean equals( Object other ) {
        if ( !( other instanceof StyleTrait ) )
            return false;

        StyleTrait<?> that = (StyleTrait<?>) other;
        return _group .equals( that._group ) &&
               _id    .equals( that._id    ) &&
               _type  .equals( that._type  ) &&
                java.util.Arrays.equals(_toInherit, that._toInherit);
    }

}
