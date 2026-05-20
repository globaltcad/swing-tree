package examples.breathing.mvi;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.HasId;

import java.util.UUID;

/**
 *  An immutable record of one breathing cycle the user has finished. <br>
 *  A {@link sprouts.Tuple} of these lives on the {@link BreathingViewModel} and
 *  is rendered as a scrollable list of little cards. Because the
 *  {@code BreathingViewModel} is value-based, every {@code CompletedCycle}
 *  carries an explicit {@link UUID} {@link #id()} (via the {@link HasId}
 *  marker interface) so that SwingTree's {@code addAll(..)} list binding can
 *  tell the per-item sub-views apart and only rebuild what actually changed.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode @ToString
public final class CompletedCycle implements HasId<UUID> {

    private final UUID   id;
    private final int    index;
    private final double durationSeconds;
}