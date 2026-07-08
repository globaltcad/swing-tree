package examples.almanack.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.Association;
import sprouts.Tuple;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 *  The single immutable root of the whole almanack state (MVI / MVL).
 *  <p>
 *  The {@link AlmanackView} never holds Swing state of its own: it zooms lenses into
 *  the fields of this class and renders a pure function of it. Every user action
 *  produces a <em>new</em> {@code AlmanackViewModel} through the Lombok-generated
 *  withers, and the lenses fire only for the slices that actually changed.
 *  <p>
 *  Note the deliberate split between {@link #pages()} and {@link #inks()}:
 *  the tuple of {@link Page}s drives the tab strip, so its items must stay stable
 *  while a page is merely being <i>written in</i>. The text therefore lives in the
 *  {@code inks} association, keyed by page id — typing changes only that map and
 *  never causes a tab to be rebuilt.
 *  <p>
 *  {@link #desiredPage()} is the star of this example: it is bound two-way to the
 *  selection index of the main tabbed pane and is <b>allowed to point at a page
 *  that does not (yet) exist</b>. SwingTree then keeps nothing selected and applies
 *  the index automatically the moment a matching tab appears — which is exactly
 *  what the "close &amp; re-open at the bookmark" feature relies on.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class AlmanackViewModel {

    private final Tuple<Page>              pages;        // the open pages, in tab order
    private final Association<UUID,String> inks;         // page id -> written text
    private final int                      desiredPage;  // desired selection index (may be out of range!)
    private final int                      bookmark;     // remembered page index for re-opening
    private final EditorMode               mode;         // how every page presents itself
    private final DrawerSection            drawer;       // which drawer tab is open (or NONE)
    private final boolean                  restoring;    // true while pages stream back in
    private final Tuple<String>            log;          // newest first

    /**
     *  The sections a naturalist's almanack may grow: {@link #openNextPage()} opens
     *  the first one whose title is not already among the open pages.
     */
    private static final String[][] SECTIONS = {
        { "✎", "Sightings", "Dawn chorus at the east ridge.\n\nTwo buzzards circling the fallow field,\na wren fussing in the hedge below." },
        { "❀", "Flora",     "The blackthorn is in full blossom a week early.\n\nFound wood anemones along the stream bank." },
        { "☂", "Weather",   "Light rain before sunrise, clearing by nine.\nWind from the west, gentle." },
        { "⚑", "Routes",    "Followed the old drover's path to the twin oaks.\nThe ford is passable again." },
        { "✦", "Night Sky", "Clear night.\nJupiter bright over the barn roof, Orion setting early now." },
        { "☾", "Camp Log",  "" },
        { "✂", "Specimens", "" },
        { "✿", "Sketches",  "" },
    };

    /** The initial state: three pages open, reading the second, bookmark on the third. */
    public static AlmanackViewModel initial() {
        AlmanackViewModel vm = new AlmanackViewModel(
            Tuple.of(Page.class),
            Association.between(UUID.class, String.class),
            1,     // start on the second page — bound before any tab exists, so it is
                   // briefly deferred and applied as soon as the second tab appears
            2,
            EditorMode.WRITE,
            DrawerSection.LOG,
            false,
            Tuple.of("Welcome to the almanack. Every tab in this window is property-bound.")
        );
        for ( int i = 0; i < 3; i++ )
            vm = vm.openNextPage();
        return vm;
    }

    // ── Business logic — pure functions from one state to the next ──────────────

    /** Opens the first almanack section not already open, or a numbered loose leaf. */
    public AlmanackViewModel openNextPage() {
        for ( String[] section : SECTIONS ) {
            String title = section[1];
            if ( pages.none( p -> p.title().equals(title) ) ) {
                Page page = Page.of(section[0], title);
                return withPages(pages.add(page)).withInks(inks.put(page.id(), section[2]));
            }
        }
        Page page = Page.of("✧", "Loose Leaf " + (pages.size() + 1));
        return withPages(pages.add(page)).withInks(inks.put(page.id(), ""));
    }

    public AlmanackViewModel closePage( UUID id ) {
        return withPages(pages.removeIf( p -> p.id().equals(id) )).withInks(inks.remove(id));
    }

    public String inkOf( UUID id ) {
        return inks.get(id).orElse("");
    }

    public AlmanackViewModel withInkOf( UUID id, String text ) {
        return withInks(inks.put(id, text));
    }

    public int wordCountOf( UUID id ) {
        String ink = inkOf(id).trim();
        return ink.isEmpty() ? 0 : ink.split("\\s+").length;
    }

    /** The live tab title: glyph, current title, and a dot once the page holds ink. */
    public String tabTitleOf( UUID id ) {
        return pages.stream()
                    .filter( p -> p.id().equals(id) ).findFirst()
                    .map( p -> p.glyph() + "  " + p.title() + ( wordCountOf(id) > 0 ? " ·" : "" ) )
                    .orElse("");
    }

    public boolean hasOpenPage() {
        return desiredPage >= 0 && desiredPage < pages.size();
    }

    /** The title of the page the desired index points to, or nothing to show. */
    public String currentTitle() {
        return hasOpenPage() ? pages.get(desiredPage).title() : "";
    }

    /** Renames the page the desired index points to (a no-op when there is none). */
    public AlmanackViewModel renameCurrent( String newTitle ) {
        if ( !hasOpenPage() )
            return this;
        Page page = pages.get(desiredPage);
        if ( page.title().equals(newTitle) )
            return this;
        return withPages(pages.setAt(desiredPage, page.withTitle(newTitle)));
    }

    /** One human sentence describing how the desired index relates to reality. */
    public String selectionSummary() {
        if ( restoring )
            return "Re-opening the notebook — pages return one by one…";
        if ( desiredPage < 0 )
            return "No page requested (index -1), so nothing is selected.";
        if ( desiredPage < pages.size() )
            return "Reading page " + (desiredPage + 1) + " of " + pages.size() + ".";
        return "Waiting for page " + (desiredPage + 1) + " — only " + pages.size() +
               " exist. It will be selected the moment it appears.";
    }

    /** Prepends a time-stamped entry to the event log, capped at 60 entries. */
    public AlmanackViewModel logged( String entry ) {
        String stamped = LocalTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "   " + entry;
        Tuple<String> newLog = log.addAt(0, stamped);
        if ( newLog.size() > 60 )
            newLog = newLog.slice(0, 60);
        return withLog(newLog);
    }
}
