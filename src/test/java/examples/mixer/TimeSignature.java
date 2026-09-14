package examples.mixer;

import java.util.function.Function;

/**
 *  The metre of a song: how many beats a bar holds and how long one beat is.
 *  <p>
 *  Positions on the timeline are counted in <b>ticks</b>, the unit every sequencer
 *  and digital audio workstation uses underneath its bars and beats: a quarter note
 *  is {@link #TICKS_PER_QUARTER} ticks long, whatever the tempo. That is what lets the
 *  timeline slider place a tick mark at every bar and snap to bars with whole
 *  numbers, because a bar is always a whole number of ticks long: 3840 in 4/4, 2880 in
 *  3/4 and in 6/8, 3360 in 7/8. The tempo only decides how many seconds a tick lasts.
 */
public enum TimeSignature
{
    FOUR_FOUR  ("4/4", 4, 960),
    THREE_FOUR ("3/4", 3, 960),
    SIX_EIGHT  ("6/8", 6, 480),
    SEVEN_EIGHT("7/8", 7, 480);

    /** The resolution of the timeline: the number of ticks in one quarter note. */
    public static final long TICKS_PER_QUARTER = 960;

    private final String label;
    private final int    beatsPerBar;
    private final long   ticksPerBeat;

    /**
     *  The label text of a major tick mark on the timeline: the number of the bar which starts at
     *  that tick. It is created once per time signature and kept, rather than written as a lambda
     *  where the {@code SliderTicks} are built, because a {@code SliderTicks} compares its label
     *  function by identity. A fresh lambda on every change of the view model would make every new
     *  {@code SliderTicks} unequal to the previous one, and the slider would build its labels anew
     *  50 times a second while the song plays.
     */
    private final Function<Long, String> barNumberAtTick;

    TimeSignature( String label, int beatsPerBar, long ticksPerBeat ) {
        this.label           = label;
        this.beatsPerBar     = beatsPerBar;
        this.ticksPerBeat    = ticksPerBeat;
        this.barNumberAtTick = this::barNumberAt;
    }

    public int beatsPerBar() { return beatsPerBar; }

    public long ticksPerBeat() { return ticksPerBeat; }

    public long ticksPerBar() { return beatsPerBar * ticksPerBeat; }

    public Function<Long, String> barNumberAtTick() { return barNumberAtTick; }

    private String barNumberAt( Long tick ) {
        return String.valueOf(tick / ticksPerBar() + 1);
    }

    @Override
    public String toString() { return label; }
}
