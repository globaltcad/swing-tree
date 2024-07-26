package examples.calculator.mvvm;

import sprouts.From;

final class Calculator {
    private final CalculatorInputs inputs = new CalculatorInputs();
    private final CalculatorOutput output = new CalculatorOutput();

    public CalculatorInputs inputs() {
        return inputs;
    }

    public CalculatorOutput output() {
        return output;
    }

    public void runCalculation() {
        try {
            double left = Double.parseDouble(inputs.left().get());
            double right = Double.parseDouble(inputs.right().get());
            double result = 0;
            switch (inputs.operator().get()) {
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
            // set result:
            output.result().set(result);
            output.valid().set(true);
            output.error().set("").fireChange(From.VIEW_MODEL);
        } catch (NumberFormatException e) {
            output.valid().set(false);
            output.error().set("Invalid number format").fireChange(From.VIEW_MODEL);
        } catch (ArithmeticException e) {
            output.valid().set(false);
            output.error().set("Division by zero").fireChange(From.VIEW_MODEL);
        }
    }
}
