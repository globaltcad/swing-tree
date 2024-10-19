package swingtree.layout;

/**
 *  A wrapper for mig layout constraint string to avoid the inherent brittleness of strings...
 *  Instances of this are immutable collections of mig layout constraints
 *  which can be merged with other instances of this class through the {@link #and(MigAddConstraint)} method,
 *  which is in essence a wither method.
 *  <br>
 *  Here how this class would typically be used in a swing-tree UI:
 *  <pre>{@code
 *  	import static swingtree.UI.*;
 *
 *      public class MyView extends JPanel {
 *          public MyView() {
 *          	of(this).withLayout(FILL.and(GROW).and(WRAP(3)))
 *          	.add(LEFT, label("Name:") )
 *          	.add(GROW.and(SPAN), textField("name") )
 *          	.add(LEFT, label("Address:") )
 *          	.add(GROW.and(SPAN), textField("address") )
 *           }
 *      }
 *  }</pre>
 *  As you can see this class is not used directly, but rather in the form of static constants
 *  as part of the UI class.
 *  You can define your own component constraints as static constants in your own code
 *  by using the {@link #of(String...)} method.
 */
public final class MigAddConstraint extends AbstractConstraint implements AddConstraint
{
	/**
	 *  Create a new CompAttr with the given layout constraints
	 *  @param layoutConstraints the layout constraints
	 *  @return a new CompAttr, which may represent a single component or a group of layout constraints
	 */
	public static MigAddConstraint of( String... layoutConstraints ) { return new MigAddConstraint(layoutConstraints); }

	private MigAddConstraint(String[] layoutConstraints ) { super(layoutConstraints); }

	private MigAddConstraint() { super(); }

	/**
	 *  Create a new {@link MigAddConstraint} with the provided {@link MigAddConstraint} merged with this one.
	 *
	 * @param attr the {@link MigAddConstraint} to merge with this one
	 * @return a new {@link MigAddConstraint} with the provided {@link MigAddConstraint} merged with this one
	 */
	public MigAddConstraint and( MigAddConstraint attr ) { return (MigAddConstraint) _and( attr, new MigAddConstraint() ); }

	/**
	 *  Create a new {@link MigAddConstraint} with the provided layout constraints merged with this one.
	 *
	 * @param layoutConstraints the string layout constraints to merge with this one
	 * @return a new {@link MigAddConstraint} with the provided layout constraints merged with this one
	 */
	public MigAddConstraint and( String... layoutConstraints ) {
		MigAddConstraint attr = new MigAddConstraint( layoutConstraints );
		return (MigAddConstraint) _and( attr, new MigAddConstraint() );
	}

	@Override
	public Object toConstraintForLayoutManager() {
		return this.toString();
	}
}
