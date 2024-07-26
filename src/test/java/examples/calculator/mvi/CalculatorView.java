package examples.calculator.mvi;

import sprouts.Var;
import swingtree.UI;

import javax.swing.*;

import static swingtree.UI.*;

public final class CalculatorView extends JPanel {

    public static void main(String... args) {
        UI.show(f->createView());
    }

    public static CalculatorView createView() {
        Var<Calculator> vm = Var.of(Calculator.empty());
        return new CalculatorView(vm);
    }

    public CalculatorView(Var<Calculator> vm) {
        Var<CalculatorInputs> inputs = vm.zoomTo(Calculator::inputs, Calculator::withInputs);
        Var<CalculatorOutput> output = vm.zoomTo(Calculator::output, Calculator::withOutput);
        UI.of(this).withLayout("fill")
        .add("span, center, wrap", html("<h2>Calculator</h2>"))
        .add("pushx, growx, width 60px::",
            textField(inputs.zoomTo(CalculatorInputs::left, CalculatorInputs::withLeft))
        )
        .add("width 40px::",
            comboBox(inputs.zoomTo(CalculatorInputs::operator, CalculatorInputs::withOperator), o -> {
                switch ( o ) {
                    case ADD: return " + ";
                    case SUBTRACT: return " - ";
                    case MULTIPLY: return " * ";
                    case DIVIDE: return " / ";
                }
                return "";
            })
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
}
