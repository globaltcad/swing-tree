package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Event
import sprouts.Tuple
import sprouts.Val
import sprouts.Var
import swingtree.api.model.BasicTableModel
import swingtree.api.model.TableData
import swingtree.api.model.TableListDataSource
import swingtree.threading.EventProcessor

import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import java.awt.*

@Title("Creating Tables")
@Narrative("""

    Swing-Tree exposes a user friendly API for defining tables in a declarative manner.
    You don't necessarily have to implement your own table model, because the Swing-Tree
    API allows you to supply simple collection based data as a data source for your table.

    Note that the **recommended** way of modelling a table is the `TableData` value,
    which describes an entire table (its cells, column names, column classes, cell order
    and editability) as a single immutable value that you hold in a property and bind
    to a table.
    See the `Table_Data_Spec` for what that looks like, and read on here for the older,
    pull based data sources, which are still supported and still handy for a quick table
    over data you already hold somewhere else.

""")
@Subject([UIForTable, BasicTableModel])
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
            <p>
            Note that this data source is pull based: the table reads it whenever it feels
            like it, and it has no way of knowing when your map changed. Prefer a
            `TableData` value in a property (see the `Table_Data_Spec`) unless you really
            do have a map lying around which you would rather not copy.
        """
        given : 'A simple table UI with a map based data table model.'
            var ui =
                    UI.table(UI.Editability.EDITABLE, { ["X":["a", "b", "c"], "Y":["1", "2", "3"]] })
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
            var ui = UI.table(UI.CellOrder.COLUMN_MAJOR, UI.Editability.EDITABLE, { [["a", "b", "c"], ["x", "y", "z"]] })
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

    def 'A `List` based table can also be bound further down the chain, through `withModel(..)`.'()
    {
        reportInfo """
            Just like the property based bindings, the older pull based data sources come
            in a factory spelling (`UI.table(..)`) and a builder spelling
            (`UI.table().withModel(..)`). Reach for the builder one when you have already
            begun declaring a table and want to configure something about it before saying
            where its data comes from.

            The two argument spelling leaves the editability out, which reads as read only.
            And because a list is a *pull* based source, the table only looks at it when
            something tells it to, which is what `updateTableOn(..)` is for.
        """
        given : 'A mutable list of rows, and an event with which to announce changes to it.'
            var rows = [["Alice", "30"], ["Bob", "42"]]
            var update = Event.create()
        and : 'A table which configures itself first and only then binds the list:'
            var table =
                    UI.table()
                    .id("people")
                    .withModel(UI.CellOrder.ROW_MAJOR, { rows } as TableListDataSource)
                    .updateTableOn(update as Event)
                    .get(JTable)
        and : 'A listener recording every event the table model announces.'
            var events = []
            table.getModel().addTableModelListener({ TableModelEvent e -> events << e } as TableModelListener)

        expect : 'What we declared before the binding survived it.'
            table.getName() == "people"
        and : 'The table shows the two rows of the list.'
            table.getRowCount() == 2
            table.getColumnCount() == 2
            table.getValueAt(0, 0) == "Alice"
            table.getValueAt(1, 1) == "42"
        and : 'Its cells are read only, because we did not ask for anything else.'
            !table.isCellEditable(0, 0)

        when : 'We add a row to the list without telling the table about it...'
            rows.add(["Carol", "27"])
        then : '...the table stays quiet, so a table on screen would still be painting two rows.'
            events.isEmpty()

        when : 'We fire the update event...'
            update.fire()
            UI.sync()
        then : '...the table announces the change and reads the third row.'
            !events.isEmpty()
            table.getRowCount() == 3
            table.getValueAt(2, 0) == "Carol"
    }

    def 'The `withModel(UI.CellOrder, ..)` shorthand reads your list along whichever axis you name.'()
    {
        reportInfo """
            A list of lists is an ambiguous thing: the very same nesting can mean "a list
            of rows" or "a list of columns". The `UI.CellOrder` you pass says which of the
            two you meant, and the table transposes the nesting for you when you meant
            columns.

            Note that this two argument spelling always yields a read only table. Pass a
            `UI.Editability` as well if the user should be able to edit the cells.
        """
        given : 'One nested list, which could just as well describe rows or columns.'
            var cells = [["a", "b", "c"], ["x", "y", "z"]]
        and : 'A table which reads it along the cell order under test:'
            var table =
                    UI.table()
                    .withModel(cellOrder, { cells } as TableListDataSource)
                    .get(JTable)

        when : 'We read the first row back out of the table.'
            var firstRow = (0..<table.getColumnCount()).collect({ table.getValueAt(0, it) })
        then : 'The nesting was interpreted along exactly the axis we named.'
            table.getRowCount() == rowCount
            table.getColumnCount() == columnCount
            firstRow == expectedFirstRow
        and : 'Either way the table is read only, because we named no editability.'
            !table.isCellEditable(0, 0)

        where : 'We read one and the same nested list first as rows, and then as columns.'
            cellOrder                 | rowCount | columnCount || expectedFirstRow
            UI.CellOrder.ROW_MAJOR    | 2        | 3           || ["a", "b", "c"]
            UI.CellOrder.COLUMN_MAJOR | 3        | 2           || ["a", "x"]
    }

    def 'We can pass an `Event` to the table model to trigger updates.'()
    {
        reportInfo """
            Note that in this example we use a lambda based table model
            through the `withModel(Configurator)` method. This is a convenient
            way to create a table model without having to implement the TableModel
            interface yourself.
            <p>
            Observe how the `Event` has to be fired by hand after the data changed, and
            how the table can then only rebuild itself wholesale, because the event does
            not say what changed. A `TableData` value in a property (see the
            `Table_Data_Spec`) needs neither: it updates itself, and it knows exactly
            which rows moved.
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
                        UI.table(UI.CellOrder.ROW_MAJOR, UI.Editability.EDITABLE, { data })
                        .updateTableOn(event as Event)
        and : 'We actually build the component:'
            var table = ui.get(JTable)
        when : 'We fire the event.'
            event.fire()
            UI.sync() // Let the EDT finish rebuilding the table before we read it.
        then : 'The table UI is updated.'
            table.getRowCount() == 2
            table.getValueAt(0, 0) == 1
            table.getValueAt(0, 1) == 2
            table.getValueAt(0, 2) == 3
            table.getValueAt(1, 0) == 7
            table.getValueAt(1, 1) == 8
            table.getValueAt(1, 2) == 9
    }

    def 'A table can update itself reactively when bound to an `Observable`.'()
    {
        reportInfo """
            Binding to an `Observable` is the most versatile and generic way of updating a table reactively.
            This observable may be derived from things like properties, property lists or a simple `Event`.
            You may declare them in you view model and expose observables derived from them to your view to then bind
            to your tables. Changes in your VM will then automatically update the table!
        """
        given : 'A simple property holding some data.'
            var data = Var.of([[1, 2, 3], [7, 8, 9]])

        and : 'A simple table UI with a nested list based data table model.'
            var ui =
                        UI.table(UI.CellOrder.ROW_MAJOR, UI.Editability.EDITABLE, { data.get() })
                        .updateTableOn(data.view())
        and : 'We actually build the component:'
            var table = ui.get(JTable)
        when : 'We change the data in the property.'
            data.set([[11, 12, 13], [17, 18, 19]])
            UI.sync() // Let the EDT finish rebuilding the table before we read it.
        then : 'The table UI is updated automatically:'
            table.getRowCount() == 2
            table.getValueAt(0, 0) == 11
            table.getValueAt(0, 1) == 12
            table.getValueAt(0, 2) == 13
            table.getValueAt(1, 0) == 17
            table.getValueAt(1, 1) == 18
            table.getValueAt(1, 2) == 19
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

    def 'Fixed column names declared through `colNames(..)` also determine the column count.'()
    {
        reportInfo """
            When you declare a fixed array of column names using the
            `colNames(String...)` method, you usually do not have to
            declare a `colCount(..)` lambda anymore, because the number
            of column names already implies the number of columns.
        """
        given : 'Some row major table data.'
            var data = [
                            ["Joey",   34],
                            ["Fabian", 28],
                        ]
        and : 'A table with a model that declares fixed column names, but no explicit column count.'
            var table =
                    UI.table().withModel( m -> m
                        .colNames("Name", "Age")
                        .rowCount( () -> data.size() )
                        .getsEntryAt( (r, c) -> data[r][c] )
                    )
                    .get(JTable)

        expect : 'The column count is inferred from the two column names.'
            table.columnCount == 2
            table.rowCount == 2
        and : 'The columns have the declared names.'
            table.getColumnName(0) == "Name"
            table.getColumnName(1) == "Age"
        and : 'The cells resolve against the data as expected.'
            table.getValueAt(0, 0) == "Joey"
            table.getValueAt(1, 1) == 28
        and : 'Asking the model for a name outside of the declared range yields an empty string rather than an error.'
            table.getModel().getColumnName(42) == ""
    }

    def 'Fixed column classes declared through `colClasses(..)` imply the column count and default names.'()
    {
        reportInfo """
            The `colClasses(Class...)` method defines the types of the
            individual columns, which a `JTable` uses to pick appropriate
            renderers and editors (a checkbox for booleans for example).

            Just like with `colNames(..)`, the number of declared classes
            implies the column count if you do not specify one.
            And if you do not declare any column names either, the simple
            names of the column classes serve as default column names.
        """
        given : 'A table model with two typed columns and nothing else but data.'
            var data = [ [42, "meaning of life"] ]
            var table =
                    UI.table().withModel( m -> m
                        .colClasses(Integer, String)
                        .rowCount( () -> data.size() )
                        .getsEntryAt( (r, c) -> data[r][c] )
                    )
                    .get(JTable)

        expect : 'The column count is inferred from the two column classes.'
            table.columnCount == 2
        and : 'The model reports the declared classes to the table.'
            table.getModel().getColumnClass(0) == Integer
            table.getModel().getColumnClass(1) == String
        and : 'The default column names are derived from the class names.'
            table.getColumnName(0) == "Integer"
            table.getColumnName(1) == "String"
    }

    def 'Use `setsEntryAt(..)` to receive the edits a user makes through the table.'()
    {
        reportInfo """
            The `getsEntryAt(..)` lambda feeds data into the table, and the
            `setsEntryAt(..)` lambda is its counterpart which channels user
            edits back out of the table and into your data structure.
            Note that for edits to reach the setter in a real application,
            the cells also have to be declared editable using `isEditableIf(..)`.
        """
        given : 'A mutable matrix of data.'
            var data = [
                            [1, 2],
                            [3, 4],
                        ]
        and : 'A fully editable table model with both a getter and a setter lambda.'
            var table =
                    UI.table().withModel( m -> m
                        .colNames("A", "B")
                        .rowCount( () -> data.size() )
                        .getsEntryAt( (r, c) -> data[r][c] )
                        .setsEntryAt( (r, c, v) -> data[r][c] = v )
                        .isEditableIf( (r, c) -> true )
                    )
                    .get(JTable)

        when : 'The value of a particular cell is set through the standard `JTable` API.'
            table.setValueAt(42, 1, 0)
        then : 'The edit was channeled through the setter lambda into our data structure.'
            data == [
                        [1,  2],
                        [42, 4],
                    ]
        and : 'The table of course displays the new value.'
            table.getValueAt(1, 0) == 42
    }

    def 'A table model without any configuration is empty, but does not blow up.'()
    {
        reportInfo """
            All aspects of the lambda based table model are optional.
            Whatever you do not declare falls back to a sensible default:
            no rows, no columns, `null` entries, no editability and
            silently ignored edits.
        """
        given : 'A table with a model that was not configured at all.'
            var table = UI.table().withModel( m -> m ).get(JTable)
            var model = table.getModel()

        expect : 'The model is empty.'
            model.getRowCount() == 0
            model.getColumnCount() == 0
        and : 'Entries are reported as null and cells are not editable.'
            model.getValueAt(0, 0) == null
            !model.isCellEditable(0, 0)

        when : 'We try to store a value despite there being no setter lambda...'
            model.setValueAt(42, 0, 0)
        then : '...the call is simply ignored.'
            noExceptionThrown()
            model.getValueAt(0, 0) == null
    }

    def 'Firing the `updateOn` event notifies the table model listeners and refreshes the values.'()
    {
        reportInfo """
            This is a regression guard for the lambda based table model update path.
            When the bound `updateOn` event fires, the model must notify its
            registered `TableModelListener`s (so a `JTable` repaints) and the
            values served afterwards must reflect the new state of the data.
        """
        given : 'Some mutable data and an update event.'
            var data = [10, 20, 30]
            var update = Event.create()
        and : 'A lambda based table model bound to the update event.'
            var model =
                    UI.table().withModel( m -> m
                        .colNames("V")
                        .rowCount( () -> data.size() )
                        .getsEntryAt( (r, c) -> data[r] )
                        .updateOn(update)
                    )
                    .get(JTable).getModel()
        and : 'A listener recording the events it receives.'
            var events = []
            model.addTableModelListener({ TableModelEvent e -> events << e } as TableModelListener)

        expect : 'The model starts with the initial data.'
            model.getRowCount() == 3
            model.getValueAt(0, 0) == 10
            model.getValueAt(2, 0) == 30

        when : 'We change the data and fire the update event.'
            data = [11, 22, 33, 44]
            update.fire()
            UI.sync()
        then : 'The listeners were notified...'
            !events.isEmpty()
        and : '...and the model now serves the new data.'
            model.getRowCount() == 4
            model.getValueAt(0, 0) == 11
            model.getValueAt(3, 0) == 44
    }

    def 'Firing `updateTableOn` notifies the listeners of a list based table model.'()
    {
        reportInfo """
            The same regression guard as above, but for the collection based
            table models installed through `updateTableOn(Event)`.
        """
        given : 'A mutable matrix and an update event.'
            var data = [[1, 2], [3, 4]]
            var event = Event.create()
        and : 'A row major list based table bound to the update event.'
            var model =
                    UI.table(UI.CellOrder.ROW_MAJOR, UI.Editability.EDITABLE, { data })
                    .updateTableOn(event as Event)
                    .get(JTable).getModel()
        and : 'A listener recording the events it receives.'
            var events = []
            model.addTableModelListener({ TableModelEvent e -> events << e } as TableModelListener)

        expect : 'The model starts with the initial data.'
            model.getRowCount() == 2
            model.getValueAt(1, 1) == 4

        when : 'We grow the data and fire the event.'
            data = [[5, 6], [7, 8], [9, 10]]
            event.fire()
            UI.sync()
        then : 'The listeners were notified and the values reflect the new data.'
            !events.isEmpty()
            model.getRowCount() == 3
            model.getValueAt(2, 0) == 9
            model.getValueAt(2, 1) == 10
    }

    def 'Every aspect of a lambda based table model may only be declared once.'()
    {
        reportInfo """
            The `BasicTableModel.Builder` fails fast when you accidentally
            declare the same model aspect twice, because the second declaration
            would silently override the first one, which is most likely a bug
            in the declaration. So instead of guessing which lambda you meant,
            it throws an `IllegalStateException` right away.
        """
        given : 'A table model builder with a row count already defined.'
            var builder = new BasicTableModel.Builder<>(Object).rowCount( () -> 2 )

        when : 'We try to define a second row count lambda...'
            builder.rowCount( () -> 42 )
        then : '...the builder complains immediately.'
            thrown(IllegalStateException)

        when : 'A null lambda on the other hand is reported as an invalid argument.'
            builder.colCount(null)
        then :
            thrown(IllegalArgumentException)
    }

    def 'A property of a `Tuple` of `Tuple`s can be used as a data source for tables.'()
    {
        reportInfo """
            The most convenient way to model the data of a table is a property
            holding a `Tuple` of `Tuple`s of cell values, a matrix of rows.
            Because a `Tuple` is immutable, such a table is thread safe by
            construction, and because the property is observable, the table
            updates itself whenever the property changes. So unlike for the
            collection based data sources, you do not need to bind an update
            `Event` through `updateTableOn(..)`.
        """
        given : 'A property holding a row major matrix of cell values.'
            var rows = Var.of(Tuple.of(
                                Tuple.of("Alice", "30"),
                                Tuple.of("Bob",   "42")
                            ))
        and : 'A table built from this property:'
            var table = UI.table(UI.CellOrder.ROW_MAJOR, rows).get(JTable)

        expect : 'The table displays the rows of the property.'
            table.getRowCount() == 2
            table.getColumnCount() == 2
            table.getValueAt(0, 0) == "Alice"
            table.getValueAt(0, 1) == "30"
            table.getValueAt(1, 0) == "Bob"
            table.getValueAt(1, 1) == "42"
        and : 'The columns have the default (spreadsheet style) names.'
            table.getColumnName(0) == "A"
            table.getColumnName(1) == "B"
        and : 'The cells are read only, because we did not ask for an editable table.'
            !table.isCellEditable(0, 0)

        when : 'We change the property by adding a row...'
            rows.update({ it.add(Tuple.of("Carol", "27")) })
            UI.sync()
        then : '...the table updates itself, no update event needed.'
            table.getRowCount() == 3
            table.getValueAt(2, 0) == "Carol"

        when : 'We replace the entire tuple with a completely different one...'
            rows.set(Tuple.of(Tuple.of("Dave", "51", "extra")))
            UI.sync()
        then : '...the table follows, including the new column count.'
            table.getRowCount() == 1
            table.getColumnCount() == 3
            table.getValueAt(0, 2) == "extra"
    }

    def 'The cell order of a `Tuple` based table can be configured to be column major.'()
    {
        reportInfo """
            Just like for the list based data sources, you may tell the table
            how to interpret the matrix you hand it, through one of the
            `UI.CellOrder` constants. In a column major cell order the outer `Tuple`
            holds the columns of the table and each inner `Tuple` the cells of
            a column, which the table transposes for you.
        """
        given : 'A property holding a column major matrix of cell values.'
            var columns = Var.of(Tuple.of(
                                Tuple.of("a", "b", "c"),
                                Tuple.of("x", "y", "z")
                            ))
        and : 'A table built from this property, declared to be column major:'
            var table = UI.table(UI.CellOrder.COLUMN_MAJOR, columns).get(JTable)

        expect : 'The two tuples are displayed as the two columns of a 3 row table.'
            table.getRowCount() == 3
            table.getColumnCount() == 2
            table.getValueAt(0, 0) == "a"
            table.getValueAt(0, 1) == "x"
            table.getValueAt(1, 0) == "b"
            table.getValueAt(1, 1) == "y"
            table.getValueAt(2, 0) == "c"
            table.getValueAt(2, 1) == "z"

        when : 'We add another column to the property...'
            columns.update({ it.add(Tuple.of("1", "2", "3")) })
            UI.sync()
        then : '...the table displays a third column.'
            table.getRowCount() == 3
            table.getColumnCount() == 3
            table.getValueAt(0, 2) == "1"
    }

    def 'A `Tuple` based table can also be bound further down the chain, through `withModel(..)`.'()
    {
        reportInfo """
            `UI.table(..)` and `UI.table().withModel(..)` are two spellings of the same
            thing. The factory is the shorter one for when the table is the whole
            declaration, and the builder method is for when you have already begun a
            table and want to bind its data further down the chain, after configuring
            something else about it first.

            Both come in a two argument spelling which leaves the editability out. That
            reads as read only, which is the safe thing to default to.
        """
        given : 'A property holding two rows of cells.'
            var rows = Var.of(Tuple.of(
                            Tuple.of("Alice", "30"),
                            Tuple.of("Bob",   "42")
                        ))
        and : 'A table which binds that property through the builder, without naming an editability:'
            var table =
                    UI.table()
                    .id("people")
                    .withModel(UI.CellOrder.ROW_MAJOR, rows)
                    .get(JTable)

        expect : 'Everything we declared before the binding survived it.'
            table.getName() == "people"
        and : 'The table shows the two rows held by the property.'
            table.getRowCount() == 2
            table.getColumnCount() == 2
            table.getValueAt(0, 0) == "Alice"
            table.getValueAt(1, 1) == "42"
        and : 'Its cells are read only, because we did not ask for anything else.'
            !table.isCellEditable(0, 0)

        when : 'We add a row to the property...'
            rows.update({ it.add(Tuple.of("Carol", "27")) })
            UI.sync()
        then : '...the table follows it, exactly as it would have through `UI.table(..)`.'
            table.getRowCount() == 3
            table.getValueAt(2, 0) == "Carol"
    }

    def 'The cells of an editable `Tuple` based table are written back into the property.'()
    {
        reportInfo """
            If you declare `UI.Editability.EDITABLE` and hand the table a mutable
            `Var`, then the user may edit the cells of the table.
            Such an edit is written back into the property, which means your
            application state stays the single source of truth for the table.
        """
        given : 'A property holding a matrix of cell values, in the given cell order.'
            var cells = Var.of(Tuple.of(
                                Tuple.of("a", "b"),
                                Tuple.of("x", "y")
                            ))
        and : 'A table built from this property, declared to be editable:'
            var table = UI.table(cellOrder, UI.Editability.EDITABLE, cells).get(JTable)

        expect : 'The table considers its cells editable.'
            table.isCellEditable(0, 0)

        when : 'The user edits a cell through the regular `JTable` API...'
            table.setValueAt("!", 1, 0)
            UI.sync()
        then : '...the edit landed in the property, at the place the cell order dictates.'
            cells.get() == Tuple.of(expectedFirst, expectedSecond)
        and : 'The table of course displays the new value.'
            table.getValueAt(1, 0) == "!"

        where : 'We check this for both cell orders.'
            cellOrder                   || expectedFirst          | expectedSecond
            UI.CellOrder.ROW_MAJOR      || Tuple.of("a", "b")     | Tuple.of("!", "y")
            UI.CellOrder.COLUMN_MAJOR   || Tuple.of("a", "!")     | Tuple.of("x", "y")
    }

    def 'A `Tuple` based table is read only if either its editability or its property says so.'()
    {
        reportInfo """
            Editability of a tuple based table needs two things: permission through
            `UI.Editability.EDITABLE`, and a property which can actually receive the
            edit. A read only `Val` cannot, which is why it always yields a read only
            table, even if you ask for an editable one. Any edit which sneaks in
            through the model API anyway is silently ignored instead of
            corrupting the table.
        """
        given : 'A tuple of cells, wrapped in the given kind of property.'
            var tuple = Tuple.of(Tuple.of("a", "b"))
            var property = ( mutable ? Var.of(tuple) : Val.of(tuple) )
        and : 'A table built from this property, with the given editability:'
            var table = UI.table(UI.CellOrder.ROW_MAJOR, editability, property).get(JTable)

        expect : 'The table is only editable if both the editability and the property allow it.'
            table.isCellEditable(0, 0) == isEditable

        when : 'We push an edit through the model API regardless...'
            table.getModel().setValueAt("!", 0, 0)
            UI.sync()
        then : '...it is only honored if the table is actually editable.'
            noExceptionThrown()
            table.getValueAt(0, 0) == ( isEditable ? "!" : "a" )

        where : 'We check every combination of editability and property mutability.'
            editability                    | mutable || isEditable
            UI.Editability.EDITABLE        | true    || true
            UI.Editability.EDITABLE        | false   || false
            UI.Editability.READ_ONLY       | true    || false
            UI.Editability.READ_ONLY       | false   || false
    }

    def 'A `Tuple` based table survives empty, ragged and out of bounds data.'()
    {
        reportInfo """
            A table is queried by Swing at times you do not control, which is why
            a tuple based table never throws at its data source boundaries:
            an empty tuple is simply an empty table, a ragged matrix is as wide
            as its longest row (with the missing cells reading as `null`), and
            any out of bounds read is answered with `null`.
        """
        given : 'A property holding a ragged matrix, including an empty row.'
            var rows = Var.of(Tuple.of(
                                Tuple.of("a", "b", "c"),
                                Tuple.of("d"),
                                Tuple.of(String)
                            ))
        and : 'A table built from this property:'
            var table = UI.table(UI.CellOrder.ROW_MAJOR, rows).get(JTable)
            var model = table.getModel()

        expect : 'The table is as wide as the longest row.'
            model.getRowCount() == 3
            model.getColumnCount() == 3
        and : 'The cells the shorter rows do not have are reported as null.'
            model.getValueAt(1, 0) == "d"
            model.getValueAt(1, 2) == null
            model.getValueAt(2, 0) == null
        and : 'Reads beyond the bounds of the table are answered with null instead of an exception.'
            model.getValueAt(3, 0) == null
            model.getValueAt(0, 3) == null
            model.getValueAt(-1, -1) == null

        when : 'We empty the property completely...'
            rows.set(Tuple.of(Tuple.classTyped(String)))
            UI.sync()
        then : '...the table is empty, and still does not blow up when read.'
            model.getRowCount() == 0
            model.getColumnCount() == 0
            model.getValueAt(0, 0) == null
            noExceptionThrown()
    }

    def 'A `Tuple` based table tolerates a property which currently holds no tuple at all.'()
    {
        reportInfo """
            A nullable property is a natural way to model table data which has not
            arrived yet (or which went away again). Binding such a property must
            yield an empty table rather than an exception, no matter whether the
            property is mutable or read only, and the table must fill up as soon
            as the data arrives.
        """
        given : 'A mutable and a read only nullable property, neither holding a tuple yet.'
            var rows = Var.ofNullable(Tuple, null)
            var readOnlyRows = Val.ofNullable(Tuple, null)
        and : 'A table bound to each of them:'
            var table = UI.table(UI.CellOrder.ROW_MAJOR, UI.Editability.EDITABLE, rows).get(JTable)
            var readOnlyTable = UI.table(UI.CellOrder.ROW_MAJOR, readOnlyRows).get(JTable)

        expect : 'Both tables read as empty instead of being broken.'
            table.getRowCount() == 0
            table.getColumnCount() == 0
            readOnlyTable.getRowCount() == 0
            readOnlyTable.getColumnCount() == 0

        when : 'The data arrives in the mutable property...'
            rows.set(Tuple.of(Tuple.of("a", "b"), Tuple.of("c", "d")))
            UI.sync()
        then : '...the table fills up.'
            table.getRowCount() == 2
            table.getColumnCount() == 2
            table.getValueAt(1, 0) == "c"

        when : 'The data goes away again...'
            rows.set(null)
            UI.sync()
        then : '...the table empties out, again without any drama.'
            table.getRowCount() == 0
            table.getColumnCount() == 0
    }

    def 'A `Tuple` based table translates a change of its property into the most targeted table events possible.'()
    {
        reportInfo """
            A `Tuple` does not merely tell you that it changed, it also tells you
            *how* it changed. A tuple based table exploits this: instead of
            blindly rebuilding itself on every change, it translates a row
            insertion into a `fireTableRowsInserted`, a removal into a
            `fireTableRowsDeleted` and an in place change into a
            `fireTableRowsUpdated`, which is what keeps updates to large tables
            cheap. Only a change which cannot be expressed as a single row range,
            or one which changes the shape of the table, makes it fall back to a
            full rebuild (a structure change followed by a data change).
        """
        given : 'A property holding 3 rows of 2 cells each, and a table bound to it.'
            var rows = Var.of(Tuple.of(
                                Tuple.of("A", "1"),
                                Tuple.of("B", "2"),
                                Tuple.of("C", "3")
                            ))
            var table = UI.table(UI.CellOrder.ROW_MAJOR, rows).get(JTable)
        and : 'A listener recording the table model events in a readable form.'
            var events = []
            table.getModel().addTableModelListener({ TableModelEvent e -> events << describe(e) } as TableModelListener)

        when : 'We apply the given change to the property...'
            rows.update({ change(it) })
            UI.sync()
        then : '...the table fired exactly the expected events...'
            events == expectedEvents
        and : '...and it displays exactly the expected rows.'
            (0..<table.rowCount).collect({ r ->
                (0..<table.columnCount).collect({ c -> table.getValueAt(r, c) })
            }) == expectedRows

        where : 'We cover every kind of change a tuple of rows may undergo.'
            change                                                      || expectedEvents            | expectedRows
            ({ it.add(Tuple.of("D", "4")) })                            || ["INSERT[3..3]"]          | [["A","1"], ["B","2"], ["C","3"], ["D","4"]]
            ({ it.addAt(1, Tuple.of("X", "9")) })                       || ["INSERT[1..1]"]          | [["A","1"], ["X","9"], ["B","2"], ["C","3"]]
            ({ it.addAllAt(1, Tuple.of("X","9"), Tuple.of("Y","8")) })   || ["INSERT[1..2]"]          | [["A","1"], ["X","9"], ["Y","8"], ["B","2"], ["C","3"]]
            ({ it.removeAt(1) })                                        || ["DELETE[1..1]"]          | [["A","1"], ["C","3"]]
            ({ it.removeFirst(2) })                                     || ["DELETE[0..1]"]          | [["C","3"]]
            ({ it.setAt(2, Tuple.of("Z", "9")) })                       || ["UPDATE[2..2]"]          | [["A","1"], ["B","2"], ["Z","9"]]
            ({ it.setAllAt(0, Tuple.of("X","7"), Tuple.of("Y","8")) })   || ["UPDATE[0..1]"]          | [["X","7"], ["Y","8"], ["C","3"]]
            ({ it.setAt(0, Tuple.of("A", "1")) })                       || []                        | [["A","1"], ["B","2"], ["C","3"]]
            ({ it.reversed() })                                         || ["STRUCTURE", "ALL_DATA"] | [["C","3"], ["B","2"], ["A","1"]]
            ({ it.clear() })                                            || ["STRUCTURE", "ALL_DATA"] | []
            ({ it.add(Tuple.of("D", "4", "!")) })                       || ["STRUCTURE", "ALL_DATA"] | [["A","1",null], ["B","2",null], ["C","3",null], ["D","4","!"]]
    }

    /**
     *  Turns a raw {@link TableModelEvent} into a readable description,
     *  so that the expectations of a feature can be written down as plain strings.
     */
    private static String describe( TableModelEvent event ) {
        if ( event.firstRow == TableModelEvent.HEADER_ROW )
            return "STRUCTURE"
        if ( event.firstRow == 0 && event.lastRow == Integer.MAX_VALUE )
            return "ALL_DATA"
        var type = [
                (TableModelEvent.INSERT) : "INSERT",
                (TableModelEvent.DELETE) : "DELETE",
                (TableModelEvent.UPDATE) : "UPDATE",
            ][event.type]
        return "$type[$event.firstRow..$event.lastRow]"
    }

    def 'A property of a `TableData` can be used as a data source for tables.'()
    {
        reportInfo """
            A `TableData` is the most complete description of the contents of
            a table: it carries the cells, the column names, the column classes, the
            cell order and the editability of the data, all in a single immutable
            value. So if you hold such a value in a property, then that property alone
            describes your whole table, which is why you may bind it directly.
            Note that no `updateTableOn(..)` binding is needed here, because the
            table listens to the property itself.
        """
        given : 'A property holding a fully described table.'
            var model = Var.of(
                            TableData.of(UI.CellOrder.ROW_MAJOR, "Name", "Age")
                                .setColumnClassAt(0, String)
                                .setColumnClassAt(1, Integer)
                                .addRow("Alice", 30)
                                .addRow("Bob",   42)
                        )
        and : 'A table built from this property:'
            var table = UI.table(model).get(JTable)

        expect : 'The table displays everything the data describes.'
            table.getRowCount() == 2
            table.getColumnCount() == 2
            table.getValueAt(0, 0) == "Alice"
            table.getValueAt(1, 1) == 42
        and : 'The column names and classes reach the table as well.'
            table.getColumnName(0) == "Name"
            table.getColumnName(1) == "Age"
            table.getModel().getColumnClass(1) == Integer

        when : 'We add a row to the data held by the property...'
            model.update({ it.addRow("Carol", 55) })
            UI.sync()
        then : '...the table shows the new row.'
            table.getRowCount() == 3
            table.getValueAt(2, 0) == "Carol"
    }

    def 'A `TableData` property based table is read only if either its value or its property is read only.'()
    {
        reportInfo """
            Just like for a `Tuple` based table, two conditions have to be met before
            the user may edit the cells of a `TableData` based table: the snapshot
            itself has to permit it (see `TableData::asEditable`), and the
            property has to be a mutable `Var` which can actually receive the edit.
            If either is missing, then the table is simply read only.
        """
        given : 'A table with the given editability, held by a property of the given mutability.'
            var data  = TableData.of(UI.CellOrder.ROW_MAJOR)
                                 .withEditability(editability)
                                 .addRow("a", "b")
            var model = mutable ? Var.of(data) : Val.of(data)
        and : 'A table bound to that property:'
            var table = UI.table(model).get(JTable)

        expect : 'The table only lets the user edit if both conditions are met.'
            table.isCellEditable(0, 0) == editable

        where :
            editability               | mutable || editable
            UI.Editability.EDITABLE   | true    || true
            UI.Editability.EDITABLE   | false   || false
            UI.Editability.READ_ONLY  | true    || false
            UI.Editability.READ_ONLY  | false   || false
    }

    def 'A `Var` exposed as a read only `Val` cannot be edited through the table.'()
    {
        reportInfo """
            A view model routinely keeps its state in a `Var` but exposes only a read
            only `Val` of it to the outside (a `public Val<TableData> tableModel()`
            over a `private Var<TableData>`, say). If such a `Val` is bound through the
            read only `withModel(Val)` overload, then the table must be read only, no
            matter that the reference happens to point at a mutable `Var` at runtime:
            the overload the user picked is what expresses the intent, not the runtime
            type of the property. Anything else would let edits leak into state the
            view model never meant to expose.
        """
        given : 'A mutable `Var` holding an editable snapshot.'
            var backing = Var.of(
                            TableData.of(UI.CellOrder.ROW_MAJOR).asEditable()
                                .addRow("a", "b")
                        )
        and : '''
                A table bound to it through the read only overload. The `(Val)` cast
                is what picks that overload here, just as a Java caller holding a
                `Val` reference (a view model exposing its `Var` as a `Val`, say)
                would pick it, rather than Groovy dispatching on the runtime `Var`.
            '''
            var table = UI.table((Val) backing).get(JTable)

        expect : 'The table reports its cells as read only, despite the mutable backing.'
            !table.isCellEditable(0, 0)

        when : 'An edit is forced through the model API regardless...'
            table.getModel().setValueAt("!", 0, 0)
            UI.sync()
        then : '...it is silently ignored: neither the table nor the backing property change.'
            noExceptionThrown()
            table.getValueAt(0, 0) == "a"
            backing.get().getValueAt(0, 0) == "a"
    }

    def 'The cells of an editable `TableData` based table are written back into the property.'()
    {
        reportInfo """
            A `Var` holding an editable `TableData` is a two way binding: an edit
            made through the table model finds its way back into the property, and the
            property in turn is what the table reads. Note that the snapshot is a value,
            so the property receives a new snapshot rather than a mutated one.
        """
        given : 'A property holding an editable snapshot, and a table bound to it.'
            var model = Var.of(
                            TableData.of(UI.CellOrder.ROW_MAJOR).asEditable()
                                .addRow("a", "b")
                                .addRow("c", "d")
                        )
            var table = UI.table(model).get(JTable)
        and : 'We remember the initial snapshot, so that we can tell it was not mutated.'
            var initial = model.get()

        when : 'The user edits a cell of the table...'
            table.getModel().setValueAt("!", 1, 0)
            UI.sync()
        then : '...the edit is written back into the property.'
            model.get().getValueAt(1, 0) == "!"
        and : 'The table displays the new value as well.'
            table.getValueAt(1, 0) == "!"
        and : 'The snapshot the property held before the edit is untouched.'
            initial.getValueAt(1, 0) == "c"
    }

    def 'A change of the column names of a `TableData` property reaches the table.'()
    {
        reportInfo """
            The columns of a table are not just its cells: a `TableData` also
            describes the name and class of every column. A `JTable` can only pick
            those up through a table structure change, so the model has to recognize
            a change of the columns as being structural.
            <p>
            Note how deliberate this is: below we rename a column and edit a row in
            one and the same change. The row edit alone would be synced through a
            targeted row update (the cells carry a diff which says so), and a row
            update would leave the table sitting on its old header. Recognizing the
            renamed column is what makes the model fall back to a full rebuild here.
        """
        given : 'A property holding a named table, and a table bound to it.'
            var model = Var.of(
                            TableData.of(UI.CellOrder.ROW_MAJOR, "First", "Second")
                                .addRow("a", "b")
                        )
            var table = UI.table(model).get(JTable)
        and : 'A listener recording the table model events in a readable form.'
            var events = []
            table.getModel().addTableModelListener({ TableModelEvent e -> events << describe(e) } as TableModelListener)

        expect : 'The table starts out with the initial column names.'
            table.getColumnName(0) == "First"

        when : 'We rename a column and change a cell of the only row, both at once...'
            model.update({ it.setColumnNameAt(0, "Renamed").setCellAt(0, 0, "x") })
            UI.sync()
        then : '...the table rebuilds itself entirely, instead of merely updating the row.'
            events.contains("STRUCTURE")
        and : 'Both the renamed column and the changed cell arrive in the table.'
            table.getColumnName(0) == "Renamed"
            table.getValueAt(0, 0) == "x"
    }

}
