package examples.trains.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import sprouts.HasId;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 *  One row of a station board: a train calling at the selected station,
 *  either departing (showing where it goes) or arriving (showing where it
 *  came from). Its {@link #id} is the MOTIS trip id and doubles as the stable
 *  {@link HasId} identity used when the board is rendered from a tuple, and as
 *  the key passed to {@link TransitClient#trip} to load the full route.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class Departure implements HasId<String> {

    private static final ZoneId VIENNA = ZoneId.of("Europe/Vienna");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final String  id;          // MOTIS trip id
    private final String  line;        // e.g. "REX 2732", "RJ 558"
    private final String  headsign;    // destination (departures) / origin (arrivals)
    private final String  mode;        // e.g. "REGIONAL_RAIL", "LONG_DISTANCE"
    private final Instant time;        // the relevant departure/arrival time
    private final Instant scheduledTime;
    private final @Nullable String track;
    private final boolean cancelled;

    /** Wall-clock "HH:mm" of the (real-time) departure/arrival in Vienna time. */
    public String clock() { return HM.format(time.atZone(VIENNA)); }

    /** Positive minutes of delay versus the scheduled time, or 0 if punctual/early. */
    public long delayMinutes() {
        long m = Duration.between(scheduledTime, time).toMinutes();
        return Math.max(0, m);
    }

    public boolean isDelayed() { return delayMinutes() > 0; }
}