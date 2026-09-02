package examples.laf.app;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.Tuple;
import swingtree.UI;
import swingtree.api.model.TableData;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 *  The single immutable root of the whole atelier application (MVI / MVL).
 *  <p>
 *  {@link AtelierView} holds no state of its own: it zooms lenses into the
 *  fields below and renders a pure function of them. Every gesture — a
 *  keystroke in the client field, a cell edited in the order book, a loom
 *  finishing a run — produces a <em>new</em> {@code AtelierViewModel} through a
 *  wither or one of the business methods here, and the lenses fire only for the
 *  slices that actually changed. The class imports no Swing type beyond
 *  {@link TableData}, which is a value describing a table rather than a
 *  component, so the whole workshop is testable without a display.
 *
 *  <h2>Two projections worth pointing at</h2>
 *  <ul>
 *    <li>{@link #orderTable()} / {@link #withOrderTable(TableData)} is a lens
 *        onto something this model <em>computes</em> rather than stores: the
 *        filtered order book, rendered as a table value. The getter re-derives
 *        whenever the orders, the search text or the material filter change, and
 *        the wither folds edited cells back into the matching commissions by
 *        docket number — so one lens reacts to four inputs with no listeners at
 *        all.</li>
 *    <li>{@link #selected()} / {@link #withSelected(Order)} does the same for a
 *        single commission, which is what lets the whole editor form be built
 *        out of ordinary nested lenses.</li>
 *  </ul>
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class AtelierViewModel
{
    /** Column headings of the order book. Cells are addressed by meaning, never
     *  by a bare index, because a column may move. */
    public static final String COL_REF    = "Docket";
    public static final String COL_CLIENT = "Client";
    public static final String COL_FIBRE  = "Fibre";
    public static final String COL_METRES = "Metres";
    public static final String COL_DUE    = "Due";
    public static final String COL_STAGE  = "Stage";

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final int JOURNAL_LINES = 120;

    private final Tuple<Order>  orders;
    private final Tuple<Loom>   looms;
    private final Tuple<Yarn>   store;
    private final String        selectedRef;    // docket number of the edited commission
    private final String        search;
    private final String        materialFilter; // "" | an origin label | a fibre label
    private final boolean       rushOnly;
    private final int           editorTab;      // 0 Commission · 1 Cloth · 2 Docket
    private final boolean       journalOpen;
    private final String        journal;
    private final String        status;
    private final String        millAccount;
    private final String        millSecret;
    private final boolean       millConnected;
    private final boolean       loomsRunning;

    /** The workshop as it stands when the doors open in the morning. */
    public static AtelierViewModel initial() {
        Tuple<Order> orders = Tuple.of(
            new Order("2211", "Halden & Co",       Fibre.LINEN,    Weave.TWILL,       Finish.HEMMED,  24, 18,  90, "12 Sep", Stage.FINISHING, false, "Two bolts, selvedge left plain.\nThey collect in person."),
            new Order("2212", "Voss Papeterie",    Fibre.HEMP,     Weave.CANVAS,      Finish.RAW,     40, 12, 140, "19 Sep", Stage.WEAVING,   false, "Book cloth. Stiff hand wanted — do not full it."),
            new Order("2213", "Sundby Skole",      Fibre.COTTON,   Weave.PLAIN,       Finish.HEMMED,  60, 16, 100, "26 Sep", Stage.WARPING,   false, "Sixty aprons. One weft colour throughout."),
            new Order("2214", "Ateljé Rosenlund",  Fibre.SILK,     Weave.SATIN,       Finish.FRINGED, 12, 32,  70, "05 Sep", Stage.WEAVING,   true,  "Bridal. Handle with gloves.\nFringe knotted in pairs."),
            new Order("2215", "Fjeldberg Hytter",  Fibre.WOOL,     Weave.HERRINGBONE, Finish.RAW,     35, 10, 150, "03 Oct", Stage.DRAFTED,   false, "Blanket weight. Full it hard after cutting."),
            new Order("2216", "Nord Interiør",     Fibre.RAMIE,    Weave.BOUCLE,      Finish.HEMMED,  18, 14, 120, "10 Oct", Stage.DRAFTED,   false, "Upholstery trial. One metre sample first."),
            new Order("2217", "Kvist Skrædderi",   Fibre.CASHMERE, Weave.TWILL,       Finish.HEMMED,   8, 28,  80, "28 Aug", Stage.SHIPPED,   false, "Jacketing. Shipped last Thursday.")
        );
        Tuple<Loom> looms = Tuple.of(
            Loom.named("Nordre", "2212").withProgress(62),
            Loom.named("Søndre", "2214").withProgress(18),
            Loom.named("Vesle",  "")
        );
        Tuple<Yarn> store = Tuple.of(
            new Yarn(Fibre.LINEN,    180),
            new Yarn(Fibre.HEMP,      95),
            new Yarn(Fibre.COTTON,   240),
            new Yarn(Fibre.RAMIE,     32),
            new Yarn(Fibre.WOOL,     120),
            new Yarn(Fibre.SILK,      26),
            new Yarn(Fibre.CASHMERE,  14),
            new Yarn(Fibre.MOHAIR,    48)
        );
        return new AtelierViewModel(
            orders, looms, store,
            "2214", "", "", false,
            0, false, "",
            "Seven commissions on the books. Two looms threaded.",
            "atelier@flaxen", "", false, false
        );
    }

    // ── The order book, as a table value ──────────────────────────────────

    /** @return the commissions the filters let through, in docket order. */
    public Tuple<Order> visibleOrders() {
        String needle = search.trim().toLowerCase(Locale.ROOT);
        Tuple<Order> visible = orders;
        if ( !needle.isEmpty() )
            visible = visible.retainIf( order -> order.matches(needle) );
        if ( rushOnly )
            visible = visible.retainIf(Order::rush);
        if ( !materialFilter.isEmpty() )
            visible = visible.retainIf(this::passesMaterialFilter);
        return visible;
    }

    private boolean passesMaterialFilter( Order order ) {
        for ( Fibre.Origin origin : Fibre.Origin.values() )
            if ( origin.label().equals(materialFilter) )
                return order.fibre().origin() == origin;
        return order.fibre().label().equals(materialFilter);
    }

    /**
     *  The filtered order book rendered as the immutable value a
     *  {@code UI.table(..)} binds to. It is recomputed rather than stored, so it
     *  can never disagree with {@link #orders()}.
     *  <p>
     *  Every column here is one the commission <em>stores</em>. A derived column
     *  — what the commission is worth, say — would go stale the moment somebody
     *  edited the metres beside it, because SwingTree does not push a table
     *  change back into the table that raised it. So the money is shown where it
     *  is not also being edited: on the Cloth tab and in the status line.
     *
     *  @return the visible commissions as an editable, row-major table
     */
    public TableData orderTable() {
        TableData table = TableData.of(UI.CellOrder.ROW_MAJOR,
                COL_REF, COL_CLIENT, COL_FIBRE, COL_METRES, COL_DUE, COL_STAGE);
        for ( Order order : visibleOrders() )
            table = table.addRow(
                order.ref(), order.client(), order.fibre().label(),
                order.metres(), order.due(), order.stage().label()
            );
        return table
                .setColumnClassAt(table.indexOfColumn(COL_METRES), Integer.class)
                .asEditable();
    }

    /**
     *  Folds cells edited in the order book back into the commissions they came
     *  from, matched by docket number so that filtering and sorting cannot
     *  misdirect an edit. The model is the authority on what an edit means: a
     *  fibre or a stage that names nothing the workshop knows is simply not
     *  taken, and the computed Value column is never read back at all.
     *
     *  @param edited the table as the user left it
     *  @return a view model carrying the accepted edits
     */
    public AtelierViewModel withOrderTable( TableData edited ) {
        int refCol    = edited.indexOfColumn(COL_REF);
        int clientCol = edited.indexOfColumn(COL_CLIENT);
        int fibreCol  = edited.indexOfColumn(COL_FIBRE);
        int metresCol = edited.indexOfColumn(COL_METRES);
        int dueCol    = edited.indexOfColumn(COL_DUE);
        int stageCol  = edited.indexOfColumn(COL_STAGE);

        // Rows are matched by position rather than by the docket number in them,
        // because the docket number is itself editable — matching on it would
        // make renumbering a commission the one edit that silently did nothing.
        Tuple<Order> visible   = visibleOrders();
        Tuple<Order> merged    = orders;
        Tuple<Loom>  floor     = looms;
        String       selection = selectedRef;

        int rows = Math.min(edited.getRowCount(), visible.size());
        for ( int row = 0; row < rows; row++ ) {
            Order previous = visible.get(row);
            int   index    = indexOfRef(merged, previous.ref());
            if ( index < 0 )
                continue;
            Order updated = previous
                    .withRef(renumbered(merged, previous, text(edited.getValueAt(row, refCol))))
                    .withClient(text(edited.getValueAt(row, clientCol)))
                    .withFibre(Fibre.byLabel(text(edited.getValueAt(row, fibreCol)), previous.fibre()))
                    .withMetres(number(edited.getValueAt(row, metresCol), previous.metres()))
                    .withDue(text(edited.getValueAt(row, dueCol)))
                    .withStage(Stage.byLabel(text(edited.getValueAt(row, stageCol)), previous.stage()));
            if ( updated.equals(previous) )
                continue;
            merged = merged.setAt(index, updated);
            if ( !updated.ref().equals(previous.ref()) ) {
                // A renumbered commission takes its looms and the selection with it.
                final String was = previous.ref(), now = updated.ref();
                floor = floor.map( loom -> loom.orderRef().equals(was) ? loom.withOrderRef(now) : loom );
                if ( selection.equals(was) )
                    selection = now;
            }
        }
        if ( merged.equals(orders) )
            return this;
        return withOrders(merged).withLooms(floor).withSelectedRef(selection);
    }

    /**
     *  Accepts a new docket number for a commission, unless it is blank or one
     *  another commission already carries — a book with two #2214s in it would
     *  make every lookup ambiguous.
     *
     *  @param book    the commissions as they currently stand
     *  @param subject the commission being renumbered
     *  @param wanted  the number that was typed
     *  @return the accepted number, or the one the commission already had
     */
    private static String renumbered( Tuple<Order> book, Order subject, String wanted ) {
        if ( wanted.isEmpty() || wanted.equals(subject.ref()) )
            return subject.ref();
        for ( Order other : book )
            if ( other.ref().equals(wanted) )
                return subject.ref();
        return wanted;
    }

    // ── The one commission the editor is pointed at ───────────────────────

    /** @return the commission being edited, or {@link Order#none()} when none is. */
    public Order selected() {
        int index = indexOfRef(orders, selectedRef);
        return index < 0 ? Order.none() : orders.get(index);
    }

    /**
     *  Writes an edited commission back into the book. This is the wither half
     *  of the editor's lens, so every field the form binds — client, fibre,
     *  sett, notes — arrives here as a whole new {@link Order}.
     *
     *  @param edited the commission as the form left it
     *  @return a view model carrying the edit, or {@code this} if nothing changed
     */
    public AtelierViewModel withSelected( Order edited ) {
        int index = indexOfRef(orders, edited.ref());
        if ( index < 0 || orders.get(index).equals(edited) )
            return this;
        return withOrders(orders.setAt(index, edited));
    }

    public boolean hasSelection() { return selected().exists(); }

    public AtelierViewModel select( String ref ) {
        return ref.equals(selectedRef) ? this : withSelectedRef(ref);
    }

    // ── Commands ──────────────────────────────────────────────────────────

    /** Opens a fresh docket, numbered one past the highest in the book. */
    public AtelierViewModel addOrder() {
        int highest = 0;
        for ( Order order : orders )
            highest = Math.max(highest, number(order.ref(), 0));
        Order fresh = new Order(String.valueOf(highest + 1), "New client",
                                Fibre.LINEN, Weave.PLAIN, Finish.RAW,
                                10, 16, 100, "30 Sep", Stage.DRAFTED, false, "");
        return withOrders(orders.add(fresh))
                .withSelectedRef(fresh.ref())
                .note("Opened docket #" + fresh.ref() + ".");
    }

    /** Copies the selected commission onto a new docket — the usual way a
     *  repeat order gets entered. */
    public AtelierViewModel duplicateSelected() {
        Order source = selected();
        if ( !source.exists() )
            return withStatus("Nothing selected to copy.");
        AtelierViewModel opened = addOrder();
        Order fresh = opened.selected()
                .withClient(source.client())
                .withFibre(source.fibre()).withWeave(source.weave()).withFinish(source.finish())
                .withMetres(source.metres()).withSett(source.sett()).withWidthCm(source.widthCm())
                .withNotes(source.notes());
        return opened.withSelected(fresh)
                     .note("Copied #" + source.ref() + " onto docket #" + fresh.ref() + ".");
    }

    /** Strikes the selected commission out of the book and lands the selection
     *  on whatever takes its place. */
    public AtelierViewModel deleteSelected() {
        int index = indexOfRef(orders, selectedRef);
        if ( index < 0 )
            return withStatus("Nothing selected to strike out.");
        Order struck = orders.get(index);
        Tuple<Order> remaining = orders.removeAt(index);
        String nextRef = remaining.isEmpty() ? ""
                       : remaining.get(Math.min(index, remaining.size() - 1)).ref();
        return withOrders(remaining)
                .withSelectedRef(nextRef)
                .withLooms(looms.map( loom -> loom.orderRef().equals(struck.ref())
                                             ? loom.withOrderRef("").withRunning(false).withProgress(0)
                                             : loom ))
                .note("Struck #" + struck.ref() + " (" + struck.client() + ") out of the book.");
    }

    /** Moves the selected commission on to the next station, drawing the yarn
     *  out of the store room once it ships. */
    public AtelierViewModel advanceSelected() {
        Order order = selected();
        if ( !order.exists() )
            return withStatus("Nothing selected to advance.");
        if ( order.stage() == Stage.SHIPPED )
            return withStatus("#" + order.ref() + " has already gone out.");
        return advance(order);
    }

    private AtelierViewModel advance( Order order ) {
        Order moved = order.withStage(order.stage().next());
        AtelierViewModel updated = withSelected(moved);
        if ( moved.stage() == Stage.SHIPPED )
            updated = updated.withStore(drawDown(updated.store(), moved));
        return updated.note("#" + moved.ref() + " is now " + moved.stage().label().toLowerCase(Locale.ROOT) + ".");
    }

    private static Tuple<Yarn> drawDown( Tuple<Yarn> store, Order shipped ) {
        return store.map( yarn -> yarn.fibre() == shipped.fibre()
                                ? yarn.withMetresInStock(Math.max(0, yarn.metresInStock() - shipped.metres()))
                                : yarn );
    }

    // ── The loom floor ────────────────────────────────────────────────────

    /**
     *  Throws the switch on the whole floor, which starts or stops every loom
     *  that has a warp on it. A loom's own switch then holds just that one
     *  without stopping the others.
     *
     *  @return the workshop with the floor switched the other way
     */
    public AtelierViewModel toggleLooms() {
        boolean running = !loomsRunning;
        return withLoomsRunning(running)
                .withLooms(looms.map( loom -> loom.isIdle() ? loom : loom.withRunning(running) ))
                .note(running ? "Looms started." : "Looms stopped.");
    }

    /**
     *  One beat of the loom floor: every running loom gains a little on its run,
     *  and a loom that completes one advances its commission a station. Called
     *  from an animation in the view while the floor is running, so it must be
     *  cheap and must return {@code this} unchanged when there is nothing to do.
     *
     *  @return the workshop a moment later
     */
    public AtelierViewModel tickLooms() {
        if ( !loomsRunning )
            return this;

        AtelierViewModel updated = this;
        Tuple<Loom>      floor   = looms;
        for ( int i = 0; i < floor.size(); i++ ) {
            Loom loom = floor.get(i);
            if ( !loom.running() || loom.isIdle() )
                continue;
            int progress = loom.progress() + 3;
            if ( progress < 100 ) {
                floor = floor.setAt(i, loom.withProgress(progress));
                continue;
            }
            int index = indexOfRef(updated.orders(), loom.orderRef());
            if ( index < 0 ) {
                floor = floor.setAt(i, loom.withProgress(0).withRunning(false).withOrderRef(""));
                continue;
            }
            Order finished = updated.orders().get(index);
            updated = updated.advance(finished)
                             .note(loom.name() + " finished a run on #" + finished.ref() + ".");
            // The cloth has left the loom, so the loom is free: it takes the
            // next commission still waiting for a warp, or stands empty.
            floor = floor.setAt(i, loom.withOrderRef("").withProgress(0).withRunning(false));
            floor = floor.setAt(i, rethread(floor.get(i), updated.orders(), floor));
        }
        return floor.equals(looms) ? updated : updated.withLooms(floor);
    }

    /**
     *  Mounts the next commission that still needs weaving on an empty loom,
     *  skipping any already running on another one.
     *
     *  @param loom   the loom that has just come free
     *  @param orders the book to look for work in
     *  @param floor  the rest of the floor, so two looms cannot take the same job
     *  @return the loom with new work on it, or standing empty
     */
    private static Loom rethread( Loom loom, Tuple<Order> orders, Tuple<Loom> floor ) {
        for ( Order candidate : orders ) {
            if ( !candidate.stage().isOnTheFloor() )
                continue;
            boolean taken = false;
            for ( Loom other : floor )
                taken |= other.orderRef().equals(candidate.ref());
            if ( !taken )
                return loom.withOrderRef(candidate.ref()).withProgress(0).withRunning(true);
        }
        return loom;
    }

    // ── The mill portal ───────────────────────────────────────────────────

    /** Signs in to the yarn mill, which is what the reorder button needs. */
    public AtelierViewModel connectMill() {
        if ( millSecret.trim().isEmpty() )
            return withMillConnected(false).withStatus("The mill wants a passphrase before it opens the catalogue.");
        return withMillConnected(true).note("Signed in to the mill as " + millAccount + ".");
    }

    /** Tops every shelf in the store room back up to a working quantity. */
    public AtelierViewModel reorderYarn() {
        if ( !millConnected )
            return withStatus("Sign in to the mill first.");
        return withStore(store.map( yarn -> yarn.metresInStock() >= 120 ? yarn : yarn.withMetresInStock(120) ))
                .note("Reordered yarn — every shelf back to 120 m.");
    }

    // ── Derived readings ──────────────────────────────────────────────────

    /** @return metres of cloth still owed to somebody. */
    public int metresOnBooks() {
        int metres = 0;
        for ( Order order : orders )
            if ( order.stage() != Stage.SHIPPED )
                metres += order.metres();
        return metres;
    }

    /** @return what the open commissions are worth, with a euro sign. */
    public String bookValue() {
        double total = 0;
        for ( Order order : orders )
            if ( order.stage() != Stage.SHIPPED )
                total += order.total();
        return String.format(Locale.GERMANY, "€%,.0f", total);
    }

    /** @return how far the whole book has come, weighted by metres, from 0 to 1. */
    public double workload() {
        double done = 0, total = 0;
        for ( Order order : orders ) {
            done  += order.metres() * order.stage().completion();
            total += order.metres();
        }
        return total == 0 ? 0 : done / total;
    }

    /** @return the one-line summary under the workshop's name. */
    public String headline() {
        int open = 0;
        for ( Order order : orders )
            if ( order.stage() != Stage.SHIPPED )
                open++;
        return open + " open commissions · " + metresOnBooks() + " m on the books · " + bookValue();
    }

    /** @return what the order book prints when every filter has eaten every row. */
    public String emptyBookText() {
        if ( !search.trim().isEmpty() )
            return "No commission matches “" + search.trim() + "”.";
        if ( rushOnly )
            return "Nothing in the book is a rush job.";
        if ( !materialFilter.isEmpty() )
            return "Nothing in the book is woven from " + materialFilter.toLowerCase(Locale.ROOT) + ".";
        return "The book is empty. Open a docket to start one.";
    }

    /** @return {@code true} while any filter is narrowing the book down. */
    public boolean isFiltered() {
        return !search.trim().isEmpty() || rushOnly || !materialFilter.isEmpty();
    }

    public AtelierViewModel clearFilters() {
        return withSearch("").withRushOnly(false).withMaterialFilter("").withStatus("Filters cleared.");
    }

    // ── Housekeeping ──────────────────────────────────────────────────────

    /**
     *  Appends a stamped line to the day book and repeats it in the status line,
     *  which is the workshop's way of saying something happened. Old lines fall
     *  off the top so the journal cannot grow without bound.
     *
     *  @param line what happened
     *  @return a view model that has recorded it
     */
    public AtelierViewModel note( String line ) {
        String stamped = CLOCK.format(LocalTime.now()) + "  " + line;
        String updated = journal.isEmpty() ? stamped : journal + "\n" + stamped;
        String[] lines = updated.split("\n", -1);
        if ( lines.length > JOURNAL_LINES ) {
            StringBuilder kept = new StringBuilder();
            for ( int i = lines.length - JOURNAL_LINES; i < lines.length; i++ )
                kept.append(i > lines.length - JOURNAL_LINES ? "\n" : "").append(lines[i]);
            updated = kept.toString();
        }
        return withJournal(updated).withStatus(line);
    }

    // ── Small shared helpers ──────────────────────────────────────────────

    private static int indexOfRef( Tuple<Order> orders, String ref ) {
        for ( int i = 0; i < orders.size(); i++ )
            if ( orders.get(i).ref().equals(ref) )
                return i;
        return -1;
    }

    private static String text( Object cell ) {
        return cell == null ? "" : String.valueOf(cell).trim();
    }

    private static int number( Object cell, int fallback ) {
        if ( cell instanceof Number )
            return ((Number) cell).intValue();
        try {
            return Integer.parseInt(text(cell));
        } catch ( NumberFormatException nothingUsable ) {
            return fallback;
        }
    }
}
