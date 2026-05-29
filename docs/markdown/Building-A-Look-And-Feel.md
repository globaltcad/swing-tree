
# Building a Look-and-Feel on the SwingTree Style Engine #

> **Audience:** Advanced library users who want to ship a reusable Swing
> *Look-and-Feel* (a set of `javax.swing.plaf.ComponentUI` extensions registered
> through `UIManager`) and have it integrate cleanly with the SwingTree style
> engine, including all of its richer features — rounded borders, drop shadows,
> noise paints, gradient fonts, painter layers and HiDPI scaling.
>
> **Prerequisites:** You should already be comfortable with the per-component
> [`withStyle(..)`](./Climbing-Swing-Tree.md#blooming-flowers) API and with the
> central [`StyleSheet`](./Style-Sheets-And-Groups.md). This guide is *not* an
> introduction to those — it shows how to plug a **third** styling layer (your
> LAF) into the same engine. Familiarity with
> [HiDPI scaling](./HiDPI-Scaling.md) is also helpful.

A Swing Look-and-Feel has historically been a heavyweight thing to write. Even a
"simple" custom LAF requires you to extend a `BasicXxxUI` for every component
type and re-implement painting from scratch using `Graphics2D` primitives:
rectangles, anti-aliased rounded corners, gradient paints, drop shadows by hand,
focus indicators, disabled-state desaturation, and so on. Most of the line count
in any production LAF is plumbing for *the rendering*, not for the parts that
actually describe how the LAF should look.

SwingTree's style engine already knows how to render all of that. This guide
shows you how to **delegate the painting** to it while keeping the description
of what your LAF should look like in clear, declarative code.

---

## What you actually get from SwingTree ##

Before diving in, it helps to know the size of the gift. The style engine —
the same one that backs `withStyle(..)` and `StyleSheet` — can render, per
component:

- **A box model**: margin → border → padding → content, with per-edge widths
  and per-corner arcs.
- **Borders** of any width, with per-edge colors, per-corner radii, and a
  pluggable border `Painter`.
- **Background fills** that respect the rounded body, with optional
  *foundation* (under the margin) versus *background* (under the padding) split.
- **Drop shadows**, both outset and inset, with horizontal/vertical offset,
  blur radius, spread radius and colour — driven by `ShadowConf`.
- **Gradients**: linear, radial and conic, with multiple stops, rotation,
  scale, offset and clip area — driven by `GradientConf`.
- **Procedural noise paints**: ~30 built-in noise functions (stochastic,
  voronoi, marble, plasma, lightning, …) plus arbitrary user-supplied
  `NoiseFunction`s — driven by `NoiseConf`.
- **Image backgrounds** with fit, repeat, clip and tint controls — driven by
  `ImageConf`.
- **Frosted-glass parent filters** for the layer behind a component — driven
  by `FilterConf` (see [Background Filtering](./Background-Filtering.md)).
- **Custom text overlays**, freely positioned inside the component, with full
  font/paint control — driven by `TextConf`.
- **Painter hooks** at four named layers (`BACKGROUND`, `CONTENT`, `BORDER`,
  `FOREGROUND`) where you can drop in arbitrary `Painter` lambdas for anything
  not covered above.
- **Rich font configuration**: family, size, weight, posture, tracking,
  underline, strike-through, alignment, background fill, gradient/noise
  *paints applied to glyphs*, and selection colour — driven by `FontConf`
  (see [Font Styling](./Font-Styling.md)).
- **Size constraints** (`minSize`, `maxSize`, `prefSize`) and a layout
  installer that lets a style swap the component's `LayoutManager`.
- **Full HiDPI scaling** of every numeric input above (margins, radii, shadow
  offsets, gradient offsets, font sizes, …) so the same code looks the same
  physical size at any scale factor.

Anything you would otherwise have to hand-paint in a `ComponentUI` — anti-aliased
rounded corners, a softly blurred drop shadow under a button, a subtle diagonal
gradient on a panel, a marble-noise titlebar — is already in there, exposed
through the same fluent API you already use with `withStyle(..)`.

---

## The three styling layers ##

When SwingTree resolves the style of a component on every paint, it composes
**three** styling sources in this strict order:

```
                      ┌──────────────────────────┐
   1. Global default  │  StyleSheet               │  ← your app's central sheet
                      ├──────────────────────────┤
   2. Look-and-Feel   │  SwingTreeStyledComponentUI  │  ← *this guide*
                      ├──────────────────────────┤
   3. Per component   │  withStyle( it -> .. )    │  ← inline overrides
                      └──────────────────────────┘
                                  │
                                  ▼
                       Animation stylers (last)
```

Each layer receives the `ComponentStyleDelegate` produced by the previous one
and may transform it. This means **your LAF establishes defaults that
applications and end users can still override** with a style sheet entry or an
inline `withStyle(..)` call — exactly the layering CSS authors expect.

The hook for layer 2 is a single marker interface:

```java
package swingtree.api.laf;

public interface SwingTreeStyledComponentUI<C extends JComponent> {

    /**  Implement via ComponentUI#installUI(JComponent). */
    void installUI(JComponent c);

    /**  Implement via ComponentUI#paint(Graphics, JComponent). */
    void paint(Graphics g, JComponent c);

    /**  Declare the style of the component (pure function, no side effects!). */
    ComponentStyleDelegate<C> style(ComponentStyleDelegate<C> delegate) throws Exception;

    /**  Return true if paint() forwards to SwingTree (recommended). */
    default boolean canForwardPaintingToSwingTree() { return false; }
}
```

Make the three methods of your `ComponentUI` extension call into this interface
and SwingTree handles the rest.

---

## The mental model: declarative style, delegated paint ##

A SwingTree-compatible LAF gives up two old burdens and keeps two new ones:

| Burden                                            | Who handles it                       |
|---|---|
| Painting the background fill, border, gradient, shadow, noise, … | **SwingTree**                         |
| Caching painted layers, handling clip and alpha, HiDPI multipliers for *style geometry* | **SwingTree**                         |
| Deciding *what* a component should look like given its state    | **You** (in `style(..)`)             |
| Painting *content* the engine cannot know (icon, text glyphs, focus rectangle)  | **You** (in `paint(..)`)             |

Concretely:

- The `style(..)` method is invoked **once per paint cycle**, before any
  rendering happens. You read whatever state of the component is relevant
  (rollover, pressed, focused, enabled, selected, …) and return a
  `ComponentStyleDelegate` configured to look the way the component should
  look in that state. You do not paint anything here.
- The `paint(..)` method is invoked by Swing once the engine has finished its
  layers. You delegate to `ComponentExtension.paintBackground(..)` and let the
  engine paint background + border + shadows + gradients + noise, then —
  inside the callback — call `super.paint(g, c)` so the inherited
  `BasicXxxUI` can draw the icon/text on top.

The two methods are completely separate concerns: `style(..)` is a **pure
function** that produces a style, `paint(..)` is the **glue** that hands
painting over.

---

## The minimum viable LAF component ##

Here is the skeleton for a custom `JButton` UI that integrates with SwingTree.
It is the same shape as the internal `DynamicLaF.ButtonStyler`, generalised to
your own LAF.

```java
package com.example.lookandfeel;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

public final class MyButtonUI
        extends    BasicButtonUI
        implements SwingTreeStyledComponentUI<AbstractButton>
{
    // === Required by Swing — ComponentUI lifecycle ===

    public static ComponentUI createUI(JComponent c) {
        return new MyButtonUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        // Tell SwingTree to gather, apply and install the style right away.
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2d -> {
            // SwingTree has already drawn the background, border, shadows,
            // gradients, noise — now let the basic LAF paint icon & text.
            super.paint(g2d, c);
        });
    }

    @Override
    public void update(Graphics g, JComponent c) {
        // Critical: route update() through paint() so SwingTree always runs.
        paint(g, c);
    }

    // === Required by SwingTreeStyledComponentUI ===

    @Override
    public boolean canForwardPaintingToSwingTree() {
        return true;   // we *do* delegate to SwingTree in paint()
    }

    @Override
    public ComponentStyleDelegate<AbstractButton> style(
        ComponentStyleDelegate<AbstractButton> it
    ) {
        AbstractButton b = it.component();   // read-only, do not mutate!
        ButtonModel    m = b.getModel();

        // Base style — applied to every button by this LAF.
        it = it
            .padding(8, 18, 8, 18)
            .borderRadius(10)
            .borderWidth(1)
            .borderColor(new Color(0x40_000000, true))
            .backgroundColor(new Color(0xF5, 0xF5, 0xF7))
            .foregroundColor(new Color(0x20, 0x20, 0x28));

        // State overlays.
        if ( !b.isEnabled() ) {
            it = it.foregroundColor(new Color(0x88, 0x88, 0x90));
        }
        else if ( m.isPressed() ) {
            it = it.backgroundColor(new Color(0xE0, 0xE0, 0xE5))
                   .shadowBlurRadius(0);
        }
        else if ( m.isRollover() ) {
            it = it.backgroundColor(new Color(0xFA, 0xFA, 0xFD))
                   .shadowBlurRadius(8)
                   .shadowColor(new Color(0, 0, 0, 60))
                   .shadowSpreadRadius(-1);
        }
        if ( b.isFocusOwner() ) {
            it = it.borderColor(new Color(0x3E, 0x82, 0xF6));
        }

        return it;
    }
}
```

That is roughly **a hundred lines for a fully styled, HiDPI-aware, animated
button UI** — radius, shadow, rollover, focus, disabled state included. The
same skeleton works for every component type: replace `BasicButtonUI` with
`BasicLabelUI`, `BasicPanelUI`, `BasicTextFieldUI`, … and change the generic
parameter to match.

Wire it up like any LAF:

```java
public final class MyLookAndFeel extends BasicLookAndFeel {

    @Override protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        table.put("ButtonUI", MyButtonUI.class.getName());
        table.put("LabelUI",  MyLabelUI.class.getName());
        table.put("PanelUI",  MyPanelUI.class.getName());
        // …one entry per component type you support.
    }

    @Override public String getName()        { return "MyLAF"; }
    @Override public String getID()          { return "MyLAF"; }
    @Override public String getDescription() { return "A SwingTree-backed LAF"; }
    @Override public boolean isNativeLookAndFeel()    { return false; }
    @Override public boolean isSupportedLookAndFeel() { return true;  }
}

UIManager.setLookAndFeel(new MyLookAndFeel());
```

---

## Anatomy of the three contract methods ##

### `installUI(JComponent)` — *kick the engine* ###

Your `installUI` is invoked by Swing whenever a component first acquires this UI
(and after every `updateUI()`). You almost always want this exact pattern:

```java
@Override
public void installUI(JComponent c) {
    super.installUI(c);                                   // install BasicXxxUI defaults
    ComponentExtension.from(c).gatherApplyAndInstallStyle(true);  // ← key line
}
```

The `gatherApplyAndInstallStyle(true)` call asks SwingTree to immediately walk
the three styling layers — sheet, LAF, inline — and install the result
(border, opacity, scaled font, dynamic LAF, animation hooks). The `true` forces
the work even if the style hash hasn't changed; on first install it always has,
but passing `true` documents intent and avoids subtle races.

Calling `super.installUI(c)` first lets the basic UI populate its own defaults
(input maps, key bindings, action maps); SwingTree then rides on top.

### `paint(Graphics, JComponent)` — *forward and let SwingTree run the layers* ###

The recommended body is the same shape for every component:

```java
@Override
public void paint(Graphics g, JComponent c) {
    ComponentExtension.from(c).paintBackground(g, g2d -> {
        super.paint(g2d, c);   // basic LAF draws icon/text in a clipped region
    });
}
@Override
public void update(Graphics g, JComponent c) { paint(g, c); }
```

What `paintBackground(g, lookAndFeelPainting)` does for you:

1. Re-resolves the current style for `c` (sheet → LAF → inline → animations).
2. Re-installs the dynamic border that owns the rounded shape.
3. Renders the `BACKGROUND` layer (foundation fill, background fill, images,
   gradients, noise, shadows, painters) into `g`.
4. **Clips `g` to `ComponentArea.BODY`** — the rounded interior, after the
   margin — and invokes your `lookAndFeelPainting` callback.
5. Buffers everything if a child component is using a parent filter so the
   blur reads from the rendered parent.

Inside the callback you call `super.paint(..)` (or `super.paintSafely(..)` for
text components), which draws the *content*: icons, glyphs, focus ticks. The
`CONTENT`, `BORDER` and `FOREGROUND` layers of the style engine are painted by
the dynamic `Border` SwingTree installs on the component — you do not have to
trigger them yourself.

**Always override `update(..)` to delegate to `paint(..)`.** Swing's default
`update` wipes the component with the AWT background color first, which would
erase the gradient/shadow you just painted.

### `style(ComponentStyleDelegate)` — *the declarative core* ###

This is where the actual *look* of your LAF lives. It is a **pure function**:

- Inputs: the component (read-only) and the current cumulative
  `ComponentStyleDelegate` carrying the style declared by the global
  `StyleSheet`.
- Output: a new `ComponentStyleDelegate` describing the look you want.

Treat it like a render function. SwingTree calls it on every paint cycle —
roughly every ~16 ms during an animation. **Do not**:

- Mutate the component (no `b.setText(..)`, no `setEnabled(..)`, no
  `setBackground(..)`). Mutating a `JComponent` inside paint triggers another
  paint and can produce infinite loops.
- Allocate large objects or perform I/O. Cache anything expensive at class
  scope.
- Rely on identity. The delegate may have been transformed by a style sheet
  in ways you cannot see; build your style by adding to it, not by starting
  fresh.

The recommended idiom is the chained `it = it.x(..).y(..)` form shown above:
you receive the previous-layer style and add to it, so a `StyleSheet`'s
`add(type(JButton.class), …)` rule is honoured *and* your LAF's defaults still
apply. Inline `.withStyle(..)` on a specific component then has the final say.

### `canForwardPaintingToSwingTree()` — *the contract switch* ###

Returning `true` tells SwingTree:

> "My `paint(..)` forwards to `ComponentExtension.paintBackground(..)`. You may
> rely on me to be the **only** hook into Swing's rendering pipeline for this
> component type."

Once `true`, SwingTree stops trying to install its own fallback UI on top of
yours. If you ever return `true` *without* actually forwarding, you will see
unstyled components — no rounded corners, no shadows. The default is `false`,
which is safe but means SwingTree may overlay a *second* dynamic UI in addition
to yours for components that need it. **For a real LAF, always return `true`
and always forward**.

---

## HiDPI: what scales itself, what you must scale ##

The style engine scales everything you pass into it. A `borderRadius(10)` is
ten **developer pixels** — on a 2.0 scale factor it is rendered as 20 component
pixels, and `it.componentPrefHeight()` will round-trip a value cleanly. You
will rarely think about scale inside `style(..)`.

You still have one obligation: **any custom painting you do yourself in
`paint(..)` is in component pixels.** If your inherited `BasicButtonUI.paint`
draws a focus rectangle two pixels inside the border, those two pixels are
literal component pixels and will look hairline thin on a 2× display.

The fix is to multiply by `UI.scale()` wherever you reach for AWT primitives:

```java
int focusInset = UI.scale(2);                  // 2 dev px → 4 component px at 2×
int strokeWidth = (int) Math.max(1, UI.scale(1.5f));
g2d.setStroke(new BasicStroke(strokeWidth));
g2d.drawRoundRect(focusInset, focusInset,
                  c.getWidth()  - 2*focusInset,
                  c.getHeight() - 2*focusInset,
                  UI.scale(8), UI.scale(8));
```

The accessor [`UI.scale(int)`/`UI.scale(float)`/`UI.scale(double)`](./HiDPI-Scaling.md)
is the one knob you need; everything else flows from it. For custom *painters*
registered via `it.painter(layer, painter)`, the painter is invoked inside a
graphics context whose transform is already pre-scaled, so there you usually do
not need to scale — you work in developer pixels too.

---

## Reading state inside `style(..)` ##

The whole reason a LAF exists, rather than a global `StyleSheet`, is that it
can branch on **per-component model state** in a generic way:

| Component family    | State you typically read |
|---|---|
| `AbstractButton`    | `getModel().isRollover()` / `isPressed()` / `isArmed()`, `isFocusOwner()`, `isEnabled()`, `isSelected()` |
| `JLabel`            | `isEnabled()` only — labels have no model |
| `JTextComponent`    | `isEditable()`, `isEnabled()`, `hasFocus()`, `getCaret().isVisible()` |
| `JPanel` / `JBox`   | typically just `isEnabled()`; panels rarely change with state |
| `JScrollPane`       | `getVerticalScrollBar().getValueIsAdjusting()`, focus, hover |

Putting all state-driven logic in `style(..)` keeps painting out of the
equation entirely. The familiar Swing pattern of "set the background colour in
the model listener" disappears — your LAF is *purely* a function of the
component's current state.

```java
ButtonModel m = b.getModel();
Color base   = baseColorFor(m, b);            // pure helper, no side effects
Color border = borderColorFor(m, b);

return it.backgroundColor(base)
         .borderColor(border)
         .padding(8, 18, 8, 18)
         .borderRadius(10);
```

---

## Animation: free of charge ##

Because `style(..)` runs every paint cycle, you do not have to wire up timers
to animate rollover or focus transitions. Just hand the delegate over to
`withTransitionalStyle(..)` at the application level (see
[An Advanced Style Animation](./An-Advanced-Style-Animation.md)) and the
animation runs *on top of* the LAF's base style — the two compose cleanly
because animation is always the last styling layer.

For LAF-level effects that animate (e.g. a button that pulses while focused),
you can also drive a property/observable from the application and consume it
in `style(..)`, but in practice this belongs in a per-component style overlay,
not in the LAF.

---

## What you do *not* implement ##

A handful of features handled directly by the style engine and never your
responsibility:

- Painting **rounded corner clipping** — SwingTree replaces the border with one
  that clips the body to the configured arcs.
- Painting **shadows** — `shadow…` keys in `style(..)` produce a real
  Gaussian-blurred drop shadow rendered before the background fill.
- Drawing **gradients** or **noise** as the background — adding a `gradient(..)`
  or `noise(..)` to the style is enough; the engine paints it. There is no
  paint code to write.
- Maintaining the **opacity** flag — the engine flips `isOpaque()` based on
  whether the resolved style is fully covering, so you never have to.
- **Restoring** the original background colour when a transitional style ends
  — `StyleInstaller` remembers the original values for you.
- **HiDPI multiplication** of any numeric style input — every method on
  `ComponentStyleDelegate` scales its argument.

This is why a SwingTree-backed LAF is typically a fraction of the size of a
conventional one: the rendering logic is one declarative call away.

---

## A worked example: a glassy panel LAF ##

Here is a complete `PanelUI` that draws a subtle vertical gradient and a soft
drop shadow under every `JPanel`, while still letting an application-supplied
`StyleSheet` or inline `withStyle(..)` override anything.

```java
public final class GlassyPanelUI
        extends    BasicPanelUI
        implements SwingTreeStyledComponentUI<JPanel>
{
    public static ComponentUI createUI(JComponent c) { return new GlassyPanelUI(); }

    @Override public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override public void paint(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2d -> super.paint(g2d, c));
    }
    @Override public void update(Graphics g, JComponent c) { paint(g, c); }

    @Override public boolean canForwardPaintingToSwingTree() { return true; }

    @Override public ComponentStyleDelegate<JPanel> style(
        ComponentStyleDelegate<JPanel> it
    ) {
        return it
            .borderRadius(12)
            .padding(12)
            .gradient(g -> g
                .colors(new Color(0xFF_FFFFFF, true), new Color(0x10_FFFFFF, true))
                .span(UI.Span.TOP_TO_BOTTOM)
            )
            .shadowBlurRadius(14)
            .shadowSpreadRadius(-2)
            .shadowColor(new Color(0, 0, 0, 40))
            .shadowOffset(0, 4);
    }
}
```

That is the *entire* LAF for the panel. No `Graphics2D`, no anti-aliasing
flags, no `RoundRectangle2D`, no per-monitor scale handling. Drop the matching
entry into your `BasicLookAndFeel`'s defaults table and every `JPanel` in the
application acquires the look — while still honouring `it.borderRadius(0)` in
an application's style sheet for a specific group.

---

## Cooperating with application-level styling ##

A LAF is *one* of three styling layers. The application is allowed to override
you, and your LAF should be designed with that in mind:

- **Prefer additive declarations.** A LAF rule that calls `it.borderRadius(10)`
  sets a default that an application's `add(type(JPanel.class), …)` rule can
  freely change to `0`. Avoid hard-coding values that other layers may want to
  pin to brand colours or accessibility-driven sizes.
- **Read what the previous layer already declared** if you only want to fill
  in defaults. The delegate carries the cumulative style; for properties you
  don't want to clobber, do not set them.
- **Branch on state, not on identity.** Use `getModel().isRollover()`, not
  `c.getClientProperty(..)`, for state checks. Clients use client properties
  for their own bookkeeping; a LAF that reads them couples to a specific
  application's conventions.
- **Document your group tags.** If your LAF supports semantic variants
  (`SECONDARY`, `DANGER`, …), expose them through a public `enum` that
  application code can use with `.group(MyLAF.Variant.DANGER)`, and branch on
  the component's tags inside `style(..)` via the SwingTree styler API.

---

## Caveats and tripwires ##

- **`style(..)` must be pure.** Calling `c.setBackground(..)`, `setFont(..)`,
  `revalidate()` or `repaint()` from inside it will, at best, cause a render
  flicker; at worst, an infinite paint loop. Read the model, return a new
  delegate, done.
- **Always honour incoming style.** Never construct a fresh
  `ComponentStyleDelegate` from scratch; always transform the one passed in.
  Otherwise the global `StyleSheet` silently stops working for the components
  your LAF owns.
- **Never paint without forwarding to `paintBackground(..)`.** If your
  `paint(..)` calls `super.paint(g, c)` directly, the engine renders nothing,
  and you lose every feature in this guide. The forwarding is what runs the
  layers.
- **`canForwardPaintingToSwingTree()` is a contract, not a hint.** Returning
  `true` while not forwarding *breaks all styling* for the component type.
- **Mind exceptions.** Anything thrown from `style(..)` is caught and logged
  by `StyleSource`; the component falls back to the previous layer's style.
  Use it as a safety net during development, not as a control-flow tool.
- **Component pixels in `paint(..)` only.** Inside the callback passed to
  `paintBackground(..)`, your inherited `BasicXxxUI.paint` works in component
  pixels and must scale its own primitives with `UI.scale(..)`.
- **Coverage gap.** SwingTree's own `DynamicLaF` covers `JPanel`,
  `AbstractButton`, `JLabel` and `JTextField` out of the box. For other
  component types (`JComboBox`, `JList`, `JScrollBar`, …) the engine still
  *gathers* style from your `style(..)` method, but you also have to inherit
  from the right `BasicXxxUI` and forward `paint`/`update` yourself.

---

## Where to next? ##

- [`SwingTreeStyledComponentUI`](../../src/main/java/swingtree/api/laf/SwingTreeStyledComponentUI.java)
  — the interface's full javadoc.
- [`DynamicLaF.PanelStyler` / `ButtonStyler` / `LabelStyler` / `TextFieldStyler`](../../src/main/java/swingtree/style/DynamicLaF.java)
  — the four implementations SwingTree ships internally; the canonical
  reference for the forwarding pattern.
- [`ComponentExtension`](../../src/main/java/swingtree/style/ComponentExtension.java)
  — the entry point for `paintBackground(..)` and `gatherApplyAndInstallStyle(..)`.
- [Style Sheets and Groups](./Style-Sheets-And-Groups.md) — layer 1 of the
  cascade; combine with your LAF to give applications a turnkey theming story.
- [Font Styling](./Font-Styling.md), [Background Filtering](./Background-Filtering.md),
  [An Advanced Style Animation](./An-Advanced-Style-Animation.md) — features
  worth knowing about because *your LAF can offer them for free* once it forwards
  to the engine.
- [HiDPI Scaling](./HiDPI-Scaling.md) — the rest of the scaling rules, including
  the `componentPrefWidth()` round-trip pattern.