package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.SwingTree;
import swingtree.UIEnum;

import javax.swing.*;

/**
 * Set of enum instances defining common types of Swing look and feels.
 * Use {@link LookAndFeelType#current()} to check which look and feel is currently active.<br>
 * <b>Note: This class is deliberately package private and should not be public!</b>
 */
@Immutable
enum LookAndFeelType implements UIEnum<LookAndFeelType> {
    OTHER,
    METAL,
    FLAT_LAF,
    NIMBUS;
    private static final Logger log = LoggerFactory.getLogger(LookAndFeelType.class);

    /**
     * SwingTree tries to be compatible with different look and feels, which is
     * why it maintains a set of constants for the most common look and feels through
     * the {@link LookAndFeelType} enum.
     * This method returns the current look and feel of the application
     * or {@link LookAndFeelType#OTHER} if the look and feel is not recognized.
     *
     * @return One of
     * <ul>
     *     <li>{@link LookAndFeelType#FLAT_LAF}</li>
     *     <li>{@link LookAndFeelType#NIMBUS}</li>
     *     <li>{@link LookAndFeelType#METAL}</li>
     * </ul>
     * or {@link LookAndFeelType#OTHER} if none of the above
     * was recognized.
     */
    public static LookAndFeelType current() {
        try {
            String laf = UIManager.getLookAndFeel().getClass().getName();
            if (laf.contains("FlatLaf")) return FLAT_LAF;
            if (laf.contains("Nimbus")) return NIMBUS;
            if (laf.contains("Metal")) return METAL;
        } catch (Exception e) {
            log.warn(SwingTree.get().logMarker(), "Failed to determine current look and feel.", e);
        }

        return OTHER;
    }
}
