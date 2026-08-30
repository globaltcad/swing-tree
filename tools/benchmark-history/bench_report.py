#!/usr/bin/env python3
"""
Turns the compiled 'history.json' into a single self-contained HTML report.

The report answers one question: which commits in the measured range moved which numbers,
and by how much compared to how noisy the measurement itself was. It therefore plots the
spread of the repeated measurements alongside the median, because a 5% step between two
commits means nothing if the same commit measured 15% apart across rounds.

No network access, no external files: the data is embedded and the charts are drawn by a
small inline script, so the file can be mailed, committed, or opened offline.
"""

from __future__ import annotations

import json

#  The example UIs in the order the benchmark measures them, so the report does not
#  re-order them alphabetically and make two runs harder to compare side by side. The seven
#  look and feel presets in the middle are one and the same view, which is why they are kept
#  adjacent: read side by side they say what the styling costs, with everything else equal.
VIEW_ORDER = [
    "chat", "sequencer", "trains", "team", "scribe", "almanack", "garden",
    "glass", "soft", "bevel",
    "linen", "glassmorphic", "skeuomorphic", "softui", "aero", "material", "flat",
    "studio", "budget", "breathing",
]

#  Which measures are drawn, and in which colour. The mapping is by *what is measured*
#  rather than by position, so 'repaint after a size change' is the same colour in the
#  resize charts and in the noise charts, and adding a facet never repaints another one.
#
#  Only three series are ever drawn in one facet: these are small multiples, where every
#  pair of colours has to be separable (not just neighbouring ones), and the reference
#  palette clears that bar for its first three slots only.
SERIES = {
    "layout":       {"label": "re-layout",            "short": "layout",      "slot": 1},
    "paint_resize": {"label": "repaint, caches cold", "short": "paint cold",  "slot": 2},
    "resizing":     {"label": "repaint, caches cold", "short": "paint cold",  "slot": 2},
    "paint_static": {"label": "repaint, caches warm", "short": "paint warm",  "slot": 3},
    "static":       {"label": "repaint, caches warm", "short": "paint warm",  "slot": 3},
}
#  Measured, reported in the tables, but not drawn: 'drag' is the sum of two series that
#  are already on the chart, and 'clip' is an order of magnitude smaller than the rest and
#  would sit flat on the axis.
TABLE_ONLY = ["clip", "drag"]
TABLE_ONLY_LABELS = {"clip": "clip 300px", "drag": "drag frame"}

FACET_TITLES = {
    "noise/opaque": "Noise gradient, opaque",
    "noise/translucent": "Noise gradient, translucent",
    "gradient/cached": "Rounded gradient, cache on",
    "gradient/uncached": "Rounded gradient, cache off",
}

#  The look and feel showcase is measured under seven presets. Each note says what that
#  preset actually puts on a style layer, because that - not the picture - is what decides
#  which of the renderer's paths the view exercises.
SHOWCASE_NOTES = {
    "linen":        "The look and feel showcase under the Linen preset: flat cream surfaces, "
                    "taupe borders, and a woven noise grain on the window background.",
    "glassmorphic": "The showcase under Glassmorphism: nothing opaque, every surface a "
                    "translucent wash over a blurred parent, with a wide soft shadow.",
    "skeuomorphic": "The showcase under Skeuomorphism: a grain, a vertical gradient and a bevel "
                    "on every raised control, which is the most layered of the presets.",
    "softui":       "The showcase under Soft UI: no borders and no gradients, only generous "
                    "radii and a paired highlight and shadow - shadow-dominated.",
    "aero":         "The showcase under Frutiger Aero: saturated fills under a hard gloss "
                    "gradient, sky gradients behind everything, outlines and drop shadows.",
    "material":     "The showcase under Material: flat fills, a small 4px radius, and depth "
                    "said entirely with elevation shadows - no gradient anywhere.",
    "flat":         "The showcase under Flat design: no shadow, no gradient, no bevel and no "
                    "rounded corner - the cheapest thing the style engine can be asked to draw.",
}

FACET_NOTES = {
    "noise/opaque": "A large FABRIC noise over the whole component, 1100..1600 x 1000.",
    "noise/translucent": "The same noise with a translucent colour, which denies it the opaque blit.",
    "gradient/cached": "A rounded panel with a radial gradient - rounded denies it the "
                       "antialiasing-free path, the gradient denies it a size independent cache.",
    "gradient/uncached": "The same panel with the render cache switched off, i.e. the cost the "
                         "cache exists to avoid.",
}
FACET_NOTES.update({f"resize/{view}": note for view, note in SHOWCASE_NOTES.items()})


def facet_key(metric: dict) -> str:
    return f"{metric['benchmark']}/{metric['group']}" if metric["group"] else metric["benchmark"]


def build_facets(history: dict) -> list[dict]:
    """
    Groups the metrics into one chart per measured subject: one per example UI for the
    resize benchmark, one per variant for the other two.
    """
    commits = history["commits"]
    by_facet: dict[str, list[dict]] = {}
    for metric in history["metrics"]:
        by_facet.setdefault(facet_key(metric), []).append(metric)

    def facet_sort_key(key: str) -> tuple:
        benchmark, _, group = key.partition("/")
        order = {"resize": 0, "noise": 1, "gradient": 2}.get(benchmark, 3)
        if benchmark == "resize" and group in VIEW_ORDER:
            return (order, VIEW_ORDER.index(group))
        return (order, group)

    facets = []
    for key in sorted(by_facet, key=facet_sort_key):
        metrics = by_facet[key]
        drawn = [m for m in metrics if m["measure"] in SERIES]
        drawn.sort(key=lambda m: SERIES[m["measure"]]["slot"])
        if not drawn:
            continue
        extra = [m for m in metrics if m["measure"] in TABLE_ONLY]
        extra.sort(key=lambda m: TABLE_ONLY.index(m["measure"]))

        series = []
        for metric in drawn:
            points = []
            for commit in commits:
                stat = commit["stats"].get(metric["id"])
                points.append(
                    None if not stat else {
                        "v": stat["median"], "lo": stat["min"], "hi": stat["max"], "n": stat["n"]
                    }
                )
            series.append({
                "id": metric["id"],
                "label": SERIES[metric["measure"]]["label"],
                "short": SERIES[metric["measure"]]["short"],
                "slot": SERIES[metric["measure"]]["slot"],
                "points": points,
            })

        table_series = []
        for metric in extra:
            points = []
            for commit in commits:
                stat = commit["stats"].get(metric["id"])
                points.append(None if not stat else {"v": stat["median"], "n": stat["n"]})
            table_series.append({
                "id": metric["id"], "label": metric["label"],
                "short": TABLE_ONLY_LABELS.get(metric["measure"], metric["measure"]),
                "points": points,
            })

        benchmark, _, group = key.partition("/")
        geometry = ""
        for commit in commits:
            box = commit.get("context", {}).get(group)
            if box:
                geometry = f"{box['width']} x {box['height']}"
                break

        facets.append({
            "id": key,
            "title": FACET_TITLES.get(key, group or benchmark),
            "note": FACET_NOTES.get(key, ""),
            "geometry": geometry,
            "series": series,
            "table_series": table_series,
        })
    return facets


def headline(facets: list[dict]) -> dict:
    """
    The one number the report leads with: the median across every drawn series of how the
    last commit compares with the first. A median rather than a mean, because one series
    that halved would otherwise speak for all of them.
    """
    ratios = []
    for facet in facets:
        for series in facet["series"]:
            points = series["points"]
            first = next((p for p in points if p), None)
            last = next((p for p in reversed(points) if p), None)
            if first and last and first["v"] > 0:
                ratios.append(last["v"] / first["v"])
    if not ratios:
        return {"ratio": None, "count": 0}
    ratios.sort()
    middle = len(ratios) // 2
    median = (ratios[middle] if len(ratios) % 2 else (ratios[middle - 1] + ratios[middle]) / 2)
    return {"ratio": median, "count": len(ratios)}


def render_report(history: dict) -> str:
    facets = build_facets(history)
    commits = [
        {
            "index": c["index"], "short": c["short"], "sha": c["sha"],
            "subject": c["subject"], "date": c["author_date"][:10],
            "warnings": c.get("warnings", []),
            "failed": len(c.get("failed_runs", [])),
        }
        for c in history["commits"]
    ]
    payload = {
        "commits": commits,
        "facets": facets,
        "meta": history.get("meta", {}),
        "environment": history.get("environment", {}),
        "generated": history.get("generated", ""),
    }
    lead = headline(facets)
    rounds = history.get("meta", {}).get("rounds_completed") or history.get("meta", {}).get("rounds", 0)
    sample_note = (
        f"median of {rounds} interleaved measurement rounds per commit"
        if rounds else "single measurement round per commit"
    )
    if lead["ratio"] is None:
        hero_value, hero_note = "n/a", "no comparable series"
    else:
        change = (lead["ratio"] - 1) * 100
        hero_value = f"{change:+.1f}%"
        hero_note = (
            f"median change across all {lead['count']} measured series, "
            f"first commit to last"
        )

    environment = history.get("environment", {})
    meta = history.get("meta", {})
    subtitle_bits = [
        f"{len(commits)} commits",
        f"{meta.get('from_short', '?')} &rarr; {meta.get('to_short', '?')}",
        sample_note,
    ]

    data_json = json.dumps(payload, separators=(",", ":"))

    return _TEMPLATE.format(
        data_json=data_json,
        hero_value=hero_value,
        hero_note=_escape(hero_note),
        hero_class=("good" if lead["ratio"] and lead["ratio"] < 1 else
                    "bad" if lead["ratio"] and lead["ratio"] > 1 else "flat"),
        subtitle=" &middot; ".join(subtitle_bits),
        generated=_escape(history.get("generated", "")),
        env_java=_escape(environment.get("java", "unknown")),
        env_host=_escape(f"{environment.get('cpu', 'unknown cpu')}, "
                         f"{environment.get('os', '')}, display {environment.get('display', '?')}"),
    )


def _escape(text: str) -> str:
    return (str(text).replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace('"', "&quot;"))


_TEMPLATE = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>SwingTree benchmark history</title>
<style>
  :root {{
    color-scheme: light;
    --plane:          #f9f9f7;
    --surface:        #fcfcfb;
    --text-primary:   #0b0b0b;
    --text-secondary: #52514e;
    --text-muted:     #898781;
    --grid:           #e1e0d9;
    --axis:           #c3c2b7;
    --border:         rgba(11,11,11,0.10);
    --series-1:       #2a78d6;
    --series-2:       #eb6834;
    --series-3:       #1baf7a;
    --good:           #006300;
    --bad:            #d03b3b;
  }}
  @media (prefers-color-scheme: dark) {{
    :root:where(:not([data-theme="light"])) {{
      color-scheme: dark;
      --plane:          #0d0d0d;
      --surface:        #1a1a19;
      --text-primary:   #ffffff;
      --text-secondary: #c3c2b7;
      --text-muted:     #898781;
      --grid:           #2c2c2a;
      --axis:           #383835;
      --border:         rgba(255,255,255,0.10);
      --series-1:       #3987e5;
      --series-2:       #d95926;
      --series-3:       #199e70;
      --good:           #0ca30c;
      --bad:            #d03b3b;
    }}
  }}
  :root[data-theme="dark"] {{
    color-scheme: dark;
    --plane:          #0d0d0d;
    --surface:        #1a1a19;
    --text-primary:   #ffffff;
    --text-secondary: #c3c2b7;
    --text-muted:     #898781;
    --grid:           #2c2c2a;
    --axis:           #383835;
    --border:         rgba(255,255,255,0.10);
    --series-1:       #3987e5;
    --series-2:       #d95926;
    --series-3:       #199e70;
    --good:           #0ca30c;
    --bad:            #d03b3b;
  }}

  * {{ box-sizing: border-box; }}
  body {{
    margin: 0; padding: 32px 24px 64px;
    background: var(--plane); color: var(--text-primary);
    font: 15px/1.55 system-ui, -apple-system, "Segoe UI", sans-serif;
  }}
  .wrap {{ max-width: 1220px; margin: 0 auto; }}
  h1 {{ font-size: 22px; font-weight: 650; margin: 0 0 4px; letter-spacing: -0.01em; }}
  .sub {{ color: var(--text-secondary); font-size: 13.5px; margin-bottom: 24px; }}
  .env {{ color: var(--text-muted); font-size: 12.5px; margin-top: 6px; }}

  .lead {{
    display: flex; flex-wrap: wrap; gap: 28px; align-items: baseline;
    background: var(--surface); border: 1px solid var(--border); border-radius: 10px;
    padding: 22px 24px; margin-bottom: 22px;
  }}
  .hero {{ font-size: 52px; font-weight: 650; line-height: 1; letter-spacing: -0.02em; }}
  .hero.good {{ color: var(--good); }}
  .hero.bad {{ color: var(--bad); }}
  .hero-note {{ color: var(--text-secondary); font-size: 13.5px; max-width: 44ch; }}

  .controls {{
    display: flex; flex-wrap: wrap; gap: 10px; align-items: center;
    margin: 0 0 18px;
  }}
  .controls label {{ color: var(--text-secondary); font-size: 13px; margin-right: 2px; }}
  .seg {{ display: inline-flex; border: 1px solid var(--border); border-radius: 8px; overflow: hidden; }}
  .seg button {{
    appearance: none; border: 0; background: var(--surface); color: var(--text-secondary);
    font: inherit; font-size: 13px; padding: 6px 13px; cursor: pointer;
  }}
  .seg button[aria-pressed="true"] {{ background: var(--series-1); color: #fff; }}
  .seg button + button {{ border-left: 1px solid var(--border); }}

  .legend {{ display: flex; flex-wrap: wrap; gap: 18px; margin: 0 0 18px; font-size: 13px;
             color: var(--text-secondary); }}
  .legend span {{ display: inline-flex; align-items: center; gap: 7px; }}
  .key {{ width: 16px; height: 3px; border-radius: 2px; display: inline-block; }}

  .grid {{ display: grid; grid-template-columns: repeat(auto-fill, minmax(330px, 1fr)); gap: 16px; }}
  .facet {{
    background: var(--surface); border: 1px solid var(--border);
    border-radius: 10px; padding: 14px 14px 8px;
  }}
  .facet h2 {{ font-size: 14.5px; font-weight: 620; margin: 0 0 2px; }}
  .facet .meta {{ color: var(--text-muted); font-size: 12px; margin: 0 0 8px; }}
  .facet .delta {{ float: right; font-size: 12.5px; font-weight: 600;
                   font-variant-numeric: tabular-nums; }}
  .delta.good {{ color: var(--good); }}
  .delta.bad {{ color: var(--bad); }}
  .delta.flat {{ color: var(--text-muted); }}
  svg {{ display: block; width: 100%; height: auto; overflow: visible; }}

  .tip {{
    position: fixed; pointer-events: none; z-index: 20; opacity: 0;
    background: var(--surface); color: var(--text-primary);
    border: 1px solid var(--border); border-radius: 8px; padding: 9px 11px;
    font-size: 12.5px; line-height: 1.45; max-width: 320px;
    box-shadow: 0 6px 22px rgba(0,0,0,0.16); transition: opacity .08s;
  }}
  .tip b {{ font-weight: 620; }}
  .tip .row {{ display: flex; gap: 8px; align-items: center;
               font-variant-numeric: tabular-nums; }}
  .tip .row .key {{ flex: none; }}
  .tip .row .val {{ margin-left: auto; }}
  .tip .subject {{ color: var(--text-secondary); margin-top: 3px; }}

  details {{ margin-top: 26px; background: var(--surface); border: 1px solid var(--border);
             border-radius: 10px; padding: 12px 16px; }}
  summary {{ cursor: pointer; font-weight: 600; font-size: 14px; }}
  table {{ border-collapse: collapse; width: 100%; margin-top: 12px; font-size: 12.5px;
           font-variant-numeric: tabular-nums; }}
  th, td {{ text-align: right; padding: 5px 8px; border-bottom: 1px solid var(--grid);
            white-space: nowrap; }}
  th:first-child, td:first-child, th.left, td.left {{ text-align: left; }}
  th {{ color: var(--text-secondary); font-weight: 600; }}
  td.subject {{ text-align: left; white-space: normal; color: var(--text-secondary);
                min-width: 30ch; max-width: 46ch; }}
  /* Wide content scrolls inside its own box; the page itself never scrolls sideways. */
  .scroll {{ overflow-x: auto; }}
  caption {{ text-align: left; color: var(--text-secondary); font-size: 13px;
             padding: 10px 0 0; font-weight: 600; }}
  .foot {{ color: var(--text-muted); font-size: 12.5px; margin-top: 28px; }}
</style>
</head>
<body>
<div class="wrap">
  <h1>SwingTree benchmark history</h1>
  <div class="sub">{subtitle}</div>

  <div class="lead">
    <div>
      <div class="hero {hero_class}">{hero_value}</div>
    </div>
    <div class="hero-note">
      {hero_note}. Negative is faster. Every point is a median; the shaded band around
      a line is the spread between the fastest and slowest round measured for that commit,
      so a step smaller than the band is not a result.
    </div>
  </div>

  <div class="controls">
    <label>Scale</label>
    <div class="seg" role="group" aria-label="Value scale">
      <button id="mode-abs"  aria-pressed="true">Milliseconds</button>
      <button id="mode-rel"  aria-pressed="false">% of first commit</button>
    </div>
  </div>

  <div class="legend" id="legend"></div>
  <div class="grid" id="charts"></div>

  <details id="tables">
    <summary>All measured values, per commit</summary>
    <div id="tables-body"></div>
  </details>

  <div class="foot">
    Generated {generated} &middot; {env_java} &middot; {env_host}
  </div>
</div>

<div class="tip" id="tip" role="status" aria-live="polite"></div>

<script id="data" type="application/json">{data_json}</script>
<script>
(function () {{
  const DATA    = JSON.parse(document.getElementById('data').textContent);
  const commits = DATA.commits;
  const facets  = DATA.facets;
  const tip     = document.getElementById('tip');

  const W = 340, H = 182, PAD_L = 44, PAD_R = 12, PAD_T = 10, PAD_B = 22;
  const PLOT_W = W - PAD_L - PAD_R, PLOT_H = H - PAD_T - PAD_B;
  const SVG_NS = 'http://www.w3.org/2000/svg';

  let mode  = 'abs';     // 'abs' = milliseconds, 'rel' = % of the first commit
  let hover = null;      // the commit index every facet currently highlights

  const colorOf = slot => `var(--series-${{slot}})`;

  function el(name, attrs, parent) {{
    const node = document.createElementNS(SVG_NS, name);
    for (const key in attrs) node.setAttribute(key, attrs[key]);
    if (parent) parent.appendChild(node);
    return node;
  }}

  /* The value a series shows under the current scale: milliseconds, or the same number
     as a percentage of what the first commit measured - which is the only way series of
     very different absolute cost can share one axis. */
  function scaled(series, point) {{
    if (!point) return null;
    if (mode === 'abs') return {{ v: point.v, lo: point.lo, hi: point.hi }};
    const base = series.base;
    if (!base) return null;
    return {{ v: 100 * point.v / base, lo: 100 * point.lo / base, hi: 100 * point.hi / base }};
  }}

  for (const facet of facets)
    for (const series of facet.series) {{
      const first = series.points.find(p => p);
      series.base = first ? first.v : 0;
    }}

  /* The axis top is four clean steps rather than a round number of its own, so that the
     four gridlines land on values a reader can hold in their head (0, 5, 10, 15, 20) instead
     of the quarters of a round top (0, 12.5, 25, 37.5, 50). */
  const AXIS_TICKS = 4;
  function niceStep(peak) {{
    if (peak <= 0) return 0.25;
    const raw = peak / AXIS_TICKS;
    const magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
    for (const step of [1, 1.5, 2, 2.5, 3, 4, 5, 7.5, 10])
      if (step * magnitude >= raw) return step * magnitude;
    return 10 * magnitude;
  }}

  const x = i => PAD_L + (commits.length === 1 ? PLOT_W / 2
                          : PLOT_W * i / (commits.length - 1));

  function drawFacet(facet) {{
    const svg = facet.svg;
    while (svg.firstChild) svg.removeChild(svg.firstChild);

    let peak = 0;
    for (const series of facet.series)
      for (const point of series.points) {{
        const s = scaled(series, point);
        if (s) peak = Math.max(peak, s.hi);
      }}
    const step = niceStep(peak * 1.06);
    const top = step * AXIS_TICKS;
    const y = value => PAD_T + PLOT_H - PLOT_H * (value / top);

    /* Gridlines first, so every data mark sits above them. */
    for (let tick = 0; tick <= AXIS_TICKS; tick++) {{
      const value = step * tick;
      el('line', {{ x1: PAD_L, x2: W - PAD_R, y1: y(value), y2: y(value),
                   stroke: tick === 0 ? 'var(--axis)' : 'var(--grid)', 'stroke-width': 1 }}, svg);
      const label = el('text', {{ x: PAD_L - 7, y: y(value) + 3.5, 'text-anchor': 'end',
                                fill: 'var(--text-muted)', 'font-size': 9.5 }}, svg);
      label.textContent = formatTick(value, step) + (mode === 'abs' ? '' : '%');
    }}
    /* The 100% reference in relative mode: where every series starts. */
    if (mode === 'rel' && top >= 100)
      el('line', {{ x1: PAD_L, x2: W - PAD_R, y1: y(100), y2: y(100),
                   stroke: 'var(--axis)', 'stroke-width': 1, 'stroke-dasharray': '0' }}, svg);

    for (let i = 0; i < commits.length; i++) {{
      if (commits.length > 12 && i % 3 !== 0 && i !== commits.length - 1) continue;
      const label = el('text', {{ x: x(i), y: H - 6, 'text-anchor': 'middle',
                                fill: 'var(--text-muted)', 'font-size': 9.5 }}, svg);
      label.textContent = i;
    }}

    for (const series of facet.series) {{
      const scaledPoints = series.points.map(p => scaled(series, p));
      /* The spread band: a wash of the series hue between the fastest and slowest round.
         It is the honest error bar of this measurement and belongs under the line. */
      const upper = [], lower = [];
      scaledPoints.forEach((s, i) => {{
        if (!s) return;
        upper.push(`${{x(i)}},${{y(s.hi)}}`);
        lower.unshift(`${{x(i)}},${{y(s.lo)}}`);
      }});
      if (upper.length > 1)
        el('polygon', {{ points: upper.concat(lower).join(' '), fill: colorOf(series.slot),
                        'fill-opacity': 0.10, stroke: 'none' }}, svg);

      const path = scaledPoints.map((s, i) => s ? `${{x(i)}},${{y(s.v)}}` : null)
                               .filter(Boolean).join(' ');
      if (path)
        el('polyline', {{ points: path, fill: 'none', stroke: colorOf(series.slot),
                         'stroke-width': 2, 'stroke-linejoin': 'round',
                         'stroke-linecap': 'round' }}, svg);

      /* Only the end point carries a dot; a marker on all nineteen would bury the line. */
      let lastIndex = -1;
      scaledPoints.forEach((s, i) => {{ if (s) lastIndex = i; }});
      if (lastIndex >= 0) {{
        el('circle', {{ cx: x(lastIndex), cy: y(scaledPoints[lastIndex].v), r: 4.5,
                       fill: colorOf(series.slot), stroke: 'var(--surface)',
                       'stroke-width': 2 }}, svg);
      }}
    }}

    /* The crosshair, drawn last so it is never hidden by a band. */
    if (hover !== null && hover >= 0 && hover < commits.length) {{
      el('line', {{ x1: x(hover), x2: x(hover), y1: PAD_T, y2: PAD_T + PLOT_H,
                   stroke: 'var(--axis)', 'stroke-width': 1 }}, svg);
      for (const series of facet.series) {{
        const s = scaled(series, series.points[hover]);
        if (!s) continue;
        el('circle', {{ cx: x(hover), cy: y(s.v), r: 4, fill: colorOf(series.slot),
                       stroke: 'var(--surface)', 'stroke-width': 2 }}, svg);
      }}
    }}
  }}

  function formatMs(value) {{
    if (value >= 100) return value.toFixed(0);
    if (value >= 1)   return value.toFixed(1);
    return value.toFixed(2);
  }}

  /* Axis ticks drop the decimals a round step does not need, so a 0/5/10/15/20 axis is not
     printed as 0.00/5.0/10.0/15.0/20.0. The precision comes from the *step* rather than
     from each value, or a 7.5 step would print its 22.5 tick as '23' beside an exact '15'. */
  function formatTick(value, step) {{
    if (value === 0) return '0';
    const digits = step >= 1 ? (step % 1 === 0 ? 0 : 1) : 2;
    return value.toFixed(digits).replace(/(\\.\\d*?)0+$/, '$1').replace(/\\.$/, '');
  }}

  function deltaOf(facet) {{
    /* The facet's headline: how its most expensive drawn series moved end to end. That
       series is the one a user would notice, and averaging it with a cheap one would hide
       exactly the regression worth seeing. */
    let chosen = null, chosenCost = -1;
    for (const series of facet.series) {{
      const first = series.points.find(p => p);
      const last  = [...series.points].reverse().find(p => p);
      if (!first || !last || !first.v) continue;
      if (first.v > chosenCost) {{ chosenCost = first.v; chosen = {{ first, last, series }}; }}
    }}
    if (!chosen) return null;
    return {{ pct: 100 * (chosen.last.v / chosen.first.v - 1), label: chosen.series.label }};
  }}

  const charts = document.getElementById('charts');
  for (const facet of facets) {{
    const card = document.createElement('div');
    card.className = 'facet';
    const delta = deltaOf(facet);
    const cls = !delta ? 'flat' : delta.pct < -2 ? 'good' : delta.pct > 2 ? 'bad' : 'flat';
    card.innerHTML =
      `<h2>${{delta ? `<span class="delta ${{cls}}">${{delta.pct >= 0 ? '+' : ''}}` +
              `${{delta.pct.toFixed(1)}}%</span>` : ''}}${{esc(facet.title)}}</h2>` +
      `<p class="meta">${{esc(facet.geometry || facet.note || '')}}</p>`;
    const svg = document.createElementNS(SVG_NS, 'svg');
    svg.setAttribute('viewBox', `0 0 ${{W}} ${{H}}`);
    svg.setAttribute('role', 'img');
    svg.setAttribute('aria-label',
      `${{facet.title}}: ${{facet.series.map(s => s.label).join(', ')}} over ${{commits.length}} commits`);
    card.appendChild(svg);
    charts.appendChild(card);
    facet.svg = svg;
    facet.card = card;

    svg.addEventListener('mousemove', event => {{
      const box = svg.getBoundingClientRect();
      const px = (event.clientX - box.left) / box.width * W;
      const ratio = (px - PAD_L) / PLOT_W;
      const index = Math.round(ratio * (commits.length - 1));
      setHover(Math.max(0, Math.min(commits.length - 1, index)), event, facet);
    }});
    svg.addEventListener('mouseleave', () => setHover(null));
  }}

  function esc(text) {{
    return String(text).replace(/[&<>"]/g, ch =>
      ({{ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }})[ch]);
  }}

  function setHover(index, event, facet) {{
    hover = index;
    facets.forEach(drawFacet);
    if (index === null || !event) {{ tip.style.opacity = 0; return; }}
    const commit = commits[index];
    const unit = mode === 'abs' ? ' ms' : '%';
    let html = `<b>#${{index}} ${{esc(commit.short)}}</b> &middot; ${{esc(commit.date)}}` +
               `<div class="subject">${{esc(commit.subject)}}</div>`;
    for (const series of facet.series) {{
      const s = scaled(series, series.points[index]);
      if (!s) continue;
      html += `<div class="row"><span class="key" style="background:${{colorOf(series.slot)}}">` +
              `</span>${{esc(series.label)}}<span class="val">${{
                mode === 'abs' ? formatMs(s.v) : s.v.toFixed(1)}}${{unit}}</span></div>`;
    }}
    tip.innerHTML = html;
    tip.style.opacity = 1;
    const box = tip.getBoundingClientRect();
    let left = event.clientX + 14, top = event.clientY + 14;
    if (left + box.width  > window.innerWidth  - 8) left = event.clientX - box.width - 14;
    if (top  + box.height > window.innerHeight - 8) top  = event.clientY - box.height - 14;
    tip.style.left = left + 'px';
    tip.style.top  = top + 'px';
  }}

  /* The legend is built from the series that actually appear, so it can never claim a
     colour no chart uses. */
  const legend = document.getElementById('legend');
  const seen = new Map();
  for (const facet of facets)
    for (const series of facet.series)
      if (!seen.has(series.label)) seen.set(series.label, series.slot);
  for (const [label, slot] of seen) {{
    const span = document.createElement('span');
    span.innerHTML = `<i class="key" style="background:${{colorOf(slot)}}"></i>${{esc(label)}}`;
    legend.appendChild(span);
  }}

  document.getElementById('mode-abs').addEventListener('click', () => setMode('abs'));
  document.getElementById('mode-rel').addEventListener('click', () => setMode('rel'));
  function setMode(next) {{
    mode = next;
    document.getElementById('mode-abs').setAttribute('aria-pressed', String(next === 'abs'));
    document.getElementById('mode-rel').setAttribute('aria-pressed', String(next === 'rel'));
    facets.forEach(drawFacet);
  }}

  /* The table view: everything measured, including the two columns the charts leave out,
     so no number is reachable only by hovering a line. */
  const body = document.getElementById('tables-body');
  for (const facet of facets) {{
    const columns = facet.series.concat(facet.table_series);
    let html = `<div class="scroll"><table><caption>${{esc(facet.title)}}` +
               `${{facet.geometry ? ' &middot; ' + esc(facet.geometry) : ''}}</caption><thead><tr>` +
               `<th class="left">#</th><th class="left">commit</th><th class="left">subject</th>`;
    for (const column of columns) html += `<th>${{esc(column.short)}} (ms)</th>`;
    html += `</tr></thead><tbody>`;
    commits.forEach((commit, i) => {{
      html += `<tr><td class="left">${{i}}</td><td class="left">${{esc(commit.short)}}</td>` +
              `<td class="subject">${{esc(commit.subject)}}</td>`;
      for (const column of columns) {{
        const point = column.points[i];
        html += `<td>${{point ? formatMs(point.v) : '&ndash;'}}</td>`;
      }}
      html += `</tr>`;
    }});
    html += `</tbody></table></div>`;
    body.insertAdjacentHTML('beforeend', html);
  }}

  facets.forEach(drawFacet);
}})();
</script>
</body>
</html>
"""
