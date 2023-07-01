package examples.stylish;

import swingtree.UI;

import java.awt.*;

import static swingtree.UI.Panel;
import static swingtree.UI.*;

public class WellRoundedView extends Panel
{
    public WellRoundedView()
    {
        of(this)
        .withLayout(FILL.and(WRAP(2)))
        .withStyle( it ->
            it.margin(24).padding(24)
              .backgroundColor(new Color(57, 221, 255,255))
              .borderRadius(32)
              .shadowBlurRadius(5)
              .shadowColor(new Color(0,0,0, 128))
              .shadowIsInset(true)
        )
        .add(SPAN.and(ALIGN_X_CENTER),
            box(INS(12)).add(html("<h1>A Well Rounded View</h1><p>Built using the SwingTree style API.</p>"))
        )
        .add(
            panel(INS(12), "[grow]")
            .withStyle( it ->
                it.backgroundColor(new Color(255, 255, 255,255))
                  .borderRadius(24)
            )
            .add(SPAN.and(WRAP), label("Isn't this a well rounded view?"))
            .add(GROW_X,
                button("Yes").withStyle( it -> it.borderRadius(24)),
                button("No")
            )
        )
        .add(TOP,
            box(INS(24)).add(html(
                "<i>SwingTree can override the default<br>" +
                        "look and feel based style<br>" +
                        "according to your needs.</i>"
            ))
        );
    }

    public static void main(String[] args)
    {
        UI.show(new WellRoundedView());
    }
}
