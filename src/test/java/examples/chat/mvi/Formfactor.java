package examples.chat.mvi;

/**
 *  How much horizontal room the page chrome has to play with.
 *  <p>
 *  Unlike the aspect-ratio form factor of the trains example, a chat page
 *  converges around <b>width</b>: the header either has room for a tag line and
 *  a worded theme button, or it does not. So this enum classifies a width, and
 *  it is written by exactly one {@code onResize} handler on the root view.
 *  <p>
 *  It lives in the {@link ChatViewModel} like any other state, which means
 *  "which shape are we in" stays ordinary, Swing-free, unit-testable data —
 *  and the widgets that care simply bind to a view of it
 *  ({@code isVisibleIf(..)}, a bound label text), so nothing is ever rebuilt.
 */
public enum Formfactor {
    /** Narrow: only what is unique survives. */
    COMPACT,
    /** Wide: there is room for tag lines, worded buttons and topics. */
    ROOMY;

    public boolean isRoomy()   { return this == ROOMY; }
    public boolean isCompact() { return this == COMPACT; }

    /** Where the page stops being roomy, in developer pixels. */
    private static final int THRESHOLD = 880;

    /**
     *  Classifies a width, anchored on the shape we are already in so that
     *  dragging the window edge across the threshold cannot strobe: we only
     *  leave a shape once we are 10% clear of it.
     *
     * @param width   The current width of the view, in developer pixels.
     * @param current The shape the view is in right now.
     * @return The shape the view should render in.
     */
    public static Formfactor of( int width, Formfactor current ) {
        double slack = 1.1;
        if ( current == COMPACT )
            return width > THRESHOLD * slack ? ROOMY : COMPACT;
        else
            return width < THRESHOLD / slack ? COMPACT : ROOMY;
    }
}
