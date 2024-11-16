package examples.simple;

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
                of(new ListView<Test>()).id("my-test-list")
                .withEntries(Stream.of(Test.A, Test.B, Test.C).collect(Collectors.toList()))
                .withCells( it -> it
                    .when(Test.class)
                    .asText( cell -> cell.entryAsString() )
                )
            )
        )
        .add(GROW,
            panel().withBorderTitled("Number Rendering").withMinWidth(200)
            .add(GROW,
                listOf(new Number[]{1f, 2L, 3.0, 4})
                .withCells( it -> it
                    .when(Integer.class).asText( cell -> "Integer: "+cell.entry().get() )
                    .when(Long.class).asText( cell -> "Long: "+cell.entry().get() )
                    .when(Float.class).asText( cell -> "Float: "+cell.entry().get() )
                    .when(Number.class).asText( cell -> "Something else: "+cell.entry().get() )
                )
            )
        );
    }

    // Here you can test the UI:

    public static void main(String[] args) {
        UI.show( f -> new ListRendering() );
    }

}
