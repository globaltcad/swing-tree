package examples.chat.mvi;

/**
 *  Semantic style groups tagged onto components with {@code .group(..)} and
 *  resolved by {@link ChatStyle}. An <b>enum</b> rather than string tags, so a
 *  rename is a compiler error instead of a silently unstyled panel.
 *  <p>
 *  Everything in here is <i>chrome</i>: rules that depend only on the theme.
 *  Anything that depends on a room's hue, a member's presence or a message's
 *  author is styled inline with {@code withStyle(prop, (item, it) -> ..)} in
 *  {@link ChatView} instead, because those are functions of the data, not of
 *  the skin.
 */
public enum Skin {
    FRAME,
    PAGE_SCROLL,
    HEADER, APP_TITLE, APP_SUBTITLE,
    CARD, CARD_TITLE, CARD_SUB,
    SECTION_LABEL,
    STATUS,
    ACCENT_BUTTON, GHOST_BUTTON, ICON_BUTTON,
    SEARCH_FIELD,
    COMPOSER, COMPOSER_INPUT,
    META,
    EMPTY
}
