package examples.animated;

import sprouts.Var;
import swingtree.UI;
import swingtree.animation.LifeTime;

import javax.swing.*;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

public class FoldableExample extends JPanel {

    private static final Color COLOR1 = new Color(243, 198, 83);
    private static final Color COLOR2 = new Color(83, 189, 243);
    private static final Color COLOR3 = new Color(234, 40, 108);

    private static final String TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod" +
            " incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco" +
            " laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate" +
            " velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt" +
            " in culpa qui officia deserunt mollit anim id est laborum.";

    public FoldableExample() {
        Var<Boolean> show_1 = Var.of(false);
        Var<Boolean> show_2 = Var.of(true);
        Var<Boolean> show_3 = Var.of(false);

        of(this).withLayout("fillx, wrap")
                .withPrefSize(500, 500)
                .add(toggleButton("show", show_1))
                .add(panel().withBackground(COLOR1).add(html(TEXT))
                        .withTransitionalStyle(show_1, LifeTime.of(0.1, TimeUnit.SECONDS), (status, delegate) -> {
                            int prefHeight = 200;
                            int h = (int) Math.round(prefHeight * status.fadeIn());
                            System.out.printf("fadeIn: %.2f h: %d\n", status.fadeIn(), h);
                            return delegate.prefHeight(h).minHeight(h).maxHeight(h).height(h);
                        })
                )
                .add(toggleButton("show", show_2))
                .add(panel().withBackground(COLOR2).add(html(TEXT))
                        .withTransitionalStyle(show_2, LifeTime.of(0.1, TimeUnit.SECONDS), (status, delegate) -> {
                            int h = (int) Math.round(200 * status.fadeIn());
                            return delegate.prefHeight(h).minHeight(h).maxHeight(h);
                        })
                )
                .add(toggleButton("show", show_3))
                .add(panel().withBackground(COLOR3).add(html(TEXT))
                        .withTransitionalStyle(show_3, LifeTime.of(0.1, TimeUnit.SECONDS), (status, delegate) -> {
                            int h = (int) Math.round(200 * status.fadeIn());
                            return delegate.prefHeight(h).minHeight(h).maxHeight(h);
                        })
                );
    }

    public static void main(String[] args) {
        UI.show(f -> {
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            return new FoldableExample();
        });
    }

}
