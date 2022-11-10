package com.globaltcad.swingtree.common

import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.utility.Utility
import spock.lang.Specification

import javax.swing.*

class UI_Query_Spec extends Specification
{
    def 'We can query the swing tree within an action lambda.'()
    {
        given :
            var root =
            UI.panel("fill, insets 3", "[grow][shrink]")
            .add("grow",
                UI.splitPane(UI.Split.HORIZONTAL)
                .withDividerAt(50)
                .add(
                    UI.scrollPane()
                    .add(
                        UI.tabbedPane()
                        .add(UI.tab("1").add(UI.panel("fill").add("grow", UI.button("A").id("B1"))))
                        .add(UI.tab("2").add(UI.panel("fill").add("grow", UI.button("B").id("B2"))))
                        .add(UI.tab("3").add(
                            UI.panel("fill").add("grow",
                                UI.button("C").id("B3")
                                .onClick( it -> { // The delegate lets us query the tree!
                                    it.find(JButton, "B2").ifPresent(b -> b.setText("I got hacked by B3"))
                                    it.find(JTextArea, "TA1").ifPresent(a -> a.setText("I got hacked by B3"))
                                    it.find(JTextField, "TF1").ifPresent(a -> a.setText("I got hacked by B3"))
                                })
                            )
                        ))
                    )
                )
                .add(
                    UI.scrollPane()
                    .add(
                        UI.panel("fill")
                        .add("grow,  wrap",
                            UI.scrollPane().add( UI.textArea("I am a text area!").id("TA1") )
                        )
                        .add("shrinky, growx",
                            UI.textField("I am a text field").id("TF1")
                        )
                    )
                )
            )
            .get(JPanel)

        expect :
            new Utility.Query(root).find(JButton, "B1").isPresent()
            new Utility.Query(root).find(JButton, "B2").isPresent()
            new Utility.Query(root).find(JButton, "B3").isPresent()
            new Utility.Query(root).find(JTextArea, "TA1").isPresent()
            new Utility.Query(root).find(JTextField, "TF1").isPresent()

        when :
            new Utility.Query(root).find(JButton, "B3").get().doClick()

        then :
            new Utility.Query(root).find(JButton, "B1").get().text == "A"
            new Utility.Query(root).find(JButton, "B2").get().text == "I got hacked by B3"
            new Utility.Query(root).find(JButton, "B3").get().text == "C"
            new Utility.Query(root).find(JTextArea, "TA1").get().text == "I got hacked by B3"
            new Utility.Query(root).find(JTextField, "TF1").get().text == "I got hacked by B3"
    }



}
