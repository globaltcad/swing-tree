package examples.simple;


import swingtree.UI;

import static swingtree.UI.*;

/**
 *  Note that in this example we import the "UI" class from the "swingtree" package statically.
 *  This allows us to omit the "UI." prefix when using the methods of the "UI" class.
 */
class TodoApp extends Panel
{
    TodoApp() {
        of(this).withLayout(FILL)
        .add(GROW.and(SPAN).and(WRAP), label("Todo List"))
        .add(GROW.and(SPAN).and(WRAP),
            panel(FILL).add(SHRINK, button("Add"), button("Remove"))
        )
        .add(
            GROW.and(SPAN).and(WRAP),
            panel()
            .apply(it -> {
                String[] tasks = {"Get up in the morning", "Make coffee", "Drink coffee", "Code something in swingtree"};
                for (int i = 0; i < tasks.length; i++) {
                    it.add( WRAP,
                        panel().id("task-"+i)
                        .add(SHRINK_X, label("Task "+i+":"))
                        .add(PUSH_X, checkBox(tasks[i]))
                    );
                }
            })
        );
    }

    // Use this to test the above UI.
    public static void main(String... args) { UI.show(new TodoApp());  }

}
