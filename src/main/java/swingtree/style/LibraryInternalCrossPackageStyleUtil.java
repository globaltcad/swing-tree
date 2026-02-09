package swingtree.style;

import com.google.errorprone.annotations.DoNotCall;
import org.jspecify.annotations.Nullable;
import sprouts.Result;
import swingtree.UI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.Objects;


/**
 *  <b>This class is technically public, but NOT INTENDED TO BE CALLED BY CLIENT CODE!</b>
 *  It has to be public to allow for code reuse between the {@link swingtree} and {@link swingtree.style} packages.
 */
@Deprecated
public final class LibraryInternalCrossPackageStyleUtil {
    private static final Insets ZERO_INSETS = new Insets(0,0,0,0);

    @DoNotCall @Deprecated
    public static Result<Insets> _onlyBorderInsetsOf(@Nullable Border b, java.awt.Component c) {
        return Result.ofTry(Insets.class, ()->{
            if ( b == null )
                return ZERO_INSETS;

            if ( b instanceof StyleAndAnimationBorder )
                return ((StyleAndAnimationBorder<?>)b).getFullPaddingInsets();

            // Compound border
            if ( b instanceof javax.swing.border.CompoundBorder ) {
                javax.swing.border.CompoundBorder cb = (javax.swing.border.CompoundBorder) b;
                return cb.getOutsideBorder().getBorderInsets(c);
            }
            Insets insets = b.getBorderInsets(c);
            if ( _needsNimbusBorderInsetsCorrection(c) ) {
                int min = Math.min(insets.top, Math.min(insets.left, Math.min(insets.bottom, insets.right)));
                insets = new Insets(min, min, min, min);
            }
            return insets;
        });
    }

    private static boolean _needsNimbusBorderInsetsCorrection(Component c) {
        if ( UI.currentLookAndFeel() == UI.LookAndFeel.NIMBUS ) {
            return c instanceof JButton || c instanceof JToggleButton;
        }
        return false;
    }

    @DoNotCall @Deprecated
    public static void applyFontConfAlignmentsToComponent(FontConf fontConf, JComponent owner) {
        fontConf.horizontalAlignment().forSwing().ifPresent( forSwing -> {
            if ( owner instanceof JLabel ) {
                JLabel label = (JLabel) owner;
                if ( !Objects.equals( label.getHorizontalAlignment(), forSwing ) )
                    label.setHorizontalAlignment( forSwing );
            }
            if ( owner instanceof AbstractButton ) {
                AbstractButton button = (AbstractButton) owner;
                if ( !Objects.equals( button.getHorizontalAlignment(), forSwing ) )
                    button.setHorizontalAlignment( forSwing );
            }
            if ( owner instanceof JTextField ) {
                JTextField textField = (JTextField) owner;
                if ( !Objects.equals( textField.getHorizontalAlignment(), forSwing ) )
                    textField.setHorizontalAlignment( forSwing );
            }
            if ( owner instanceof JTextPane ) {
                int forStyle = StyleConstants.ALIGN_LEFT;
                if ( forSwing == SwingConstants.LEFT ) {
                    forStyle = StyleConstants.ALIGN_LEFT;
                } else if ( forSwing == SwingConstants.RIGHT ) {
                    forStyle = StyleConstants.ALIGN_RIGHT;
                } else if ( forSwing == SwingConstants.CENTER ) {
                    forStyle = StyleConstants.ALIGN_CENTER;
                } else {
                    forStyle = StyleConstants.ALIGN_JUSTIFIED;
                }
                JTextPane textPane = (JTextPane) owner;
                StyledDocument style = textPane.getStyledDocument();
                SimpleAttributeSet align= new SimpleAttributeSet();
                StyleConstants.setAlignment(align, forStyle);
                style.setParagraphAttributes(0, style.getLength(), align, false);
            }
        });
        fontConf.verticalAlignment().forSwing().ifPresent( forSwing -> {
            if ( owner instanceof JLabel ) {
                JLabel label = (JLabel) owner;
                if ( !Objects.equals( label.getVerticalAlignment(), forSwing ) )
                    label.setVerticalAlignment( forSwing );
            }
            if ( owner instanceof AbstractButton ) {
                AbstractButton button = (AbstractButton) owner;
                if ( !Objects.equals( button.getVerticalAlignment(), forSwing ) )
                    button.setVerticalAlignment( forSwing );
            }
        });
    }

    @DoNotCall @Deprecated
    public static @Nullable ComponentUI _findComponentUIOf(JComponent component) {
        // Try to cast to known component types that have getUI() method
        if (component instanceof AbstractButton) {
            return ((AbstractButton) component).getUI();
            /*
                Note, this branch also covers:
                    JButton, JToggleButton, JMenu, JMenuItem,
                    JCheckBox, JCheckBoxMenuItem and JRadioButtonMenuItem
            */
        }
        if (component instanceof JTextComponent) {
            return ((JTextComponent) component).getUI();
            /*
                Note, this branch also covers:
                    JTextField, JPasswordField, JFormattedTextField,
                    JTextArea,
            */
        }
        if (component instanceof JPanel) {
            return ((JPanel) component).getUI();
        }
        if ( component instanceof JTableHeader) {
            return ((JTableHeader)component).getUI();
        }
        if ( component instanceof JLayer<?>) {
            return ((JLayer<?>)component).getUI();
        }
        if (component instanceof JSeparator) {
            return ((JSeparator)component).getUI();
        }
        if (component instanceof JLabel) {
            return ((JLabel) component).getUI();
        }
        if (component instanceof JScrollBar) {
            return ((JScrollBar) component).getUI();
        }
        if (component instanceof JScrollPane) {
            return ((JScrollPane) component).getUI();
        }
        if (component instanceof JSplitPane) {
            return ((JSplitPane) component).getUI();
        }
        if (component instanceof JTabbedPane) {
            return ((JTabbedPane) component).getUI();
        }
        if (component instanceof JToolBar) {
            return ((JToolBar) component).getUI();
        }
        if (component instanceof JRootPane) {
            return ((JRootPane) component).getUI();
        }
        if (component instanceof JPopupMenu) {
            return ((JPopupMenu) component).getUI();
        }
        if (component instanceof JMenuBar) {
            return ((JMenuBar) component).getUI();
        }
        if (component instanceof JProgressBar) {
            return ((JProgressBar) component).getUI();
        }
        if (component instanceof JSlider) {
            return ((JSlider) component).getUI();
        }
        if (component instanceof JSpinner) {
            return ((JSpinner) component).getUI();
        }
        if (component instanceof JToolTip) {
            return ((JToolTip) component).getUI();
        }
        if (component instanceof JTable) {
            return ((JTable) component).getUI();
        }
        if (component instanceof JTree) {
            return ((JTree) component).getUI();
        }
        if (component instanceof JList<?>) {
            return ((JList<?>) component).getUI();
        }
        if (component instanceof JColorChooser) {
            return ((JColorChooser) component).getUI();
        }
        if (component instanceof JFileChooser) {
            return ((JFileChooser) component).getUI();
        }
        if (component instanceof JDesktopPane) {
            return ((JDesktopPane) component).getUI();
        }
        if (component instanceof JInternalFrame) {
            return ((JInternalFrame) component).getUI();
        }
        if (component instanceof JViewport) {
            return ((JViewport) component).getUI();
        }
        if (component instanceof JComboBox<?>) {
            return ((JComboBox<?>) component).getUI();
        }
        if (component instanceof JOptionPane) {
            return ((JOptionPane) component).getUI();
        }

        return null;
    }

}
