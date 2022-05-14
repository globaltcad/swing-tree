package com.globaltcad.swingtree;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

public class Example3 extends JPanel
{
    public Example3()
    {
        FlatLightLaf.setup();
        String description = "All of this is less than a hundred lines of code!<br>The layout of this is powered by MigLayout.<br><br>";

        UI.of(this)
        .withLayout("fill, insets 10","[grow][shrink]")
        .withBackground(Color.WHITE)
        .add("cell 0 0",
            UI.label("Hello and welcome to this UI! (Click me I'm a link)")
            .makeBold()
            .makeLinkTo("https://github.com/globaltcad")
        )
        .add("cell 0 1, grow, shrinky",
            UI.panel("fill, insets 0","[grow][shrink]")
            .withBackground(Color.WHITE)
            .add("cell 0 0, aligny top, grow x, grow y",
                UI.panel("fill, insets 7","grow")
                    .withBackground(Color.LIGHT_GRAY)
                    .add( "span",
                            UI.label("<html><div style=\"width:275px;\">"+ description +"</div></html>")
                    )
                    .add("shrink", UI.label("First Name"))
                    .add("grow", UI.textField("John"))
                    .add("gap unrelated, shrink", UI.label("Last Name"))
                    .add("wrap, grow", UI.textField("Smith"))
                    .add("shrink", UI.label("Address"))
                    .add("grow",
                        UI.splitButton("Hey!")
                        .onButtonClick(it -> it.displayCurrentItemText() )
                        .onSplitClick(it -> it.displayCurrentItemText() )
                        .onSelection(
                            it -> it.displayCurrentItemText()
                                    .unselectAllItems().selectCurrentItem()
                        )
                        .add(UI.splitRadioItem("first")
                            .onButtonClick(it -> it.getCurrentItem().setText(":P") )
                        )
                        .add(UI.splitRadioItem("second")
                            .onButtonClick(it -> System.out.println("SECOND CLICKED"))
                        )
                        .add(UI.splitRadioItem("third")
                            .onButtonClick(it -> System.out.println("THIRD CLICKED"))
                        )
                        .add(UI.splitRadioItem("Fourth")
                            .onButtonClick(it -> System.out.println("THIRD CLICKED"))
                        )
                    )
            )
            .add("cell 1 0, grow y",
                UI.panel("fill", "[grow]")
                .add("cell 0 0, aligny top", UI.button("I am a normal button"))
                .add("cell 0 1, aligny top",
                    UI.button("<html><i>I am a naked button</i><html>")
                    .withCursor(UI.Cursor.HAND)
                    .makePlain()
                    .onClick( e -> {/* does something */} )
                )
                .withBorder(BorderFactory.createMatteBorder(0,0,1,0,Color.LIGHT_GRAY))
                .add( "cell 0 2, aligny bottom, span, shrink", UI.label("Here is a text area:"))
                .add("cell 0 3, aligny bottom, span, grow", UI.textArea("Anything..."))
            )
        )
        .add("cell 0 2, grow",
            UI.panel("fill, insets 0 0 0 0","[grow][grow][grow]")
            .withBackground(Color.WHITE)
            .add("cell 1 0", UI.label("Built with swingtree"))
            .add("cell 2 0", UI.label("GTS-OSS"))
            .add("cell 3 0", UI.label("29-March-2022"))
        )
        .add("cell 0 3, span 2, grow",
            UI.label("...here the UI comes to an end...").withForeground(Color.LIGHT_GRAY)
        )
        .withBackground(Color.WHITE);
    }

    // Use this to test the UI!
    public static void main(String... args) {
        new UI.TestWindow(JFrame::new,new Example3()).getFrame().setSize(new Dimension(700, 300));
    }

}
