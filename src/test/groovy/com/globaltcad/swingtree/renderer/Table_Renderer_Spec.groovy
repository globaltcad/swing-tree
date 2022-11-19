package com.globaltcad.swingtree.renderer

import com.globaltcad.swingtree.Render
import com.globaltcad.swingtree.UI
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

import javax.swing.JTable

@Title("Rendering Table Cells")
@Narrative("""

    Swing-Tree exposes a user friendly API for rendering table cells.
    For simple table UIs none of this is necessary, but it
    is important when you want to populate your table with more complex data.
    The renderer is a simple function that takes a value and returns a UI node.
    The UI node is then rendered as a table cell.
    
""")
class Table_Renderer_Spec extends Specification
{

    def 'We can attach a simple table cell renderer to a JTable in just a few lines of code.'()
    {
        given : 'We create a table with a lambda based table model.'
            var ui =
                    UI.table().withModel(
                        UI.tableModel()
                        .colNames("A", "B")
                        .onColCount({2})
                        .onGet({"O"})
                    )
        and : """
                A mocked cell interpreter which interprets the state of the table cell
                and then defines how it should be rendered (by setting a UI component).
            """
            var render = Mock(Render.Cell.Interpreter)

        when : 'We attach the interpreter to a table renderer which we then attach to the table.'
            ui.withRendererForColumn("A", UI.renderTable().when(String).as(render) )
        and : 'We we access the resulting TableCellRenderer instance from the UI.'
            var found = ui.get(JTable).getColumn("A").cellRenderer
        and : 'Finally we access the component from the renderer (which is responsible for the actual rendering).'
            found.getTableCellRendererComponent(null, "1", false, false, 0, 0)

        then : 'The mocked cell interpreter is called.'
            1 * render.interpret(_)
    }


    def 'We can create a simple table cell renderer through a UI factory method.'()
    {
        reportInfo """
            Note that you can actually pass a simple list of lists provider to the table factory method
            and it will create a table model for you.
            In the table defined below we create a list data based row major table. 
        """
        given : 'A simple table UI with a nested list based data table model.'
            var ui = UI.table(UI.ListData.ROW_MAJOR_EDITABLE, { [["a", "b", "c"], ["1", "2", "3"]] })
        and : """
                A mocked cell interpreter which interprets the state of the table cell
                and then defines how it should be rendered (by setting a UI component).
            """
            var render = Mock(Render.Cell.Interpreter)

        expect : 'The table UI has the following state:'
            ui.component.getColumnName(0) == "A" // default column names
            ui.component.getColumnName(1) == "B"
            ui.component.getRowCount() == 2
            ui.component.getValueAt(0, 0) == "a"
            ui.component.getValueAt(0, 1) == "b"

        when : 'We build a table renderer for strings and pass our mocked renderer to it.'
            ui.withRendererForColumn(1, UI.renderTable().when(String).as(render) )
        and : 'We we access the resulting TableCellRenderer instance from the UI.'
            var found = ui.get(JTable)
                    .columnModel
                    .getColumn(1)
                    .cellRenderer
        and : 'Finally we access the component from the renderer (which is responsible for the actual rendering).'
            found.getTableCellRendererComponent(null, "1", false, false, 0, 0)

        then : 'The mocked cell interpreter is called.'
            1 * render.interpret(_)
    }

    def 'We can create a simple column major table cell renderer through a UI factory method.'()
    {
        reportInfo """
            Note that you can actually pass a simple list of lists provider to the table factory method
            and it will create a table model for you.
            In the table defined below we create a list data based column major table. 
        """
        given :
            var ui = UI.table(UI.ListData.COLUMN_MAJOR_EDITABLE, { [["a", "b", "c"], ["1", "2", "3"]] })
        and : """
                A mocked cell interpreter which interprets the state of the table cell
                and then defines how it should be rendered (by setting a UI component).
            """
            var render = Mock(Render.Cell.Interpreter)

        expect : 'The table UI has the following state:'
            ui.component.getColumnName(0) == "A" // default column names
            ui.component.getColumnName(1) == "B"
            ui.component.getRowCount() == 3
            ui.component.getColumnCount() == 2
            ui.component.getValueAt(0, 0) == "a"
            ui.component.getValueAt(0, 1) == "1"

        when : 'We build a table renderer for strings and pass our mocked renderer to it.'
            ui.withRendererForColumn(1, UI.renderTable().when(String).as(render) )
        and :
            var found = ui.get(JTable)
                    .columnModel
                    .getColumn(1)
                    .cellRenderer
        and : 'Finally we access the component from the renderer (which is responsible for the actual rendering).'
            found.getTableCellRendererComponent(null, "1", false, false, 0, 0)

        then : 'The mocked cell interpreter is called.'
            1 * render.interpret(_)
    }

    def 'A map can be used as a data source for tables.'()
    {
        reportInfo """
            Note that you can actually pass a provider lambda for a map of header names to column lists 
            to the table factory method and it will create a table model for you, which is based on the provided 
            map provider. This will always result in a column major table. 
        """
        given : 'A simple table UI with a map based data table model.'
            var ui =
                    UI.table(UI.MapData.EDITABLE, { ["X":["a", "b", "c"], "Y":["1", "2", "3"]] })
        and : """
                A mocked cell interpreter which interprets the state of the table cell
                and then defines how it should be rendered (by setting a UI component).
            """
            var render = Mock(Render.Cell.Interpreter)

        expect : 'The table UI has the following state:'
            ui.component.getColumnName(0) == "X"
            ui.component.getColumnName(1) == "Y"
            ui.component.getRowCount() == 3
            ui.component.getColumnCount() == 2
            ui.component.getValueAt(0, 0) == "a"
            ui.component.getValueAt(0, 1) == "1"

        when : 'We build a table renderer for strings and pass our mocked renderer to it.'
            ui.withRendererForColumn(1, UI.renderTable().when(String).as(render) )
        and : 'We we access the resulting TableCellRenderer instance from the UI.'
            var found = ui.get(JTable)
                        .columnModel
                        .getColumn(1)
                        .cellRenderer
        and : 'Finally we access the component from the renderer (which is responsible for the actual rendering).'
            found.getTableCellRendererComponent(null, "1", false, false, 0, 0)

        then : 'The mocked cell interpreter is called.'
            1 * render.interpret(_)
    }

}
