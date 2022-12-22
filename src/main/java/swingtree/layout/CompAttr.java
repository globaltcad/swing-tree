package swingtree.layout;

/**
 *  A wrapper for mig layout constraint string to avoid the inherent brittleness of strings...
 */
public final class CompAttr extends AbstractAttr
{
	public static CompAttr of(String... layoutConstraints) { return new CompAttr(layoutConstraints); }

	private CompAttr(String[] layoutConstraints) {
		super(layoutConstraints);
	}

	private CompAttr() {
		super();
	}

	public CompAttr and( CompAttr attr ) {
		return (CompAttr) _and( attr, new CompAttr() );
	}

}
