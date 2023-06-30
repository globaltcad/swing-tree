package examples.stylish;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;

import java.awt.*;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.Panel;
import static swingtree.UI.*;

public class SoftUIView extends Panel
{
    public SoftUIView() {
        FlatLightLaf.setup();
        UI.use(new SoftUIStyleSheet(), ()->
            UI.of(this).group("soft sink")
            .add(SHRINK.and(ALIGN_LEFT),
                box().group("soft raise")
                .withLayout(FILL.and(WRAP(3)).and(INS(32)), "", "[][]24[]24[]")
                .add( SHRINK.and(SPAN).and(ALIGN_CENTER),
                    panel().group("soft banner")
                    .add(
                        html(
                            "<h1>Soft UI In Swing O.o</h1>" +
                            "<p>" +
                            "See what you can do with this library! " +
                            "</p>"
                        )
                    )
                    .onMouseMove( it -> {
                        it.animateOnce(1, TimeUnit.SECONDS, state -> {
                            double r = 30 * state.fadeIn() * it.getUIScale();
                            double x = it.getEvent().getX() - r / 2.0;
                            double y = it.getEvent().getY() - r / 2.0;
                            it.paint(state, g -> {
                                g.setColor(new Color(1f, 1f, 0f, (float) state.fadeOut()));
                                g.fillOval((int) x, (int) y, (int) r, (int) r);
                            });
                        });
                    })
                    .add(
                        label(50,50,icon("img/trees.png")).group("soft banner")
                    )
                )
                .add( SHRINK.and(SPAN).and(ALIGN_CENTER),
                    box().group("soft sink")
                    .add(
                        slider(Align.VERTICAL, 0, 255).group("soft slim")
                    )
                    .add(WRAP,
                        box(FILL.and(WRAP(2)))
                        .add(
                            box(FILL.and(GAP_REL(0)))
                            .add(GROW_X,
                                button("Click Me!").group("soft button")
                            )
                            .add(GROW_X.and(WRAP),
                                toggleButton("Toggle Me!").group("soft button")
                            )
                            .add(GROW_X,
                                checkBox("Check")
                            )
                            .add(GROW_X.and(WRAP),
                                radioButton("Radio")
                            )
                            .add(GROW_X,
                                spinner()
                            )
                            .add(GROW_X.and(WRAP),
                                comboBox("A", "B", "C")
                            )
                        )
                        .add(
                            box(FILL.and(GAP_REL(0)).and(WRAP(1)))
                            .add(GROW_X,
                                textField("Text Field")
                            )
                            .add(GROW_X,
                                passwordField("Password Field")
                            )
                            .add(GROW_X.and(WRAP),
                                textArea("Text Area")
                            )
                        )
                        .add(SPAN.and(GROW_X),
                             progressBar(Align.HORIZONTAL, 0, 100).withValue(68)
                             .peek(it->{it.setString("%"); it.setStringPainted(true);})
                             .withBackground(Color.WHITE)
                        )
                    )
                )
            )
            .add(
                label(icon("img/swing.png")).withStyle( it -> it.padding(24) )
            )
            .onMouseClick( it -> it.animateOnce(2, TimeUnit.SECONDS, state -> {
                it.paint(state, g -> {
                    g.setColor(new Color(0.1f, 0.25f, 0.5f, (float) state.fadeOut()));
                    for ( int i = 0; i < 5; i++ ) {
                        double r = 300 * state.fadeIn() * ( 1 - i * 0.2 ) * it.getUIScale();
                        double x = it.getEvent().getX() - r / 2;
                        double y = it.getEvent().getY() - r / 2;
                        g.drawOval((int) x, (int) y, (int) r, (int) r);
                    }
                });
            }))
        );
    }


    public static void main(String[] args) {
        UI.show(new SoftUIView());
    }
}
