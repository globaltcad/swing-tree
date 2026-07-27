package examples.almanack.mvi;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import swingtree.Tab;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.api.Layout;
import swingtree.layout.MigAddConstraint;
import swingtree.threading.EventProcessor;

import javax.swing.JPanel;
import java.awt.Color;
import java.util.UUID;

import static swingtree.UI.*;

/**
 *  <b>The Almanack — a naturalist's field notebook, held together by tab bindings.</b>
 *  <p>
 *  A small journal application in which every tab you can see is wired to the single
 *  immutable {@link AlmanackViewModel} through a different tab binding mechanism.
 *  It doubles as a manual test bed for properties bound to tabs — in particular for
 *  <i>deferred selection</i>: a bound selection index may point to a tab that does
 *  not exist yet, in which case nothing is selected until a matching tab appears.
 *  <p>
 *  The bindings on display:
 *  <ul>
 *    <li><b>{@code tabbedPane(Var<Integer>)}</b> — the main pane's selection index is
 *        bound two-way to {@code desiredPage}. The same property drives the numeric
 *        field and the arrows in the navigator, so all of them stay in lock-step.</li>
 *    <li><b>Deferred selection</b> — type an index beyond the last page (or press
 *        "Request page 8") and everything deselects; open pages until the index
 *        becomes valid and the page selects itself. The "close &amp; re-open"
 *        button replays this asynchronously: it empties the notebook, requests the
 *        bookmarked page <i>while no page exists</i>, and streams the pages back —
 *        the bookmarked page is picked up the moment it returns.</li>
 *    <li><b>{@code addAll(Val<Tuple<Page>>, TabSupplier)}</b> — the page tabs
 *        themselves are a function of the {@code pages} tuple: opening, closing and
 *        renaming pages is pure view-model arithmetic.</li>
 *    <li><b>Enum⇄index lenses</b> — the Write / Preview / Details sub-strip inside
 *        <i>every</i> page shares one {@code Var<Integer>} (the {@link EditorMode}
 *        enum seen through a lens), so all the strips and the toolbar radio buttons
 *        switch together. The drawer does the same with {@link DrawerSection},
 *        whose {@code NONE} maps to index {@code -1}: bound <i>before</i> any
 *        drawer tab exists (so it starts out deferred) and driven from the toolbar
 *        toggles through little boolean lenses, with {@code -1} keeping every tab
 *        deselected while the drawer is collapsed.</li>
 *    <li>Plus: {@code tab(Val<String>)} bound titles (watch the ink-dot appear as
 *        you write), {@code withTip(Val)} bound tooltips with a live word count,
 *        {@code isEnabledIf(..)} (Details is disabled while a page holds no ink),
 *        {@code withTabPlacementAt(Val<Side>)}, and {@code onSelection(..)} and
 *        {@code onTabMouseClick(..)} feeding the event log.</li>
 *  </ul>
 *  <p>
 *  <b>It also converges.</b> Drag the window narrow and the toolbar folds into
 *  lines, the drawer slides under the pages, the header stacks and the tab strip
 *  scrolls instead of growing rows. All of it is one {@code Var<Breakpoint>}
 *  feeding four {@code Val<Layout>} properties — a <i>reactive layout</i>, so
 *  nothing is rebuilt and every tab selection, caret and scroll position survives.
 *  See {@link Breakpoint} and the layout constants beside it.
 *  Run {@link #main(String[])} to open the notebook.
 */
public final class AlmanackView extends JPanel {

    // ── Palette — pressed-flower parchment ──────────────────────────────────────
    private static final Color PARCHMENT   = new Color(244, 238, 222);
    private static final Color CARD        = new Color(252, 249, 240);
    private static final Color INK         = new Color( 54,  48,  38);
    private static final Color FADED_INK   = new Color(138, 127,  98);
    private static final Color MOSS        = new Color( 62,  81,  62);
    private static final Color MOSS_LIGHT  = new Color(233, 238, 227);
    private static final Color CREAM       = new Color(243, 239, 226);
    private static final Color RUST        = new Color(176,  85,  47);
    private static final Color HAIRLINE    = new Color(210, 200, 175);

    private final Var<AlmanackViewModel> vm;

    // Lenses kept as fields: SwingTree bindings hold them strongly anyway, but a
    // field makes the lifetime obvious and guards against the weak-lens GC gotcha.
    private final Var<Tuple<Page>>    pages;
    private final Var<Integer>        desiredPage;
    private final Var<EditorMode>     mode;
    /**
     *  The {@link #mode} enum expressed as a tab index through a logic lens — this
     *  single {@code Var<Integer>} is bound to the {@code withSelectedIndex(..)} of
     *  <b>every</b> page's sub-tab strip at once, so switching the mode anywhere
     *  (sub-tab click, toolbar radio) switches it everywhere.
     */
    private final Var<Integer>        modeIndex;
    /**
     *  The {@link AlmanackViewModel#drawer()} enum expressed as the drawer pane's
     *  selection index, where {@link DrawerSection#NONE} becomes {@code -1} — the
     *  "nothing selected" index.
     */
    private final Var<Integer>        drawerIndex;
    private final Val<Boolean>        idle;
    private final Val<Tuple<String>>  log;

    /** View-only preference: where the page tabs sit — not part of the view model. */
    private final Var<UI.Side> tabSide = Var.of(UI.Side.TOP);

    /**
     *  How much horizontal room the window offers. Like {@link #tabSide} this is
     *  view-only state, so it lives here rather than in the view model. One
     *  {@code onResize} handler writes it; the views below turn it into layouts.
     */
    private final Var<Breakpoint> breakpoint = Var.of(Breakpoint.WIDE);

    // The reactive layouts. Every region keeps its children and merely rearranges
    // them — rebuilding would throw away the tab selections, the caret in the text
    // area and the scroll position of the log.
    private final Val<Layout>  headerLayout;
    private final Val<Layout>  toolbarLayout;
    private final Val<Layout>  bodyLayout;
    private final Val<Layout>  statusLayout;
    private final Val<Boolean> roomy;
    private final Val<String>  reopenText;
    /** One more bound tab property: a short strip scrolls rather than stacking rows. */
    private final Val<UI.OverflowPolicy> tabOverflow;

    public AlmanackView( Var<AlmanackViewModel> vm ) {
        this.vm = vm;

        pages       = vm.zoomTo(AlmanackViewModel::pages,       AlmanackViewModel::withPages);
        desiredPage = vm.zoomTo(AlmanackViewModel::desiredPage, AlmanackViewModel::withDesiredPage);
        mode        = vm.zoomTo(AlmanackViewModel::mode,        AlmanackViewModel::withMode);
        modeIndex   = vm.zoomTo( m -> m.mode().index(), (m, i) -> m.withMode(EditorMode.fromIndex(i)) );
        drawerIndex = vm.zoomTo( m -> m.drawer().index(), (m, i) -> m.withDrawer(DrawerSection.fromIndex(i)) );
        idle        = vm.viewAs(Boolean.class, m -> !m.restoring());
        log         = vm.zoomTo(AlmanackViewModel::log, AlmanackViewModel::withLog);

        headerLayout  = breakpoint.viewAs(Layout.class, b -> b == Breakpoint.NARROW ? HEADER_STACKED : HEADER_ROW);
        toolbarLayout = breakpoint.viewAs(Layout.class, b -> b.pick(TOOLBAR_ONE_ROW, TOOLBAR_TWO_ROWS, TOOLBAR_TWO_ROWS, TOOLBAR_STACKED));
        bodyLayout    = breakpoint.viewAs(Layout.class, b -> b.pick(BODY_BESIDE_330, BODY_BESIDE_280, BODY_BELOW, BODY_BELOW));
        statusLayout  = breakpoint.viewAs(Layout.class, b -> b == Breakpoint.NARROW ? STATUS_TWO_ROWS : STATUS_ONE_ROW);
        roomy         = breakpoint.viewAs(Boolean.class, b -> b == Breakpoint.WIDE);
        tabOverflow   = breakpoint.viewAs(UI.OverflowPolicy.class,
                            b -> b == Breakpoint.WIDE ? UI.OverflowPolicy.WRAP : UI.OverflowPolicy.SCROLL);
        reopenText    = breakpoint.viewAsString( b -> b == Breakpoint.WIDE
                            ? "Close notebook  &  re-open at the bookmark"
                            : "Close  &  re-open at ⚑" );

        // "wmin 0" on every row, or the widest row (the toolbar) becomes the
        // window's minimum width and the reflow can never be reached.
        of(this).withLayout("fill, wrap 1, ins 0, gap 0")
        .withPrefSize(1180, 780)
        .withStyle( it -> it.backgroundColor(PARCHMENT) )
        // The single input to every arrangement below.
        .onResize( it -> breakpoint.set(Breakpoint.of(it.getWidth())) )
        .add("growx, wmin 0", header())
        .add("growx, wmin 0", toolbar())
        // "hmin 0" for the same reason: stacked, the notebook and the drawer would
        // claim more height than a short window has and push the status bar off.
        .add("grow, push, wmin 0, hmin 0",
            panel(bodyLayout)
            .withStyle( it -> it.backgroundColor(PARCHMENT) )
            .add(pagePane())
            .add(drawerPane())
        )
        .add("growx, wmin 0", statusBar());
    }

    // ═════════════════════════════════════════════════════════════════════════════
    //  Convergence — the same components, rearranged by a Var<Layout>
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     *  Four amounts of horizontal room. No hysteresis needed (unlike a layout that
     *  swaps sub-views): reflowing a {@link Layout} never changes the width it was
     *  chosen from, so the switch cannot oscillate.
     */
    private enum Breakpoint {
        /** ≥ 1020px — the notebook, the drawer and a single toolbar line all fit. */
        WIDE,
        /** 700–1020px — the toolbar needs a second line and the drawer gives up room. */
        COMPACT,
        /** 520–700px — too narrow for two columns: the drawer moves below the notebook. */
        SNUG,
        /** &lt; 520px — a phone: one control group per toolbar line, header stacked. */
        NARROW;

        static Breakpoint of( int width ) {
            return width >= 1020 ? WIDE
                 : width >=  700 ? COMPACT
                 : width >=  520 ? SNUG
                 :                 NARROW;
        }

        <T> T pick( T wide, T compact, T snug, T narrow ) {
            switch ( this ) {
                case WIDE:    return wide;
                case COMPACT: return compact;
                case SNUG:    return snug;
                default:      return narrow;
            }
        }
    }

    /*
     *  ⚠ Every variant below spells out a constraint for *every* child. They are
     *  applied positionally and only overwritten where a new layout supplies one,
     *  so a gap would leave the previous variant's constraint (a stray "wrap", say)
     *  in place after switching back.
     */

    // ── header ── children: [0] title block, [1] bookmark, [2] close & re-open ──
    private static final Layout HEADER_ROW =
            Layout.mig("fill, ins 14 22 14 22, hidemode 3", "[grow][][]", "")
                  .withChildConstraints(
                      MigAddConstraint.of("growx, wmin 0"),
                      MigAddConstraint.of("aligny center"),
                      MigAddConstraint.of("aligny center"));
    /** The button is too wide to share a line, so it takes one of its own. */
    private static final Layout HEADER_STACKED =
            Layout.mig("fill, wrap 2, ins 12 16 10 16, hidemode 3", "[grow][]", "")
                  .withChildConstraints(
                      MigAddConstraint.of("growx, wmin 0"),
                      MigAddConstraint.of("aligny center"),
                      MigAddConstraint.of("growx, span 2, gaptop 8"));

    // ── toolbar ── children: [0] open, [1] "Mode:", [2..4] radios,
    //               [5] "Tabs:", [6] placement combo, [7] spacer,
    //               [8] "Drawer:", [9..11] drawer toggles ──
    private static final Layout TOOLBAR_ONE_ROW =
            Layout.mig("fillx, ins 8 18 8 18, gap 6")
                  .withChildConstraints(
                      MigAddConstraint.of(""), MigAddConstraint.of("gapleft 18"),
                      MigAddConstraint.of(""), MigAddConstraint.of(""), MigAddConstraint.of(""),
                      MigAddConstraint.of("gapleft 18"), MigAddConstraint.of(""),
                      MigAddConstraint.of("pushx"),
                      MigAddConstraint.of(""),
                      MigAddConstraint.of(""), MigAddConstraint.of(""), MigAddConstraint.of(""));
    /*
     *  "nogrid" on the wrapped variants: MigLayout is a grid, so otherwise every
     *  row's columns line up and the second row inherits the width of
     *  "＋ Open a page" as its first column. In no-grid mode rows simply flow.
     */
    /** Break after the mode radios: notebook controls above, view controls below. */
    private static final Layout TOOLBAR_TWO_ROWS =
            Layout.mig("fillx, nogrid, ins 8 18 8 18, gap 6")
                  .withChildConstraints(
                      MigAddConstraint.of(""), MigAddConstraint.of("gapleft 18"),
                      MigAddConstraint.of(""), MigAddConstraint.of(""), MigAddConstraint.of("wrap"),
                      MigAddConstraint.of(""), MigAddConstraint.of(""),
                      MigAddConstraint.of("pushx"),
                      MigAddConstraint.of(""),
                      MigAddConstraint.of(""), MigAddConstraint.of(""), MigAddConstraint.of(""));
    /** One group per line: open · mode · placement · drawer. */
    private static final Layout TOOLBAR_STACKED =
            Layout.mig("fillx, nogrid, ins 8 12 8 12, gap 5")
                  .withChildConstraints(
                      MigAddConstraint.of("growx, wrap"),
                      MigAddConstraint.of(""),
                      MigAddConstraint.of(""), MigAddConstraint.of(""), MigAddConstraint.of("wrap"),
                      MigAddConstraint.of(""), MigAddConstraint.of("growx, wrap"),
                      MigAddConstraint.of("w 0!"),
                      MigAddConstraint.of(""),
                      MigAddConstraint.of(""), MigAddConstraint.of(""), MigAddConstraint.of(""));

    // ── body ── children: [0] the notebook, [1] the drawer ──
    private static final Layout BODY_BESIDE_330 = besideLayout(330);
    private static final Layout BODY_BESIDE_280 = besideLayout(280);
    private static final Layout BODY_BELOW =
            Layout.mig("fill, wrap 1, ins 8 10 6 10, gap 8, hidemode 3")
                  .withChildConstraints(
                      MigAddConstraint.of("grow, push, wmin 0"),
                      // A range, not a fixed "300!": a short window may squeeze it.
                      MigAddConstraint.of("growx, wmin 0, height 140:300:300"));

    private static Layout besideLayout( int drawerWidth ) {
        return Layout.mig("fill, ins 10 12 6 12, gap 10, hidemode 3")
                     .withChildConstraints(
                         MigAddConstraint.of("grow, push, wmin 0"),
                         MigAddConstraint.of("growy, wmin 0, width " + drawerWidth + "!"));
    }

    // ── status bar ── children: [0..2] counters, [3] spacer, [4] summary ──
    private static final Layout STATUS_ONE_ROW =
            Layout.mig("fillx, ins 7 18 7 18, gap 18")
                  .withChildConstraints(
                      MigAddConstraint.of(""), MigAddConstraint.of(""), MigAddConstraint.of(""),
                      MigAddConstraint.of("pushx, growx"),
                      MigAddConstraint.of(""));
    private static final Layout STATUS_TWO_ROWS =
            Layout.mig("fillx, ins 6 12 6 12, gap 8")
                  .withChildConstraints(
                      MigAddConstraint.of(""), MigAddConstraint.of(""), MigAddConstraint.of("wrap"),
                      MigAddConstraint.of("w 0!"),
                      MigAddConstraint.of("growx, wmin 0"));

    // ═════════════════════════════════════════════════════════════════════════════
    //  Header — title, bookmark, and the deferred-selection showpiece
    // ═════════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> header() {
        return panel(headerLayout)
            .withStyle( it -> it.backgroundColor(MOSS) )
            .add(
                box("fill, wrap 1, ins 0, hidemode 3")
                .add("growx, wmin 0", label("The Almanack")
                    .withStyle( it -> it.componentFont( f -> f.family("Serif").size(23).weight(2f).color(CREAM) ) ))
                .add("growx, wmin 0",
                    label("a field notebook — every tab in this window is property-bound")
                    // The tag line is the first thing to go when the room runs out.
                    .isVisibleIf(roomy)
                    .withStyle( it -> it.componentFont( f -> f.family("Serif").size(12).posture(0.09f).color(new Color(197, 208, 189)) ) ))
            )
            .add(
                label(vm.viewAsString( m -> "⚑ bookmark: page " + (m.bookmark() + 1) ))
                .withStyle( it -> it
                    .componentFont( f -> f.family("SansSerif").size(12).color(new Color(226, 205, 145)) )
                    .padding(0, 12, 0, 0)
                )
            )
            .add(
                button(reopenText)
                .isEnabledIf(idle)
                .withTooltip("Empties the notebook, requests the bookmarked page while it does not exist, " +
                             "then streams the pages back in — watch the selection wait for its tab.")
                .withStyle( it -> it
                    .backgroundColor(RUST).foregroundColor(Color.WHITE)
                    .borderRadius(9).padding(7, 14, 7, 14)
                    .componentFont( f -> f.family("SansSerif").size(12).weight(2f).color(Color.WHITE) )
                )
                .onClick( it -> closeAndReopenAtBookmark() )
            );
    }

    // ═════════════════════════════════════════════════════════════════════════════
    //  Toolbar — mode radios (enum property), placement, drawer toggles (booleans)
    // ═════════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> toolbar() {
        return panel(toolbarLayout)
            .withStyle( it -> it.backgroundColor(CREAM).borderAt(Edge.BOTTOM, 1, HAIRLINE) )
            .add(
                button("＋ Open a page")
                .isEnabledIf(idle)
                .onClick( it -> vm.update( m -> {
                    AlmanackViewModel next = m.openNextPage();
                    Page opened = next.pages().get(next.pages().size() - 1);
                    return next.logged("Opened “" + opened.title() + "” (tab " + next.pages().size() + ")");
                }))
            )
            .add(toolbarLabel("Mode:"))
            // The same enum property the sub-tabs bind to — radios and tabs stay in sync.
            .add(radioButton(EditorMode.WRITE.label(),   EditorMode.WRITE,   mode))
            .add(radioButton(EditorMode.PREVIEW.label(), EditorMode.PREVIEW, mode))
            .add(radioButton(EditorMode.DETAILS.label(), EditorMode.DETAILS, mode))
            .add(toolbarLabel("Tabs:"))
            .add(comboBox(tabSide))
            .add(box())
            .add(toolbarLabel("Drawer:"))
            // Boolean twins of the drawer tabs' enum bindings: each toggle looks at
            // the same DrawerSection property through a little boolean lens, so a
            // pressed toggle, its drawer tab and the view model always agree —
            // and releasing the pressed one collapses the drawer entirely.
            .add(toggleButton("Navigator", drawerToggle(DrawerSection.NAVIGATOR)))
            .add(toggleButton("Inspector", drawerToggle(DrawerSection.INSPECTOR)))
            .add(toggleButton("Log",       drawerToggle(DrawerSection.LOG)));
    }

    /** A two-way boolean lens onto {@link AlmanackViewModel#drawer()} for one section. */
    private Var<Boolean> drawerToggle( DrawerSection section ) {
        return vm.zoomTo(
            m -> m.drawer() == section,
            (m, pressed) -> pressed
                ? m.withDrawer(section)
                : ( m.drawer() == section ? m.withDrawer(DrawerSection.NONE) : m )
        );
    }

    private static UIForAnySwing<?,?> toolbarLabel( String text ) {
        return label(text).withStyle( it -> it.componentFont( f -> f.size(11).color(FADED_INK) ) );
    }

    // ═════════════════════════════════════════════════════════════════════════════
    //  The notebook itself — tabs from a tuple, selection from a Var<Integer>
    // ═════════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> pagePane() {
        return tabbedPane(desiredPage)              // two-way selection index binding
            .withTabPlacementAt(tabSide)            // bound placement, switchable live
            .withOverflowPolicy(tabOverflow)        // bound policy: scroll the strip when narrow
            .onTabMouseClick( it ->
                vm.update( m -> m.logged("Mouse click on tab header #" + (it.tabIndex() + 1)) )
            )
            .addAll(pages, this::pageTab)           // the tabs are a function of the tuple
            .withStyle( it -> it
                .backgroundColor(CARD)
                .border(1, HAIRLINE).borderRadius(10)
            );
    }

    private Tab pageTab( Page page ) {
        UUID id = page.id();
        // The written text: a logic lens into the inks association. Typing changes
        // only this map entry, so no tab is ever rebuilt while writing.
        Var<String>  ink    = vm.zoomTo( m -> m.inkOf(id), (m, text) -> m.withInkOf(id, text) );
        Val<String>  title  = vm.viewAsString( m -> m.tabTitleOf(id) );
        Val<String>  tip    = vm.viewAsString( m -> m.wordCountOf(id) + " words of ink on this page" );
        Val<Boolean> hasInk = ink.viewAs(Boolean.class, text -> !text.trim().isEmpty());
        return tab(title)                           // live title: the ink-dot appears as you write
            .withTip(tip)                           // live tooltip: bound word count
            .onSelection( it ->
                vm.update( m -> m.logged("onSelection: “" + m.tabTitleOf(id) + "”") )
            )
            .withHeader(
                button("✕")
                .withTooltip("Close this page")
                .withStyle( it -> it.padding(0, 4, 0, 4).margin(0)
                                    .backgroundColor(UI.Color.TRANSPARENT)
                                    .componentFont( f -> f.size(10).color(FADED_INK) ) )
                .onClick( it -> vm.update( m -> m.closePage(id).logged("Closed “" + page.title() + "”") ) )
            )
            .add(
                // Every page's sub-strip shares ONE selection index property (the
                // mode enum seen through an index lens): switching any page's
                // sub-tab switches them all, and the toolbar radios follow along.
                tabbedPane(modeIndex)
                .add(
                    tab(EditorMode.WRITE.label())
                    .add(
                        scrollPane().withStyle( it -> it.borderWidth(0) )
                        .add(
                            textArea(ink)
                            // No SwingTree equivalent for these two yet.
                            .peek( area -> { area.setLineWrap(true); area.setWrapStyleWord(true); } )
                            .withStyle( it -> it
                                .padding(18, 22, 18, 22)
                                .backgroundColor(CARD)
                                .componentFont( f -> f.family("Serif").size(15).color(INK) )
                            )
                        )
                    )
                )
                .add(
                    tab(EditorMode.PREVIEW.label())
                    .add(
                        scrollPane().withStyle( it -> it.borderWidth(0) )
                        .add(
                            label(ink.viewAsString( text -> renderPreview(page, text) ))
                            .withVerticalAlignment(VerticalAlignment.TOP)
                            .withStyle( it -> it.padding(18, 22, 18, 22).backgroundColor(CARD) )
                        )
                    )
                )
                .add(
                    tab(EditorMode.DETAILS.label())
                    .isEnabledIf(hasInk)            // no ink, no details — a bound enabled flag
                    .add(
                        panel("wrap 1, ins 20, gapy 6").withStyle( it -> it.backgroundColor(CARD) )
                        .add(detailLine(ink.viewAsString( t -> "Words:       " + wordCount(t) )))
                        .add(detailLine(ink.viewAsString( t -> "Characters:  " + t.length() )))
                        .add(detailLine(ink.viewAsString( t -> "Lines:       " + (t.isEmpty() ? 0 : t.split("\n", -1).length) )))
                        .add(detailLine(Val.of("Page id:     " + id)))
                    )
                )
            );
    }

    private static UIForAnySwing<?,?> detailLine( Val<String> text ) {
        return label(text).withStyle( it -> it.componentFont( f -> f.family("Monospaced").size(13).color(INK) ) );
    }

    private static String renderPreview( Page page, String ink ) {
        StringBuilder html = new StringBuilder("<html><body style='width:340px;color:#363026;font-family:serif'>");
        html.append("<h2 style='margin-top:0'>").append(page.glyph()).append("  ").append(escape(page.title())).append("</h2>");
        if ( ink.trim().isEmpty() )
            html.append("<i style='color:#8a7f62'>This page holds no ink yet — switch to ✎ Write.</i>");
        else
            for ( String paragraph : ink.split("\n\\s*\n", -1) )
                html.append("<p>").append(escape(paragraph).replace("\n", "<br>")).append("</p>");
        return html.append("</body></html>").toString();
    }

    private static String escape( String text ) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static int wordCount( String text ) {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }

    // ═════════════════════════════════════════════════════════════════════════════
    //  The drawer — boolean-bound tabs plus a deferred *constant* selection index
    // ═════════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> drawerPane() {
        // The DrawerSection enum seen through an index lens IS the pane's selection
        // index, with NONE mapping to -1 — the index that deselects every tab. The
        // binding is established BEFORE any tab exists (initially LOG, index 2), so
        // it starts out deferred and is applied once the third tab below is added.
        return tabbedPane(drawerIndex)
            // Wrapped in a scroll pane: the drawer is only 300px tall once it moves
            // below the notebook, and short windows squeeze it either way.
            .add(tab("Navigator").add(drawerScroll(navigator())))
            .add(tab("Inspector").add(drawerScroll(inspector())))
            .add(tab("Log")      .add(eventLog()))
            // NONE means collapsed: hiding the pane lets the pages take the room.
            .isVisibleIf(vm.viewAs(Boolean.class, m -> m.drawer() != DrawerSection.NONE))
            .withStyle( it -> it
                .backgroundColor(MOSS_LIGHT)
                .border(1, HAIRLINE).borderRadius(10)
            );
    }

    private UIForAnySwing<?,?> navigator() {
        return panel("fillx, wrap 1, ins 16, gapy 7").withStyle( it -> it.backgroundColor(MOSS_LIGHT) )
            .add(drawerTitle("DESIRED PAGE INDEX"))
            .add("growx",
                panel("fillx, ins 0, gap 6", "[][grow][]").withStyle( it -> it.backgroundColor(MOSS_LIGHT) )
                .add(button("◀").onClick( it -> desiredPage.set(Math.max(-1, desiredPage.get() - 1)) ))
                // The very same Var<Integer> the tabbed pane is bound to — type any
                // number, even one no page has yet.
                .add("growx", numericTextField(desiredPage))
                // Deliberately unclamped: overshooting is how you defer the selection.
                .add(button("▶").onClick( it -> desiredPage.set(desiredPage.get() + 1) ))
            )
            .add("growx, gaptop 4",
                label(vm.viewAsString( m ->
                        "<html><body style='width:240px;color:#5c5545'>" + m.selectionSummary() + "</body></html>" ))
            )
            .add("growx, gaptop 10",
                button("Request page 8 — likely not there yet")
                .withTooltip("Sets the bound index to 7. If fewer pages exist, everything " +
                             "deselects until page 8 appears — then it selects itself.")
                .onClick( it -> {
                    desiredPage.set(7);
                    vm.update( m -> m.logged("Navigator requested page 8 (index 7)") );
                })
            )
            .add("growx",
                button("＋ Open pages until it exists")
                .isEnabledIf(idle)
                .onClick( it -> vm.update( m -> {
                    while ( m.desiredPage() >= m.pages().size() )
                        m = m.openNextPage();
                    return m.logged("Opened pages up to the requested index");
                }))
            )
            .add("growx, gaptop 12",
                label("<html><body style='width:240px;color:#8a7f62;font-style:italic'>" +
                      "The index property is the single source of truth: the notebook, " +
                      "this field and the arrows are all bound to it. An index pointing " +
                      "past the last page keeps nothing selected — no clamping, no reset — " +
                      "until a matching page appears.</body></html>")
            );
    }

    private UIForAnySwing<?,?> inspector() {
        // A logic lens onto the title of whichever page the desired index points at.
        // Renaming rewrites that page in the tuple, so its tab is rebuilt live while
        // you type — the selection must survive every keystroke.
        Var<String> currentTitle = vm.zoomTo(AlmanackViewModel::currentTitle, AlmanackViewModel::renameCurrent);
        return panel("fillx, wrap 1, ins 16, gapy 7").withStyle( it -> it.backgroundColor(MOSS_LIGHT) )
            .add(drawerTitle("TITLE OF THE OPEN PAGE"))
            .add("growx", textField(currentTitle).isEditableIf(vm.viewAs(Boolean.class, AlmanackViewModel::hasOpenPage)))
            .add("growx",
                label("<html><body style='width:240px;color:#8a7f62;font-style:italic'>" +
                      "Renaming replaces the page inside the bound tuple — watch its tab " +
                      "rebuild in place on every keystroke without losing the selection." +
                      "</body></html>")
            )
            .add("growx, gaptop 14", drawerTitle("BOOKMARK"))
            .add("growx",
                button(vm.viewAsString( m -> "⚑ Bookmark this page (page " + (m.desiredPage() + 1) + ")" ))
                .isEnabledIf(vm.viewAs(Boolean.class, AlmanackViewModel::hasOpenPage))
                .onClick( it -> vm.update( m ->
                    m.withBookmark(m.desiredPage())
                     .logged("Bookmark moved to page " + (m.desiredPage() + 1))
                ))
            )
            .add("growx",
                label("<html><body style='width:240px;color:#8a7f62;font-style:italic'>" +
                      "“Close &amp; re-open” up in the header requests this page while the " +
                      "notebook is still empty — the deferred index waits for its tab." +
                      "</body></html>")
            );
    }

    private UIForAnySwing<?,?> eventLog() {
        return panel("fill, wrap 1, ins 10").withStyle( it -> it.backgroundColor(MOSS_LIGHT) )
            .add(drawerTitle(" EVERY TAB EVENT, NEWEST FIRST"))
            .add("grow, push, gaptop 4",
                scrollPanels()
                .addAll(log, entry ->
                    label("<html><body style='width:230px'>" + escape(entry) + "</body></html>")
                    .withStyle( it -> it
                        .componentFont( f -> f.family("Monospaced").size(10).color(INK) )
                        .padding(2, 6, 2, 6)
                    )
                )
            );
    }

    /** Lets a drawer section grow taller than the drawer without being cut off. */
    private static UIForAnySwing<?,?> drawerScroll( UIForAnySwing<?,?> content ) {
        return scrollPane( conf -> conf.fitWidth(true) )
            .withHorizontalScrollBarPolicy(UI.Active.NEVER)
            .withScrollIncrement(20)
            .withStyle( it -> it.borderWidth(0).backgroundColor(MOSS_LIGHT) )
            .add(content);
    }

    private static UIForAnySwing<?,?> drawerTitle( String text ) {
        return label(text).withStyle( it -> it.componentFont( f -> f.size(10).weight(2f).spacing(0.08f).color(FADED_INK) ) );
    }

    // ═════════════════════════════════════════════════════════════════════════════
    //  Status bar — the model's view of the selection, in plain words
    // ═════════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> statusBar() {
        return panel(statusLayout)
            .withStyle( it -> it.backgroundColor(MOSS) )
            .add(statusLabel(vm.viewAsString( m -> m.pages().size() + " pages open" )))
            .add(statusLabel(vm.viewAsString( m -> "desired index: " + m.desiredPage() )))
            .add(statusLabel(vm.viewAsString( m -> "mode: " + m.mode().label() )))
            .add(box())
            .add(statusLabel(vm.viewAsString(AlmanackViewModel::selectionSummary)));
    }

    private static UIForAnySwing<?,?> statusLabel( Val<String> text ) {
        return label(text).withStyle( it -> it.componentFont( f -> f.family("SansSerif").size(11).color(new Color(206, 215, 199)) ) );
    }

    // ═════════════════════════════════════════════════════════════════════════════
    //  The deferred-selection showpiece, asynchronously
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     *  Empties the notebook, then — while it is still empty — points the bound
     *  selection index at the bookmarked page and streams the saved pages back in,
     *  one every 700ms. The bookmarked tab does not exist for most of that time:
     *  the pane simply keeps nothing selected until the page returns, at which
     *  point the deferred index is applied automatically.
     */
    private void closeAndReopenAtBookmark() {
        AlmanackViewModel snapshot = vm.get();
        if ( snapshot.restoring() || snapshot.pages().isEmpty() )
            return;
        Tuple<Page> saved  = snapshot.pages();
        int         target = Math.max(0, Math.min(snapshot.bookmark(), saved.size() - 1));
        vm.update( m -> m
            .withRestoring(true)
            .withPages(Tuple.of(Page.class))     // the tabs vanish; the pane deselects
            .logged("Notebook closed — re-opening at bookmarked page " + (target + 1) + " …")
        );
        Thread restorer = new Thread(() -> {
            sleep(500);
            // Request the bookmarked page while not a single page exists — this is
            // the deferred selection: the property holds the index, the pane waits.
            UI.runLater(() -> vm.update( m -> m
                .withDesiredPage(target)
                .logged("Bookmark requests page " + (target + 1) + " — it does not exist yet…")
            ));
            for ( int i = 0; i < saved.size(); i++ ) {
                Page page = saved.get(i);
                sleep(700);
                UI.runLater(() -> vm.update( m -> m
                    .withPages(m.pages().add(page))
                    .logged("Recovered “" + page.title() + "” with all its ink")
                ));
            }
            UI.runLater(() -> vm.update( m -> m.withRestoring(false).logged("Notebook fully restored.") ));
        }, "almanack-restorer");
        restorer.setDaemon(true);
        restorer.start();
    }

    private static void sleep( long millis ) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ═════════════════════════════════════════════════════════════════════════════

    public static void main( String[] args ) {
        FlatLightLaf.setup();
        Var<AlmanackViewModel> state = Var.of(AlmanackViewModel.initial());
        UI.show("The Almanack — a field notebook of tab bindings", frame -> new AlmanackView(state));
        EventProcessor.DECOUPLED.join();
    }
}
