
# Responsive Layouts #

> **Looking for the bigger picture?** This guide is the *mechanics* of
> SwingTree's responsive grid. For *why* a desktop app should adapt at all, and
> how this fits together with `Var<Layout>` and form-factor state, read
> [Convergent Design](./Convergent-Design.md) first — then come back here for
> the details.

An important aspect of modern UI design is to make your UI layout responsive
to different screen sizes and resolutions. The state of the art in web design
is to use CSS Grids to create responsive layouts.

SwingTree offers a similar approach to building responsive
layouts for Swing components through the `ResponsiveGridFlowLayout` class,
which is heavily inspired by the [12 columns based grid layout from bootstrap](https://getbootstrap.com/docs/4.0/layout/grid/).

In this guide we will show you how to make your SwingTree UI declarations
responsive to different screen and parent container sizes.

## Width Classification ##

The basic idea behind any responsive layout is to define a set of relative size categories
that a container component can be in (either in terms of width or height),
and then based on this category place the child components so that they
fit the available space in the best way possible.

A popular approach is to have simple "small", "medium", "large" and "very large" categories
and then use these categories to define how many columns a component should span in a grid layout.

This is the approach that SwingTree takes with the `ResponsiveGridFlowLayout` class.

So a component wrapper, like a `JPanel` with a SwingTree based flow layout
takes in components with custom span configurations for each size category.
These span configurations define how many columns/cells should be spanned.

This size category is based on the width of the parent compared to its **reference
width** — by default its preferred width. So a component is considered large if it
nearly meets that width, and smaller as it approaches 0.
(A size larger than the reference width is considered "oversize", which is also a
valid category.)

The bands are exact fifths, which is worth knowing when you are choosing spans:

| Category | Container width ÷ reference width | Configurator method |
|---|---|---|
| `VOID` | ≤ 0 | — (nothing is laid out yet) |
| `VERY_SMALL` | 0 … ⅕ | `.verySmall(n)` |
| `SMALL` | ⅕ … ⅖ | `.small(n)` |
| `MEDIUM` | ⅖ … ⅗ | `.medium(n)` |
| `LARGE` | ⅗ … ⅘ | `.large(n)` |
| `VERY_LARGE` | ⅘ … 1 | `.veryLarge(n)` |
| `OVERSIZE` | ≥ 1 | `.oversize(n)` |

You do not have to declare all six. If the current category has no span, the
layout looks outward for the **nearest declared** one — so
`AUTO_SPAN(it -> it.large(12))` is a perfectly good way of saying "always the
full row". Spelling all six out is still the friendlier choice in shared code:
the table then reads as the design document for that component.

## Example ##

Enough theory, let's see how this works in practice!

```java
public static void main(String[] args) {
    UI.show("Responsive", f ->
        UI.panel().withFlowLayout(UI.HorizontalAlignment.CENTER, 7,7)
        .withPrefSize(500, 400)
        .withBackground(UI.Color.LIGHTGRAY)
        .add(UI.AUTO_SPAN(it->it.small(12).medium(4).large(3).veryLarge(3)),
             UI.box().withPrefSize(100,100).withStyle(it->it
                 .backgroundColor(UI.Color.RED).borderRadius(24)
             )
        )
        .add(UI.AUTO_SPAN( it -> it.small(3).medium(4).large(3).veryLarge(3).oversize(12)),
             UI.box().withPrefSize(100,100).withStyle(it->it
                 .backgroundColor(UI.Color.GREEN).borderRadius(24)
             )
        )
        .add(UI.AUTO_SPAN(it->it.small(3).medium(4).large(3)),
             UI.box().withPrefSize(100,100).withStyle(it->it
                 .backgroundColor(UI.Color.BLUE).borderRadius(24)
             )
        )
        .add(UI.AUTO_SPAN(it->it.small(3).medium(4).large(3)),
             UI.box().withPrefSize(100,100).withStyle(it->it
                 .backgroundColor(UI.Color.CYAN).borderRadius(24)
             )
        )
        .add(UI.AUTO_SPAN(it->it.small(12).medium(4).large(6)),
             UI.box().withPrefSize(100,100).withStyle(it->it
                 .backgroundColor(UI.Color.OAK).borderRadius(24)
             )
        )
        .get(javax.swing.JPanel.class)
    );
} 
```

<img src="../img/tutorial/responsive-grid-flow-layout.gif" style = "float: right; width: 40%; margin: 2em;">

This code creates a responsive layout consisting of 5 boxes,
which are laid out in vastly different ways depending on the size of the parent panel.
The most important part here is the `UI.AUTO_SPAN(..)` method calls, which
produce `FlowCell` objects that define the span configuration for each size category.
Without these component constraints, the layout would behave exactly like a normal flow layout.

Note that we are not explicitly creating a `ResponsiveGridFlowLayout` object.
This is done automatically by the `withFlowLayout(..)` method, which instantiates
and installs the layout manager for you.

The configurator lambda passed to the `UI.AUTO_SPAN(..)` method is 
not only a fluent API for setting the span configuration, but also
a way to determine these span configurations dynamically.
What this means is that every time the parent component is resized,
the layout manager will re-evaluate these lambda expressions and
re-layout the components accordingly.

This is a powerful feature that allows you to create dynamic layouts
that adapt to any conceivable screen size or parent container size.

## Heights: rows are as tall as their tallest child ##

The grid wraps in **one** direction. A row's height is
`max(child preferred height)` and nothing more — a flow grid will *never*
stretch a row to fill a tall window the way `MigLayout`'s `grow, push` does.

That has two practical consequences.

**First, give a preferred height to anything that has none.** A `scrollPane` or
`scrollPanels` is happy to be one pixel tall, and in a grid that is exactly what
it will get:

```java
UI.scrollPanels().withPrefSize(340, 470)   // or the roster collapses to a line
.addAll(members, (Var<Person> p) -> memberCard(p, vm))
```

**Second, put a page-level grid inside a scroll pane.** Once the cards stack the
page is taller than the window, and without a scroll pane the bottom of it is
simply clipped:

```java
UI.scrollPane( conf -> conf.fitWidth(true) )
.withHorizontalScrollBarPolicy(UI.Active.NEVER)
.withVerticalScrollIncrement(24)
.add( thePageGrid )
```

`fitWidth(true)` makes the viewport force the content's width to match its own,
which is what keeps the grid measuring itself against the visible width rather
than against some larger ideal.

### Making cards in a row equal height: `fill(true)` ###

Within a row you *can* equalise heights — that is what `fill` is for. A cell
declared with `fill(true)` is stretched to the height of the row (i.e. of the
tallest child in it):

```java
AUTO_SPAN( it -> it.fill(true).medium(12).large(5).veryLarge(4) )
```

This is how a short sidebar card ends up flush with a tall content card instead
of floating at the top of the row. A `MigLayout` child whose container
constraint contains `fill` or `filly` gets this behaviour automatically — the
grid detects it — so you mostly need `fill(true)` explicitly for children that
are *not* fill-constrained MigLayout panels.

`align(UI.VerticalAlignment.TOP | CENTER | BOTTOM)` is the alternative when you
want the cell to keep its own height and merely position itself in the row.

## Minimum widths: the reason your layout won't narrow ##

`ResponsiveGridFlowLayout` reports **the sum of all its children's minimum
widths** as its own minimum. That is a defensible definition — it is what a
single row of everything would need — but it is almost never what you want for
a *page*, because any parent that honours minimums (including the box a
`scrollPane` wraps its content in) will then refuse to let the page get narrower
than "all cards side by side". The grid never reaches its narrow bands at all.

Say what you actually mean:

```java
UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 18, 18)
.withMinSize(0, 0)                     // this page may shrink to whatever it is given
```

…and pair it with `"wmin 0"` on whatever MigLayout cell holds the grid, plus on
the long labels inside the cards, since minimums propagate upwards through the
whole tree. A single forgotten label deep inside a card is enough to
re-establish the floor.

> A span is assigned **regardless of a child's minimum width**. So after you
> pick a band, check that the narrowest member of it actually fits — the layout
> will happily hand a card 3 of 12 columns even if its content needs more.

<a id="nesting-grids-and-the-reference-width"></a>

## Nesting Grids and the Reference Width ##

A responsive grid may be placed inside another responsive grid, which is
how you would express a page whose sidebar is itself a grid of items.
There is one thing to know before you do that.

Size categories are measured against the width at which a grid considers
itself "full", its *reference width*. By default that is the width of all
of its children laid out in a single row, which is also the preferred
width the grid reports to its parent.

For a nested grid those two roles pull in opposite directions. A sidebar
holding a dozen items has an ideal single row width of well over a thousand
pixels, and reporting *that* to the page it lives on would push the page
into its narrowest size category and make the sidebar the widest thing on
the screen.

So give a nested grid an explicit preferred size:

```java
UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 6, 6)
.withPrefSize(700, 0) // <- the reference width
.withMinSize(0, 0)
.add(UI.AUTO_SPAN(it->it.small(12).medium(6).veryLarge(4).oversize(3)), item1)
.add(UI.AUTO_SPAN(it->it.small(12).medium(6).veryLarge(4).oversize(3)), item2)
// ...
```

This declares the width at which the nested grid considers itself full, and
it is what the parent gets to see. Its *height*, however, still follows the
rows it wraps into — for a grid an explicit preferred size is a lower bound
rather than a fixed size, because the rows have to fit no matter what.

Pick a reference width which is wider than the narrowest slot the grid will
ever occupy, so that there is room left for the size categories you want to
use once it stretches out.

### ⚠️ A grid nests inside a grid — not inside a MigLayout cell ###

The `withPrefSize(refWidth, 0)` idiom above has a hard boundary, and it is the
single most common way a responsive SwingTree layout goes wrong.

A wrapping grid is a **width-for-height** layout: the height it needs is a
function of the width it is given, and it cannot be expressed as one fixed
number. `ResponsiveGridFlowLayout` therefore asks each child *"how tall would
you like to be at the width you are about to receive?"* — and only a nested
`ResponsiveGridFlowLayout` can meaningfully answer that question. Everything
else is asked for its plain preferred height.

Now recall that `JComponent.getPreferredSize()` short-circuits the layout
manager entirely once a preferred size has been set explicitly. So a MigLayout
parent asking a nested grid for its preferred height gets the literal `0` you
wrote — and the grid collapses to nothing, taking all of its content with it.

```java
// ❌ the form is laid out at height 0 and silently clipped away
UI.panel("fill, wrap 1")                              // MigLayout
.add("growx",      titleStrip())
.add("grow, push", UI.panel().withFlowLayout(..)
                     .withPrefSize(620, 0)
                     .add(FULL_ROW, field("Name", ..)));

// ✅ the card is a grid too, so the width-for-height question reaches the form
UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 0, 0)   // a grid
.withMinSize(0, 0)
.withPrefSize(620, 0)
.add(FULL_ROW, titleStrip())
.add(FULL_ROW, UI.panel().withFlowLayout(..)
                 .withPrefSize(620, 0)
                 .add(FULL_ROW, field("Name", ..)));
```

The rule in one line: **a grid that declares a reference width must live inside
another grid, or directly inside a `scrollPane(conf -> conf.fitWidth(true))`.**
Anywhere else, drop the explicit preferred size — or make the container a grid.

You can see both halves of this in
[`examples/team/mvi/TeamView.java`](../../src/test/java/examples/team/mvi/TeamView.java):
the header is deliberately a plain MigLayout panel (it declares no reference
width, so it doesn't need to be a grid), while the editor **card** is a grid
purely so that the responsive form nested inside it measures correctly.

## Debugging a layout that won't converge ##

A short field guide, in the order the causes actually occur:

| Symptom | Likely cause | Fix |
|---|---|---|
| The window refuses to get narrower than some arbitrary width | A child's minimum width has propagated to the top | `"wmin 0"` on the rows, `withMinSize(0, 0)` on the grid |
| A card is one line tall, or missing entirely | It has no preferred height (`scrollPane`, `scrollPanels`, empty `textField`) | `withPrefSize(w, h)` on it |
| A *nested* grid renders at zero height | It declares `withPrefSize(w, 0)` inside a MigLayout cell | Make the parent a grid, or drop the explicit preferred size |
| The bands trigger at the wrong widths | The reference width is the single-row sum, which is huge | Declare it: `withPrefSize(referenceWidth, 0)` |
| A nested grid makes the whole page think it is narrow | Same — the nested grid is reporting its single-row width upward | Same |
| The stacked layout is cut off at the bottom | The page has outgrown the window | Wrap it in `scrollPane(conf -> conf.fitWidth(true))` |
| One card floats at the top of its row | It isn't filling the row height | `AUTO_SPAN(it -> it.fill(true)...)` |
| The same width renders differently depending on which way you dragged | A vertical scrollbar appearing steals ~15 px and can flip the category near a band boundary | Both states are self-consistent, so it settles rather than oscillating; move the reference width if it bothers you |

Two tools worth knowing: add `debug` to a MigLayout container constraint to
draw its guide borders, and — because the whole thing is deterministic — you can
simply `setSize(..)`, `doLayout()` and assert on `getHeight()` in a test rather
than eyeballing it.

## Conclusion ##

SwingTree's `ResponsiveGridFlowLayout` is a nice alternative to
the rather complex `MigLayout` and `GridBagLayout` classes that
are often used for creating responsive layouts in Swing applications.

By utilizing the concept of size categories and span configurations, 
SwingTree optimizes the trade-off between flexibility and ease of use
to allow you to design UIs that look great and function well on a wide range 
of screen sizes and devices.

## Where to next? ##

- [Convergent Design](./Convergent-Design.md) — the strategy this grid serves:
  when to use it, when to reach for `Var<Layout>` or a form factor instead, and
  a checklist for reviewing a view.
- [Reactive Layouts](./Reactive-Layouts.md) — pair what you learned here
  with a `Var<Layout>` to switch *layout families* at runtime (compact
  view ↔ tablet view ↔ analytics, …) without rebuilding any components.
- [Climbing the Swing Tree → Growing Branches](./Climbing-Swing-Tree.md#growing-branches)
  for the underlying MigLayout basics.
