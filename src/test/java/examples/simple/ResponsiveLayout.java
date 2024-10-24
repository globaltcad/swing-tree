package examples.simple;

import swingtree.UI;

import static swingtree.UI.*;


public class ResponsiveLayout extends Panel
{
    ResponsiveLayout(HorizontalAlignment align) {
        UI.of(this).withPrefSize(500, 400)
        .withFlowLayout(align, 7,7)
        .withBackground(Color.LIGHTGRAY)
        .add(AUTO_SPAN(it->it.small(12).medium(4).large(3).veryLarge(3)),
             box().withPrefSize(100,100).withStyle(it->it
                 .backgroundColor(Color.RED).borderRadius(24)
             )
        )
        .add(AUTO_SPAN( it -> it.small(3).medium(4).large(3).veryLarge(3).oversize(12)),
             box().withPrefSize(100,100).withStyle(it->it
                 .backgroundColor(Color.GREEN).borderRadius(24)
             )
        )
        .add(AUTO_SPAN(it->it.small(3).medium(4).large(3)),
             box().withPrefSize(100,100).withStyle(it->it
                 .backgroundColor(Color.BLUE).borderRadius(24)
             )
        )
        .add(AUTO_SPAN(it->it.small(3).medium(4).large(3)),
             box().withPrefSize(100,100).withStyle(it->it
                 .backgroundColor(Color.CYAN).borderRadius(24)
             )
        )
        .add(AUTO_SPAN(it->it.small(12).medium(4).large(6)),
             box().withPrefSize(100,100).withStyle(it->it
                 .backgroundColor(Color.OAK).borderRadius(24)
             )
        );
    }

    public static void main(String[] args) {
        UI.show("Align Center", f->new ResponsiveLayout(HorizontalAlignment.CENTER));
        UI.show("Align Left", f->new ResponsiveLayout(HorizontalAlignment.LEFT));
        UI.show("Align Right", f->new ResponsiveLayout(HorizontalAlignment.RIGHT));
    }
}
