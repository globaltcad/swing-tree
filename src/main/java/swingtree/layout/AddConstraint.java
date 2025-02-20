package swingtree.layout;

/**
 * Represents a constraint that can be added to a layout manager
 * on a component by component basis. A typical example of a constraint
 * are layout constants like {@link swingtree.UILayoutConstants#GROW},
 * {@link swingtree.UILayoutConstants#WRAP}, etc.
 */
public interface AddConstraint {

    /**
     *  Supplies the actual {@link Object} that is used by a particular layout manager
     *  to apply the constraint to a component.
     *  @return The {@link Object} that is used by a particular layout manager
     *          to apply the constraint to a component.
     */
    Object toConstraintForLayoutManager();

}
