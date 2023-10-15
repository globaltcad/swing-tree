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
            .add("grow, span", table(ListData.ROW_MAJOR, ()->[["A", "B", "C"], ["a", "b", "c"]]).id("RM"))
            .add("grow, span", separator())
            .add("grow, span", label("Column Major"))
            .add("grow, span", table(ListData.COLUMN_MAJOR, ()->[["A", "B", "C"], ["a", "b", "c"]]).id("CM"))
        )
        .add("grow", separator(Align.VERTICAL))
        .add("grow",
            panel("fill")
            .add("grow, span", label("Row Major 2"))
            .add("grow, span",
                table(
                   tableModel()
                   .colCount( () -> data[0].size() ).rowCount( () -> data.size() )
                   .getsEntryAt((col, row) -> data[col][row] )
                )
                .id("RM2")
            )
            .add("grow, span", separator())
            .add("grow, span", label("Column Major 2"))
            .add("grow, span",
                table(
                   tableModel()
                   .colCount( () -> data.size() )
                   .rowCount( () -> data[0].size() )
                   .getsEntryAt((col, row) -> data[row][col] )
                )
                .id("CM2")
            )
        )
        .get(Panel)
    }

    // Use this to test the UI!
    static void main(String... args) { UI.show(create()) }

}
