package examples.simple;

import swingtree.UI;

import static swingtree.UI.*;


public class ResponsiveLayoutFill extends Panel
{
    ResponsiveLayoutFill(HorizontalAlignment align, boolean fill) {
        UI.of(this).withPrefSize(500, 400)
        .withFlowLayout(align, 7,7)
        .withBackground(Color.LIGHTGRAY)
        .add(AUTO_SPAN(it->it.small(12).medium(4).large(3).veryLarge(3).fill(fill)),
             box().withPrefSize(100,20).withStyle(it->it
                 .backgroundColor(Color.RED).borderRadius(24)
             )
        )
        .add(AUTO_SPAN( it -> it.small(3).medium(4).large(3).veryLarge(3).oversize(12).fill(fill)),
             box().withPrefSize(100,40).withStyle(it->it
                 .backgroundColor(Color.GREEN).borderRadius(24)
             )
        )
        .add(AUTO_SPAN(it->it.small(3).medium(4).large(3).fill(fill)),
             box().withPrefSize(100,50).withStyle(it->it
                 .backgroundColor(Color.BLUE).borderRadius(24)
             )
        )
        .add(AUTO_SPAN(it->it.small(3).medium(4).large(3).fill(fill)),
             box().withPrefSize(100,60).withStyle(it->it
                 .backgroundColor(Color.CYAN).borderRadius(24)
             )
        )
        .add(AUTO_SPAN(it->it.small(12).medium(4).large(6).fill(fill)),
             box().withPrefSize(100,70).withStyle(it->it
                 .backgroundColor(Color.OAK).borderRadius(24)
             )
        );
    }

    public static void main(String[] args) {
        UI.show("Fill Align Center", f->new ResponsiveLayoutFill(HorizontalAlignment.CENTER, true));
        //UI.show("Fill Align Left", f->new ResponsiveLayoutFill(HorizontalAlignment.LEFT, true));
        //UI.show("Fill Align Right", f->new ResponsiveLayoutFill(HorizontalAlignment.RIGHT, true));
        //UI.show("Don't Align Center", f->new ResponsiveLayoutFill(HorizontalAlignment.CENTER, false));
        //UI.show("Don't Align Left", f->new ResponsiveLayoutFill(HorizontalAlignment.LEFT, false));
        //UI.show("Don't Align Right", f->new ResponsiveLayoutFill(HorizontalAlignment.RIGHT, false));
    }
}
