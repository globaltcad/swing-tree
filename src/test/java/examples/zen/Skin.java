package examples.zen;

/**
 *  Semantic group tags used by the {@link ThemeGardenView} skeleton.
 *
 *  <p>The skeleton attaches one of these tags to every component, just like
 *  CSS classes in a Zen-Garden style HTML page. Each {@link Theme} then
 *  decides what every tag looks like — corners, gradients, fonts, shadows,
 *  noise, painters — without the skeleton having to be touched.</p>
 *
 *  <p>The point: the structural code in {@link ThemeGardenView} does not
 *  know what a "neon" or "parchment" or "brutalist" widget looks like.
 *  It only knows that <i>this is a play button</i>, <i>this is the album
 *  art slot</i>, <i>this is a chip</i>. The {@link ThemedStyleSheet}
 *  decides everything visual.</p>
 */
public enum Skin {
    /** The outermost frame — the page itself. Themes paint the backdrop here. */
    FRAME,

    /** The header bar containing the app title and the theme picker. */
    HEADER,
    APP_TITLE,
    APP_SUBTITLE,
    THEME_PICKER,

    /** The "now playing" pane. */
    NOW_PLAYING,
    ALBUM_ART,
    TRACK_TITLE,
    TRACK_ARTIST,

    /** Transport bar (the row with prev / play / next). */
    TRANSPORT,
    PLAY_BUTTON,
    NAV_BUTTON,

    /** Continuous controls. */
    PROGRESS,
    VOLUME,

    /** Equalizer column with vertical sliders. */
    EQ_PANEL,
    EQ_BAR,
    EQ_LABEL,

    /** Genre filter chips. */
    GENRE_PANEL,
    CHIP,

    /** Playlist list-of-tracks. */
    PLAYLIST,
    PLAYLIST_ITEM,
    PLAYLIST_DURATION,

    /** Status line at the bottom. */
    STATUS,

    /** Decorative ornament — themes may paint flourishes here. */
    DECOR,

    /** Inline section heading inside the right-hand pane. */
    SECTION_LABEL
}