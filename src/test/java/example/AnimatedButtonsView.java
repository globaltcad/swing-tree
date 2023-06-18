package example;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;
import swingtree.animation.Animation;
import swingtree.animation.AnimationState;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

public class AnimatedButtonsView extends Panel
{
    public AnimatedButtonsView() {
        FlatLightLaf.setup();
        of(this).withLayout(FILL.and(WRAP(3)).and(INS(32)), "", "[][]24[]24[]")
        .add( SHRINK.and(SPAN).and(ALIGN_CENTER),
            html(
                "<h1>Animated Buttons</h1>" +
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
                    it.animateOnce(0.5, TimeUnit.SECONDS, state -> {
                        double highlight = 1 - state.progress() * 0.5;
                        it.setBackgroundColor(highlight, 1, highlight);
                    })
                )
                .onMouseExit( it ->
                    it.animateOnce(0.5, TimeUnit.SECONDS, state -> {
                        double highlight = 0.5 + state.progress() * 0.5;
                        it.setBackgroundColor(highlight, 1f, highlight);
                    })
                )
            )
            .add(
                button("I blink when you hover over me")
                .onMouseEnter(it -> it.animateTwice(0.25, TimeUnit.SECONDS, state -> {
                    float highlight = 1f - (float) state.pulse() * 0.5f;
                    it.setBackgroundColor(highlight, highlight, 1);
                }))
            )
            .add(
                button("My text spreads when you hover over me")
                .onMouseEnter( it ->
                    it.animateOnce(0.5, TimeUnit.SECONDS, state -> {
                        float spacing = (float) state.pulse() * 0.1f;
                        AffineTransform at = it.getFont().getTransform();
                        at.setToScale(1 + spacing, 1);
                        it.setFont(it.getFont().deriveFont(at));
                    })
                )
            )
            .add(
                button("I simply fade away when you hover over me").withBackground(Color.LIGHT_GRAY)
                .isBorderPaintedIf(false)
                .onMouseEnter(it -> it.animateOnce(0.5, TimeUnit.SECONDS, state -> {
                    Color lg = Color.LIGHT_GRAY;
                    int alpha = (int) (255 * (1 - state.progress()));
                    it.setBackgroundColor(lg.getRed(), lg.getGreen(), lg.getBlue(), alpha);
                    it.setForegroundColor(0, 0, 0, alpha);
                }))
                .onMouseExit(it -> it.animateOnce(0.5, TimeUnit.SECONDS, state -> {
                    Color lg = Color.LIGHT_GRAY;
                    int alpha = (int) (255 * state.progress());
                    it.setBackgroundColor(lg.getRed(), lg.getGreen(), lg.getBlue(), alpha);
                    it.setForegroundColor(0, 0, 0, alpha);
                }))
            )
            .add(
                button("I shake when you hover over me").apply( ui -> {
                    Dimension prefSize = ui.getComponent().getPreferredSize();
                    ui.onMouseEnter(it -> it.animateTwice(0.3, TimeUnit.SECONDS, new Animation() {
                        @Override public void run(AnimationState state) {
                            double centerX = it.getWidth() / 2.0;
                            double phase = (state.pulse()*2 - 1);
                            int h = (int) ( prefSize.width * Math.abs(phase) / 2 );
                            AffineTransform at = AffineTransform.getRotateInstance(phase * Math.PI/12, centerX, 0);
                            at.translate(0, h/12d);
                            it.setFont(it.getFont().deriveFont(at));
                            it.setPrefSize(prefSize.width, h + prefSize.height);
                        }
                        @Override public void finish(AnimationState state) {
                            AffineTransform at = AffineTransform.getRotateInstance(0, 0, 0);
                            it.setFont(it.getFont().deriveFont(at));
                            it.setPrefSize(prefSize);
                        }
                    }));
                })
            )
            .add(
                button("I have a click ripple effect")
                .onMouseClick( it -> it.animateOnce(2, TimeUnit.SECONDS, state -> {
                    it.paint( g -> {
                        g.setColor(new Color(0.1f, 0.25f, 0.5f, (float) state.fadeOut()));
                        for ( int i = 0; i < 5; i++ ) {
                            double r = 300 * state.fadeIn() * ( 1 - i * 0.2 );
                            double x = it.getEvent().getX() - r / 2;
                            double y = it.getEvent().getY() - r / 2;
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
                    Dimension prefSize = ui.getComponent().getPreferredSize();
                    ui.onMouseEnter(it -> it.animateOnce(0.2, TimeUnit.SECONDS, state -> {
                        int bump = (int) (state.pulse() * 15);
                        it.setPrefSize(prefSize.width + bump, prefSize.height + bump);
                      }));
                })
            )
            .add(
                button("I swell when you hover over me").apply( ui -> {
                    Dimension prefSize = ui.getComponent().getPreferredSize();
                    ui.onMouseEnter( it -> it.animateOnce(0.2, TimeUnit.SECONDS, state -> {
                        int bump = (int) (state.progress() * 15);
                        it.setPrefSize(prefSize.width + bump, prefSize.height + bump);
                      }))
                      .onMouseExit( it -> it.animateOnce(0.2, TimeUnit.SECONDS, state -> {
                          int bump = (int) ((1-state.progress()) * 15);
                          it.setPrefSize(prefSize.width + bump, prefSize.height + bump);
                      }));
                })
            )
            .add(
                button("I shrink when you hover over me")
                .apply( ui -> {
                    Dimension prefSize = ui.getComponent().getPreferredSize();
                    ui.withPrefSize(prefSize.width + 15, prefSize.height + 15);
                    ui.onMouseEnter( it -> it.animateOnce(0.2, TimeUnit.SECONDS, state -> {
                        int bump = (int) ((1-state.progress()) * 15);
                        it.setPrefSize(prefSize.width + bump, prefSize.height + bump);
                      }))
                      .onMouseExit( it -> it.animateOnce(0.2, TimeUnit.SECONDS, state -> {
                          int bump = (int) (state.progress() * 15);
                          it.setPrefSize(prefSize.width + bump, prefSize.height + bump);
                      }));
                })
            )
            .add(
                button("My text grows when you hover over me")
                .apply( ui -> {
                    Font f = ui.getComponent().getFont();
                    ui.onMouseEnter( it -> it.animateOnce(0.2, TimeUnit.SECONDS, state -> {
                        float scale = (float) (1 + state.progress() * 0.5);
                        AffineTransform at = AffineTransform.getScaleInstance(scale, scale);
                        ui.withFont(f.deriveFont(at));
                    }))
                    .onMouseExit( it -> it.animateOnce(0.2, TimeUnit.SECONDS, state -> {
                        float scale = (float) (1 + (1-state.progress()) * 0.5);
                        AffineTransform at = AffineTransform.getScaleInstance(scale, scale);
                        ui.withFont(f.deriveFont(at));
                    }));
                })
            )
            .add(
                button("My border grows when you hover over me").withLineBorder(Color.LIGHT_GRAY, 0)
                .withPrefSize(300, 40)
                .apply( ui ->
                    ui.onMouseEnter( it -> it.animateOnce(0.2, TimeUnit.SECONDS, state -> {
                        int bump = (int) (state.progress() * 15);
                        ui.withBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, bump));
                    }))
                    .onMouseExit( it -> it.animateOnce(0.2, TimeUnit.SECONDS, state -> {
                        int bump = (int) ((1-state.progress()) * 15);
                        ui.withBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, bump));
                    }))
                )
            )
        )
        .add(GROW.and(SPAN),
            button("I show a fancy explosion when you click me")
            .withPrefHeight(100)
            .onClick( it -> it.animateOnce(0.25, TimeUnit.SECONDS, state -> {
                double w = it.getWidth()  * state.progress();
                double h = it.getHeight() * state.progress();
                double x = it.getWidth()  / 2.0 - w / 2.0;
                double y = it.getHeight() / 2.0 - h / 2.0;
                it.paint( g -> {
                    g.setColor(new Color(1f, 1f, 0f, (float) state.fadeOut()));
                    g.fillRect((int) x, (int) y, (int) w, (int) h);
                });
            }))
        )
        .add(GROW.and(SPAN),
            button("I show many little mouse move explosions when you move your mouse over me")
            .withPrefHeight(100)
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
            .onMouseClick( it -> it.animateOnce(2, TimeUnit.SECONDS, state -> {
                double r = 300 * state.fadeIn();
                double x = it.getEvent().getX() - r / 2;
                double y = it.getEvent().getY() - r / 2;
                it.paint( g -> {
                    g.setColor(new Color(1f, 1f, 0f, (float) state.fadeOut()));
                    g.fillOval((int) x, (int) y, (int) r, (int) r);
                });
            }))
        );
    }

    public static void main(String... args) { UI.show(new AnimatedButtonsView()); }
}
