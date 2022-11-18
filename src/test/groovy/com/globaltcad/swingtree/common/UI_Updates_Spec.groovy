package com.globaltcad.swingtree.common

import com.globaltcad.swingtree.UI
import com.globaltcad.swingtree.utility.Utility
import spock.lang.Specification

import javax.swing.*
import java.time.LocalDateTime

class UI_Updates_Spec extends Specification
{
    def 'We can register periodically called UI updates!'()
    {
        given :
            var ui =
                 UI.panel()
                .add(
                    UI.label("Label 1").id("L1")
                    .doUpdates(20,it -> {
                        it.component.text = LocalDateTime.now().toString()
                    })
                )

        when :
            Thread.sleep(200)

        then :
            new Utility.Query(ui.component).find(JLabel, "L1").isPresent()
        and :
            new Utility.Query(ui.component).find(JLabel, "L1").get().text != "Label 1"
    }


}
