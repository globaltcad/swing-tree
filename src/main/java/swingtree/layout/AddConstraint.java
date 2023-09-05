package swingtree.layout;

/**
 *  A wrapper for mig layout constraint string to avoid the inherent brittleness of strings...
 *  Instances of this are immutable collections of mig layout constraints
 *  which can be merged with other instances of this class through the {@link #and(AddConstraint)} method,
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
public final class AddConstraint extends AbstractConstraint
{
	/**
	 *  Create a new CompAttr with the given layout constraints
	 *  @param layoutConstraints the layout constraints
	 *  @return a new CompAttr, which may represent a single component or a group of layout constraints
	 */
	public static AddConstraint of(String... layoutConstraints ) { return new AddConstraint(layoutConstraints); }

	private AddConstraint(String[] layoutConstraints ) { super(layoutConstraints); }

	private AddConstraint() { super(); }

	/**
	 *  Create a new {@link AddConstraint} with the provided {@link AddConstraint} merged with this one.
	 *
	 * @param attr the {@link AddConstraint} to merge with this one
	 * @return a new {@link AddConstraint} with the provided {@link AddConstraint} merged with this one
	 */
	public AddConstraint and(AddConstraint attr ) { return (AddConstraint) _and( attr, new AddConstraint() ); }

	/**
	 *  Create a new {@link AddConstraint} with the provided layout constraints merged with this one.
	 *
	 * @param layoutConstraints the string layout constraints to merge with this one
	 * @return a new {@link AddConstraint} with the provided layout constraints merged with this one
	 */
	public AddConstraint and(String... layoutConstraints ) {
		AddConstraint attr = new AddConstraint( layoutConstraints );
		return (AddConstraint) _and( attr, new AddConstraint() );
	}

}
