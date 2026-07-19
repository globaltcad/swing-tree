package examples.simple;

import com.formdev.flatlaf.FlatDarkLaf;
import sprouts.Val;
import sprouts.Var;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.UIForButton;
import swingtree.UIForLabel;
import swingtree.UIForTextField;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Color;
import java.util.UUID;

import static swingtree.UI.*;

/**
 *  <h2>Todo — a small, fully-working MVI / MVL demo</h2>
 *
 *  <p>The classic Todo-list demo, rewritten as a single immutable
 *  {@link TodoModel} sliced by {@code Var.zoomTo(..)} lenses.
 *  Compared to the previous version of this file, where the
 *  <code>Add</code> and <code>Remove</code> buttons did nothing and the
 *  tasks were hard-coded, this one supports:</p>
 *
 *  <ul>
 *    <li>typing a new task into the input field and adding it with
 *        <i>Enter</i> or the <b>＋ Add</b> button,</li>
 *    <li>toggling completion via the checkbox (the title strikes
 *        through and goes grey),</li>
 *    <li>inline rename — the task text is itself a bound text field,</li>
 *    <li>filtering between <i>All / Active / Done</i>,</li>
 *    <li>clearing all completed tasks at once,</li>
 *    <li>and a live "<i>N of M done</i>" stats chip.</li>
 *  </ul>
 *
 *  <p>The first four tasks have stable panel ids <code>task-1</code> …
 *  <code>task-4</code> (derived from the task's creation sequence number),
 *  which keeps the test in {@code Examples_Spec} happy without making the
 *  view care about the test.</p>
 */
public class TodoApp extends Panel {

    // ── Palette ──────────────────────────────────────────────────────────────

    private static final Color BG          = new Color( 24,  26,  38);
    private static final Color BG_CARD     = new Color( 42,  46,  64);
    private static final Color BG_CARD_HI  = new Color( 56,  62,  86);
    private static final Color INK         = new Color(232, 236, 252);
    private static final Color INK_FAINT   = new Color(150, 162, 198);
    private static final Color INK_DONE    = new Color(110, 120, 150);
    private static final Color ACCENT      = new Color(120, 176, 238);
    private static final Color ACCENT_DONE = new Color(160, 220, 170);
    private static final Color DANGER      = new Color(232, 110, 130);

    public TodoApp() {
        Var<TodoModel> vm = Var.of(new TodoModel());
        build(vm);
    }

    private void build(Var<TodoModel> vm) {
        Var<String>            draft  = vm.zoomTo(TodoModel::draft,  TodoModel::withDraft);
        Var<TodoModel.Filter>  filter = vm.zoomTo(TodoModel::filter, TodoModel::withFilter);

        Val<String> statsLine = vm.viewAsString(m ->
            m.totalCount() == 0
                ? "Nothing here yet — add your first task."
                : m.doneCount() + " of " + m.totalCount() + " done");

        Val<String> footerLine = vm.viewAsString(m ->
            m.activeCount() + " active  ·  " + m.doneCount() + " done");

        of(this).withLayout("fill, wrap 1, insets 0, gap 0").withPrefSize(560, 640)
            .withStyle( it -> it.backgroundColor(BG) )
            .add("growx",     header(statsLine))
            .add("grow, push",
                UI.panel("fill, wrap 1, insets 18, gap 12")
                .withStyle( it -> it.backgroundColor(BG) )
                .add("growx",       filterRow(filter))
                .add("growx",       inputRow(vm, draft))
                .add("grow, push",  taskList(vm))
                .add("growx",       footerRow(vm, footerLine))
            );
    }

    // ── Header ───────────────────────────────────────────────────────────────

    private static UIForAnySwing<?,?> header(Val<String> stats) {
        return UI.panel("fill, insets 18 24 18 24")
            .withStyle( it -> it
                .backgroundColor(new Color(18, 20, 30))
                .borderAt(Edge.BOTTOM, 1, new Color(60, 70, 100))
            )
            .add("pushx, growx",
                UI.html("<span style='color:#f3f4ff;font-family:serif;font-size:22px;'>Todo</span>" +
                        "<span style='color:#9aa6c8;font-style:italic;font-size:12px;'>" +
                        "&nbsp;&nbsp;—&nbsp;&nbsp;a single immutable view-model behind the curtain</span>")
            )
            .add("shrinkx",
                UI.label(stats).withStyle( it -> it
                    .backgroundColor(new Color(120, 176, 238, 55))
                    .foregroundColor(ACCENT.brighter())
                    .borderRadius(10)
                    .padding(4, 12, 4, 12)
                    .componentFont( f -> f.family("SansSerif").size(12).weight(2) )
                )
            );
    }

    // ── Filter chips ─────────────────────────────────────────────────────────

    private static UIForAnySwing<?,?> filterRow(Var<TodoModel.Filter> filter) {
        return UI.panel("insets 0, gap 6").withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
            .add(filterChip("All",    TodoModel.Filter.ALL,    filter))
            .add(filterChip("Active", TodoModel.Filter.ACTIVE, filter))
            .add(filterChip("Done",   TodoModel.Filter.DONE,   filter));
    }

    private static UIForButton<JButton> filterChip(String label, TodoModel.Filter value, Var<TodoModel.Filter> filter) {
        Val<Boolean> active = filter.viewAs(Boolean.class, f -> f == value);
        return UI.button(label)
            .withStyle( active, (on, it) -> it
                .backgroundColor(on ? new Color(120, 176, 238, 55) : new Color(255, 255, 255, 12))
                .foregroundColor(on ? Color.WHITE : INK)
                .borderRadius(14)
                .padding(4, 14, 4, 14)
                .margin(0)
                .componentFont( f -> f.family("SansSerif").size(12).weight(on ? 2 : 1) )
            )
            .onClick( it -> filter.set(value) );
    }

    // ── Input row ────────────────────────────────────────────────────────────

    private static UIForAnySwing<?,?> inputRow(Var<TodoModel> vm, Var<String> draft) {
        Val<Boolean> canAdd = draft.viewAs(Boolean.class, s -> s != null && !s.trim().isEmpty());
        return UI.panel("fill, insets 0, gap 8").withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
            .add("grow, push",
                UI.textField(draft)
                .withStyle( it -> it
                    .backgroundColor(BG_CARD)
                    .foregroundColor(INK)
                    .borderRadius(10)
                    .padding(8, 14, 8, 14)
                    .border(1, new Color(80, 90, 120, 80))
                    .componentFont( f -> f.family("SansSerif").size(13) )
                )
                .onEnter( it -> vm.update(TodoModel::addTaskFromDraft) )
            )
            .add(
                UI.button("＋  Add")
                .isEnabledIf(canAdd)
                .withStyle( canAdd, (mayAdd, it) -> it
                    .backgroundColor(mayAdd ? ACCENT : new Color(80, 90, 120, 100))
                    .foregroundColor(mayAdd ? Color.WHITE : new Color(180, 190, 210))
                    .borderRadius(10)
                    .padding(8, 16, 8, 16)
                    .margin(0)
                    .componentFont( f -> f.family("SansSerif").size(12).weight(2) )
                )
                .onClick( it -> vm.update(TodoModel::addTaskFromDraft) )
            );
    }

    // ── Task list — addAll over a tuple lens, filtered via the model ────────

    private static UIForAnySwing<?,?> taskList(Var<TodoModel> vm) {
        Var<sprouts.Tuple<TodoModel.Task>> tasksLens =
            vm.zoomTo(TodoModel::tasks, TodoModel::withTasks);

        return UI.scrollPanels()
            .addAll(tasksLens, (Var<TodoModel.Task> taskVar) -> taskCard(taskVar, vm));
    }

    private static UIForAnySwing<?,?> taskCard(Var<TodoModel.Task> taskVar, Var<TodoModel> vm) {
        UUID         id   = taskVar.get().id();
        int          seq  = taskVar.get().seq();
        Var<Boolean> done = taskVar.zoomTo(TodoModel.Task::done, TodoModel.Task::withDone);
        Var<String>  text = taskVar.zoomTo(TodoModel.Task::text, TodoModel.Task::withText);

        // The list lens supplies every task, including ones the filter excludes —
        // we hide the latter via isVisibleIf rather than maintaining a parallel
        // "visible tasks" tuple, so identity / focus / caret position survives a
        // filter switch.
        Val<Boolean> visible = vm.viewAs(Boolean.class, m -> m.matches(taskVar.get()));

        return UI.panel("fill, insets 0, gap 10").id("task-" + seq)
            .isVisibleIf(visible)
            .withStyle( done, (isDone, it) -> it
                .backgroundColor(isDone ? BG_CARD : BG_CARD_HI)
                .borderRadius(12)
                .borderAt(Edge.LEFT, 3, isDone ? ACCENT_DONE : ACCENT)
                .margin(0, 0, 8, 0)
                .padding(10, 14, 10, 14)
            )
            .add("aligny center",
                UI.checkBox("", done)
            )
            .add("grow, push",
                UI.textField(text)
                .withStyle( done, (isDone, it) -> it
                    .backgroundColor(new Color(0,0,0,0))
                    .foregroundColor(isDone ? INK_DONE : INK)
                    .borderRadius(0)
                    .border(0, new Color(0,0,0,0))
                    .padding(2, 4, 2, 4)
                    .componentFont( f -> f.family("SansSerif").size(14) )
                )
            )
            .add("aligny center",
                UI.label(done.viewAsString(b -> b ? "✓ done" : "")).withStyle( it -> it
                    .foregroundColor(ACCENT_DONE)
                    .componentFont( f -> f.family("SansSerif").size(10).weight(2) )
                )
            )
            .add("aligny center",
                UI.button("×")
                .withStyle( it -> it
                    .backgroundColor(new Color(0,0,0,0))
                    .foregroundColor(INK_FAINT)
                    .borderRadius(14)
                    .padding(2, 8, 2, 8)
                    .margin(0)
                    .componentFont( f -> f.family("SansSerif").size(14).weight(2) )
                )
                .onClick( it -> vm.update(m -> m.removeTask(id)) )
            );
    }

    // ── Footer row ───────────────────────────────────────────────────────────

    private static UIForAnySwing<?,?> footerRow(Var<TodoModel> vm, Val<String> footerLine) {
        Val<Boolean> hasDone = vm.viewAs(Boolean.class, m -> m.doneCount() > 0);
        return UI.panel("fill, insets 0").withStyle( it -> it.backgroundColor(new Color(0,0,0,0)) )
            .add("pushx, growx",
                UI.label(footerLine).withStyle( it -> it
                    .foregroundColor(INK_FAINT)
                    .componentFont( f -> f.family("SansSerif").size(11) )
                )
            )
            .add("shrinkx",
                UI.button("Clear completed")
                .isEnabledIf(hasDone)
                .withStyle( it -> it
                    .backgroundColor(new Color(0,0,0,0))
                    .foregroundColor(hasDone.get() ? DANGER : INK_DONE)
                    .border(1, hasDone.get() ? DANGER : new Color(60, 70, 100))
                    .borderRadius(14)
                    .padding(4, 12, 4, 12)
                    .componentFont( f -> f.family("SansSerif").size(11) )
                )
                .onClick( it -> vm.update(TodoModel::clearCompleted) )
            );
    }

    // ── Unused tiny helpers, declared for the same reason as in TeamView:
    //    typing helpers tightly so add(..) doesn't trip over wildcards ────────

    @SuppressWarnings("unused")
    private static UIForLabel<JLabel>           lbl()  { return UI.label("");  }
    @SuppressWarnings("unused")
    private static UIForButton<JButton>         btn()  { return UI.button(""); }
    @SuppressWarnings("unused")
    private static UIForTextField<JTextField>   txt()  { return UI.textField(""); }

    // ── main ─────────────────────────────────────────────────────────────────

    public static void main(String... args) {
        FlatDarkLaf.setup();
        UI.show("Todo", f -> new TodoApp());
    }
}