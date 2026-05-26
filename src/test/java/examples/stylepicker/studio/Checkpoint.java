package examples.stylepicker.studio;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.HasId;

import java.util.UUID;

/**
 *  A named, applied snapshot of a {@link StyleConfig} — one entry in the
 *  studio's "applied history". Every press of <b>Apply</b> appends a checkpoint,
 *  giving the user a timeline of committed looks they can jump back to
 *  (distinct from the fine-grained per-edit undo on the draft).
 *
 *  <p>Implements {@link HasId} because checkpoints are rendered through
 *  {@code scrollPanels().addAll(..)}; a stable {@link UUID} tells SwingTree which
 *  row-view belongs to which checkpoint (content-derived identity would collide
 *  for two visually-identical snapshots).</p>
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class Checkpoint implements HasId<UUID> {

    private final UUID        id;
    private final int         number;   // 1-based, human-friendly
    private final String      label;    // e.g. a timestamp
    private final StyleConfig config;

    public static Checkpoint of(int number, String label, StyleConfig config) {
        return new Checkpoint(UUID.randomUUID(), number, label, config);
    }

    @Override public UUID id() { return id; }
}