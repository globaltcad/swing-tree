package examples.trains.mvi;

import swingtree.UI;
import swingtree.style.StyleSheet;

import javax.swing.AbstractButton;

/**
 *  One mutable {@link StyleSheet} that paints the app chrome (frame, header,
 *  cards, status bar, primary buttons) from the current {@link Theme}'s
 *  palette. Calling {@link #setTheme(Theme)} swaps the palette and
 *  {@code reconfigure()}s, instantly repainting everything built inside the
 *  {@code UI.use(sheet, ...)} scope — the standard SwingTree hot-swap idiom.
 *  <p>
 *  Per-row, data-dependent painting (line badges, the clickable departure
 *  cards, the highlighted "you are here" stop) is done with inline
 *  {@code withStyle} in {@link TrainsView}, because those depend on each row's
 *  own model rather than on a global rule.
 */
public final class TrainStyle extends StyleSheet {

    private Theme theme = Theme.LIGHT;

    public Theme theme() { return theme; }

    public void setTheme(Theme newTheme) {
        if (newTheme != theme) {
            this.theme = newTheme;
            reconfigure();
        }
    }

    @Override
    protected void configure() {
        Theme.Palette p = theme.palette();

        // The page itself.
        add(group(Skin.FRAME), it -> it
            .backgroundColor(p.page)
            .foundationColor(p.page)
            .padding(0)
        );

        // Top header bar with an accent underline.
        add(group(Skin.HEADER), it -> it
            .backgroundColor(p.card)
            .borderAt(UI.Edge.BOTTOM, 3, p.accent)
            .padding(14, 20, 14, 20)
            .shadow("h", s -> s.color(shadow()).offset(0, 2).blurRadius(10).spreadRadius(0))
        );
        add(group(Skin.APP_TITLE), it -> it
            .componentFont(f -> f.family("SansSerif").size(22).weight(2f).color(p.text))
        );
        add(group(Skin.APP_SUBTITLE), it -> it
            .componentFont(f -> f.family("SansSerif").size(12).color(p.subtext))
        );

        // Search / filter toolbar.
        add(group(Skin.TOOLBAR), it -> it
            .backgroundColor(p.card)
            .borderAt(UI.Edge.BOTTOM, 1, p.border)
            .padding(10, 20, 10, 20)
        );
        add(group(Skin.TOOL_LABEL), it -> it
            .componentFont(f -> f.family("SansSerif").size(12).weight(2f).color(p.subtext))
        );

        // Status / error line.
        add(group(Skin.STATUS), it -> it
            .backgroundColor(p.page)
            .padding(6, 22, 6, 22)
            .componentFont(f -> f.family("SansSerif").size(12).color(p.subtext))
        );

        // Card containers (the board panel and the route panel).
        add(group(Skin.CARD), it -> it
            .backgroundColor(p.card)
            .borderRadius(14)
            .border(1, p.border)
            .margin(10)
            .padding(0)
            .shadow("c", s -> s.color(shadow()).offset(0, 4).blurRadius(16).spreadRadius(0))
        );
        add(group(Skin.CARD_TITLE), it -> it
            .componentFont(f -> f.family("SansSerif").size(15).weight(2f).color(p.text))
        );
        add(group(Skin.CARD_SUB), it -> it
            .componentFont(f -> f.family("SansSerif").size(12).color(p.subtext))
        );

        // Primary (accent) buttons — Search, Refresh.
        add(type(AbstractButton.class).group(Skin.ACCENT_BUTTON), it -> it
            .backgroundColor(p.accent)
            .foregroundColor(p.onAccent)
            .borderRadius(9)
            .border(0, p.accent)
            .padding(6, 16, 6, 16)
            .margin(2)
            .componentFont(f -> f.family("SansSerif").size(13).weight(2f).color(p.onAccent))
            .cursor(UI.Cursor.HAND)
        );

        // Empty-state hint text.
        add(group(Skin.EMPTY), it -> it
            .componentFont(f -> f.family("SansSerif").size(14).color(p.subtext).posture(0.12f))
        );
    }

    private java.awt.Color shadow() {
        return theme.isDark() ? new java.awt.Color(0, 0, 0, 120)
                              : new java.awt.Color(20, 30, 60, 35);
    }
}