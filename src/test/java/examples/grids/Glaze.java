package examples.grids;

/**
 *  The glazes of the tiles in {@link TileWorkshopView}. Every tile gets the glaze of
 *  its number, cycling through all twelve, so neighbouring tiles are easy to tell apart
 *  and a tile keeps its colour wherever the grid puts it.
 *  <p>
 *  The colours are plain RGB integers, which keeps the view model free of AWT types.
 */
public enum Glaze
{
    TERRACOTTA("Terracotta", 0xC8643F),
    SAGE      ("Sage",       0x8FA68A),
    COBALT    ("Cobalt",     0x3F5FA8),
    SAFFRON   ("Saffron",    0xE3A92F),
    PLUM      ("Plum",       0x7B4B7A),
    TEAL      ("Teal",       0x3C8C8A),
    CORAL     ("Coral",      0xE07A6A),
    OLIVE     ("Olive",      0x7D8546),
    SLATE     ("Slate",      0x5E6B78),
    ROSE      ("Rose",       0xD28CA0),
    OCHRE     ("Ochre",      0xB9853A),
    LAGOON    ("Lagoon",     0x4FA3C4);

    private final String title;
    private final int    rgb;

    Glaze( String title, int rgb ) {
        this.title = title;
        this.rgb   = rgb;
    }

    public String title() { return title; }

    public int rgb() { return rgb; }

    /**
     *  Tells whether dark text reads better on this glaze than white text,
     *  based on the perceived brightness of the colour.
     */
    public boolean isLight() {
        int red   = (rgb >> 16) & 0xFF;
        int green = (rgb >>  8) & 0xFF;
        int blue  =  rgb        & 0xFF;
        return ( red * 299 + green * 587 + blue * 114 ) / 1000 > 150;
    }

    /** The glaze of the tile with the given number, where the first tile has the number 1. */
    public static Glaze forTile( int number ) {
        Glaze[] glazes = values();
        return glazes[ Math.floorMod(number - 1, glazes.length) ];
    }
}
