package examples.mvc;

import sprouts.Var;
import swingtree.UI;

import javax.swing.JPanel;

import static swingtree.UI.*;

public class FunctionalMVVM
{
    public enum Operator {
        ADD, SUBTRACT, MULTIPLY, DIVIDE
    }

    public record CalculatorInputs(
            String left, String right, Operator operator
    ){
        public static CalculatorInputs empty(){return new CalculatorInputs("", "", Operator.ADD);}
        public CalculatorInputs withLeft(String left){return new CalculatorInputs(left, right, operator);}
        public CalculatorInputs withRight(String right){return new CalculatorInputs(left, right, operator);}
        public CalculatorInputs withOperator(Operator operator){return new CalculatorInputs(left, right, operator);}
    }

    public record CalculatorOutput(
            double result,
            boolean valid,
            String error
    ){
        public static CalculatorOutput empty(){return new CalculatorOutput(0, false, "");}
        public CalculatorOutput withResult(double result){return new CalculatorOutput(result, valid, error);}
        public CalculatorOutput withValid(boolean valid){return new CalculatorOutput(result, valid, error);}
        public CalculatorOutput withError(String error){return new CalculatorOutput(result, valid, error);}
    }

    public record CalculatorViewModel(
            CalculatorInputs inputs,
            CalculatorOutput output
    ){
        public static CalculatorViewModel empty(){return new CalculatorViewModel(CalculatorInputs.empty(), CalculatorOutput.empty());}
        public CalculatorViewModel withInputs(CalculatorInputs inputs){return new CalculatorViewModel(inputs, output);}
        public CalculatorViewModel withOutput(CalculatorOutput output){return new CalculatorViewModel(inputs, output);}
        public CalculatorViewModel runCalculation(){
            try{
                double left = Double.parseDouble(inputs.left());
                double right = Double.parseDouble(inputs.right());
                double result = switch(inputs.operator()){
                    case ADD -> left + right;
                    case SUBTRACT -> left - right;
                    case MULTIPLY -> left * right;
                    case DIVIDE -> left / right;
                };
                return withOutput(output.withResult(result).withValid(true));
            } catch ( NumberFormatException e ) {
                return withOutput(output.withError("Invalid number format").withValid(false));
            } catch ( ArithmeticException e ) {
                return withOutput(output.withError("Division by zero").withValid(false));
            }
        }
    }

    public static class CalculatorView extends JPanel {
        public CalculatorView(Var<CalculatorViewModel> vm) {
            Var<CalculatorInputs> inputs = vm.zoomTo(CalculatorViewModel::inputs, CalculatorViewModel::withInputs);
            Var<CalculatorOutput> output = vm.zoomTo(CalculatorViewModel::output, CalculatorViewModel::withOutput);
            UI.of(this).withLayout("fill")
            .add("span, center, wrap", html("<h2>Calculator</h2>"))
            .add("pushx, growx, width 60px::",
                textField(inputs.zoomTo(CalculatorInputs::left, CalculatorInputs::withLeft))
            )
            .add("width 40px::",
                comboBox(inputs.zoomTo(CalculatorInputs::operator, CalculatorInputs::withOperator), o ->
                    switch ( o ) {
                        case ADD -> " + ";
                        case SUBTRACT -> " - ";
                        case MULTIPLY -> " * ";
                        case DIVIDE -> " / ";
                    }
                )
            )
            .add("pushx, growx, width 60px::",
                textField(inputs.zoomTo(CalculatorInputs::right, CalculatorInputs::withRight))
            )
            .add("wrap",
                button("Run!").onClick( e -> vm.set(vm.get().runCalculation()) )
            )
            .add("span, center, wrap, height 60px::",
                label(output.viewAsString( result -> {
                    if ( result.valid() ) {
                        return "Result: " + result.result();
                    } else if ( !result.error().isEmpty() ) {
                        return "Error: " + result.error();
                    }
                    return "";
                }))
            );
        }
        // For running the example
        public static void main(String... args) {
            Var<CalculatorViewModel> vm = Var.of(CalculatorViewModel.empty());
            UI.show(f->new CalculatorView(vm));
        }
    }
}
