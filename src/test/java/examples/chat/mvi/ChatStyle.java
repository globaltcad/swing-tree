package examples.chat.mvi;

import swingtree.UI;
import swingtree.style.StyleSheet;

import javax.swing.AbstractButton;
import javax.swing.text.JTextComponent;

/**
 *  One mutable {@link StyleSheet} painting all of the app chrome — the page, the
 *  header, the cards, the composer, the buttons — from whichever {@link Theme}
 *  is currently selected.
 *  <p>
 *  {@link #setTheme(Theme)} swaps the palette and calls {@code reconfigure()},
 *  which re-runs {@link #configure()} and instantly repaints every component
 *  built inside the surrounding {@code UI.use(sheet, ..)} scope. That is the
 *  whole theme switch: no component is touched, rebuilt or even told about it.
 *  <p>
 *  Note what is <b>not</b> in here. Room chips, avatars, message bubbles and
 *  reaction pills all depend on the <em>data</em> (a room's hue, a member's
 *  presence, who wrote a message), not on the skin, so they are styled inline in
 *  {@link ChatView} with the property-bound
 *  {@code withStyle(prop, (item, it) -> ..)}. A style sheet is for rules that
 *  hold for a whole category of component; anything per-item belongs next to the
 *  component that draws it.
 */
public final class ChatStyle extends StyleSheet {

    private static final String FONT = "SansSerif";

    private Theme theme;

    public ChatStyle() { this(Theme.DARK); }

    public ChatStyle( Theme initial ) { this.theme = initial; }

    public Theme theme() { return theme; }

    public void setTheme( Theme newTheme ) {
        if ( newTheme != theme ) {
            this.theme = newTheme;
            reconfigure();
        }
    }

    @Override
    protected void configure() {
        Theme.Palette p = theme.palette();

        // ── The page itself ────────────────────────────────────────────────
        // A flat fill reads as dead space, so the backdrop gets two almost
        // invisible layers on top of the base colour: a wide radial glow in the
        // corner and a fine grain. Neither is an image — both are painted by
        // the style engine at whatever size the window happens to be.
        add(group(Skin.FRAME), it -> it
            .backgroundColor(p.page)
            .foundationColor(p.page)
            .padding(0)
            .gradient("canopy", g -> g
                .type(UI.GradientType.RADIAL)
                .boundary(UI.ComponentBoundary.OUTER_TO_EXTERIOR)
                .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                .offset(it.componentWidth() * 0.16, 0)
                .size(Math.max(it.componentWidth(), it.componentHeight()) * 0.85f)
                .colors(p.glow, new java.awt.Color(0, 0, 0, 0))
                .clipTo(UI.ComponentArea.BODY)
            )
            .noise("canopy-grain", n -> n
                .function(UI.NoiseType.FOLIAGE)
                .colors(new java.awt.Color(0, 0, 0, 0), p.grain)
                .scale(1.6)
                .clipTo(UI.ComponentArea.BODY)
            )
            .componentFont(f -> f.family(FONT).size(13).color(p.text))
        );

        // The page-wide scroll pane has to disappear into the page, or its
        // viewport shows up as a rectangle of the wrong colour.
        add(group(Skin.PAGE_SCROLL), it -> it
            .backgroundColor("transparent")
            .border(0, p.page)
            .padding(0)
        );

        // ── Top chrome ─────────────────────────────────────────────────────
        add(group(Skin.HEADER), it -> it
            .backgroundColor(p.card)
            .borderAt(UI.Edge.BOTTOM, 2, p.accent)
            .padding(12, 20, 12, 20)
            .shadow("header", s -> s.color(p.shadow).offset(0, 3).blurRadius(14))
        );
        add(group(Skin.APP_TITLE), it -> it
            .componentFont(f -> f.family(FONT).size(21).weight(2f).color(p.text))
        );
        add(group(Skin.APP_SUBTITLE), it -> it
            .componentFont(f -> f.family(FONT).size(12).color(p.subtext))
        );

        // ── Cards ──────────────────────────────────────────────────────────
        add(group(Skin.CARD), it -> it
            .backgroundColor(p.card)
            .borderRadius(16)
            .border(1, p.border)
            .margin(0)
            .padding(0)
            .shadow("card", s -> s.color(p.shadow).offset(0, 4).blurRadius(18))
            .noise("weave", n -> n
                .function(UI.NoiseType.FABRIC)
                .colors(new java.awt.Color(0, 0, 0, 0), p.grain)
                .scale(0.35)
                .clipTo(UI.ComponentArea.BODY)
            )
        );
        add(group(Skin.CARD_TITLE), it -> it
            .componentFont(f -> f.family(FONT).size(15).weight(2f).color(p.text))
        );
        add(group(Skin.CARD_SUB), it -> it
            .componentFont(f -> f.family(FONT).size(12).color(p.subtext))
        );
        add(group(Skin.SECTION_LABEL), it -> it
            .componentFont(f -> f.family(FONT).size(11).weight(2f).spacing(0.14f).color(p.subtext))
        );
        add(group(Skin.META), it -> it
            .componentFont(f -> f.family(FONT).size(11).color(p.subtext))
        );
        add(group(Skin.EMPTY), it -> it
            .componentFont(f -> f.family(FONT).size(13).posture(0.14f).color(p.subtext))
            .padding(26, 18, 26, 18)
        );

        // ── Status line ────────────────────────────────────────────────────
        add(group(Skin.STATUS), it -> it
            .backgroundColor(p.page)
            .padding(7, 22, 8, 22)
            .componentFont(f -> f.family(FONT).size(12).color(p.subtext))
        );

        // ── Buttons ────────────────────────────────────────────────────────
        add(type(AbstractButton.class).group(Skin.ACCENT_BUTTON), it -> it
            .backgroundColor(p.accent)
            .foregroundColor(p.onAccent)
            .borderRadius(10)
            .border(0, p.accent)
            .padding(7, 18, 7, 18)
            .margin(0)
            .componentFont(f -> f.family(FONT).size(13).weight(2f).color(p.onAccent))
            .cursor(UI.Cursor.HAND)
        );
        add(type(AbstractButton.class).group(Skin.GHOST_BUTTON), it -> it
            .backgroundColor(p.raised)
            .foregroundColor(p.text)
            .borderRadius(10)
            .border(1, p.border)
            .padding(6, 14, 6, 14)
            .margin(0)
            .componentFont(f -> f.family(FONT).size(12).color(p.text))
            .cursor(UI.Cursor.HAND)
        );
        add(type(AbstractButton.class).group(Skin.ICON_BUTTON), it -> it
            .backgroundColor(new java.awt.Color(0, 0, 0, 0))
            .foregroundColor(p.subtext)
            .borderRadius(9)
            .border(0, p.border)
            .padding(3, 7, 3, 7)
            .margin(0)
            .componentFont(f -> f.family(FONT).size(13).color(p.subtext))
            .cursor(UI.Cursor.HAND)
        );

        // ── Inputs ─────────────────────────────────────────────────────────
        add(type(JTextComponent.class).group(Skin.SEARCH_FIELD), it -> it
            .backgroundColor(p.raised)
            .foregroundColor(p.text)
            .borderRadius(10)
            .border(1, p.border)
            .padding(6, 12, 6, 12)
            .componentFont(f -> f.family(FONT).size(13).color(p.text))
        );
        add(group(Skin.COMPOSER), it -> it
            .backgroundColor(p.raised)
            .borderRadius(14)
            .border(1, p.border)
            .padding(8)
            .margin(0)
        );
        // The input reads as a well sunk into the composer. Stating its surface
        // explicitly matters: left alone, the look-and-feel paints a text-field
        // background of its own underneath and the composer ends up wearing two
        // competing borders.
        add(type(JTextComponent.class).group(Skin.COMPOSER_INPUT), it -> it
            .backgroundColor(p.card)
            .foregroundColor(p.text)
            .borderRadius(10)
            .border(1, p.border)
            .padding(5, 10, 5, 10)
            .componentFont(f -> f.family(FONT).size(14).color(p.text))
        );
    }
}
