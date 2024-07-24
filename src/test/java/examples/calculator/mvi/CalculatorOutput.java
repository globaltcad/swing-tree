package examples.calculator.mvi;

final class CalculatorOutput {
    private final double result;
    private final boolean valid;
    private final String error;

    public CalculatorOutput(double result, boolean valid, String error) {
        this.result = result;
        this.valid = valid;
        this.error = error;
    }

    public static CalculatorOutput empty() {
        return new CalculatorOutput(0, false, "");
    }

    public double result() {
        return result;
    }

    public boolean valid() {
        return valid;
    }

    public String error() {
        return error;
    }

    public CalculatorOutput withResult(double result) {
        return new CalculatorOutput(result, valid, error);
    }

    public CalculatorOutput withValid(boolean valid) {
        return new CalculatorOutput(result, valid, error);
    }

    public CalculatorOutput withError(String error) {
        return new CalculatorOutput(result, valid, error);
    }
}
