package examples.almanack.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;

import java.util.UUID;

/**
 *  The identity of one open page of the almanack: a stable {@link UUID}, a little
 *  glyph for the tab header and a title. A tuple of these drives the main tabbed
 *  pane through {@code addAll(Val<Tuple<Page>>, TabSupplier)}, so an instance only
 *  ever changes when a page is opened, closed or renamed — never while writing,
 *  because the written text (the "ink") lives in a separate association inside the
 *  {@link AlmanackViewModel}, keyed by {@link #id()}. This keeps the tuple diff
 *  small: typing does not rebuild any tab, only a rename rebuilds the one affected.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class Page {

    private final UUID   id;
    private final String glyph;
    private final String title;

    public static Page of( String glyph, String title ) {
        return new Page(UUID.randomUUID(), glyph, title);
    }

    @Override public String toString() { return glyph + " " + title; }
}
