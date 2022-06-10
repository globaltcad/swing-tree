package com.globaltcad.swingtree.examples;

import com.formdev.flatlaf.FlatLightLaf;
import com.globaltcad.swingtree.UI;

import javax.swing.*;
import java.awt.*;

public class Example4 extends JPanel
{
    public Example4()
    {
        FlatLightLaf.setup();

        UI.of(this)
        .withLayout("fill, insets 10")
        .add("grow, wrap",
            UI.panel("fill, ins 0")
            .add("grow, wrap", UI.label("419.0").withPosition(UI.VerticalAlign.CENTER))
            .add("grow, wrap", UI.textArea("245 + 534 - 24"))
        )
        .add("grow, wrap",
            UI.panel("fill")
            .add("span, grow, wrap",
                UI.panel("fill, ins 0")
                .add("grow",
                    UI.button("(").withProperty("JButton.buttonType", "roundRect"),
                    UI.button(")").withProperty("JButton.buttonType", "roundRect")
                )
            )
            .add("grow",
                UI.panel("fill, ins 0")
                .add("grow", UI.button("7"))
                .add("grow", UI.button("8"))
                .add("grow, wrap",UI.button("9"))
                .add("grow", UI.button("4"))
                .add("grow", UI.button("5"))
                .add("grow, wrap",UI.button("6"))
                .add("grow", UI.button("1"))
                .add("grow", UI.button("2"))
                .add("grow, wrap",UI.button("3"))
                .add("grow", UI.button("0"))
                .add("grow", UI.button("."))
                .add("grow, wrap",UI.button("C"))
            )
            .add("grow",
                UI.panel("fill, ins 0")
                .add("grow", UI.button("-").withProperty("JButton.buttonType", "roundRect"))
                .add("grow, wrap", UI.button("/").withProperty("JButton.buttonType", "roundRect"))
                .add("span, grow, wrap",
                    UI.panel("fill, ins 0")
                    .add("grow", UI.button("+").withProperty("JButton.buttonType", "roundRect"))
                    .add("grow",
                        UI.panel("fill, ins 0")
                        .add("grow, wrap",
                            UI.button("*").withProperty("JButton.buttonType", "roundRect"),
                            UI.button("%").withProperty("JButton.buttonType", "roundRect")
                        )
                    ),
                    UI.button("=").withProperty("JButton.buttonType", "roundRect")
                )
            )
        );
    }

    // Use this to test the UI!
    public static void main(String... args) {
        new UI.TestWindow(JFrame::new,new Example4()).getFrame().setSize(new Dimension(270, 400));
    }

}
