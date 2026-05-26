package examples.stylepicker.studio;

import sprouts.Tuple;

/**
 *  A tiny, fully-immutable <b>time-travel</b> structure: a {@code present}
 *  value flanked by a stack of {@code past} values and a stack of {@code future}
 *  (undone) values. This is the classic redux-style undo/redo zipper.
 *
 *  <p>It exists precisely because the studio is modelled data-orientedly: since
 *  every edit already produces a fresh immutable {@link StyleConfig}, wrapping
 *  those values in a {@code History} buys unlimited undo/redo essentially for
 *  free — no command objects, no mementos, no diffing.</p>
 *
 *  <pre>{@code
 *      past = [v0, v1]   present = v2   future = []
 *      push(v3) -> past = [v0, v1, v2]  present = v3  future = []
 *      undo()   -> past = [v0, v1]      present = v2  future = [v3]
 *      redo()   -> past = [v0, v1, v2]  present = v3  future = []
 *  }</pre>
 *
 *  @param <T> the immutable value type travelling through time
 */
public final class History<T> {

    /** Keep the undo stack bounded so a long slider-drag session can't grow it without limit. */
    private static final int MAX_PAST = 256;

    private final Tuple<T> past;
    private final T        present;
    private final Tuple<T> future;

    private History(Tuple<T> past, T present, Tuple<T> future) {
        this.past = past;
        this.present = present;
        this.future = future;
    }

    public static <T> History<T> of(Class<T> type, T present) {
        return new History<>(Tuple.of(type), present, Tuple.of(type));
    }

    public T present() { return present; }

    public boolean canUndo() { return !past.isEmpty(); }
    public boolean canRedo() { return !future.isEmpty(); }
    public int undoCount()   { return past.size(); }
    public int redoCount()   { return future.size(); }

    /** Record a new present, discarding any redo branch. */
    public History<T> push(T next) {
        Tuple<T> newPast = past.add(present);
        if (newPast.size() > MAX_PAST)
            newPast = newPast.removeFirst(newPast.size() - MAX_PAST);
        return new History<>(newPast, next, clear(future));
    }

    public History<T> undo() {
        if (!canUndo()) return this;
        return new History<>(past.removeLast(), past.last(), future.add(present));
    }

    public History<T> redo() {
        if (!canRedo()) return this;
        return new History<>(past.add(present), future.last(), future.removeLast());
    }

    /** Empty a tuple while preserving its element type (no Class token needed). */
    private static <T> Tuple<T> clear(Tuple<T> t) {
        return t.isEmpty() ? t : t.removeFirst(t.size());
    }
}