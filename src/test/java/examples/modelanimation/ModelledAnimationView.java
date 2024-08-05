package examples.modelanimation;

import sprouts.Val;
import sprouts.Var;
import swingtree.UI;
import swingtree.animation.Stride;

import static swingtree.UI.*;

public class ModelledAnimationView extends Panel
{
    public ModelledAnimationView(Var<ModelledAnimationViewModel> vm) {
        Val<String> buttonText = vm.viewAsString(ModelledAnimationViewModel::buttonText);
        Val<Double> borderWidth = vm.viewAsDouble(ModelledAnimationViewModel::borderWidth);
        Val<Double> borderOpacity = vm.viewAsDouble(ModelledAnimationViewModel::borderOpacity);
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
                    .withRepaintOn(borderOpacity, borderWidth)
                    .withStyle( it -> it
                        .padding(26 - vm.get().borderWidth()/2)
                        .margin(42 - vm.get().borderWidth()/2)
                        .borderRadius( 38 )
                        .border(vm.get().borderWidth(), color(0.5,1,1, vm.get().borderOpacity()))
                        .backgroundColor(200/255d, 210/255d, 220/255d, 0.5 )
                        .shadow("bright", s -> s
                            .color(0.5, 1, 1, 0.5)
                            .offset(-6)
                        )
                        .shadow("dark", s -> s
                            .color(0, 0, 0, 0.5/4)
                            .offset(+6)
                        )
                        .shadowBlurRadius(5)
                        .shadowSpreadRadius(-2.5)
                        .shadowIsInset(false)
                        .gradient(Layer.BORDER, "border-gradient", grad -> grad
                            .span(Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                            .colors(
                                color(0.75, 1, 0.5, 0.5),
                                color(0.5, 1, 1, 0)
                            )
                            .clipTo(ComponentArea.BORDER)
                        )
                        .gradient(Layer.BACKGROUND, "content-gradient", grad -> grad
                            .type(GradientType.RADIAL)
                            .boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                            .offset(
                                it.component().getWidth()*0.5,
                                it.component().getHeight()*0.5
                            )
                            .colors(
                                color(0.75, 1, 0.5, 0.5),
                                color(0.5, 1, 1, 0)
                            )
                            .clipTo(ComponentArea.BODY)
                            .size(225)
                        )
                    )
                )
            )
            .add(CENTER,
                toggleButton(buttonText)
                .onClick( e ->
                    UI.animate(vm, ModelledAnimationViewModel::borderAnimation)
                )
            )
        )
        .add(GROW_X, label("opacity"))
        .add(GROW_X, progressBar(Align.HORIZONTAL, borderOpacity));
    }

    public static void main(String[] args) {
        Var<ModelledAnimationViewModel> vm = Var.of(new ModelledAnimationViewModel("Toggle Me", Stride.PROGRESSIVE, 0, 0));
        UI.show( f -> new ModelledAnimationView(vm) );
    }
}
