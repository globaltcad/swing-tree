# Slider Ticks and Labels #

A slider on its own is a little shy. It lets your users pick a number, but it
doesn't tell them much about that number: is the knob halfway to 100, or halfway
to 1? Tick marks and labels answer that question. They turn a bare track into
something your users can read at a glance, much like the markings on a ruler.

With plain Swing, you describe those markings through half a dozen setters on
`JSlider`: `setMajorTickSpacing`, `setMinorTickSpacing`, `setPaintTicks`,
`setPaintLabels`, `setLabelTable` and `setSnapToTicks`. All of them speak in whole
numbers, you have to remember to switch the painting on separately, and, as you
will see further down, a few of them quietly undo each other.

In SwingTree, you describe all of it with **one immutable value**, a `SliderTicks`,
and hand that value to your slider. This guide walks you through it step by step,
starting with a simple volume slider and adding one idea at a time.

---

## Your first labelled slider ##

Let's say you have a `volume` property holding a number from 0 to 100. You would
like a major tick mark every 25, a few smaller tick marks in between, and a
percentage under every major tick mark. This is how you write that:

```java
UI.slider(UI.Axis.HORIZONTAL, 0, 100, volume)
.withTicks(
    SliderTicks.of(Integer.class)
    .withMajorSpacing(25)
    .withMinorTicksBetween(4)
    .withLabelsAtMajorTicks( v -> v + "%" )
);
```

Let's read it line by line:

1. `SliderTicks.of(Integer.class)` gives you tick marks and labels for a slider of
   `Integer`s, and so far there are none of either. Every `SliderTicks` starts out
   like this.
2. `withMajorSpacing(25)` adds a major tick mark every 25, which puts them at 0, 25,
   50, 75 and 100.
3. `withMinorTicksBetween(4)` adds four smaller tick marks between each pair of major
   ones. Four tick marks divide a gap of 25 into five equal parts, so you get a minor
   tick mark at every multiple of 5.
4. `withLabelsAtMajorTicks(v -> v + "%")` adds a label under every major tick mark.
   Your function receives the number at the tick mark and returns the text of its
   label, so the labels read "0%", "25%", "50%", "75%" and "100%".

Notice what you did *not* have to write: you never switched the painting of tick
marks or labels on. Your slider simply draws what your `SliderTicks` describes.

Also notice that every one of those `with..` calls gives you back a `SliderTicks`
with the change applied, and never modifies the one you called it on. That is what
makes it a value, just like a `String` or a Sprouts `Tuple`. You can store it in a
constant, compare two of them with `equals`, and pass one around without worrying
that someone changes it under your feet.

---

## Why you count minor tick marks instead of spacing them ##

You might wonder why you say *how many* minor tick marks you want, rather than how far
apart they are, which is what `JSlider.setMinorTickSpacing(..)` asks you for.

A spacing lets you make a mistake which nobody warns you about. Give a `JSlider` a major
spacing of 25 and a minor spacing of 10, and its minor tick marks sit at 0, 10, 20, 30 and
so on, while its major tick marks sit at 0, 25 and 50. The small marks no longer divide
the gaps between the big ones evenly, and your slider looks slightly broken. When you count
the tick marks between the major ones instead, they always divide the gap evenly, so this
mistake simply cannot happen.

There is one thing to keep in mind on a slider for whole numbers: the gap between two major
tick marks still has to divide into whole numbers. Three minor tick marks between major tick
marks 25 apart would need a tick mark every 6.25, and a slider of `Integer`s has no 6.25.
Rather than drawing tick marks in the wrong places, SwingTree then draws no minor tick marks
at all, and logs a warning which tells you why.

---

## Sliders for fractional numbers ##

Now let's build an opacity slider for a `Var<Double>` running from 0.0 to 1.0:

```java
UI.slider(UI.Axis.HORIZONTAL, 0.0, 1.0, opacity)
.withTicks(
    SliderTicks.of(Double.class)
    .withMajorSpacing(0.25)
    .withLabelsAtMajorTicks()
);
```

Two things are different from the volume slider.

First, you asked for `SliderTicks.of(Double.class)`, and the spacing is `0.25`. Every
spacing and every label position in a `SliderTicks` is a number of the same type as the
value of your slider. The slider builder knows that type as well: this one is a
`UIForSlider<JSlider, Double>`, so if you accidentally hand it a `SliderTicks<Integer>`,
your code does not compile.

Second, there is no text function this time, so the labels show their numbers: "0.00",
"0.25", "0.50", "0.75" and "1.00". SwingTree writes all number labels of a slider with the
same count of decimal places, namely the fewest which write every one of them exactly.
That is why you read "0.50", and not "0.5" standing next to "0.25".

You might be surprised that this works at all, because a `JSlider` only knows whole numbers.
Behind the scenes, SwingTree maps the fractional numbers of your slider onto a range of whole
numbers, and it picks that range so that every tick mark lands exactly on one of them. You
never see those whole numbers, and you never have to convert anything yourself.

### No floating point surprises ###

Here is a small Java puzzle for you: what is `0.1 + 0.1 + 0.1`? It is not `0.3`. It is
`0.30000000000000004`, because a `double` cannot hold the number 0.1 exactly, and the tiny
errors add up.

Now imagine a slider which finds its tick marks by adding the spacing to the minimum, again
and again. On a slider from 0.0 with a spacing of 0.1, that is exactly the number it would
hand to your label function for its fourth tick mark. SwingTree does not do that. It computes
the number at a tick mark as the minimum of your slider plus a whole multiple of the spacing,
in decimal arithmetic. So your label function receives exactly `0.3`, and a slider which snaps
to its tick marks writes exactly `0.3` into your property.

---

## Where the tick marks start ##

Tick marks always count from the minimum of your slider. On a slider from 3 to 97 with a
major spacing of 25, the major tick marks sit at 3, 28, 53 and 78, and not at 25, 50 and 75.

This is not a choice SwingTree makes for you. `BasicSliderUI`, which the look and feels of
Swing and FlatLaf build on, draws tick marks by starting at the minimum and stepping forward.
The labels at the major tick marks follow the tick marks, because a label is only useful where
its tick mark is.

If you would like a label at 50 on such a slider anyway, you can put it there yourself.
That is what the next section is about.

---

## Labels with words and icons ##

Not every slider wants numbers under it. A temperature slider, for example, is much friendlier
with words:

```java
UI.slider(UI.Axis.HORIZONTAL, 0, 100, temperature)
.withTicks(
    SliderTicks.of(Integer.class)
    .withLabelAt(0,   "Cold")
    .withLabelAt(50,  "Warm")
    .withLabelAt(100, "Hot")
);
```

With `withLabelAt(..)` you can place a label at any number you like, and there does not need
to be a tick mark at that number. If you place a label outside the range of your slider, it
is simply not shown.

A label can also be an icon. You pass the same `IconDeclaration` you use everywhere else in
SwingTree, which means your view model can hold the icon without ever loading an image:

```java
SliderTicks.of(Integer.class)
.withLabelAt(0,   Icons.QUIET)
.withLabelAt(100, Icons.LOUD)
```

You can also combine both kinds of labels. When a label you placed with `withLabelAt(..)` lands
on the same spot as a label at a major tick mark, your label wins. That gives you a neat way to
rename a single tick mark:

```java
SliderTicks.of(Integer.class)
.withMajorSpacing(50)
.withLabelsAtMajorTicks()
.withLabelAt(100, "Done")      // the labels read "0", "50" and "Done"
```

---

## Tick marks you don't draw ##

Many flat designs want the numbers under a slider, but no marks on its track. You get that
with `withTickMarksVisible(false)`:

```java
SliderTicks.of(Integer.class)
.withMajorSpacing(25)
.withTickMarksVisible(false)
.withLabelsAtMajorTicks()
```

Hidden tick marks still exist, you just don't see them. The labels are still placed at them,
the knob can still snap to them, and when your users click into the track beside the knob, it
still moves by the distance between two neighbouring tick marks.

That makes hidden tick marks surprisingly handy. Combine them with snapping, which comes next,
and you have given your slider a step size without drawing a single mark.

---

## Letting the knob snap ##

Sometimes only a few numbers make sense. A breathing exercise may want its timings in half
seconds, and a budget planner its amounts in steps of €250. In cases like these you let the
knob snap to the tick marks:

```java
SliderTicks.of(Integer.class)
.withMajorSpacing(25)
.withSnapToTicks(true)
```

This is what your users experience with such a slider:

- While they drag the knob, the value of the slider is the number at the nearest tick mark.
- When they let go of the knob, it moves onto that tick mark.
- When they press an arrow key, the knob jumps to the next tick mark in that direction.

If you declared minor tick marks, the knob snaps to those, and otherwise it snaps to the major ones.

There is one more rule, and it is an important one: **snapping only applies to what your users
do.** When your application sets the value to a number between two tick marks, say 33 on a
slider with tick marks every 25, the knob sits at 33, between the tick marks, and your property
keeps exactly the 33 you set.

That is a deliberate difference to a plain `JSlider`, whose `setSnapToTicks(true)` moves the
knob onto a tick mark no matter who set the value. Set such a slider to 33, and the knob jumps
to 25 while your property still says 33, so your view and your model now disagree about the
number. Change the tick spacing to 20 afterwards, and the slider snaps again, this time to 20,
and it reports that change. A binding then writes 20 into your model, even though nobody touched
the knob. With SwingTree, neither of these things happens.

---

## Numbers in your users' language ##

Out of the box, number labels are written the way Java source code writes numbers: with a dot
as decimal separator, and without grouping the thousands. A label at 1234.5 reads "1234.5" on
every machine in the world, which also keeps your tests independent of the machine they run on.

If your application should follow the conventions of a particular locale, you tell your
`SliderTicks` so:

```java
SliderTicks.of(Double.class)
.withMajorSpacing(1234.5)
.withLabelsAtMajorTicks()
.withLabelLocale(Locale.GERMANY)      // the labels read "0,0", "1.234,5" and "2.469,0"
```

To follow the locale your users' machines are set to, pass `Locale.getDefault()`.

The locale only applies to labels which show their numbers. If you pass your own text function
to `withLabelsAtMajorTicks(..)`, the formatting is up to you, and the locale leaves your text alone.

---

## Changing tick marks and labels while your app runs ##

So far, every slider in this guide got a fixed `SliderTicks`. But `withTicks` also accepts a
property, and your slider then follows every change of it. Here is a slider which shows its labels
only while a setting in your view model says so:

```java
Var<Boolean> showLabels = vm.showLabels();

SliderTicks<Integer> plain    = SliderTicks.of(Integer.class).withMajorSpacing(25);
SliderTicks<Integer> labelled = plain.withLabelsAtMajorTicks();

UI.slider(UI.Axis.HORIZONTAL, 0, 100, volume)
.withTicks( showLabels.viewAs(SliderTicks.classTyped(Integer.class), show -> show ? labelled : plain) );
```

You might wonder what `SliderTicks.classTyped(Integer.class)` is doing there. `SliderTicks` is
generic in the number type of your slider, so if you wrote `SliderTicks.class` instead, `viewAs`
would give you a property of the raw type `SliderTicks`, and `withTicks(..)` would refuse it with a
compile error. `classTyped(..)` returns the very same class, but typed as
`Class<SliderTicks<Integer>>`. If you know `Tuple.classTyped(..)` from Sprouts, this is the same trick.

Where should such a property live? You are free to keep a `SliderTicks` in your view model. Quite
often, though, your code reads nicer when your view model holds plain data, like *whether* labels
are shown or *which chapters* a video has, and your view turns that data into tick marks and
labels, just like the example above does.

The range of your slider may change too, if you bind its minimum and maximum through
`slider(Axis, Val<N>, Val<N>, Var<N>)`. Whenever it does, the labels at the major tick marks are
laid out again for the new range, and the labels you placed with `withLabelAt(..)` stay exactly
where you put them.

---

## Making the labels look right ##

You never add the labels of a slider to a panel yourself, so you cannot style them one by one.
Instead, they take the font and the foreground colour of their slider. In other words, you style
the labels by styling the slider:

```java
UI.slider(UI.Axis.HORIZONTAL, 0, 100, volume)
.withTicks( SliderTicks.of(Integer.class).withMajorSpacing(25).withLabelsAtMajorTicks() )
.withStyle( it -> it.componentFont( f -> f.size(11) ) );
```

Be a little careful with the foreground colour, though, because some look and feels paint parts
of the slider itself in it. FlatLaf, for example, paints the knob and the filled part of the track
in the foreground colour of the slider. So if you make the foreground grey to get grey labels, you
get a grey knob as well.

And please give a labelled slider some room. Swing centres every label under its tick mark and
never thins out labels which overlap, so a narrow slider with a label every 1000 turns its labels
into an unreadable smear. If that happens to you, label fewer tick marks, or give the slider a row
of its own. That is exactly what `BudgetView` does with its budget slider.

---

## A word about threads ##

If your application uses `EventProcessor.DECOUPLED`, your properties belong to the application
thread, while your slider lives on the UI thread. A `SliderTicks` holds no Swing component, so you
can safely create it on the application thread, keep it in your view model, and let it travel to
the UI thread. A new `SliderTicks` reaches your slider as part of its property's change event, in
the same order as every other change to that slider.

The number your users pick travels the other way, from the UI thread to the application thread,
which writes it into your value property. At that moment, SwingTree also keeps the number within
your current minimum and maximum properties. To see why that matters, picture your users dragging
the knob to the end of the slider in the very moment your application lowers the maximum. Without
that last check, the number they picked would land in your property outside of the new range.

---

## What you no longer have to worry about ##

If you have built sliders with plain Swing before, this table sums up the traps `SliderTicks`
takes off your hands:

| With a plain `JSlider` | With `SliderTicks` |
|---|---|
| Tick spacings and label positions are whole numbers, so a slider from `0.0` to `1.0` cannot have a tick mark every 0.1. | Spacings and label positions are numbers of your slider's own type, and the tick marks land exactly. |
| When you change the major tick spacing, the automatic labels stay where they were. | The labels are laid out again whenever anything they depend on changes. |
| After you replace the automatic labels with a label table of your own, changing the maximum brings the automatic labels back, mixed in with yours. | Your labels stay exactly as you declared them. |
| An empty label table combined with `setPaintLabels(true)` throws a `NullPointerException` in `BasicSliderUI`. | A `SliderTicks` without labels simply shows no labels. |
| A minor spacing which does not divide the major spacing evenly is accepted without a word. | You count the minor tick marks between the major ones, so they always divide the gap evenly. |
| `setSnapToTicks(true)` changes the number of the knob behind your model's back. | Only the moves of your users snap, and your property is never overwritten. |

---

## Where to go from here ##

- To see every behaviour on this page as a runnable scenario, read the living documentation of
  `Slider_Ticks_Spec`. The javadoc of `SliderTicks` describes each of its methods in detail.
- For real examples, open `examples.budget.mvi.BudgetView`, which labels its budget slider with the
  same money formatting as the rest of its view and snaps it to steps of €250, and
  `examples.breathing.mvi.BreathingView`, whose timing sliders use hidden tick marks as a step size.
- If you would like to keep a `SliderTicks` in a view model, [Functional MVVM](./Functional-MVVM.md)
  shows you how view models and properties fit together.
