package examples;

import com.formdev.flatlaf.FlatLightLaf;
import swingtree.UI;

import javax.swing.*;
import java.awt.*;

public class Calculator extends JPanel
{
    public Calculator()
    {
        FlatLightLaf.setup();
        UI.of(this).withLayout("fill, insets 10")
        .add("grow, span, wrap",
            UI.panel("fill, ins 0")
            .add("shrink", UI.label("Result:"))
            .add("grow, wrap",
                UI.label("42.0").with(UI.HorizontalAlignment.RIGHT).withProperty("FlatLaf.styleClass", "large")
            )
            .add("grow, span, wrap",
                UI.textField(UI.HorizontalAlignment.RIGHT, "73 - 31").id("input-text-area")
            )
        )
        .add("growx", UI.radioButton("DEG"), UI.radioButton("RAD"))
        .add("shrinkx", UI.splitButton("sin"))
        .add("growx, wrap", UI.button("Help").withProperty("JButton.buttonType", "help"))
        .add("growx, span, wrap",
            UI.panel("fill")
            .add("span, grow, wrap",
                UI.panel("fill, ins 0")
                .add("grow",
                    UI.button("(").withProperty("JButton.buttonType", "roundRect"),
                    UI.button(")").withProperty("JButton.buttonType", "roundRect")
                )
            )
            .add("grow",
                UI.panel("fill, ins 0, wrap 3")
                .apply( it -> {
                    String[] labels = new String[]{"7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "C"};
                    for ( String label : labels ) it.add("grow", UI.button(label));
                }),
                UI.panel("fill, ins 0")
                .add("grow", UI.button("-").withProperty("JButton.buttonType", "roundRect"))
                .add("grow, wrap", UI.button("/").withProperty("JButton.buttonType", "roundRect"))
                .add("span, grow, wrap",
                    UI.panel("fill, ins 0")
                    .add("grow",
                        UI.button("+").withProperty("JButton.buttonType", "roundRect"),
                        UI.panel("fill, ins 0")
                        .add("grow, wrap",
                            UI.button("*").withProperty("JButton.buttonType", "roundRect"),
                            UI.button("%").withProperty("JButton.buttonType", "roundRect")
                        )
                    ),
                    UI.button("=")
                    .withBackground(new Color(103, 255, 190))
                    .withProperty("JButton.buttonType", "roundRect")
                )
            )
        );
    }

    // Use this to test the UI!
    public static void main(String... args) { UI.show(new Calculator()); }

}













