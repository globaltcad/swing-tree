package swingtree.style;

public final class LayoutStyle
{
    private final Outline margin;
    private final Outline padding;

    public LayoutStyle( Outline margin, Outline padding ) {
        this.margin = margin;
        this.padding = padding;
    }

    public Outline margin() { return margin; }

    public Outline padding() { return padding; }

    public LayoutStyle withMargin( Outline margin ) { return new LayoutStyle( margin, padding ); }

    public LayoutStyle withPadding( Outline padding ) { return new LayoutStyle( margin, padding ); }

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
