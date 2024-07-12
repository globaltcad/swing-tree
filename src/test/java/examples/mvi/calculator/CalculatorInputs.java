package examples.mvi.calculator;

final class CalculatorInputs {
    private final String left;
    private final String right;
    private final Operator operator;

    public CalculatorInputs(String left, String right, Operator operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    public static CalculatorInputs empty() {
        return new CalculatorInputs("", "", Operator.ADD);
    }

    public String left() {
        return left;
    }

    public String right() {
        return right;
    }

    public Operator operator() {
        return operator;
    }

    public CalculatorInputs withLeft(String left) {
        return new CalculatorInputs(left, right, operator);
    }

    public CalculatorInputs withRight(String right) {
        return new CalculatorInputs(left, right, operator);
    }

    public CalculatorInputs withOperator(Operator operator) {
        return new CalculatorInputs(left, right, operator);
    }
}
