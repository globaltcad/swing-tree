package example;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;
import swingtree.animation.Animation;
import swingtree.animation.AnimationState;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

public class AnimatedButtonsView extends JPanel
{
    public AnimatedButtonsView() {
        FlatLightLaf.setup();
        of(this).withLayout(FILL.and(WRAP(3))).withPrefSize(800, 600)
        .add( SHRINK.and(SPAN).and(ALIGN_CENTER),
            label(
                "<html>" +
                    "<h1>Animated Buttons</h1>" +
                    "<p>" +
                    "A view of various button with lot's of animations." +
                    "</p>" +
                "</html>"
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
                .onMouseEnter(it -> {
                    it.animateOnce(0.5, TimeUnit.SECONDS, state -> {
                        // Let's increase the character spacing by max 10% of the cycle.
                        float spacing = (float) state.pulse() * 0.1f;
                        // We do this by changing th affine transform of the font.
                        Font f = it.getFont(); // Get the current font
                        AffineTransform at = it.getFont().getTransform();
                        at.setToScale(1 + spacing, 1);
                        it.setFont(f.deriveFont(at));
                    });
                })
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
                button("I spin when you hover over me").apply( ui -> {
                    Dimension prefSize = ui.getComponent().getPreferredSize();
                    ui.onMouseEnter(it -> it.animateTwice(0.3, TimeUnit.SECONDS, new Animation() {
                        @Override public void run(AnimationState state) {
                            double centerX = it.getWidth() / 2.0;
                            double phase = (state.pulse()*2 - 1);
                            int h = (int) ( prefSize.width * Math.abs(phase)/2 );
                            AffineTransform at = AffineTransform.getRotateInstance(phase * Math.PI/12, centerX, 0);
                            at.translate(0, h/12d);
                            Font f = it.getFont();
                            it.setFont(f.deriveFont(at));
                            it.setPrefSize(prefSize.width, h + prefSize.height);
                        }
                        @Override public void finish(AnimationState state) {
                            Font f = it.getFont();
                            AffineTransform at = AffineTransform.getRotateInstance(0, 0, 0);
                            it.setFont(f.deriveFont(at));
                            it.setPrefSize(prefSize);
                        }
                    }));
                })
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
                .apply( ui -> {
                    ui.onMouseEnter( it -> it.animateOnce(0.2, TimeUnit.SECONDS, state -> {
                        int bump = (int) (state.progress() * 15);
                        ui.withBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, bump));
                    }))
                    .onMouseExit( it -> it.animateOnce(0.2, TimeUnit.SECONDS, state -> {
                        int bump = (int) ((1-state.progress()) * 15);
                        ui.withBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, bump));
                    }));
                })
            )
        )
        .add(GROW.and(SPAN),
            button("I show a fancy explosion when you click me")
            .withPrefHeight(100)
            .onClick(it -> it.animateOnce(0.25, TimeUnit.SECONDS, state -> {
                Graphics g = it.getComponent().getGraphics();
                double progress = state.progress();
                double w = it.getWidth() * progress;
                double h = it.getHeight() * progress;
                double centerX = it.getWidth() / 2.0 - w / 2.0;
                double centerY = it.getHeight() / 2.0 - h / 2.0;
                g.setColor(new Color(1f, 1f, 0f, (float) (1-progress)));
                g.fillRect((int) centerX, (int) centerY, (int) w, (int) h);
            }))
        )
        .add(GROW.and(SPAN),
            button("I show many little mouse move explosions when you move your mouse over me")
            .withPrefHeight(100)
            .onMouseMove( it -> {
                it.animateOnce(1, TimeUnit.SECONDS, state -> {
                    int x = it.getEvent().getX();
                    int y = it.getEvent().getY();
                    double progress = state.progress();
                    double w = 30 * progress;
                    double h = 30 * progress;
                    double centerX = x - w / 2.0;
                    double centerY = y - h / 2.0;
                    SwingUtilities.invokeLater(() -> {
                    /*
                        We do this later because after the mouse move event
                        the button gets repainted, meaning that the explosions
                        will be erased. So we use invoke later to schedule the drawing
                        of the explosion to happen after the button is repainted.
                    */
                        Graphics g = it.getComponent().getGraphics();
                        g.setColor(new Color(1f, 1f, 0f, (float) (1 - progress)));
                        g.fillOval((int) centerX, (int) centerY, (int) w, (int) h);
                    });
                });
            })
            .onMouseClick(it -> it.animateOnce(2, TimeUnit.SECONDS, state -> {
                int x = it.getEvent().getX();
                int y = it.getEvent().getY();
                double progress = state.progress();
                double w = 300 * progress;
                double h = 300 * progress;
                double centerX = x - w / 2.0;
                double centerY = y - h / 2.0;
                SwingUtilities.invokeLater(() -> {
                    Graphics g = it.getComponent().getGraphics();
                    g.setColor(new Color(1f, 1f, 0f, (float) (1 - progress)));
                    g.fillOval((int) centerX, (int) centerY, (int) w, (int) h);
                });
            }))
        );
    }

    public static void main(String... args) { UI.show(new AnimatedButtonsView()); }
}
