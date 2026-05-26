package examples.trains.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.Tuple;

import java.time.Duration;

/**
 *  The full path a selected train takes: its line, its final destination and
 *  the ordered {@link RouteStop}s from origin to terminus. This is the payload
 *  behind the app's headline feature — clicking a board entry resolves into one
 *  of these so the whole journey can be inspected for manual route planning.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class TrainRoute {

    private final String line;        // e.g. "REX 2732"
    private final String headsign;    // final destination
    private final Tuple<RouteStop> stops;

    /** The empty route is the initial / "nothing selected" state. */
    public static TrainRoute empty() {
        return new TrainRoute("", "", Tuple.of(RouteStop.class));
    }

    public boolean isEmpty() { return stops.isEmpty(); }

    public String origin() {
        return stops.isEmpty() ? "" : stops.get(0).name();
    }

    public String terminus() {
        return stops.isEmpty() ? "" : stops.get(stops.size() - 1).name();
    }

    /** A one-line summary like "23 stops · 2h 30m". */
    public String summary() {
        if (stops.isEmpty()) return "";
        StringBuilder sb = new StringBuilder().append(stops.size()).append(" stops");
        RouteStop first = stops.get(0);
        RouteStop last  = stops.get(stops.size() - 1);
        if (first.departure() != null && last.arrival() != null) {
            Duration d = Duration.between(first.departure(), last.arrival());
            long h = d.toHours(), m = d.toMinutes() % 60;
            sb.append(" · ");
            if (h > 0) sb.append(h).append("h ");
            sb.append(m).append("m");
        }
        return sb.toString();
    }
}