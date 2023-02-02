package swingtree.renderer

import jdk.internal.event.Event
import swingtree.Render
import swingtree.UI
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

import javax.swing.JLabel
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
                        .onRowCount({3})
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

    def 'We can pass an Event to the table model to trigger updates.'()
    {
        given : 'We have an update event and some data.'
            var data = [1, 2, 3, 4]
            var update = sprouts.Event.of()
        and : 'We create a table with a lambda based table model.'
            var ui =
                    UI.table().withModel(
                        UI.tableModel()
                        .onColName( {["X", "Y", "Z"][it]})
                        .onColCount({3})
                        .onRowCount({data.size()})
                        .onGet({r,c ->data[r]})
                        .updateOn(update)
                    )

        expect : 'The table has 4 rows and 3 columns.'
            ui.get(JTable).rowCount == 4
            ui.get(JTable).columnCount == 3
        and : 'The table has the correct data.'
            ui.get(JTable).getValueAt(0, 0) == 1
            ui.get(JTable).getValueAt(1, 0) == 2
            ui.get(JTable).getValueAt(2, 0) == 3
            ui.get(JTable).getValueAt(3, 0) == 4
        when : 'We update the data.'
            data = [5, 6, 7, 8]
            update.fire()
        then : 'The table has the correct data.'
            ui.get(JTable).getValueAt(0, 0) == 5
            ui.get(JTable).getValueAt(1, 0) == 6
            ui.get(JTable).getValueAt(2, 0) == 7
            ui.get(JTable).getValueAt(3, 0) == 8
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

    def 'You can render the cells of a table as text by using the "asText" method.'()
    {
        reportInfo """
            Usually you want to render the cells of a table as text, you don't have to
            define a renderer component for that. Simply use the "asText" method to 
            define how a cell should be converted to a string, which will be rendered for you.
            Also, note that you can actually pass a simple list of lists provider to the table factory method
            and it will create a table model for you.
            In the table defined below we create a list data based row major table. 
        """
        given : """
                A simple table UI with a nested list based data table model
                and a default renderer used for all columns.
            """
            var ui =
                        UI.table(UI.ListData.ROW_MAJOR_EDITABLE, { [[1, 2, 3], [7, 8, 9]] })
                        .withRenderer(
                            UI.renderTable()
                            .when(Integer).asText( cell -> cell.valueAsString().orElse("")+"!" )
                        )
        when : 'We access the resulting TableCellRenderer instance from the UI.'
            var found = ui.get(JTable)
                                    .getDefaultRenderer(Object)
        and : 'Finally we access the component from the renderer (which is responsible for the actual rendering).'
            var component = found.getTableCellRendererComponent(null, 1, false, false, 0, 0)

        then : 'The cell is rendered as text (based on a JLabel).'
            component instanceof JLabel
            component.text == "1!"
    }

    def 'We need to attach a update Event to our table when the table data is list based and its data changes.'()
    {
        reportInfo """
            Use the Event class in your view model to define an event you can fire when you modify the data
            of your table. The event will be used to update the table UI  if you register it with the table UI.
        """
        given : 'A simple event and some data.'
            var event = sprouts.Event.of()
            var data = [[1, 2, 3], [7, 8, 9]]

        and : 'A simple table UI with a nested list based data table model.'
            var ui =
                        UI.table(UI.ListData.ROW_MAJOR_EDITABLE, { data })
                        .updateTableOn(event)
        when : 'We fire the event.'
            event.fire()
        then : 'The table UI is updated.'
            ui.component.getRowCount() == 2
            ui.component.getValueAt(0, 0) == 1
            ui.component.getValueAt(0, 1) == 2
            ui.component.getValueAt(0, 2) == 3
            ui.component.getValueAt(1, 0) == 7
            ui.component.getValueAt(1, 1) == 8
            ui.component.getValueAt(1, 2) == 9
    }

}
