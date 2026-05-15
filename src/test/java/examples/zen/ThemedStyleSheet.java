package examples.zen;

import swingtree.UI;
import swingtree.style.StyleSheet;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.geom.Path2D;

/**
 *  A single mutable {@link StyleSheet} that hosts every theme.
 *  Calling {@link #setTheme(Theme)} swaps in a completely different look
 *  for the same {@link ThemeGardenView} skeleton:
 *
 *  <pre>{@code
 *      ThemedStyleSheet sheet = new ThemedStyleSheet();
 *      UI.use(sheet, () -> ...build skeleton... );
 *
 *      sheet.setTheme(Theme.NEON_ARCADE);     // arcade glow
 *      sheet.setTheme(Theme.PARCHMENT_CODEX); // medieval manuscript
 *      sheet.setTheme(Theme.BRUTALIST_CONCRETE); // raw slabs
 *      sheet.setTheme(Theme.COSMIC_DRIFT);    // night sky
 *      sheet.setTheme(Theme.PASTEL_BLOOM);    // soft pastel
 *  }</pre>
 *
 *  <p>The mechanism is the same as in
 *  {@code examples.stylish.DynamicUIStyleSheet}: {@link #reconfigure()} clears
 *  the previously registered traits, calls {@link #configure()} again with the
 *  new theme, and fires the internal observable. Anything inside the
 *  {@code UI.use(...)} scope where this sheet was installed will repaint with
 *  the new rules.</p>
 */
public final class ThemedStyleSheet extends StyleSheet {

    private Theme theme = Theme.NEON_ARCADE;

    public Theme theme() {
        return theme;
    }

    /** Switch to the given theme and rebuild every styling rule. */
    public void setTheme(Theme newTheme) {
        if (newTheme != theme) {
            this.theme = newTheme;
            reconfigure();
        }
    }

    @Override
    protected void configure() {
        switch (theme) {
            case NEON_ARCADE:        configureNeon();       break;
            case PARCHMENT_CODEX:    configureParchment();  break;
            case BRUTALIST_CONCRETE: configureBrutalist();  break;
            case COSMIC_DRIFT:       configureCosmic();     break;
            case PASTEL_BLOOM:       configurePastel();     break;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Theme 1 — NEON ARCADE
    //  Black void, cyan and magenta beams, glowing edges, scanline noise.
    // ════════════════════════════════════════════════════════════════════════

    private void configureNeon() {
        Color VOID    = new Color(  8,  8, 22);
        Color INK     = new Color(220,250,255);
        Color CYAN    = new Color( 80,255,235);
        Color MAGENTA = new Color(255, 70,200);
        Color YELLOW  = new Color(255,235,120);

        // Page backdrop: pitch-black with a faint retro grid noise.
        add(group(Skin.FRAME), it -> it
            .backgroundColor(VOID)
            .foundationColor(VOID)
            .padding(14)
            .noise("scan", n -> n
                .function(UI.NoiseType.RETRO)
                .colors(new Color(0, 0, 0, 0), new Color(120, 30, 220, 70))
                .scale(2)
                .clipTo(UI.ComponentArea.BODY)
            )
            .componentFont(f -> f.family("Monospaced").size(13).color(INK))
        );

        // The header is a thin chrome strip with a magenta underline.
        add(group(Skin.HEADER), it -> it
            .backgroundColor(new Color(20, 18, 40))
            .borderAt(UI.Edge.BOTTOM, 2, MAGENTA)
            .padding(10, 14, 10, 14)
            .margin(0)
            .componentFont(f -> f.family("Monospaced").size(13).color(INK))
        );
        add(group(Skin.APP_TITLE), it -> it
            .componentFont(f -> f.family("Monospaced").size(20).weight(2).color(CYAN))
        );
        add(group(Skin.APP_SUBTITLE), it -> it
            .componentFont(f -> f.family("Monospaced").size(11).color(MAGENTA).posture(0.2f))
        );

        // Now-playing: dark slab with a strong magenta-cyan diagonal gradient.
        add(group(Skin.NOW_PLAYING), it -> it
            .backgroundColor(new Color(15, 12, 35))
            .borderRadius(0)
            .border(2, CYAN)
            .padding(16)
            .margin(8)
            .gradient("ridge", g -> g
                .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                .colors(new Color(40, 0, 60, 200), new Color(0, 40, 60, 200))
            )
            .shadow("glow", s -> s
                .color(new Color(255, 80, 220, 100)).offset(0).blurRadius(18).spreadRadius(1)
            )
        );

        // Album art slot: a square panel with a vinyl-record style radial gradient.
        add(group(Skin.ALBUM_ART), it -> it
            .borderRadius(1000)
            .border(3, CYAN)
            .gradient("vinyl", g -> g
                .type(UI.GradientType.RADIAL)
                .boundary(UI.ComponentBoundary.BORDER_TO_INTERIOR)
                .size(Math.min(it.componentWidth(), it.componentHeight()))
                .colors(MAGENTA, new Color(40, 0, 60), new Color(8, 8, 22))
                .clipTo(UI.ComponentArea.BODY)
            )
            .gradient(UI.Layer.FOREGROUND, "spin", g -> g
                .type(UI.GradientType.CONIC)
                .boundary(UI.ComponentBoundary.BORDER_TO_INTERIOR)
                .colors(new Color(0,0,0,0), new Color(255,255,255,40), new Color(0,0,0,0))
                .clipTo(UI.ComponentArea.BODY)
            )
            .shadow("haze", s -> s.color(new Color(255, 80, 220, 120)).offset(0).blurRadius(28).spreadRadius(2))
        );
        add(group(Skin.TRACK_TITLE), it -> it
            .componentFont(f -> f.family("Monospaced").size(20).weight(2).color(CYAN))
        );
        add(group(Skin.TRACK_ARTIST), it -> it
            .componentFont(f -> f.family("Monospaced").size(12).color(MAGENTA))
        );

        // Transport: thin neon-ringed buttons.
        add(group(Skin.TRANSPORT), it -> it.backgroundColor(new Color(0, 0, 0, 0)).margin(0).padding(0));
        add(type(AbstractButton.class).group(Skin.PLAY_BUTTON), it -> it
            .backgroundColor(new Color(8, 8, 22))
            .borderRadius(1000)
            .border(2, YELLOW)
            .padding(10, 18, 10, 18)
            .margin(6)
            .componentFont(f -> f.family("Monospaced").size(16).weight(2).color(YELLOW))
            .shadow("glow", s -> s.color(new Color(255, 235, 120, 150)).offset(0).blurRadius(16).spreadRadius(1))
            .cursor(UI.Cursor.HAND)
        );
        add(type(AbstractButton.class).group(Skin.NAV_BUTTON), it -> it
            .backgroundColor(new Color(8, 8, 22))
            .borderRadius(1000)
            .border(1, CYAN)
            .padding(8, 14, 8, 14)
            .margin(4)
            .componentFont(f -> f.family("Monospaced").size(13).color(CYAN))
            .shadow("glow", s -> s.color(new Color(80, 255, 235, 110)).offset(0).blurRadius(10).spreadRadius(0))
            .cursor(UI.Cursor.HAND)
        );

        // Sliders.
        add(type(JSlider.class).group(Skin.PROGRESS), it -> it
            .backgroundColor(new Color(20, 18, 40))
            .borderRadius(0)
            .border(1, CYAN)
            .padding(2)
            .cursor(UI.Cursor.HAND)
        );
        add(type(JSlider.class).group(Skin.VOLUME), it -> it
            .backgroundColor(new Color(20, 18, 40))
            .borderRadius(0)
            .border(1, MAGENTA)
            .padding(2)
            .cursor(UI.Cursor.HAND)
        );

        // Equalizer: a wireframe panel of vertical sliders.
        add(group(Skin.EQ_PANEL), it -> it
            .backgroundColor(new Color(0, 0, 0, 100))
            .borderRadius(0)
            .border(1, CYAN)
            .padding(8)
            .margin(6)
        );
        add(type(JSlider.class).group(Skin.EQ_BAR), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .padding(0)
        );
        add(group(Skin.EQ_LABEL), it -> it
            .componentFont(f -> f.family("Monospaced").size(10).color(MAGENTA))
        );

        // Chips: little pill switches that glow when toggled.
        add(group(Skin.GENRE_PANEL), it -> it
            .backgroundColor(new Color(0, 0, 0, 0)).padding(2).margin(0)
        );
        add(type(JToggleButton.class).group(Skin.CHIP), it -> {
            JToggleButton b = it.component();
            boolean on = b.isSelected();
            return it
                .backgroundColor(on ? new Color(80, 255, 235, 90) : new Color(20, 18, 40))
                .borderRadius(0)
                .border(1, on ? CYAN : new Color(80, 90, 130))
                .padding(4, 12, 4, 12)
                .margin(3)
                .componentFont(f -> f.family("Monospaced").size(11)
                    .color(on ? Color.WHITE : new Color(150, 200, 220)))
                .shadow("g", s -> s
                    .color(on ? new Color(80, 255, 235, 180) : new Color(0,0,0,0))
                    .offset(0).blurRadius(on ? 14 : 0).spreadRadius(0))
                .cursor(UI.Cursor.HAND);
        });

        // Playlist.
        add(group(Skin.PLAYLIST), it -> it
            .backgroundColor(new Color(15, 12, 35))
            .borderRadius(0)
            .border(2, MAGENTA)
            .padding(0)
            .margin(8)
        );
        add(group(Skin.PLAYLIST_ITEM), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .borderAt(UI.Edge.BOTTOM, 1, new Color(120, 30, 220, 90))
            .padding(8, 12, 8, 12)
            .margin(0)
            .componentFont(f -> f.family("Monospaced").size(12).color(new Color(220, 250, 255)))
        );
        add(group(Skin.PLAYLIST_DURATION), it -> it
            .componentFont(f -> f.family("Monospaced").size(11).color(YELLOW))
        );

        add(group(Skin.STATUS), it -> it
            .backgroundColor(new Color(20, 18, 40))
            .borderAt(UI.Edge.TOP, 1, MAGENTA)
            .padding(6, 12, 6, 12)
            .componentFont(f -> f.family("Monospaced").size(10).color(new Color(255, 235, 120)))
        );
        add(group(Skin.SECTION_LABEL), it -> it
            .componentFont(f -> f.family("Monospaced").size(11).weight(2).color(CYAN))
            .padding(2, 4, 2, 4)
        );

        // Decorative slot — a hand-drawn neon zigzag.
        add(group(Skin.DECOR), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .painter(UI.Layer.BACKGROUND, g2 -> {
                int w = it.componentWidth(), h = it.componentHeight();
                g2.setColor(MAGENTA);
                g2.setStroke(new java.awt.BasicStroke(2f));
                Path2D.Double p = new Path2D.Double();
                int steps = 24;
                for (int i = 0; i <= steps; i++) {
                    double x = (double) i * w / steps;
                    double y = h * 0.5 + Math.sin(i * 0.9) * h * 0.35;
                    if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
                }
                g2.draw(p);
                g2.setColor(new Color(255, 70, 200, 80));
                g2.setStroke(new java.awt.BasicStroke(8f));
                g2.draw(p);
            })
        );

        // Combo box (theme picker) styling.
        add(type(JComboBox.class).group(Skin.THEME_PICKER), it -> it
            .backgroundColor(new Color(20, 18, 40))
            .border(1, CYAN)
            .borderRadius(0)
            .padding(2, 8, 2, 8)
            .componentFont(f -> f.family("Monospaced").size(12).color(CYAN))
            .cursor(UI.Cursor.HAND)
        );

        commonTextField(new Color(15, 12, 35), CYAN, INK);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Theme 2 — PARCHMENT CODEX
    //  Warm ivory page, brown ink, serif font, paper-grain noise, square edges.
    // ════════════════════════════════════════════════════════════════════════

    private void configureParchment() {
        Color PAPER   = new Color(248, 232, 196);
        Color PAPER_D = new Color(228, 207, 159);
        Color INK     = new Color( 64,  35,  18);
        Color RED     = new Color(140,  30,  30);
        Color GOLD    = new Color(170, 120,  30);

        add(group(Skin.FRAME), it -> it
            .backgroundColor(PAPER)
            .foundationColor(PAPER_D.darker())
            .padding(20)
            .gradient("vignette", g -> g
                .type(UI.GradientType.RADIAL)
                .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                .size(Math.max(it.componentWidth(), it.componentHeight()))
                .boundary(UI.ComponentBoundary.OUTER_TO_EXTERIOR)
                .colors(new Color(0,0,0,0), new Color(80, 50, 20, 90))
                .clipTo(UI.ComponentArea.BODY)
            )
            .noise("grain", n -> n
                .function(UI.NoiseType.POND_IN_DRIZZLE)
                .colors(new Color(0,0,0,0), new Color(80, 50, 20, 35))
                .scale(2)
                .clipTo(UI.ComponentArea.BODY)
            )
            .componentFont(f -> f.family("Serif").size(13).color(INK))
        );

        add(group(Skin.HEADER), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .borderAt(UI.Edge.BOTTOM, 2, INK)
            .padding(8, 16, 12, 16)
            .margin(0)
        );
        add(group(Skin.APP_TITLE), it -> it
            .componentFont(f -> f.family("Serif").size(28).weight(2).color(RED))
        );
        add(group(Skin.APP_SUBTITLE), it -> it
            .componentFont(f -> f.family("Serif").size(12).color(INK).posture(0.25f))
        );

        // Now playing: a framed manuscript card.
        add(group(Skin.NOW_PLAYING), it -> it
            .backgroundColor(new Color(255, 245, 215))
            .border(2, INK)
            .padding(20)
            .margin(10)
            .shadow("ink", s -> s.color(new Color(60, 30, 10, 110)).offset(3, 4).blurRadius(2))
        );

        // Album art: gold seal on ivory, with a hand-drawn star painter.
        add(group(Skin.ALBUM_ART), it -> it
            .backgroundColor(new Color(255, 245, 215))
            .borderRadius(0)
            .border(3, INK)
            .padding(0)
            .gradient("paper", g -> g.colors(new Color(255, 245, 215), PAPER_D).span(UI.Span.TOP_TO_BOTTOM))
            .painter(UI.Layer.CONTENT, g2 -> {
                int w = it.componentWidth(), h = it.componentHeight();
                int cx = w / 2, cy = h / 2;
                int r  = Math.min(w, h) / 3;
                g2.setColor(GOLD);
                Path2D.Double star = new Path2D.Double();
                for (int i = 0; i < 12; i++) {
                    double ang = -Math.PI/2 + i * Math.PI / 6;
                    double rr  = (i % 2 == 0) ? r : r * 0.55;
                    double x = cx + Math.cos(ang) * rr;
                    double y = cy + Math.sin(ang) * rr;
                    if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
                }
                star.closePath();
                g2.fill(star);
                g2.setColor(INK);
                g2.setStroke(new java.awt.BasicStroke(1.5f));
                g2.draw(star);
            })
        );

        add(group(Skin.TRACK_TITLE), it -> it
            .componentFont(f -> f.family("Serif").size(22).weight(2).color(INK))
        );
        add(group(Skin.TRACK_ARTIST), it -> it
            .componentFont(f -> f.family("Serif").size(13).color(RED).posture(0.25f))
        );

        // Transport: square wax-stamped buttons.
        add(group(Skin.TRANSPORT), it -> it.backgroundColor(new Color(0, 0, 0, 0)).margin(0));
        add(type(AbstractButton.class).group(Skin.PLAY_BUTTON), it -> it
            .backgroundColor(RED)
            .borderRadius(0)
            .border(2, INK)
            .padding(8, 18, 8, 18)
            .margin(4)
            .componentFont(f -> f.family("Serif").size(18).weight(2).color(new Color(255, 245, 215)))
            .shadow("seal", s -> s.color(new Color(60, 30, 10, 130)).offset(2, 3).blurRadius(2))
            .cursor(UI.Cursor.HAND)
        );
        add(type(AbstractButton.class).group(Skin.NAV_BUTTON), it -> it
            .backgroundColor(new Color(255, 245, 215))
            .borderRadius(0)
            .border(2, INK)
            .padding(6, 14, 6, 14)
            .margin(4)
            .componentFont(f -> f.family("Serif").size(13).color(INK))
            .cursor(UI.Cursor.HAND)
        );

        add(type(JSlider.class).group(Skin.PROGRESS), it -> it
            .backgroundColor(new Color(255, 245, 215))
            .borderRadius(0)
            .border(1, INK)
            .padding(2)
            .cursor(UI.Cursor.HAND)
        );
        add(type(JSlider.class).group(Skin.VOLUME), it -> it
            .backgroundColor(new Color(255, 245, 215))
            .borderRadius(0)
            .border(1, INK)
            .padding(2)
            .cursor(UI.Cursor.HAND)
        );

        add(group(Skin.EQ_PANEL), it -> it
            .backgroundColor(new Color(255, 245, 215))
            .border(2, INK)
            .padding(8)
            .margin(6)
        );
        add(type(JSlider.class).group(Skin.EQ_BAR), it -> it
            .backgroundColor(new Color(255, 245, 215)).padding(0)
        );
        add(group(Skin.EQ_LABEL), it -> it
            .componentFont(f -> f.family("Serif").size(11).color(INK))
        );

        add(group(Skin.GENRE_PANEL), it -> it.backgroundColor(new Color(0, 0, 0, 0)).margin(0));
        add(type(JToggleButton.class).group(Skin.CHIP), it -> {
            JToggleButton b = it.component();
            boolean on = b.isSelected();
            return it
                .backgroundColor(on ? GOLD : new Color(255, 245, 215))
                .borderRadius(0)
                .border(1.5, INK)
                .padding(4, 10, 4, 10)
                .margin(3)
                .componentFont(f -> f.family("Serif").size(12)
                    .color(on ? new Color(255, 245, 215) : INK))
                .cursor(UI.Cursor.HAND);
        });

        add(group(Skin.PLAYLIST), it -> it
            .backgroundColor(new Color(255, 245, 215))
            .border(2, INK)
            .padding(0)
            .margin(8)
        );
        add(group(Skin.PLAYLIST_ITEM), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .borderAt(UI.Edge.BOTTOM, 1, new Color(120, 80, 40, 180))
            .padding(7, 12, 7, 12)
            .componentFont(f -> f.family("Serif").size(13).color(INK))
        );
        add(group(Skin.PLAYLIST_DURATION), it -> it
            .componentFont(f -> f.family("Serif").size(11).color(RED).posture(0.25f))
        );

        add(group(Skin.STATUS), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .borderAt(UI.Edge.TOP, 1, INK)
            .padding(6, 12, 6, 12)
            .componentFont(f -> f.family("Serif").size(11).color(INK).posture(0.25f))
        );
        add(group(Skin.SECTION_LABEL), it -> it
            .componentFont(f -> f.family("Serif").size(15).weight(2).color(RED))
            .padding(2, 4, 2, 4)
        );

        add(group(Skin.DECOR), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .painter(UI.Layer.BACKGROUND, g2 -> {
                int w = it.componentWidth(), h = it.componentHeight();
                g2.setColor(INK);
                g2.setStroke(new java.awt.BasicStroke(1.2f));
                int cy = h / 2;
                g2.drawLine(8, cy, w - 8, cy);
                int cx = w / 2;
                int s = Math.min(20, h - 4);
                g2.drawLine(cx - s, cy - s/2, cx, cy);
                g2.drawLine(cx, cy, cx - s, cy + s/2);
                g2.drawLine(cx + s, cy - s/2, cx, cy);
                g2.drawLine(cx, cy, cx + s, cy + s/2);
            })
        );

        add(type(JComboBox.class).group(Skin.THEME_PICKER), it -> it
            .backgroundColor(new Color(255, 245, 215))
            .border(2, INK)
            .borderRadius(0)
            .padding(2, 8, 2, 8)
            .componentFont(f -> f.family("Serif").size(13).color(INK))
            .cursor(UI.Cursor.HAND)
        );

        commonTextField(new Color(255, 245, 215), INK, INK);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Theme 3 — BRUTALIST CONCRETE
    //  Concrete grey, thick black borders, monospace shouting, no shadows.
    // ════════════════════════════════════════════════════════════════════════

    private void configureBrutalist() {
        Color CONCRETE = new Color(207, 207, 197);
        Color SLAB     = new Color(178, 178, 168);
        Color INK      = Color.BLACK;
        Color HOT      = new Color(220,  60,  40);

        add(group(Skin.FRAME), it -> it
            .backgroundColor(CONCRETE)
            .foundationColor(CONCRETE)
            .padding(0)
            .noise("grit", n -> n
                .function(UI.NoiseType.GRAINY)
                .colors(new Color(0, 0, 0, 0), new Color(0, 0, 0, 30))
                .scale(1)
                .clipTo(UI.ComponentArea.BODY)
            )
            .componentFont(f -> f.family("Monospaced").size(13).color(INK))
        );

        add(group(Skin.HEADER), it -> it
            .backgroundColor(INK)
            .padding(12, 18, 12, 18)
            .margin(0)
        );
        add(group(Skin.APP_TITLE), it -> it
            .componentFont(f -> f.family("Monospaced").size(22).weight(2).color(CONCRETE).spacing(0.05f))
        );
        add(group(Skin.APP_SUBTITLE), it -> it
            .componentFont(f -> f.family("Monospaced").size(11).color(HOT))
        );

        add(group(Skin.NOW_PLAYING), it -> it
            .backgroundColor(SLAB)
            .borderRadius(0)
            .border(3, INK)
            .padding(16)
            .margin(0)
        );
        add(group(Skin.ALBUM_ART), it -> it
            .backgroundColor(INK)
            .borderRadius(0)
            .border(3, INK)
            .painter(UI.Layer.CONTENT, g2 -> {
                int w = it.componentWidth(), h = it.componentHeight();
                g2.setColor(HOT);
                g2.fillRect(0, 0, w / 2, h);
                g2.setColor(CONCRETE);
                g2.fillRect(w / 2, 0, w / 2, h);
                g2.setColor(INK);
                g2.setStroke(new java.awt.BasicStroke(3f));
                g2.drawRect(0, 0, w - 1, h - 1);
                g2.drawLine(w / 2, 0, w / 2, h);
            })
        );

        add(group(Skin.TRACK_TITLE), it -> it
            .componentFont(f -> f.family("Monospaced").size(20).weight(2).color(INK).spacing(0.04f))
        );
        add(group(Skin.TRACK_ARTIST), it -> it
            .componentFont(f -> f.family("Monospaced").size(12).color(HOT))
        );

        add(group(Skin.TRANSPORT), it -> it.backgroundColor(new Color(0, 0, 0, 0)).margin(0));
        add(type(AbstractButton.class).group(Skin.PLAY_BUTTON), it -> it
            .backgroundColor(HOT)
            .borderRadius(0)
            .border(3, INK)
            .padding(10, 22, 10, 22)
            .margin(0)
            .componentFont(f -> f.family("Monospaced").size(18).weight(2).color(INK))
            .cursor(UI.Cursor.HAND)
        );
        add(type(AbstractButton.class).group(Skin.NAV_BUTTON), it -> it
            .backgroundColor(CONCRETE)
            .borderRadius(0)
            .border(2, INK)
            .padding(8, 16, 8, 16)
            .margin(0)
            .componentFont(f -> f.family("Monospaced").size(13).weight(2).color(INK))
            .cursor(UI.Cursor.HAND)
        );

        add(type(JSlider.class).group(Skin.PROGRESS), it -> it
            .backgroundColor(CONCRETE)
            .border(2, INK)
            .borderRadius(0)
            .padding(2)
            .cursor(UI.Cursor.HAND)
        );
        add(type(JSlider.class).group(Skin.VOLUME), it -> it
            .backgroundColor(CONCRETE)
            .border(2, INK)
            .borderRadius(0)
            .padding(2)
            .cursor(UI.Cursor.HAND)
        );

        add(group(Skin.EQ_PANEL), it -> it
            .backgroundColor(SLAB)
            .border(3, INK)
            .padding(8)
            .margin(0)
        );
        add(type(JSlider.class).group(Skin.EQ_BAR), it -> it
            .backgroundColor(SLAB).padding(0)
        );
        add(group(Skin.EQ_LABEL), it -> it
            .componentFont(f -> f.family("Monospaced").size(10).color(INK))
        );

        add(group(Skin.GENRE_PANEL), it -> it.backgroundColor(new Color(0, 0, 0, 0)).margin(0));
        add(type(JToggleButton.class).group(Skin.CHIP), it -> {
            JToggleButton b = it.component();
            boolean on = b.isSelected();
            return it
                .backgroundColor(on ? INK : CONCRETE)
                .borderRadius(0)
                .border(2, INK)
                .padding(4, 10, 4, 10)
                .margin(2)
                .componentFont(f -> f.family("Monospaced").size(11).weight(2)
                    .color(on ? HOT : INK))
                .cursor(UI.Cursor.HAND);
        });

        add(group(Skin.PLAYLIST), it -> it
            .backgroundColor(SLAB)
            .border(3, INK)
            .padding(0)
            .margin(0)
        );
        add(group(Skin.PLAYLIST_ITEM), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .borderAt(UI.Edge.BOTTOM, 2, INK)
            .padding(7, 12, 7, 12)
            .componentFont(f -> f.family("Monospaced").size(12).color(INK))
        );
        add(group(Skin.PLAYLIST_DURATION), it -> it
            .componentFont(f -> f.family("Monospaced").size(11).weight(2).color(HOT))
        );

        add(group(Skin.STATUS), it -> it
            .backgroundColor(INK)
            .padding(6, 12, 6, 12)
            .componentFont(f -> f.family("Monospaced").size(11).color(CONCRETE))
        );
        add(group(Skin.SECTION_LABEL), it -> it
            .backgroundColor(INK)
            .componentFont(f -> f.family("Monospaced").size(11).weight(2).color(HOT).spacing(0.05f))
            .padding(3, 8, 3, 8)
        );

        add(group(Skin.DECOR), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .painter(UI.Layer.BACKGROUND, g2 -> {
                int w = it.componentWidth(), h = it.componentHeight();
                g2.setColor(INK);
                int bw = Math.max(8, w / 14);
                for (int x = 0; x < w; x += bw * 2) {
                    g2.fillRect(x, 0, bw, h);
                }
            })
        );

        add(type(JComboBox.class).group(Skin.THEME_PICKER), it -> it
            .backgroundColor(CONCRETE)
            .border(2, INK)
            .borderRadius(0)
            .padding(2, 8, 2, 8)
            .componentFont(f -> f.family("Monospaced").size(12).weight(2).color(INK))
            .cursor(UI.Cursor.HAND)
        );

        commonTextField(CONCRETE, INK, INK);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Theme 4 — COSMIC DRIFT
    //  Deep navy, radial nebula gradients, twinkling pinpoints, gold serif.
    // ════════════════════════════════════════════════════════════════════════

    private void configureCosmic() {
        Color VOID    = new Color(  6,   8,  26);
        Color GLOW    = new Color(140, 180, 255);
        Color GOLD    = new Color(255, 214, 120);
        Color VIOLET  = new Color(160,  90, 220);

        add(group(Skin.FRAME), it -> it
            .backgroundColor(VOID)
            .foundationColor(VOID)
            .padding(14)
            .gradient("nebula", g -> g
                .type(UI.GradientType.RADIAL)
                .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                .boundary(UI.ComponentBoundary.OUTER_TO_EXTERIOR)
                .size(Math.max(it.componentWidth(), it.componentHeight()))
                .offset(it.componentWidth() * 0.3, it.componentHeight() * 0.2)
                .colors(new Color(60, 30, 100, 180), new Color(0, 0, 30, 0))
                .clipTo(UI.ComponentArea.BODY)
            )
            .noise("stars", n -> n
                .function(UI.NoiseType.HARD_SPOTS)
                .colors(new Color(0,0,0,0), new Color(255, 255, 240, 110))
                .scale(0.35f)
                .clipTo(UI.ComponentArea.BODY)
            )
            .componentFont(f -> f.family("Serif").size(13).color(GLOW))
        );

        add(group(Skin.HEADER), it -> it
            .backgroundColor(new Color(0, 0, 0, 100))
            .borderAt(UI.Edge.BOTTOM, 1, VIOLET)
            .padding(10, 18, 10, 18)
        );
        add(group(Skin.APP_TITLE), it -> it
            .componentFont(f -> f.family("Serif").size(24).weight(2).color(GOLD))
        );
        add(group(Skin.APP_SUBTITLE), it -> it
            .componentFont(f -> f.family("Serif").size(12).color(GLOW).posture(0.2f))
        );

        add(group(Skin.NOW_PLAYING), it -> it
            .backgroundColor(new Color(0, 0, 0, 110))
            .borderRadius(18)
            .border(1, VIOLET)
            .padding(20)
            .margin(8)
            .gradient("inner", g -> g
                .type(UI.GradientType.RADIAL)
                .boundary(UI.ComponentBoundary.BORDER_TO_INTERIOR)
                .size(Math.max(it.componentWidth(), it.componentHeight()) * 0.7)
                .offset(it.componentWidth() * 0.2, it.componentHeight() * 0.2)
                .colors(new Color(80, 40, 160, 120), new Color(0, 0, 0, 0))
                .clipTo(UI.ComponentArea.BODY)
            )
        );

        add(group(Skin.ALBUM_ART), it -> it
            .backgroundColor(VOID)
            .borderRadius(1000)
            .border(1, GOLD)
            .gradient("planet", g -> g
                .type(UI.GradientType.RADIAL)
                .boundary(UI.ComponentBoundary.BORDER_TO_INTERIOR)
                .size(Math.min(it.componentWidth(), it.componentHeight()))
                .offset(- it.componentWidth() * 0.15, - it.componentHeight() * 0.15)
                .colors(new Color(255, 240, 180), GOLD, new Color(120, 60, 40), new Color(20, 10, 30))
                .clipTo(UI.ComponentArea.BODY)
            )
            .shadow("aura", s -> s.color(new Color(140, 100, 220, 150))
                .offset(0).blurRadius(28).spreadRadius(2))
        );

        add(group(Skin.TRACK_TITLE), it -> it
            .componentFont(f -> f.family("Serif").size(22).weight(2).color(GOLD))
        );
        add(group(Skin.TRACK_ARTIST), it -> it
            .componentFont(f -> f.family("Serif").size(13).color(GLOW).posture(0.2f))
        );

        add(group(Skin.TRANSPORT), it -> it.backgroundColor(new Color(0, 0, 0, 0)).margin(0));
        add(type(AbstractButton.class).group(Skin.PLAY_BUTTON), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .borderRadius(1000)
            .border(1.5, GOLD)
            .padding(10, 18, 10, 18)
            .margin(6)
            .componentFont(f -> f.family("Serif").size(18).weight(2).color(GOLD))
            .gradient("pulse", g -> g
                .type(UI.GradientType.RADIAL)
                .boundary(UI.ComponentBoundary.BORDER_TO_INTERIOR)
                .size(Math.min(it.componentWidth(), it.componentHeight()))
                .colors(new Color(80, 40, 160, 200), new Color(0,0,0,0))
                .clipTo(UI.ComponentArea.BODY)
            )
            .shadow("aura", s -> s.color(new Color(255, 214, 120, 140))
                .offset(0).blurRadius(18).spreadRadius(0))
            .cursor(UI.Cursor.HAND)
        );
        add(type(AbstractButton.class).group(Skin.NAV_BUTTON), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .borderRadius(1000)
            .border(1, VIOLET)
            .padding(7, 13, 7, 13)
            .margin(4)
            .componentFont(f -> f.family("Serif").size(13).color(GLOW))
            .cursor(UI.Cursor.HAND)
        );

        add(type(JSlider.class).group(Skin.PROGRESS), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .border(1, GOLD)
            .borderRadius(20)
            .padding(2)
            .cursor(UI.Cursor.HAND)
        );
        add(type(JSlider.class).group(Skin.VOLUME), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .border(1, VIOLET)
            .borderRadius(20)
            .padding(2)
            .cursor(UI.Cursor.HAND)
        );

        add(group(Skin.EQ_PANEL), it -> it
            .backgroundColor(new Color(0, 0, 0, 110))
            .borderRadius(14)
            .border(1, VIOLET)
            .padding(8)
            .margin(6)
        );
        add(type(JSlider.class).group(Skin.EQ_BAR), it -> it
            .backgroundColor(new Color(0, 0, 0, 0)).padding(0)
        );
        add(group(Skin.EQ_LABEL), it -> it
            .componentFont(f -> f.family("Serif").size(11).color(GOLD))
        );

        add(group(Skin.GENRE_PANEL), it -> it.backgroundColor(new Color(0, 0, 0, 0)).margin(0));
        add(type(JToggleButton.class).group(Skin.CHIP), it -> {
            JToggleButton b = it.component();
            boolean on = b.isSelected();
            return it
                .backgroundColor(on ? new Color(255, 214, 120, 200) : new Color(0, 0, 0, 110))
                .borderRadius(20)
                .border(1, on ? GOLD : VIOLET)
                .padding(4, 12, 4, 12)
                .margin(3)
                .componentFont(f -> f.family("Serif").size(12).color(on ? VOID : GLOW))
                .shadow("g", s -> s.color(on ? new Color(255, 214, 120, 140) : new Color(0,0,0,0))
                    .offset(0).blurRadius(on ? 12 : 0))
                .cursor(UI.Cursor.HAND);
        });

        add(group(Skin.PLAYLIST), it -> it
            .backgroundColor(new Color(0, 0, 0, 110))
            .borderRadius(14)
            .border(1, VIOLET)
            .padding(0)
            .margin(8)
        );
        add(group(Skin.PLAYLIST_ITEM), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .borderAt(UI.Edge.BOTTOM, 1, new Color(160, 90, 220, 100))
            .padding(8, 12, 8, 12)
            .componentFont(f -> f.family("Serif").size(13).color(GLOW))
        );
        add(group(Skin.PLAYLIST_DURATION), it -> it
            .componentFont(f -> f.family("Serif").size(11).color(GOLD).posture(0.2f))
        );

        add(group(Skin.STATUS), it -> it
            .backgroundColor(new Color(0, 0, 0, 110))
            .borderAt(UI.Edge.TOP, 1, VIOLET)
            .padding(6, 12, 6, 12)
            .componentFont(f -> f.family("Serif").size(11).color(GOLD).posture(0.2f))
        );
        add(group(Skin.SECTION_LABEL), it -> it
            .componentFont(f -> f.family("Serif").size(13).weight(2).color(GOLD))
            .padding(2, 4, 2, 4)
        );

        add(group(Skin.DECOR), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .painter(UI.Layer.BACKGROUND, g2 -> {
                int w = it.componentWidth(), h = it.componentHeight();
                g2.setColor(new Color(255, 214, 120, 180));
                int cy = h / 2;
                g2.setStroke(new java.awt.BasicStroke(1f));
                g2.drawLine(0, cy, w, cy);
                long seed = 7L;
                for (int i = 0; i < 22; i++) {
                    seed = seed * 6364136223846793005L + 1442695040888963407L;
                    int x = (int) Math.floorMod(seed >>> 16, Math.max(1, w - 4)) + 2;
                    seed = seed * 6364136223846793005L + 1442695040888963407L;
                    int y = (int) Math.floorMod(seed >>> 16, Math.max(1, h - 4)) + 2;
                    int r = 1 + (int) Math.floorMod(seed, 3);
                    g2.fillOval(x - r, y - r, r * 2, r * 2);
                }
            })
        );

        add(type(JComboBox.class).group(Skin.THEME_PICKER), it -> it
            .backgroundColor(new Color(0, 0, 0, 110))
            .border(1, GOLD)
            .borderRadius(20)
            .padding(2, 8, 2, 8)
            .componentFont(f -> f.family("Serif").size(13).color(GOLD))
            .cursor(UI.Cursor.HAND)
        );

        commonTextField(new Color(0, 0, 0, 110), GOLD, GLOW);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Theme 5 — PASTEL BLOOM
    //  Pink and mint, generous rounding, soft puff shadows, sans-serif.
    // ════════════════════════════════════════════════════════════════════════

    private void configurePastel() {
        Color SKY     = new Color(255, 232, 240);  // soft pink page
        Color CARD    = new Color(255, 255, 255);
        Color INK     = new Color( 90,  64,  90);
        Color MINT    = new Color(150, 220, 195);
        Color CORAL   = new Color(255, 145, 165);
        Color SUN     = new Color(255, 205, 130);

        add(group(Skin.FRAME), it -> it
            .backgroundColor(SKY)
            .foundationColor(SKY)
            .padding(18)
            .gradient("sky", g -> g
                .span(UI.Span.TOP_TO_BOTTOM)
                .colors(new Color(255, 240, 248), new Color(220, 245, 240))
            )
            .componentFont(f -> f.family("SansSerif").size(13).color(INK))
        );

        add(group(Skin.HEADER), it -> it
            .backgroundColor(CARD)
            .borderRadius(22)
            .padding(10, 18, 10, 18)
            .margin(0, 0, 8, 0)
            .shadow("puff", s -> s.color(new Color(255, 145, 165, 80))
                .offset(0, 6).blurRadius(18).spreadRadius(0))
        );
        add(group(Skin.APP_TITLE), it -> it
            .componentFont(f -> f.family("SansSerif").size(22).weight(2).color(CORAL))
        );
        add(group(Skin.APP_SUBTITLE), it -> it
            .componentFont(f -> f.family("SansSerif").size(11).color(INK).posture(0.15f))
        );

        add(group(Skin.NOW_PLAYING), it -> it
            .backgroundColor(CARD)
            .borderRadius(28)
            .padding(20)
            .margin(8)
            .shadow("puff", s -> s.color(new Color(255, 145, 165, 110))
                .offset(0, 10).blurRadius(28).spreadRadius(0))
        );

        add(group(Skin.ALBUM_ART), it -> it
            .borderRadius(28)
            .gradient("bubble", g -> g
                .type(UI.GradientType.RADIAL)
                .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                .boundary(UI.ComponentBoundary.BORDER_TO_INTERIOR)
                .size(Math.min(it.componentWidth(), it.componentHeight()))
                .offset(- it.componentWidth() * 0.15, - it.componentHeight() * 0.15)
                .colors(SUN, CORAL, new Color(190, 140, 220))
                .clipTo(UI.ComponentArea.BODY)
            )
            .shadow("puff", s -> s.color(new Color(255, 145, 165, 130))
                .offset(0, 12).blurRadius(28).spreadRadius(0))
        );

        add(group(Skin.TRACK_TITLE), it -> it
            .componentFont(f -> f.family("SansSerif").size(20).weight(2).color(INK))
        );
        add(group(Skin.TRACK_ARTIST), it -> it
            .componentFont(f -> f.family("SansSerif").size(13).color(CORAL))
        );

        add(group(Skin.TRANSPORT), it -> it.backgroundColor(new Color(0, 0, 0, 0)).margin(0));
        add(type(AbstractButton.class).group(Skin.PLAY_BUTTON), it -> it
            .backgroundColor(CORAL)
            .borderRadius(1000)
            .padding(12, 22, 12, 22)
            .margin(8)
            .componentFont(f -> f.family("SansSerif").size(18).weight(2).color(Color.WHITE))
            .gradient("hi", g -> g
                .span(UI.Span.TOP_TO_BOTTOM)
                .colors(new Color(255, 175, 195), CORAL)
            )
            .shadow("puff", s -> s.color(new Color(255, 145, 165, 180))
                .offset(0, 6).blurRadius(16).spreadRadius(0))
            .cursor(UI.Cursor.HAND)
        );
        add(type(AbstractButton.class).group(Skin.NAV_BUTTON), it -> it
            .backgroundColor(MINT)
            .borderRadius(1000)
            .padding(8, 16, 8, 16)
            .margin(6)
            .componentFont(f -> f.family("SansSerif").size(13).weight(2).color(INK))
            .shadow("puff", s -> s.color(new Color(150, 220, 195, 160))
                .offset(0, 4).blurRadius(10))
            .cursor(UI.Cursor.HAND)
        );

        add(type(JSlider.class).group(Skin.PROGRESS), it -> it
            .backgroundColor(new Color(255, 240, 245))
            .borderRadius(20)
            .padding(4)
            .cursor(UI.Cursor.HAND)
        );
        add(type(JSlider.class).group(Skin.VOLUME), it -> it
            .backgroundColor(new Color(225, 245, 235))
            .borderRadius(20)
            .padding(4)
            .cursor(UI.Cursor.HAND)
        );

        add(group(Skin.EQ_PANEL), it -> it
            .backgroundColor(new Color(255, 245, 250))
            .borderRadius(22)
            .padding(10)
            .margin(6)
            .shadow("puff", s -> s.color(new Color(255, 145, 165, 90))
                .offset(0, 6).blurRadius(14))
        );
        add(type(JSlider.class).group(Skin.EQ_BAR), it -> it
            .backgroundColor(new Color(0, 0, 0, 0)).padding(0)
        );
        add(group(Skin.EQ_LABEL), it -> it
            .componentFont(f -> f.family("SansSerif").size(10).color(CORAL))
        );

        add(group(Skin.GENRE_PANEL), it -> it.backgroundColor(new Color(0, 0, 0, 0)).margin(0));
        add(type(JToggleButton.class).group(Skin.CHIP), it -> {
            JToggleButton b = it.component();
            boolean on = b.isSelected();
            return it
                .backgroundColor(on ? CORAL : new Color(255, 245, 248))
                .borderRadius(1000)
                .padding(4, 12, 4, 12)
                .margin(3)
                .componentFont(f -> f.family("SansSerif").size(12)
                    .color(on ? Color.WHITE : INK))
                .shadow("puff", s -> s.color(on ? new Color(255, 145, 165, 160)
                                              : new Color(0, 0, 0, 30))
                    .offset(0, on ? 5 : 2).blurRadius(on ? 12 : 5))
                .cursor(UI.Cursor.HAND);
        });

        add(group(Skin.PLAYLIST), it -> it
            .backgroundColor(CARD)
            .borderRadius(22)
            .padding(0)
            .margin(8)
            .shadow("puff", s -> s.color(new Color(0, 0, 0, 30))
                .offset(0, 6).blurRadius(16))
        );
        add(group(Skin.PLAYLIST_ITEM), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .borderAt(UI.Edge.BOTTOM, 1, new Color(255, 145, 165, 90))
            .padding(8, 14, 8, 14)
            .componentFont(f -> f.family("SansSerif").size(13).color(INK))
        );
        add(group(Skin.PLAYLIST_DURATION), it -> it
            .componentFont(f -> f.family("SansSerif").size(11).color(CORAL))
        );

        add(group(Skin.STATUS), it -> it
            .backgroundColor(new Color(255, 232, 240, 0))
            .padding(6, 12, 6, 12)
            .componentFont(f -> f.family("SansSerif").size(11).color(INK).posture(0.15f))
        );
        add(group(Skin.SECTION_LABEL), it -> it
            .componentFont(f -> f.family("SansSerif").size(13).weight(2).color(CORAL))
            .padding(2, 4, 2, 4)
        );

        add(group(Skin.DECOR), it -> it
            .backgroundColor(new Color(0, 0, 0, 0))
            .painter(UI.Layer.BACKGROUND, g2 -> {
                int w = it.componentWidth(), h = it.componentHeight();
                Color[] palette = {CORAL, MINT, SUN};
                long seed = 11L;
                for (int i = 0; i < 14; i++) {
                    seed = seed * 6364136223846793005L + 1442695040888963407L;
                    int x = (int) Math.floorMod(seed >>> 16, Math.max(1, w - 16));
                    seed = seed * 6364136223846793005L + 1442695040888963407L;
                    int y = (int) Math.floorMod(seed >>> 16, Math.max(1, h - 16));
                    seed = seed * 6364136223846793005L + 1442695040888963407L;
                    int d = 6 + (int) Math.floorMod(seed >>> 16, 12);
                    Color c = palette[(int) Math.floorMod(seed, 3)];
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 180));
                    g2.fillOval(x, y, d, d);
                }
            })
        );

        add(type(JComboBox.class).group(Skin.THEME_PICKER), it -> it
            .backgroundColor(MINT)
            .borderRadius(1000)
            .padding(2, 12, 2, 12)
            .componentFont(f -> f.family("SansSerif").size(13).color(INK))
            .cursor(UI.Cursor.HAND)
        );

        commonTextField(CARD, CORAL, INK);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Tiny helper applied per-theme to give text fields and combos a
    //  consistent baseline appearance (one less thing to repeat in each theme).
    // ────────────────────────────────────────────────────────────────────────

    private void commonTextField(Color bg, Color border, Color ink) {
        add(type(JTextComponent.class), it -> it
            .backgroundColor(bg)
            .border(1, border)
            .padding(4, 8, 4, 8)
            .margin(2)
            .componentFont(f -> f.color(ink))
        );
    }
}