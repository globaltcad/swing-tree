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
            var node = UI.table(UI.TableData.ROW_MAJOR_EDITABLE, { [["a", "b", "c"], ["1", "2", "3"]] })
        and :
            var render = Mock(Render.Cell.Interpreter)

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
