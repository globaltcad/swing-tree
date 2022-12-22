package swingtree.layout;

/**
 *  A wrapper for mig layout constraint string to avoid the inherent brittleness of strings...
 */
public final class LayoutAttr extends AbstractAttr
{
	public static LayoutAttr of(String... layoutConstraints ) { return new LayoutAttr(layoutConstraints); }

	private LayoutAttr(String[] layoutConstraints) {
		super(layoutConstraints);
	}

	private LayoutAttr() {
		super();
	}

	public LayoutAttr and( LayoutAttr attr ) {
		return (LayoutAttr) _and( attr, new LayoutAttr() );
	}

}
