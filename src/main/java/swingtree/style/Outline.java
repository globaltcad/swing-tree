package swingtree.style;

import java.awt.*;

public final class Outline
{
    private final int top;
    private final int right;
    private final int bottom;
    private final int left;

    public Outline( int top, int left, int right, int bottom ) {
        this.top = top;
        this.left = left;
        this.right = right;
        this.bottom = bottom;
    }

    public int top() { return top; }

    public int right() { return right; }

    public int bottom() { return bottom; }

    public int left() { return left; }

    Outline withTop(int top) { return new Outline(top, left, right, bottom); }

    Outline withLeft(int left) { return new Outline(top, left, right, bottom); }

    Outline withRight(int right) { return new Outline(top, left, right, bottom); }

    Outline withBottom(int bottom) { return new Outline(top, left, right, bottom); }


    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + top;
        hash = 31 * hash + right;
        hash = 31 * hash + bottom;
        hash = 31 * hash + left;
        return hash;
    }

    public Insets toInsets() {
        return new Insets(top, left, bottom, right);
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        Outline rhs = (Outline) obj;
        return top    == rhs.top    &&
               right  == rhs.right  &&
               bottom == rhs.bottom &&
               left   == rhs.left;
    }

    @Override
    public String toString() {
        return "Outline[" +
                    "top="    + top    + ", " +
                    "right="  + right  + ", " +
                    "bottom=" + bottom + ", " +
                    "left="   + left   +
                "]";
    }

}
