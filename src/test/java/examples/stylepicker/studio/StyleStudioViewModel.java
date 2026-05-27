package examples.stylepicker.studio;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.Tuple;

/**
 *  The single immutable root of the whole studio (MVI / MVL). The
 *  {@link StyleStudioView} holds none of this state itself — it zooms lenses in
 *  and renders a pure function of this record. Every user action returns a
 *  <em>new</em> {@code StyleStudioViewModel} via the Lombok withers and the
 *  business methods below.
 *
 *  <p>Two independent flavours of time-travel live here, exactly as requested:
 *  <ul>
 *    <li><b>Per-edit undo/redo</b> on the <i>draft</i> — the {@link History} of
 *        {@link StyleConfig}s. Each field tweak pushes a new present.</li>
 *    <li><b>Applied checkpoints</b> — every <b>Apply</b> appends a
 *        {@link Checkpoint}, building a timeline of committed looks the user can
 *        jump back to.</li>
 *  </ul>
 *  The currently-{@code applied} config is what the live {@link LookSheet}
 *  paints; the draft is what the editor shows. They diverge until Apply, which
 *  is the whole point of <i>not</i> styling eagerly.</p>
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class StyleStudioViewModel {

    private final History<StyleConfig> history;        // the editable draft (per-edit undo/redo)
    private final StyleConfig          applied;        // what the live sheet renders
    private final Tuple<Checkpoint>    checkpoints;    // applied-history timeline
    private final StyleTarget          selectedTarget; // which group OR type the editor edits

    /** Launch with the handsome starter theme already applied. */
    public static StyleStudioViewModel initial() {
        StyleConfig starter = StyleConfig.starter();
        return new StyleStudioViewModel(
            History.of(StyleConfig.class, starter),
            starter,
            Tuple.of(Checkpoint.class),
            StyleTarget.of(Look.CARD)
        );
    }

    // ── Derived read helpers (used by view lenses) ───────────────────────────

    /** The live draft the editor is mutating. */
    public StyleConfig draft() { return history.present(); }

    /** The style of the target (group or type) currently selected for editing. */
    public GroupStyle currentGroupStyle() { return history.present().styleFor(selectedTarget); }

    public boolean canUndo()  { return history.canUndo(); }
    public boolean canRedo()  { return history.canRedo(); }

    // ── Business transitions (pure; return new instances) ────────────────────

    /** Write-back wither for the "current target's style" lens — records one undoable edit. */
    public StyleStudioViewModel withEditedCurrentGroup(GroupStyle edited) {
        StyleConfig next = history.present().withStyleFor(selectedTarget, edited);
        return withHistory(history.push(next));
    }

    public StyleStudioViewModel undo() { return withHistory(history.undo()); }
    public StyleStudioViewModel redo() { return withHistory(history.redo()); }

    /** Commit the draft: it becomes the applied look and a checkpoint is recorded. */
    public StyleStudioViewModel apply(String label) {
        StyleConfig committed = history.present();
        Checkpoint cp = Checkpoint.of(checkpoints.size() + 1, label, committed);
        return withApplied(committed).withCheckpoints(checkpoints.add(cp));
    }

    /** Load a previously-applied checkpoint back into the draft (then the user can re-Apply). */
    public StyleStudioViewModel restore(Checkpoint cp) {
        return withHistory(history.push(cp.config()));
    }

    /** Reset the draft to a blank (overrides-nothing) theme as a fresh edit. */
    public StyleStudioViewModel clearDraft() {
        return withHistory(history.push(StyleConfig.blank()));
    }
}