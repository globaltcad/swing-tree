package examples.trains.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import sprouts.HasId;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 *  A single calling point along a train's full route. The first stop has no
 *  arrival and the last no departure. {@link #current} marks the station the
 *  user is currently looking at, so the route view can highlight it.
 *  The {@link #id} is a synthetic stable identity for tuple rendering.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class RouteStop implements HasId<String> {

    private static final ZoneId VIENNA = ZoneId.of("Europe/Vienna");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final String  id;
    private final String  name;
    private final @Nullable Instant arrival;
    private final @Nullable Instant departure;
    private final @Nullable String  track;
    private final boolean current;

    public String arrivalClock()   { return clock(arrival); }
    public String departureClock() { return clock(departure); }

    private static String clock(@Nullable Instant t) {
        return t == null ? "" : HM.format(t.atZone(VIENNA));
    }
}