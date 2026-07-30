package examples.chat.mvi;

/**
 *  The shape of the message composer — one row, or a stack.
 *  <p>
 *  This is deliberately <b>not</b> derived from the window: the composer lives
 *  inside the conversation card, whose width depends on the 12-column span the
 *  page grid handed it. A window can be very wide while that card is narrow (or
 *  the other way round), so the composer measures <em>itself</em> with its own
 *  {@code onResize} handler. Measure the region that actually cares.
 *  <p>
 *  What it drives is a {@code Val<Layout>} (skill §2e, "gear 2"): the very same
 *  widgets are re-laid-out rather than rebuilt, so the caret, the selection and
 *  the half-typed sentence in the text area all survive the reflow. For a
 *  composer that is not a nicety — rebuilding mid-sentence would be a bug.
 */
public enum ComposerShape {
    /** Emoji rail, text area and Send button all on one line. */
    ROW,
    /** Text area on its own line, the buttons below it. */
    STACK;

    public boolean isRow() { return this == ROW; }

    /** Below this width, in developer pixels, one row stops being comfortable. */
    private static final int THRESHOLD = 430;

    /**
     *  Classifies the composer's own width with a 10% dead band around the
     *  threshold, anchored on the current shape.
     *
     * @param width   The composer's current width, in developer pixels.
     * @param current The shape the composer is in right now.
     * @return The shape the composer should render in.
     */
    public static ComposerShape of( int width, ComposerShape current ) {
        double slack = 1.1;
        if ( current == STACK )
            return width > THRESHOLD * slack ? ROW : STACK;
        else
            return width < THRESHOLD / slack ? STACK : ROW;
    }
}
