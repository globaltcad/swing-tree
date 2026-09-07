package examples.trains.mvi;
import java.util.Locale;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import sprouts.From;
import sprouts.HasId;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.UIForPanel;
import swingtree.UIForScrollPane;
import swingtree.UIForSplitPane;
import swingtree.api.Layout;
import swingtree.api.mvvm.BoundViewSupplier;
import swingtree.layout.MigAddConstraint;
import swingtree.threading.EventProcessor;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static swingtree.UI.*;

/**
 *  An Austrian railway station board built with SwingTree on the MVI / MVL
 *  pattern. Everything the view shows is a pure function of one immutable
 *  {@link TrainsViewModel} held in a single {@link Var}; the view only zooms
 *  lenses into it and binds widgets. User actions and network results both
 *  produce a brand-new view model.
 *
 *  <h2>What it does</h2>
 *  <ul>
 *    <li>Shows the live departures (or arrivals) board of a station —
 *        default <b>Aspang-Markt</b>, changeable via the search box.</li>
 *    <li>Clicking any train resolves and shows its <b>entire route</b>: every
 *        calling point with times and platforms, the current station
 *        highlighted — so you can plan a journey yourself instead of trusting a
 *        black-box router.</li>
 *    <li>Switches between a light and dark theme at runtime.</li>
 *    <li><b>Converges</b>: drag the window taller than it is wide and the whole
 *        GUI turns into a phone-shaped, top-to-bottom scrolling column.</li>
 *  </ul>
 *
 *  <h2>Convergence — one view, two form factors</h2>
 *  <p>The aspect ratio of the view is classified into a {@link Formfactor}
 *  (stored in the view model like any other state, written by a single
 *  {@code onResize} handler) and three things follow from it, each through a
 *  different SwingTree mechanism:</p>
 *  <ul>
 *    <li><b>The body</b> swaps sub-view via the property-bound
 *        {@code add(Val, ViewSupplier)}: {@link #wideBody()} is a split pane of
 *        two independently scrolling cards, {@link #tallBody()} one page-wide
 *        scroll holding a single column.</li>
 *    <li><b>The toolbar</b> reflows via a <i>reactive layout</i>
 *        ({@code panel(Val<Layout>)}): one long row becomes three short ones.
 *        Nothing is rebuilt, so the search field keeps its focus and caret.</li>
 *    <li><b>The header</b> drops its subtitle and shortens the theme button
 *        through ordinary property bindings.</li>
 *  </ul>
 *
 *  <p>Data comes from the public, key-less Transitous / MOTIS API via
 *  {@link TransitClient}. All network calls run on a background executor; their
 *  results are applied back on the EDT with {@link UI#runLater}.</p>
 */
public final class TrainsView extends JPanel {

    private final Var<TrainsViewModel> vm;
    private final TrainStyle sheet;

    private final ExecutorService io =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "transit-io");
                t.setDaemon(true);
                return t;
            });

    // Lenses kept as fields: those consumed only by raw onChange subscriptions
    // (theme, station, mode) must be strongly held or they get GC'd (see skill §9c).
    private final Var<Station>          station;
    private final Var<String>           query;
    private final Var<Tuple<Station>>   stationResults;
    private final Var<BoardMode>        mode;
    private final Var<Tuple<Departure>> board;
    private final Var<TrainRoute>       route;
    private final Var<Tuple<RouteStop>> routeStops;
    private final Var<String>           status;
    private final Var<Boolean>          loadingBoard;
    private final Var<Boolean>          loadingRoute;
    private final Var<Theme>            theme;
    private final Var<Formfactor>       formfactor;

    // Derived views of the form factor, kept as fields for the same reason as the
    // lenses above: a view is only weakly held by its source.
    private final Val<Boolean> isWide;
    private final Val<Layout>  toolbarLayout;
    private final Val<String>  themeButtonText;
    private final Val<String>  routeHint;

    public TrainsView(Var<TrainsViewModel> vm, TrainStyle sheet) {
        this.vm    = vm;
        this.sheet = sheet;

        station        = vm.zoomTo(TrainsViewModel::station,        TrainsViewModel::withStation);
        query          = vm.zoomTo(TrainsViewModel::query,          TrainsViewModel::withQuery);
        stationResults = vm.zoomTo(TrainsViewModel::stationResults, TrainsViewModel::withStationResults);
        mode           = vm.zoomTo(TrainsViewModel::mode,           TrainsViewModel::withMode);
        board          = vm.zoomTo(TrainsViewModel::board,          TrainsViewModel::withBoard);
        route          = vm.zoomTo(TrainsViewModel::route,          TrainsViewModel::withRoute);
        routeStops     = route.zoomTo(TrainRoute::stops,            TrainRoute::withStops);
        status         = vm.zoomTo(TrainsViewModel::status,         TrainsViewModel::withStatus);
        loadingBoard   = vm.zoomTo(TrainsViewModel::loadingBoard,   TrainsViewModel::withLoadingBoard);
        loadingRoute   = vm.zoomTo(TrainsViewModel::loadingRoute,   TrainsViewModel::withLoadingRoute);
        theme          = vm.zoomTo(TrainsViewModel::theme,          TrainsViewModel::withTheme);
        formfactor     = vm.zoomTo(TrainsViewModel::formfactor,     TrainsViewModel::withFormfactor);

        isWide          = formfactor.viewAs(Boolean.class, Formfactor::isWide);
        toolbarLayout   = formfactor.viewAs(Layout.class,  f -> f.isTall() ? TALL_TOOLBAR : WIDE_TOOLBAR);
        themeButtonText = Viewable.of(String.class, theme, formfactor, (t, f) ->
                                f.isTall() ? ( t.isDark() ? "☀" : "☾" )
                                           : ( t.isDark() ? "☀  Light mode" : "☾  Dark mode" ));
        routeHint       = Viewable.of(String.class, route, formfactor, (r, f) ->
                                !r.isEmpty()
                                    ? r.origin() + "   →   " + r.terminus() + "      ·   " + r.summary()
                                    : f.isTall() ? "Pick a train above to trace every stop on its journey."
                                                 : "Click a train on the left to trace every stop on its journey.");

        // React to user-driven changes (combo / toggle) by reloading.
        Viewable.cast(station).onChange(From.VIEW, it -> loadBoard());
        Viewable.cast(mode).onChange(From.VIEW, it -> loadBoard());
        // React to theme changes (from anywhere) by swapping the look-and-feel.
        Viewable.cast(theme).onChange(From.ALL, it -> applyTheme(it.currentValue().orElseThrowUnchecked()));

        // Build the whole tree inside the style-sheet scope so every component
        // binds to it and repaints when the theme is hot-swapped.
        UI.use(sheet, () ->
            of(this).group(Skin.FRAME).withLayout(FILL.and(WRAP(1)).and(INS(0)).and("gap 0"))
            .withPrefSize(1080, 740)
            // The single place where the window shape enters the model.
            .onResize(it -> formfactor.update(From.VIEW, f -> Formfactor.of(it.getWidth(), it.getHeight(), f)))
            .add(GROW_X, header())
            .add(GROW_X, toolbar())
            .add(GROW_X, label(status).group(Skin.STATUS))
            // The one part that genuinely differs between the two shapes, so it is
            // swapped wholesale — at most once per shape change.
            .add(GROW.and(PUSH), formfactor, this::body)
        );

        // Resolve the default station and load its board on startup.
        search();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Top chrome
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> header() {
        return
            panel(FILL).group(Skin.HEADER).withLayout("fill, ins 0", "[grow][]")
            // "wmin 0" lets the titles ellipsize instead of pushing the theme
            // button off screen.
            .add(LEFT.and("wmin 0"),
                box(FILL.and(WRAP(1)).and(INS(0)).and("hidemode 3"))
                .add(GROW_X.and("wmin 0"), label("🚆  Austrian Station Board").group(Skin.APP_TITLE))
                // In the tall shape the header has to earn every pixel of width,
                // so the tag line is the first thing to go.
                .add(GROW_X.and("wmin 0"),
                    label("Live ÖBB timetable · click any train to reveal its full route")
                        .group(Skin.APP_SUBTITLE)
                        .isVisibleIf(isWide))
            )
            .add(RIGHT,
                button(themeButtonText)
                .onClick(it -> theme.set(theme.get().toggled()))
                .withStyle(theme, (t, it) -> {
                    Theme.Palette p = t.palette();
                    return it
                        .backgroundColor(p.row)
                        .border(1, p.border)
                        .borderRadius(9)
                        .padding(7, 14, 7, 14)
                        .componentFont(f -> f.family("SansSerif").size(13).weight(2f).color(p.text))
                        .cursor(UI.Cursor.HAND);
                })
            );
    }

    /**
     *  Everything on one line — there is room for it.
     *  <pre>
     *    STATION [search…] (Search) [station ▾] SHOW [mode ▾] (Refresh)
     *  </pre>
     */
    private static final Layout WIDE_TOOLBAR =
            Layout.mig("fill, ins 0", "[][240px:300px,grow][][grow,fill][][]", "")
                  .withChildConstraints(
                      MigAddConstraint.of(""),       // "STATION"
                      MigAddConstraint.of("growx"),  // search field
                      MigAddConstraint.of(""),       // Search button
                      MigAddConstraint.of("growx"),  // station picker
                      MigAddConstraint.of(""),       // "SHOW"
                      MigAddConstraint.of(""),       // mode picker
                      MigAddConstraint.of("")        // Refresh button
                  );

    /**
     *  The same seven widgets folded into three short rows.
     *  <pre>
     *    STATION [search…] (Search)
     *    [ station ▾                ]
     *    SHOW    [mode ▾]  (Refresh)
     *  </pre>
     */
    private static final Layout TALL_TOOLBAR =
            Layout.mig("fill, wrap 3, ins 0", "[][grow][]", "")
                  .withChildConstraints(
                      MigAddConstraint.of(""),
                      MigAddConstraint.of("growx"),
                      MigAddConstraint.of(""),
                      MigAddConstraint.of("growx, span 3"),
                      MigAddConstraint.of(""),
                      MigAddConstraint.of("growx"),
                      MigAddConstraint.of("")
                  );

    /**
     *  A <i>reactive</i> toolbar: the panel is bound to a {@link Layout} property
     *  derived from the form factor, so switching shapes reflows the very same
     *  seven widgets instead of rebuilding them. That matters here — a rebuilt
     *  text field would lose the user's focus and caret mid-typing.
     */
    private UIForAnySwing<?,?> toolbar() {
        return
            panel(toolbarLayout).group(Skin.TOOLBAR)
            .add(label("STATION").group(Skin.TOOL_LABEL))
            .add(GROW_X,
                textField(query)
                .onEnter(it -> search())
            )
            .add(button("Search").group(Skin.ACCENT_BUTTON).onClick(it -> search()))
            .add(GROW_X, comboBox(station, stationResults, Station::name))
            .add(label("SHOW").group(Skin.TOOL_LABEL))
            .add(comboBox(mode, BoardMode::label))
            .add(button("↻  Refresh").group(Skin.ACCENT_BUTTON).onClick(it -> loadBoard()));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  The body — the same two cards, arranged for the shape of the window
    // ════════════════════════════════════════════════════════════════════════

    /**
     *  Builds the body for one of the two shapes.
     *  <p>
     *  The {@code UI.use(..)} matters: this method runs <i>later</i>, whenever the
     *  shape flips, and a style sheet only binds components built inside its scope.
     *  It hands back the finished component rather than the builder, so we wrap it
     *  again to keep the declaration flowing.
     */
    private UIForAnySwing<?,?> body( Formfactor f ) {
        return UI.of(UI.use(sheet, () -> f.isTall()
                ? tallBody().get(JScrollPane.class)
                : wideBody().get(JSplitPane.class)));
    }

    /** Landscape: board and route side by side, each scrolling on its own. */
    private UIForSplitPane<JSplitPane> wideBody() {
        return
            splitPane(UI.Axis.HORIZONTAL).withDivisionOf(0.52)
            .add(boardCard(true))
            .add(routeCard(true));
    }

    /**
     *  Portrait: one column, scrolled top to bottom like a phone screen. The cards
     *  lose their own scroll bars and grow to their natural height instead — a
     *  scroll pane inside a scroll pane would swallow the wheel.
     */
    private UIForScrollPane<JScrollPane> tallBody() {
        return
            scrollPane(conf -> conf.fitWidth(true))
            .group(Skin.PAGE_SCROLL)
            .withHorizontalScrollBarPolicy(UI.Active.NEVER)
            .withScrollIncrement(24)
            .add(
                box(FILL_X.and(WRAP(1)).and(INS(0)))
                .add(GROW_X, boardCard(false))
                .add(GROW_X,
                    routeCard(false)
                    // In one long column the route sits below the entire board, so
                    // a freshly loaded route would be off screen. Bring it up.
                    .onView(Viewable.cast(route), it -> {
                        if ( !route.get().isEmpty() )
                            it.get().scrollRectToVisible(
                                new Rectangle(0, 0, it.get().getWidth(), it.get().getHeight()));
                    })
                )
            );
    }

    /**
     *  The rows of one of the two lists: in their own scroll pane (landscape) or as
     *  a plain stack carried by the page scroll (portrait). Both bind the same
     *  tuple property — only who does the scrolling differs.
     */
    private static <M extends HasId<?>> UIForAnySwing<?,?> rows(
        boolean ownScroll, Var<Tuple<M>> models, BoundViewSupplier<M> rowView
    ) {
        if ( ownScroll )
            return scrollPanels().withStyle(it -> it.padding(0, 8, 8, 8)).addAll(models, rowView);
        else
            return box(FILL_X.and(WRAP(1)).and(INS(0, 8, 8, 8))).addAll(GROW_X, models, rowView);
    }

    /** How the list inside a card claims space: all of it, or just its own height. */
    private static MigAddConstraint listConstraint( boolean ownScroll ) {
        return ownScroll ? GROW.and(PUSH) : GROW_X;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Left — the station board
    // ════════════════════════════════════════════════════════════════════════

    private UIForPanel<JPanel> boardCard( boolean ownScroll ) {
        return
            panel(FILL.and(WRAP(1)).and(INS(0))).group(Skin.CARD)
            .add(GROW_X,
                box(FILL).withLayout("fill, ins 16 18 8 18, hidemode 3", "[grow][]")
                .add(LEFT.and("wmin 0"), label(mode.viewAsString(BoardMode::label)).group(Skin.CARD_TITLE))
                // Already in the toolbar and the status line, so in the tall shape
                // it steps aside.
                .add(RIGHT.and("wmin 0"),
                    label(station.viewAsString(Station::name)).group(Skin.CARD_SUB).isVisibleIf(isWide))
            )
            .add(GROW_X,
                label(loadingBoard.viewAs(String.class, b -> b ? "Loading…" : ""))
                .group(Skin.CARD_SUB)
                .isVisibleIf(loadingBoard)
                .withStyle(it -> it.padding(0, 18, 6, 18))
            )
            .add(listConstraint(ownScroll), rows(ownScroll, board, this::departureCard));
    }

    private UIForAnySwing<?,?> departureCard(Var<Departure> entry) {
        Departure d = entry.get();
        String trackText = d.track() == null ? "" : "Pl. " + d.track();
        String noteText  = d.cancelled() ? "cancelled"
                         : d.isDelayed() ? "+" + d.delayMinutes() + " min" : "on time";

        return
            panel(FILL).withLayout("fill, ins 10 12 10 12", "[shrink]14[grow]12[shrink]")
            // time
            .add(GROW_Y,
                label(d.clock()).withStyle(theme, (t, it) -> it
                    .componentFont(f -> f.family("SansSerif").size(20).weight(2f)
                    .color(t.palette().text))
                )
            )
            // line badge + headsign ("wmin 0" so long headsigns ellipsize instead
            // of stretching the row past the window)
            .add(GROW.and("wmin 0"),
                box(FILL.and(WRAP(1)).and(INS(0)))
                .add(LEFT,
                    label(" " + d.line() + " ").withStyle(theme, (t, it) -> {
                        Theme.Palette p = t.palette();
                        return it
                            .backgroundColor(p.modeColor(d.mode()))
                            .foregroundColor(java.awt.Color.WHITE)
                            .borderRadius(6)
                            .padding(2, 8, 2, 8)
                            .componentFont(f -> f.family("SansSerif").size(12).weight(2f)
                                .color(java.awt.Color.WHITE));
                    })
                )
                .add(GROW_X.and("wmin 0"),
                    label((mode.get().isArrivals() ? "from  " : "→  ") + d.headsign())
                    .withStyle(theme, (t, it) -> it.padding(4, 1, 0, 0)
                        .componentFont(f -> f.family("SansSerif").size(14)
                            .color(t.palette().text)))
                )
            )
            // platform + punctuality
            .add(GROW_Y,
                box(FILL.and(WRAP(1)).and(INS(0)))
                .add(RIGHT, label(trackText).withStyle(theme, (t, it) -> it
                    .componentFont(f -> f.family("SansSerif").size(13).weight(2f)
                        .color(t.palette().subtext))))
                .add(RIGHT, label(noteText).withStyle(theme, (t, it) -> {
                    Theme.Palette p = t.palette();
                    java.awt.Color c = d.cancelled() || d.isDelayed() ? p.accent : p.onTime;
                    return it.componentFont(f -> f.family("SansSerif").size(11).color(c));
                }))
            )
            // The row look depends on both the theme and the selection, so we
            // simply bind the whole view model and read both from its item:
            .withStyle(vm, (m, it) -> {
                Theme.Palette p = m.theme().palette();
                boolean sel = m.selectedTripId().equals(d.id());
                return it
                    .backgroundColor(sel ? p.currentStop : p.row)
                    .borderRadius(10)
                    .border(1, sel ? p.accent : p.border)
                    .borderAt(UI.Edge.LEFT, 4, p.modeColor(d.mode()))
                    .margin(4, 2, 4, 2);
            })
            .peek(comp -> makeClickable(comp, () -> loadRoute(d.id())));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Right — the full route of the selected train
    // ════════════════════════════════════════════════════════════════════════

    private UIForPanel<JPanel> routeCard( boolean ownScroll ) {
        return
            panel(FILL.and(WRAP(1)).and(INS(0))).group(Skin.CARD)
            .add(GROW_X,
                box(FILL.and(WRAP(1))).withLayout("fill, wrap 1, ins 16 18 8 18")
                .add(GROW_X.and("wmin 0"), label(route.viewAsString(r ->
                        r.isEmpty() ? "No train selected" : r.line())).group(Skin.CARD_TITLE))
                .add(GROW_X.and("wmin 0"), label(routeHint)
                    .group(Skin.CARD_SUB))
            )
            .add(GROW_X,
                label(loadingRoute.viewAs(String.class, b -> b ? "Loading route…" : ""))
                .group(Skin.CARD_SUB).isVisibleIf(loadingRoute)
                .withStyle(it -> it.padding(0, 18, 6, 18))
            )
            .add(listConstraint(ownScroll), rows(ownScroll, routeStops, this::stopRow));
    }

    private UIForAnySwing<?,?> stopRow(Var<RouteStop> entry) {
        RouteStop s = entry.get();
        String arr = s.arrivalClock();
        String dep = s.departureClock();
        String timeText = !arr.isEmpty() && !dep.isEmpty() && !arr.equals(dep) ? arr + " – " + dep
                        : !dep.isEmpty() ? dep
                        : arr;
        String trackText = s.track() == null ? "" : "Pl. " + s.track();

        return
            panel(FILL).withLayout("fill, ins 7 14 7 12", "[shrink]18[grow]10[shrink]")
            .add(GROW_Y, label(timeText).withStyle(theme, (t, it) -> it
                .componentFont(f -> f.family("Monospaced").size(13)
                    .weight(s.current() ? 2f : 1f)
                    .color(t.palette().text))))
            .add(GROW.and("wmin 0"),
                label((s.current() ? "● " : "") + s.name())
                .withStyle(theme, (t, it) -> {
                    Theme.Palette p = t.palette();
                    return it.componentFont(f -> f.family("SansSerif").size(14)
                        .weight(s.current() ? 2f : 1f)
                        .color(s.current() ? p.accent : p.text)
                );
            }))
            .add(GROW_Y, label(trackText).withStyle(theme, (t, it) -> it
                .componentFont(f -> f.family("SansSerif").size(12)
                    .color(t.palette().subtext))))
            .withStyle(theme, (t, it) -> {
                Theme.Palette p = t.palette();
                return it
                    .backgroundColor(s.current() ? p.currentStop : p.card)
                    .borderAt(UI.Edge.LEFT, 3, s.current() ? p.accent : p.border)
                    .borderAt(UI.Edge.BOTTOM, 1, p.border)
                    .margin(0);
            });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Actions — all network IO is dispatched off the EDT
    // ════════════════════════════════════════════════════════════════════════

    private void search() {
        String q = vm.get().query();
        vm.update(v -> v.withStatus("Searching for \"" + q + "\"…"));
        io.execute(() -> {
            try {
                List<Station> found = TransitClient.searchStations(q);
                UI.runLater(() -> {
                    if (found.isEmpty()) {
                        vm.update(v -> v.withStatus("No station found matching \"" + q + "\""));
                        return;
                    }
                    Station first = found.get(0);
                    vm.update(v -> v.withStationResults(Tuple.of(Station.class, found)).withStation(first));
                    loadBoard();
                });
            } catch (Exception e) {
                UI.runLater(() -> vm.update(v -> v.withStatus("Search failed: " + message(e))));
            }
        });
    }

    private void loadBoard() {
        TrainsViewModel current = vm.get();
        Station st = current.station();
        if (!st.isReal()) return;
        boolean arrivals = current.mode().isArrivals();
        vm.set(current
            .withLoadingBoard(true)
            .withBoard(Tuple.of(Departure.class))
            .withRoute(TrainRoute.empty())
            .withSelectedTripId("")
            .withStatus("Loading " + current.mode().label().toLowerCase(Locale.ROOT) + " for " + st.name() + "…"));
        io.execute(() -> {
            try {
                List<Departure> deps = TransitClient.board(st.id(), arrivals);
                UI.runLater(() -> vm.update(v -> v
                    .withBoard(Tuple.of(Departure.class, deps))
                    .withLoadingBoard(false)
                    .withStatus(deps.isEmpty()
                        ? "No trains found for " + st.name()
                        : deps.size() + " " + v.mode().label().toLowerCase(Locale.ROOT) + "  ·  " + st.name())));
            } catch (Exception e) {
                UI.runLater(() -> vm.update(v -> v.withLoadingBoard(false)
                    .withStatus("Could not load board: " + message(e))));
            }
        });
    }

    private void loadRoute(String tripId) {
        // Clicking the train that is already shown (or already loading) is a
        // no-op: it would re-fetch the identical route, and re-rendering the
        // same stop ids would make scrollPanels reuse every row (a harmless but
        // noisy "already tied to another parent" log). Different trains have
        // train-scoped stop ids, so they never reuse across each other.
        if (tripId.equals(vm.get().selectedTripId())) return;
        String stationName = vm.get().station().name();
        vm.update(v -> v.withSelectedTripId(tripId).withLoadingRoute(true));
        io.execute(() -> {
            try {
                TrainRoute r = TransitClient.trip(tripId, stationName);
                UI.runLater(() -> vm.update(v -> v.withRoute(r).withLoadingRoute(false)));
            } catch (Exception e) {
                UI.runLater(() -> vm.update(v -> v.withRoute(TrainRoute.empty()).withLoadingRoute(false)
                    .withSelectedTripId("")               // allow a retry after a failure
                    .withStatus("Could not load route: " + message(e))));
            }
        });
    }

    private void applyTheme(Theme t) {
        UI.run(() -> {
            if (t.isDark()) FlatDarkLaf.setup(); else FlatLightLaf.setup();
            sheet.setTheme(t);
            FlatLaf.updateUI();
        });
    }

    private static String message(Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    /** Make a whole composite card react to clicks, not just its leaf labels. */
    private static void makeClickable(Component c, Runnable action) {
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.run(); }
        });
        if (c instanceof Container)
            for (Component child : ((Container) c).getComponents())
                makeClickable(child, action);
    }

    // ════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        FlatLightLaf.setup();
        Var<TrainsViewModel> vm = Var.of(TrainsViewModel.initial());
        TrainStyle sheet = new TrainStyle();
        UI.show(f -> {
            f.setTitle("Austrian Station Board — SwingTree");
            return new TrainsView(vm, sheet);
        });
        EventProcessor.DECOUPLED.join();
    }
}