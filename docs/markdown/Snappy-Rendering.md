
# Snappy Rendering — Caching & the Memory ⇄ CPU Trade-off #

> **TL;DR:** SwingTree renders the expensive, *stable* parts of your UI — styled
> backgrounds, borders, shadows, gradients, and noise — **once**, keeps the
> result in (often GPU-accelerated) image memory, and **blits** it on subsequent
> repaints instead of re-rendering from scratch. For flat-colored, bordered and
> shadowed styles it goes one step further and caches them **independently of the
> component size** (a "9-patch"), so even a live window resize re-renders nothing.
> That is what keeps resizes and animations smooth where plain Swing stutters. It
> is **on by default** (the `BALANCED` cache mode) and costs a modest,
> RAM-proportional amount of memory that you can dial up, dial down, or switch off
> entirely with a single setting — `SwingTree.get().setCacheMode(..)`.

## Why Swing has a reputation for being sluggish

Swing's reputation for low, stuttery frame rates is mostly earned by *how much
work it repeats*. On every single repaint — and a window resize or an animation
produces dozens of them per second — a classic Swing UI does the full job again:
the Look-and-Feel rebuilds a `TextLayout`, re-shapes every glyph and composites it
in software; rounded borders and drop shadows are re-tesselated; gradients and
procedural noise are re-sampled pixel by pixel. None of those inputs *changed*
between two frames of a resize, yet all of that CPU work is redone, on the Event
Dispatch Thread, while the user is waiting to see the next frame.

SwingTree attacks this directly. The insight is simple: **most of what a component
paints is a pure function of its style and size, and that rarely changes frame to
frame.** So SwingTree computes it once, stores the result, and on the next paint
just copies the pixels. A blit of an already-rendered image is *orders of
magnitude* cheaper than re-rendering it — and because these cached images are
"managed" images that Java2D keeps resident in video memory, the copy is typically
done by the GPU.

The payoff is most visible exactly where Swing usually falls apart: live window
resizes, style animations, and busy lists/tables of styled, text-bearing
components.

## What gets cached

All of SwingTree's caches are internal and automatic — you never construct or
manage them. They are:

| Cache | What it stores | Why it's expensive to recompute |
|---|---|---|
| **Style layers** | the fully rendered background/border/foundation image of a styled component | rounded corners, multiple gradient/shadow/image layers composited per paint |
| **Noise tiles** | pre-rendered tiles of a procedural noise paint, in offset-independent *noise space* | per-pixel noise sampling through the `Paint` pipeline |
| **Shadow gradients** | the blended colour-stop arrays of a drop/inner shadow | blending the falloff curve into dozens of gradient stops |
| **Text layouts** | `TextLayout`s and paragraph line-break data for rich/wrapped text | line-breaking and attribute setup over the whole paragraph |

## It is automatic — *and* always correct

You do not invalidate anything. Every cache is keyed by the **immutable
configuration** that produced the cached result (the style, the box model, the
text appearance, …). When a component's configuration changes, it simply looks up
a *different* key — a cache miss — and the stale entry, no longer referenced by
anything live, is reclaimed on its own. Two components with an identical
appearance automatically *share* one cached image.

This means caching can never change *what* you see — anything not found in a cache
is simply rendered directly. It only changes *how often* the expensive path runs.
There is no staleness to reason about and no cache to flush.

## Resizing for free: size-independent style caching

Keying a cache on the configuration has one nasty consequence: **the component
size is part of that configuration.** A cached image of a 300×80 button is simply
not the image of a 301×80 button. So the moment a user grabs a window edge and
drags, every styled component in that window changes its key on *every frame*, the
cache misses on every frame, and the style cache — precisely in the scenario it
exists for — degrades into pure overhead. This is the classic weakness of
image-based render caching, and it is why naive "just cache the painted component"
schemes disappoint in practice.

SwingTree's answer is to notice that **most real styles do not actually change
their pixels when the component grows — they only get *more of the same
pixels*.** Take a rounded, shadowed panel with a flat fill. Stretch it wider and
the corners are untouched, the top and bottom edges just repeat their band
horizontally, the left and right edges repeat theirs vertically, and the middle
is one flat color. Only nine *regions* exist, and only five of them stretch.

So for such styles SwingTree does not cache the component's rendering at all. It
renders a **minimal exemplar** of the style — the smallest component at which
every size-dependent pixel still exists, typically a few dozen pixels square —
and caches *that*. At paint time, the real component size is reassembled from
that little image with nine blits: the four corners copied 1:1, the four edge
bands stretched along their own axis, the center stretched in both. Cutting an
image into a 3×3 grid with fixed corners and stretchable edges is exactly the
technique behind **Android's 9-patch drawables** and **CSS `border-image-slice`**,
which is where the name comes from.

Three things follow, and they are the whole point:

1. **A resize is no longer a cache miss.** Every size of a style maps onto the
   *same* cached exemplar, so dragging a window edge re-renders nothing at all.
   A component can grow, shrink, be dragged to an extreme aspect ratio and back,
   and the style renderer is never invoked again.
2. **Differently sized components share one cached image.** A toolbar of twenty
   buttons with one common style used to need twenty cache entries (one per
   size); now the first one to paint fills a single shared entry and the other
   nineteen hit it on their first paint.
3. **Size stops gating what can be cached at all.** The cache refuses to spend
   memory on very large images, so a full-screen styled panel was historically
   *too big to cache* and re-rendered forever. A tiling style's memory cost is
   the exemplar, not the component — so a 3000×1500 panel now costs the same few
   kilobytes as a small one, and gets cached eagerly on its first paint.

### The invariant, and what is *not* tiled

Stretching is only truthful for a style whose body is homogeneous and whose edges
are homogeneous along their own axis. SwingTree therefore tiles **flat background
and foundation colors, borders, and shadows** (inset and outset) — the
overwhelming bulk of real styles — and deliberately refuses everything whose
pixels genuinely depend on the full component bounds:

| Not tiled | Why |
|---|---|
| gradients | the gradient geometry spans the whole component |
| noise | procedural noise varies with every pixel position |
| background images | placement and fit are relative to the component bounds |
| styled text | text is laid out within the component bounds |
| custom painters | SwingTree cannot know what your painter does |
| per-edge border **colors** *combined with* rounded corners | the diagonal miter seam between two differently colored edges runs toward the component center, so its slope inside a rounded corner follows the aspect ratio |
| components smaller than the exemplar | there is no room to stretch; the corners would overlap |

Anything on that list silently falls back to the classic exact-size cache key and
behaves exactly as it did before — cached, but re-rendered on resize. The rule is
kept deliberately conservative, because an over-eager one would not crash, it
would draw *subtly wrong pixels*. Note also that eligibility is decided **per
style layer**: a panel with a gradient background and a shadow tiles its shadow
layer and full-size-caches its background layer.

### How other stacks solve the same problem

Every mature rendering stack hits this wall, and they all landed on some variation
of the same idea:

- **Browsers** keep a display list and re-*raster* changed tiles rather than
  re-painting whole layers; and where they can, they avoid raster entirely — a
  scaled or resized composited layer is re-composited by the GPU from the same
  texture. Chromium goes further with a *nine-patch display item* for box-shadows
  specifically: it rasters one small shadow corner and stretches it, for exactly
  the reason SwingTree does.
- **Android** ships 9-patch as a first-class asset format (`.9.png`), with the
  stretchable regions marked by hand in the image's border pixels. SwingTree's
  twist is that nobody marks anything: the stretchable regions are *derived* from
  the style configuration (margin, border widths, corner arcs, shadow blur and
  spread reach), so it applies to styles you declare in code, not to artwork you
  ship.
- **Qt** exposes the same primitive as `qDrawBorderPixmap` / `QSGNinePatchNode`,
  and its style sheets use `border-image` with the CSS slicing semantics.
- **WPF / macOS AppKit** rely on resolution-independent vector redraw plus
  retained visual/layer trees — the closest analogue to "don't re-render on
  resize" is layer re-composition rather than nine-slicing.

What is unusual about SwingTree here is not the technique but *where it sits*: it
is not an asset format or an API you draw with, it is an **automatic property of
the render cache**, inferred from your declarative style. You write
`.borderRadius(16).backgroundColor(..).shadowBlurRadius(8)` and resizing becomes
free, without knowing any of this exists.

### Turning it off

It is **on by default**. Because it reconstructs pixels rather than re-rendering
them, it also ships with a safety hatch — if you ever suspect a rendering artifact
comes from the reconstruction, take it out of the picture:

```java
// at startup:
SwingTree.initializeUsing( cfg -> cfg.withCacheTilingEnabled(false) );

// or at runtime (takes effect immediately; the caches repopulate under the new policy):
SwingTree.get().setCacheTilingEnabled(false);
boolean on = SwingTree.get().isCacheTilingEnabled();
```

Or without touching code:

```
-Dswingtree.cacheMode.tiling=false
```

Switching it off does not disable caching — it restores the classic exact-size
cache keys, so styles are still cached, they just re-render on every resize.
Rendering stays correct either way; it only gets slower. The dev tools expose the
same switch as a **9 patch tiling** checkbox, so you can A/B it on a live UI.

## The main knob: `CacheMode`

A single enum governs how aggressively *all* of the caches trade memory for CPU
time. It lives on the library context and can be changed at any time.

| Mode | Posture | Roughly comparable to |
|---|---|---|
| `DISABLED` | every cache off; always render directly | a strict memory budget / debugging a render issue |
| `CONSERVATIVE` | minimal memory, still caches the hottest results | embedded / kiosk / low-RAM devices |
| `BALANCED` ⭐ | **the default** — smooth without being greedy | a good-citizen desktop app |
| `GENEROUS` | keeps more in memory for extra smoothness | animation-heavy or content-dense UIs |
| `AGGRESSIVE` | maximises retained results | a workstation app where smoothness trumps RAM |

### Setting it

At startup, through the initializer (American spelling, like the rest of the API):

```java
import swingtree.SwingTree;
import swingtree.SwingTreeInitConfig.CacheMode;

SwingTree.initializeUsing( cfg -> cfg.withCacheMode(CacheMode.GENEROUS) );
```

At runtime — it takes effect on the next paint, and *lowering* it frees the
now-excess memory **immediately** (the caches shrink on the spot, they don't wait
to evict lazily):

```java
SwingTree.get().setCacheMode(CacheMode.CONSERVATIVE);
CacheMode current = SwingTree.get().getCacheMode();
```

Or without touching code at all, via a system property:

```
-Dswingtree.cacheMode=AGGRESSIVE
```

## How much memory does it actually cost?

The mode does not pick a fixed number of megabytes — it picks a **fraction of
physical RAM, clamped between a floor and a cap**, so the same mode scales
sensibly from a 2 GB tablet to a 256 GB workstation. That total budget is then
partitioned across the four caches.

| Mode | Share of RAM | Floor | Cap (real-world anchor) |
|---|---|---|---|
| `DISABLED` | 0% | — | 0 MB |
| `CONSERVATIVE` | 0.25% | 8 MB | 64 MB |
| `BALANCED` | 0.5% | 16 MB | 128 MB — *a light browser tab* |
| `GENEROUS` | 1% | 32 MB | 256 MB — *a heavy browser tab* |
| `AGGRESSIVE` | 2% | 64 MB | 512 MB — *a heavy Electron app* |

So the default `BALANCED` mode tops out around the memory of a single light
browser tab, and even the all-out `AGGRESSIVE` mode stays well under what a
typical Electron app spends — SwingTree remains the *lightweight* native option.
Concretely, the total budget at `BALANCED` works out to roughly:

| Physical RAM | `BALANCED` total budget |
|---|---|
| 4 GB | ~20 MB |
| 8 GB | ~41 MB |
| 16 GB | ~82 MB |
| 32 GB and up | 128 MB (capped) |

A crucial detail: **these are ceilings on retention, not pre-allocations.** A
cache only ever holds what your components actually produce, up to its slice of
the budget. A small app on a big machine therefore costs very little even though
its ceiling is high — the budget just prevents a large or animation-heavy UI from
growing without bound.

## What about text rendering?

Text needs no cache of its own: SwingTree keeps component fonts free of layout
attributes (a solid font color travels through the component *foreground*, not
the font), which keeps every measure and draw on the JDK's fast glyph-cache
path. Only genuinely layout-hungry text — gradient-painted, letter-spaced,
underlined — pays the slower `TextLayout` route, and such text is a decorative
accent in real UIs, not the bulk.

## Tune it live in the dev tools

You don't have to guess. SwingTree ships browser-style developer tools: press
**`Ctrl + Shift + I`** over a running SwingTree window (the shortcut is
configurable, and you can also enable them with
`SwingTree.get().setDevToolEnabled(true)`). At the very top of the dev-tool window
is a collapsed **SwingTree Library Settings** panel — drag the divider down to
reveal it. There you'll find a live **Cache mode** selector, a **9 patch tiling**
checkbox, and a **live cache entries** readout covering every global rendering
cache (style layers, text layouts, noise paints, shadow gradients), so you can
flip between settings and watch the effect on a running UI in real time (set the
mode to `DISABLED` and watch the counts drop to zero; untick *9 patch tiling* and
watch a resize start re-rendering again).

## Choosing a mode

- **Just shipping an app?** Leave it on `BALANCED`. It is the default for a
  reason — smooth, and a good memory citizen.
- **Memory-constrained target** (kiosk, embedded, a tablet, many simultaneous
  app instances)? Drop to `CONSERVATIVE`, or `DISABLED` if RAM is truly scarce.
- **Lots of style animation, frosted-glass filters, big resizable windows, or
  long styled lists/tables?** Move up to `GENEROUS` (or `AGGRESSIVE` on a
  workstation) for the extra headroom.
- **Chasing a rendering bug?** Temporarily set `DISABLED` to take every cache out
  of the picture and confirm the direct-render path looks identical.

You can also adapt at runtime — e.g. start `GENEROUS` and drop to `CONSERVATIVE`
when the OS signals memory pressure.

## Checklist

- ✅ Do nothing and you already get smooth, cached rendering at `BALANCED`, with
  free resizing for flat/bordered/shadowed styles.
- ✅ Pick a `CacheMode` to match your target — `withCacheMode(..)` at startup,
  `SwingTree.get().setCacheMode(..)` at runtime, or `-Dswingtree.cacheMode=..`.
- ✅ Trust correctness: caches are pure derived data keyed by immutable config, so
  they self-invalidate and never change what you see.
- ✅ Lower the mode to release memory *immediately*; raise it for extra smoothness.
- ✅ Want a resize to be free? Keep the style's *edges* size-independent — a flat
  fill, border and shadow tile; a gradient or background image does not.
- 🛠️ Use `Ctrl + Shift + I` → *SwingTree Library Settings* to experiment with
  modes and 9-patch tiling on a live UI.
- ⚠️ `DISABLED` is a correctness-neutral way to rule caching out while debugging —
  it only makes rendering do more work, never different work. The same goes for
  `setCacheTilingEnabled(false)` (`-Dswingtree.cacheMode.tiling=false`), which
  rules out just the 9-patch reconstruction.
