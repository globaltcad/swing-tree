package examples.trains.mvi;
import java.util.Locale;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.threading.EventProcessor;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
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
            .add(GROW_X, header())
            .add(GROW_X, toolbar())
            .add(GROW_X, label(status).group(Skin.STATUS))
            .add(GROW.and(PUSH),
                splitPane(UI.Align.HORIZONTAL).withDivisionOf(0.52)
                .add(boardCard())
                .add(routeCard())
            )
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
            .add(LEFT,
                box(FILL.and(WRAP(1)).and(INS(0)))
                .add(label("🚆  Austrian Station Board").group(Skin.APP_TITLE))
                .add(label("Live ÖBB timetable · click any train to reveal its full route")
                        .group(Skin.APP_SUBTITLE))
            )
            .add(RIGHT,
                button(theme.viewAsString(t -> t.isDark() ? "☀  Light mode" : "☾  Dark mode"))
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

    private UIForAnySwing<?,?> toolbar() {
        return
            panel(FILL).group(Skin.TOOLBAR)
            .withLayout("fill, ins 0", "[][240px:300px,grow][][grow,fill][][]")
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
    //  Left — the station board
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?,?> boardCard() {
        return
            panel(FILL.and(WRAP(1)).and(INS(0))).group(Skin.CARD)
            .add(GROW_X,
                box(FILL).withLayout("fill, ins 16 18 8 18", "[grow][]")
                .add(LEFT, label(mode.viewAsString(BoardMode::label)).group(Skin.CARD_TITLE))
                .add(RIGHT, label(station.viewAsString(Station::name)).group(Skin.CARD_SUB))
            )
            .add(GROW_X,
                label(loadingBoard.viewAs(String.class, b -> b ? "Loading…" : ""))
                .group(Skin.CARD_SUB)
                .isVisibleIf(loadingBoard)
                .withStyle(it -> it.padding(0, 18, 6, 18))
            )
            .add(GROW.and(PUSH),
                scrollPanels().withStyle(it -> it.padding(0, 8, 8, 8))
                .addAll(board, (Var<Departure> entry) -> departureCard(entry))
            );
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
            // line badge + headsign
            .add(GROW,
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
                .add(LEFT,
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

    private UIForAnySwing<?,?> routeCard() {
        return
            panel(FILL.and(WRAP(1)).and(INS(0))).group(Skin.CARD)
            .add(GROW_X,
                box(FILL.and(WRAP(1))).withLayout("fill, wrap 1, ins 16 18 8 18")
                .add(GROW_X, label(route.viewAsString(r ->
                        r.isEmpty() ? "No train selected" : r.line())).group(Skin.CARD_TITLE))
                .add(GROW_X, label(route.viewAsString(r -> r.isEmpty()
                        ? "Click a train on the left to trace every stop on its journey."
                        : r.origin() + "   →   " + r.terminus() + "      ·   " + r.summary()))
                    .group(Skin.CARD_SUB))
            )
            .add(GROW_X,
                label(loadingRoute.viewAs(String.class, b -> b ? "Loading route…" : ""))
                .group(Skin.CARD_SUB).isVisibleIf(loadingRoute)
                .withStyle(it -> it.padding(0, 18, 6, 18))
            )
            .add(GROW.and(PUSH),
                scrollPanels().withStyle(it -> it.padding(0, 8, 8, 8))
                .addAll(routeStops, (Var<RouteStop> entry) -> stopRow(entry))
            );
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
            .add(GROW,
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