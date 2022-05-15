package com.globaltcad.swingtree;


import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

public class Example4 extends JPanel
{
    private final static String S = "6";
    private final static String M = "12";
    private final static String L = "16";
    private final static String INDENT = "22";

    public Example4()
    {
        FlatLightLaf.setup();

        UI.of(this).withLayout("fillx, gap rel 0")
        .add("aligny top, growx, span, wrap",
            UI.panel("insets 0, fillx", "[shrink][grow][shrink][grow]", "[]"+S+"[]"+M+"[]"+L+"[]"+L+"[]0")
            .add(UI.label("Label 1"))
            .add("growx, pushy", UI.textField("aligned on text baseline"))
            .add("growx", UI.label("left: big space"))
            .add("growx, pushy, wrap", UI.textField("same width as left field"))
            .add(UI.label("Label 2"))
            .add("growx, pushy", UI.textField("aligned on text baseline"))
            .add("growx, span 2, wrap", UI.label("fields resize horizontally with the panel!"))
            .add(UI.label("Label 3"))
            .add("w 230!, span, wrap", UI.textField("medium space above"))
            .add(UI.label("Label 4"))
            .add("w 230!, span, wrap", UI.textField("big space above"))
            .add("span, wrap", UI.label("label after another big space"))
        )
        .add("aligny top, growx, span, wrap",
            UI.panel("insets 0, fill")
            .add("span, wrap", UI.radioButton("radio button 1")
                .onClick( it -> {
                    it.getComponent().setSelected(true);
                    it.getSiblingsOfType(JRadioButton.class).forEach( s -> s.setSelected(false) );
                })
                .onChange( it -> {
                    it.find(JLabel.class, "indent-label-1").ifPresent( l -> l.setEnabled(it.getComponent().isSelected()) );
                    it.find(JCheckBox.class, "indent-checkbox-1").ifPresent( c -> c.setEnabled(it.getComponent().isSelected()) );
                })
            )
            .add("gapx "+INDENT+" 0, span, wrap", UI.label("indented one step (because depends on radio button, inactive if rb not selected))").id("indent-label-1"))
            .add("gapx "+INDENT+" 0, span, wrap", UI.checkBox("indented checkbox").id("indent-checkbox-1"))
            .add("span, wrap", UI.radioButton("radio button 2 (medium space above)")
               .onClick( it -> {
                   it.getComponent().setSelected(true);
                   it.getSiblingsOfType(JRadioButton.class).forEach( s -> s.setSelected(false) );
               })
               .onChange( it ->
                  it.find(JTextField.class, "indent-field-2").ifPresent(f -> f.setEnabled(it.getComponent().isSelected()) )
               )
            )
            .add("gapx "+INDENT+" 0, span, wrap", UI.textField("jTextField6 (indented)").id("indent-field-2"))
            .add("span, wrap", UI.radioButton("radio button 3 (medium space above)")
                .onClick( it -> {
                    it.getComponent().setSelected(true);
                    it.getSiblingsOfType(JRadioButton.class).forEach( s -> s.setSelected(false) );
                })
                .onChange( it ->
                    it.find(JTextField.class, "indent-field-3").ifPresent(f -> f.setEnabled(it.getComponent().isSelected()) )
                )
            )
            .add("span, wrap",
                UI.panel("fill, insets 0 "+INDENT+" 0 0")
                .add("span", UI.textField("jTextField7 (indented)").id("indent-field-3"))
                .add("span", UI.checkBox("indent, because this depends on JRadioButton3"))
                .add("gapx "+INDENT+" 0, span, wrap", UI.textField("indent 2 levels (depends on checkbox)"))
            )
            .add("growx, span, wrap",
               UI.panel("", "[]"+S+"[]"+M+"[]")
               .add(UI.button("jButton1"), UI.button("jButton2"), UI.button("jButton3"))
            )
        );
    }

    // Use this to test the UI!
    public static void main(String... args) {
        new UI.TestWindow(JFrame::new,new Example4()).getFrame().setSize(new Dimension(500, 550));
    }

}
