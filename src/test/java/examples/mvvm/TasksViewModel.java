package examples.mvvm;

import sprouts.Val;
import sprouts.Var;
import sprouts.Vars;

import java.time.DayOfWeek;

public class TasksViewModel
{
    private final Vars<TaskViewModel>   tasksInProgress = Vars.of(TaskViewModel.class);
    private final Vars<TaskViewModel>   tasksDone       = Vars.of(TaskViewModel.class);
    private final Var<SummaryViewModel> summary         = Var.ofNull(SummaryViewModel.class);


    public TasksViewModel() {
        TaskViewModel task1 = new TaskViewModel();
        task1.task().set("Book train to Munich");
        task1.dueTo().set(DayOfWeek.MONDAY);
        TaskViewModel task2 = new TaskViewModel();
        task2.task().set("Buy Aburaage at local store");
        task2.dueTo().set(DayOfWeek.TUESDAY);
        tasksInProgress.addAll(task1, task2);

        TaskViewModel task3 = new TaskViewModel();
        task3.task().set("Ride bike to work");
        task3.dueTo().set(DayOfWeek.WEDNESDAY);
        tasksDone.add(task3);
    }

    public Vars<TaskViewModel> tasksInProgress() { return tasksInProgress; }
    public Vars<TaskViewModel> tasksDone() { return tasksDone; }
    public Var<SummaryViewModel> summary() { return summary; }

    public void showSummary(boolean show) {
        if (show) {
            SummaryViewModel summaryViewModel = new SummaryViewModel(tasksInProgress, tasksDone);
            summary.set(summaryViewModel);
        } else {
            summary.set(null);
        }
    }

    public final static class TaskViewModel
    {
        private final Var<String> task = Var.of("");
        private final Var<DayOfWeek> dueTo = Var.of(DayOfWeek.MONDAY);

        public Var<String> task() { return task; }
        public Var<DayOfWeek> dueTo() { return dueTo; }
    }

    public final static class SummaryViewModel
    {
        private final Val<Integer> numberOfTasksInProgress;
        private final Val<Integer> numberOfTasksDone;
        private final Val<Integer> numberOfTasksInTotal;

        public SummaryViewModel(Vars<TaskViewModel> tasksInProgress, Vars<TaskViewModel> tasksDone) {
            this.numberOfTasksInProgress = tasksInProgress.viewSize();
            this.numberOfTasksDone = tasksDone.viewSize();
            this.numberOfTasksInTotal = Val.viewOf(this.numberOfTasksInProgress, this.numberOfTasksDone, (a, b) -> a + b);
        }

        public Val<Integer> numberOfTasksInProgress() { return numberOfTasksInProgress; }
        public Val<Integer> numberOfTasksDone() { return numberOfTasksDone; }
        public Val<Integer> numberOfTasksInTotal() { return numberOfTasksInTotal; }
    }
}
