package examples.zen;

/**
 *  The set of swappable visual identities for the {@link ThemeGardenView}
 *  skeleton. Each constant maps to a complete configuration in
 *  {@link ThemedStyleSheet} — colors, fonts, gradients, noise, corners,
 *  shadows and decorative painters.
 *
 *  <p>The skeleton does not change between themes. Only the
 *  {@link ThemedStyleSheet} does — exactly like how the
 *  <a href="https://csszengarden.com/">CSS Zen Garden</a> showed off
 *  the expressive power of CSS by serving the same HTML with very
 *  different stylesheets.</p>
 */
public enum Theme {
    /** Black backdrop, electric cyan and magenta, RETRO scanline noise, glow shadows, monospace. */
    NEON_ARCADE("Neon Arcade"),
    /** Warm ivory page, brown serif ink, square corners, paper noise, hand-drawn flourishes. */
    PARCHMENT_CODEX("Parchment Codex"),
    /** Concrete grey slabs, thick black borders, harsh angles, no shadows, monospace shouting. */
    BRUTALIST_CONCRETE("Brutalist Concrete"),
    /** Deep navy night sky, radial gradients, twinkling pinpoints, serif starlight. */
    COSMIC_DRIFT("Cosmic Drift"),
    /** Pink and mint, generous rounding, soft puff shadows, rounded sans-serif. */
    PASTEL_BLOOM("Pastel Bloom");

    private final String pretty;

    Theme(String pretty) {
        this.pretty = pretty;
    }

    public String pretty() {
        return pretty;
    }

    @Override
    public String toString() {
        return pretty;
    }
}