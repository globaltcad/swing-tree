package examples.scrollpanes;

import swingtree.UI;
import static swingtree.UI.*;

import javax.swing.*;

public final class ScrollConfigExample extends JPanel {

    private static final String TEXT =
            "Lorem ipsum odor amet, consectetuer adipiscing elit. Taciti nec curabitur massa fringilla; taciti " +
            "purus faucibus nulla. Maecenas odio adipiscing viverra nisi nibh pharetra ex. Sit sed dolor fames " +
            "scelerisque est vel turpis per vehicula. Luctus justo semper maximus proin maecenas; dapibus malesuada " +
            "tempor. Bibendum dictum euismod nunc condimentum; magna amet. Penatibus pulvinar sapien molestie";

    public ScrollConfigExample() {
        of(this).withLayout("wrap, fill").withPrefSize(400, 600)
        .add("grow",label("In a Simple Panel:"))
        .add("grow",
            panel().withLayout("ins 0, debug, fill")
                .add("grow",
                    panel().withLayout("wrap", "", "[]push[]")
                    .withBackground(Color.LIGHT_GRAY)
                    .add(html(TEXT))
                    .add(html("END"))
                )
        )
        .add("grow",label("In a Scroll Pane:"))
        .add("grow",
            scrollPane(config -> config.fitWidth(true))
            .add(GROW,
                panel().withLayout("wrap", "", "[]push[]")
                .withBackground(Color.LIGHT_GRAY)
                .add(html(TEXT))
                .add(html("END"))
            )
        )
        .add("grow",label("In a Colored Scroll Pane:"))
        .add("grow",
            scrollPane(config -> config.fitWidth(true))
            .withBackground(Color.ORANGE)
            .add(GROW,
                panel().withLayout("wrap", "", "[]push[]")
                .withBackground(Color.LIGHT_GRAY)
                .add(html(TEXT))
                .add(html("END"))
            )
        );
    }


    public static void main(String[] args) {
        UI.show(frame -> new ScrollConfigExample());
    }

}
