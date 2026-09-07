package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.SwingTree;
import swingtree.UIEnum;

import javax.swing.*;

/**
 * Names the Swing look and feels SwingTree recognises by name, so that it can work
 * around the quirks of each of them. {@link LookAndFeelType#current()} reports which
 * one is installed right now.<br>
 * <b>Note: this enum is package private and should stay that way.</b> Which look and
 * feel is installed is something SwingTree compensates for internally; client code
 * which branches on it hard-codes an assumption that a later release may invalidate.
 */
@Immutable
enum LookAndFeelType implements UIEnum<LookAndFeelType> {
    OTHER,
    METAL,
    FLAT_LAF,
    NIMBUS;
    private static final Logger log = LoggerFactory.getLogger(LookAndFeelType.class);

    /**
     * Reads the class name of the look and feel currently installed in the
     * {@link UIManager} and returns the constant standing for it.
     *
     * @return One of
     * <ul>
     *     <li>{@link LookAndFeelType#FLAT_LAF}</li>
     *     <li>{@link LookAndFeelType#NIMBUS}</li>
     *     <li>{@link LookAndFeelType#METAL}</li>
     *     <li>{@link LookAndFeelType#OTHER}, if none of the above was recognized,
     *         or if the current look and feel could not be read at all.</li>
     * </ul>
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
