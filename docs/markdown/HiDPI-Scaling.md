
# HiDPI Scaling — "Developer Pixels" vs "Component Pixels" #

> **TL;DR:** Everything you pass *into* the SwingTree API is measured in
> **developer pixels** and gets scaled **up** for you. Everything SwingTree hands
> *back* to you (sizes, positions, mouse coordinates) is scaled **down** into
> developer pixels again, so it round-trips cleanly. The one place this breaks is
> when you reach past the SwingTree API to a **raw Swing component** (e.g. the
> deprecated `component()` accessor) — those values are in component pixels and
> will be **double-scaled** if you feed them back in.

## Why scaling exists at all

High-resolution displays with a high pixel density (measured in DPI/PPI) are the
norm today, but vanilla Swing handles them poorly. Even with the HiDPI support
added in Java 9, applying a scale factor to the UI is left to the Look-and-Feel —
and **none** of the Look-and-Feels shipped with the JDK actually scale the UI.

SwingTree fills that gap. It maintains a single **UI scale factor**
(`UI.scale()`, a `float`), computed automatically from the system font, and
applies it consistently so that a "16px button" looks the same physical size on a
1× laptop screen and a 2× 4K monitor.

This gives us two coordinate spaces:

| Space | What it means | Example |
|---|---|---|
| **Developer pixels** | The DPI-agnostic numbers *you* write in your code. | `withPrefSize(100, 50)` |
| **Component pixels** | The real, scaled numbers Swing lays out and paints with. | at scale `2.0` → `200 × 100` |

`UI.scale(x)` converts developer → component pixels (multiply); `UI.unscale(x)`
converts the other way (divide). You rarely call these directly — **the whole
point of SwingTree is that it scales for you.**

## The golden rule

You almost never have to think about scaling, *provided you stay inside the
SwingTree API*, because the conversion is symmetric:

```
        you write (developer px)                SwingTree gives back (developer px)
            │                                              ▲
            │  UI.scale(..)  ── multiply ──┐    ┌── divide ──  UI.unscale(..)
            ▼                              ▼    │
                         Swing component (component px)
```

- **Inputs are scaled up.** Every dimension you pass to a builder
  (`withPrefSize`, `withMinSize`, `withWidth`, …) or to the style API
  (`prefSize`, `minHeight`, `margin`, `padding`, `borderWidth`, `borderRadius`,
  gradient/shadow offsets, …) is interpreted as developer pixels and multiplied
  by `UI.scale()`.
- **Outputs are scaled down.** Every size/position SwingTree reads back for you
  through its delegates is divided by `UI.scale()`, so it is in developer pixels
  again — the same units you wrote.

Because both directions agree, a value cleanly **round-trips**:

```java
UI.button("OK")
.withPrefSize(100, 50)                       // you write developer px → stored as 200×100 at scale 2.0
.withStyle( it -> it
    .minHeight(it.componentPrefHeight())     // read back as 50 (dev px) → re-scaled to 100. Correct! ✅
);
```

## The trap: raw Swing values are in *component* pixels

The symmetry only holds for the SwingTree API. The moment you grab the **raw
Swing component** — via the **deprecated** `component()` accessor on a style
delegate, or via `peek(c -> ...)` — you are looking at *component pixels*. They
are already scaled. If you feed such a value back into a SwingTree method that
scales its input, it gets scaled **twice**:

```java
UI.button("Buggy")
.withPrefSize(100, 50)                                    // stored as 200×100 at scale 2.0
.withStyle( it -> it
    .minHeight(it.component().getPreferredSize().height)  // reads 100 (component px) → re-scaled to 200. BUG! ❌
);
```

At scale `2.0` the minimum height ends up `200` instead of the intended `100` —
and the error grows with the scale factor. This is the single most common
scaling bug, and it is exactly why `component()` is deprecated.

**The fix** is to use the scaling-aware accessors that hand you developer pixels:

```java
.withStyle( it -> it.minHeight(it.componentPrefHeight()) )   // ✅ round-trips cleanly
```

## The scaling-aware accessors (use these instead of `component()`)

### On the style delegate (`withStyle(it -> ...)`)

`ComponentStyleDelegate` exposes the component's geometry already converted to
developer pixels:

| Method | Returns (in developer pixels) |
|---|---|
| `it.componentWidth()` | the current width |
| `it.componentHeight()` | the current height |
| `it.componentPrefWidth()` | the **preferred** width |
| `it.componentPrefHeight()` | the **preferred** height |

> `it.component()` is **deprecated** — not only for the scaling trap above, but
> also because mutating a live component inside a (supposedly side-effect-free)
> styler invites endless-repaint bugs.

### On event delegates (`onClick`, `onMouseMove`, `onResize`, `onDrag`, …)

Every event handler receives a delegate (`it`) that extends `AbstractDelegate`,
whose geometry getters are **all** in developer pixels:

| Method | Developer-pixel value |
|---|---|
| `it.getX()`, `it.getY()`, `it.getPosition()` | component position relative to its parent |
| `it.getWidth()`, `it.getHeight()`, `it.getSize()` | current component size |
| `it.getPrefSize()` | preferred size |
| `it.getBounds()` | full bounds rectangle |
| `it.setBounds(x,y,w,h)`, `it.setPrefSize(..)`, `it.setMinSize(..)`, … | **setters** take developer pixels and scale up for you |

Mouse delegates add component-relative pointer coordinates (also developer
pixels):

| Method | Developer-pixel value |
|---|---|
| `it.mouseX()`, `it.mouseY()` | pointer position relative to the component |
| `it.mousePosition()` | the same as a `Position` |

Drag delegates expose the gesture history, again in developer pixels:

| Method | Developer-pixel value |
|---|---|
| `it.initialComponentPosition()` | where the component sat when the drag began |
| `it.dragPositions()` | the path of the drag |
| `it.deltaXSinceStart()`, `it.deltaYSinceStart()` | accumulated drag delta |

### ⚠️ The one exception: on-screen coordinates are *raw*

Absolute, screen-relative mouse coordinates are **not** unscaled — they are
reported exactly as the OS/AWT delivers them:

| Method | Coordinate space |
|---|---|
| `it.mouseXOnScreen()`, `it.mouseYOnScreen()` | **raw** absolute screen pixels |
| `it.mousePositionOnScreen()` | **raw** absolute screen position |

These are absolute desktop coordinates (possibly spanning multiple monitors), so
"developer pixels" would be meaningless for them. Don't mix an on-screen value
with a component-relative one without converting.

## When you *do* need to scale manually

If you drop down to raw Swing — custom `Graphics2D` painting outside the style
API, hand-rolled `setBounds` on a peeked component, integrating a third-party
widget — SwingTree can't scale for you, so reach for the helpers directly:

```java
import swingtree.UI;

int scaled   = UI.scale(120);        // developer → component (int, rounded)
double s     = UI.scale(12.5);       // developer → component (double)
int back     = UI.unscale(240);      // component → developer
Dimension d  = UI.unscale(rawSize);  // whole Dimension at once
UI.scale(graphics2D);                // scale a Graphics2D context in place
float factor = UI.scale();           // the raw multiplier, if you need it
```

To override the factor (e.g. in tests or to force a scale):

```java
SwingTree.get().setUiScaleFactor(2.0f);
// or, at startup:
SwingTree.initializeUsing(cfg -> cfg.uiScaleFactor(2.0f));
```

## Checklist

- ✅ Pass **developer pixels** to every `withXyz(..)` and style method — never
  pre-multiply by `UI.scale()` yourself.
- ✅ Read geometry back through the **delegate** accessors
  (`componentWidth/Height/PrefWidth/PrefHeight`, `getWidth/Height/Bounds/Size`,
  `mouseX/Y`, …) — they are already in developer pixels.
- ❌ Don't read `component().getPreferredSize()` / `getBounds()` / `getWidth()`
  and feed the result back into a SwingTree method — that **double-scales**.
- ⚠️ Remember `mouse*OnScreen()` is the lone exception: raw screen pixels, not
  developer pixels.
- 🛠️ Only call `UI.scale(..)` / `UI.unscale(..)` when you are working against
  **raw Swing** (custom painting, peeked components, third-party widgets).