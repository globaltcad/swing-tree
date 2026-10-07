package examples.scrollpanes;

import net.miginfocom.swing.MigLayout;
import sprouts.Var;
import swingtree.UI;

import javax.swing.*;
import java.awt.Color;

/**
 *  Opens two windows with the same content, one built with plain Swing and one with SwingTree.
 *  Press the same button in both windows and watch the orange labels:
 *  <ul>
 *      <li><b>Hide / show the text field</b>:
 *          in the plain Swing window, hiding leaves an empty gap where the text field was,
 *          and showing makes no room for it, so it does not appear.</li>
 *      <li><b>Add a row to the scroll pane</b>:
 *          in the plain Swing window, the scroll pane keeps its height and grows a scroll bar,
 *          although there is plenty of room below it.</li>
 *      <li><b>Change the header</b>:
 *          this changes nothing but the text of the header, but in the plain Swing window it
 *          finally lays out the window, so everything you did before suddenly jumps into place.</li>
 *  </ul>
 *  The SwingTree window does the right thing right away, and the header changes nothing else in it.
 *  <p>
 *  The two are separate windows on purpose: a layout anywhere in a window also lays out
 *  every part of the window left waiting for one, so a SwingTree half would repair a Swing half.
 */
public class StaleLayoutDemo
{
    private static final String SHORT_HEADER = "A header";
    private static final String LONG_HEADER  = "A header with a longer text";

    public static void main( String... args ) {
        SwingUtilities.invokeLater(() -> {
            JFrame swing = swingWindow();
            JFrame swingTree = swingTreeWindow();
            swing.setLocation(100, 100);
            swingTree.setLocation(100 + swing.getWidth() + 20, 100);
            swing.setVisible(true);
            swingTree.setVisible(true);
        });
    }

    static JFrame swingWindow() {
        JLabel header = new JLabel(SHORT_HEADER);
        JTextField textField = new JTextField("A text field");
        JPanel rows = new JPanel(new MigLayout("wrap 1, ins 0"));
        rows.add(new JLabel("Row 1"));

        JButton toggle = new JButton("Hide / show the text field");
        toggle.addActionListener(e -> textField.setVisible(!textField.isVisible()));
        JButton addRow = new JButton("Add a row to the scroll pane");
        addRow.addActionListener(e -> {
            rows.add(new JLabel("Row " + (rows.getComponentCount() + 1)));
            rows.revalidate();
        });
        JButton changeHeader = new JButton("Change the header");
        changeHeader.addActionListener(e -> header.setText(header.getText().equals(SHORT_HEADER) ? LONG_HEADER : SHORT_HEADER));

        JPanel column = new JPanel(new MigLayout("wrap 1, hidemode 3", "[grow]"));
        column.add(toggle, "growx");
        column.add(addRow, "growx");
        column.add(changeHeader, "growx, gapbottom 20");
        column.add(header);
        column.add(textField, "growx");
        column.add(orange(new JLabel("Below the text field")), "growx");
        column.add(new JScrollPane(rows), "growx");
        column.add(orange(new JLabel("Below the scroll pane")), "growx");

        return window("Swing by itself", column);
    }

    static JFrame swingTreeWindow() {
        Var<Boolean> textFieldIsShown = Var.of(true);
        Var<String> headerText = Var.of(SHORT_HEADER);
        JPanel[] rows = new JPanel[1];

        JPanel column =
            UI.panel("wrap 1, hidemode 3", "[grow]")
            .add("growx",
                UI.button("Hide / show the text field")
                .onClick( it -> textFieldIsShown.set(!textFieldIsShown.get()) )
            )
            .add("growx",
                UI.button("Add a row to the scroll pane")
                .onClick( it -> {
                    rows[0].add(new JLabel("Row " + (rows[0].getComponentCount() + 1)));
                    rows[0].revalidate();
                })
            )
            .add("growx, gapbottom 20",
                UI.button("Change the header")
                .onClick( it -> headerText.set(headerText.get().equals(SHORT_HEADER) ? LONG_HEADER : SHORT_HEADER) )
            )
            .add(UI.label(headerText))
            .add("growx", UI.textField("A text field").isVisibleIf(textFieldIsShown))
            .add("growx", UI.label("Below the text field").withBackground(Color.ORANGE))
            .add("growx",
                UI.scrollPane()
                .add(
                    UI.panel("wrap 1, ins 0").peek( it -> rows[0] = it )
                    .add(UI.label("Row 1"))
                )
            )
            .add("growx", UI.label("Below the scroll pane").withBackground(Color.ORANGE))
            .get(JPanel.class);

        return window("SwingTree", column);
    }

    private static JLabel orange( JLabel label ) {
        label.setOpaque(true);
        label.setBackground(Color.ORANGE);
        return label;
    }

    private static JFrame window( String title, JPanel content ) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.add(content);
        frame.setSize(360, 480);
        return frame;
    }
}
