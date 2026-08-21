package examples.laf.app;

/**
 *  Semantic style groups the atelier tags onto its <em>text</em> with
 *  {@code .group(...)}, and which {@link AtelierSheet} turns into fonts. Using
 *  an enum rather than string tags keeps the sheet's rules refactor-safe.
 *  <p>
 *  There is deliberately nothing here for surfaces, buttons or fields: under a
 *  SwingTree-backed look-and-feel those are the LAF's to paint, and a sheet rule
 *  for them would be resolved first and then overwritten. See
 *  {@link AtelierSheet} for the division of labour.
 */
public enum Skin
{
    /** The workshop's name in the header. */
    APP_TITLE,
    /** The live one-line summary under it. */
    APP_SUBTITLE,
    /** The heading of a card. */
    CARD_TITLE,
    /** The sentence under a card heading. */
    CARD_SUB,
    /** A small, letter-spaced heading inside a card: "STORE ROOM". */
    SECTION,
    /** The caption in front of a form field. */
    FIELD_LABEL,
    /** Small, quiet text: chip captions, loom sub-lines, hints. */
    META,
    /** What a card says when a filter has left it with nothing to show. */
    EMPTY,
    /** The status line along the bottom of the window. */
    STATUS,
    /** A figure worth reading from across the room. */
    FIGURE,
    /** The day book and the delivery note — a document, not a widget. */
    DOCUMENT
}
