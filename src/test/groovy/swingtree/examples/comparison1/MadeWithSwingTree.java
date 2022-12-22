package swingtree.examples.comparison1;


import swingtree.UI;
import swingtree.Utility;

import javax.swing.*;
import java.awt.*;

public class MadeWithSwingTree extends JPanel
{
    private final static String S = "6", SM = "2", M = "12", L = "18";
    private final static String INDENT = "22";

    public MadeWithSwingTree()
    {
        Utility.setLaF(Utility.LaF.NIMBUS);
        add(UI.panel("fillx, gap rel 0, insets "+M+"")
            .add("aligny top, growx, span, wrap",
                UI.panel("insets 0, fillx", "[shrink]"+M+"[grow]"+L+"[shrink][grow]", "[]"+S+"[]"+M+"[]"+L+"[]"+L+"[]0")
                .add(UI.label("Label 1"))
                .add("growx, pushy", UI.textField("aligned on text baseline"))
                .add("growx", UI.label("left: big space"))
                .add("growx, pushy, wrap", UI.textField("same width as left field"))
                .add(UI.label("Label 2"))
                .add("growx, pushy", UI.textField("aligned on text baseline"))
                .add("growx, span 2, wrap", UI.label("fields resize horizontally with the panel!"))
                .add(UI.label("Label 3"))
                .add("growx, gapx 0 235, span, wrap", UI.textField("medium space above"))
                .add(UI.label("Label 4"))
                .add("growx, gapx 0 235, span, wrap", UI.textField("big space above"))
                .add("span, wrap", UI.label("Label after another big space"))
            )
            .add("aligny top, growx, span, wrap",
                UI.panel("insets 0, fill, gap rel 2")
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
                .add("gapx "+INDENT+" 0, gapy 0 "+SM+", span, wrap", UI.checkBox("indented checkbox").id("indent-checkbox-1"))
                .add("span, wrap", UI.radioButton("radio button 2 (medium space above)")
                   .onClick( it -> {
                       it.getComponent().setSelected(true);
                       it.getSiblingsOfType(JRadioButton.class).forEach( s -> s.setSelected(false) );
                   })
                   .onChange( it ->
                      it.find(JTextField.class, "indent-field-2").ifPresent(f -> f.setEnabled(it.getComponent().isSelected()) )
                   )
                )
                .add("growx, gapx "+INDENT+" 260, gapy 0 "+S+", span, wrap", UI.textField("jTextField6 (indented)").id("indent-field-2"))
                .add("span, wrap", UI.radioButton("radio button 3 (medium space above)")
                    .onClick( it -> {
                        it.getComponent().setSelected(true);
                        it.getSiblingsOfType(JRadioButton.class).forEach( s -> s.setSelected(false) );
                    })
                    .onChange( it ->
                        it.find(JTextField.class, "indent-field-3").ifPresent(f -> f.setEnabled(it.getComponent().isSelected()) )
                    )
                )
                .add("growx, span, wrap",
                    UI.panel("fill, insets 0 "+INDENT+" 0 0, gap rel 0")
                    .add("span, growx, gapx 0 260", UI.textField("jTextField7 (indented)").id("indent-field-3"))
                    .add("span", UI.checkBox("indented, because this depends on JRadioButton3"))
                    .add("gapx "+INDENT+" 0, gapy 0 "+SM+", span, wrap", UI.textField("indent 2 levels (depends on checkbox)"))
                )
                .add("growx, gapy "+M+" 0, span, wrap",
                   UI.panel("insets 0", "[]"+S+"[]"+M+"[]")
                   .add("width 142!",
                        UI.button("jButton1"), UI.button("jButton2"), UI.button("jButton3 long")
                   )
                )
                .add("gapy "+S+" 0, span, wrap", UI.label("Different distance (small / medium) between the buttons."))
                .add("gapy "+S+" 0, span, wrap", UI.label("Buttons have same size despite different text length"))
            )
            .get(JPanel.class)
        );
    }

    // Use this to test the UI!
    public static void main(String... args) {
        new UI.TestWindow(JFrame::new,new MadeWithSwingTree()).getFrame().setSize(new Dimension(650, 550));
    }

}
