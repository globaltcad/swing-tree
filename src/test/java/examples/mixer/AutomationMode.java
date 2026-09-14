package examples.mixer;

/**
 *  Who moves the master fader. In {@link #MANUAL} mode the engineer does, and the fader writes
 *  into the view model. In {@link #READ} mode a recorded fade does: the fader follows a curve
 *  over the song and is bound read-only, through {@code withValue(Val)}, so grabbing it while
 *  the song plays holds the knob, but letting go writes nothing and the curve takes over again.
 */
public enum AutomationMode
{
    MANUAL("Manual"),
    READ  ("Read automation");

    private final String title;

    AutomationMode( String title ) { this.title = title; }

    @Override
    public String toString() { return title; }
}
