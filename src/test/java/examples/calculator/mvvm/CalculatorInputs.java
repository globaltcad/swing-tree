package examples.calculator.mvvm;

import sprouts.Var;

final class CalculatorInputs {
    private final Var<String>   left = Var.of("");
    private final Var<String>   right = Var.of("");
    private final Var<Operator> operator = Var.of(Operator.ADD);

    public Var<String> left() {
        return left;
    }

    public Var<String> right() {
        return right;
    }

    public Var<Operator> operator() {
        return operator;
    }
}
