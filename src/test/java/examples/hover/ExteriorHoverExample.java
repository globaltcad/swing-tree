package examples.hover;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Action;
import swingtree.ComponentMouseEventDelegate;
import swingtree.UI;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static swingtree.UI.*;

@NullMarked
public class ExteriorHoverExample extends JPanel {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ExteriorHoverExample.class);

    private static final Color COLOR_1 = new Color(189, 189, 189);
    private static final Color COLOR_2 = new Color(139, 139, 139);
    private static final Color COLOR_1_HOVER = new Color(253, 100, 100);
    private static final Color COLOR_2_HOVER = new Color(100, 207, 253);
    private static final Color COLOR_3_HOVER = new Color(215, 253, 100);
    private static final Color COLOR_4_HOVER = new Color(253, 182, 100);

    private static void printAndDispatchIndicationAnimation( ComponentMouseEventDelegate<?> delegate, String info ) {
        System.out.println(info);
        delegate.animateFor(0.25, java.util.concurrent.TimeUnit.SECONDS, status -> {
            double r = 35 * status.fadeIn() * delegate.getScale();
            double x = delegate.mouseX() - r / 2.0;
            double y = delegate.mouseY() - r / 2.0;
            delegate.paint(ComponentArea.EXTERIOR, status, g -> {
                g.setColor(new Color(0f, 1f, 1f, (float) status.fadeOut()));
                g.fillOval((int) x, (int) y, (int) r, (int) r);
            });
        });

    }

    public ExteriorHoverExample() {
        of(this).withLayout("wrap 2, fill", "[]40[]", "[]40[]")
            .add(
                panel().withLayout("wrap, ins 0, fill")
                .add(label("SwingTree enter/exit behavior"))
                .add("grow",
                    panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                    .withLayout("wrap 2, fill", "0[100]0[100]20", "20[100]20[100]20")
                    .withBackground(COLOR_1)
                    .onMouseEnter(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "root -> enter"))
                    .onMouseExit(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "root -> exit "))
                    .onMouseEnter(ComponentArea.EXTERIOR, changeBackground(COLOR_1_HOVER))
                    .onMouseExit(ComponentArea.EXTERIOR, changeBackground(COLOR_1))
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                        .add(html("child 1<br>with listener"))
                        .withBackground(COLOR_2)
                        .onMouseEnter(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "  child 1 -> enter"))
                        .onMouseExit(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "  child 1 -> exit"))
                        .onMouseEnter(ComponentArea.EXTERIOR, changeBackground(COLOR_2_HOVER))
                        .onMouseExit(ComponentArea.EXTERIOR, changeBackground(COLOR_2))
                    )
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                        .add(html("child 2<br>with listener"))
                        .withBackground(COLOR_2)
                        .onMouseEnter(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "  child 2 -> enter"))
                        .onMouseExit(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "  child 2 -> exit"))
                        .onMouseEnter(ComponentArea.EXTERIOR, changeBackground(COLOR_4_HOVER))
                        .onMouseExit(ComponentArea.EXTERIOR, changeBackground(COLOR_2))
                    )
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("white"))
                        .add(html("child 3<br>no listener"))
                        .withBackground(COLOR_2)
                    )
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                        .add(html("child 4<br>with listener"))
                        .withBackground(COLOR_2)
                        .onMouseEnter(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "  child 4 -> enter"))
                        .onMouseExit(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "  child 4 -> exit"))
                        .onMouseEnter(ComponentArea.EXTERIOR, changeBackground(COLOR_3_HOVER))
                        .onMouseExit(ComponentArea.EXTERIOR, changeBackground(COLOR_2))
                    )
                )
            )
            .add(panel().withLayout("wrap, ins 0, fill")
                .add(label("Swing enter/exit behavior"))
                .add("grow",
                    panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                    .withLayout("wrap 2, fill", "0[100]0[100]20", "20[100]20[100]20")
                    .withBackground(COLOR_1)
                    .peek(p -> p.addMouseListener(hover(COLOR_1_HOVER)))
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                        .add(html("child 1<br>with listener"))
                        .withBackground(COLOR_2)
                        .peek(p -> p.addMouseListener(hover(COLOR_2_HOVER)))
                    )
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                        .add(html("child 2<br>with listener"))
                        .withBackground(COLOR_2)
                        .peek(p -> p.addMouseListener(hover(COLOR_4_HOVER)))
                    )
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("white"))
                        .add(html("child 3<br>no listener"))
                        .withBackground(COLOR_2)
                    )
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                        .add(html("child 4<br>with listener"))
                        .withBackground(COLOR_2)
                        .onMouseEnterGreedy(changeBackground(COLOR_3_HOVER))
                        .onMouseExitGreedy(changeBackground(COLOR_2))
                    )
                )
            )
            .add(panel().withLayout("wrap, ins 0, fill")
                .add(label("SwingTree: only enter listener"))
                .add("grow",
                    panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                    .withLayout("wrap 2, fill", "0[100]0[100]20", "20[100]20[100]20")
                    .withBackground(COLOR_1)
                    .onMouseEnter(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "root -> enter"))
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                        .add(html("child 1<br>with listener"))
                        .withBackground(COLOR_2)
                        .onMouseEnter(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "  child 1 -> enter"))
                    )
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                        .add(html("child 2<br>with listener"))
                        .withBackground(COLOR_2)
                        .onMouseEnter(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "  child 2 -> enter"))
                    )
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("white"))
                        .add(html("child 3<br>no listener"))
                        .withBackground(COLOR_2)
                    )
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                        .add(html("child 4<br>with listener"))
                        .withBackground(COLOR_2)
                        .onMouseEnter(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "  child 4 -> enter"))
                    )
                )
            )
            .add(panel().withLayout("wrap, ins 0, fill")
                .add(label("SwingTree: only exit listener"))
                .add("grow",
                    panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                    .withLayout("wrap 2, fill", "0[100]0[100]20", "20[100]20[100]20")
                    .withBackground(COLOR_1)
                    .onMouseExit(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "root -> exit"))
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                        .add(html("child 1<br>with listener"))
                        .withBackground(COLOR_2)
                        .onMouseExit(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "  child 1 -> exit"))
                    )
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                        .add(html("child 2<br>with listener"))
                        .withBackground(COLOR_2)
                        .onMouseExit(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "  child 2 -> exit"))
                    )
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("white"))
                        .add(html("child 3<br>no listener"))
                        .withBackground(COLOR_2)
                    )
                    .add(GROW,
                        panel().withStyle(it->it.margin(12).borderRadius(12).foundationColor("black"))
                        .add(html("child 4<br>with listener"))
                        .withBackground(COLOR_2)
                        .onMouseExit(ComponentArea.EXTERIOR, it -> printAndDispatchIndicationAnimation(it, "  child 4 -> exit"))
                    )
                )
            );

    }

    public static void main(String[] args) {
        UI.show(f -> {
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            return new ExteriorHoverExample();
        });
    }

    private static <C extends JComponent> Action<ComponentMouseEventDelegate<C>> changeBackground(Color color) {
        return delegate -> delegate.getComponent().setBackground(color);
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
