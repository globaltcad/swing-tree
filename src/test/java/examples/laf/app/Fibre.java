package examples.laf.app;

import java.awt.Color;

/**
 *  The raw materials the atelier weaves with, each one belonging to either the
 *  plant or the animal side of the store room. The {@link Origin} grouping is
 *  what the materials tree in {@link AtelierView} is built from, the
 *  {@link #pricePerMetre()} is what an order is costed with, and
 *  {@link #shade()} is the yarn colour that {@link AtelierArt} weaves into the
 *  cloth swatch.
 */
public enum Fibre
{
    LINEN(   "Linen",    Origin.PLANT,  12.50, 0xC9BFA3),
    HEMP(    "Hemp",     Origin.PLANT,   7.80, 0xB7B48B),
    COTTON(  "Cotton",   Origin.PLANT,   9.20, 0xE0D8C4),
    RAMIE(   "Ramie",    Origin.PLANT,  11.40, 0xCBC7A8),
    WOOL(    "Wool",     Origin.ANIMAL, 18.40, 0xB9A38C),
    SILK(    "Silk",     Origin.ANIMAL, 32.00, 0xD9C7B0),
    CASHMERE("Cashmere", Origin.ANIMAL, 46.00, 0xC4AE9A),
    MOHAIR(  "Mohair",   Origin.ANIMAL, 27.50, 0xCBB79C);

    /**
     *  The two halves of the store room. The materials tree shows one branch
     *  per constant, and selecting a branch filters the order book down to the
     *  fibres below it.
     */
    public enum Origin
    {
        PLANT("Plant"), ANIMAL("Animal");

        private final String label;

        Origin( String label ) { this.label = label; }

        public String label() { return label; }
    }

    private final String label;
    private final Origin origin;
    private final double pricePerMetre;
    private final Color  shade;

    Fibre( String label, Origin origin, double pricePerMetre, int shadeRgb ) {
        this.label         = label;
        this.origin        = origin;
        this.pricePerMetre = pricePerMetre;
        this.shade         = new Color(shadeRgb);
    }

    public String label()         { return label;  }
    public Origin origin()        { return origin; }
    public double pricePerMetre() { return pricePerMetre; }
    public Color  shade()         { return shade;  }

    /**
     *  Resolves a fibre from the text a table cell or a tree node carries,
     *  falling back to {@code fallback} when nothing matches. Table edits and
     *  tree selections both arrive as plain strings, and neither is allowed to
     *  invent a fibre that does not exist.
     *
     * @param label    the text to resolve, matched case-insensitively
     * @param fallback the fibre to keep when {@code label} names none
     * @return the matching fibre, or {@code fallback}
     */
    public static Fibre byLabel( String label, Fibre fallback ) {
        for ( Fibre fibre : values() )
            if ( fibre.label.equalsIgnoreCase(label.trim()) )
                return fibre;
        return fallback;
    }
}
