package swingtree;

/**
 *  A set of extension methods for various SwingTree enums.
 * @param <E> The enum type.
 */
public /*seiled*/ interface UIEnum<E extends Enum<E>> /*permits UI...*/
{
    /**
     *  Checks if this enum value is one of the given values.
     *
     * @param first The first enum value to compare to.
     * @param rest The rest of the enum values to compare to.
     * @return True if this enum value is one of the given values.
     */
    default boolean isOneOf( E first, E... rest )
    {
        if ( this == first )
            return true;
        for ( E value : rest )
            if ( this == value )
                return true;

        return false;
    }

    /**
     *  Checks if this enum value is one of the 2 given values.
     *
     * @param first The first enum value to compare to.
     * @param second The second enum value to compare to.
     * @return True if this enum value is one of the given values.
     */
    default boolean isOneOf( E first, E second )
    {
        return this == first || this == second;
    }

    /**
     *  Checks if this enum value is none of the given values.
     *
     * @param first The first enum value to compare to.
     * @param rest The rest of the enum values to compare to.
     * @return True if this enum value is none of the given values.
     */
    default boolean isNoneOf( E first, E... rest )
    {
        return !isOneOf( first, rest );
    }

    /**
     *  Checks if this enum value is none of the 2 given values.
     *
     * @param first The first enum value to compare to.
     * @param second The second enum value to compare to.
     * @return True if this enum value is none of the given values.
     */
    default boolean isNoneOf( E first, E second )
    {
        return !isOneOf( first, second );
    }

    /**
     *  Checks if this enum value is one of the given iterable values.
     *  @param values The iterable values to compare to.
     *  @return True if this enum value is one of the given values.
     */
    default boolean isOneOf( Iterable<E> values )
    {
        for ( E value : values )
            if ( this == value )
                return true;

        return false;
    }

    /**
     *  Checks if this enum value is none of the given iterable values.
     *  @param values The iterable values to compare to.
     *  @return True if this enum value is none of the given values.
     */
    default boolean isNoneOf( Iterable<E> values )
    {
        return !isOneOf( values );
    }
}
