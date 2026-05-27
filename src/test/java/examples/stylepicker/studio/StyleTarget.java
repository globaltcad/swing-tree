package examples.stylepicker.studio;

import sprouts.Tuple;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  What a single style rule applies to. A {@code StyleSheet} can select
 *  components in several ways; this studio supports the two that make sense for
 *  designing a reusable look-and-feel:
 *
 *  <ul>
 *    <li>a high-level semantic <b>{@link Look} group</b> ({@code group(Look.CARD)}) —
 *        for roles the app opts into by tagging, and</li>
 *    <li>a Swing <b>component type</b> ({@code type(JButton.class)}) — so a sheet
 *        can give <i>every</i> button or slider a baseline look without any
 *        tagging at all.</li>
 *  </ul>
 *
 *  Groups and types compose in real SwingTree style sheets (a group rule is more
 *  specific than a type rule), which is exactly why a coherent sheet needs both.
 *  Instances are value objects so they work as map keys and combo-box items.
 */
public final class StyleTarget {

    private final Look                          group; // non-null iff this is a group target
    private final Class<? extends JComponent>   type;  // non-null iff this is a type target

    private StyleTarget(Look group, Class<? extends JComponent> type) {
        this.group = group;
        this.type  = type;
    }

    public static StyleTarget of(Look group)                        { return new StyleTarget(group, null); }
    public static StyleTarget of(Class<? extends JComponent> type)  { return new StyleTarget(null, type); }

    public boolean isGroup()                       { return group != null; }
    public Look group()                            { return group; }
    public Class<? extends JComponent> type()      { return type; }

    public String pretty() {
        return isGroup() ? "Group · " + group.pretty()
                         : "Type · " + type.getSimpleName();
    }

    /** The canonical, ordered list shown in the selector: all groups, then a comprehensive set of types. */
    public static List<StyleTarget> all() {
        List<StyleTarget> out = new ArrayList<>();
        for (Look look : Look.values())
            out.add(of(look));
        for (Class<? extends JComponent> type : TYPES)
            out.add(of(type));
        return out;
    }

    /** A broad-but-sensible cross-section of Swing widget types worth styling. */
    static final List<Class<? extends JComponent>> TYPES = Tuple.of(
        JComponent.class,        // everything (the ultimate fallback)
        JPanel.class,
        JLabel.class,
        AbstractButton.class,    // all buttons at once
        JButton.class,
        JToggleButton.class,
        JCheckBox.class,
        JRadioButton.class,
        JTextComponent.class,    // all text inputs at once
        JTextField.class,
        JTextArea.class,
        JComboBox.class,
        JSlider.class,
        JList.class,
        JScrollPane.class,
        JTabbedPane.class,
        JProgressBar.class,
        JSpinner.class,
        JSeparator.class
    ).toList();

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StyleTarget)) return false;
        StyleTarget that = (StyleTarget) o;
        return group == that.group && Objects.equals(type, that.type);
    }

    @Override public int hashCode() { return Objects.hash(group, type); }

    @Override public String toString() { return pretty(); }
}