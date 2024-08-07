package examples.animated;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;
import swingtree.animation.Animation;
import swingtree.animation.AnimationStatus;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

/**
 *  An advanced feature of SwingTree is the ability to animate any property of any component.
 *  In this example, we animate various buttons in various creative ways for you to get some inspiration from.
 */
public class AnimatedButtonsView extends Panel
{
    public AnimatedButtonsView() {
        FlatLightLaf.setup();
        of(this).withLayout(FILL.and(WRAP(3)).and(INS(32)), "", "[][]24[]24[]")
        .add( SHRINK.and(SPAN).and(ALIGN_CENTER),
            html(
                "<h1 style=\"text-align: center;\">Animated Buttons</h1>" +
                "<p>" +
                "A view of various button with lot's of animations." +
                "</p>"
            )
        )
        .add( GROW.and(PUSH_Y),
            panel(FILL.and(WRAP(1)))
            .add(
                button("I turn green when you hover over me")
                .onMouseEnter( it ->
                    it.animateFor(0.5, TimeUnit.SECONDS, status -> {
                        double highlight = 1 - status.progress() * 0.5;
                        it.setBackgroundColor(highlight, 1, highlight);
                    })
                )
                .onMouseExit( it ->
                    it.animateFor(0.5, TimeUnit.SECONDS, status -> {
                        double highlight = 0.5 + status.progress() * 0.5;
                        it.setBackgroundColor(highlight, 1f, highlight);
                    })
                )
            )
            .add(
                button("I blink when you hover over me")
                .onMouseEnter( it -> it
                    .animateFor(0.25, TimeUnit.SECONDS)
                    .asLongAs( status -> status.repeats() <= 1 )
                    .go(status -> {
                        float highlight = 1f - (float) status.pulse() * 0.5f;
                        it.setBackgroundColor(highlight, highlight, 1);
                    })
                )
            )
            .add(
                button("My text spreads when you hover over me")
                .onMouseEnter( it ->
                    it.animateFor(0.5, TimeUnit.SECONDS, status -> {
                        float spacing = (float) status.pulse() * 0.1f;
                        AffineTransform at = it.getFont().getTransform();
                        at.setToScale(1 + spacing, 1);
                        it.setFont(it.getFont().deriveFont(at));
                    })
                )
            )
            .add(
                button("I simply fade away when you hover over me").withBackground(Color.LIGHT_GRAY)
                .isBorderPaintedIf(false)
                .onMouseEnter(it -> it.animateFor(0.5, TimeUnit.SECONDS, status -> {
                    Color lg = Color.LIGHT_GRAY;
                    int alpha = (int) (255 * (1 - status.progress()));
                    it.setBackgroundColor(lg.getRed(), lg.getGreen(), lg.getBlue(), alpha);
                    it.setForegroundColor(0, 0, 0, alpha);
                }))
                .onMouseExit(it -> it.animateFor(0.5, TimeUnit.SECONDS, status -> {
                    Color lg = Color.LIGHT_GRAY;
                    int alpha = (int) (255 * status.progress());
                    it.setBackgroundColor(lg.getRed(), lg.getGreen(), lg.getBlue(), alpha);
                    it.setForegroundColor(0, 0, 0, alpha);
                }))
            )
            .add(
                button("I shake when you hover over me").apply( ui -> {
                    ui.onMouseEnter(it -> it
                        .animateFor(0.25, TimeUnit.SECONDS)
                        .asLongAs( status -> status.repeats() <= 1 )
                        .go(new Animation() {
                            final Dimension prefSize = it.get().getPreferredSize();
                            @Override public void run(AnimationStatus status) {
                                double centerX = it.getWidth() / 2.0;
                                double phase = (status.pulse()*2 - 1);
                                int h = (int) ( prefSize.width * Math.abs(phase) / 2 );
                                AffineTransform at = AffineTransform.getRotateInstance(phase * Math.PI/12, centerX, 0);
                                at.translate(0, h/12d);
                                it.setFont(it.getFont().deriveFont(at));
                                it.setPrefSize(prefSize.width, h + prefSize.height);
                            }
                            @Override public void finish(AnimationStatus status) {
                                AffineTransform at = AffineTransform.getRotateInstance(0, 0, 0);
                                it.setFont(it.getFont().deriveFont(at));
                                it.setPrefSize(prefSize);
                            }
                        })
                    );
                })
            )
            .add(
                button("I have a click ripple effect")
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
            )
        )
        .add( GROW.and(SPAN),
            panel(FILL.and(WRAP(1)))
            .add(
                button("I pop when you hover over me").apply( ui -> {
                    Dimension prefSize = ui.get(JButton.class).getPreferredSize();
                    ui.onMouseEnter(it -> it.animateFor(0.2, TimeUnit.SECONDS, status -> {
                        int bump = (int) (status.pulse() * 15);
                        it.setPrefSize(prefSize.width + bump, prefSize.height + bump);
                      }));
                })
            )
            .add(
                button("I swell when you hover over me").apply( ui -> {
                    Dimension prefSize = ui.get(JButton.class).getPreferredSize();
                    ui.onMouseEnter( it -> it.animateFor(0.2, TimeUnit.SECONDS, status -> {
                        int bump = (int) (status.progress() * 15);
                        it.setPrefSize(prefSize.width + bump, prefSize.height + bump);
                      }))
                      .onMouseExit( it -> it.animateFor(0.2, TimeUnit.SECONDS, status -> {
                          int bump = (int) ((1-status.progress()) * 15);
                          it.setPrefSize(prefSize.width + bump, prefSize.height + bump);
                      }));
                })
            )
            .add(
                button("I shrink when you hover hover me")
                .apply( ui -> {
                    Dimension prefSize = ui.get(JButton.class).getPreferredSize();
                    ui.withPrefSize(prefSize.width + 15, prefSize.height + 15);
                    ui.onMouseEnter( it -> it.animateFor(0.2, TimeUnit.SECONDS, status -> {
                        int bump = (int) ((1-status.progress()) * 15);
                        it.setPrefSize(prefSize.width + bump, prefSize.height + bump);
                      }))
                      .onMouseExit( it -> it.animateFor(0.2, TimeUnit.SECONDS, status -> {
                          int bump = (int) (status.progress() * 15);
                          it.setPrefSize(prefSize.width + bump, prefSize.height + bump);
                      }));
                })
            )
            .add(
                button("My text grows when you hover over me")
                .apply( ui -> {
                    Font f = ui.get(JButton.class).getFont();
                    ui.onMouseEnter( it -> it.animateFor(0.2, TimeUnit.SECONDS, status -> {
                        float scale = (float) (1 + status.progress() * 0.5);
                        AffineTransform at = AffineTransform.getScaleInstance(scale, scale);
                        it.setFont(f.deriveFont(at));
                    }))
                    .onMouseExit( it -> it.animateFor(0.2, TimeUnit.SECONDS, status -> {
                        float scale = (float) (1 + (1-status.progress()) * 0.5);
                        AffineTransform at = AffineTransform.getScaleInstance(scale, scale);
                        it.setFont(f.deriveFont(at));
                    }));
                })
            )
            .add(
                button("My border grows when you hover over me").withLineBorder(Color.LIGHT_GRAY, 0)
                .withPrefSize(300, 40)
                .apply( ui ->
                    ui.onMouseEnter( it -> it.animateFor(0.2, TimeUnit.SECONDS, status -> {
                        int bump = (int) (status.progress() * 15);
                        it.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, bump));
                    }))
                    .onMouseExit( it -> it.animateFor(0.2, TimeUnit.SECONDS, status -> {
                        int bump = (int) ((1-status.progress()) * 15);
                        it.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, bump));
                    }))
                )
            )
        )
        .add(GROW.and(SPAN),
            button("I show a fancy explosion when you click me")
            .withPrefHeight(100)
            .onClick( it -> it.animateFor(0.25, TimeUnit.SECONDS, status -> {
                double w = it.getWidth()  * status.progress();
                double h = it.getHeight() * status.progress();
                double x = it.getWidth()  / 2.0 - w / 2.0;
                double y = it.getHeight() / 2.0 - h / 2.0;
                it.paint(status, g -> {
                    g.setColor(new Color(1f, 1f, 0f, (float) status.fadeOut()));
                    g.fillRect((int) x, (int) y, (int) w, (int) h);
                });
            }))
            .onMouseEnter( it -> it.animateFor(1, TimeUnit.SECONDS, status -> {
                it.style(status, style ->
                    style.borderWidth((int)(10 * status.cycle()))
                         .borderColor(new Color(1f, 1f, 0f, (float) (1 - status.cycle())))
                         .borderRadius((int)(100 * status.cycle()))
                         .foundationColor(new Color(1f,1f,1f,0f))
                );
            }))
        )
        .add(GROW.and(SPAN),
            button("I show many little mouse move explosions when you move your mouse over me")
            .withPrefHeight(100)
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
            .onMouseClick( it -> it.animateFor(2, TimeUnit.SECONDS, status -> {
                double r = 300 * status.fadeIn() * it.getScale();
                double x = it.mouseX() - r / 2;
                double y = it.mouseY() - r / 2;
                it.paint(status, g -> {
                    g.setColor(new Color(1f, 1f, 0f, (float) status.fadeOut()));
                    g.fillOval((int) x, (int) y, (int) r, (int) r);
                });
            }))
        );
    }

    public static void main(String... args) { UI.show(f->new AnimatedButtonsView()); }
}
