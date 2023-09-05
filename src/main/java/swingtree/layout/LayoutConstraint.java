package swingtree.layout;

/**
 *  A wrapper for mig layout constraint string to avoid the inherent brittleness of strings...
 *  which can be merged with other instances of this class through the {@link #and(LayoutConstraint)} method,
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
 *  You can define your own mig layout constraints as static constants in your own code
 *  by using the {@link #of(String...)} method.
 */
public final class LayoutConstraint extends AbstractConstraint
{
	/**
	 *  Create a new LayoutConstraint with the given layout constraints
	 *  @param layoutConstraints the layout constraints in the form of a string array.
	 *  @return a new LayoutConstraint, which may represent a single component or a group of layout constraints.
	 */
	public static LayoutConstraint of( String... layoutConstraints ) { return new LayoutConstraint(layoutConstraints); }

	private LayoutConstraint( String[] layoutConstraints ) { super(layoutConstraints); }

	private LayoutConstraint() { super(); }

	/**
	 *  Create a new {@link LayoutConstraint} with the provided {@link LayoutConstraint} merged with this one.
	 *
	 * @param attr the {@link LayoutConstraint} to merge with this one
	 * @return a new {@link LayoutConstraint} with the provided {@link LayoutConstraint} merged with this one
	 */
	public LayoutConstraint and( LayoutConstraint attr ) { return (LayoutConstraint) _and( attr, new LayoutConstraint() ); }

	/**
	 *  Create a new {@link LayoutConstraint} with the provided layout constraints merged with this one.
	 *
	 * @param layoutConstraints the string layout constraints to merge with this one
	 * @return a new {@link LayoutConstraint} with the provided layout constraints merged with this one
	 */
	public LayoutConstraint and( String... layoutConstraints ) {
		LayoutConstraint attr = new LayoutConstraint( layoutConstraints );
		return (LayoutConstraint) _and( attr, new LayoutConstraint() );
	}
}
