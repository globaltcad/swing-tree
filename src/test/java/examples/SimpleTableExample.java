package examples;

import swingtree.UI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

import static swingtree.UIFactoryMethods.of;

public class SimpleTableExample {
    static final class MyCellRenderer extends UI.Label {

        private static final float BORDER_WIDTH = 1.25f;
        private static final Color HIGHLIGHT = UI.Color.GREEN;

        public MyCellRenderer(
            final Color backgroundColor,
            final Color foregroundColor,
            final boolean isModified,
            final boolean isFocused
        ) {
            of(this).withLayout("fill, ins 0, gap 0").withMinWidth(60)
                .withText("test test test test")
                .withForeground(foregroundColor)
                .withBackground(backgroundColor)
                .peek( it -> it.setOpaque(true) )
                .isBoldIf(isModified)
                .withStyle( it -> it
                    .padding(0, 0, 0, 22)
                    .cursor(UI.Cursor.HAND)
                    .applyIf(isFocused, inner -> inner
                        .border(BORDER_WIDTH, HIGHLIGHT)
                        .padding(-BORDER_WIDTH)
                        .paddingLeft((22) - BORDER_WIDTH)
                    )
                );
        }
    }

    public static void main(String[] args) {// First we set nimbus as the look and feel.
        try { // TODO: Is there a convenience method for nimbus switching in the framework?
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ( "Nimbus".equals(info.getName()) ) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            UI.show(f->
                UI.panel().withLayout("wrap 2", "", "[]5[]15[]5[]15[]5[]")
                    .add(UI.SPAN(2), UI.label("text field"))
                    .add(UI.textField("").withPrefSize(100, 50).withBackground(UI.Color.RED))
                    .add(UI.textField("").withPrefSize(100, 50).withStyle(delegate -> delegate.backgroundColor(UI.Color.RED)))
                    .add(UI.SPAN(2), UI.label("menu"))
                    .add(UI.menu("").withPrefSize(100, 50).withBackground(UI.Color.RED))
                    .add(UI.menu("").withPrefSize(100, 50).withStyle(delegate -> delegate.backgroundColor(UI.Color.RED)))
                    .add(UI.SPAN(2), UI.label("menu item"))
                    .add(UI.menuItem("").withPrefSize(100, 50).withBackground(UI.Color.RED))
                    .add(UI.menuItem("").withPrefSize(100, 50).withStyle(delegate -> delegate.backgroundColor(UI.Color.RED)))
                    .add(UI.SPAN(2), UI.label("button"))
                    .add(UI.button("").withPrefSize(100, 50).withBackground(UI.Color.RED))
                    .add(UI.button("").withPrefSize(100, 50).withStyle(delegate -> delegate.backgroundColor(UI.Color.RED)))
                    .add(UI.SPAN(2), UI.label("check box"))
                    .add(UI.checkBox("").withPrefSize(100, 50).withBackground(UI.Color.RED))
                    .add(UI.checkBox("").withPrefSize(100, 50).withStyle(delegate -> delegate.backgroundColor(UI.Color.RED)))
                    .add(UI.SPAN(2), UI.label("panel"))
                    .add(UI.panel().withPrefSize(100, 50).withBackground(UI.Color.RED))
                    .add(UI.panel().withPrefSize(100, 50).withStyle(delegate -> delegate.backgroundColor(UI.Color.RED)))
                    .add(UI.SPAN(2), UI.label("label"))
                    .add(UI.label("").withPrefSize(100, 50).withBackground(UI.Color.RED))
                    .add(UI.label("").withPrefSize(100, 50).withStyle(delegate -> delegate.backgroundColor(UI.Color.RED)))
                    .add(UI.SPAN(2), UI.label("html"))
                    .add(UI.html("").withPrefSize(100, 50).withBackground(UI.Color.RED))
                    .add(UI.html("").withPrefSize(100, 50).withStyle(delegate -> delegate.backgroundColor(UI.Color.RED)))
                    .get(JPanel.class)
            );
            JFrame frame = new JFrame("Simple Table Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Create simple data
            Object[][] data = {
                {"Data 1", false, true},
                {"Data 2", true, false},
                {"Data 3", false, false},
                {"Data 4", true, true}
            };

            String[] columns = {"Value", "Modified", "Focused"};

            DefaultTableModel model = new DefaultTableModel(data, columns);
            JTable table = new JTable(model);

            // Custom renderer
            TableCellRenderer renderer = new TableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                    JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int column
                ) {
                    Color bg = isSelected ? table.getSelectionBackground() :
                        (row % 2 == 0 ? new Color(240, 240, 240) : Color.WHITE);

                    Color fg = isSelected ? table.getSelectionForeground() : Color.BLACK;

                    boolean isModified = column == 1 && Boolean.TRUE.equals(value);
                    boolean isFocused = column == 2 && Boolean.TRUE.equals(value);

                    MyCellRenderer cell =
                        new MyCellRenderer(bg, fg, isModified, isFocused);
                    cell.setText(value != null ? value.toString() : "");
                    return cell;
                }
            };

            // Apply to all columns
            for (int i = 0; i < table.getColumnCount(); i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(renderer);
            }

            table.setRowHeight(30);
            JScrollPane scrollPane = new JScrollPane(table);
            frame.add(scrollPane);
            frame.setSize(600, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
