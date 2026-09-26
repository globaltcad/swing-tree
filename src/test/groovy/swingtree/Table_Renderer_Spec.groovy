package swingtree


import swingtree.api.Configurator
import swingtree.threading.EventProcessor
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.UIManager

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
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }


    def 'We can attach a simple table cell renderer to a JTable in just a few lines of code.'()
    {
        given : 'We create a table with a lambda based table model.'
            var ui =
                    UI.table().withModel( m -> m
                        .colNames("A", "B")
                        .colCount({2})
                        .rowCount({3})
                        .getsEntryAt({"O"})
                    )
        and : """
                A mocked cell interpreter which interprets the state of the table cell
                and then defines how it should be rendered (by setting a UI component).
            """
            var render = Mock(Configurator)

        when : 'We attach the interpreter to a table renderer which we then attach to the table.'
            ui = ui.withCellsForColumn("A", it->it.when(String).as(render) )
        and : 'We access the resulting TableCellRenderer instance from the UI.'
            var found = ui.get(JTable).getColumn("A").cellRenderer
        and : 'Finally we access the component from the renderer (which is responsible for the actual rendering).'
            UI.runAndGet({found.getTableCellRendererComponent(new JTable(), "1", false, false, 0, 0)})

        then : 'The mocked cell interpreter is called.'
            1 * render.configure(_)
    }

    def 'We can create a simple table cell renderer through a UI factory method.'()
    {
        reportInfo """
            Note that you can actually pass a simple list of lists provider to the table factory method
            and it will create a table model for you.
            In the table defined below we create a list data based row major table. 
        """
        given : 'A simple table UI with a nested list based data table model.'
            var ui = UI.table(UI.CellOrder.ROW_MAJOR, UI.Editability.EDITABLE, { [["a", "b", "c"], ["1", "2", "3"]] })
        and : 'A component built using the above UI declaration:'
            var table = ui.get(JTable)
        and : """
                A mocked cell interpreter which interprets the state of the table cell
                and then defines how it should be rendered (by setting a UI component).
            """
            var render = Mock(Configurator)

        expect : 'The table UI has the following state:'
            table.getColumnName(0) == "A" // default column names
            table.getColumnName(1) == "B"
            table.getRowCount() == 2
            table.getValueAt(0, 0) == "a"
            table.getValueAt(0, 1) == "b"

        when : 'We build a table renderer for strings and pass our mocked renderer to it.'
            ui = ui.withCellsForColumn(1, it->it.when(String).as(render) )
            table = ui.get(JTable)
        and : 'We access the resulting TableCellRenderer instance from the UI.'
            var found = table
                        .columnModel
                        .getColumn(1)
                        .cellRenderer
        and : 'Finally we access the component from the renderer (which is responsible for the actual rendering).'
            UI.runAndGet({found.getTableCellRendererComponent(new JTable(), "1", false, false, 0, 0)})

        then : 'The mocked cell interpreter is called.'
            1 * render.configure(_)
    }

    def 'We can create a simple column major table cell renderer through a UI factory method.'()
    {
        reportInfo """
            Note that you can actually pass a simple list of lists provider to the table factory method
            and it will create a table model for you.
            In the table defined below we create a list data based column major table. 
        """
        given :
            var ui = UI.table(UI.CellOrder.COLUMN_MAJOR, UI.Editability.EDITABLE, { [["a", "b", "c"], ["1", "2", "3"]] })
        and : 'A component built using the above UI declaration:'
            var table = ui.get(JTable)
        and : """
                A mocked cell interpreter which interprets the state of the table cell
                and then defines how it should be rendered (by setting a UI component).
            """
            var render = Mock(Configurator)

        expect : 'The table UI has the following state:'
            table.getColumnName(0) == "A" // default column names
            table.getColumnName(1) == "B"
            table.getRowCount() == 3
            table.getColumnCount() == 2
            table.getValueAt(0, 0) == "a"
            table.getValueAt(0, 1) == "1"

        when : 'We build a table renderer for strings and pass our mocked renderer to it.'
            ui = ui.withCellsForColumn(1, it->it.when(String).as(render) )
            table = ui.get(JTable)
        and :
            var found = table
                            .columnModel
                            .getColumn(1)
                            .cellRenderer
        and : 'Finally we access the component from the renderer (which is responsible for the actual rendering).'
            UI.runAndGet({found.getTableCellRendererComponent(new JTable(), "1", false, false, 0, 0)})

        then : 'The mocked cell interpreter is called.'
            1 * render.configure(_)
    }

    def 'A map based table can have a custom cell renderer.'()
    {
        reportInfo """
            Note that you can actually pass a provider lambda for a map of header names to column lists 
            to the table factory method and it will create a table model for you, which is based on the provided 
            map provider. This will always result in a column major table. 
        """
        given : 'A simple table UI with a map based data table model.'
            var ui =
                    UI.table(UI.Editability.EDITABLE, { ["X":["a", "b", "c"], "Y":["1", "2", "3"]] })
        and : 'A component built using the above UI declaration:'
            var table = ui.get(JTable)
        and : """
                A mocked cell interpreter which interprets the state of the table cell
                and then defines how it should be rendered (by setting a UI component).
            """
            var render = Mock(Configurator)

        expect : 'The table UI has the following state:'
            table.getColumnName(0) == "X"
            table.getColumnName(1) == "Y"
            table.getRowCount() == 3
            table.getColumnCount() == 2
            table.getValueAt(0, 0) == "a"
            table.getValueAt(0, 1) == "1"

        when : 'We build a table renderer for strings and pass our mocked renderer to it.'
            ui = ui.withCellsForColumn(1, it->it.when(String).as(render) )
            table = ui.get(JTable)
        and : 'We access the resulting TableCellRenderer instance from the UI.'
            var found = table
                                .columnModel
                                .getColumn(1)
                                .cellRenderer
        and : 'Finally we access the component from the renderer (which is responsible for the actual rendering).'
            UI.runAndGet({found.getTableCellRendererComponent(new JTable(), "1", false, false, 0, 0)})

        then : 'The mocked cell interpreter is called.'
            1 * render.configure(_)
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
                        UI.table(UI.CellOrder.ROW_MAJOR, UI.Editability.EDITABLE, { [[1, 2, 3], [7, 8, 9]] })
                        .withCells(
                            it -> it.when(Integer).asText( cell -> cell.entryAsString()+"!" )
                        )
        when : 'We access the resulting TableCellRenderer instance from the UI.'
            var found = ui.get(JTable)
                                    .getDefaultRenderer(Object)
        and : 'Finally we access the component from the renderer (which is responsible for the actual rendering).'
            var component = UI.runAndGet({found.getTableCellRendererComponent(new JTable(), 1, false, false, 0, 0)})

        then : 'The cell is rendered as text (based on a JLabel).'
            component instanceof JLabel
            component.text == "1!"
    }

    def 'The cells of a table built with `withCells(..)` follow a switch of the look and feel.'()
    {
        reportInfo """
            When your application switches the look and feel while it is running, every
            component on screen has to be handed a UI delegate of the new look and feel,
            and that includes the labels a table draws its cells with. SwingTree makes sure
            the component its cell renderer hands out for a cell carries a delegate of the
            look and feel that is installed now, not of the one that was installed when the
            table was built.

            The usual way to switch is to call `UIManager.setLookAndFeel(..)` and then
            `SwingUtilities.updateComponentTreeUI(..)` on each window. That second call walks
            the component tree and calls `updateUI()` on every component it finds. The label
            a cell renderer paints a cell with is not in that tree. To paint one cell,
            Swing adds the label to a hidden `CellRendererPane`, paints it there, and removes
            it again, so between two paints the label has no parent at all. That is why
            `JTable.updateUI()` also calls `updateUI()` on the renderers of its columns and on its
            default renderers, but only on those which are themselves a `Component`. The renderer
            SwingTree builds from your `withCells(..)` rules is not a component, it only creates
            and keeps the labels it hands out. So if SwingTree left those labels alone, they would keep the delegate of
            the old look and feel forever, and every cell would be painted with the old look and
            feel's colours and fonts.

            In this scenario we switch from Metal, Swing's cross-platform look and feel, to
            Nimbus, because both ship with every JDK and they draw a label with different
            delegate classes: Metal with a `MetalLabelUI`, and Nimbus with a `SynthLabelUI`.
            So the class of the label's delegate tells us which look and feel it follows. We
            ask the renderer for a cell the way Swing does, handing it the table itself. This
            specification runs with the strict event processor, which only allows Swing work on
            the event dispatch thread, so every step that touches Swing runs through
            `UI.runAndGet(..)`. At the end we install Metal again, so that no other scenario
            runs under Nimbus.
        """
        given : 'Metal is the installed look and feel.'
            UI.runNow({ UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()) })
        and : 'A table whose cells are rendered as text through `withCells(..)`.'
            var table =
                        UI.table(UI.CellOrder.ROW_MAJOR, UI.Editability.EDITABLE, { [[1, 2, 3], [7, 8, 9]] })
                        .withCells(
                            it -> it.when(Integer).asText( cell -> cell.entryAsString() )
                        )
                        .get(JTable)
        and : 'The label the renderer hands out for a cell, while Metal is installed.'
            var labelUnderMetal = UI.runAndGet({
                table.getDefaultRenderer(Object).getTableCellRendererComponent(table, 1, false, false, 0, 0)
            })

        expect : 'The label is drawn by Metal.'
            labelUnderMetal.getUI() instanceof javax.swing.plaf.metal.MetalLabelUI

        when : 'The application switches to Nimbus the way Swing recommends it.'
            UI.runNow({
                UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel())
                SwingUtilities.updateComponentTreeUI(table)
            })
        and : 'Swing asks the renderer for the same cell again.'
            var labelUnderNimbus = UI.runAndGet({
                table.getDefaultRenderer(Object).getTableCellRendererComponent(table, 1, false, false, 0, 0)
            })

        then : 'The label it hands out is now drawn by Nimbus.'
            labelUnderNimbus.getUI() instanceof javax.swing.plaf.synth.SynthLabelUI

        cleanup : 'We give every other scenario back the look and feel it expects.'
            UI.runNow({ UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()) })
    }

}
