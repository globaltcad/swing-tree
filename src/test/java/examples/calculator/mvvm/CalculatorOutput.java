package examples.calculator.mvvm;

import sprouts.Var;

final class CalculatorOutput {
    private final Var<Double>  result = Var.of(0.0);
    private final Var<Boolean> valid = Var.of(true);
    private final Var<String>  error = Var.of("");

    public Var<Double> result() {
        return result;
    }

    public Var<Boolean> valid() {
        return valid;
    }

    public Var<String> error() {
        return error;
    }
}
