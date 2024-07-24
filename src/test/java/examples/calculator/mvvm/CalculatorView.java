package examples.calculator.mvvm;

import swingtree.UI;

import javax.swing.JPanel;

import static swingtree.UI.*;
import static swingtree.UIFactoryMethods.comboBox;
import static swingtree.UIFactoryMethods.textField;

public final class CalculatorView extends JPanel {

    public static void main(String... args) {
        UI.show(f->createView());
    }

    public static CalculatorView createView() {
        return new CalculatorView(new Calculator());
    }

    public CalculatorView(Calculator vm) {
        CalculatorInputs inputs = vm.inputs();
        CalculatorOutput output = vm.output();
        UI.of(this).withLayout("fill")
        .add("span, center, wrap", html("<h2>Calculator</h2>"))
        .add("pushx, growx, width 60px::",
            textField(inputs.left())
        )
        .add("width 40px::",
            comboBox(inputs.operator(), o -> {
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
            textField(inputs.right())
        )
        .add("wrap",
            button("Run!").onClick( e -> vm.runCalculation() )
        )
        .add("span, center, wrap, height 60px::",
            label(output.error().viewAsString( error -> {
                if ( output.valid().get() ) {
                    return "Result: " + output.result().get();
                } else if ( !output.error().get().isEmpty() ) {
                    return "Error: " + output.error().get();
                }
                return "";
            }))
        );
    }
}
