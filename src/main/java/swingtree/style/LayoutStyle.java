package swingtree.style;

final class LayoutStyle
{
    private static final LayoutStyle _NONE = new LayoutStyle();

    public static LayoutStyle none() { return _NONE; }


    LayoutStyle() {
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals( Object obj ) {
        return obj instanceof LayoutStyle;
    }

    @Override
    public String toString() {
        return "LayoutStyle()";
    }

}
