package examples.trains.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.Tuple;

/**
 *  The single immutable root of the whole application state (MVI / MVL).
 *  <p>
 *  The {@link TrainsView} never holds Swing state of its own: it zooms lenses
 *  into the fields of this record and renders a pure function of it. Every user
 *  action and every network result produces a <em>new</em> {@code TrainsViewModel}
 *  via the Lombok-generated withers, and the lenses fire only for the slices
 *  that actually changed. The class imports zero Swing types and is trivially
 *  unit-testable.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class TrainsViewModel {

    private final Station          station;          // station whose board we show
    private final String           query;            // text in the search field
    private final Tuple<Station>   stationResults;   // matches for the current query
    private final BoardMode        mode;             // departures vs arrivals
    private final Tuple<Departure> board;            // the loaded board rows
    private final TrainRoute       route;            // full route of the selected train
    private final String           selectedTripId;   // which board row is selected
    private final boolean          loadingBoard;
    private final boolean          loadingRoute;
    private final String           status;           // status / error line
    private final Theme            theme;
    private final Formfactor       formfactor;       // wide (desktop) vs tall (phone-like) window

    /** The initial state: Aspang-Markt pre-filled, nothing loaded yet. */
    public static TrainsViewModel initial() {
        return new TrainsViewModel(
            Station.none(),
            "Aspang Markt",
            Tuple.of(Station.class),
            BoardMode.DEPARTURES,
            Tuple.of(Departure.class),
            TrainRoute.empty(),
            "",
            false,
            false,
            "Loading Aspang-Markt…",
            Theme.LIGHT,
            Formfactor.WIDE
        );
    }
}