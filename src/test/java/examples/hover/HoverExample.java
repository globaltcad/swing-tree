package examples.hover;

import org.slf4j.Logger;
import swingtree.UI;

import javax.swing.*;

import java.awt.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static swingtree.UI.*;

public class HoverExample extends JPanel {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HoverExample.class);

    private static final Color COLOR_1 = new Color(189, 189, 189);
    private static final Color COLOR_1_HOVER = new Color(253, 100, 100);

    private static final Color COLOR_2 = new Color(139, 139, 139);
    private static final Color COLOR_2_HOVER = new Color(100, 207, 253);


    public HoverExample() {

        of(this).withLayout("wrap 2, fill", "20[100]20[100]20", "20[100]20[100]20")
            .withBackground(COLOR_1)
            .onMouseEnter(delegate -> delegate.getComponent().setBackground(COLOR_1_HOVER))
            .onMouseExit(delegate -> delegate.getComponent().setBackground(COLOR_1))

            .add(GROW,panel().withBackground(COLOR_2)
                .onMouseEnter(delegate -> delegate.getComponent().setBackground(COLOR_2_HOVER))
                .onMouseExit(delegate -> delegate.getComponent().setBackground(COLOR_2))
                .add(label("With listener"))
            )
            .add(GROW, panel().withBackground(COLOR_2).add(label("No listener")))
            .add(GROW, panel().withBackground(COLOR_2).add(label("No listener")))
            .add(GROW, panel().withBackground(COLOR_2).add(label("No listener")));

    }

    public static void main(String[] args) {
        UI.show(f -> {
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            return new HoverExample();
        });
    }

}
