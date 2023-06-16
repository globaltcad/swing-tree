package swingtree.examples.simple;

import swingtree.UI;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static swingtree.UI.*;

public class ListRendering extends Panel
{
    enum Test { A, B, C }

    public ListRendering() {
        of(this).withLayout(FILL.and(WRAP(1)))
        .add( SHRINK.and(SPAN).and(ALIGN_CENTER),
            html(
                "<h1>Declarative JList Creation</h1>" +
                "<p>" +
                "A view of various JList instances with custom renderer." +
                "</p>"
            )
        )
        .add(GROW.and(PUSH_X), separator())
        .add(GROW,
            panel().withBorderTitled("Object to String").withMinWidth(200)
            .add(GROW,
                of(new List<Test>()).id("my-test-list")
                .withEntries(Stream.of(Test.A, Test.B, Test.C).collect(Collectors.toList()))
                .withRenderer(
                    renderListItem(Test.class).asText( cell -> cell.valueAsString().orElse("") )
                )
            )
        )
        .add(GROW,
            panel().withBorderTitled("Number Rendering").withMinWidth(200)
            .add(GROW,
                listOf(new Number[]{1f, 2L, 3.0, 4})
                .withRenderer(
                    renderList(Number.class)
                    .when(Integer.class).asText( cell -> "Integer: "+cell.value().get() )
                    .when(Long.class).asText( cell -> "Long: "+cell.value().get() )
                    .when(Float.class).asText( cell -> "Float: "+cell.value().get() )
                    .when(Number.class).asText( cell -> "Something else: "+cell.value().get() )
                )
            )
        );
    }

    // Here you can test the UI:

    public static void main(String[] args) {
        UI.show(new ListRendering());
        UI.joinDecoupledEventProcessor();
    }

}
