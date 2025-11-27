package examples.animated;

import sprouts.Var;
import swingtree.UI;
import swingtree.animation.LifeTime;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

public final class TransitionalAnimation extends Panel
{
    private static final double THICKNESS = 5;

    private final Var<Boolean> isOn = Var.of(false);
    private final Var<Double>  progress = Var.of(0.0);


    public TransitionalAnimation() {
        UI.of(this).withLayout(WRAP(1), "[grow]", "[grow]")
        .add(CENTER,
            html("<h1 style=\"text-align: center;\">A Style Animation</h1>" +
                    "<p>...transitioning between 2 states...</p>")
        )
        .add(GROW,
            panel(FILL.and(WRAP(1))).withStyle(it->it.borderRadius(32).backgroundColor(Color.LIGHT_GRAY))
            .add(CENTER,
                box().add(
                    label("Toggle the switch to see the animation!")
                    .withTransitionalStyle(this.isOn, LifeTime.of(0.5, TimeUnit.SECONDS), (state,it) -> it
                        .padding(26 - (THICKNESS/2) * this.progress.set(state.progress()).get())
                        .margin(42 - (THICKNESS/2) * state.progress())
                        .borderRadius( 38 * state.progress() )
                        .border(THICKNESS * state.progress(), color(0.5,1,1))
                        .backgroundColor(200/255d, 210/255d, 220/255d, state.progress() )
                        .shadow("bright", s -> s
                            .color(0.5, 1, 1, state.progress())
                            .offset(-6)
                        )
                        .shadow("dark", s -> s
                            .color(0, 0, 0, state.progress()/4)
                            .offset(+6)
                        )
                        .shadowBlurRadius(10 * state.progress())
                        .shadowSpreadRadius(-5 * state.progress())
                        .shadowIsInset(false)
                        .gradient(Layer.BORDER, "border-gradient", grad -> grad
                            .span(Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                            .colors(
                                color(0.75, 1, 0.5, state.progress()),
                                color(0.5, 1, 1, 0)
                            )
                            .clipTo(ComponentArea.BORDER)
                        )
                        .gradient(Layer.BACKGROUND, "content-gradient", grad -> grad
                            .type(GradientType.RADIAL)
                            .boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                            .offset(
                                it.componentWidth()*state.progress(),
                                it.componentHeight()*state.progress()
                            )
                            .colors(
                                color(0.75, 1, 0.5, state.progress()),
                                color(0.5, 1, 1, 0)
                            )
                            .clipTo(ComponentArea.BODY)
                            .size(100+250*state.progress())
                        )
                    )
                )
            )
            .add(CENTER,
                toggleButton("toggle me").onClick( it -> isOn.set(it.get().isSelected()) )
            )
        )
        .add(GROW_X, progressBar(Align.HORIZONTAL, this.progress));
    }
    public static void main(String[] args) {
        UI.show( f -> new TransitionalAnimation() );
    }
}
