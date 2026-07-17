package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Tuple
import sprouts.Var
import swingtree.api.model.TableData
import swingtree.threading.EventProcessor

import javax.swing.JTable
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener

@Title("Modelling Tables as Data")
@Narrative('''

    The `TableData` value is SwingTree's answer to the question
    "where does the data of my table live?".

    It is a single immutable value describing an entire table: its cells, the name
    and class of every column, and the layout tying the two together. You hold one
    in a property, you bind that property to a table, and from then on your business
    logic never touches Swing again - it merely produces the next version of a value.

    Because it is a value, it is thread safe by construction: the application thread
    and the UI thread can hand it to each other without locks. And because its cells
    live in `Tuple`s, which remember how they were derived from their predecessor,
    a table bound to it updates *incrementally* rather than rebuilding itself.

''')
@Subject([TableData])
class Table_Data_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def 'A table is built up out of columns and rows, and reads back the way you wrote it.'()
    {
        reportInfo """
            The usual way of building a table is to name its columns first and then
            to pour rows into it. Note how the row values are plain objects: you do
            not have to wrap anything.
        """
        given : 'A table with three named columns and two rows.'
            var data = TableData.of(UI.ListData.ROW_MAJOR, "Name", "Age", "City")
                            .addRow("Alice", 30, "Rome")
                            .addRow("Bob",   42, "Oslo")

        expect : 'It has the dimensions you would expect.'
            data.getRowCount() == 2
            data.getColumnCount() == 3
            !data.isEmpty()
        and : 'Every cell reads back in plain (row, column) terms.'
            data.getValueAt(0, 0) == "Alice"
            data.getValueAt(0, 1) == 30
            data.getValueAt(1, 2) == "Oslo"
        and : 'The columns know their names, and you may look them up by name.'
            data.getColumnName(1) == "Age"
            data.indexOfColumn("City") == 2
            data.indexOfColumn("Nope") == -1
        and : 'You may also read whole rows and columns at once.'
            data.getRow(0) == Tuple.ofNullable(Object, "Alice", 30, "Rome")
            data.getColumn(0) == Tuple.ofNullable(Object, "Alice", "Bob")
    }

    def 'A table is a value, so changing it never touches the table you started with.'()
    {
        reportInfo """
            Every method which sounds like it changes something really returns a *new*
            `TableData`. This is what makes the whole thing thread safe, and it is also
            what lets a table tell whether anything actually changed: two tables with
            the same columns and cells are equal to each other.
        """
        given : 'A table, and a second one built exactly the same way.'
            var data  = TableData.of(UI.ListData.ROW_MAJOR, "A", "B").addRow("x", "y")
            var other = TableData.of(UI.ListData.ROW_MAJOR, "A", "B").addRow("x", "y")

        expect : 'The two are equal and agree on their hash code.'
            data == other
            data.hashCode() == other.hashCode()

        when : 'We derive a new table by changing a cell...'
            var changed = data.setCellAt(0, 0, "!")
        then : '...the original is left exactly as it was.'
            data.getValueAt(0, 0) == "x"
            changed.getValueAt(0, 0) == "!"
            changed != data
        and : 'A change which changes nothing hands you back the very same table.'
            data.setCellAt(0, 0, "x") === data
        and : 'So does a change which is out of bounds.'
            data.setCellAt(7, 7, "?") === data
            data.removeRowAt(7) === data
            data.removeColumnAt(7) === data
    }

    def 'The empty table is empty, and is where a table without columns starts.'()
    {
        expect : 'It has nothing in it, no matter how you ask.'
            TableData.empty().getRowCount() == 0
            TableData.empty().getColumnCount() == 0
            TableData.empty().isEmpty()
            TableData.empty().getValueAt(0, 0) == null
            TableData.empty().getRow(0).isEmpty()
        and : 'A table with columns but no rows is still empty, because it has nothing to show.'
            TableData.of(UI.ListData.ROW_MAJOR, "A", "B").isEmpty()
        and : 'But it does know about its columns, which is what a table header needs.'
            TableData.of(UI.ListData.ROW_MAJOR, "A", "B").getColumnCount() == 2
            TableData.of(UI.ListData.ROW_MAJOR, "A", "B").getRowCount() == 0
    }

    def 'Rows can be added, replaced and removed, one at a time or in ranges.'()
    {
        reportInfo """
            Range operations are not just a convenience, they are the efficient path:
            a row major table hands a range change to a `JTable` as a *single* event,
            so adding a hundred rows repaints once rather than a hundred times.
        """
        given : 'A table with two columns and three rows.'
            var data = TableData.of(UI.ListData.ROW_MAJOR, "A", "B")
                            .addRow("a1", "b1")
                            .addRow("a2", "b2")
                            .addRow("a3", "b3")

        when : 'We insert a row in the middle...'
            var inserted = data.addRowAt(1, "a!", "b!")
        then : '...it lands exactly where we asked.'
            inserted.getRowCount() == 4
            inserted.getRow(1) == Tuple.ofNullable(Object, "a!", "b!")
            inserted.getRow(2) == Tuple.ofNullable(Object, "a2", "b2")

        when : 'We add several rows at once...'
            var grown = data.addRows(Tuple.of(
                            TableData.row("a4", "b4"),
                            TableData.row("a5", "b5")
                        ))
        then : '...they are all appended, in order.'
            grown.getRowCount() == 5
            grown.getRow(4) == Tuple.ofNullable(Object, "a5", "b5")

        when : 'We replace a range of rows...'
            var replaced = data.setRowsAt(1, Tuple.of(
                                TableData.row("X", "Y"),
                                TableData.row("Z", "W")
                            ))
        then : '...only that range changed.'
            replaced.getRowCount() == 3
            replaced.getRow(0) == Tuple.ofNullable(Object, "a1", "b1")
            replaced.getRow(1) == Tuple.ofNullable(Object, "X", "Y")
            replaced.getRow(2) == Tuple.ofNullable(Object, "Z", "W")

        when : 'We remove a range of rows...'
            var shrunk = data.removeRowsAt(0, 2)
        then : '...only the survivors remain.'
            shrunk.getRowCount() == 1
            shrunk.getRow(0) == Tuple.ofNullable(Object, "a3", "b3")

        when : 'We remove every row...'
            var emptied = data.removeAllRows()
        then : '...the columns stay behind, ready to be filled again.'
            emptied.getRowCount() == 0
            emptied.getColumnCount() == 2
            emptied.getColumnName(0) == "A"
    }

    def 'Columns can be added and removed, taking their names and classes along.'()
    {
        reportInfo """
            A column is not just a strip of cells, it also has a name and a class
            (which is what a `JTable` consults to pick a renderer). So adding or
            removing one moves all three together, and you never have to keep a
            separate list of headers in sync by hand.
        """
        given : 'A table with two columns and two rows.'
            var data = TableData.of(UI.ListData.ROW_MAJOR, "Name", "Age")
                            .addRow("Alice", 30)
                            .addRow("Bob",   42)

        when : 'We append a column...'
            var wider = data.addColumn("City", String, TableData.row("Rome", "Oslo"))
        then : '...the table grew a column, with its name, class and cells all in place.'
            wider.getColumnCount() == 3
            wider.getColumnName(2) == "City"
            wider.getColumnClass(2) == String
            wider.getValueAt(0, 2) == "Rome"
            wider.getValueAt(1, 2) == "Oslo"
        and : 'The rows we already had are still intact.'
            wider.getValueAt(0, 0) == "Alice"

        when : 'We insert a column in the middle instead...'
            var middle = data.addColumnAt(1, "Nick", String, TableData.row("Ally", "Bobby"))
        then : '...everything to its right shifts over.'
            middle.getColumnName(1) == "Nick"
            middle.getColumnName(2) == "Age"
            middle.getRow(0) == Tuple.ofNullable(Object, "Alice", "Ally", 30)

        when : 'We remove a column...'
            var narrower = data.removeColumnAt(0)
        then : '...its name and cells go with it.'
            narrower.getColumnCount() == 1
            narrower.getColumnName(0) == "Age"
            narrower.getRow(0) == Tuple.ofNullable(Object, 30)

        when : 'We replace the cells of a column...'
            var recoloured = data.setColumnAt(1, TableData.row(31, 43))
        then : '...only that column changed.'
            recoloured.getColumn(1) == Tuple.ofNullable(Object, 31, 43)
            recoloured.getColumn(0) == Tuple.ofNullable(Object, "Alice", "Bob")
    }

    def 'A table can be reshaped fundamentally: its rows, columns and types may all change.'()
    {
        reportInfo """
            Nothing about the shape of a `TableData` is fixed once and for all. The
            same property can hold a two column table of strings now and a four column
            table of numbers and booleans a moment later. This matters because real
            applications do exactly this: you switch a report, you pivot a view, you
            load a different file.
        """
        given : 'A modest table of two string columns.'
            var data = TableData.of(UI.ListData.ROW_MAJOR, "Name", "City")
                            .addRow("Alice", "Rome")

        expect : 'It starts out as plain untyped columns.'
            data.getColumnCount() == 2
            data.getColumnClass(0) == Object

        when : 'We reshape it completely: new columns, new names, new classes, new rows.'
            var reshaped = data
                                .removeAllRows()
                                .removeColumnsAt(0, 2)
                                .addColumn("Id",     Integer, TableData.row())
                                .addColumn("Active", Boolean, TableData.row())
                                .addColumn("Score",  Double,  TableData.row())
                                .addRow(1, true,  9.5d)
                                .addRow(2, false, 7.25d)

        then : 'It is a different table entirely, and it knows its new types.'
            reshaped.getColumnCount() == 3
            reshaped.getRowCount() == 2
            reshaped.columnNames() == Tuple.ofNullable(String, "Id", "Active", "Score")
            reshaped.getColumnClass(0) == Integer
            reshaped.getColumnClass(1) == Boolean
            reshaped.getColumnClass(2) == Double
        and : 'The cells are the new ones.'
            reshaped.getRow(0) == Tuple.ofNullable(Object, 1, true, 9.5d)
        and : 'The table we started with never noticed any of it.'
            data.getColumnCount() == 2
            data.getValueAt(0, 0) == "Alice"
    }

    def 'The class of a column can change on the fly, which is how a table changes its renderers.'()
    {
        given : 'A table whose single column is untyped.'
            var data = TableData.of(UI.ListData.ROW_MAJOR, "Done").addRow(true)

        expect : 'It starts out as a plain object column.'
            data.getColumnClass(0) == Object

        when : 'We tell it that the column really holds booleans...'
            var typed = data.setColumnClassAt(0, Boolean)
        then : '...it says so, which is what makes a JTable draw check boxes for it.'
            typed.getColumnClass(0) == Boolean
        and : 'Setting the class it already has hands back the very same table.'
            typed.setColumnClassAt(0, Boolean) === typed
    }

    def 'A table can be made editable (or read only) without touching a cell.'()
    {
        reportInfo """
            Whether the user may edit the cells is part of the layout, so flipping it
            is a change of the value like any other. Note that a table also has to live
            in a mutable `Var` before an edit has anywhere to go.
        """
        given : 'A read only table.'
            var data = TableData.of(UI.ListData.ROW_MAJOR, "A").addRow("x")

        expect : 'It does not permit editing.'
            !data.isEditable()

        when : 'We switch its layout to the editable one...'
            var editable = data.withLayout(UI.ListData.ROW_MAJOR_EDITABLE)
        then : '...it does, and not a single cell moved.'
            editable.isEditable()
            editable.getValueAt(0, 0) == "x"
            editable.cells() == data.cells()
        and : 'Asking for the layout it already has hands back the very same table.'
            editable.withLayout(UI.ListData.ROW_MAJOR_EDITABLE) === editable
    }

    def 'A column major table speaks in rows and columns just like a row major one.'()
    {
        reportInfo """
            The layout only decides how the cells are *stored*. Everything else on this
            class talks in `(row, column)` terms regardless, so a column major table
            grows a row just as happily as a row major one does - it is merely a little
            more work for it internally, which is one reason to prefer row major for
            large tables.
        """
        given : 'The very same cells, read once as rows and once as columns.'
            var cells = Tuple.of(
                            Tuple.ofNullable(Object, "a", "b"),
                            Tuple.ofNullable(Object, "c", "d")
                        )
            var rowMajor    = TableData.of(UI.ListData.ROW_MAJOR, cells)
            var columnMajor = TableData.of(UI.ListData.COLUMN_MAJOR, cells)

        expect : 'They are different values, because they show different tables.'
            rowMajor != columnMajor
        and : 'The same cells are transposed between the two.'
            rowMajor.getValueAt(0, 1) == "b"
            columnMajor.getValueAt(0, 1) == "c"
            rowMajor.getRow(0) == columnMajor.getColumn(0)

        when : 'We add a row to the column major table...'
            var grown = columnMajor.addRow("x", "y")
        then : '...it lands as a row, exactly as it would have in a row major table.'
            grown.getRowCount() == 3
            grown.getRow(2) == Tuple.ofNullable(Object, "x", "y")
            grown.getValueAt(2, 0) == "x"
            grown.getValueAt(2, 1) == "y"

        when : 'And when we remove a column from it...'
            var narrower = columnMajor.removeColumnAt(0)
        then : '...that works out too.'
            narrower.getColumnCount() == 1
            narrower.getColumn(0) == Tuple.ofNullable(Object, "c", "d")
    }

    def 'A ragged table reads as though its holes were filled with nulls.'()
    {
        reportInfo """
            You are not forced to keep every row the same width. A table is as wide as
            its widest row (or as its column metadata, whichever says more), and the
            cells nobody filled in simply read as `null`.
        """
        given : 'A deliberately ragged table.'
            var data = TableData.of(UI.ListData.ROW_MAJOR, Tuple.of(
                            Tuple.ofNullable(Object, "a", "b", "c"),
                            Tuple.ofNullable(Object, "d"),
                            Tuple.ofNullable(Object)
                        ))

        expect : 'It is as wide as its widest row.'
            data.getRowCount() == 3
            data.getColumnCount() == 3
        and : 'The holes read as null, rather than throwing at you.'
            data.getValueAt(1, 0) == "d"
            data.getValueAt(1, 2) == null
            data.getValueAt(2, 0) == null
        and : 'Reading a whole row pads it out to the width of the table.'
            data.getRow(1) == TableData.row("d", null, null)
            data.getRow(2) == TableData.row(null, null, null)
    }

    def 'Naming more columns than the cells fill widens the table.'()
    {
        reportInfo """
            The column metadata takes part in deciding how wide a table is, which is
            exactly what lets a table have columns but no rows. It also means that
            naming a column nobody has filled in yet simply adds an empty column.
        """
        given : 'A table with one filled column, but three names.'
            var data = TableData.of(UI.ListData.ROW_MAJOR, "A")
                            .addRow("x")
                            .setColumnNames("A", "B", "C")

        expect : 'It is three columns wide, the last two of them empty.'
            data.getColumnCount() == 3
            data.getRowCount() == 1
            data.getValueAt(0, 0) == "x"
            data.getValueAt(0, 1) == null
        and : 'The names are all there.'
            data.getColumnName(2) == "C"
    }

    def 'A table has a string representation which tells you what it holds.'()
    {
        given : 'A small table.'
            var data = TableData.of(UI.ListData.ROW_MAJOR, Tuple.of(
                            Tuple.ofNullable(Object, "a", "b"),
                            Tuple.ofNullable(Object, "c", "d")
                        ))

        expect : 'It describes itself usefully.'
            data.toString() == "TableData[layout=ROW_MAJOR, rowCount=2, columnCount=2, " +
                                    "columnNames=Tuple<String?>[], " +
                                    "cells=Tuple<TupleWithDiff>[Tuple<Object?>[a, b], Tuple<Object?>[c, d]]]"
    }

    def 'Reshaping the data of a bound table reaches the JTable, whatever the reshaping was.'()
    {
        reportInfo """
            All of the above would be academic if it did not arrive on screen. So here
            we bind a property to a real `JTable` and then reshape it in the given way,
            checking that the table ends up agreeing with the data.
        """
        given : 'A property holding a table, bound to a real JTable.'
            var model = Var.of(
                            TableData.of(UI.ListData.ROW_MAJOR, "Name", "Age")
                                .addRow("Alice", 30)
                                .addRow("Bob",   42)
                        )
            var table = UI.table(model).get(JTable)

        expect : 'The table starts out mirroring the data.'
            table.getRowCount() == 2
            table.getColumnCount() == 2

        when : 'We apply the given reshaping to the property...'
            model.update({ reshape(it) })
            UI.sync()
        then : '...the JTable agrees with the data on every count.'
            table.getRowCount() == model.get().getRowCount()
            table.getColumnCount() == model.get().getColumnCount()
        and : 'It also agrees on every single cell.'
            (0..<table.getRowCount()).every { r ->
                (0..<table.getColumnCount()).every { c ->
                    table.getValueAt(r, c) == model.get().getValueAt(r, c)
                }
            }

        where : 'We cover the many ways a table may be reshaped.'
            reshape << [
                ({ it.addRow("Carol", 55) }),
                ({ it.addRowAt(0, "Zed", 1) }),
                ({ it.addRows(Tuple.of(TableData.row("C", 1), TableData.row("D", 2))) }),
                ({ it.removeRowAt(0) }),
                ({ it.removeRowsAt(0, 2) }),
                ({ it.removeAllRows() }),
                ({ it.setRowAt(0, "Alicia", 31) }),
                ({ it.setCellAt(1, 1, 43) }),
                ({ it.addColumn("City", String, TableData.row("Rome", "Oslo")) }),
                ({ it.addColumnAt(0, "Id", Integer, TableData.row(1, 2)) }),
                ({ it.removeColumnAt(0) }),
                ({ it.removeColumnsAt(0, 2) }),
                ({ it.setColumnNameAt(0, "Renamed") }),
                ({ it.setColumnClassAt(1, Integer) }),
                ({ it.removeAllRows().removeColumnsAt(0, 2).addColumn("Only", String, TableData.row()).addRow("!") }),
                ({ TableData.empty() }),
            ]
    }

    def 'A bound table syncs a range of rows through a single, targeted event.'()
    {
        reportInfo """
            This is the pay off of the whole design: because a `Tuple` remembers how it
            was derived, adding a range of rows arrives at the `JTable` as *one*
            insertion of a range, not as a rebuild and not as one event per row.
            This is why you should not worry about the performance of a big table.
        """
        given : 'A property holding a table of three rows, bound to a JTable.'
            var model = Var.of(
                            TableData.of(UI.ListData.ROW_MAJOR, "A")
                                .addRow("a1").addRow("a2").addRow("a3")
                        )
            var table = UI.table(model).get(JTable)
        and : 'A listener recording the table model events in a readable form.'
            var events = []
            table.getModel().addTableModelListener({ TableModelEvent e ->
                events << "${e.type == TableModelEvent.INSERT ? 'INSERT' : e.type == TableModelEvent.DELETE ? 'DELETE' : e.type == TableModelEvent.UPDATE ? 'UPDATE' : '?'}[${e.firstRow}..${e.lastRow}]".toString()
            } as TableModelListener)

        when : 'We add two rows in one go...'
            model.update({ it.addRows(Tuple.of(TableData.row("a4"), TableData.row("a5"))) })
            UI.sync()
        then : '...the table hears about it exactly once, as a range.'
            events == ["INSERT[3..4]"]
            table.getRowCount() == 5

        when : 'We remove a range of rows...'
            events.clear()
            model.update({ it.removeRowsAt(0, 2) })
            UI.sync()
        then : '...that too is a single event.'
            events == ["DELETE[0..1]"]
            table.getRowCount() == 3
    }
}
