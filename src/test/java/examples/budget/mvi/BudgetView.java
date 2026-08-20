package examples.budget.mvi;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.CellConf;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.api.model.TableData;
import swingtree.layout.FlowCell;
import swingtree.style.ComponentStyleDelegate;
import swingtree.threading.EventProcessor;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.RenderingHints;
import java.util.Locale;

import static swingtree.UI.*;

/**
 *  A self-contained <b>monthly budget planner</b> built on the MVI / MVL pattern:
 *  everything on screen is a pure function of one immutable {@link BudgetViewModel}
 *  held in a single {@link Var}. It exists to show four SwingTree / Sprouts ideas
 *  working together in one small, genuinely useful app — each with a distinct home:
 *
 *  <ol>
 *    <li><b>A value-model table.</b> The editable expense list is bound with
 *        {@code UI.table(Var<TableData>)} — no {@code TableModel}, no listeners.
 *        {@link TableData} is an immutable value describing the whole table; the
 *        user's inline edits flow straight back into the property as a new value,
 *        which reshapes the chart and the health banner automatically. See
 *        {@link #expensesCard}.</li>
 *
 *    <li><b>A value-capturing SVG style.</b> The donut infographic is generated
 *        from the numbers as an SVG string by {@link Budget#donutSvg}, then handed
 *        to a component through {@code withStyle(svgText, (svg, it) -> it.image(...))}
 *        — the property item <em>is</em> the SVG, so the whole chart re-renders,
 *        crisply at any DPI, whenever the table changes. See {@link #donutCard}.</li>
 *
 *    <li><b>A composite view.</b> The "budget health" banner depends on three
 *        independent properties (the budget slider, the derived total spent, and
 *        the row count). Instead of chaining three {@code withStyle} calls, they are
 *        merged into one {@link BudgetHealth} item with the Sprouts composite
 *        builder {@code Viewable.of(seed, it -> it.join(a, ..).join(b, ..)...)} and a
 *        single {@code withStyle} drives the whole banner. See the constructor and
 *        {@link #healthCard}.</li>
 *
 *    <li><b>A responsive 12-column grid.</b> The three cards are laid out by a
 *        {@code withFlowLayout()} panel, each declaring with {@code AUTO_SPAN(..)}
 *        how many of 12 virtual columns it wants per width category. The window
 *        converges through four arrangements <em>without a single line of state</em>:
 *        no form-factor field, no resize listener, nothing in the view model.
 *        See {@link #body}.</li>
 *  </ol>
 *
 *  <p>The app imports zero data from the network and needs no files: a realistic
 *  month is pre-filled so there is something to play with immediately.</p>
 */
public final class BudgetView extends JPanel {

    // ── One calm, light palette (no theme switching, to keep the focus on the four ideas) ──
    private static final Color PAGE    = new Color(0xF4, 0xF6, 0xF9);
    private static final Color CARD    = new Color(0xFF, 0xFF, 0xFF);
    private static final Color INK     = new Color(0x1E, 0x24, 0x30);
    private static final Color SUBTEXT = new Color(0x6B, 0x74, 0x86);
    private static final Color BORDER  = new Color(0xE4, 0xE8, 0xEF);
    private static final Color ACCENT  = new Color(0x17, 0xA6, 0x7B);   // money green
    private static final Color TRACK   = new Color(0xE8, 0xEC, 0xF2);   // progress-bar track

    private final Var<BudgetViewModel> vm;

    // Lenses into the one immutable root — each a Var<field> that reads via a getter
    // and writes via a wither, producing a brand-new view model on every edit.
    private final Var<TableData> expenses;
    private final Var<Double>    budget;
    private final Var<Category>  draftCategory;
    private final Var<String>    draftDetail;
    private final Var<String>    draftAmount;
    private final Var<String>    status;

    // The live JTable, captured so "Remove selected" can read its selection.
    private JTable table;

    public BudgetView(Var<BudgetViewModel> vm) {
        this.vm            = vm;
        this.expenses      = vm.zoomTo(BudgetViewModel::expenses,      BudgetViewModel::withExpenses);
        this.budget        = vm.zoomTo(BudgetViewModel::monthlyBudget, BudgetViewModel::withMonthlyBudget);
        this.draftCategory = vm.zoomTo(BudgetViewModel::draftCategory, BudgetViewModel::withDraftCategory);
        this.draftDetail   = vm.zoomTo(BudgetViewModel::draftDetail,   BudgetViewModel::withDraftDetail);
        this.draftAmount   = vm.zoomTo(BudgetViewModel::draftAmount,   BudgetViewModel::withDraftAmount);
        this.status        = vm.zoomTo(BudgetViewModel::status,        BudgetViewModel::withStatus);

        // Read-only views derived from the (editable) table value. They recompute
        // whenever a cell is edited or a row is added/removed.
        Val<String>  donutSvg = expenses.viewAsString(Budget::donutSvg);   // the whole chart, as SVG text
        Val<Double>  spent    = expenses.viewAsDouble(Budget::totalOf);    // the total of every amount
        Val<Integer> count    = expenses.viewAsInt(td -> td.getRowCount());

        // ── The composite view: fold three independent properties into ONE item ──
        // so a single withStyle can drive the whole "budget health" banner. It is
        // recomputed as a whole whenever the budget, the total spent, OR the item
        // count changes. (A composite is held strongly by SwingTree's binding, so
        // building it inline here is safe from GC.)
        Viewable<BudgetHealth> health = Viewable.of(BudgetHealth.empty(), it -> it
            .join(budget, BudgetHealth::withBudget)
            .join(spent,  BudgetHealth::withSpent)
            .join(count,  BudgetHealth::withItemCount));

        // Every row is added with "wmin 0": a flow grid reports the *sum* of its
        // children's minimum widths, so without it the body would pin the window
        // open at the width of all three cards side by side.
        of(this).withLayout(FILL.and(WRAP(1)).and(INS(0)).and("gap 0")).withPrefSize(1420, 764)
        .withStyle(it -> it.backgroundColor(PAGE).foundationColor(PAGE))
        .add(GROW_X.and("wmin 0"), header())
        .add(GROW.and(PUSH).and("wmin 0"), body(donutSvg, health))
        .add(GROW_X.and("wmin 0"), statusBar());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  The body — concept #4: a responsive 12-column grid
    // ════════════════════════════════════════════════════════════════════════

    /*
     *  The three cards are siblings in one flow grid, each declaring per size
     *  category how many of the 12 virtual columns it wants. A category is the
     *  panel's width relative to its *preferred* width, so the breakpoints move
     *  with the content instead of being hard-coded pixels:
     *
     *      OVERSIZE / VERY_LARGE  6 | 3 | 3   three columns — the dashboard look
     *      LARGE                  6 | 6 | 12  table beside chart, health a wide strip
     *      MEDIUM                 12 | 6 | 6  full-width table, the rest below it
     *      SMALL and narrower     12 | 12 | 12  one column, scrolled: a phone
     */
    private static final FlowCell EXPENSES_SPAN = AUTO_SPAN(it -> it
            .verySmall(12).small(12).medium(12).large(6).veryLarge(6).oversize(6));
    private static final FlowCell DONUT_SPAN = AUTO_SPAN(it -> it
            .verySmall(12).small(12).medium(6).large(6).veryLarge(3).oversize(3));
    private static final FlowCell HEALTH_SPAN = AUTO_SPAN(it -> it
            .verySmall(12).small(12).medium(6).large(12).veryLarge(3).oversize(3));

    /**
     *  A flow grid gives every row the height of its tallest child, so once the
     *  cards stack the page outgrows the window — hence the scroll pane.
     */
    private UIForAnySwing<?, ?> body(Val<String> donutSvg, Viewable<BudgetHealth> health) {
        return scrollPane(conf -> conf.fitWidth(true))
            .withHorizontalScrollBarPolicy(UI.Active.NEVER)
            .withScrollIncrement(24)
            .withStyle(it -> it.backgroundColor(PAGE).border(0, PAGE))
            .add(
                // A zero minimum, or the grid's own minimum (the SUM of its
                // children's) would pin the page open at three cards wide.
                panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 18, 18)
                .withMinSize(0, 0)
                .withStyle(it -> it.backgroundColor(PAGE))
                .add(EXPENSES_SPAN, expensesCard())
                .add(DONUT_SPAN,    donutCard(donutSvg))
                .add(HEALTH_SPAN,   healthCard(health))
            );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Header
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?, ?> header() {
        return panel(FILL).withLayout("fill, ins 20 26 18 26", "[grow][]")
            .withStyle(it -> it
                .backgroundColor(CARD)
                .borderAt(UI.Edge.BOTTOM, 3, ACCENT)
                .shadow("h", s -> s.color(new Color(20, 30, 60, 26)).offset(0, 2).blurRadius(10)))
            .add(GROW_X.and("wmin 0"),
                box(FILL.and(WRAP(1)).and(INS(0)))
                .add(GROW_X.and("wmin 0"), label("💶  Monthly Budget Planner")
                    .withStyle(it -> it.componentFont(f -> f.family("SansSerif").size(20).weight(2f).color(INK))))
                .add(GROW_X.and("wmin 0"),
                    label("Editable value-model table · live SVG breakdown · responsive 12-column grid")
                    .withStyle(it -> it.componentFont(f -> f.family("SansSerif").size(12).color(SUBTEXT))))
            );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Left — the editable expense table (concept #3: Var<TableData>)
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?, ?> expensesCard() {
        return panel(FILL.and(WRAP(1)).and(INS(0))).withStyle(BudgetView::card)
            .add(GROW_X, cardHeader("Expenses",
                "Edit any cell inline — the Amount column is euro-formatted, yet commits a Double."))
            .add(GROW.and(PUSH),
                // A declared preferred size: the grid derives both the card's share
                // of the row and its breakpoints from the preferred sizes inside it.
                scrollPane().withPrefSize(520, 380)
                .withStyle(it -> it.backgroundColor(CARD).border(0, CARD).padding(0, 10, 0, 10))
                .add(
                    // The whole binding: an immutable TableData value in a mutable
                    // property. An editable TableData value + a Var makes it editable,
                    // and edits are written back into the property as a new value.
                    table(expenses)
                    .peek(t -> {
                        this.table = t;
                        t.setRowHeight(30);
                        t.setFillsViewportHeight(true);
                        t.getTableHeader().setReorderingAllowed(false);
                    })
                    // A withCell renderer *and* editor for just the Amount column: it
                    // shows a right-aligned "€68.40" while displaying, and a plain,
                    // dot-decimal number while editing (so the commit parses cleanly
                    // back to the column's Double type).
                    .withCellForColumn(Budget.COL_AMOUNT, this::amountCell)
                )
            )
            .add(GROW_X, addForm())
            .add(GROW_X, actionBar());
    }

    /**
     *  Two deliberate rows rather than one clever one: the card is only ever as
     *  wide as its span — 6 of 12 columns — so one line holding three inputs and a
     *  button was cramped at <em>every</em> size.
     *  <p>
     *  A nested {@code withFlowLayout} was the tempting alternative, but a grid
     *  only reports an honest height to a parent that asks it width-for-height,
     *  which a MigLayout cell does not. Nest a grid in a grid, or in a scroll pane.
     */
    private UIForAnySwing<?, ?> addForm() {
        return box(FILL).withLayout("fillx, wrap 2, ins 8 16 4 16, gap 6", "[90::160][grow]")
            .add(GROW_X, comboBox(draftCategory, Category::label))
            .add(GROW_X.and("wmin 0"), textField(draftDetail))
            .add(GROW_X.and("span 2, wmin 0"),
                box(FILL).withLayout("fillx, ins 0, gap 6", "[][grow][]")
                .add(label("€").withStyle(it -> it.componentFont(f -> f.family("SansSerif").size(13).color(SUBTEXT))))
                .add(GROW_X.and("wmin 0"),
                    textField(draftAmount).onEnter(it -> vm.update(BudgetViewModel::addDraft)))
                .add(primaryButton("＋ Add", () -> vm.update(BudgetViewModel::addDraft)))
            );
    }

    private UIForAnySwing<?, ?> actionBar() {
        return box(FILL).withLayout("fillx, ins 4 16 12 16, gap 8", "[grow][][]")
            .add(GROW_X, box())   // spacer, pushes the buttons to the right
            .add(ghostButton("Remove selected",
                () -> vm.update(v -> v.removeExpenseAt(table == null ? -1 : table.getSelectedRow()))))
            .add(ghostButton("Clear all", () -> vm.update(BudgetViewModel::clearAll)));
    }

    /**
     *  A {@code withCell} renderer <em>and</em> editor for the Amount column. The one
     *  configurator is called in both modes: while <b>editing</b> it seeds the field
     *  with a plain, dot-decimal number so the commit converts straight back to the
     *  column's {@code Double}; while <b>rendering</b> it shows a right-aligned,
     *  euro-formatted, selection-aware label.
     */
    private CellConf<JTable, Object> amountCell(CellConf<JTable, Object> cell) {
        double amount = Budget.toDouble(cell.entry().orElse(null));
        if (cell.isEditing())
            return cell.presentationEntry(String.format(Locale.US, "%.2f", amount));

        boolean selected = cell.isSelected();
        return cell.updateView(view -> view
            .orGetUi(() -> UI.label(""))
            .updateIf(JLabel.class, label -> {
                label.setText(Budget.moneyCents(amount));
                label.setHorizontalAlignment(SwingConstants.RIGHT);
                label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
                label.setOpaque(selected);
                label.setForeground(selected && table != null ? table.getSelectionForeground() : INK);
                if (selected && table != null)
                    label.setBackground(table.getSelectionBackground());
                return label;
            }));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  The donut chart and the health banner — two more cards in the same grid
    // ════════════════════════════════════════════════════════════════════════

    /**
     *  Concept #1 — a value-capturing SVG style. The property item {@code svg} is the
     *  whole chart as SVG text (generated by {@link Budget#donutSvg}); the style hands
     *  it to {@code img.svg(..)} and the component re-renders on every change, in a
     *  thread-safe fashion, without us ever reading the property inside the lambda.
     */
    private UIForAnySwing<?, ?> donutCard(Val<String> donutSvg) {
        return panel(FILL.and(WRAP(1)).and(INS(0))).withStyle(BudgetView::card)
            .add(GROW_X, cardHeader("Where it goes", "By category — the whole infographic is one generated SVG string."))
            .add(GROW.and(PUSH),
                box().withPrefSize(300, 320).withMinSize(200, 200)
                .withStyle(donutSvg, (svg, it) -> it
                    .image(img -> img
                        .svg(svg)
                        .fitMode(UI.FitComponent.MIN_DIM)
                        .placement(UI.Placement.CENTER))
                    .padding(0, 12, 14, 12))
            );
    }

    /**
     *  Concept #2 — the composite view drives everything here. The big number, its
     *  colour, the caption, the sub-line and the progress bar are all read from the
     *  single merged {@link BudgetHealth} item, so they can never disagree with each
     *  other. The slider writes the budget, one of the composite's three inputs.
     */
    private UIForAnySwing<?, ?> healthCard(Viewable<BudgetHealth> health) {
        return panel(FILL.and(WRAP(1)).and(INS(0))).withStyle(BudgetView::card)
            .add(GROW_X, cardHeader("Budget health", "Drag to set your monthly target."))
            .add(GROW_X,
                box(FILL).withLayout("fill, ins 2 18 6 18, gap 10", "[][grow][70!]")
                .add(label("Budget").withStyle(it -> it.componentFont(f -> f.family("SansSerif").size(12).weight(2f).color(SUBTEXT))))
                .add(GROW_X, slider(UI.Align.HORIZONTAL, 0.0, 5000.0, budget))
                .add(label(budget.viewAsString(Budget::money)).withStyle(it -> it.componentFont(f -> f.family("SansSerif").size(13).weight(2f).color(INK))))
            )
            .add(GROW_X,
                box(FILL.and(INS(0))).withLayout("fill, ins 0 14 14 14")
                .add(GROW_X, banner(health))
            );
    }

    private UIForAnySwing<?, ?> banner(Viewable<BudgetHealth> health) {
        return panel(FILL.and(WRAP(1)).and(INS(0))).withLayout("fill, wrap 1, ins 0")
            // The single, composite-driven style: background tint, border and the
            // progress bar are all a pure function of the merged health item.
            .withStyle(health, BudgetView::bannerStyle)
            .add(GROW_X.and("wmin 0"), label(health.viewAsString(h -> Budget.money(Math.abs(h.remaining()))))
                .withStyle(health, (h, it) -> it.componentFont(f -> f.family("SansSerif").size(32).weight(2f).color(stateColor(h.state())))))
            .add(GROW_X.and("wmin 0"), label(health.viewAsString(h -> h.state() == BudgetHealth.State.OVER ? "over budget" : "left to spend"))
                .withStyle(it -> it.componentFont(f -> f.family("SansSerif").size(13).weight(2f).color(SUBTEXT))))
            .add(GROW_X.and("wmin 0"), label(health.viewAsString(h ->
                    Budget.money(h.spent()) + " spent of " + Budget.money(h.budget()) + "   ·   " + h.itemCount() + " items"))
                .withStyle(it -> it.componentFont(f -> f.family("SansSerif").size(12).color(SUBTEXT)).padding(2, 0, 0, 0)));
    }

    /** The banner's background tint, border and the little progress bar drawn along its bottom. */
    private static ComponentStyleDelegate<JPanel> bannerStyle(BudgetHealth h, ComponentStyleDelegate<JPanel> it) {
        Color c = stateColor(h.state());
        double ratio = Math.max(0, Math.min(1, h.ratio()));
        final int w = it.componentWidth();
        final int hh = it.componentHeight();
        return it
            .backgroundColor(mix(c, Color.WHITE, 0.88))
            .borderRadius(12)
            .border(1, mix(c, BORDER, 0.45))
            .margin(0)
            .padding(14, 18, 34, 18)     // extra bottom padding reserves room for the bar
            .painter(UI.Layer.CONTENT, UI.ComponentArea.BODY, g -> {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                final int pad = 16, barH = 8, y = hh - 20, barW = w - 2 * pad;
                g.setColor(TRACK);
                g.fillRoundRect(pad, y, barW, barH, barH, barH);
                g.setColor(c);
                g.fillRoundRect(pad, y, (int) Math.round(barW * ratio), barH, barH, barH);
            });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Status line
    // ════════════════════════════════════════════════════════════════════════

    private UIForAnySwing<?, ?> statusBar() {
        return label(status).withStyle(it -> it
            .backgroundColor(PAGE)
            .padding(9, 26, 11, 26)
            .componentFont(f -> f.family("SansSerif").size(12).color(SUBTEXT)));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Small shared building blocks
    // ════════════════════════════════════════════════════════════════════════

    /** The white, rounded, softly-shadowed card surface shared by every panel. */
    private static <C extends JComponent> ComponentStyleDelegate<C> card(ComponentStyleDelegate<C> it) {
        return it
            .backgroundColor(CARD)
            .borderRadius(14)
            .border(1, BORDER)
            .margin(0)
            .padding(0)
            .shadow("card", s -> s.color(new Color(20, 30, 60, 22)).offset(0, 3).blurRadius(16).spreadRadius(0));
    }

    private UIForAnySwing<?, ?> cardHeader(String title, String sub) {
        return box(FILL.and(WRAP(1)).and(INS(0))).withLayout("fill, wrap 1, ins 16 18 6 18", "[grow]")
            .add(GROW_X.and("wmin 0"), label(title).withStyle(it -> it.componentFont(f -> f.family("SansSerif").size(15).weight(2f).color(INK))))
            .add(GROW_X.and("wmin 0"), label(sub).withStyle(it -> it.componentFont(f -> f.family("SansSerif").size(12).color(SUBTEXT))));
    }

    private UIForAnySwing<?, ?> primaryButton(String text, Runnable onClick) {
        return button(text).withCursor(UI.Cursor.HAND).onClick(it -> onClick.run())
            .withStyle(it -> it
                .backgroundColor(ACCENT).foregroundColor(Color.WHITE)
                .borderRadius(9).border(0, ACCENT).padding(7, 16, 7, 16).margin(2)
                .componentFont(f -> f.family("SansSerif").size(13).weight(2f).color(Color.WHITE)));
    }

    private UIForAnySwing<?, ?> ghostButton(String text, Runnable onClick) {
        return button(text).withCursor(UI.Cursor.HAND).onClick(it -> onClick.run())
            .withStyle(it -> it
                .backgroundColor(CARD).foregroundColor(INK)
                .borderRadius(9).border(1, BORDER).padding(6, 14, 6, 14).margin(2)
                .componentFont(f -> f.family("SansSerif").size(13).color(INK)));
    }

    private static Color stateColor(BudgetHealth.State state) {
        switch (state) {
            case OVER: return new Color(0xE0, 0x57, 0x4B);   // red
            case NEAR: return new Color(0xE0, 0xA3, 0x2E);   // amber
            default:   return ACCENT;                        // green (same as the brand accent)
        }
    }

    /** Linear blend of two colours, {@code t} in [0,1] moving from {@code a} to {@code b}. */
    private static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            (int) Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }

    // ════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        FlatLightLaf.setup();
        Var<BudgetViewModel> vm = Var.of(BudgetViewModel.initial());
        UI.show("Monthly Budget Planner — SwingTree", f -> new BudgetView(vm));
        EventProcessor.DECOUPLED.join();
    }
}
