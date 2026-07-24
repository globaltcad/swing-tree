package examples.budget.mvi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;

/**
 *  A tiny immutable value merging everything the "budget health" banner needs
 *  into <em>one</em> item, so a <em>single</em> {@code withStyle} call can drive
 *  the whole banner instead of chaining one per property.
 *  <p>
 *  It is never stored in the view model. It is assembled on the fly by the
 *  Sprouts <b>composite view builder</b> in {@link BudgetView} —
 *  <pre>{@code
 *  Viewable.of(BudgetHealth.empty(), it -> it
 *      .join(budget, BudgetHealth::withBudget)
 *      .join(spent,  BudgetHealth::withSpent)
 *      .join(count,  BudgetHealth::withItemCount));
 *  }</pre>
 *  — which folds the three independent properties (the budget slider, the
 *  derived total spent, and the row count) into this record and recomputes it as
 *  a whole whenever any of them changes. The derived helpers ({@link #remaining()},
 *  {@link #ratio()}, {@link #state()}) then keep all the "is this healthy?"
 *  arithmetic in one pure, testable place.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor
public final class BudgetHealth {

    /** How the spending compares to the budget — drives the banner colour. */
    public enum State { UNDER, NEAR, OVER }

    private final double budget;      // the monthly budget the user set
    private final double spent;       // the total of all expenses
    private final int    itemCount;   // how many expenses there are

    public static BudgetHealth empty() { return new BudgetHealth(0, 0, 0); }

    /** What is left of the budget; negative once the budget is exceeded. */
    public double remaining() { return budget - spent; }

    /** Fraction of the budget already spent (0 when no budget is set). */
    public double ratio() { return budget <= 0 ? 0 : spent / budget; }

    public State state() {
        if (budget <= 0)      return State.UNDER;
        if (spent > budget)   return State.OVER;
        if (ratio() >= 0.85)  return State.NEAR;
        return State.UNDER;
    }
}
