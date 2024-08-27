package examples.hover;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Action;
import swingtree.ComponentSurfaceEventDelegate;
import swingtree.UI;

import javax.swing.*;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static swingtree.UI.*;

@NullMarked
public class HoverExample extends JPanel {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HoverExample.class);

    private static final Color COLOR_1 = new Color(189, 189, 189);
    private static final Color COLOR_2 = new Color(139, 139, 139);
    private static final Color COLOR_1_HOVER = new Color(253, 100, 100);
    private static final Color COLOR_2_HOVER = new Color(100, 207, 253);
    private static final Color COLOR_3_HOVER = new Color(215, 253, 100);


    public HoverExample() {
        of(this).withLayout("wrap, fill", "", "[][]40[][]40[][]")
                .add(label("SwingTree enter/exit behavior"))
                .add("grow", panel()
                        .withLayout("wrap 2, fill", "0[100]20[100]20", "20[100]20[100]20")
                        .withBackground(COLOR_1)
                        .onMouseEnter(log("root -> enter"))
                        .onMouseExit(log("root -> exit "))
                        .onMouseEnter(changeBackground(COLOR_1_HOVER))
                        .onMouseExit(changeBackground(COLOR_1))
                        .add(GROW, panel().withBackground(COLOR_2)
                                .peek(p -> p.addMouseListener(hover(COLOR_2_HOVER)))
                                .add(label("With listener"))
                                .onMouseEnter(log("  child -> enter"))
                                .onMouseExit(log("  child -> exit "))
                        )
                        .add(GROW, panel().withBackground(COLOR_2).add(label("No listener")))
                        .add(GROW, panel().withBackground(COLOR_2).add(label("No listener")))
                        .add(GROW, panel().withBackground(COLOR_2).add(label("With listener"))
                                .onMouseEnter(changeBackground(COLOR_3_HOVER))
                                .onMouseExit(changeBackground(COLOR_2))
                        )
                )
                .add(label("Swing enter/exit behavior"))
                .add("grow", panel()
                        .withLayout("wrap 2, fill", "0[100]20[100]20", "20[100]20[100]20")
                        .withBackground(COLOR_1)
                        .peek(p -> p.addMouseListener(hover(COLOR_1_HOVER)))
                        .add(GROW, panel().withBackground(COLOR_2)
                                .peek(p -> p.addMouseListener(hover(COLOR_2_HOVER)))
                                .add(label("With listener"))
                        )
                        .add(GROW, panel().withBackground(COLOR_2).add(label("No listener")))
                        .add(GROW, panel().withBackground(COLOR_2).add(label("No listener")))
                        .add(GROW, panel().withBackground(COLOR_2).add(label("With listener"))
                                .onMouseEnter(changeBackground(COLOR_3_HOVER))
                                .onMouseExit(changeBackground(COLOR_2))
                        )
                )
                .add(label("Swing enter/exit behavior with SwingTree"))
                .add("grow", panel()
                        .withLayout("wrap 2, fill", "0[100]20[100]20", "20[100]20[100]20")
                        .withBackground(COLOR_1)
                        .onMouseEnter(changeBackgroundIfSource(COLOR_1_HOVER))
                        .onMouseExit(changeBackgroundIfSource(COLOR_1))
                        .add(GROW, panel().withBackground(COLOR_2)
                                .peek(p -> p.addMouseListener(hover(COLOR_2_HOVER)))
                                .add(label("With listener"))
                        )
                        .add(GROW, panel().withBackground(COLOR_2).add(label("No listener")))
                        .add(GROW, panel().withBackground(COLOR_2).add(label("No listener")))
                        .add(GROW, panel().withBackground(COLOR_2).add(label("With listener"))
                                .onMouseEnter(changeBackgroundIfSource(COLOR_3_HOVER))
                                .onMouseExit(changeBackgroundIfSource(COLOR_2))
                        )
                );

    }

    public static void main(String[] args) {
        UI.show(f -> {
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            return new HoverExample();
        });
    }

    private static <C extends JComponent> Action<ComponentSurfaceEventDelegate<C>> changeBackground(Color color) {
        return delegate -> delegate.getComponent().setBackground(color);
    }

    private static <C extends JComponent> Action<ComponentSurfaceEventDelegate<C>> changeBackgroundIfSource(Color color) {
        return delegate -> {
            if (delegate.isSource())
                delegate.getComponent().setBackground(color);
        };
    }

    private static <C extends JComponent> Action<ComponentSurfaceEventDelegate<C>> log(String s) {
        return delegate -> System.out.printf("%s      source: %b\n", s, delegate.isSource());
    }

    private static MouseListener hover(final Color color) {
        return new MouseAdapter() {
            private final Color hoverColor = color;
            private @Nullable Color oldColor = null;

            @Override
            public void mouseEntered(MouseEvent e) {
                oldColor = e.getComponent().getBackground();
                e.getComponent().setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (oldColor != null) {
                    e.getComponent().setBackground(oldColor);
                    oldColor = null;
                }
            }
        };
    }
}
