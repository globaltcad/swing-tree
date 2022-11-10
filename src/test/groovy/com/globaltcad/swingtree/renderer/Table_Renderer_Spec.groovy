package com.globaltcad.swingtree.renderer

import com.globaltcad.swingtree.Render
import com.globaltcad.swingtree.UI
import spock.lang.Specification

import javax.swing.JTable

class Table_Renderer_Spec extends Specification
{

    def 'We can attach a simple table cell renderer to a JTable in just a few lines of code.'()
    {
        given :
            var node =
                    UI.table().withModel(
                        UI.tableModel()
                        .colNames("A", "B").onColCount({2}).onGet({"O"})
                    )
        and :
            var render = Mock(Render.Cell.Interpreter)

        when :
            node.withRendererForColumn("A", UI.renderTable().when(String).as(render) )
        and :
            var found = node.get(JTable).getColumn("A").cellRenderer
        and :
            found.getTableCellRendererComponent(null, "1", false, false, 0, 0)

        then :
            1 * render.interpret(_)
    }


    def 'We can create a simple table cell renderer through a UI factory method.'()
    {
        given :
            var node = UI.table(UI.ListData.ROW_MAJOR_EDITABLE, { [["a", "b", "c"], ["1", "2", "3"]] })
        and :
            var render = Mock(Render.Cell.Interpreter)

        expect :
            node.component.getColumnName(0) == "A" // default column names
            node.component.getColumnName(1) == "B"
            node.component.getRowCount() == 2
            node.component.getValueAt(0, 0) == "a"
            node.component.getValueAt(0, 1) == "b"

        when :
            node.withRendererForColumn(1, UI.renderTable().when(String).as(render) )
        and :
            var found = node.get(JTable)
                    .columnModel
                    .getColumn(1)
                    .cellRenderer
        and :
            found.getTableCellRendererComponent(null, "1", false, false, 0, 0)

        then :
            1 * render.interpret(_)
    }

    def 'We can create a simple column major table cell renderer through a UI factory method.'()
    {
        given :
            var node = UI.table(UI.ListData.COLUMN_MAJOR_EDITABLE, { [["a", "b", "c"], ["1", "2", "3"]] })
        and :
            var render = Mock(Render.Cell.Interpreter)

        expect :
            node.component.getColumnName(0) == "A" // default column names
            node.component.getColumnName(1) == "B"
            node.component.getRowCount() == 3
            node.component.getColumnCount() == 2
            node.component.getValueAt(0, 0) == "a"
            node.component.getValueAt(0, 1) == "1"

        when :
            node.withRendererForColumn(1, UI.renderTable().when(String).as(render) )
        and :
            var found = node.get(JTable)
                    .columnModel
                    .getColumn(1)
                    .cellRenderer
        and :
            found.getTableCellRendererComponent(null, "1", false, false, 0, 0)

        then :
            1 * render.interpret(_)
    }

    def 'A map can be used as a data source for tables.'()
    {
        given :
            var node =
                    UI.table(UI.MapData.EDITABLE, { ["X":["a", "b", "c"], "Y":["1", "2", "3"]] })
        and :
            var render = Mock(Render.Cell.Interpreter)

        expect :
            node.component.getColumnName(0) == "X"
            node.component.getColumnName(1) == "Y"
            node.component.getRowCount() == 3
            node.component.getColumnCount() == 2
            node.component.getValueAt(0, 0) == "a"
            node.component.getValueAt(0, 1) == "1"

        when :
            node.withRendererForColumn(1, UI.renderTable().when(String).as(render) )
        and :
            var found = node.get(JTable)
                        .columnModel
                        .getColumn(1)
                        .cellRenderer
        and :
            found.getTableCellRendererComponent(null, "1", false, false, 0, 0)

        then :
            1 * render.interpret(_)
    }

}
