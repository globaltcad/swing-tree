package swingtree.style;

public enum ShadingStrategy
{
    NONE,
    TOP_LEFT_TO_BOTTOM_RIGHT, BOTTOM_LEFT_TO_TOP_RIGHT,
    TOP_RIGHT_TO_BOTTOM_LEFT, BOTTOM_RIGHT_TO_TOP_LEFT,

    TOP_TO_BOTTOM, LEFT_TO_RIGHT,
    BOTTOM_TO_TOP, RIGHT_TO_LEFT;

    public boolean isNone() { return this == NONE; }

    public boolean isDiagonal() {
        return this == TOP_LEFT_TO_BOTTOM_RIGHT || this == BOTTOM_LEFT_TO_TOP_RIGHT ||
               this == TOP_RIGHT_TO_BOTTOM_LEFT || this == BOTTOM_RIGHT_TO_TOP_LEFT;
    }
}
