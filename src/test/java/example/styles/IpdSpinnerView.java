package example.styles;

import sprouts.Var;
import swingtree.UI;

import java.awt.*;

import static swingtree.UI.Panel;
import static swingtree.UI.*;

public class IpdSpinnerView<N extends Number> extends Panel
{
    public IpdSpinnerView(IpdAttributeVM<N> vm, N min, N max)
    {
        of(this).withLayout(FILL.and(INS(0)))
        .add(
            spinner(vm.value().get().intValue(), min.intValue(), max.intValue()).withValue(vm.value())
            .withStyle( it ->
                it.style().foregroundPainter( g2d -> {
                    if ( vm.isKnown().is(false) ) {
                        int width = it.component().getWidth();
                        int height = it.component().getHeight();
                        // We paint over the regular spinner contents with a white background
                        // and then a question mark in the middle.
                        g2d.setColor(Color.RED);
                        g2d.fillRect(4, 4, width-22, height-8);
                        g2d.setColor(Color.BLACK);
                    }
                })
            )
        );
    }

    public static void main(String... args) {
        UI.show(
            new IpdSpinnerView(
                new IpdAttributeVM<Object>(false, Var.of(2)),
                1, 10
            )
        );
    }
}
