package examples.simple;


import sprouts.Var;
import swingtree.UI;

import javax.swing.*;

public final class VariousSliders extends JPanel {


    public VariousSliders() {
        Var<Integer> intMin = Var.of(0);
        Var<Integer> intMax = Var.of(100);
        Var<Integer> intValue = Var.of(50);
        Var<Float> floatMin = Var.of(0f);
        Var<Float> floatMax = Var.of(100f);
        Var<Float> floatValue = Var.of(50f);
        UI.of(this).withLayout("fillx, wrap 3").withPrefSize(540, 500)
        .add(UI.label("Int Min:"))
        .add("pushx, growx",
            UI.slider(UI.Align.HORIZONTAL, -100, 100, intMin)
            .peek( it -> {
                it.setPaintLabels(true);
                it.setMajorTickSpacing(20);
                it.setMinorTickSpacing(5);
                it.setPaintTicks(true);
                it.setSnapToTicks(true);
            })
        )
        .add(UI.label(intMin.viewAsString()))
        .add(UI.label("Int Max:"))
        .add("pushx, growx",
            UI.slider(UI.Align.HORIZONTAL, -100, 100, intMax)
            .peek( it -> {
                it.setPaintLabels(true);
                it.setMajorTickSpacing(20);
                it.setMinorTickSpacing(5);
                it.setPaintTicks(true);
                it.setSnapToTicks(true);
            })
        )
        .add(UI.label(intMax.viewAsString()))
        .add(UI.label("Int Slider:"))
        .add("pushx, growx",
            UI.slider(UI.Align.HORIZONTAL, intMin, intMax, intValue)
            .peek( it -> {
                it.setPaintLabels(true);
                it.setMajorTickSpacing(20);
                it.setMinorTickSpacing(5);
                it.setPaintTicks(true);
                it.setSnapToTicks(true);
            })
        )
        .add(UI.label(intValue.viewAsString()))
        .add(UI.label("Float Min:"))
        .add("pushx, growx",
            UI.slider(UI.Align.HORIZONTAL, -100f, 100f, floatMin)
            .peek( it -> {
                it.setPaintLabels(true);
                it.setMajorTickSpacing(20);
                it.setMinorTickSpacing(5);
                it.setPaintTicks(true);
                it.setSnapToTicks(true);
            })
        )
        .add(UI.label(floatMin.viewAsString()))
        .add(UI.label("Float Max:"))
        .add("pushx, growx",
            UI.slider(UI.Align.HORIZONTAL, -100f, 100f, floatMax)
            .peek( it -> {
                it.setPaintLabels(true);
                it.setMajorTickSpacing(20);
                it.setMinorTickSpacing(5);
                it.setPaintTicks(true);
                it.setSnapToTicks(true);
            })
        )
        .add(UI.label(floatMax.viewAsString()))
        .add(UI.label("Float Slider:"))
        .add("pushx, growx",
            UI.slider(UI.Align.HORIZONTAL, floatMin, floatMax, floatValue)
            .peek( it -> {
                it.setPaintLabels(true);
                it.setMajorTickSpacing(20);
                it.setMinorTickSpacing(5);
                it.setPaintTicks(true);
                it.setSnapToTicks(true);
            })
        )
        .add(UI.label(floatValue.viewAsString()));
    }

    public static void main(String... args) {
        UI.show( f -> new VariousSliders() );
    }

}
