package examples.laf.app;

/**
 *  What happens to the cut edge of the cloth once it comes off the loom. Three
 *  mutually exclusive choices, which is why the editor offers them as radio
 *  buttons rather than as a drop-down.
 */
public enum Finish
{
    RAW(     "Raw edge"),
    HEMMED(  "Hand-hemmed"),
    FRINGED( "Knotted fringe");

    private final String label;

    Finish( String label ) { this.label = label; }

    public String label() { return label; }
}
