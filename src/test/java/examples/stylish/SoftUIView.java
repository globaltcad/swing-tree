package examples.stylish;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

public class SoftUIView extends Panel
{
    public SoftUIView() {
        FlatLightLaf.setup();
        UI.use(new SoftUIStyleSheet(), ()->
            UI.of(this).group(Soft.SINK_ROOMY)
            .add(SHRINK_Y.and(ALIGN_LEFT),
                box().group(Soft.RAISE)
                .withLayout(FILL.and(WRAP(3)).and(INS(32)), "", "[][]24[]24[]")
                .add( SHRINK.and(SPAN).and(ALIGN_CENTER),
                    panel().group(Soft.BANNER)
                    .add(
                        html(
                            "<h1>Soft UI In Swing O.o</h1>" +
                            "<p>" +
                            "See what you can do with this library! " +
                            "</p>"
                        )
                    )
                    .onMouseMove( it -> {
                        it.animateFor(1, TimeUnit.SECONDS, status -> {
                            double r = 30 * status.fadeIn() * it.getScale();
                            double x = it.mouseX() - r / 2.0;
                            double y = it.mouseY() - r / 2.0;
                            it.paint(status, g -> {
                                g.setColor(new Color(1f, 1f, 0f, (float) status.fadeOut()));
                                g.fillOval((int) x, (int) y, (int) r, (int) r);
                            });
                        });
                    })
                    .add(
                        icon(50,50, "img/trees.png").group(Soft.BANNER)
                    )
                )
                .add( SHRINK.and(SPAN).and(ALIGN_CENTER),
                    box().group(Soft.SINK_ROOMY)
                    .add(
                        slider(Align.VERTICAL, 0, 255).group(Soft.SLIM)
                    )
                    .add(WRAP,
                        box(FILL.and(WRAP(2)))
                        .add(
                            box(FILL.and(GAP_REL(0)))
                            .add(GROW_X,
                                button("Click Me!").group(Soft.BUTTON)
                            )
                            .add(GROW_X.and(WRAP),
                                toggleButton("Toggle Me!").group(Soft.BUTTON)
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
            .add(GROW.and(PUSH_Y),
                box(WRAP(1).and(FILL))
                .add(
                    icon(256, 256, "img/swing.png")
                    .withStyle( it -> it.padding(24) )
                )
                .add(SHRINK.and(CENTER), html("<h2>Progress</h2>").withMaxHeight(30))
                .add(GROW.and(PUSH_Y),
                    scrollPanels().group(Soft.SINK_SLIM)
                    .withMinHeight(262)
                    .apply( ui -> {
                        for ( int i = 0; i < 16; i++ )
                            ui.add(
                                box().withStyle( it -> it
                                    .margin(3)
                                    .padding(3)
                                    //.border(3, Color.RED)
                                    //.backgroundColor(Color.ORANGE)
                                )
                                .add(
                                    label("Step " + (i+1))
                                    .withBackground(new Color(0,0,0,0))
                                    .withStyle( it -> it
                                        .fontFamily("Dancing Script")
                                        .fontSize(12)
                                        .fontColor(new Color(14, 90, 140))
                                        .fontWeight(2f)
                                    )
                                )
                                .add(
                                    slider(Align.HORIZONTAL, 0, 100).group(Soft.SLIM)
                                    .withValue(25+(int) Math.abs(Math.pow(91,i+7)%51))
                                )
                                .add(
                                    toggleButton(11,11,findIcon("img/two-16th-notes.svg").get())
                                    .group(Soft.BUTTON)
                                )
                            );
                    })
                )
            )
            .onMouseClick( it -> it.animateFor(2, TimeUnit.SECONDS, status -> {
                it.paint(status, g -> {
                    g.setColor(new Color(0.1f, 0.25f, 0.5f, (float) status.fadeOut()));
                    for ( int i = 0; i < 5; i++ ) {
                        double r = 300 * status.fadeIn() * ( 1 - i * 0.2 ) * it.getScale();
                        double x = it.mouseX() - r / 2;
                        double y = it.mouseY() - r / 2;
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
