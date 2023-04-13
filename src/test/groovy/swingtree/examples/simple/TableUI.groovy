package swingtree.examples.simple

import swingtree.UI

import static swingtree.UI.*

class TableUI
{
    static Panel create()
    {
        var data = [["X", "Y"], ["1", "2"], ["3", "4"]]

        panel("fill")
        .add("grow",
            panel("fill")
            .add("grow, span", label("Row Major"))
            .add("grow, span", table(ListData.ROW_MAJOR, {[["A", "B", "C"], ["a", "b", "c"]]}).id("RM"))
            .add("grow, span", separator())
            .add("grow, span", label("Column Major"))
            .add("grow, span", table(ListData.COLUMN_MAJOR, {[["A", "B", "C"], ["a", "b", "c"]]}).id("CM"))
        )
        .add("grow", separator(Align.VERTICAL))
        .add("grow",
            panel("fill")
            .add("grow, span", label("Row Major 2"))
            .add("grow, span",
                table(tableModel()
                   .onColCount({data[0].size()}).onRowCount({data.size()})
                   .onGet({col, row -> data[col][row]})
                )
                .id("RM2")
            )
            .add("grow, span", separator())
            .add("grow, span", label("Column Major 2"))
            .add("grow, span",
                table(tableModel()
                   .onColCount({data.size()}).onRowCount({data[0].size()})
                   .onGet({col, row -> data[row][col]})
                )
                .id("CM2")
            )
        )
        .get(Panel)
    }

    // Use this to test the UI!
    static void main(String... args) { UI.show(create()) }

}
