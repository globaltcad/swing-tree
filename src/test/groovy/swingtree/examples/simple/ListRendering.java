package swingtree.examples.simple;

import swingtree.UI;
import com.sun.tools.javac.util.List;

import javax.swing.*;

import static swingtree.UI.*;

public class ListRendering extends JPanel
{
    enum Test { A, B, C }

    public ListRendering() {
        of(this).withLayout(FILL)
        .add(GROW.and(WRAP).and(SPAN), label("List Rendering Examples:"))
        .add(GROW,
            of(new JList<Test>()).id("my-test-list")
            .withEntries(List.of(Test.A, Test.B, Test.C))
            .withRenderer(
                renderListItem(Test.class).asText( cell -> cell.valueAsString().orElse("") )
            )
        )
        .add(GROW,
            listOf(new Number[]{1f, 2L, 3.0, 4})
            .withRenderer(
                renderList(Number.class)
                .when(Integer.class).asText( cell -> "Integer: "+cell.value().get() )
                .when(Long.class).asText( cell -> "Long: "+cell.value().get() )
                .when(Float.class).asText( cell -> "Float: "+cell.value().get() )
                .when(Number.class).asText( cell -> "Something else: "+cell.value().get() )
            )
        );
    }

    // Here you can test the UI:

    public static void main(String[] args) {
        UI.show(new ListRendering());
    }

}
