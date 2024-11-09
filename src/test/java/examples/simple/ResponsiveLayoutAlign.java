package examples.simple;

import swingtree.UI;

import static swingtree.UI.*;


public class ResponsiveLayoutAlign extends Panel
{
    ResponsiveLayoutAlign(HorizontalAlignment align, UI.VerticalAlignment alignment) {
        UI.of(this).withPrefSize(500, 400)
        .withFlowLayout(align, 7,7)
        .withBackground(Color.LIGHTGRAY)
        .add(AUTO_SPAN(it->it.small(12).medium(4).large(3).veryLarge(3).align(alignment)),
             box().withPrefSize(100,20).withStyle(it->it
                 .backgroundColor(Color.RED).borderRadius(24)
             )
        )
        .add(AUTO_SPAN( it -> it.small(3).medium(4).large(3).veryLarge(3).oversize(12).align(alignment)),
             box().withPrefSize(100,40).withStyle(it->it
                 .backgroundColor(Color.GREEN).borderRadius(24)
             )
        )
        .add(AUTO_SPAN(it->it.small(3).medium(4).large(3).align(alignment)),
             box().withPrefSize(100,50).withStyle(it->it
                 .backgroundColor(Color.BLUE).borderRadius(24)
             )
        )
        .add(AUTO_SPAN(it->it.small(3).medium(4).large(3).align(alignment)),
             box().withPrefSize(100,60).withStyle(it->it
                 .backgroundColor(Color.CYAN).borderRadius(24)
             )
        )
        .add(AUTO_SPAN(it->it.small(12).medium(4).large(6).align(alignment)),
             box().withPrefSize(100,70).withStyle(it->it
                 .backgroundColor(Color.OAK).borderRadius(24)
             )
        );
    }

    public static void main(String[] args) {
        UI.show("Align Vertically Top & Horizontally Center", f->new ResponsiveLayoutAlign(HorizontalAlignment.CENTER, VerticalAlignment.TOP));
        //UI.show("Align Vertically Top & Horizontally Left", f->new ResponsiveLayoutFill(HorizontalAlignment.LEFT, VerticalAlignment.TOP));
        //UI.show("Align Vertically Top & Horizontally Right", f->new ResponsiveLayoutFill(HorizontalAlignment.RIGHT, VerticalAlignment.TOP));

        UI.show("Align Vertically Center & Horizontally Center", f->new ResponsiveLayoutAlign(HorizontalAlignment.CENTER, VerticalAlignment.CENTER));
        //UI.show("Align Vertically Center & Horizontally Left", f->new ResponsiveLayoutFill(HorizontalAlignment.LEFT, VerticalAlignment.CENTER));
        //UI.show("Align Vertically Center & Horizontally Right", f->new ResponsiveLayoutFill(HorizontalAlignment.RIGHT, VerticalAlignment.CENTER));

        UI.show("Align Vertically Bottom & Horizontally Center", f->new ResponsiveLayoutAlign(HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM));
        //UI.show("Align Vertically Bottom & Horizontally Left", f->new ResponsiveLayoutFill(HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM));
        //UI.show("Align Vertically Bottom & Horizontally Right", f->new ResponsiveLayoutFill(HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM));
    }
}
