package swingtree.style;

public class PaddingStyle
{
    private final int top;
    private final int right;
    private final int bottom;
    private final int left;

    public PaddingStyle(int top, int left, int right, int bottom) {
        this.top = top;
        this.left = left;
        this.right = right;
        this.bottom = bottom;
    }

    public int top() { return top; }

    public int right() { return right; }

    public int bottom() { return bottom; }

    public int left() { return left; }

    PaddingStyle withTop(int top) { return new PaddingStyle(top, left, right, bottom); }

    PaddingStyle withLeft(int left) { return new PaddingStyle(top, left, right, bottom); }

    PaddingStyle withRight(int right) { return new PaddingStyle(top, left, right, bottom); }

    PaddingStyle withBottom(int bottom) { return new PaddingStyle(top, left, right, bottom); }

}
