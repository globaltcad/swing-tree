package examples.trains.mvi;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static examples.trains.mvi.Json.*;

/**
 *  The data layer. A thin, Swing-free client over the public
 *  <a href="https://transitous.org">Transitous</a> MOTIS REST API, which serves
 *  free, key-less timetable data for Austria (and much of Europe).
 *  <p>
 *  Three calls cover the whole app:
 *  <ul>
 *    <li>{@link #searchStations}  &mdash; {@code /geocode}, resolve a typed name to stations,</li>
 *    <li>{@link #board}           &mdash; {@code /stoptimes}, the departures/arrivals board,</li>
 *    <li>{@link #trip}            &mdash; {@code /trip}, the full ordered route of one train.</li>
 *  </ul>
 *  Every method performs blocking IO and must therefore be called off the EDT
 *  (the view dispatches them on a background executor).
 */
public final class TransitClient {

    private static final String BASE = "https://api.transitous.org/api/v1";

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS    = 20_000;

    private TransitClient() {}

    /** Resolve a free-text query into matching railway stations (best match first). */
    public static List<Station> searchStations(String query) throws Exception {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>();
        String body = get(BASE + "/geocode?text=" + enc(query) + "&language=de");
        List<Station> out = new ArrayList<>();
        for (Object o : arr(parse(body))) {
            Map<String,Object> m = obj(o);
            if ("STOP".equals(m.get("type"))) {
                String id = str(m, "id");
                if (!id.isEmpty()) out.add(new Station(id, str(m, "name")));
            }
            if (out.size() >= 12) break;
        }
        return out;
    }

    /** Load the next departures (or arrivals) calling at the given station, trains only. */
    public static List<Departure> board(String stopId, boolean arrivals) throws Exception {
        String url = BASE + "/stoptimes?stopId=" + enc(stopId)
                   + "&n=40&arriveBy=" + arrivals + "&language=de";
        Map<String,Object> root = obj(parse(get(url)));
        List<Departure> out = new ArrayList<>();
        for (Object o : arr(root.get("stopTimes"))) {
            Map<String,Object> m = obj(o);
            String mode = str(m, "mode");
            if ("BUS".equals(mode)) continue;            // the user wants trains

            Map<String,Object> place = obj(m.get("place"));
            Instant time = parseTime(arrivals ? place.get("arrival")          : place.get("departure"));
            Instant sched = parseTime(arrivals ? place.get("scheduledArrival") : place.get("scheduledDeparture"));
            if (time == null) time = sched;
            if (time == null) continue;
            if (sched == null) sched = time;

            String track = optStr(place, "track");
            if (track == null) track = optStr(place, "scheduledTrack");

            String line = firstNonBlank(str(m, "displayName"), str(m, "routeShortName"), mode);
            String headsign = arrivals
                    ? str(obj(m.get("tripFrom")), "name")    // where it came from
                    : str(m, "headsign");                    // where it is going

            boolean cancelled = bool(m, "cancelled") || bool(m, "tripCancelled");

            out.add(new Departure(str(m, "tripId"), line, headsign, mode, time, sched, track, cancelled));
        }
        return out;
    }

    /** Resolve a trip id into its full route; {@code currentStationName} is highlighted. */
    public static TrainRoute trip(String tripId, String currentStationName) throws Exception {
        Map<String,Object> root = obj(parse(get(BASE + "/trip?tripId=" + enc(tripId) + "&language=de")));
        List<Object> legs = arr(root.get("legs"));
        if (legs.isEmpty()) return TrainRoute.empty();
        Map<String,Object> leg = obj(legs.get(0));

        List<RouteStop> stops = new ArrayList<>();
        int seq = 0;
        Map<String,Object> from = obj(leg.get("from"));
        stops.add(stop(tripId, seq++, from, currentStationName, true));   // origin: departure only
        for (Object o : arr(leg.get("intermediateStops")))
            stops.add(stop(tripId, seq++, obj(o), currentStationName, false));
        Map<String,Object> to = obj(leg.get("to"));
        stops.add(stop(tripId, seq++, to, currentStationName, false));    // terminus: arrival only

        String line = firstNonBlank(str(leg, "displayName"), str(leg, "routeShortName"), str(leg, "mode"));
        String headsign = firstNonBlank(str(leg, "headsign"), str(to, "name"));
        return new TrainRoute(line, headsign, Tuple.of(RouteStop.class, stops));
    }

    // ---- mapping helpers -----------------------------------------------------

    private static RouteStop stop(String tripId, int seq, Map<String,Object> m, String currentName, boolean isOrigin) {
        String name = str(m, "name");
        Instant arr = isOrigin ? null : parseTime(m.get("arrival"));
        Instant dep = parseTime(m.get("departure"));
        String track = optStr(m, "track");
        if (track == null) track = optStr(m, "scheduledTrack");
        boolean current = !currentName.isEmpty() && name.equalsIgnoreCase(currentName);
        // The id is scoped to the trip (not just seq:stopId): SwingTree's
        // scrollPanels reuses entry views by HasId.id() across re-renders, so a
        // route-stop id that collided across different trains (e.g. a shared
        // origin at seq 0) would make it reuse one train's row to show another's
        // — stale data, plus the "already tied to another parent" warning.
        return new RouteStop(tripId + "#" + seq + ":" + str(m, "stopId"), name, arr, dep, track, current);
    }

    private static @Nullable Instant parseTime(@Nullable Object v) {
        if (v == null) return null;
        String s = v.toString();
        if (s.isEmpty()) return null;
        try { return Instant.parse(s); }
        catch (Exception e) { return null; }
    }

    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) if (c != null && !c.isEmpty()) return c;
        return "";
    }

    // ---- transport -----------------------------------------------------------

    private static String get(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(CONNECT_TIMEOUT_MS);
        con.setReadTimeout(READ_TIMEOUT_MS);
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "SwingTree-TrainBoard-Example");
        try {
            int status = con.getResponseCode();
            if (status / 100 != 2)
                throw new IllegalStateException("HTTP " + status + " from transit service");
            try (InputStream in = con.getInputStream()) {
                return readAll(in);
            }
        } finally {
            con.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1)
            buffer.write(chunk, 0, read);
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8"); // UTF-8 is always supported, so this never fails.
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}