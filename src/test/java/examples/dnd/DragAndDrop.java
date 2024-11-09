package examples.dnd;

import static swingtree.UI.*;

import swingtree.UI;

public final class DragAndDrop extends Box
{
    public DragAndDrop()
    {
        UI.of(this).withFlowLayout()
        .withPrefWidth(450)
        .add(AUTO_SPAN(it->it.small(12).large(6)),
            box().withStyle( it -> it
                .foundationColor("light grey")
                .shadowIsInset(true)
                .shadowColor("black")
                .shadowBlurRadius(3)
                .borderRadius(24)
                .margin(16)
                .padding(16)
            )
        )
        .add(AUTO_SPAN(it->it.small(12).large(6)),
            box().withStyle( it -> it
                .foundationColor("light grey")
                .shadowIsInset(true)
                .shadowColor("black")
                .shadowBlurRadius(3)
                .borderRadius(24)
                .margin(16)
                .padding(16)
            )
        );
    }

    public static void main(String[] args)
    {
        UI.show( f -> new DragAndDrop());
    }
}
