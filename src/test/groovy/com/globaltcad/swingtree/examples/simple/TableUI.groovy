package com.globaltcad.swingtree.examples.simple

import com.globaltcad.swingtree.UI

import javax.swing.*
import java.awt.*

class TableUI
{
    static JPanel create()
    {
        var data = [["X", "Y"], ["1", "2"], ["3", "4"]]

        UI.panel("fill")
        .add("grow",
            UI.panel("fill")
            .add("grow, span", UI.label("Row Major"))
            .add("grow, span", UI.table(UI.TableData.ROW_MAJOR, {[["A", "B", "C"],["a", "b", "c"]]}).id("RM"))
            .add("grow, span", UI.separator())
            .add("grow, span", UI.label("Column Major"))
            .add("grow, span", UI.table(UI.TableData.COLUMN_MAJOR, {[["A", "B", "C"],["a", "b", "c"]]}).id("CM"))
        )
        .add("grow", UI.separator(UI.Align.VERTICAL))
        .add("grow",
            UI.panel("fill")
            .add("grow, span", UI.label("Row Major 2"))
            .add("grow, span",
                UI.table(UI.tableModel()
                   .onColCount({data[0].size()}).onRowCount({data.size()})
                   .onGet({col, row -> data[col][row]})
                )
                .id("RM2")
            )
            .add("grow, span", UI.separator())
            .add("grow, span", UI.label("Column Major 2"))
            .add("grow, span",
                UI.table(UI.tableModel()
                   .onColCount({data.size()}).onRowCount({data[0].size()})
                   .onGet({col, row -> data[row][col]})
                )
                .id("CM2")
            )
        )
        .get(JPanel)
    }

    // Use this to test the UI!
    static void main(String... args) {
        new UI.TestWindow({new JFrame()}, create()).getFrame().setSize(new Dimension(350, 225))
    }

}
