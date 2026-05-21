package examples.simple;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.HasId;
import sprouts.Tuple;

import java.util.UUID;

/**
 *  The immutable view model behind {@link TodoApp}.
 *
 *  <p>Everything the UI displays — the task list, the current draft, the
 *  active filter, the derived counters — is a function of this single
 *  record. The view never mutates a {@code Task}; it asks the model for
 *  the next state via one of the helper methods below ({@code addTaskFromDraft},
 *  {@code toggleTask}, …), which return a new {@code TodoModel}.</p>
 *
 *  <p>Each {@link Task} implements {@link HasId HasId&lt;UUID&gt;} so SwingTree's
 *  tuple-bound {@code addAll(..)} can tell one card from another even when
 *  their text matches — and so a card's identity survives a text edit.</p>
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class TodoModel {

    public enum Filter { ALL, ACTIVE, DONE }

    private final Tuple<Task> tasks;
    private final String      draft;
    private final Filter      filter;

    public TodoModel() { this(seed(), "", Filter.ALL); }

    // ── Derived stats — view binds to these via viewAs / viewAsString ───────

    public int totalCount()  { return tasks.size(); }
    public int doneCount()   { return (int) tasks.stream().filter(Task::done).count(); }
    public int activeCount() { return totalCount() - doneCount(); }

    public boolean matches(Task t) {
        switch (filter) {
            case ACTIVE: return !t.done();
            case DONE:   return  t.done();
            case ALL:
            default:     return true;
        }
    }

    // ── Withers / commands ──────────────────────────────────────────────────

    public TodoModel addTaskFromDraft() {
        String text = draft.trim();
        if (text.isEmpty()) return this;
        int nextSeq = tasks.stream().mapToInt(Task::seq).max().orElse(0) + 1;
        Task t = new Task(UUID.randomUUID(), nextSeq, text, false);
        return new TodoModel(tasks.add(t), "", filter);
    }

    public TodoModel toggleTask(UUID id) {
        int i = indexOf(id);
        if (i < 0) return this;
        Task t = tasks.get(i);
        return withTasks(tasks.setAt(i, t.withDone(!t.done())));
    }

    public TodoModel renameTask(UUID id, String text) {
        int i = indexOf(id);
        if (i < 0) return this;
        return withTasks(tasks.setAt(i, tasks.get(i).withText(text)));
    }

    public TodoModel removeTask(UUID id) {
        return withTasks(tasks.removeIf(t -> t.id().equals(id)));
    }

    public TodoModel clearCompleted() {
        return withTasks(tasks.removeIf(Task::done));
    }

    private int indexOf(UUID id) {
        for (int i = 0; i < tasks.size(); i++)
            if (tasks.get(i).id().equals(id)) return i;
        return -1;
    }

    // ── Seed data — four plausible tasks so the test suite's task-1 / task-2
    //    / task-3 panel ids resolve out of the box. ───────────────────────────

    private static Tuple<Task> seed() {
        return Tuple.of(Task.class,
            new Task(UUID.randomUUID(), 1, "Get up in the morning",       true),
            new Task(UUID.randomUUID(), 2, "Make coffee",                  true),
            new Task(UUID.randomUUID(), 3, "Drink coffee",                 false),
            new Task(UUID.randomUUID(), 4, "Code something in SwingTree",  false)
        );
    }

    // ── Task — a single line item ───────────────────────────────────────────

    @With @Getter @Accessors(fluent = true) @AllArgsConstructor
    public static final class Task implements HasId<UUID> {
        private final UUID    id;
        /** 1-based creation index; used to derive the stable {@code "task-N"} panel id. */
        private final int     seq;
        private final String  text;
        private final boolean done;
    }
}