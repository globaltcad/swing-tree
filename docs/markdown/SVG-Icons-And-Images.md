
# SVG Icons and Images #

> **Prerequisites:** This guide assumes basic familiarity with SwingTree
> declarations (`UI.panel()`, `.add(..)`, `withStyle(..)`). If those are new
> to you, start with
> [Climbing the Swing Tree](./Climbing-Swing-Tree.md) first.

Plain Swing predates the era of vector graphics on the desktop, so its
`ImageIcon` world is entirely raster based — and raster icons are exactly
what falls apart on modern HiDPI displays. Scale a 16×16 PNG up to a 200%
display and you get mush.

SwingTree fixes this by supporting **SVG documents as first-class citizens
everywhere an icon or image can appear**: component icons, buttons, labels,
tabs, dialogs, table cells, and the style API's image layers. Under the hood
this is powered by the excellent
[JSVG](https://github.com/weisJ/jsvg) library, which renders SVG through
Java2D. Combined with SwingTree's
[HiDPI scaling system](./HiDPI-Scaling.md), an SVG icon is rendered
freshly at whatever scale it is displayed — so it stays crisp at
every DPI, every zoom level, and every component size.

There are three layers to the SVG/icon story, from most to least commonly
used:

1. **`IconDeclaration`** — a lightweight value object *describing* an icon,
   ideal for view models and bindings.
2. **`UI.findIcon(..)` / `UI.findSvgIcon(..)`** — cached loading of icons
   from the classpath or file system.
3. **`SvgIcon`** — the actual `ImageIcon` subclass that renders SVG, with
   fluent sizing/placement policies.

Let's look at each, and then at SVG in the style API.

---

## The Quick Path — Just Show an SVG ##

Every icon-accepting factory method understands `.svg` files.
Paths are resolved against the classpath first (e.g. `src/main/resources`),
then the file system:

```java
  UI.panel("wrap 1")
  .add(UI.icon("img/dandelion.svg"))                  // a JIcon; the SVG scales with the component
  .add(UI.icon(48, 48, "img/dandelion.svg"))          // fixed size (in developer pixels)
  .add(UI.label("Seeds").withIcon(() -> "img/seed.png"))
  .add(UI.button("Filter").withIcon(() -> "img/funnel.svg"));
```

Note that the `48, 48` above is in **developer pixels** — on a 200% scaled
display the icon is actually rendered at 96×96, and because it is vector
based it is re-rendered crisply instead of being upscaled.
(See [HiDPI Scaling](./HiDPI-Scaling.md) for the full story.)

This works, but hard-coding paths and eagerly loading `Icon` objects in your
UI code has real downsides — which brings us to icon *declarations*.

---

## `IconDeclaration` — Icons as Values ##

A traditional Swing `ImageIcon` is a heavyweight object: loading it may fail,
it drags in AWT, and it does not belong in a view model you want to unit
test. SwingTree's answer is `swingtree.api.IconDeclaration` — an immutable
**value object that merely describes where an icon comes from and how big it
should be**. The actual loading happens lazily (and cached) when the UI
resolves the declaration.

It is a functional interface with a single abstract method, `source()`,
so the simplest declaration is a lambda:

```java
  IconDeclaration funnel = () -> "img/funnel.svg";
```

The recommended pattern for real applications is a constants class or enum,
so all your icons live in one place:

```java
  public enum Icons implements IconDeclaration {
      FUNNEL("img/funnel.svg"),
      SEED("img/seed.png"),
      TREE("img/curvey-bubble-tree.svg");

      private final String path;

      Icons(String path) { this.path = path; }

      @Override public String source() { return path; }
  }

  // ...in your view:
  UI.button("Filter").withIcon(Icons.FUNNEL)
```

Declarations are immutable and come with withers for sizing:

```java
  IconDeclaration icon = IconDeclaration.of("img/dandelion.svg"); // natural size
  IconDeclaration big  = icon.withSize(64, 64);                   // scaled to 64×64
  IconDeclaration wide = icon.withWidth(64);                      // height stays flexible
```

### Programmatic SVG — no file needed ###

You can also declare an icon from a complete SVG document string,
which is great for small, self-contained icons generated in code:

```java
  IconDeclaration play = IconDeclaration.ofSvg(
      "<svg width='24' height='24' viewBox='0 0 24 24'>" +
      "  <path d='M8 5v14l11-7z' fill='currentColor'/>" +
      "</svg>"
  );

  UI.button(play).onClick( it -> mediaPlayer.play() );
```

There are two SVG-string factories, and the difference is what size the
resulting icon *reports*:

| Factory | Reported icon size |
|---|---|
| `IconDeclaration.ofSvg(svgText)` | The size declared inside the SVG text (`width='24' height='24'` → 24×24). |
| `IconDeclaration.ofAutoScaledSvg(svgText)` | Unknown (`-1`) — the icon adapts to whatever component it is painted into. |

Use `ofSvg` when the icon should behave like a regular fixed-size icon
(e.g. on a button), and `ofAutoScaledSvg` when it should stretch to fill its
component.

### Why declarations belong in view models ###

Because an `IconDeclaration` is just an immutable value, it is safe (and
encouraged) to put into properties and bind dynamically:

```java
  Var<IconDeclaration> statusIcon = Var.of(Icons.OFFLINE);

  UI.labelWithIcon(statusIcon)        // JLabel whose icon follows the property
  UI.buttonWithIcon(statusIcon)       // same for a JButton
  UI.icon(statusIcon)                 // same for a bare JIcon
  UI.menuItem("Connect", statusIcon)  // menu items too

  // later, from your business logic:
  statusIcon.set(Icons.ONLINE);       // the UI updates automatically
```

Your view model never touches an AWT class, stays trivially unit-testable,
and cannot fail at construction time just because an image file is missing —
if resolution fails, SwingTree logs it and simply renders nothing rather
than throwing (see [Sane Error Handling](./Sane-Error-Handling.md)).

---

## Loading and Caching — `UI.findIcon(..)` ##

When you *do* want an actual `Icon` object (e.g. for a
[dialog](./Simple-Dialogs.md) or an interop API), use:

```java
  Optional<ImageIcon> icon = UI.findIcon("img/dandelion.svg");   // SvgIcon if the file is .svg
  Optional<SvgIcon>   svg  = UI.findSvgIcon("img/dandelion.svg"); // typed variant, SVG only
  Optional<ImageIcon> any  = UI.findIcon(Icons.FUNNEL);           // also takes declarations
```

These search the classpath and file system and cache the result in the
SwingTree context (keyed by the `IconDeclaration` — see
`SwingTree.get().getIconCache()`), so repeated lookups are free. This cache
is also why the declaration-based APIs are preferred over constructing
`SvgIcon`s manually: equal declarations share one loaded icon.

---

## `SvgIcon` — the Rendering Workhorse ##

`swingtree.style.SvgIcon` is the `ImageIcon` subclass doing the actual
rendering. You rarely need to construct one yourself (prefer declarations
and `findIcon`), but it shines when the SVG **text itself is dynamic** —
for example an SVG editor, or server-delivered graphics:

```java
  import swingtree.style.SvgIcon;

  SvgIcon fromText   = SvgIcon.of(svgTextString);        // from a String
  SvgIcon fromRes    = SvgIcon.at("/img/dandelion.svg"); // from a classpath resource
  SvgIcon fromUrl    = SvgIcon.at(someUrl);              // from a URL
  SvgIcon fromStream = SvgIcon.of(inputStream);          // from a stream
```

Like everything in SwingTree, an `SvgIcon` is an **immutable value** — all
configuration happens through withers that return new instances:

```java
  SvgIcon icon = SvgIcon.of(svgText)
      .withIconSize(64, 64)          // fixed size (may distort aspect ratio)
      .withIconSizeFromWidth(64)     // width 64, height derived from aspect ratio
      .withIconSizeFromHeight(64)    // ...or the other way around
      .withFitComponent(UI.FitComponent.MIN_DIM)
      .withPreferredPlacement(UI.Placement.CENTER)
      .withOpacity(0.5f);
```

### The "no size" superpower ###

An SVG document has no inherently fixed size, and `SvgIcon` embraces that:
a dimension can be *unknown*, in which case `getIconWidth()` /
`getIconHeight()` return `-1` and the icon is rendered **according to the
size of the component it appears in**. What a given icon reports:

- An **explicit size** always wins — set through the withers below or
  through a declaration's `withSize(..)`.
- Constructed **directly** via `SvgIcon.at(..)` / `SvgIcon.of(..)`, the icon
  adopts the pixel-based `width`/`height` declared inside the SVG text and
  reports that (DPI-scaled). It reports `-1` only when those attributes are
  missing, percentage-based, or use units SwingTree does not resolve
  (`pt`, `em`, ...).
- Loaded through the **declaration pipeline** (`UI.icon(path)`,
  `UI.findIcon(..)`, `IconDeclaration.of(path)`), the icon is *reset to
  flexible*: a declaration's default preferred size is `Size.unknown()`,
  which deliberately clears the SVG's declared size so the icon reports
  `-1` and scales with its component. (This is also the difference between
  `IconDeclaration.ofSvg(..)` and `ofAutoScaledSvg(..)` from earlier.)

While a dimension is unknown, two policies control the rendering:

- **`UI.FitComponent`** — *how* the SVG stretches to the component:
  `NO` (natural size), `WIDTH`, `HEIGHT`, `WIDTH_AND_HEIGHT` (each may
  change the aspect ratio!), `MAX_DIM` / `MIN_DIM` (fit the larger/smaller
  component dimension **while preserving aspect ratio** — usually what you
  want), or `UNDEFINED` (defer to context defaults).
- **`UI.Placement`** — *where* it sits when it doesn't fill everything:
  `CENTER`, `TOP`, `LEFT`, `BOTTOM`, `RIGHT`, `TOP_LEFT`, `TOP_RIGHT`,
  `BOTTOM_LEFT`, `BOTTOM_RIGHT`, or `UNDEFINED`.

Both policies only kick in while the icon size is (partially) unknown —
once you give it a fixed width *and* height, it behaves like a regular icon.

Also worth knowing:

- `getSvgSize()` returns the size declared in the SVG document itself,
  and `getBaseWidth()` / `getBaseHeight()` the pre-DPI-scaling base size.
- `getImage()` rasterizes the SVG into a `BufferedImage` — handy for
  interop with APIs that require a bitmap (you lose scalability, of course).
- `getSvgDocument()` exposes the underlying JSVG `SVGDocument`.
- `withPercentageSizeResolvedAsPixels()` converts percentage-based SVG
  dimensions (e.g. `width="100%"`) into concrete pixels using the SVG's
  view box as the reference frame.

---

## SVG in the Style API ##

The [style API](./Climbing-Swing-Tree.md#blooming-flowers) can paint images
— including SVG — onto any component as part of its background or foreground
layers. The `image(..)` sub-style accepts SVG three ways:

```java
  UI.box().withMinSize(290, 220)
  .withStyle( conf -> conf
      .image( img -> img
          .svg(svgTextString)                    // 1. an SVG document string
          .fitMode(UI.FitComponent.MIN_DIM)
          .placement(UI.Placement.CENTER)
      )
      .border(12, UI.Color.LIGHTSTEELBLUE)
      .borderRadius(8)
  )
```

```java
  .image( img -> img.image(Icons.DANDELION) )    // 2. an IconDeclaration (SVG-aware)
  .image( img -> img.image(mySvgIcon) )          // 3. an SvgIcon / any ImageIcon
```

This API offers the most room for configurability:
Inside the `image(..)` sub-style you can further configure `opacity(..)`,
`size(..)`, `offset(..)`, `repeat(..)`, `autoFit(..)`, `primer(..)` (a fill
color painted beneath the image) and `clipTo(..)` (clip to
`ComponentArea.BODY`, `BORDER`, `INTERIOR`, ...) — so an SVG can serve as a
watermark, a decorative background, or a scalable illustration behind real
content. Which layer the image is painted on is chosen through the outer
overloads: `image(UI.Layer.BACKGROUND, img -> ...)`, and named images
(`image("my-image", img -> ...)`) let a later style rule override an
earlier one.

If the style depends on property state that can change at runtime (like the
SVG text in an editor), bind the property through
`withStyle(property, (svgText, it) -> ..)` — it hands the current item to the
style lambda and re-renders the component automatically on every change,
in a thread safe fashion.

---

## See It All Together — the SvgViewer Example ##

The runnable example
[`examples/stylish/SvgViewer.java`](../../src/test/java/examples/stylish/SvgViewer.java)
is a small SVG playground that demonstrates every mechanism from this guide
side by side. You type SVG text into a text area, pick a `Placement` and
`FitComponent` from combo boxes, and watch four tabs render the same SVG
through four different pipelines:

1. **Icon in Style API** — `SvgIcon.of(text)` passed to `.image(img -> img.image(icon))`.
2. **SVG String in Style API** — the raw text passed to `.image(img -> img.svg(text))`.
3. **Buffered Image** — the icon rasterized via `getImage()` first
   (notice how this one gets blurry when stretched — that's the difference
   vector rendering makes).
4. **Component Icon** — a `JIcon` whose icon is swapped through property
   change listeners.

Run its `main` method and play with the placement/fit combos — it is the
fastest way to build an intuition for how the two policies interact.

---

## Where to next? ##

- [HiDPI Scaling](./HiDPI-Scaling.md) — why "developer pixels" make SVG
  icons crisp on every display.
- [Simple Dialogs](./Simple-Dialogs.md) — dialogs accept custom (SVG) icons
  via `UI.findIcon(..)`.
- [Climbing the Swing Tree](./Climbing-Swing-Tree.md#blooming-flowers) —
  the style API that hosts the `image(..)` sub-style.
- [Sane Error Handling](./Sane-Error-Handling.md) — what happens when an
  icon fails to load (spoiler: not an exception in your face).
