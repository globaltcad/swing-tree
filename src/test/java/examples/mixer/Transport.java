package examples.mixer;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import lombok.experimental.Accessors;

import java.util.Locale;

/**
 *  The tape machine of the console: which song is loaded, where the playhead is, whether it
 *  runs, and the loop region it keeps returning to.
 *  <p>
 *  All positions are ticks (see {@link TimeSignature#TICKS_PER_QUARTER}). The loop start and
 *  end are bound to each other in the view, the start slider's maximum being the end and the
 *  end slider's minimum being the start, so the model keeps them in that order too, in case
 *  anything else writes them.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode @ToString
public final class Transport
{
    private final Song          song;
    private final TimeSignature signature;
    private final int           bars;
    private final int           bpm;
    private final long          position;
    private final long          loopStart;
    private final long          loopEnd;
    private final boolean       looping;
    private final boolean       playing;
    private final boolean       snapToBars;

    public static Transport of( Song song ) {
        TimeSignature signature = song.signature();
        long bar = signature.ticksPerBar();
        return new Transport(song, signature, song.bars(), song.bpm(), 0, 4 * bar, 8 * bar, false, false, true);
    }

    /** The tick at which the song ends, which is the maximum of the timeline. */
    public long end() {
        return bars * signature.ticksPerBar();
    }

    public Transport withSongLoaded( Song newSong ) {
        return Transport.of(newSong).withSnapToBars(snapToBars).withLooping(looping);
    }

    /**
     *  Changes the metre while keeping every position on the same bar and at the same fraction
     *  of that bar, which is what a musician expects when the tempo map is edited.
     */
    public Transport withSignatureChangedTo( TimeSignature newSignature ) {
        double scale = newSignature.ticksPerBar() / (double) signature.ticksPerBar();
        long newEnd = bars * newSignature.ticksPerBar();
        long newStart = Math.min(Math.round(loopStart * scale), newEnd);
        long newLoopEnd = Math.max(newStart, Math.min(Math.round(loopEnd * scale), newEnd));
        return new Transport(
                    song, newSignature, bars, bpm,
                    Math.min(Math.round(position * scale), newEnd),
                    newStart, newLoopEnd, looping, playing, snapToBars
                );
    }

    public Transport withPositionAt( long tick ) {
        return withPosition(Math.max(0, Math.min(end(), tick)));
    }

    public Transport withLoopStartAt( long tick ) {
        return withLoopStart(Math.max(0, Math.min(loopEnd, tick)));
    }

    public Transport withLoopEndAt( long tick ) {
        return withLoopEnd(Math.max(loopStart, Math.min(end(), tick)));
    }

    public Transport playedOrStopped() {
        if ( playing )
            return withPlaying(false);
        if ( position >= end() )
            return withPosition(0).withPlaying(true);
        return withPlaying(true);
    }

    /**
     *  Moves the playhead forward by the ticks that pass in the given number of seconds at the
     *  current tempo, wrapping around the loop region when looping, and stopping at the end.
     */
    public Transport advancedBy( double seconds ) {
        if ( !playing )
            return this;
        long ticks = Math.round(seconds * bpm / 60.0 * TimeSignature.TICKS_PER_QUARTER);
        long next = position + ticks;
        if ( looping && loopEnd > loopStart && position < loopEnd && next >= loopEnd )
            return withPosition(loopStart + (next - loopEnd) % (loopEnd - loopStart));
        if ( next >= end() )
            return withPosition(end()).withPlaying(false);
        return withPosition(next);
    }

    public double seconds() {
        return position / (double) TimeSignature.TICKS_PER_QUARTER * 60.0 / bpm;
    }

    /** Where the playhead is, the way the big display above the timeline shows it. */
    public String timecode() {
        long bar  = position / signature.ticksPerBar() + 1;
        long beat = position % signature.ticksPerBar() / signature.ticksPerBeat() + 1;
        double seconds = seconds();
        int minutes = (int) (seconds / 60);
        return String.format(Locale.ROOT, "Bar %3d · Beat %d   %02d:%04.1f", bar, beat, minutes, seconds - minutes * 60);
    }
}
