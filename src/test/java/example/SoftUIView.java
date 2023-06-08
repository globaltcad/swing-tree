package example;

import com.formdev.flatlaf.FlatLightLaf;
import example.styles.SoftUIStyleSheet;
import swingtree.UI;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

public class SoftUIView extends Panel
{
    public SoftUIView() {
        FlatLightLaf.setup();
        UI.use(new SoftUIStyleSheet(), ()->
            UI.of(this).groups("soft inwards")
            .add(SHRINK.and(ALIGN_LEFT),
                box()
                .withLayout(FILL.and(WRAP(3)).and(INS(32)), "", "[][]24[]24[]")
                .withPrefSize(800, 600)
                .add( SHRINK.and(SPAN).and(ALIGN_CENTER),
                    panel().groups("neumorphic")
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
                            double r = 30 * state.fadeIn();
                            double x = it.getEvent().getX() - r / 2.0;
                            double y = it.getEvent().getY() - r / 2.0;
                            it.paint( g -> {
                                g.setColor(new Color(1f, 1f, 0f, (float) state.fadeOut()));
                                g.fillOval((int) x, (int) y, (int) r, (int) r);
                            });
                        });
                    })
                )
                .add( SHRINK.and(SPAN).and(ALIGN_CENTER),
                    box().groups("soft inwards")
                    .add(
                        button("Click Me!").groups("neumorphic button").onClick(e -> {
                            System.out.println("Clicked!");
                        })
                    )
                    .add(
                        toggleButton("Toggle Me!").groups("neumorphic button")
                    )
                    .add(WRAP,
                        label(50,50,icon("img/trees.png")).groups("neumorphic")
                    )
                    .add(SPAN.and(GROW_X),
                        slider(Align.HORIZONTAL, 0, 255).groups("soft slim")
                    )
                )
            )
            .add(
                label(icon("img/swing.png"))
            )
        );
    }


    public static void main(String[] args) {
        UI.show(new SoftUIView());
    }
}
