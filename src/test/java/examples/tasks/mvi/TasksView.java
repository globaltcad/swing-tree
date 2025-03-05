package examples.tasks.mvi;

import sprouts.Tuple;
import sprouts.Var;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.threading.EventProcessor;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDropEvent;
import java.util.Locale;

import static swingtree.UI.*;

public final class TasksView extends Panel
{
    enum TaskType {
        IN_PROGRESS, DONE;

        TaskType toOther() {
            return this == IN_PROGRESS ? DONE : IN_PROGRESS;
        }
        String toTitle() {
            return this == IN_PROGRESS ? "In Progress" : "Done";
        }
    }

    TasksView(Var<TasksViewModel> vm) {
        Var<Tuple<TasksViewModel.TaskViewModel>> tasksInProgress = vm.zoomTo(TasksViewModel::tasksInProgress, TasksViewModel::withTasksInProgress);
        Var<Tuple<TasksViewModel.TaskViewModel>> tasksDone = vm.zoomTo(TasksViewModel::tasksDone, TasksViewModel::withTasksDone);
        Var<TasksViewModel.SummaryViewModel> summary = vm.zoomToNullable(TasksViewModel.SummaryViewModel.class, TasksViewModel::summary, TasksViewModel::withSummary);
        of(this).withLayout(FILL.and(INS(16)).and(WRAP(2)))
        .withPrefSize(750, 400)
        .add(CENTER.and(SPAN), html("<h1>Tasks</h1>"))
        .add(GROW.and(PUSH).and((SPAN)),
            scrollPane(it->it.fitWidth(true))
            .add(
                panel().withFlowLayout().withPrefSize(750,200)
                .add(AUTO_SPAN(it->it.veryLarge(6).medium(12)),
                    taskPanel(TaskType.IN_PROGRESS, tasksInProgress, tasksDone)
                )
                .add(AUTO_SPAN(it->it.veryLarge(6).medium(12)),
                    taskPanel(TaskType.DONE, tasksDone, tasksInProgress)
                )
            )
        )
        .add(
            panel(FILL).add(
                checkBox("Show Summary:").onClick(it -> {
                    vm.update(model -> model.showSummary(UI.runAndGet(()->it.get().isSelected())));
                })
            )
            .add(summary, summaryModel ->
                panel(FILL)
                .add(label("Tasks in progress:"))
                .add(GROW_X.and(PUSH_X), label(String.valueOf(summaryModel.numberOfTasksInProgress())))
                .add(label("Tasks done:"))
                .add(GROW_X.and(PUSH_X), label(String.valueOf(summaryModel.numberOfTasksDone())))
                .add(label("Total tasks:"))
                .add(GROW_X.and(PUSH_X), label(String.valueOf(summaryModel.numberOfTasksInTotal())))
            )
        );
    }

    private static UIForAnySwing<?,?> taskPanel(
        TaskType id,
        Var<Tuple<TasksViewModel.TaskViewModel>> entries,
        Var<Tuple<TasksViewModel.TaskViewModel>> otherEntries
    ) {
        return panel(FILL.and(WRAP(1)))
               .add(CENTER.and(SPAN), html("<h2>"+id.toTitle()+"</h2>"))
               .add(GROW_X.and(PUSH_X).and(SPAN),
                   button("+")
                   .onClick(it -> entries.update(tuple->tuple.add(new TasksViewModel.TaskViewModel())))
               )
               .add(GROW.and(PUSH),
                    scrollPanels().id(id).withPrefHeight(200)
                    .addAll(entries, (Var<TasksViewModel.TaskViewModel> entry) ->
                        panel(FILL)
                        .add(GROW_X.and(PUSH_X), textField(entry.zoomTo(TasksViewModel.TaskViewModel::task, TasksViewModel.TaskViewModel::withTask)))
                        .add(comboBox(entry.zoomTo(TasksViewModel.TaskViewModel::dueTo, TasksViewModel.TaskViewModel::withDueTo), it -> it.name().toLowerCase(Locale.ROOT)))
                        .add(button("✕").onClick(it -> entries.update(tuple->tuple.remove(entry))))
                        .add(button("⨮").onClick(it -> {
                            entries.update(tuple-> tuple.add(
                                new TasksViewModel.TaskViewModel()
                                    .withTask(entry.get().task())
                                    .withDueTo(entry.get().dueTo())
                            ));
                        }))
                        .withDragAway( conf -> conf
                            .payload(id.toOther().toString()+"|"+entries.get().firstIndexOf(entry))
                        )
                    )
                    .withDropSite( conf -> conf
                        .onDrop(it -> {
                            DropTargetDropEvent dropEvent = it.getEvent();
                            Transferable transferable = dropEvent.getTransferable();
                            if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                                String payload = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                                String[] parts = payload.split("\\|");
                                if (parts.length == 2) {
                                    TaskType sourceId = TaskType.valueOf(parts[0]);
                                    if ( sourceId == id ) {
                                        int sourceIndex = Integer.parseInt(parts[1]);
                                        TasksViewModel.TaskViewModel otherEntry = otherEntries.get().get(sourceIndex);
                                        otherEntries.update(tuple->tuple.remove(otherEntry));
                                        entries.update(tuple->tuple.add(otherEntry));
                                    }
                                }
                            }
                        })
                    )
                );
    }

    public static void main(String[] args)
    {
        Var<TasksViewModel> vm = Var.of(new TasksViewModel());
        UI.show(frame -> new TasksView(vm));
        EventProcessor.DECOUPLED.join();
    }
}
