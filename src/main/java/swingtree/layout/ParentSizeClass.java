package swingtree.layout;

/**
 * Represents a classification of the size of a parent component in a {@link ResponsiveGridFlowLayout}
 * that serves as a sort of layout mode for the child components.
 * <p>
 *     The size classes are ordered from smallest to largest, and are used to determine the
 *     relative size of the parent component in relation to the other components in the layout.
 * </p>
 */
public enum ParentSizeClass
{
    /**
     * The size is considered to be void, meaning that the
     * dimension smaller or equal to {@code 0}.
     */
    VOID,
    /**
     * The size is considered to be very small, meaning that the
     * dimension is greater than {@code 0} and less than 1/5 of its preferred size.
     */
    VERY_SMALL,
    /**
     * The size is considered to be small, meaning that the
     * dimension is greater than or equal to 1/5 of its preferred size
     * and less than 2/5 of its preferred size.
     */
    SMALL,
    /**
     * The size is considered to be medium, meaning that the
     * dimension is greater than or equal to 2/5 of its preferred size
     * and less than 3/5 of its preferred size.
     */
    MEDIUM,
    /**
     * The size is considered to be large, meaning that the
     * dimension is greater than or equal to 3/5 of its preferred size
     * and less than 4/5 of its preferred size.
     */
    LARGE,
    /**
     * The size is considered to be very large, meaning that the
     * dimension is greater than or equal to 4/5 of its preferred size
     * and less than 5/5 of its preferred size.
     */
    VERY_LARGE,
    /**
     * The size is considered to be oversize, meaning that the
     * dimension is greater than or equal to 5/5 of its preferred size.
     */
    OVERSIZE;

    /**
     * Returns the {@link ParentSizeClass} that corresponds to the given current size and preferred size.
     * @param currentSize The current size of the parent component.
     * @param preferredSize The preferred size of the parent component.
     * @return The {@link ParentSizeClass} that corresponds to the given current size and preferred size.
     */
    static ParentSizeClass of( int currentSize, int preferredSize ) {
        // How much preferred width the parent actually fills:
        double howFull = currentSize / (double) preferredSize;
        howFull = Math.max(0, howFull);

        if ( howFull <= 0 ) {
            return ParentSizeClass.VOID;
        } else if (howFull < 1/5d) {
            return ParentSizeClass.VERY_SMALL;
        } else if (howFull < 2/5d) {
            return ParentSizeClass.SMALL;
        } else if (howFull < 3/5d) {
            return ParentSizeClass.MEDIUM;
        } else if (howFull < 4/5d) {
            return ParentSizeClass.LARGE;
        } else if (howFull <= 1) {
            return ParentSizeClass.VERY_LARGE;
        }
        return ParentSizeClass.OVERSIZE;
    }
}
