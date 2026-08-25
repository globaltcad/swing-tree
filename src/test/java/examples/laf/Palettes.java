package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;

import java.awt.Color;

/**
 *  The colour sets behind {@link SwingTreeLookAndFeel.PalettePreset}.
 *  <p>
 *  A palette is the one thing every style preset and every symbol set reads through, and it is
 *  named semantically rather than literally: a rule asks for "the surface a raised control is
 *  filled with", never for a colour. That is what lets any of these be paired with any preset, and
 *  it is why a theme is re-tinted by swapping one of these rather than by editing the rules.
 *  <p>
 *  Each palette starts from {@link Palette#neutral()} and names every slot, so a colour is never
 *  inherited by accident from whatever the neutral placeholder happened to be.
 */
final class Palettes
{
    private Palettes() {}

    /** What a browser shows a page with no stylesheet: a white sheet, black text, grey chrome. */
    static final Palette BLANK = Palette.neutral()
            .background     (rgb(0xFFFFFF))
            .surface        (rgb(0xF0F0F0))
            .surfaceHover   (rgb(0xE8E8E8))
            .surfacePressed (rgb(0xD8D8D8))
            .surfaceDisabled(rgb(0xF5F5F5))
            .surfaceField   (rgb(0xFFFFFF))
            .border         (rgb(0xA0A0A0))
            .borderSoft     (rgb(0xD0D0D0))
            .text           (rgb(0x000000))
            .textMuted      (rgb(0x555555))
            .textDisabled   (rgb(0xA0A0A0))
            .accent         (rgb(0x2563EB))
            .accentSoft     (rgb(0xBFD7FF))
            .textureLight   (rgb(0xFFFFFF))
            .textureDark    (rgb(0xFFFFFF))
            .primary        (rgb(0x2563EB))
            .primaryHover   (rgb(0x1D4ED8))
            .primaryPressed (rgb(0x1E40AF))
            .danger         (rgb(0xDC2626))
            .dangerHover    (rgb(0xB91C1C))
            .dangerPressed  (rgb(0x991B1B))
            .onFilled       (rgb(0xFFFFFF));

    /** Aged paper, raw linen and weathered taupe stone, with a deep olive accent. */
    static final Palette LINEN = Palette.neutral()
            .background     (rgb(0xF5F1E8))
            .surface        (rgb(0xFBF8F0))
            .surfaceHover   (rgb(0xFFFCF5))
            .surfacePressed (rgb(0xE8E2D4))
            .surfaceDisabled(rgb(0xEFEBE0))
            .surfaceField   (rgb(0xFCFAF3))
            .border         (rgb(0xC9C0AB))
            .borderSoft     (rgb(0xDCD4BE))
            .text           (rgb(0x3D352A))
            .textMuted      (rgb(0x8A7F6A))
            .textDisabled   (rgb(0xB5AC9B))
            .accent         (rgb(0x7A6E55))
            .accentSoft     (rgb(0xD8CCAE))
            .textureLight   (rgb(0xF9F5EC))
            .textureDark    (rgb(0xF0ECE3))
            .primary        (rgb(0x365C3B))
            .primaryHover   (rgb(0x416B46))
            .primaryPressed (rgb(0x2B4A30))
            .danger         (rgb(0x8B3A3A))
            .dangerHover    (rgb(0x9C4545))
            .dangerPressed  (rgb(0x742E2E))
            .onFilled       (rgb(0xFAF6EC));

    /** A dark room: near-black surfaces a few steps apart, cool grey text, one bright blue. */
    static final Palette MIDNIGHT = Palette.neutral()
            .background     (rgb(0x16181D))
            .surface        (rgb(0x22252C))
            .surfaceHover   (rgb(0x2C3038))
            .surfacePressed (rgb(0x14161A))
            .surfaceDisabled(rgb(0x1C1F24))
            .surfaceField   (rgb(0x1B1E24))
            .border         (rgb(0x3A3F49))
            .borderSoft     (rgb(0x2A2E36))
            .text           (rgb(0xE6E8EC))
            .textMuted      (rgb(0x9AA1AD))
            .textDisabled   (rgb(0x5A6069))
            .accent         (rgb(0x6EA8FE))
            .accentSoft     (rgb(0x2B3A55))
            .textureLight   (rgb(0x191C21))
            .textureDark    (rgb(0x131519))
            .primary        (rgb(0x3B82F6))
            .primaryHover   (rgb(0x2F6FE0))
            .primaryPressed (rgb(0x2559B4))
            .danger         (rgb(0xE5484D))
            .dangerHover    (rgb(0xCE3B41))
            .dangerPressed  (rgb(0xA82F34))
            .onFilled       (rgb(0xFFFFFF));

    /** Sky, water and glass, with the saturated grass green of every 2007 affirmative button. */
    static final Palette AERO = Palette.neutral()
            .background     (rgb(0xCFE9F7))
            .surface        (rgb(0xE8F6FD))
            .surfaceHover   (rgb(0xF4FBFF))
            .surfacePressed (rgb(0xB8DDF0))
            .surfaceDisabled(rgb(0xDCE9F0))
            .surfaceField   (rgb(0xFFFFFF))
            .border         (rgb(0x7FB4D4))
            .borderSoft     (rgb(0xB9DBEE))
            .text           (rgb(0x103A52))
            .textMuted      (rgb(0x4A7B96))
            .textDisabled   (rgb(0x7C9DB0))
            .accent         (rgb(0x0A84C8))
            .accentSoft     (rgb(0xA9DCF5))
            .textureLight   (rgb(0xDFF1FB))
            .textureDark    (rgb(0xC8E5F6))
            .primary        (rgb(0x35A64B))
            .primaryHover   (rgb(0x43BB5A))
            .primaryPressed (rgb(0x2A8A3C))
            .danger         (rgb(0xD2453A))
            .dangerHover    (rgb(0xE2564B))
            .dangerPressed  (rgb(0xAE372E))
            .onFilled       (rgb(0xFFFFFF));

    /**
     *  One single cool grey for the window and everything standing on it. Soft UI needs exactly
     *  that: with nothing to tell a card from its background by colour, the light has to do all
     *  of the work, which is the whole point of the idiom.
     */
    static final Palette CLAY = Palette.neutral()
            .background     (rgb(0xE0E5EC))
            .surface        (rgb(0xE0E5EC))
            .surfaceHover   (rgb(0xE6EBF2))
            .surfacePressed (rgb(0xD5DAE1))
            .surfaceDisabled(rgb(0xE0E5EC))
            .surfaceField   (rgb(0xE0E5EC))
            .border         (rgb(0xC8D0DA))
            .borderSoft     (rgb(0xD6DDE6))
            .text           (rgb(0x4A5568))
            .textMuted      (rgb(0x7C8899))
            .textDisabled   (rgb(0xAEB8C4))
            .accent         (rgb(0x5B6ABF))
            .accentSoft     (rgb(0xC7CEEA))
            .textureLight   (rgb(0xE4E9F0))
            .textureDark    (rgb(0xDCE1E8))
            .primary        (rgb(0x5B6ABF))
            .primaryHover   (rgb(0x6A78C9))
            .primaryPressed (rgb(0x4C5AA8))
            .danger         (rgb(0xC05A5A))
            .dangerHover    (rgb(0xCE6868))
            .dangerPressed  (rgb(0xA54B4B))
            .onFilled       (rgb(0xFFFFFF));

    /** White cards on an off-white ground, greys in even steps, one indigo carrying every accent. */
    static final Palette MATERIAL = Palette.neutral()
            .background     (rgb(0xFAFAFA))
            .surface        (rgb(0xFFFFFF))
            .surfaceHover   (rgb(0xF5F5F5))
            .surfacePressed (rgb(0xEEEEEE))
            .surfaceDisabled(rgb(0xF5F5F5))
            .surfaceField   (rgb(0xF1F1F3))
            .border         (rgb(0xBDBDBD))
            .borderSoft     (rgb(0xE0E0E0))
            .text           (rgb(0x212121))
            .textMuted      (rgb(0x757575))
            .textDisabled   (rgb(0x9E9E9E))
            .accent         (rgb(0x3F51B5))
            .accentSoft     (rgb(0xC5CAE9))
            .textureLight   (rgb(0xFAFAFA))
            .textureDark    (rgb(0xFAFAFA))
            .primary        (rgb(0x3F51B5))
            .primaryHover   (rgb(0x4B5CC0))
            .primaryPressed (rgb(0x303F9F))
            .danger         (rgb(0xD32F2F))
            .dangerHover    (rgb(0xE33A3A))
            .dangerPressed  (rgb(0xB71C1C))
            .onFilled       (rgb(0xFFFFFF));

    /**
     *  Bold, unmixed colour on a plain sheet, the way flat design uses it: nothing here is a
     *  shade of anything else, because with no shadow and no gradient to carry meaning the
     *  colour has to carry all of it.
     */
    static final Palette VIVID = Palette.neutral()
            .background     (rgb(0xF2F2F2))
            .surface        (rgb(0xFFFFFF))
            .surfaceHover   (rgb(0xE6E6E6))
            .surfacePressed (rgb(0xD4D4D4))
            .surfaceDisabled(rgb(0xEDEDED))
            .surfaceField   (rgb(0xFFFFFF))
            .border         (rgb(0x1A1A1A))
            .borderSoft     (rgb(0xD6D6D6))
            .text           (rgb(0x1A1A1A))
            .textMuted      (rgb(0x6B6B6B))
            .textDisabled   (rgb(0xB0B0B0))
            .accent         (rgb(0x0078D7))
            .accentSoft     (rgb(0xB5DCF7))
            .textureLight   (rgb(0xF2F2F2))
            .textureDark    (rgb(0xF2F2F2))
            .primary        (rgb(0x107C10))
            .primaryHover   (rgb(0x138A13))
            .primaryPressed (rgb(0x0B5A0B))
            .danger         (rgb(0xE81123))
            .dangerHover    (rgb(0xF32636))
            .dangerPressed  (rgb(0xC50F1F))
            .onFilled       (rgb(0xFFFFFF));

    /**
     *  A bench in a workshop: worn leather, card stock, writing paper, brass fittings and felt.
     *  Everything a skeuomorphic theme wants to pretend to be made of, and all of it light enough
     *  that the dark ink on top stays readable.
     */
    static final Palette WORKSHOP = Palette.neutral()
            .background     (rgb(0xA3947A))
            .surface        (rgb(0xE0D6BE))
            .surfaceHover   (rgb(0xEDE3CC))
            .surfacePressed (rgb(0xC4B79A))
            .surfaceDisabled(rgb(0xD2CAB6))
            .surfaceField   (rgb(0xF7F1E1))
            .border         (rgb(0x6F5F45))
            .borderSoft     (rgb(0xA08F70))
            .text           (rgb(0x33291C))
            .textMuted      (rgb(0x6B5C45))
            .textDisabled   (rgb(0x97896F))
            .accent         (rgb(0xA87C2C))
            .accentSoft     (rgb(0xE6D3A4))
            .textureLight   (rgb(0xB0A188))
            .textureDark    (rgb(0x94856B))
            .primary        (rgb(0x47693D))
            .primaryHover   (rgb(0x547A48))
            .primaryPressed (rgb(0x375130))
            .danger         (rgb(0x8E3B2E))
            .dangerHover    (rgb(0xA2483A))
            .dangerPressed  (rgb(0x6F2C22))
            .onFilled       (rgb(0xF8F2E2));

    /**
     *  Night sky through a frosted pane: a deep indigo ground with a violet and a magenta bloom
     *  in it, and white for everything the glass is made of. The two grain slots carry the two
     *  blooms, since a glass theme has no grain to spend them on and everything it does have
     *  depends on there being something vivid behind it.
     */
    static final Palette AURORA = Palette.neutral()
            .background     (rgb(0x241A4D))
            .surface        (rgb(0xFFFFFF))
            .surfaceHover   (rgb(0xFFFFFF))
            .surfacePressed (rgb(0xB9A9FF))
            .surfaceDisabled(rgb(0x8E86B0))
            .surfaceField   (rgba(0x120A28, 150))
            .border         (rgb(0xFFFFFF))
            .borderSoft     (rgb(0xC9C2E8))
            .text           (rgb(0xF2EFFF))
            .textMuted      (rgb(0xB7AEDC))
            .textDisabled   (rgb(0x7C74A0))
            .accent         (rgb(0x7DE2FF))
            .accentSoft     (rgb(0x3B2E6E))
            .textureLight   (rgb(0x6D3BFF))
            .textureDark    (rgb(0xFF4FD8))
            .primary        (rgb(0x6C5CE7))
            .primaryHover   (rgb(0x7E6FF0))
            .primaryPressed (rgb(0x5A4BD0))
            .danger         (rgb(0xFF6B81))
            .dangerHover    (rgb(0xFF8095))
            .dangerPressed  (rgb(0xE05468))
            .onFilled       (rgb(0xFFFFFF));

    private static Color rgb( int packed ) {
        return new Color((packed >> 16) & 0xFF, (packed >> 8) & 0xFF, packed & 0xFF);
    }

    /**
     *  The same, at less than full opacity. A palette slot is normally solid, but Swing fills a
     *  few areas - the strip a combo box shows its value in, the ground behind a list, a table
     *  and a tree - straight from a {@code UIDefaults} colour rather than from the component, so
     *  a theme made of glass has to hand it a colour it can see through.
     */
    private static Color rgba( int packed, int alpha ) {
        return new Color((packed >> 16) & 0xFF, (packed >> 8) & 0xFF, packed & 0xFF, alpha);
    }
}
