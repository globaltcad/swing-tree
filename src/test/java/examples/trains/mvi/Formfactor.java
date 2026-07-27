package examples.trains.mvi;

/**
 *  The shape of the window as far as the layout cares: wider than tall (a desktop
 *  window) or taller than wide (a phone-like column).
 *  <p>
 *  This is what makes the {@link TrainsView} <b>convergent</b>: the same view model
 *  renders either as a two-pane board or as a single scrolling column, depending on
 *  nothing but the aspect ratio. Because it lives in the {@link TrainsViewModel}
 *  rather than in a Swing field, "which shape are we in" stays ordinary, testable
 *  application state.
 */
public enum Formfactor {
    /** Landscape: there is room for two panes side by side. */
    WIDE,
    /** Portrait: one column, scrolled top to bottom, like a phone. */
    TALL;

    public boolean isTall() { return this == TALL; }

    public boolean isWide() { return this == WIDE; }

    /**
     *  Classifies a view size into one of the two shapes, with a 10% dead band
     *  around the square — without it, dragging a window corner along the diagonal
     *  would flip the layout back and forth many times per second.
     *
     * @param width   The current width of the view, in developer pixels.
     * @param height  The current height of the view, in developer pixels.
     * @param current The shape the view is in right now, which anchors the
     *                hysteresis (we only leave a shape once we are clearly out of it).
     * @return The shape the view should be rendered in.
     */
    public static Formfactor of( int width, int height, Formfactor current ) {
        double slack = 1.1;
        if ( current == TALL )
            return width > height * slack ? WIDE : TALL;
        else
            return height > width * slack ? TALL : WIDE;
    }
}
