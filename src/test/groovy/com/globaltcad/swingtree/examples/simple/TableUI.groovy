package com.globaltcad.swingtree.examples.simple

import com.globaltcad.swingtree.UI

import javax.swing.*
import java.awt.*

class TableUI
{
    static JPanel create() {
        UI.panel("fill")
        .add("grow, span", UI.label("Row Major"))
        .add("grow, span", UI.table(UI.TableData.ROW_MAJOR, {[["A", "B", "C"],["a", "b", "c"]]}).id("RM"))
        .add("grow, span", UI.separator())
        .add("grow, span", UI.label("Column Major"))
        .add("grow, span", UI.table(UI.TableData.COLUMN_MAJOR, {[["A", "B", "C"],["a", "b", "c"]]}).id("CM"))
        .get(JPanel)
    }

    // Use this to test the UI!
    static void main(String... args) {
        new UI.TestWindow({new JFrame()}, create()).getFrame().setSize(new Dimension(150, 225))
    }

}
