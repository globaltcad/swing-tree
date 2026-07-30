package examples.chat.mvi;

/**
 *  Whether a {@link Member} is around right now. Purely a value — the colour it
 *  is painted in comes from the {@link Theme}, so the same presence reads
 *  correctly in both the light and the dark skin.
 */
public enum Presence {
    ONLINE( "online", "●" ),
    AWAY(   "away",   "◗" ),
    OFFLINE("offline","○" );

    private final String label;
    private final String dot;

    Presence( String label, String dot ) {
        this.label = label;
        this.dot   = dot;
    }

    public String label() { return label; }

    /** A tiny glyph used in the roster, so presence survives a colour-blind reading too. */
    public String dot() { return dot; }

    public boolean isReachable() { return this != OFFLINE; }
}
