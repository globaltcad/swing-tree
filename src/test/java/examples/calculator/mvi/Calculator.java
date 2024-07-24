package examples.calculator.mvi;

final class Calculator {
    private final CalculatorInputs inputs;
    private final CalculatorOutput output;

    public Calculator(CalculatorInputs inputs, CalculatorOutput output) {
        this.inputs = inputs;
        this.output = output;
    }

    public static Calculator empty() {
        return new Calculator(CalculatorInputs.empty(), CalculatorOutput.empty());
    }

    public CalculatorInputs inputs() {
        return inputs;
    }

    public CalculatorOutput output() {
        return output;
    }

    public Calculator withInputs(CalculatorInputs inputs) {
        return new Calculator(inputs, output);
    }

    public Calculator withOutput(CalculatorOutput output) {
        return new Calculator(inputs, output);
    }

    public Calculator runCalculation() {
        try {
            double left = Double.parseDouble(inputs.left());
            double right = Double.parseDouble(inputs.right());
            double result = 0;
            switch (inputs.operator()) {
                case ADD:
                    result = left + right;
                    break;
                case SUBTRACT:
                    result = left - right;
                    break;
                case MULTIPLY:
                    result = left * right;
                    break;
                case DIVIDE:
                    result = left / right;
                    break;
            }
            ;
            return withOutput(output.withResult(result).withValid(true));
        } catch (NumberFormatException e) {
            return withOutput(output.withError("Invalid number format").withValid(false));
        } catch (ArithmeticException e) {
            return withOutput(output.withError("Division by zero").withValid(false));
        }
    }
}
