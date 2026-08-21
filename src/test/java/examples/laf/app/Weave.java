package examples.laf.app;

/**
 *  How the warp and the weft cross each other. Beside naming the cloth, the
 *  {@link #floats()} count is the number of warp threads a single weft thread
 *  passes over before it dips under again — which is exactly what
 *  {@link AtelierArt#swatchSvg} needs to draw the interlacing, so the swatch in
 *  the Cloth tab really is a picture of the selected weave rather than a
 *  decoration.
 */
public enum Weave
{
    PLAIN(       "Plain",        1),
    TWILL(       "Twill",        2),
    SATIN(       "Satin",        4),
    BOUCLE(      "Bouclé",       2),
    CANVAS(      "Canvas",       1),
    HERRINGBONE( "Herringbone",  3);

    private final String label;
    private final int    floats;

    Weave( String label, int floats ) {
        this.label  = label;
        this.floats = floats;
    }

    public String label()  { return label;  }
    public int    floats() { return floats; }

    /**
     *  Resolves a weave from the text of an edited table cell, keeping
     *  {@code fallback} when the text names no weave this atelier can set up.
     *
     * @param label    the text to resolve, matched case-insensitively
     * @param fallback the weave to keep when {@code label} names none
     * @return the matching weave, or {@code fallback}
     */
    public static Weave byLabel( String label, Weave fallback ) {
        for ( Weave weave : values() )
            if ( weave.label.equalsIgnoreCase(label.trim()) )
                return weave;
        return fallback;
    }
}
