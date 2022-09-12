package com.globaltcad.swingtree.examples.slim

import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.examples.simple.Calculator

import javax.swing.JFrame
import javax.swing.JPanel
import java.awt.Dimension

// We import the "UI" class from the "swingtree" package statically:
import static com.globaltcad.swingtree.UI.*
// This allows us to ommit the "UI." prefix when using the methods of the "UI" class.

class TodoApp extends JPanel
{
    TodoApp() {
        of(this).withLayout("fill")
        .add(GROW & SPAN & WRAP, label("Todo List"))
        .add(GROW & SPAN & WRAP,
            panel(FILL).add(SHRINK, button("Add"), button("Remove"))
        )
        .add(
            GROW & SPAN & WRAP,
            panel()
            .apply({
                var myList = ["Get up in the morning", "Make coffee", "Drink coffee", "Code something in swingtree"]
                myList.eachWithIndex { task, i ->
                    it.add( WRAP,
                        panel().id("task-$i")
                        .add(SHRINK_X, label("Task $i:"))
                        .add(PUSH_X, checkBox(task))
                    )
                }
            })
        )
    }

    // Use this to test the above UI.
    public static void main(String... args) {
        new TestWindow(JFrame::new,new TodoApp()).getFrame().setSize(new Dimension(240, 325));
    }

}
