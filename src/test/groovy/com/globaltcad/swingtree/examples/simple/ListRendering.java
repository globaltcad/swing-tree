package com.globaltcad.swingtree.examples.simple;

import com.globaltcad.swingtree.UI;
import com.sun.tools.javac.util.List;

import javax.swing.*;

import static com.globaltcad.swingtree.UI.*;

public class ListRendering extends JPanel
{
    enum Test { A, B, C }

    public ListRendering() {
        of(this).withLayout(FILL)
        .add(GROW.and(WRAP), label("List Rendering Example:"))
        .add(GROW,
            of(new JList<Test>()).id("my-test-list")
            .withEntries(List.of(Test.A, Test.B, Test.C))
            .withRenderer(
                renderListItem(Test.class).asText( cell -> cell.getValue().toString() )
            )
        );
    }

    // Here you can test the UI:

    public static void main(String[] args) {
        UI.show(new ListRendering());
    }

}
