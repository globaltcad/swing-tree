package examples.zen;

import com.formdev.flatlaf.FlatDarkLaf;
import sprouts.From;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.threading.EventProcessor;

import static swingtree.UI.*;

/**
 *  <h2>Theme Garden — One UI, Many Skins</h2>
 *
 *  <p>This example is the SwingTree answer to the
 *  <a href="https://csszengarden.com/">CSS Zen Garden</a>: a single UI
 *  declaration acting as a skeleton, plus several wildly different
 *  {@link ThemedStyleSheet} configurations that completely transform the
 *  same components without anything in this file changing.</p>
 *
 *  <p>The skeleton is a small music-player layout — header, album art,
 *  transport, equalizer, genre chips, playlist, status bar. Each of its
 *  components gets a semantic group tag (see {@link Skin}) but otherwise
 *  has no theme-specific code: no colors, no fonts, no shadows. All those
 *  decisions live in {@link ThemedStyleSheet}, and switching the picker
 *  in the header rebuilds the entire visual identity at runtime via
 *  {@link swingtree.style.StyleSheet#reconfigure() reconfigure()}.</p>
 *
 *  <h3>Try it</h3>
 *  <pre>
 *      gradle :test --tests examples.zen.ThemeGardenView
 *  </pre>
 *  ...or run {@link #main(String[])} directly. Then play with the dropdown.
 */
public final class ThemeGardenView extends Panel {

    public ThemeGardenView(Runnable pack) {
        // ── Reactive state ──────────────────────────────────────────────────
        // Theme picker selection — the only piece of state that drives the
        // entire visual identity of the application.
        Var<Theme>   theme        = Var.of(Theme.NEON_ARCADE);
        Var<Integer> progress     = Var.of(42);
        Var<Integer> volume       = Var.of(70);
        // Per-track playlist data, kept inline for a self-contained demo.
        String[][] tracks = new String[][] {
            { "Crystal Prelude",       "Aurora Vox",     "3:42" },
            { "Velvet Static",         "Glass Mountain", "4:18" },
            { "Lanterns Over Kyoto",   "Hiro & The Tide","5:01" },
            { "Concrete Pigeons",      "Block Party",    "2:55" },
            { "Saturn Hum",            "Lalla Lou",      "6:24" },
            { "Honeyglass Window",     "Bee Vista",      "3:08" },
            { "Polar Lullaby",         "Tundra Choir",   "4:47" },
            { "Neon Cathedral",        "Dust Engine",    "5:33" },
        };
        String[] genres = { "Pop", "Jazz", "Synth", "Folk", "Ambient" };

        // The chameleon stylesheet. Anything inside the UI.use(...) scope
        // below repaints automatically when sheet.setTheme(...) is called.
        ThemedStyleSheet sheet = new ThemedStyleSheet();
        sheet.setTheme(theme.get());
        Viewable.cast(theme).onChange(From.ALL, it -> sheet.setTheme(theme.get()));

        // ── Skeleton ───────────────────────────────────────────────────────
        // The structure does NOT know what theme is active. It only attaches
        // semantic group tags. Compare this to a Zen-Garden HTML page: it
        // has divs and class names, the CSS does the rest.
        UI.use(sheet, () ->
            UI.of(this).group(Skin.FRAME)
            .withLayout("fill, wrap 1, insets 0, gap 0").withPrefSize(1100, 760)

            // ─── Header ────────────────────────────────────────────────────
            .add("growx",
                panel("fillx, insets 0").group(Skin.HEADER)
                .add("pushx, growx",
                    box("fill, wrap 1, insets 0")
                    .add(label("THEME · GARDEN").group(Skin.APP_TITLE))
                    .add(label("one skeleton, many skins").group(Skin.APP_SUBTITLE))
                )
                .add("shrinkx",
                    label("Skin:").group(Skin.SECTION_LABEL)
                )
                .add("shrinkx",
                    comboBox(theme).group(Skin.THEME_PICKER).onSelection( it -> pack.run() )
                )
            )

            // ─── Body: split into "now playing" + "playlist" ───────────────
            .add("grow, push",
                panel("fill, wrap 2, insets 14", "[grow 60][grow 40]")

                // ── Left column: now-playing + transport + EQ + chips ─────
                .add("grow, push",
                    box("fill, wrap 1, insets 0")

                    .add("growx",
                        panel("fill, wrap 2, insets 0", "[shrink][grow]").group(Skin.NOW_PLAYING)
                        .add("w 180!, h 180!",
                            box().group(Skin.ALBUM_ART).withPrefSize(180, 180)
                        )
                        .add("growx, gapleft 18",
                            box("fill, wrap 1, insets 0")
                            .add(label("LANTERNS OVER KYOTO").group(Skin.TRACK_TITLE))
                            .add(label("Hiro & The Tide  ·  Drift Reflections").group(Skin.TRACK_ARTIST))

                            // Transport row
                            .add("growx, gaptop 14",
                                panel("insets 0, gap 6").group(Skin.TRANSPORT)
                                .add(button("⏮").group(Skin.NAV_BUTTON))
                                .add(button("▶").group(Skin.PLAY_BUTTON))
                                .add(button("⏭").group(Skin.NAV_BUTTON))
                            )

                            // Progress slider
                            .add("growx, gaptop 12",
                                slider(Align.HORIZONTAL, 0, 100, progress).group(Skin.PROGRESS)
                            )

                            // Volume slider
                            .add("growx, gaptop 4",
                                box("insets 0, gap 8", "[shrink][grow]")
                                .add(label("vol").group(Skin.SECTION_LABEL))
                                .add("growx",
                                    slider(Align.HORIZONTAL, 0, 100, volume).group(Skin.VOLUME)
                                )
                            )
                        )
                    )

                    // Equalizer
                    .add("growx, gaptop 12",
                        box("fill, wrap 1, insets 0")
                        .add(label("EQUALIZER").group(Skin.SECTION_LABEL))
                        .add("growx, gaptop 4",
                            panel("fillx, insets 6, gap 10").group(Skin.EQ_PANEL)
                            .apply(p -> {
                                String[] bands = { "60", "150", "400", "1k", "2.5k", "6k", "12k" };
                                int[] vals     = {  55,   70,    62,    48,    72,    58,    66 };
                                for (int i = 0; i < bands.length; i++) {
                                    int v = vals[i];
                                    p.add("",
                                        box("fill, wrap 1, insets 0").withPrefWidth(38)
                                        .add("center, h 110!",
                                            slider(Align.VERTICAL, 0, 100)
                                                .withValue(v)
                                                .group(Skin.EQ_BAR)
                                        )
                                        .add("center",
                                            label(bands[i]).group(Skin.EQ_LABEL)
                                        )
                                    );
                                }
                            })
                        )
                    )

                    // Genre chips
                    .add("growx, gaptop 12",
                        box("fill, wrap 1, insets 0")
                        .add(label("GENRES").group(Skin.SECTION_LABEL))
                        .add("growx, gaptop 4",
                            panel("insets 0, gap 4").group(Skin.GENRE_PANEL)
                            .apply(p -> {
                                for (int i = 0; i < genres.length; i++) {
                                    boolean preselected = (i == 2 || i == 4);
                                    p.add("",
                                        toggleButton(genres[i])
                                            .isSelectedIf(preselected)
                                            .group(Skin.CHIP)
                                    );
                                }
                            })
                        )
                    )

                    // Decorative strip — themes can paint flourishes here.
                    .add("growx, gaptop 14, h 28!",
                        box().group(Skin.DECOR)
                    )
                )

                // ── Right column: playlist ────────────────────────────────
                .add("grow, push",
                    box("fill, wrap 1, insets 0")
                    .add(label("UP NEXT").group(Skin.SECTION_LABEL))
                    .add("grow, push, gaptop 4",
                        scrollPanels().group(Skin.PLAYLIST)
                        .apply(list -> {
                            for (int i = 0; i < tracks.length; i++) {
                                String[] t = tracks[i];
                                String num = String.format("%02d", i + 1);
                                list.add(
                                    panel("fillx, insets 0, gap 12", "[shrink][grow][shrink]")
                                    .group(Skin.PLAYLIST_ITEM)
                                    .add(label(num).group(Skin.PLAYLIST_DURATION))
                                    .add("growx",
                                        box("fill, wrap 1, insets 0")
                                        .add(label(t[0]))
                                        .add(label(t[1]).group(Skin.TRACK_ARTIST))
                                    )
                                    .add(label(t[2]).group(Skin.PLAYLIST_DURATION))
                                );
                            }
                        })
                    )
                )
            )

            // ─── Status bar ────────────────────────────────────────────────
            .add("growx",
                panel("fillx, insets 0").group(Skin.STATUS)
                .add("pushx, growx",
                    label(theme.viewAsString(t -> "skin: " + t.pretty()))
                        .group(Skin.STATUS)
                )
                .add("shrinkx",
                    label(progress.viewAsString(p -> "progress " + p + "%   ·   "))
                        .group(Skin.STATUS)
                )
                .add("shrinkx",
                    label(volume.viewAsString(v -> "volume " + v + "%"))
                        .group(Skin.STATUS)
                )
            )
        );
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        UI.show("Theme Garden — one UI, many skins", f -> new ThemeGardenView(f::pack));
        EventProcessor.DECOUPLED.join();
    }
}