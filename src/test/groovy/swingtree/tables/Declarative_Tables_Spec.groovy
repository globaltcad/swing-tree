package swingtree.tables

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Occurrence
import swingtree.SwingTree
import swingtree.threading.EventProcessor
import swingtree.UI
import swingtree.UIForTable

import javax.swing.*

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

        expect : 'The table UI has the following state:'
            ui.component.getColumnName(0) == "X"
            ui.component.getColumnName(1) == "Y"
            ui.component.getRowCount() == 3
            ui.component.getColumnCount() == 2
            ui.component.getValueAt(0, 0) == "a"
            ui.component.getValueAt(0, 1) == "1"
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

        expect : 'The table UI has the following state:'
            ui.component.getColumnName(0) == "A" // default column names
            ui.component.getColumnName(1) == "B"
            ui.component.getRowCount() == 3
            ui.component.getColumnCount() == 2
            ui.component.getValueAt(0, 0) == "a"
            ui.component.getValueAt(0, 1) == "x"
            ui.component.getValueAt(1, 0) == "b"
            ui.component.getValueAt(1, 1) == "y"
            ui.component.getValueAt(2, 0) == "c"
            ui.component.getValueAt(2, 1) == "z"
    }

    def 'We can pass an `Occurrence` to the table model to trigger updates.'()
    {
        reportInfo """
            Note that in this example we use a lambda based table model
            through the `UI.tableModel()` factory method. This is a convenient
            way to create a table model without having to implement the TableModel
            interface yourself.
        """

        given : 'We have an update event and some data.'
            var data = [1, 2, 3, 4]
            var update = Occurrence.create()
        and : 'We create a table with a lambda based table model.'
            var ui =
                    UI.table().withModel(
                        UI.tableModel()
                        .colName( {["X", "Y", "Z"][it]})
                        .colCount({3})
                        .rowCount({data.size()})
                        .getter({ r, c ->data[r]})
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
            update.trigger()
            UI.sync() // sync with the EDT
        then : 'The table has the correct data.'
            ui.get(JTable).getValueAt(0, 0) == 5
            ui.get(JTable).getValueAt(1, 0) == 6
            ui.get(JTable).getValueAt(2, 0) == 7
            ui.get(JTable).getValueAt(3, 0) == 8
    }

    def 'We need to attach an update `Occurrence` to our table when the table data is list based and its data changes.'()
    {
        reportInfo """
            Use the Occurrence class in your view model to define an event you can fire when you modify the data
            of your table. The event will be used to update the table UI  if you register it with the table UI.
        """
        given : 'A simple event and some data.'
            var event = Occurrence.create()
            var data = [[1, 2, 3], [7, 8, 9]]

        and : 'A simple table UI with a nested list based data table model.'
            var ui =
                        UI.table(UI.ListData.ROW_MAJOR_EDITABLE, { data })
                        .updateTableOn(event)
        when : 'We fire the event.'
            event.trigger()
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
