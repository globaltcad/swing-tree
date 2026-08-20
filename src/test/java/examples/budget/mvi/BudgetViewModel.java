package examples.budget.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import swingtree.UI;
import swingtree.api.model.TableData;

import java.util.Locale;

/**
 *  The single immutable root of the whole budget planner (MVI / MVL). The
 *  {@link BudgetView} holds no Swing state of its own: it zooms lenses into the
 *  fields of this value and renders a pure function of it. Every user action —
 *  editing a cell, moving the budget slider, adding or removing an expense —
 *  produces a brand-new {@code BudgetViewModel} through the Lombok-generated
 *  withers, and the lenses fire only for the slices that actually changed.
 *  <p>
 *  Note how the expense list is simply a {@link TableData} field: an immutable
 *  value describing the <em>whole</em> table (cells, column names, column
 *  classes, its {@link UI.CellOrder} and its {@link UI.Editability}) that the view
 *  binds to a {@code Var<TableData>}. There is no {@code TableModel} to implement
 *  and no event to fire — change the value and the table follows. Because its
 *  editability is {@link UI.Editability#EDITABLE}, the user's inline edits flow
 *  straight back into this value as a new {@code TableData}.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class BudgetViewModel {

    private final TableData expenses;        // the whole editable table, as a value
    private final double    monthlyBudget;   // the target the health banner measures against

    // The "add expense" form, kept in the model so it too is pure MVI state:
    private final Category  draftCategory;
    private final String    draftDetail;
    private final String    draftAmount;

    private final String    status;          // the status / hint line at the bottom

    /** The initial state: a realistic sample month, so the app has something to show immediately. */
    public static BudgetViewModel initial() {
        return new BudgetViewModel(
            sampleExpenses(),
            2200,
            Category.FOOD,
            "",
            "",
            "Edit any amount inline, or add an expense — the chart and health update live."
        );
    }

    /** Business logic: validate the draft and, if it is a positive amount, append it as a new row. */
    public BudgetViewModel addDraft() {
        double amount;
        try {
            amount = Double.parseDouble(draftAmount.trim().replace(",", "").replace("€", "").trim());
        } catch (RuntimeException e) {
            return withStatus("\"" + draftAmount + "\" is not a valid amount.");
        }
        if (amount <= 0)
            return withStatus("Enter an amount greater than zero.");

        String detail = draftDetail.trim().isEmpty() ? draftCategory.label() : draftDetail.trim();
        TableData updated = expenses.addRow(draftCategory.label(), detail, amount);
        return new BudgetViewModel(
            updated, monthlyBudget, draftCategory, "", "",
            String.format(Locale.US, "Added %s to %s.", Budget.money(amount), draftCategory.label())
        );
    }

    /** Business logic: drop the row at the given index (a no-op for an out-of-range index). */
    public BudgetViewModel removeExpenseAt(int index) {
        if (index < 0 || index >= expenses.getRowCount())
            return withStatus("Select a row in the table first, then remove it.");
        String label = String.valueOf(expenses.getValueAt(index, expenses.indexOfColumn(Budget.COL_DETAIL)));
        return withExpenses(expenses.removeRowAt(index)).withStatus("Removed \"" + label + "\".");
    }

    /** Business logic: wipe every expense, keeping the columns. */
    public BudgetViewModel clearAll() {
        return withExpenses(emptyExpenses()).withStatus("Cleared all expenses.");
    }

    // ── Table shape ─────────────────────────────────────────────────────────

    private static TableData emptyExpenses() {
        return TableData.of(UI.CellOrder.ROW_MAJOR, Budget.COL_CATEGORY, Budget.COL_DETAIL, Budget.COL_AMOUNT)
            .asEditable()
            .setColumnClassAt(0, String.class)
            .setColumnClassAt(1, String.class)
            .setColumnClassAt(2, Double.class);
    }

    private static TableData sampleExpenses() {
        return emptyExpenses()
            .addRow("Housing",   "Rent",              920.00)
            .addRow("Housing",   "Electricity",        68.40)
            .addRow("Food",      "Groceries",         310.00)
            .addRow("Food",      "Coffee & lunches",   96.50)
            .addRow("Transport", "Monthly rail pass",  59.00)
            .addRow("Transport", "Fuel",               72.30)
            .addRow("Leisure",   "Concert tickets",    88.00)
            .addRow("Leisure",   "Streaming",          17.99)
            .addRow("Health",    "Gym membership",     35.00);
    }
}
