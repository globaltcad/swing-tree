package examples.laf.app;

/**
 *  The stations an order passes through on its way out of the workshop. The
 *  order is significant: {@link #next()} walks it forwards, which is what the
 *  "Advance" command and a loom finishing a run both do, and
 *  {@link #completion()} turns it into the fraction that feeds the workload bar
 *  in the status line.
 */
@SuppressWarnings("EnumOrdinal") // The declaration order of the stations *is* the order of the workshop.
public enum Stage
{
    DRAFTED(   "Drafted"),
    WARPING(   "Warping"),
    WEAVING(   "Weaving"),
    FINISHING( "Finishing"),
    SHIPPED(   "Shipped");

    private final String label;

    Stage( String label ) { this.label = label; }

    public String label() { return label; }

    /** @return the next station, or this one when the order has already shipped. */
    public Stage next() {
        return this == SHIPPED ? this : values()[ordinal() + 1];
    }

    /** @return {@code true} while the order is still work for the looms. */
    public boolean isOnTheFloor() { return this == WARPING || this == WEAVING; }

    /** @return how far through the workshop this stage is, from 0 to 1. */
    public double completion() {
        return ordinal() / (double) (values().length - 1);
    }

    /**
     *  Resolves a stage from the text of an edited table cell, keeping
     *  {@code fallback} when the text names no station.
     *
     * @param label    the text to resolve, matched case-insensitively
     * @param fallback the stage to keep when {@code label} names none
     * @return the matching stage, or {@code fallback}
     */
    public static Stage byLabel( String label, Stage fallback ) {
        for ( Stage stage : values() )
            if ( stage.label.equalsIgnoreCase(label.trim()) )
                return stage;
        return fallback;
    }
}
