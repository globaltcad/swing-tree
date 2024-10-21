package examples.simple;

import swingtree.UI;

import static swingtree.UI.*;


public class ResponsiveLayout extends Panel
{
    ResponsiveLayout() {
        UI.of(this).withPrefSize(400, 300)
        .withFlowLayout(HorizontalAlignment.CENTER, 7,7)
        .add(AUTO_SPAN(it->it.small(12).medium(4).large(3).veryLarge(1)),
             html("Cell 1").withStyle(it->it
                 .backgroundColor(Color.RED)
             )
        )
        .add(AUTO_SPAN( it -> it.small(8).medium(4).large(3).veryLarge(3).oversize(12)),
             html("Cell 2").withStyle(it->it
                 .backgroundColor(Color.GREEN)
             )
        )
        .add(AUTO_SPAN(it->it.small(6).medium(4).large(3)),
             html("Cell 3").withStyle(it->it
                 .backgroundColor(Color.BLUE)
             )
        )
        .add(AUTO_SPAN(it->it.small(6).medium(4).large(3)),
             html("Cell 4").withStyle(it->it
                 .backgroundColor(Color.CYAN)
             )
        )
        .add(AUTO_SPAN(it->it.small(6).medium(4).large(3)),
             html("Cell 5").withStyle(it->it
                 .backgroundColor(Color.OAK)
             )
        );
    }

    public static void main(String[] args) {
        UI.show(f->new ResponsiveLayout());
    }
}
