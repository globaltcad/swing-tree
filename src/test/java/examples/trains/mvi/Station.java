package examples.trains.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.HasId;

/**
 *  An immutable train station (a MOTIS "STOP" place).
 *  The {@code id} is the opaque station id used by the API to query a board,
 *  and also serves as the stable {@link HasId} identity when stations are
 *  rendered in a bound combo box / tuple.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class Station implements HasId<String> {

    private final String id;
    private final String name;

    /** The "nothing selected yet" placeholder used as the initial view-model value. */
    public static Station none() { return new Station("", "—"); }

    public boolean isReal() { return !id.isEmpty(); }

    @Override public String toString() { return name; }
}