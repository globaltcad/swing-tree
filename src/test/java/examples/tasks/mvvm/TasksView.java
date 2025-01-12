package examples.tasks.mvvm;

import sprouts.Vars;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.threading.EventProcessor;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDropEvent;

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

    TasksView(TasksViewModel vm) {
        of(this).withLayout(FILL.and(INS(16)).and(WRAP(2)))
        .withPrefSize(750, 400)
        .add(CENTER.and(SPAN), html("<h1>Tasks</h1>"))
        .add(GROW.and(PUSH).and((SPAN)),
            scrollPane(it->it.fitWidth(true))
            .add(
                panel().withFlowLayout().withPrefSize(750,200)
                .add(AUTO_SPAN(it->it.veryLarge(6).medium(12)),
                    taskPanel(TaskType.IN_PROGRESS, vm.tasksInProgress(), vm.tasksDone())
                )
                .add(AUTO_SPAN(it->it.veryLarge(6).medium(12)),
                    taskPanel(TaskType.DONE, vm.tasksDone(), vm.tasksInProgress())
                )
            )
        )
        .add(
            panel(FILL).add(
                checkBox("Show Summary:").onClick(it -> {
                    vm.showSummary(UI.runAndGet(()->it.get().isSelected()));
                })
            )
            .add(vm.summary(), summary ->
                panel(FILL)
                .add(label("Tasks in progress:"))
                .add(GROW_X.and(PUSH_X), label(summary.numberOfTasksInProgress().viewAsString()))
                .add(label("Tasks done:"))
                .add(GROW_X.and(PUSH_X), label(summary.numberOfTasksDone().viewAsString()))
                .add(label("Total tasks:"))
                .add(GROW_X.and(PUSH_X), label(summary.numberOfTasksInTotal().viewAsString()))
            )
        );
    }

    private static UIForAnySwing<?,?> taskPanel(
            TaskType id,
            Vars<TasksViewModel.TaskViewModel> entries,
            Vars<TasksViewModel.TaskViewModel> otherEntries
    ) {
        return panel(FILL.and(WRAP(1)))
               .add(CENTER.and(SPAN), html("<h2>"+id.toTitle()+"</h2>"))
               .add(GROW_X.and(PUSH_X).and(SPAN),
                   button("+")
                   .onClick(it -> entries.add(new TasksViewModel.TaskViewModel()))
               )
               .add(GROW.and(PUSH),
                    scrollPanels().id(id).withPrefHeight(200)
                    .addAll(entries, entry ->
                        panel(FILL)
                        .add(GROW_X.and(PUSH_X), textField(entry.task()))
                        .add(comboBox(entry.dueTo(), it -> it.name().toLowerCase()))
                        .add(button("✕").onClick(it -> entries.remove(entry)))
                        .add(button("⨮").onClick(it -> {
                            TasksViewModel.TaskViewModel newEntry = new TasksViewModel.TaskViewModel();
                            newEntry.task().set(entry.task().get());
                            newEntry.dueTo().set(entry.dueTo().get());
                            entries.add(newEntry);
                        }))
                        .withDragAway( conf -> conf
                            .payload(id.toOther().toString()+"|"+entries.indexOf(entry))
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
                                        TasksViewModel.TaskViewModel otherEntry = otherEntries.at(sourceIndex).get();
                                        otherEntries.remove(otherEntry);
                                        entries.add(otherEntry);
                                    }
                                }
                            }
                        })
                    )
                );
    }

    public static void main(String[] args)
    {
        UI.showUsing(EventProcessor.DECOUPLED, frame -> new TasksView(new TasksViewModel()));
        EventProcessor.DECOUPLED.join();
    }
}
