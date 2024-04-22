package examples.comparisons.comparison1;


import swingtree.UI;
import utility.Utility;

import javax.swing.JPanel;

import static swingtree.UI.*;

public class MadeWithSwingTree extends Panel
{
    private final static String S = "6", SM = "2", M = "12", L = "18";
    private final static String INDENT = "22";

    public MadeWithSwingTree()
    {
        Utility.setLaF(Utility.LaF.NIMBUS);
        of(this).withLayout("fill, ins 0")
        .add("grow",
            panel("fillx, gap rel 0, insets "+M+"")
            .add("aligny top, growx, span, wrap",
                panel("insets 0, fillx", "[shrink]"+M+"[grow]"+L+"[shrink][grow]", "[]"+S+"[]"+M+"[]"+L+"[]"+L+"[]0")
                .add(label("Label 1"))
                .add("growx, pushy", textField("aligned on text baseline"))
                .add("growx", label("left: big space"))
                .add("growx, pushy, wrap", textField("same width as left field"))
                .add(label("Label 2"))
                .add("growx, pushy", textField("aligned on text baseline"))
                .add("growx, span 2, wrap", label("fields resize horizontally with the panel!"))
                .add(label("Label 3"))
                .add("growx, gapx 0 235, span, wrap", textField("medium space above"))
                .add(label("Label 4"))
                .add("growx, gapx 0 235, span, wrap", textField("big space above"))
                .add("span, wrap", label("Label after another big space"))
            )
            .add("aligny top, growx, span, wrap",
                panel("insets 0, fill, gap rel 2")
                .add("span, wrap", radioButton("radio button 1")
                    .onClick( it -> {
                        it.get().setSelected(true);
                        it.getSiblingsOfType(RadioButton.class).forEach(s -> s.setSelected(false) );
                    })
                    .onChange( it -> {
                        it.find(Label.class, "indent-label-1").ifPresent( l -> l.setEnabled(it.get().isSelected()) );
                        it.find(CheckBox.class, "indent-checkbox-1").ifPresent( c -> c.setEnabled(it.get().isSelected()) );
                    })
                )
                .add("gapx "+INDENT+" 0, span, wrap", label("indented one step (because depends on radio button, inactive if rb not selected))").id("indent-label-1"))
                .add("gapx "+INDENT+" 0, gapy 0 "+SM+", span, wrap", checkBox("indented checkbox").id("indent-checkbox-1"))
                .add("span, wrap", radioButton("radio button 2 (medium space above)")
                   .onClick( it -> {
                       it.get().setSelected(true);
                       it.getSiblingsOfType(RadioButton.class).forEach( s -> s.setSelected(false) );
                   })
                   .onChange( it ->
                      it.find(TextField.class, "indent-field-2").ifPresent(f -> f.setEnabled(it.get().isSelected()) )
                   )
                )
                .add("growx, gapx "+INDENT+" 260, gapy 0 "+S+", span, wrap", textField("jTextField6 (indented)").id("indent-field-2"))
                .add("span, wrap", radioButton("radio button 3 (medium space above)")
                    .onClick( it -> {
                        it.get().setSelected(true);
                        it.getSiblingsOfType(RadioButton.class).forEach( s -> s.setSelected(false) );
                    })
                    .onChange( it ->
                        it.find(TextField.class, "indent-field-3").ifPresent(f -> f.setEnabled(it.get().isSelected()) )
                    )
                )
                .add("growx, span, wrap",
                    panel("fill, insets 0 "+INDENT+" 0 0, gap rel 0")
                    .add("span, growx, gapx 0 260", textField("jTextField7 (indented)").id("indent-field-3"))
                    .add("span", checkBox("indented, because this depends on JRadioButton3"))
                    .add("gapx "+INDENT+" 0, gapy 0 "+SM+", span, wrap", textField("indent 2 levels (depends on checkbox)"))
                )
                .add("growx, gapy "+M+" 0, span, wrap",
                   panel("insets 0", "[]"+S+"[]"+M+"[]")
                   .add("width 142!",
                        button("jButton1"), button("jButton2"), button("jButton3 long")
                   )
                )
                .add("gapy "+S+" 0, span, wrap", label("Different distance (small / medium) between the buttons."))
                .add("gapy "+S+" 0, span, wrap", label("Buttons have same size despite different text length"))
            )
            .get(JPanel.class)
        );
    }

    // Use this to test the UI!
    public static void main(String... args) { UI.show(new MadeWithSwingTree()); }

}
