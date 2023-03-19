package example;

import sprouts.Var;
import swingtree.UI;

import javax.swing.*;

import java.awt.*;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

public class AnimatedView extends JPanel
{
    private static final String TEXT = "This is a chaotic example of a view with lot's of components and animations.";

    public AnimatedView(JFrame frame)
    {
        Var<Integer> w = Var.of(300);

        of(this).withLayout(FILL.and(WRAP(3))).withPrefSize(800, 600)
        .add(SPAN,
            label(TEXT)
            .onMouseClick( it -> {
                it.animateOnce(10, TimeUnit.SECONDS, state -> {
                    JLabel label = it.getComponent();
                    double progress = Math.abs(state.progress() - 0.5) * 2;
                    // Let's make the text shift like a ring buffer based on the progress.
                    int shift = (int) (TEXT.length() * progress);
                    String text = TEXT.substring(shift) + TEXT.substring(0, shift);
                    label.setText(text);
                });
            })
        )
        .add( GROW, listOf("A", "B", "C", "D", "E") )
        .add( GROW,
            tabbedPane()
            .add( tab("This").add( textArea("This tab is about this") ) )
            .add( tab("is").add( textArea("This tab is about is") ) )
            .add( tab("an").add( textArea("This tab is about an") ) )
            .add( tab("example").add( textArea("This tab is about example") ) )
        )
        .add( GROW,
            panel(FILL.and(WRAP(1)))
            .add(
                comboBox("Rice", "Beans", "Corn", "Potatoes").withPrefWidth(w)
            )
            .add(
                spinner(1, 1, 10).withPrefWidth(w)
            )
            .add(
                splitButton("Split").withPrefWidth(w)
                .add(splitItem("Split 1"))
                .add(splitItem("Split 2"))
            )
            .add(
                button("Button")
                .onMouseEnter(it -> it.animateOnce(0.5, TimeUnit.SECONDS, state -> {
                    float highlight = 1f - (float) state.progress() * 0.5f;
                    it.getComponent().setBackground(new Color(highlight, 1, highlight));
                }))
                .onMouseExit( it -> it.animateOnce(0.5, TimeUnit.SECONDS, state -> {
                    float highlight = 0.5f + (float) state.progress() * 0.5f;
                    it.getComponent().setBackground(new Color(highlight, 1f, highlight));
                }))
            )
        )
        .add( GROW.and(SPAN),
            scrollPanels()
            .add( panel(FILL.and(WRAP(2))).add(label("A")).add(label("B")) )
            .add( panel(FILL.and(WRAP(2))).add(label("C")).add(label("D")) )
            .add( panel(FILL.and(WRAP(2))).add(label("E")).add(label("F")) )
            .add( panel(FILL.and(WRAP(2))).add(label("G")).add(label("H")) )
        );

        int screenCenterX = frame.getX();
        int screenCenterY = frame.getY();

        schedule(1, TimeUnit.SECONDS)
            .run( state -> { w.set((int) (100 * state.cycle())); } );

        schedule(6, TimeUnit.SECONDS)
            .startingIn(14, TimeUnit.SECONDS)
            .run( state -> {
                int MAX_WIDTH = 200;
                double progress = state.cycle();
                int width  = 300 + (int) (MAX_WIDTH * progress);
                int height = 300 + (int) (MAX_WIDTH * progress);
                int widthPosOffset  = (width / 2);
                int heightPosOffset = (height / 2);
                frame.setLocation(screenCenterX - widthPosOffset, screenCenterY - heightPosOffset);
                frame.setSize(width, height);
            });
    }

    public static void main( String... args ) { UI.show( f -> new AnimatedView(f) ); }
}
