
# Animations and View Models #

> **Prerequisites:** This guide is the MVI-friendly continuation of
> [An Advanced Style Animation](./An-Advanced-Style-Animation.md) and assumes
> you already know how to bind components to a `Var<...>` view model — if not,
> read [Functional MVVM (MVI / MVL)](./Functional-MVVM.md) first.

Animations can become rather complicated. They may consist of multiple
stages as well as multiple elements that are animated at the same time.
All of this requires a lot of state management, which describes what the 
animation looks like at any given point in time and how it should change. <br>
When doing clean application design however, we want to separate
the looks of the application from the state and logic. <br>
So the way an application is presented to the user is defined by the *view*
and the views state and logic is defined by the *view model*.
This distinction should also be made when it comes to animations. 

But this is where writing them become a bit tricky because we have to
define the animation state and its update logic in the view model
and how this state should be continuously displayed in the view. <br>

In this guide we will show you the recommended way to write 
clean and robust animations in your application.

## Modelling Animations ##

For very advanced kind of animations you may want to define
dedicated model classes for your animations which are then
part of your view model. <br>
But most animations typically consist of only one or two
properties that are animated. In this guide we will examine a simple example
where we animate the opacity and border color of a view.

Take a look at following record based view model first.

```java
public record ModelledAnimationViewModel(
    String buttonText, Stride animationStride, double borderWidth, double borderOpacity
) {
    public Animatable<ModelledAnimationViewModel> borderAnimation() {
        return Animatable.of(LifeTime.of(3, TimeUnit.SECONDS), this, (status, model) -> {
            boolean isRunning = status.progress() % 1 != 0;
            double localProgress = this.animationStride.applyTo(status.progress());
            Stride nextStride = status.progress() == 1 ? this.animationStride.inverse() : this.animationStride;
            return withNewAnimationState( 
                        isRunning ? "running" : "not running", 
                        nextStride, 
                        localProgress * 10,
                        localProgress 
                    );
        });
    }
    
    public ModelledAnimationViewModel withNewAnimationState(
            String buttonText, Stride animationStride, double borderWidth, double borderOpacity
    ) {
        return new ModelledAnimationViewModel(buttonText, animationStride, borderWidth, borderOpacity);
    }
}
```

Above we model the state of a basic view as well as its animation. <br>
The interesting part in the above model is the `borderAnimation` method which returns an `Animatable` object. <br>
This object defines what it means to animate the `ModelledAnimationViewModel` class in
the form of a lambda function taking in the current `AnimationStatus` and model instance
as well as an initial model instance from which the animation should start. <br>
So it is primarily a wrapper for a pure function that is continuously called by the view
to update the model instance. <br>
The `Animatable` also stores the `LifeTime` of the animation,
which is used by the view to determine when the animation should start and stop. <br>

Now let's take a look at the view that uses this view model:

```java

import sprouts.Val;
import sprouts.Var;
import swingtree.UI;
import swingtree.animation.Stride;

import static swingtree.UI.*;

public class ModelledAnimationView extends Panel
{
    public ModelledAnimationView(Var<ModelledAnimationViewModel> vm) {
        Val<String> buttonText = vm.viewAsString(ModelledAnimationViewModel::buttonText);
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
                    .withStyle( vm, (m, it) -> it
                        .padding(26 - m.borderWidth()/2)
                        .margin(42 - m.borderWidth()/2)
                        .borderRadius( 38 )
                        .border(m.borderWidth(), color(0.5,1,1, m.borderOpacity()))
                        .backgroundColor(200/255d, 210/255d, 220/255d, 0.5 )
                        .shadow("bright", s -> s.color(0.5, 1, 1, 0.5).offset(-6) )
                        .shadow("dark", s -> s.color(0, 0, 0, 0.5/4).offset(+6) )
                        .shadowBlurRadius(5)
                        .shadowSpreadRadius(-2.5)
                        .shadowIsInset(false)
                        .gradient(Layer.BORDER, "border-gradient", grad -> grad
                            .span(Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                            .colors( color(0.75, 1, 0.5, 0.5), color(0.5, 1, 1, 0) )
                            .clipTo(ComponentArea.BORDER)
                        )
                        .gradient(Layer.BACKGROUND, "content-gradient", grad -> grad
                            .type(GradientType.RADIAL)
                            .boundary(ComponentBoundary.BORDER_TO_INTERIOR)
                            .offset(
                                it.component().getWidth()*0.5,
                                it.component().getHeight()*0.5
                            )
                            .colors( color(0.75, 1, 0.5, 0.5), color(0.5, 1, 1, 0) )
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
```

The view is a simple panel with a toggle button, progress bar
and most importantly a heavily styled and animated label. <br>

It should look like this when running `ModelledAnimationView.main`:


![](../img/tutorial/modelled-style-nimation.png)

In the source code of the view 
there are 4 important things to focus on
to understand how the animation works:

1. In the beginning we zoom to individual properties of the view model
   using the `viewAs...` methods. These properties are then used 
   to define the label text and its style.
2. The label is styled using the property bound `withStyle(vm, (m, it) -> ...)`
   method. It receives the current view model item `m` as an explicit argument
   and recalculates the style and repaints the label automatically whenever
   the view model changes — which happens continuously during the animation.
3. This property bound flavour of `withStyle` is the thread safe and preferred
   way to use property state in a style: the item is captured from the property
   change event on the property's owning thread and handed to the style lambda,
   which is evaluated by the UI thread. So the style never reads the property
   itself (as in the older `withRepaintOn(props) + withStyle(it -> vm.get()...)`
   pattern), which matters when your application uses the decoupled
   threading mode (`EventProcessor.DECOUPLED`), where properties are owned
   by the application thread.
4. And finally the place where the animation is triggered is the
   `onClick` method of the toggle button, which calls the expression
   `UI.animate(vm, ModelledAnimationViewModel::borderAnimation)`.
   This expression takes the `Animatable` object from the view model
   as well as the `vm` variable holding the mutable view model state
   and starts the animation by continuously invoking the `Animatable`
   object's lambda function and using its result to update the `vm` variable.

## Chaining multi-phase animations ##

The pattern above runs one animation to completion in response to a user
event. For something more elaborate — a multi-phase loop, for example — you
can chain `Animatable`s together by **listening for view-model phase
changes** and re-arming the next phase from inside the listener:

```java
Viewable.cast(phase).onChange(From.VIEW_MODEL, it -> {
    if ( vm.get().running() )
        UI.animate(vm, BreathingViewModel::breathAnimation);
});
```

Every time the model's `phase` field changes, the listener fires and starts
the next phase animation. Pausing the loop is simply "stop re-arming". This
pattern powers the
[BreathingView](../../src/test/java/examples/breathing/mvi/BreathingView.java)
example — a glowing orb that inhales, holds, exhales, rests, and repeats —
entirely driven by phase transitions in the immutable view model.

> **Watch out for garbage collection.** Sprouts lens and view properties
> observe their parent property only *weakly*. SwingTree's own component
> bindings (`label(..)`, `slider(..)`, `withRepaintOn(..)`, …) retain a
> strong reference internally, so the lenses you hand to them are safe.
> But a lens consumed *only* by a raw `Viewable.cast(prop).onChange(..)`
> subscription, like `phase` above, is **not** retained by SwingTree and
> must be held by you — typically as a field of the view. Without that
> strong reference, the re-arming listener would be silently collected and
> the animation would freeze after the first phase. The
> [BreathingView source](../../src/test/java/examples/breathing/mvi/BreathingView.java)
> documents exactly this pitfall.

## Where to next? ##

- [An Advanced Style Animation](./An-Advanced-Style-Animation.md) — the
  view-only variant using `withTransitionalStyle(..)` on a `Var<Boolean>`.
- [Functional MVVM (MVI / MVL)](./Functional-MVVM.md) — the architectural
  foundations the model-driven animation sits on.
- [Style Sheets and Groups](./Style-Sheets-And-Groups.md) — to share an
  animated style across many components rather than baking it into one
  view.

