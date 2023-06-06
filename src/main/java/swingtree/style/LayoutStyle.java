package swingtree.style;

final class LayoutStyle
{
    private static final LayoutStyle _NONE = new LayoutStyle( Outline.none(), Outline.none() );

    public static LayoutStyle none() { return _NONE; }

    private final Outline margin;
    private final Outline padding;

    LayoutStyle( Outline margin, Outline padding ) {
        this.margin = margin;
        this.padding = padding;
    }

    Outline margin() { return margin; }

    Outline padding() { return padding; }

    LayoutStyle margin( Outline margin ) { return new LayoutStyle( margin, padding ); }

    LayoutStyle padding( Outline padding ) { return new LayoutStyle( margin, padding ); }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + margin.hashCode();
        hash = 31 * hash + padding.hashCode();
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        LayoutStyle rhs = (LayoutStyle) obj;
        return margin.equals( rhs.margin ) &&
               padding.equals( rhs.padding );
    }

    @Override
    public String toString() {
        return "LayoutStyle[" +
                    "margin=" + margin + ", " +
                    "padding=" + padding +
                "]";
    }

}
