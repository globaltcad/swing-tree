package examples.mixer;

/**
 *  The songs of tonight's session, each with the tempo, metre and length it was recorded in.
 *  Choosing another song changes the length of the timeline, which is the maximum of the
 *  timeline slider and of both loop sliders, so their tick marks and labels are laid out again.
 */
public enum Song
{
    HARBOR_LIGHTS("Harbor Lights",  92, TimeSignature.FOUR_FOUR,   48),
    PAPER_KITES  ("Paper Kites",   138, TimeSignature.THREE_FOUR,  64),
    NIGHT_SHIFT  ("Night Shift",   116, TimeSignature.SEVEN_EIGHT, 40),
    SALT_AND_TAR ("Salt and Tar",   72, TimeSignature.SIX_EIGHT,   32);

    private final String        title;
    private final int           bpm;
    private final TimeSignature signature;
    private final int           bars;

    Song( String title, int bpm, TimeSignature signature, int bars ) {
        this.title     = title;
        this.bpm       = bpm;
        this.signature = signature;
        this.bars      = bars;
    }

    public int bpm() { return bpm; }

    public TimeSignature signature() { return signature; }

    public int bars() { return bars; }

    @Override
    public String toString() { return title; }
}
