package examples.laf.app;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.HasId;

import java.util.UUID;

/**
 *  One of the workshop's looms, standing on the loom floor with a commission
 *  mounted on it.
 *  <p>
 *  Each loom is rendered by a sub-view that binds a <em>per-item lens</em>
 *  ({@code addAll(Var<Tuple<Loom>>, entry -> ..)}), so its run switch writes
 *  straight back into the one immutable root. That overload hands out a
 *  {@code Var<Loom>} per row and therefore needs a stable identity to know which
 *  row belongs to which loom — which is what {@link HasId} and the {@link #id}
 *  below are for. Two looms threaded the same way are not the same loom, so
 *  content equality would be the wrong answer here.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode
public final class Loom implements HasId<UUID>
{
    private final UUID    id;
    private final String  name;       // "Nordre" — the looms have names, not numbers
    private final String  orderRef;   // the commission mounted on it, "" when idle
    private final int     progress;   // 0..100 through the current run
    private final boolean running;

    /**
     *  Puts a fresh, idle loom on the floor.
     *
     *  @param name     the loom's name
     *  @param orderRef the commission it is threaded for, or {@code ""} for none
     *  @return a stopped loom at the start of its run
     */
    public static Loom named( String name, String orderRef ) {
        return new Loom(UUID.randomUUID(), name, orderRef, 0, false);
    }

    /** @return {@code true} when no commission is mounted on this loom. */
    public boolean isIdle() { return orderRef.isEmpty(); }

    /** @return the run as a 0..1 fraction, which is what a progress bar wants. */
    public double fraction() { return progress / 100.0; }

    /** @return what the loom floor prints under the loom's name. */
    public String caption() {
        if ( isIdle() )
            return "unthreaded — waiting for a warp";
        return ( running ? "weaving #" : "holding at #" ) + orderRef;
    }
}
