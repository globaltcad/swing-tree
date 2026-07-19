package examples.scrollpanes;

import sprouts.Var;
import swingtree.UI;
import swingtree.animation.LifeTime;

import javax.swing.*;

import java.awt.*;
import java.awt.Color;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

public class ScrollableTest extends JPanel {

    public ScrollableTest() {
        test1();
    }

    private void test1() {
        Var<Boolean> show = Var.of(false);

        of(this).withLayout("fill").withPrefSize(300, 400)
            .add("grow",
                scrollPane(config -> config
                    .fitWidth(true)
                    //.fitHeight(config.viewport().getPreferredSize() != null)                             // <--- InvocationTargetException
                    .fitHeight(config.viewport().getHeight() > config.view().getHeight())                  // <--- flickers
                    //.fitHeight(config.viewport().getHeight() > config.view().getPreferredSize().height)) // <--- works
                )
                    .add(panel().withLayout("fill, wrap", "", "[shrink][grow]")
                        .add(toggleButton("show", show))
                        .add("growx",
                            panel().withBackground(Color.ORANGE)
                                .withTransitionalStyle(show, LifeTime.of(0.5, TimeUnit.SECONDS), (status, delegate) -> {
                                    int prefHeight = 200;
                                    int h = (int) Math.round(prefHeight * status.fadeIn());
                                    System.out.printf("fadeIn: %.2f h: %d\n", status.fadeIn(), h);
                                    return delegate.prefHeight(h).minHeight(h).maxHeight(h).height(h);
                                })
                        )
                    )

            );
    }

    public static void main(String[] args) {
        UI.show(f -> {
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            return new ScrollableTest();
        });
    }

}
