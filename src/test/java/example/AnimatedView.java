package example;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.Var;
import swingtree.UI;

import javax.swing.*;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

public class AnimatedView extends Panel
{
    private static final String TEXT = "This is a chaotic example of a view with lot's of components and animations.";

    public AnimatedView(JFrame frame)
    {
        Var<Integer> w = Var.of(300);
        FlatLightLaf.setup();
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
            .add(SPAN,
                box(FILL).peek( b -> b.setLayout(null) ).withPrefSize(620,40)
                .add(GROW,
                    toggleButton("Toggle").peek(
                         b -> UI.runLater(()-> b.setBounds(0, 0, 100, 30))
                    )
                    .onMouseClick( it -> {
                        // When it is selected we start an animation that will
                        // move the button to the right end of the parent panel.
                        if ( it.get().isSelected() )
                            it.animateOnce(1, TimeUnit.SECONDS, state -> {
                                int x = (int) (state.progress() * (it.getParent().getWidth() - it.getWidth()));
                                it.getComponent().setBounds(x, 0, 100, 30);
                            });
                        // When it is deselected we start an animation that will
                        // move the button to the left end of the parent panel.
                        else
                            it.animateOnce(1, TimeUnit.SECONDS, state -> {
                                int x = (int) ((1 - state.progress()) * (it.getParent().getWidth() - it.getWidth()));
                                it.getComponent().setBounds(x, 0, 100, 30);
                            });
                    })
                )
            )
        )
        .add( GROW.and(SPAN),
            scrollPanels()
            .add( panel(FILL.and(WRAP(2))).add(label("A")).add(label("B")) )
            .add( panel(FILL.and(WRAP(2))).add(label("C")).add(label("D")) )
            .add( panel(FILL.and(WRAP(2))).add(label("E")).add(label("F")) )
            .add( panel(FILL.and(WRAP(2))).add(label("G")).add(label("H")) )
        );

        schedule(1, TimeUnit.SECONDS)
            .go(state -> { w.set((int) (100 * state.cycle())); } );

    }

    public static void main( String... args ) { UI.show( f -> new AnimatedView(f) ); }
}
