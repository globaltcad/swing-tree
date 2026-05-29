
# Style Sheets and Groups #

> **Prerequisites:** This guide assumes you are comfortable with the
> per-component `withStyle(..)` API introduced in
> [Climbing the Swing Tree → Blooming Flowers](./Climbing-Swing-Tree.md#blooming-flowers).
> If `withStyle` still feels mysterious, start there.

Per-component styling is great for one-off tweaks, but it has a familiar
problem: as your application grows, the same style fragments end up
copy-pasted across many components. Change the brand colour and you find
yourself hunting through dozens of files. Add dark-mode support and the
problem multiplies.

In CSS, the answer was **style sheets and class selectors** — describe the
style once, attach a *class* to every component that should wear it, and
swap the whole sheet to re-skin the app.

SwingTree gives you the same lever: a **`StyleSheet`** class that holds all
your styling rules, and a **`group(..)`** tag attached to each component that
declares "I am one of these". This guide walks you through the system end
to end, and ends with the trick that the
[Theme Garden](../../src/test/java/examples/zen/ThemeGardenView.java) demo
relies on: swapping the entire theme at runtime without touching the view.

---

## The mental model ##

A SwingTree style sheet is just two ingredients glued together:

1. A **trait** — a *selector*, in CSS terms. It says *which* components a
   rule should target. Traits are created with one of these factory
   methods inside your sheet:

   | Factory | What it matches |
   |---|---|
   | `id("name")`                 | The single component you tagged with `.id("name")` in the builder. Roughly CSS `#name`. |
   | `group("tag")`               | Any component tagged with `.group("tag")`. Roughly CSS `.tag`. |
   | `group(MyEnum.FOO)`          | Same, but using a type-safe enum tag — **recommended**. |
   | `type(JButton.class)`        | Every component of (or subclassed from) `JButton`. Roughly CSS `button`. |

   Traits compose: `type(JButton.class).group(Skin.PRIMARY)` matches buttons
   that *also* carry the `PRIMARY` group.

2. A **styler** — a lambda that receives a `ComponentStyleDelegate` (the
   familiar `it` from `withStyle(..)`) and produces the styled result.

You register a `(trait, styler)` pair with `add(trait, styler)` inside the
`configure()` method of a `StyleSheet` subclass. SwingTree then asks the
sheet to compute a style for every painted component, walking the trait
graph and merging matching rules in a deterministic order.

```java
public final class MySheet extends StyleSheet {
    @Override
    protected void configure() {
        // Every JButton, anywhere
        add(type(JButton.class), it -> it
            .borderRadius(8)
            .padding(6, 14, 6, 14)
        );

        // Only buttons tagged as "primary"
        add(type(JButton.class).group(Skin.PRIMARY), it -> it
            .backgroundColor(new Color(60, 130, 246))
            .foregroundColor(Color.WHITE)
        );

        // A unique component identified by id
        add(id("ok-button"), it -> it
            .shadowBlurRadius(8)
            .shadowColor(new Color(0, 0, 0, 60))
        );
    }
}
```

---

## Tagging components in the view ##

The corresponding view code attaches the matching tags. **`group(..)` and
`id(..)` live on the builder**, not on the style:

```java
import static swingtree.UI.*;

enum Skin { PRIMARY, SECONDARY }

UI.show(
    panel("fill, wrap 2, ins 16")
    .add("growx", button("Cancel").group(Skin.SECONDARY))
    .add("growx", button("OK").id("ok-button").group(Skin.PRIMARY))
);
```

> **Always prefer enum-based group tags.** They are type-safe and survive
> refactoring — a typo in a string tag silently misses every rule.

For the rules above to actually apply, the sheet has to be installed in a
scope around the declaration. There are two ways to do this:

```java
// 1) Global default for the rest of the application
SwingTree.initializeUsing( cfg -> cfg.styleSheet(new MySheet()) );

// 2) Local scope — only the components built inside the lambda are bound
UI.use(new MySheet(), () ->
    UI.show( frame -> new MyView() )
);
```

Within the `UI.use(..)` scope, every SwingTree-built component automatically
consults the sheet on every paint. Outside the scope (or before the global
sheet is installed) components ignore it. **Note that this only affects
components built *while inside the scope*** — passing a pre-built JComponent
through `UI.of(..)` outside the scope will not bind it.

---

## How traits combine ##

When SwingTree paints a component, it walks every `(trait, styler)` pair in
the sheet and asks: *does this trait match?* All matching stylers are then
applied in declaration order, with more-specific traits applied last so
they win. The rough rules:

- `id` is the most specific — exactly one component.
- `type(C).group(T)` beats both `type(C)` and `group(T)` alone.
- `group(T)` beats `type(C)` when both match.
- Within equal specificity, later `add(..)` calls override earlier ones.

This means you can layer rules: a broad "all buttons" rule sets defaults,
a "primary buttons" rule overrides colours, an `id` rule fine-tunes one
specific component.

```java
add(type(JButton.class), it -> it.padding(6).borderRadius(8));   // defaults
add(group(Skin.PRIMARY),  it -> it.backgroundColor(BLUE));        // accent
add(id("ok-button"),      it -> it.shadowBlurRadius(8));          // unique
```

A button tagged `id("ok-button").group(Skin.PRIMARY)` ends up with all
three — padding + radius + blue background + shadow.

---

## Hot-swappable themes ##

So far we've used the sheet for static styling. The really powerful trick,
and the one the [Theme Garden](../../src/test/java/examples/zen/ThemeGardenView.java)
example is built on, is to **mutate the sheet at runtime** and let SwingTree
repaint everything.

A single `StyleSheet` subclass can host *multiple* themes and switch between
them on demand:

```java
public final class ThemedSheet extends StyleSheet {

    private Theme theme = Theme.LIGHT;

    public void setTheme(Theme newTheme) {
        if (newTheme != theme) {
            this.theme = newTheme;
            reconfigure();   // <— the magic call
        }
    }

    @Override
    protected void configure() {
        switch (theme) {
            case LIGHT -> configureLight();
            case DARK  -> configureDark();
        }
    }

    private void configureLight() { /* add(...) calls for the light theme */ }
    private void configureDark()  { /* add(...) calls for the dark theme  */ }
}
```

`reconfigure()` discards every previously registered `(trait, styler)`
pair, calls `configure()` again, and fires an internal observable that
every component inside the `UI.use(..)` scope is subscribed to. The result
is an instant, comprehensive re-skin of the *entire* component tree —
without rebuilding a single component.

In the view, all you do is observe a `Var<Theme>` and forward it to the
sheet:

```java
Var<Theme>    theme = Var.of(Theme.LIGHT);
ThemedSheet   sheet = new ThemedSheet();
sheet.setTheme(theme.get());
Viewable.cast(theme).onChange(From.ALL, it -> sheet.setTheme(theme.get()));

UI.use(sheet, () ->
    UI.of(this).group(Skin.FRAME)
        // ... entire UI skeleton ...
        .add("shrinkx",
            comboBox(theme).group(Skin.THEME_PICKER)  // picking a theme triggers it
        )
);
```

Selecting a different theme in the combo box updates `theme`, which calls
`sheet.setTheme(..)`, which calls `reconfigure()`, which repaints
everything. No `if`s in the view, no per-component reconfiguration.

---

## A minimal end-to-end example ##

Putting it all together — under 60 lines of code:

```java
import static swingtree.UI.*;
import sprouts.From;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.style.StyleSheet;
import javax.swing.JButton;
import java.awt.Color;

enum Skin { PRIMARY, SECONDARY }
enum Theme { LIGHT, DARK }

final class DemoSheet extends StyleSheet {
    private Theme theme = Theme.LIGHT;
    public void setTheme(Theme t) { if (t != theme) { theme = t; reconfigure(); } }

    @Override protected void configure() {
        Color bg = theme == Theme.LIGHT ? new Color(245,245,247) : new Color(20,22,30);
        Color fg = theme == Theme.LIGHT ? new Color(20,22,30)    : new Color(245,245,247);
        Color accent = theme == Theme.LIGHT ? new Color(60,130,246) : new Color(255,180,80);

        add(type(JButton.class), it -> it
            .borderRadius(10).padding(8, 16, 8, 16).margin(4)
            .backgroundColor(bg).foregroundColor(fg)
        );
        add(type(JButton.class).group(Skin.PRIMARY), it -> it
            .backgroundColor(accent).foregroundColor(Color.WHITE)
        );
    }
}

public final class StyleSheetDemo {
    public static void main(String[] args) {
        Var<Theme> theme = Var.of(Theme.LIGHT);
        DemoSheet  sheet = new DemoSheet();
        sheet.setTheme(theme.get());
        Viewable.cast(theme).onChange(From.ALL, it -> sheet.setTheme(theme.get()));

        UI.use(sheet, () ->
            UI.show("Themed", f ->
                panel("fill, wrap 1, ins 24")
                .add("growx",
                    panel("fillx, ins 0")
                    .add(button("LIGHT").onClick(e -> theme.set(Theme.LIGHT)))
                    .add(button("DARK" ).onClick(e -> theme.set(Theme.DARK)))
                )
                .add("growx, gaptop 12",
                    panel("fillx, ins 0")
                    .add(button("Cancel").group(Skin.SECONDARY))
                    .add(button("OK"    ).group(Skin.PRIMARY))
                )
                .get(javax.swing.JPanel.class)
            )
        );
    }
}
```

Click `LIGHT` / `DARK` and the two buttons re-skin themselves
instantly — including the unselected default button colors — because the
sheet's `configure()` is re-run and every component inside `UI.use(..)`
listens for that.

---

## Where to next? ##

- The [Theme Garden](../../src/test/java/examples/zen/ThemeGardenView.java)
  example takes this idea to the limit: a music-player skeleton with five
  completely different visual identities, each driven from one
  [`ThemedStyleSheet`](../../src/test/java/examples/zen/ThemedStyleSheet.java).
- [Font Styling](./Font-Styling.md) and
  [Background Filtering](./Background-Filtering.md) document specific
  parts of the style API that are equally usable inside `withStyle` and
  inside style-sheet `add(..)` calls.
- [An Advanced Style Animation](./An-Advanced-Style-Animation.md) shows
  how to animate the *content* of a style; combine it with style sheets
  and you can animate transitions between themes too.
- [Functional MVVM](./Functional-MVVM.md) explains the property and
  `Viewable.cast(..)` patterns used in the snippet above.
- [Building a Look-and-Feel](./Building-A-Look-And-Feel.md) shows how to
  push styling one level deeper — into a custom `LookAndFeel` that uses the
  same style engine as `withStyle(..)` and `StyleSheet`, so an application
  built on top of it can still override your defaults through the sheet.