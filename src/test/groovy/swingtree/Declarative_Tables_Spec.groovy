package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Event
import swingtree.threading.EventProcessor

import javax.swing.*
import java.awt.*

@Title("Creating Tables")
@Narrative("""

    Swing-Tree exposes a user friendly API for defining tables in a declarative manner.
    You don't necessarily have to implement your own table model, because the Swing-Tree
    API allows you to supply simple collection based data as a data source for your table.
    
""")
@Subject([UIForTable])
class Declarative_Tables_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
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
        and : 'We actually build the component:'
            var table = ui.get(JTable)

        expect : 'The table UI has the following state:'
            table.getColumnName(0) == "X"
            table.getColumnName(1) == "Y"
            table.getRowCount() == 3
            table.getColumnCount() == 2
            table.getValueAt(0, 0) == "a"
            table.getValueAt(0, 1) == "1"
    }

    def 'We can create a column major table based on a list of lists as a data model.'()
    {
        reportInfo """
            A simple list of lists provider can be supplied to the table factory method
            and it will be enough to create a simple table model for you.
            In the table defined below we create a list data based column major table. 
        """
        given :
            var ui = UI.table(UI.ListData.COLUMN_MAJOR_EDITABLE, { [["a", "b", "c"], ["x", "y", "z"]] })
        and : 'We actually build the component:'
            var table = ui.get(JTable)

        expect : 'The table UI has the following state:'
            table.getColumnName(0) == "A" // default column names
            table.getColumnName(1) == "B"
            table.getRowCount() == 3
            table.getColumnCount() == 2
            table.getValueAt(0, 0) == "a"
            table.getValueAt(0, 1) == "x"
            table.getValueAt(1, 0) == "b"
            table.getValueAt(1, 1) == "y"
            table.getValueAt(2, 0) == "c"
            table.getValueAt(2, 1) == "z"
    }

    def 'We can pass an `Event` to the table model to trigger updates.'()
    {
        reportInfo """
            Note that in this example we use a lambda based table model
            through the `UI.tableModel()` factory method. This is a convenient
            way to create a table model without having to implement the TableModel
            interface yourself.
        """

        given : 'We have an update event and some data.'
            var data = [1, 2, 3, 4]
            var update = Event.create()
        and : 'We create a table with a lambda based table model.'
            var ui =
                    UI.table().withModel( m -> m
                        .colName( col -> ["X", "Y", "Z"].get(col) )
                        .colCount( () -> 3 )
                        .rowCount( () -> data.size() )
                        .getsEntryAt( (r, c) -> data[r] )
                        .updateOn(update)
                    )
        and : 'We actually build the component:'
            var table = ui.get(JTable)

        expect : 'The table has 4 rows and 3 columns.'
            table.rowCount == 4
            table.columnCount == 3
        and : 'The table has the correct data.'
            table.getValueAt(0, 0) == 1
            table.getValueAt(1, 0) == 2
            table.getValueAt(2, 0) == 3
            table.getValueAt(3, 0) == 4
        when : 'We update the data.'
            data = [5, 6, 7, 8]
            update.fire()
            UI.sync() // sync with the EDT
        then : 'The table has the correct data.'
            table.getValueAt(0, 0) == 5
            table.getValueAt(1, 0) == 6
            table.getValueAt(2, 0) == 7
            table.getValueAt(3, 0) == 8
    }

    def 'We need to attach an update `Event` to our table when the table data is list based and its data changes.'()
    {
        reportInfo """
            Use the Event class in your view model to define an event you can fire when you modify the data
            of your table. The event will be used to update the table UI  if you register it with the table UI.
        """
        given : 'A simple event and some data.'
            var event = Event.create()
            var data = [[1, 2, 3], [7, 8, 9]]

        and : 'A simple table UI with a nested list based data table model.'
            var ui =
                        UI.table(UI.ListData.ROW_MAJOR_EDITABLE, { data })
                        .updateTableOn(event)
        and : 'We actually build the component:'
            var table = ui.get(JTable)
        when : 'We fire the event.'
            event.fire()
        then : 'The table UI is updated.'
            table.getRowCount() == 2
            table.getValueAt(0, 0) == 1
            table.getValueAt(0, 1) == 2
            table.getValueAt(0, 2) == 3
            table.getValueAt(1, 0) == 7
            table.getValueAt(1, 1) == 8
            table.getValueAt(1, 2) == 9
    }

    def 'Configure which cells are editable or not as part of the table model declaration.'()
    {
        reportInfo """
            The data model builder API allows you to define a lambda based table model
            where you can also specify if a cell is editable or not
            based on the row and column index.
            
            In the example below we define a table model where the cells are editable
            based on a flag and the condition `(r==1 || c==0)`.
        """
        given : 'We have some row major matrix like data.'
            var data = [
                            [1, 2, 3],
                            [4, 5, 6],
                        ]
        and : 'A flag for controlling if we allow editing.'
            var editable = false
        and : 'A table with a lambda based table model where the data rows are the columns.'
            var ui =
                    UI.table().withModel( m -> m
                        .colName( col -> ["A", "B"].get(col) )
                        .colCount( () -> 2 )
                        .rowCount( () -> 3 )
                        .getsEntryAt( (r, c) -> data[c][r] )
                        .isEditableIf((r, c) -> editable && (r==1 || c==0))
                    )
        and : 'We build the table.'
            var table = ui.get(JTable)
        expect : 'The table has the right dimensions:'
            table.rowCount == 3
            table.columnCount == 2

        and : """
            Initially, none of the simulated user edits through `editCellAt(int row, int column, EventObject e)`
            will be successful, because the table is not editable.
        """
            !UI.runAndGet({table.editCellAt(0, 0, new EventObject(table))})
            !UI.runAndGet({table.editCellAt(0, 1, new EventObject(table))})
            !UI.runAndGet({table.editCellAt(1, 0, new EventObject(table))})
            !UI.runAndGet({table.editCellAt(1, 1, new EventObject(table))})
            !UI.runAndGet({table.editCellAt(2, 0, new EventObject(table))})
            !UI.runAndGet({table.editCellAt(2, 1, new EventObject(table))})
        when : 'We allow editing.'
            editable = true
        then : 'The table is editable for every cell where `(r==1 || c==0)` yields true.'
            UI.runAndGet({table.editCellAt(0, 0, new EventObject(table))}) == true
            UI.runAndGet({table.editCellAt(0, 1, new EventObject(table))}) == false
            UI.runAndGet({table.editCellAt(1, 0, new EventObject(table))}) == true
            UI.runAndGet({table.editCellAt(1, 1, new EventObject(table))}) == true
            UI.runAndGet({table.editCellAt(2, 0, new EventObject(table))}) == true
            UI.runAndGet({table.editCellAt(2, 1, new EventObject(table))}) == false
    }

    def 'Use `withCell(Configurator)` to configure both a renderer and editor for your table.'()
    {
        reportInfo """
            The `withCell(Configurator)` method constitutes a useful API point
            which exposes you to a fluent API for configuring how a particular cell
            should be displayed.
            
            The `Configurator` lambda passed to the `withCell` method receives
            a delegate object of a particular cell in the table.
            You may update and return this cell with a view component
            used for either rendering, editing or both.
            
            So this may look like this:
            ```java
                .withCell( it -> it
                    .view( comp -> comp
                        .orGetUiIf(cell.isEditing(), {UI.textField().withBackground(Color.MAGENTA)})
                        .orGetUiIf(!cell.isEditing(), {UI.label("")})
                        .updateIf(JLabel.class, label -> {
                            label.text = "Day: " + cell.valueAsString().orElse("")
                            return label
                        })
                    )
                )
            ```
            Here you can see that the `Configurator` lambda receives a `cell` object
            which is a delegate object of a particular cell in the combo box.
            The view of this cell is updated with a text field or a label depending
            on whether the cell is currently being edited or not.
        """
        given : 'We have some data.'
            var data = [1, 2, 3, 4]
        and : 'A table with a lambda based table model.'
            var ui =
                    UI.table().withModel( m -> m
                        .colName( col -> ["X", "Y", "Z"].get(col) )
                        .colCount( () -> 3 )
                        .rowCount( () -> data.size() )
                        .getsEntryAt( (r, c) -> data[r] )
                        .isEditableIf((r, c) -> true)
                    )
                    .withCell(cell -> cell
                        .updateView( comp -> comp
                            .orGetUi({UI.textField().withBackground(Color.MAGENTA)})
                            .updateIf(JTextField.class, tf -> {
                                tf.text = cell.entryAsString()
                                tf.foreground = cell.isSelected() ? Color.RED : Color.WHITE
                                return tf
                            })
                        )
                    )
        and : 'We build the table.'
            var table = ui.get(JTable)
        and : 'We get the renderer and editor supplier.'
            var renderer = table.getDefaultRenderer(Object)
            var editor = table.getDefaultEditor(Object)
        expect :
            renderer != null
            editor != null
        and : 'Initially, the editor is not setup.'
            editor.getEditorComponent() == null

        when : 'We simulate a user edit through `editCellAt(int row, int column, EventObject e)`.'
            boolean success = UI.runAndGet({table.editCellAt(0, 0, new EventObject(table))})
        then : 'The editor is a text field with a magenta background.'
            success == true
            editor.getEditorComponent() instanceof JTextField
            editor.getEditorComponent().background == Color.MAGENTA
    }

}
