package examples.trains.mvi;

/**
 *  Semantic style groups tagged onto components with {@code .group(...)}.
 *  Using an enum (rather than string tags) keeps the {@link TrainStyle}
 *  rules refactor-safe and type-checked.
 */
public enum Skin {
    FRAME,
    HEADER, APP_TITLE, APP_SUBTITLE,
    TOOLBAR, TOOL_LABEL,
    STATUS,
    CARD, CARD_TITLE, CARD_SUB,
    ACCENT_BUTTON,
    EMPTY
}