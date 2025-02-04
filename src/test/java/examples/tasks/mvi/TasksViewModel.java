package examples.tasks.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import sprouts.HasId;
import sprouts.Tuple;

import java.time.DayOfWeek;
import java.util.UUID;

@With @Getter @Accessors(fluent = true)
public final class TasksViewModel
{
    private final Tuple<TaskViewModel>       tasksInProgress;
    private final Tuple<TaskViewModel>       tasksDone;
    private final @Nullable SummaryViewModel summary;


    public TasksViewModel(Tuple<TaskViewModel> tasksInProgress, Tuple<TaskViewModel> tasksDone, @Nullable SummaryViewModel summary) {
        this.tasksInProgress = tasksInProgress;
        this.tasksDone = tasksDone;
        this.summary = summary == null ? null : new SummaryViewModel(summary.numberOfTasksInProgress, summary.numberOfTasksDone, summary.numberOfTasksInTotal);
    }

    public TasksViewModel() {
        this(
            Tuple.of(
                new TaskViewModel().withTask("Book train to Munich"),
                new TaskViewModel().withTask("Buy Aburaage at local store").withDueTo(DayOfWeek.TUESDAY)
            ),
            Tuple.of(new TaskViewModel().withTask("Ride bike to work").withDueTo(DayOfWeek.WEDNESDAY)),
            null
        );
    }

    public TasksViewModel showSummary(boolean show) {
        if (show) {
            return this.withSummary(this.createSummary());
        } else {
            return this.withSummary(null);
        }
    }

    private SummaryViewModel createSummary() {
        return new SummaryViewModel(
            tasksInProgress.size(),
            tasksDone.size(),
            tasksInProgress.size() + tasksDone.size()
        );
    }

    @With @Getter @Accessors(fluent = true) @AllArgsConstructor
    public final static class TaskViewModel implements HasId<UUID>
    {
        private final UUID id;
        private final String task;
        private final DayOfWeek dueTo;

        public TaskViewModel() {this(UUID.randomUUID(), "", DayOfWeek.MONDAY);}
    }

    @With @Getter @Accessors(fluent = true) @AllArgsConstructor
    public final static class SummaryViewModel
    {
        private final int numberOfTasksInProgress;
        private final int numberOfTasksDone;
        private final int numberOfTasksInTotal;
    }
}
