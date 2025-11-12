package swingtree.style;

import com.google.errorprone.annotations.DoNotCall;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.util.Objects;


/**
 *  <b>This class is technically public, but NOT INTENDED TO BE CALLED BY CLIENT CODE!</b>
 *  It has to be public to allow for code reuse between the {@link swingtree} and {@link swingtree.style} packages.
 */
@Deprecated
public final class LibraryInternalCrossPackageStyleUtil {

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

}
