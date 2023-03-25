package swingtree.layout;

/**
 *  A wrapper for mig layout constraint string to avoid the inherent brittleness of strings...
 *  which can be merged with other instances of this class through the {@link #and(LayoutAttr)} method,
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
public final class LayoutAttr extends AbstractAttr
{
	public static LayoutAttr of( String... layoutConstraints ) { return new LayoutAttr(layoutConstraints); }

	private LayoutAttr( String[] layoutConstraints ) { super(layoutConstraints); }

	private LayoutAttr() { super(); }

	/**
	 *  Create a new {@link LayoutAttr} with the provided {@link LayoutAttr} merged with this one.
	 *
	 * @param attr the {@link LayoutAttr} to merge with this one
	 * @return a new {@link LayoutAttr} with the provided {@link LayoutAttr} merged with this one
	 */
	public LayoutAttr and( LayoutAttr attr ) { return (LayoutAttr) _and( attr, new LayoutAttr() ); }

	/**
	 *  Create a new {@link LayoutAttr} with the provided layout constraints merged with this one.
	 *
	 * @param layoutConstraints the string layout constraints to merge with this one
	 * @return a new {@link LayoutAttr} with the provided layout constraints merged with this one
	 */
	public LayoutAttr and( String... layoutConstraints ) {
		LayoutAttr attr = new LayoutAttr( layoutConstraints );
		return (LayoutAttr) _and( attr, new LayoutAttr() );
	}
}
