package swingtree;

import org.slf4j.Logger;
import sprouts.Val;

import javax.swing.*;

/**
 * A swing tree builder node for {@link AbstractButton} sub-type instances,
 * usually the {@link JButton} type.
 */
public class UIForButton<B extends AbstractButton> extends UIForAnyButton<UIForButton<B>, B>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UIForButton.class);

    protected UIForButton( B component ) { super(component); }

    public UIForButton<B> isBorderPaintedIf( boolean borderPainted ) {
        getComponent().setBorderPainted(borderPainted);
        return this;
    }

    public UIForButton<B> isBorderPaintedIf( Val<Boolean> val ) {
        _onShow(val, v -> isBorderPaintedIf(v) );
        return isBorderPaintedIf( val.get() );
    }

    /**
     * Make this button the default button for the root pane it is in.
     * @return this
     */
    public final UIForButton<B> makeDefaultButton() {
        if ( !(getComponent() instanceof JButton) ) {
            log.warn("Method 'makeDefaultButton()' called on a non JButton component.");
            return this;
        }
        UI.runLater(()->{
            // We do this later because in this point in time the UI is probably not
            // yet fully built (swing-tree is using the builder-pattern).
            JButton button = (JButton) getComponent();
            JRootPane rootPane = SwingUtilities.getRootPane(button);
            if ( rootPane != null )
                rootPane.setDefaultButton(button);
            else
                log.warn("Method 'makeDefaultButton()' called on a JButton component that is not in a JRootPane.");
        });
        return this;
    }
}
